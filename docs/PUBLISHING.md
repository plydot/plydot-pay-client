# Publishing to Maven Central

Maintainer guide for releasing new versions of `com.plydot:plydot-pay-client`.

---

## Prerequisites

1. **Central Portal account** — https://central.sonatype.com/ (login with `connect@plydot.com` or your org account)
2. **Namespace** — `com.plydot` must be registered and migrated to Central Portal
3. **User token** — Profile → Generate User Token
4. **GPG signing key** — Maven Central requires signed artifacts

---

## Local credentials file

Create `~/.config/plydot/maven-publish.env` (mode `600`):

```bash
MAVEN_CENTRAL_USERNAME=your_token_username
MAVEN_CENTRAL_PASSWORD=your_token_password
GPG_KEY_ID=your_key_id
GPG_PASSPHRASE=your_gpg_passphrase
```

Generate a GPG key if needed:

```bash
gpg --full-generate-key
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

---

## Publish manually

```bash
# bump version in build.gradle.kts first
./scripts/publish.sh
```

Or directly:

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=…
export ORG_GRADLE_PROJECT_mavenCentralPassword=…
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --armor --export-secret-keys KEY_ID)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=…

./gradlew clean build publishToMavenCentral
```

Check deployment at https://central.sonatype.com/ → Deployments.

Artifacts appear on Maven Central search within ~10–30 minutes after release.

---

## Publish via GitHub Actions

1. Add repository secrets:
   - `MAVEN_CENTRAL_USERNAME`
   - `MAVEN_CENTRAL_PASSWORD`
   - `GPG_PRIVATE_KEY` (ASCII-armored private key)
   - `GPG_PASSPHRASE`

2. Tag and push:

```bash
git tag v0.1.1
git push origin v0.1.1
```

The workflow in `.github/workflows/publish.yml` runs `./gradlew publishToMavenCentral`.

---

## Versioning

- Client semver tracks Pay API version (`0.1.x` ↔ `/v1`)
- Bump `version` in `build.gradle.kts` before each release
- Tag format: `v0.1.0`, `v0.1.1`, etc.

---

## Monorepo sync

This repository is the **public home** for the client. The same source also lives in the internal Pay monorepo at `plydot-pay/plydot-pay-client/`. When making changes:

1. Develop in the monorepo or here
2. Keep versions in sync
3. Publish from this repo (or monorepo via `scripts/publish-client.sh`)
