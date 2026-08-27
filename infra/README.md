# LoopLock demo cloud deployment

This directory describes **demo-only infrastructure** supporting the application-ready beta. It is not the public mobile-authentication design required for general availability.

## Current state

- The backend is implemented, locally testable, and deployed privately in project `looplock-hackathon-2026-v8k3`.
- Cloud Run revision `looplock-agent-00004-ztf` is ready in `australia-southeast1` and receives 100% of traffic.
- Billing is linked and the hackathon credit is available. The runtime identity has only `roles/aiplatform.user` and `roles/datastore.user` beyond Google-managed service-agent bindings.
- The Standard Native-mode `(default)` Firestore database is active in `australia-southeast1`. No public service access, service-account key, or APK credential exists.

## Intended bounded deployment

The guarded scripts deploy one private Cloud Run service in `australia-southeast1`. It uses a dedicated keyless runtime service account, Vertex AI Gemini 3.5 Flash, Standard Firestore, zero minimum instances, one maximum instance, concurrency four, 512 MiB memory, and a 60-second request timeout.

The Android APK never receives Google credentials. During the controlled demo, an operator runs an IAM-authenticated local Cloud Run proxy and uses `adb reverse`; that bridge is explicitly demo-only.

## Approval gate for a future base redeploy

Review `deploy-demo.sh` before any future redeploy. It requires both values below and intentionally refuses to run without a fresh approval flag:

```sh
export GOOGLE_CLOUD_PROJECT="your-project-id"
export LOOPLOCK_CLOUD_MUTATION_APPROVED="YES"
./infra/deploy-demo.sh
```

The script enables required APIs, creates the dedicated runtime service account if absent, grants only `roles/aiplatform.user` to that identity, builds from `backend/`, and deploys a private service. These are real cloud mutations and may incur charges.

After deployment, an authenticated operator can start the demo-only bridge:

```sh
GOOGLE_CLOUD_PROJECT="your-project-id" ./infra/start-demo-proxy.sh
adb reverse tcp:8081 tcp:8081
```

Then verify `http://127.0.0.1:8081/v1/health`. Cloud Run's front end reserves `/healthz`, so that local-only route is retained for container checks while `/v1/health` is the deployed verification route. Classification proof must use only the repository-owned harmless fixture metadata and must not record raw user activity.

## Manual controls before a future deployment

- Confirm the selected project and active billing account in the Google Cloud console.
- Create a low budget alert; an alert is not a hard spending cap.
- Confirm the deploying user is allowed to enable services, create the service account, grant its Vertex role, build, and deploy Cloud Run.
- Keep Cloud Run unauthenticated access disabled.
- Capture sanitized proof of the service URL, revision, privacy setting, runtime identity, and resource limits for the submission evidence pack.

## Item 7 Firestore gate

`enable-firestore-demo.sh` is a separate mutation because database location cannot be casually changed and the runtime gains a new data role. It refuses to run without an item-specific approval flag. After reviewing the exact actions and receiving owner approval:

```sh
export GOOGLE_CLOUD_PROJECT="looplock-hackathon-2026-v8k3"
export LOOPLOCK_FIRESTORE_MUTATION_APPROVED="YES"
./infra/enable-firestore-demo.sh
```

The script will enable the Firestore API, create the Standard Native-mode `(default)` database in `australia-southeast1` if absent, grant the existing runtime identity `roles/datastore.user`, and deploy the private backend revision with the same Cloud Run caps. It does not create keys, public access, Firestore mobile/web credentials, Firebase Authentication, or an APK database path.

Verification sent the same harmless fixture event three times through the authenticated proxy. The database contains one terminal `classification_events/{event_id}` document with only opaque UUIDs, a SHA-256 target hash, bounded result fields, and timestamps. Processing-only lease fields are deleted on completion. Raw package metadata, account identity, prompts, and model reasoning are absent.
