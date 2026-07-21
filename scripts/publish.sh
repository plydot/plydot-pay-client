#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CREDS="${MAVEN_PUBLISH_ENV:-$HOME/.config/plydot/maven-publish.env}"

if [[ ! -f "$CREDS" ]]; then
  echo "Missing credentials file: $CREDS" >&2
  echo "Create it with MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD, GPG_PASSPHRASE, GPG_KEY_ID" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "$CREDS"

: "${MAVEN_CENTRAL_USERNAME:?MAVEN_CENTRAL_USERNAME required}"
: "${MAVEN_CENTRAL_PASSWORD:?MAVEN_CENTRAL_PASSWORD required}"
: "${GPG_PASSPHRASE:?GPG_PASSPHRASE required}"
: "${GPG_KEY_ID:?GPG_KEY_ID required}"

export ORG_GRADLE_PROJECT_mavenCentralUsername="$MAVEN_CENTRAL_USERNAME"
export ORG_GRADLE_PROJECT_mavenCentralPassword="$MAVEN_CENTRAL_PASSWORD"
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(
  echo "$GPG_PASSPHRASE" | gpg --batch --pinentry-mode loopback --passphrase-fd 0 \
    --armor --export-secret-keys "$GPG_KEY_ID" 2>/dev/null
)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$GPG_PASSPHRASE"

cd "$ROOT"
./gradlew clean build publishToMavenCentral --no-daemon

echo "Published. Check deployment status at https://central.sonatype.com/"
