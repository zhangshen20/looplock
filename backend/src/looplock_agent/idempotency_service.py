from dataclasses import dataclass

from looplock_agent.classification_service import Classifier, classify_safely
from looplock_agent.event_store import EventStore, ReservationKind
from looplock_agent.models import (
    ClassificationRequest,
    ClassificationResponse,
    ProcessingResponse,
)


@dataclass(frozen=True)
class ClassificationOutcome:
    response: ClassificationResponse | ProcessingResponse
    processing: bool = False


async def classify_idempotently(
    request: ClassificationRequest,
    classifier: Classifier,
    event_store: EventStore,
) -> ClassificationOutcome:
    reservation = await event_store.reserve(request)
    if reservation.kind is ReservationKind.TERMINAL:
        return ClassificationOutcome(reservation.event.terminal_response(request))
    if reservation.kind is ReservationKind.PROCESSING:
        return ClassificationOutcome(
            ProcessingResponse(event_id=request.event_id),
            processing=True,
        )

    if reservation.lease_token is None:
        raise RuntimeError("Owner reservation is missing its lease token")
    response = await classify_safely(request, classifier)
    terminal = await event_store.complete(request, response, reservation.lease_token)
    return ClassificationOutcome(terminal.terminal_response(request))
