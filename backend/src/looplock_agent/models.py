from enum import StrEnum
from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, StringConstraints, model_validator


PackageName = Annotated[
    str,
    StringConstraints(
        min_length=3,
        max_length=255,
        pattern=r"^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$",
    ),
]


class ClosedModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Classification(StrEnum):
    DEMO_GAMBLING_APP = "DEMO_GAMBLING_APP"
    UNKNOWN = "UNKNOWN"


class Action(StrEnum):
    TIGHTEN = "TIGHTEN"
    REVIEW = "REVIEW"


class ReasonCode(StrEnum):
    FIXTURE_MATCH = "FIXTURE_MATCH"
    NEEDS_REVIEW = "NEEDS_REVIEW"
    MODEL_UNAVAILABLE = "MODEL_UNAVAILABLE"
    INVALID_MODEL_OUTPUT = "INVALID_MODEL_OUTPUT"


class ClassificationTarget(ClosedModel):
    type: Literal["PACKAGE"]
    package_name: PackageName
    label: Annotated[str, StringConstraints(min_length=1, max_length=80)]
    version_code: int = Field(ge=1)


class ClassificationRequest(ClosedModel):
    schema_version: Literal[1]
    event_id: UUID
    commitment_id: UUID
    target: ClassificationTarget


class AgentDraft(ClosedModel):
    action: Action
    classification: Classification
    confidence: float = Field(ge=0, le=1)
    reason_code: ReasonCode
    reason: Annotated[str, StringConstraints(min_length=1, max_length=240)]

    @model_validator(mode="after")
    def semantic_closure(self) -> "AgentDraft":
        if self.action is Action.TIGHTEN and (
            self.classification is not Classification.DEMO_GAMBLING_APP
            or self.reason_code is not ReasonCode.FIXTURE_MATCH
        ):
            raise ValueError("TIGHTEN requires the exact fixture classification")
        if self.action is Action.REVIEW and (
            self.classification is not Classification.UNKNOWN
            or self.reason_code is ReasonCode.FIXTURE_MATCH
        ):
            raise ValueError("REVIEW requires an unknown classification and review reason")
        return self


class ClassificationResponse(ClosedModel):
    schema_version: Literal[1]
    event_id: UUID
    commitment_id: UUID
    action: Action
    target_type: Literal["PACKAGE"]
    target_value: PackageName
    classification: Classification
    confidence: float = Field(ge=0, le=1)
    reason_code: ReasonCode
    reason: Annotated[str, StringConstraints(min_length=1, max_length=240)]


class ProcessingResponse(ClosedModel):
    status: Literal["processing"] = "processing"
    event_id: UUID


class EventStatusResponse(ClosedModel):
    event_id: UUID
    commitment_id: UUID
    status: Literal["PROCESSING", "TIGHTEN", "REVIEW"]
    classification: Classification | None = None
    confidence: float | None = Field(default=None, ge=0, le=1)
    reason_code: ReasonCode | None = None


class HealthResponse(ClosedModel):
    status: Literal["ok"] = "ok"
    service: Literal["looplock-agent"] = "looplock-agent"
