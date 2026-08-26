import json
from pathlib import Path

import pytest
from jsonschema import Draft202012Validator, FormatChecker


CONTRACTS = Path(__file__).parents[2] / "contracts"


def load(relative_path: str) -> dict:
    return json.loads((CONTRACTS / relative_path).read_text())


@pytest.mark.parametrize(
    "fixture",
    ["fixtures/valid-tighten-response.json", "fixtures/valid-review-response.json"],
)
def test_valid_responses_match_closed_schema(fixture: str) -> None:
    validator = Draft202012Validator(
        load("classification-response.schema.json"),
        format_checker=FormatChecker(),
    )
    validator.validate(load(fixture))


@pytest.mark.parametrize(
    "fixture",
    ["fixtures/invalid-unlock-response.json", "fixtures/invalid-expiry-response.json"],
)
def test_weakening_or_authority_expanding_responses_are_rejected(fixture: str) -> None:
    validator = Draft202012Validator(
        load("classification-response.schema.json"),
        format_checker=FormatChecker(),
    )
    assert list(validator.iter_errors(load(fixture)))


def test_valid_request_matches_closed_schema() -> None:
    validator = Draft202012Validator(
        load("classification-request.schema.json"),
        format_checker=FormatChecker(),
    )
    validator.validate(load("fixtures/valid-request.json"))

