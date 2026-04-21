#!/usr/bin/env bash
# Halts App Store Connect phased release for the given marketing version.
# Usage: halt_asc_phased_release.sh <marketing_version>
#
# Requires env vars: APPLE_TEAM_ID, ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_P8_BASE64
#
# Uses App Store Connect API via a JWT. Fetches the current app version and
# patches its phasedRelease state to HOLD. If no phased release is in progress,
# the script is a no-op.
set -euo pipefail

VERSION="${1:?marketing version required}"
: "${ASC_KEY_ID:?}"
: "${ASC_ISSUER_ID:?}"
: "${ASC_KEY_P8_BASE64:?}"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "$ASC_KEY_P8_BASE64" | base64 --decode > "$TMP/key.p8"

NOW=$(date +%s)
EXP=$((NOW + 600))

HEADER=$(jq -nc --arg kid "$ASC_KEY_ID" '{alg:"ES256",kid:$kid,typ:"JWT"}')
PAYLOAD=$(jq -nc --arg iss "$ASC_ISSUER_ID" --argjson iat "$NOW" --argjson exp "$EXP" \
  '{iss:$iss,iat:$iat,exp:$exp,aud:"appstoreconnect-v1"}')

b64url() { openssl base64 -e -A | tr '+/' '-_' | tr -d '='; }
H=$(printf '%s' "$HEADER" | b64url)
P=$(printf '%s' "$PAYLOAD" | b64url)
SIG=$(printf '%s.%s' "$H" "$P" | openssl dgst -sha256 -sign "$TMP/key.p8" -binary | b64url)
TOKEN="$H.$P.$SIG"

API=https://api.appstoreconnect.apple.com/v1

BUNDLE_ID="${APP_BUNDLE_ID:-com.dangerfield.hiittimer.HIITTimer}"
APPS=$(curl -sS -H "Authorization: Bearer $TOKEN" "$API/apps?filter[bundleId]=$BUNDLE_ID&limit=1")
APP_ID=$(echo "$APPS" | jq -r '.data[0].id // empty')
if [ -z "$APP_ID" ]; then
  echo "::error::App not found for bundle $BUNDLE_ID"
  exit 1
fi

VERSIONS=$(curl -sS -H "Authorization: Bearer $TOKEN" \
  "$API/apps/$APP_ID/appStoreVersions?filter[versionString]=$VERSION&limit=1")
VERSION_ID=$(echo "$VERSIONS" | jq -r '.data[0].id // empty')
if [ -z "$VERSION_ID" ]; then
  echo "No App Store version $VERSION found — no phased release to halt."
  exit 0
fi

PHASED=$(curl -sS -H "Authorization: Bearer $TOKEN" \
  "$API/appStoreVersions/$VERSION_ID/appStoreVersionPhasedRelease")
PHASED_ID=$(echo "$PHASED" | jq -r '.data.id // empty')
if [ -z "$PHASED_ID" ]; then
  echo "No phased release object for version $VERSION — nothing to halt."
  exit 0
fi

echo "Halting phased release $PHASED_ID for version $VERSION"
curl -sS -X PATCH \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"data\":{\"type\":\"appStoreVersionPhasedReleases\",\"id\":\"$PHASED_ID\",\"attributes\":{\"phasedReleaseState\":\"PAUSED\"}}}" \
  "$API/appStoreVersionPhasedReleases/$PHASED_ID" | jq '.data.attributes.phasedReleaseState'
