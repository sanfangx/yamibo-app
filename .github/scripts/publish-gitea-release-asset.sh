#!/usr/bin/env bash
set -euo pipefail

: "${GITEA_TOKEN:?GITEA_TOKEN is required}"
: "${MIRROR_OWNER:?MIRROR_OWNER is required}"
: "${MIRROR_REPO:?MIRROR_REPO is required}"
: "${TAG:?TAG is required}"
: "${TITLE:?TITLE is required}"
: "${CHANGELOG:?CHANGELOG is required}"
: "${APK:?APK is required}"
: "${APK_NAME:?APK_NAME is required}"

if [ ! -s "$CHANGELOG" ]; then
  echo "Changelog is missing or empty: $CHANGELOG" >&2
  exit 1
fi

gitea_base="${GITEA_BASE_URL:-https://gitea.com}"
gitea_base="${gitea_base%/}"
api="${gitea_base}/api/v1/repos/${MIRROR_OWNER}/${MIRROR_REPO}"
release_url="${gitea_base}/${MIRROR_OWNER}/${MIRROR_REPO}/releases/tag/${TAG}"
fallback_asset_url="${gitea_base}/${MIRROR_OWNER}/${MIRROR_REPO}/releases/download/${TAG}/${APK_NAME}"
encoded_apk_name="$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$APK_NAME")"

auth_header="Authorization: token ${GITEA_TOKEN}"

json_field() {
  local field="$1"
  python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get(sys.argv[1],""))' "$field" 2>/dev/null || true
}

release_json="$(curl -sS -H "$auth_header" "${api}/releases/tags/${TAG}" || true)"
release_id="$(printf '%s' "$release_json" | json_field id)"

if [ -n "$release_id" ]; then
  curl -fsS -X DELETE -H "$auth_header" "${api}/releases/${release_id}" >/dev/null
fi

body_file="$RUNNER_TEMP/gitea-release-body.json"
python3 - "$TAG" "$TITLE" "$CHANGELOG" "$body_file" <<'PY'
import json
import sys
from pathlib import Path

tag, title, changelog, output = sys.argv[1:5]
body = Path(changelog).read_text(encoding="utf-8")
payload = {
    "tag_name": tag,
    "target_commitish": "main",
    "name": title,
    "body": body,
    "draft": False,
    "prerelease": False,
}
Path(output).write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
PY

release_json="$(curl -sS -X POST -H "$auth_header" -H "Content-Type: application/json" \
  --data-binary "@${body_file}" \
  "${api}/releases")"
release_id="$(printf '%s' "$release_json" | json_field id)"

if [ -z "$release_id" ]; then
  echo "Failed to create Gitea release: $release_json" >&2
  exit 1
fi

upload_json="$(curl -sS -X POST -H "$auth_header" \
  -F "attachment=@${APK}" \
  "${api}/releases/${release_id}/assets?name=${encoded_apk_name}")"
release_json="$(curl -sS -H "$auth_header" "${api}/releases/tags/${TAG}" || true)"
asset_url="$(
  {
    printf '%s\n' "$upload_json"
    printf '%s\n' "$release_json"
  } | python3 .github/scripts/extract-release-asset-url.py "$APK_NAME" "$fallback_asset_url"
)"

echo "release_url=$release_url" >> "$GITHUB_OUTPUT"
echo "asset_url=$asset_url" >> "$GITHUB_OUTPUT"
