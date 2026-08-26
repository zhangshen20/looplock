import json

from fastapi.testclient import TestClient

from looplock_agent.event_store import InMemoryEventStore
from looplock_agent.main import create_app
from looplock_agent.models import Action, AgentDraft, Classification, ReasonCode


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


class FixtureClassifier:
    async def classify(self, request):
        return AgentDraft(
            action=Action.TIGHTEN,
            classification=Classification.DEMO_GAMBLING_APP,
            confidence=0.98,
            reason_code=ReasonCode.FIXTURE_MATCH,
            reason="The metadata matches the harmless LuckyMirror fixture.",
        )


def client() -> TestClient:
    return TestClient(create_app(FixtureClassifier(), InMemoryEventStore()))


def test_health_does_not_require_model_or_credentials() -> None:
    response = client().get("/healthz")
    cloud_response = client().get("/v1/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "looplock-agent"}
    assert cloud_response.status_code == 200
    assert cloud_response.json() == {"status": "ok", "service": "looplock-agent"}


def test_classification_requires_matching_idempotency_key() -> None:
    response = client().post(
        "/v1/classifications",
        json=REQUEST,
        headers={"Idempotency-Key": "wrong"},
    )

    assert response.status_code == 400


def test_exact_fixture_returns_frozen_response_shape() -> None:
    response = client().post(
        "/v1/classifications",
        json=REQUEST,
        headers={"Idempotency-Key": REQUEST["event_id"]},
    )

    assert response.status_code == 200
    assert response.json()["action"] == "TIGHTEN"
    assert response.json()["target_value"] == REQUEST["target"]["package_name"]
    assert "expires_at" not in response.json()


def test_unknown_fields_and_oversized_requests_are_rejected() -> None:
    with_unknown = json.loads(json.dumps(REQUEST))
    with_unknown["unlock"] = True
    unknown = client().post(
        "/v1/classifications",
        json=with_unknown,
        headers={"Idempotency-Key": REQUEST["event_id"]},
    )
    oversized = client().post(
        "/v1/classifications",
        content=b"x" * 8193,
        headers={
            "Content-Type": "application/json",
            "Idempotency-Key": REQUEST["event_id"],
        },
    )

    assert unknown.status_code == 422
    assert oversized.status_code == 413


def test_label_bound_matches_frozen_request_schema() -> None:
    payload = json.loads(json.dumps(REQUEST))
    payload["target"]["label"] = "x" * 81

    response = client().post(
        "/v1/classifications",
        json=payload,
        headers={"Idempotency-Key": REQUEST["event_id"]},
    )

    assert response.status_code == 422
