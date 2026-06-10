#!/usr/bin/env bash
set -euo pipefail

: "${GITCODE_TOKEN:?GITCODE_TOKEN is required}"
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

body="$(cat "$CHANGELOG")"
api="https://gitcode.com/api/v5/repos/${MIRROR_OWNER}/${MIRROR_REPO}"
release_url="https://gitcode.com/${MIRROR_OWNER}/${MIRROR_REPO}/releases/tag/${TAG}"
fallback_asset_url="https://gitcode.com/${MIRROR_OWNER}/${MIRROR_REPO}/releases/download/${TAG}/${APK_NAME}"

json_field() {
  local field="$1"
  python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get(sys.argv[1],""))' "$field" 2>/dev/null || true
}

release_json="$(curl -sS -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/tags/${TAG}" || true)"
release_id="$(printf '%s' "$release_json" | json_field id)"

if [ -n "$release_id" ]; then
  curl -fsS -X DELETE -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/${release_id}" >/dev/null || true
  curl -fsS -X DELETE -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/${TAG}" >/dev/null || true
fi

release_json="$(curl -sS -X POST -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases" \
  -F "tag_name=${TAG}" \
  -F "name=${TITLE}" \
  -F "body=${body}" \
  -F "description=${body}")"
release_id="$(printf '%s' "$release_json" | json_field id)"

if [ -z "$release_id" ]; then
  echo "Failed to create GitCode release: $release_json" >&2
  exit 1
fi

upload_json="$(curl -sS -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/${TAG}/upload_url")"
upload_url="$(printf '%s' "$upload_json" | python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get("upload_url") or data.get("url") or "")')"
if [ -z "$upload_url" ]; then
  echo "Failed to get GitCode release upload URL: $upload_json" >&2
  exit 1
fi

upload_result="$(curl -sS -X POST -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" \
  -F "file=@${APK}" \
  "$upload_url")"
release_json="$(curl -sS -H "PRIVATE-TOKEN: ${GITCODE_TOKEN}" "${api}/releases/tags/${TAG}" || true)"
asset_url="$(
  {
    printf '%s\n' "$upload_json"
    printf '%s\n' "$upload_result"
    printf '%s\n' "$release_json"
  } | python3 .github/scripts/extract-release-asset-url.py "$APK_NAME" "$fallback_asset_url"
)"

echo "release_url=$release_url" >> "$GITHUB_OUTPUT"
echo "asset_url=$asset_url" >> "$GITHUB_OUTPUT"
