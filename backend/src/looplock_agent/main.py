from typing import Annotated
from uuid import UUID

from fastapi import Depends, FastAPI, Header, HTTPException, Request, status
from fastapi.responses import JSONResponse

from looplock_agent.classification_service import (
    AdkClassifier,
    Classifier,
    classify_safely,
)
from looplock_agent.config import get_settings
from looplock_agent.event_store import (
    EventIdentityConflict,
    EventRecordInvalid,
    EventStore,
    EventStoreUnavailable,
    FirestoreEventStore,
)
from looplock_agent.idempotency_service import classify_idempotently
from looplock_agent.models import (
    ClassificationRequest,
    ClassificationResponse,
    EventStatusResponse,
    HealthResponse,
    ProcessingResponse,
)


MAX_BODY_BYTES = 8 * 1024


def create_app(
    classifier: Classifier | None = None,
    event_store: EventStore | None = None,
) -> FastAPI:
    api = FastAPI(
        title="LoopLock bounded classification agent",
        version="0.1.0",
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )

    def get_classifier() -> Classifier:
        if classifier is not None:
            return classifier
        if not hasattr(api.state, "classifier"):
            api.state.classifier = AdkClassifier(get_settings())
        return api.state.classifier

    def get_event_store() -> EventStore:
        if event_store is not None:
            return event_store
        if not hasattr(api.state, "event_store"):
            settings = get_settings()
            api.state.event_store = FirestoreEventStore(
                project=settings.google_cloud_project,
                database=settings.firestore_database,
                collection=settings.firestore_collection,
                lease_seconds=settings.processing_lease_seconds,
            )
        return api.state.event_store

    @api.middleware("http")
    async def reject_oversized_body(request: Request, call_next):
        content_length = request.headers.get("content-length")
        if content_length and content_length.isdigit() and int(content_length) > MAX_BODY_BYTES:
            return JSONResponse(
                status_code=status.HTTP_413_CONTENT_TOO_LARGE,
                content={"detail": "Request body exceeds 8 KiB"},
            )
        return await call_next(request)

    @api.get("/healthz", response_model=HealthResponse)
    async def health() -> HealthResponse:
        return HealthResponse()

    @api.get("/v1/health", response_model=HealthResponse)
    async def cloud_health() -> HealthResponse:
        return HealthResponse()

    @api.post(
        "/v1/classifications",
        response_model=ClassificationResponse | ProcessingResponse,
    )
    async def classify(
        payload: ClassificationRequest,
        idempotency_key: Annotated[str, Header(alias="Idempotency-Key")],
        active_classifier: Annotated[Classifier, Depends(get_classifier)],
        active_event_store: Annotated[EventStore, Depends(get_event_store)],
    ):
        if idempotency_key != str(payload.event_id):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Idempotency-Key must equal event_id",
            )
        try:
            outcome = await classify_idempotently(
                payload,
                active_classifier,
                active_event_store,
            )
        except EventIdentityConflict as error:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Event ID conflicts with an existing event",
            ) from error
        except (EventStoreUnavailable, EventRecordInvalid) as error:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Classification state is temporarily unavailable",
            ) from error
        if outcome.processing:
            return JSONResponse(
                status_code=status.HTTP_202_ACCEPTED,
                content=outcome.response.model_dump(mode="json"),
            )
        return outcome.response

    @api.get(
        "/v1/classifications/{event_id}",
        response_model=EventStatusResponse,
    )
    async def classification_status(
        event_id: str,
        active_event_store: Annotated[EventStore, Depends(get_event_store)],
    ) -> EventStatusResponse:
        try:
            parsed_event_id = UUID(event_id)
        except ValueError as error:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
                detail="event_id must be a UUID",
            ) from error
        try:
            event = await active_event_store.get(parsed_event_id)
        except (EventStoreUnavailable, EventRecordInvalid) as error:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Classification state is temporarily unavailable",
            ) from error
        if event is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND)
        return event.safe_status()

    return api


app = create_app()
