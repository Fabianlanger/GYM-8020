# Releases and signing

## Builds

| What | When | Signed with | Where |
|------|------|-------------|-------|
| Dev build | every push to `main` or `claude/**` | your own key (debug key only without the secrets) | [`releases/download/dev/gym-8020.apk`](https://github.com/Fabianlanger/GYM-8020/releases/download/dev/gym-8020.apk) |
| Version release | pushing a tag `v*` (e.g. `v0.5.0`) | your own release key if the secrets below are set, otherwise the debug key | its own GitHub release |

The debug key is public on purpose: it only makes sure every dev build installs over the previous one.
Anyone could sign an APK with it, so a public version should use your own key.

## Your own key (set up once, November 2026)

The key is set up and stays: **never create a new one**. It lives only in two places, both private:

- GitHub Secrets of this repo: `KEYSTORE_BASE64` (the keystore as base64) and `KEYSTORE_PASSWORD`
  (keystore and key password). Alias `gym8020`. `KEY_ALIAS` and `KEY_PASSWORD` are no longer used.
- The owner's private backup file `gym-8020-sleutel.txt` with the same two values.

CI decodes the key once per build (step "Signing key") and signs **dev builds and versions** with it, so they
install over each other. If the secrets do not open a key with alias `gym8020`, CI falls back to the debug key
and says so in a warning. The package is `nl.fabianlanger.gym8020` (since 1.0; `nl.ibs.gym8020` before).

To restore the secrets from the backup: GitHub → Settings → Secrets and variables → Actions, update the two
secrets with the values from the backup file.

## Publishing a version

```sh
git tag v0.5.0
git push origin v0.5.0
```

CI builds, tests and publishes the release with `gym-8020-v0.5.0.apk`.

Without pushing a tag: **Actions → Build → Run workflow**, fill in the release tag (e.g. `v0.5.0`).
The release then creates the tag itself on the chosen commit.

## Switching from a dev build to a signed release

Android only installs an update when it is signed with the same key. The dev builds use the debug key,
the releases your own key, so the first switch needs a one-time move of your data:

1. In the old app: **Settings → Export all data**; keep the JSON file somewhere safe.
2. Uninstall the old app.
3. Install the signed release.
4. **Settings → Import** and choose the JSON file.

After that, every signed release installs over the previous one.
