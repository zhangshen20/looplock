#!/bin/sh
set -eu

if [ "${LOOPLOCK_CLOUD_MUTATION_APPROVED:-}" != "YES" ]; then
  echo "Refusing cloud mutation: set LOOPLOCK_CLOUD_MUTATION_APPROVED=YES only after explicit owner approval." >&2
  exit 2
fi

if [ -z "${GOOGLE_CLOUD_PROJECT:-}" ]; then
  echo "Refusing cloud mutation: GOOGLE_CLOUD_PROJECT must be the exact approved project ID." >&2
  exit 2
fi

CLOUD_RUN_REGION="${LOOPLOCK_CLOUD_RUN_REGION:-australia-southeast1}"
SERVICE_NAME="looplock-agent"
RUNTIME_ACCOUNT_NAME="looplock-agent-runtime"
RUNTIME_ACCOUNT="${RUNTIME_ACCOUNT_NAME}@${GOOGLE_CLOUD_PROJECT}.iam.gserviceaccount.com"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)

gcloud services enable \
  aiplatform.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  iam.googleapis.com \
  run.googleapis.com \
  --project "${GOOGLE_CLOUD_PROJECT}"

if ! gcloud iam service-accounts describe "${RUNTIME_ACCOUNT}" --project "${GOOGLE_CLOUD_PROJECT}" >/dev/null 2>&1; then
  gcloud iam service-accounts create "${RUNTIME_ACCOUNT_NAME}" \
    --display-name "LoopLock demo agent runtime" \
    --project "${GOOGLE_CLOUD_PROJECT}"
fi

gcloud projects add-iam-policy-binding "${GOOGLE_CLOUD_PROJECT}" \
  --member "serviceAccount:${RUNTIME_ACCOUNT}" \
  --role "roles/aiplatform.user" \
  --condition=None

gcloud run deploy "${SERVICE_NAME}" \
  --source "${PROJECT_ROOT}/backend" \
  --project "${GOOGLE_CLOUD_PROJECT}" \
  --region "${CLOUD_RUN_REGION}" \
  --service-account "${RUNTIME_ACCOUNT}" \
  --no-allow-unauthenticated \
  --set-env-vars "GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT},GOOGLE_CLOUD_LOCATION=global,GOOGLE_GENAI_USE_VERTEXAI=TRUE" \
  --min-instances 0 \
  --max-instances 1 \
  --concurrency 4 \
  --memory 512Mi \
  --timeout 60

echo "Private demo service deployed. Do not place credentials in the APK; use the authenticated demo proxy." >&2
