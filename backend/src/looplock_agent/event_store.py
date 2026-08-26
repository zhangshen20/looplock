import asyncio
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from enum import StrEnum
from typing import Protocol
from uuid import UUID, uuid4

from google.api_core.exceptions import GoogleAPICallError, RetryError
from google.cloud import firestore_v1
from google.cloud.firestore_v1.async_transaction import async_transactional

from looplock_agent.hashing import target_hash
from looplock_agent.models import (
    Action,
    Classification,
    ClassificationRequest,
    ClassificationResponse,
    EventStatusResponse,
    ReasonCode,
)


class EventStoreError(RuntimeError):
    pass


class EventStoreUnavailable(EventStoreError):
    pass


class EventIdentityConflict(EventStoreError):
    pass


class EventRecordInvalid(EventStoreError):
    pass


class EventReservationLost(EventStoreUnavailable):
    pass


class ReservationKind(StrEnum):
    OWNER = "OWNER"
    PROCESSING = "PROCESSING"
    TERMINAL = "TERMINAL"


class StoredStatus(StrEnum):
    PROCESSING = "PROCESSING"
    TIGHTEN = "TIGHTEN"
    REVIEW = "REVIEW"


@dataclass(frozen=True)
class StoredEvent:
    event_id: UUID
    commitment_id: UUID
    target_hash: str
    status: StoredStatus
    classification: Classification | None = None
    confidence: float | None = None
    reason_code: ReasonCode | None = None

    def terminal_response(
        self,
        request: ClassificationRequest,
    ) -> ClassificationResponse:
        ensure_identity(self, request)
        if self.status is StoredStatus.PROCESSING:
            raise EventRecordInvalid("Processing event has no terminal response")
        if self.classification is None or self.confidence is None or self.reason_code is None:
            raise EventRecordInvalid("Terminal event is missing bounded result fields")
        return ClassificationResponse(
            schema_version=1,
            event_id=self.event_id,
            commitment_id=self.commitment_id,
            action=Action(self.status.value),
            target_type="PACKAGE",
            target_value=request.target.package_name,
            classification=self.classification,
            confidence=self.confidence,
            reason_code=self.reason_code,
            reason=canonical_reason(self.reason_code),
        )

    def safe_status(self) -> EventStatusResponse:
        return EventStatusResponse(
            event_id=self.event_id,
            commitment_id=self.commitment_id,
            status=self.status.value,
            classification=self.classification,
            confidence=self.confidence,
            reason_code=self.reason_code,
        )


@dataclass(frozen=True)
class Reservation:
    kind: ReservationKind
    event: StoredEvent
    lease_token: str | None = None


class EventStore(Protocol):
    async def reserve(self, request: ClassificationRequest) -> Reservation: ...

    async def complete(
        self,
        request: ClassificationRequest,
        response: ClassificationResponse,
        lease_token: str,
    ) -> StoredEvent: ...

    async def get(self, event_id: UUID) -> StoredEvent | None: ...


class InMemoryEventStore:
    """Deterministic test store with the same identity and terminal-state rules."""

    def __init__(self, *, lease_seconds: int = 60) -> None:
        self._events: dict[UUID, tuple[StoredEvent, datetime, str]] = {}
        self._lock = asyncio.Lock()
        self._lease = timedelta(seconds=lease_seconds)

    async def reserve(self, request: ClassificationRequest) -> Reservation:
        now = datetime.now(UTC)
        async with self._lock:
            existing = self._events.get(request.event_id)
            if existing is None:
                event = processing_event(request)
                lease_token = uuid4().hex
                self._events[request.event_id] = (event, now + self._lease, lease_token)
                return Reservation(ReservationKind.OWNER, event, lease_token)
            event, lease_expires_at, current_token = existing
            ensure_identity(event, request)
            if event.status is not StoredStatus.PROCESSING:
                return Reservation(ReservationKind.TERMINAL, event)
            if lease_expires_at > now:
                return Reservation(ReservationKind.PROCESSING, event)
            lease_token = uuid4().hex
            self._events[request.event_id] = (event, now + self._lease, lease_token)
            return Reservation(ReservationKind.OWNER, event, lease_token)

    async def complete(
        self,
        request: ClassificationRequest,
        response: ClassificationResponse,
        lease_token: str,
    ) -> StoredEvent:
        async with self._lock:
            existing = self._events.get(request.event_id)
            if existing is None:
                raise EventRecordInvalid("Cannot complete an unreserved event")
            event, lease, current_token = existing
            ensure_identity(event, request)
            if event.status is not StoredStatus.PROCESSING:
                return event
            if current_token != lease_token:
                raise EventReservationLost("Event reservation was reclaimed")
            terminal = terminal_event(request, response)
            self._events[request.event_id] = (terminal, lease, current_token)
            return terminal

    async def get(self, event_id: UUID) -> StoredEvent | None:
        async with self._lock:
            existing = self._events.get(event_id)
            return existing[0] if existing else None

    async def inspect_document(self, event_id: UUID) -> dict | None:
        async with self._lock:
            existing = self._events.get(event_id)
            if existing is None:
                return None
            event, lease, lease_token = existing
            if event.status is StoredStatus.PROCESSING:
                return processing_document(
                    event,
                    lease_expires_at=lease,
                    lease_token=lease_token,
                )
            return terminal_document(event)


class FirestoreEventStore:
    def __init__(
        self,
        *,
        project: str | None,
        database: str,
        collection: str,
        lease_seconds: int,
    ) -> None:
        self._client = firestore_v1.AsyncClient(project=project, database=database)
        self._collection = self._client.collection(collection)
        self._lease = timedelta(seconds=lease_seconds)

    async def reserve(self, request: ClassificationRequest) -> Reservation:
        reference = self._collection.document(str(request.event_id))
        now = datetime.now(UTC)
        lease_token = uuid4().hex
        transaction = self._client.transaction()

        @async_transactional
        async def reserve_in_transaction(transaction):
            snapshot = await reference.get(transaction=transaction)
            if not snapshot.exists:
                event = processing_event(request)
                transaction.create(
                    reference,
                    processing_document(
                        event,
                        lease_expires_at=now + self._lease,
                        lease_token=lease_token,
                    ),
                )
                return Reservation(ReservationKind.OWNER, event, lease_token)
            document = snapshot.to_dict()
            event = event_from_document(document)
            ensure_identity(event, request)
            if event.status is not StoredStatus.PROCESSING:
                return Reservation(ReservationKind.TERMINAL, event)
            lease_expires_at = document.get("lease_expires_at")
            if not isinstance(lease_expires_at, datetime):
                raise EventRecordInvalid("Processing event has an invalid lease")
            if lease_expires_at > now:
                return Reservation(ReservationKind.PROCESSING, event)
            transaction.update(
                reference,
                {
                    "updated_at": firestore_v1.SERVER_TIMESTAMP,
                    "lease_expires_at": now + self._lease,
                    "lease_token": lease_token,
                },
            )
            return Reservation(ReservationKind.OWNER, event, lease_token)

        try:
            return await reserve_in_transaction(transaction)
        except (EventIdentityConflict, EventRecordInvalid):
            raise
        except (GoogleAPICallError, RetryError) as error:
            raise EventStoreUnavailable("Firestore reservation failed") from error

    async def complete(
        self,
        request: ClassificationRequest,
        response: ClassificationResponse,
        lease_token: str,
    ) -> StoredEvent:
        reference = self._collection.document(str(request.event_id))
        transaction = self._client.transaction()

        @async_transactional
        async def complete_in_transaction(transaction):
            snapshot = await reference.get(transaction=transaction)
            if not snapshot.exists:
                raise EventRecordInvalid("Cannot complete an unreserved event")
            current = event_from_document(snapshot.to_dict())
            ensure_identity(current, request)
            if current.status is not StoredStatus.PROCESSING:
                return current
            current_token = snapshot.to_dict().get("lease_token")
            if current_token != lease_token:
                raise EventReservationLost("Event reservation was reclaimed")
            terminal = terminal_event(request, response)
            transaction.update(reference, terminal_update(terminal))
            return terminal

        try:
            return await complete_in_transaction(transaction)
        except (EventIdentityConflict, EventRecordInvalid, EventReservationLost):
            raise
        except (GoogleAPICallError, RetryError) as error:
            raise EventStoreUnavailable("Firestore completion failed") from error

    async def get(self, event_id: UUID) -> StoredEvent | None:
        try:
            snapshot = await self._collection.document(str(event_id)).get()
        except (GoogleAPICallError, RetryError) as error:
            raise EventStoreUnavailable("Firestore read failed") from error
        if not snapshot.exists:
            return None
        return event_from_document(snapshot.to_dict())


def processing_event(request: ClassificationRequest) -> StoredEvent:
    return StoredEvent(
        event_id=request.event_id,
        commitment_id=request.commitment_id,
        target_hash=target_hash(request.target.package_name),
        status=StoredStatus.PROCESSING,
    )


def terminal_event(
    request: ClassificationRequest,
    response: ClassificationResponse,
) -> StoredEvent:
    if response.event_id != request.event_id or response.commitment_id != request.commitment_id:
        raise EventRecordInvalid("Terminal response identity does not match request")
    return StoredEvent(
        event_id=request.event_id,
        commitment_id=request.commitment_id,
        target_hash=target_hash(request.target.package_name),
        status=StoredStatus(response.action.value),
        classification=response.classification,
        confidence=response.confidence,
        reason_code=response.reason_code,
    )


def ensure_identity(event: StoredEvent, request: ClassificationRequest) -> None:
    if (
        event.event_id != request.event_id
        or event.commitment_id != request.commitment_id
        or event.target_hash != target_hash(request.target.package_name)
    ):
        raise EventIdentityConflict("Event ID was already used for different metadata")


def canonical_reason(reason_code: ReasonCode) -> str:
    return {
        ReasonCode.FIXTURE_MATCH: "The metadata matches the harmless LuckyMirror fixture.",
        ReasonCode.NEEDS_REVIEW: "The metadata does not exactly match the harmless demo fixture.",
        ReasonCode.MODEL_UNAVAILABLE: "The classifier was unavailable; quarantine remains.",
        ReasonCode.INVALID_MODEL_OUTPUT: "The classifier did not return a valid bounded result.",
    }[reason_code]


def base_document(event: StoredEvent) -> dict:
    return {
        "schema_version": 1,
        "event_id": str(event.event_id),
        "commitment_id": str(event.commitment_id),
        "target_hash": event.target_hash,
        "status": event.status.value,
        "created_at": firestore_v1.SERVER_TIMESTAMP,
        "updated_at": firestore_v1.SERVER_TIMESTAMP,
    }


def processing_document(
    event: StoredEvent,
    *,
    lease_expires_at: datetime,
    lease_token: str,
) -> dict:
    if event.status is not StoredStatus.PROCESSING:
        raise EventRecordInvalid("Only processing events can have a lease")
    return {
        **base_document(event),
        "lease_expires_at": lease_expires_at,
        "lease_token": lease_token,
    }


def terminal_document(event: StoredEvent) -> dict:
    if event.status is StoredStatus.PROCESSING:
        raise EventRecordInvalid("Processing event has no terminal fields")
    return {**base_document(event), **terminal_fields(event)}


def terminal_update(event: StoredEvent) -> dict:
    return {
        "status": event.status.value,
        **terminal_fields(event),
        "updated_at": firestore_v1.SERVER_TIMESTAMP,
        "lease_expires_at": firestore_v1.DELETE_FIELD,
        "lease_token": firestore_v1.DELETE_FIELD,
    }


def terminal_fields(event: StoredEvent) -> dict:
    if event.classification is None or event.confidence is None or event.reason_code is None:
        raise EventRecordInvalid("Terminal event is missing bounded result fields")
    return {
        "classification": event.classification.value,
        "confidence": event.confidence,
        "reason_code": event.reason_code.value,
    }


def event_from_document(document: dict | None) -> StoredEvent:
    if not isinstance(document, dict):
        raise EventRecordInvalid("Firestore event is not a document")
    try:
        if document["schema_version"] != 1:
            raise ValueError("unsupported schema_version")
        status = StoredStatus(document["status"])
        base_fields = {
            "schema_version",
            "event_id",
            "commitment_id",
            "target_hash",
            "status",
            "created_at",
            "updated_at",
        }
        processing_fields = {"lease_expires_at", "lease_token"}
        terminal_result_fields = {"classification", "confidence", "reason_code"}
        expected_fields = (
            base_fields | processing_fields
            if status is StoredStatus.PROCESSING
            else base_fields | terminal_result_fields
        )
        if set(document) != expected_fields:
            raise ValueError("event document contains missing or unknown fields")
        classification = (
            Classification(document["classification"])
            if status is not StoredStatus.PROCESSING
            else None
        )
        confidence = float(document["confidence"]) if status is not StoredStatus.PROCESSING else None
        reason_code = (
            ReasonCode(document["reason_code"])
            if status is not StoredStatus.PROCESSING
            else None
        )
        target_hash_value = document["target_hash"]
        if (
            not isinstance(target_hash_value, str)
            or len(target_hash_value) != 64
            or any(character not in "0123456789abcdef" for character in target_hash_value)
        ):
            raise ValueError("target_hash must be a lowercase SHA-256 digest")
        if status is StoredStatus.PROCESSING:
            lease_expires_at = document["lease_expires_at"]
            lease_token = document["lease_token"]
            if not isinstance(lease_expires_at, datetime):
                raise ValueError("lease_expires_at must be a timestamp")
            if (
                not isinstance(lease_token, str)
                or len(lease_token) != 32
                or any(character not in "0123456789abcdef" for character in lease_token)
            ):
                raise ValueError("lease_token must be a lowercase UUID token")
        return StoredEvent(
            event_id=UUID(document["event_id"]),
            commitment_id=UUID(document["commitment_id"]),
            target_hash=target_hash_value,
            status=status,
            classification=classification,
            confidence=confidence,
            reason_code=reason_code,
        )
    except (KeyError, TypeError, ValueError) as error:
        raise EventRecordInvalid("Firestore event has an invalid bounded schema") from error
