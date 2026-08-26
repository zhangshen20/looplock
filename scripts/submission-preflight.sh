#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${repo_root}"

required_files=(
  README.md
  contracts/classification-request.schema.json
  contracts/classification-response.schema.json
  docs/submission/architecture.md
  docs/submission/architecture.svg
  docs/submission/architecture.png
  docs/submission/architecture.pdf
  docs/submission/demo-script.md
  docs/submission/devpost-draft.md
  docs/submission/evidence-checklist.md
)

for required_file in "${required_files[@]}"; do
  if [[ ! -s "${required_file}" ]]; then
    echo "FAIL: required artifact is missing or empty: ${required_file}" >&2
    exit 1
  fi
done

xmllint --noout docs/submission/architecture.svg

if ! file docs/submission/architecture.png | rg -q 'PNG image data, 1600 x 1000'; then
  echo "FAIL: architecture.png is not the reviewed 1600 x 1000 PNG." >&2
  exit 1
fi

if ! file docs/submission/architecture.pdf | rg -q 'PDF document'; then
  echo "FAIL: architecture.pdf is not a PDF." >&2
  exit 1
fi

bash -n android/scripts/*.sh scripts/*.sh
sh -n infra/*.sh

if rg -n --hidden \
  --glob '!.git/**' \
  --glob '!**/build/**' \
  --glob '!backend/.venv/**' \
  --glob '!backend/.pytest_cache/**' \
  'shen\.zhang@|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|AIza[0-9A-Za-z_-]{30,}|ya29\.[0-9A-Za-z._-]+|gh[pousr]_[0-9A-Za-z]{20,}' .; then
  echo "FAIL: possible personal email or credential material found." >&2
  exit 1
fi

if rg -n \
  'QUERY_ALL_PACKAGES|canRetrieveWindowContent="true"|android\.permission\.(READ_CONTACTS|READ_SMS|RECORD_AUDIO|CAMERA|PACKAGE_USAGE_STATS)' \
  android; then
  echo "FAIL: a forbidden Android permission or accessibility capability was found." >&2
  exit 1
fi

for ignored_path in android/local.properties backend/.venv .env .gcloud/placeholder; do
  if ! git check-ignore --no-index -q "${ignored_path}"; then
    echo "FAIL: sensitive/local path is not ignored: ${ignored_path}" >&2
    exit 1
  fi
done

pending=0
if rg -q 'REPLACE_WITH_REPOSITORY_URL' README.md; then
  echo "PENDING: replace the README repository URL after publication."
  pending=1
fi

if ! rg -q 'https://(www\.)?(youtube\.com|youtu\.be|vimeo\.com)/' docs/submission/devpost-draft.md; then
  echo "PENDING: add the owner-approved public YouTube or Vimeo URL."
  pending=1
fi

if [[ "${LOOPLOCK_FINAL_PUBLICATION_CHECK:-NO}" == "YES" && "${pending}" -ne 0 ]]; then
  echo "FAIL: final publication mode requires repository and video URLs." >&2
  exit 1
fi

echo "PASS: local artifacts, safety boundaries, ignore rules, and credential patterns are publication-ready."
if [[ "${pending}" -ne 0 ]]; then
  echo "Local mode passed; external link gates remain pending."
fi
