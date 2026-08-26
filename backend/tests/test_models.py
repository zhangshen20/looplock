import json
from pathlib import Path

import pytest
from jsonschema import Draft202012Validator, FormatChecker
from pydantic import ValidationError

from looplock_agent.models import (
    Action,
    AgentDraft,
    Classification,
    ClassificationRequest,
    ClassificationResponse,
    ReasonCode,
)


CONTRACTS = Path(__file__).parents[2] / "contracts"


def load(relative_path: str) -> dict:
    return json.loads((CONTRACTS / relative_path).read_text())


def test_frozen_request_round_trips_through_strict_model() -> None:
    request = ClassificationRequest.model_validate(load("fixtures/valid-request.json"))

    assert request.target.package_name == "com.histopgambling.fixture.luckymirror"
    assert request.model_dump(mode="json") == load("fixtures/valid-request.json")


def test_unknown_request_fields_are_rejected() -> None:
    raw = load("fixtures/valid-request.json")
    raw["account_id"] = "must-not-exist"

    with pytest.raises(ValidationError):
        ClassificationRequest.model_validate(raw)


def test_response_model_matches_the_frozen_json_schema() -> None:
    response = ClassificationResponse.model_validate(load("fixtures/valid-tighten-response.json"))
    validator = Draft202012Validator(
        load("classification-response.schema.json"),
        format_checker=FormatChecker(),
    )

    validator.validate(response.model_dump(mode="json"))


def test_review_draft_cannot_claim_an_exact_fixture_match() -> None:
    with pytest.raises(ValidationError):
        AgentDraft(
            action=Action.REVIEW,
            classification=Classification.DEMO_GAMBLING_APP,
            confidence=0.5,
            reason_code=ReasonCode.FIXTURE_MATCH,
            reason="Contradictory review result.",
        )
