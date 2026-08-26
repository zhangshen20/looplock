#!/bin/sh
set -eu

if [ "${LOOPLOCK_FIRESTORE_MUTATION_APPROVED:-}" != "YES" ]; then
  echo "Refusing Firestore mutation: set LOOPLOCK_FIRESTORE_MUTATION_APPROVED=YES only after explicit owner approval." >&2
  exit 2
fi

EXPECTED_GOOGLE_CLOUD_PROJECT="looplock-hackathon-2026-v8k3"
if [ "${GOOGLE_CLOUD_PROJECT:-}" != "${EXPECTED_GOOGLE_CLOUD_PROJECT}" ]; then
  echo "Refusing Firestore mutation: GOOGLE_CLOUD_PROJECT must equal ${EXPECTED_GOOGLE_CLOUD_PROJECT}." >&2
  exit 2
fi

CLOUD_RUN_REGION="australia-southeast1"
FIRESTORE_LOCATION="australia-southeast1"
SERVICE_NAME="looplock-agent"
RUNTIME_ACCOUNT_NAME="looplock-agent-runtime"
RUNTIME_ACCOUNT="${RUNTIME_ACCOUNT_NAME}@${GOOGLE_CLOUD_PROJECT}.iam.gserviceaccount.com"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)

gcloud services enable firestore.googleapis.com \
  --project "${GOOGLE_CLOUD_PROJECT}" \
  --quiet

database_config=""
if database_config=$(gcloud firestore databases describe \
  --database="(default)" \
  --project "${GOOGLE_CLOUD_PROJECT}" \
  --format='csv[no-heading](locationId,type,databaseEdition)' \
  --quiet 2>&1); then
  :
else
  case "${database_config}" in
    *NOT_FOUND*) database_config="" ;;
    *)
      echo "Refusing Firestore mutation: database inspection failed." >&2
      echo "${database_config}" >&2
      exit 1
      ;;
  esac
fi

if [ -z "${database_config}" ]; then
  gcloud firestore databases create \
    --database="(default)" \
    --location="${FIRESTORE_LOCATION}" \
    --type=firestore-native \
    --project "${GOOGLE_CLOUD_PROJECT}" \
    --quiet
  database_config=$(gcloud firestore databases describe \
    --database="(default)" \
    --project "${GOOGLE_CLOUD_PROJECT}" \
    --format='csv[no-heading](locationId,type,databaseEdition)' \
    --quiet)
fi

expected_database_config="${FIRESTORE_LOCATION},FIRESTORE_NATIVE,STANDARD"
if [ "${database_config}" != "${expected_database_config}" ]; then
  echo "Refusing Firestore mutation: expected ${expected_database_config}, got ${database_config}." >&2
  exit 1
fi

gcloud projects add-iam-policy-binding "${GOOGLE_CLOUD_PROJECT}" \
  --member "serviceAccount:${RUNTIME_ACCOUNT}" \
  --role "roles/datastore.user" \
  --condition=None \
  --quiet

gcloud run deploy "${SERVICE_NAME}" \
  --source "${PROJECT_ROOT}/backend" \
  --project "${GOOGLE_CLOUD_PROJECT}" \
  --region "${CLOUD_RUN_REGION}" \
  --service-account "${RUNTIME_ACCOUNT}" \
  --no-allow-unauthenticated \
  --set-env-vars "GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT},GOOGLE_CLOUD_LOCATION=global,GOOGLE_GENAI_USE_VERTEXAI=TRUE,ADK_CAPTURE_MESSAGE_CONTENT_IN_SPANS=false,LOOPLOCK_FIRESTORE_DATABASE=(default),LOOPLOCK_FIRESTORE_COLLECTION=classification_events,LOOPLOCK_PROCESSING_LEASE_SECONDS=60" \
  --min-instances 0 \
  --max-instances 1 \
  --concurrency 4 \
  --memory 512Mi \
  --timeout 60 \
  --quiet

echo "Private Firestore-enabled demo revision deployed. Verify one minimal document before marking item 7 complete." >&2
