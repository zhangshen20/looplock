import asyncio
import json
from uuid import UUID

import pytest
from fastapi.testclient import TestClient

from looplock_agent.classification_service import classify_safely
from looplock_agent.event_store import (
    EventReservationLost,
    EventStoreUnavailable,
    InMemoryEventStore,
    processing_event,
)
from looplock_agent.hashing import target_hash
from looplock_agent.main import create_app
from looplock_agent.models import (
    Action,
    AgentDraft,
    Classification,
    ClassificationRequest,
    ReasonCode,
)


REQUEST = {
    "schema_version": 1,
    "event_id": "84f4d0dd-1317-4f18-ad21-c3ab975b2f30",
    "commitment_id": "a5b36c91-352b-42cc-9c6a-f6ffdf12107e",
    "target": {
        "type": "PACKAGE",
        "package_name": "com.histopgambling.fixture.luckymirror",
        "label": "LuckyMirror Demo",
        "version_code": 1,
    },
}


class CountingClassifier:
    def __init__(self) -> None:
        self.calls = 0

    async def classify(self, request):
        self.calls += 1
        return AgentDraft(
            action=Action.TIGHTEN,
            classification=Classification.DEMO_GAMBLING_APP,
            confidence=0.98,
            reason_code=ReasonCode.FIXTURE_MATCH,
            reason="Any model wording is normalized before persistence.",
        )


def post(client: TestClient, payload: dict = REQUEST):
    return client.post(
        "/v1/classifications",
        json=payload,
        headers={"Idempotency-Key": payload["event_id"]},
    )


def test_three_retries_create_one_result_and_invoke_model_once() -> None:
    classifier = CountingClassifier()
    store = InMemoryEventStore()
    client = TestClient(create_app(classifier, store))

    responses = [post(client) for _ in range(3)]

    assert [response.status_code for response in responses] == [200, 200, 200]
    assert responses[0].json() == responses[1].json() == responses[2].json()
    assert classifier.calls == 1


def test_persistent_document_contains_only_approved_fields_and_hash() -> None:
    classifier = CountingClassifier()
    store = InMemoryEventStore()
    client = TestClient(create_app(classifier, store))
    assert post(client).status_code == 200

    document = asyncio.run(store.inspect_document(UUID(REQUEST["event_id"])))

    assert document is not None
    assert set(document) == {
        "schema_version",
        "event_id",
        "commitment_id",
        "target_hash",
        "status",
        "classification",
        "confidence",
        "reason_code",
        "created_at",
        "updated_at",
    }
    assert document["target_hash"] == target_hash(REQUEST["target"]["package_name"])
    serialized = json.dumps(document, default=str)
    assert REQUEST["target"]["package_name"] not in serialized
    assert REQUEST["target"]["label"] not in serialized
    assert "version_code" not in serialized
    assert "reason" not in document
    assert "user" not in serialized.lower()
    assert "account" not in serialized.lower()


def test_duplicate_event_with_different_identity_is_rejected() -> None:
    classifier = CountingClassifier()
    store = InMemoryEventStore()
    client = TestClient(create_app(classifier, store))
    assert post(client).status_code == 200
    conflicting = json.loads(json.dumps(REQUEST))
    conflicting["commitment_id"] = "18ff8197-baf8-41c2-bcd9-3f2405906c89"

    response = post(client, conflicting)

    assert response.status_code == 409
    assert classifier.calls == 1


def test_duplicate_while_lease_is_active_returns_processing_without_model_call() -> None:
    classifier = CountingClassifier()
    store = InMemoryEventStore()
    request = ClassificationRequest.model_validate(REQUEST)
    asyncio.run(store.reserve(request))
    client = TestClient(create_app(classifier, store))

    response = post(client)

    assert response.status_code == 202
    assert response.json() == {
        "status": "processing",
        "event_id": REQUEST["event_id"],
    }
    assert classifier.calls == 0


def test_status_route_exposes_codes_but_not_raw_target() -> None:
    classifier = CountingClassifier()
    store = InMemoryEventStore()
    client = TestClient(create_app(classifier, store))
    assert post(client).status_code == 200

    response = client.get(f"/v1/classifications/{REQUEST['event_id']}")

    assert response.status_code == 200
    assert response.json() == {
        "event_id": REQUEST["event_id"],
        "commitment_id": REQUEST["commitment_id"],
        "status": "TIGHTEN",
        "classification": "DEMO_GAMBLING_APP",
        "confidence": 0.98,
        "reason_code": "FIXTURE_MATCH",
    }
    assert REQUEST["target"]["package_name"] not in response.text
    assert REQUEST["target"]["label"] not in response.text


class UnavailableStore:
    async def reserve(self, request):
        raise EventStoreUnavailable("offline")

    async def complete(self, request, response, lease_token):
        raise AssertionError("complete must not run")

    async def get(self, event_id):
        raise EventStoreUnavailable("offline")


def test_store_failure_returns_retryable_error_without_false_success() -> None:
    classifier = CountingClassifier()
    client = TestClient(create_app(classifier, UnavailableStore()))

    post_response = post(client)
    get_response = client.get(f"/v1/classifications/{REQUEST['event_id']}")

    assert post_response.status_code == 503
    assert get_response.status_code == 503
    assert classifier.calls == 0


class CompletionUnavailableStore(InMemoryEventStore):
    async def complete(self, request, response, lease_token):
        raise EventStoreUnavailable("write interrupted")


def test_completion_failure_does_not_return_unpersisted_model_result() -> None:
    classifier = CountingClassifier()
    client = TestClient(create_app(classifier, CompletionUnavailableStore()))

    response = post(client)

    assert response.status_code == 503
    assert classifier.calls == 1
    assert "TIGHTEN" not in response.text


def test_status_route_rejects_unknown_and_invalid_ids() -> None:
    client = TestClient(create_app(CountingClassifier(), InMemoryEventStore()))

    missing = client.get("/v1/classifications/43e8d400-9452-43c3-9c79-0630fe1eeccd")
    invalid = client.get("/v1/classifications/not-a-uuid")

    assert missing.status_code == 404
    assert invalid.status_code == 422


@pytest.mark.asyncio
async def test_processing_event_contains_no_raw_metadata() -> None:
    request = ClassificationRequest.model_validate(REQUEST)
    event = processing_event(request)

    assert event.target_hash == target_hash(request.target.package_name)
    assert not hasattr(event, "package_name")
    assert not hasattr(event, "label")
    assert not hasattr(event, "version_code")


@pytest.mark.asyncio
async def test_reclaimed_lease_fences_out_expired_worker() -> None:
    store = InMemoryEventStore(lease_seconds=-1)
    request = ClassificationRequest.model_validate(REQUEST)
    first = await store.reserve(request)
    second = await store.reserve(request)
    classifier = CountingClassifier()
    validated = await classify_safely(request, classifier)

    assert first.lease_token != second.lease_token
    with pytest.raises(EventReservationLost):
        await store.complete(request, validated, first.lease_token)
    terminal = await store.complete(request, validated, second.lease_token)
    assert terminal.status.value == "TIGHTEN"
