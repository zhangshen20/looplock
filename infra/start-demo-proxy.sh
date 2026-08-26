#!/bin/sh
set -eu

if [ -z "${GOOGLE_CLOUD_PROJECT:-}" ]; then
  echo "GOOGLE_CLOUD_PROJECT must be the exact deployed project ID." >&2
  exit 2
fi

CLOUD_RUN_REGION="${LOOPLOCK_CLOUD_RUN_REGION:-australia-southeast1}"

echo "Starting an IAM-authenticated, demo-only local bridge on port 8081." >&2
echo "This is not the production mobile-authentication design." >&2
gcloud run services proxy looplock-agent \
  --project "${GOOGLE_CLOUD_PROJECT}" \
  --region "${CLOUD_RUN_REGION}" \
  --port 8081
