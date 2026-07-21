# Publishing to Maven Central

Maintainer guide for releasing new versions of `com.plydot:plydot-pay-client`.

---

## Automatic releases (default)

Every **push or merge to `main`** publishes a new patch version to Maven Central.

| Event | Publishes? |
|-------|------------|
| Push / merge to `main` | Yes — bumps patch (`0.1.0` → `0.1.1`) and publishes |
| Push to any other branch | No |
| Commit message contains `[skip publish]` | No (used by the release bot itself) |

Workflow: [`.github/workflows/publish.yml`](../.github/workflows/publish.yml)

What it does:

1. Bumps the patch version in `build.gradle.kts` (uses the higher of file version vs latest `v*` tag)
2. Runs tests
3. Publishes to Maven Central (Central Portal, automatic release)
4. Commits the version bump with `[skip publish]` and pushes tag `vX.Y.Z`

### Required GitHub secrets

| Secret | Purpose |
|--------|---------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user token password |
| `GPG_PRIVATE_KEY` | ASCII-armored private signing key |
| `GPG_PASSPHRASE` | GPG key passphrase |

Create the token at https://central.sonatype.com/ → Profile → Generate User Token.

---

## Manual / local publish

Create `~/.config/plydot/maven-publish.env` (mode `600`):

```bash
MAVEN_CENTRAL_USERNAME=your_token_username
MAVEN_CENTRAL_PASSWORD=your_token_password
GPG_KEY_ID=your_key_id
GPG_PASSPHRASE=your_gpg_passphrase
```

Bump `version` in `build.gradle.kts`, then:

```bash
./scripts/publish.sh
```

---

## Versioning

- Client semver tracks Pay API major/minor (`0.1.x` ↔ `/v1`)
- **Patch** versions are auto-bumped on every `main` publish
- For a minor/major bump, edit `version` in `build.gradle.kts` on `main` before merging (or bump and push), e.g. `0.2.0` — the next auto release will continue from there (`0.2.1`, …)
- Tags: `v0.1.0`, `v0.1.1`, …

To skip publishing for a docs-only or chore commit on `main`:

```bash
git commit -m "docs: fix typo [skip publish]"
```

---

## Monorepo sync

This repository is the **public home** for the client. The same source also lives in the internal Pay monorepo at `plydot-pay/plydot-pay-client/`. When making changes:

1. Develop in the monorepo or here
2. Keep sources in sync
3. Publish from **this** repo via merges to `main`
