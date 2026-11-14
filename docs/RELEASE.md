# Releases and signing

## Builds

| What | When | Signed with | Where |
|------|------|-------------|-------|
| Dev build | every push to `main` or `claude/**` | your own key (debug key only without the secrets) | [`releases/download/dev/gym-8020.apk`](https://github.com/Fabianlanger/GYM-8020/releases/download/dev/gym-8020.apk) |
| Version release | a tag `v*` (e.g. `v1.2.0`), only after the owner's "go" (see below) | your own release key if the secrets below are set, otherwise the debug key | its own GitHub release, text from `CHANGELOG.md` |

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

## Version numbers (#88)

- A version is a tag `vX.Y.Z`. **Minor** (1.1.0, 1.2.0, …) for a release with new features; **patch** (1.0.1, …)
  for fixes only.
- The app shows the number under "About". A version build is `1.2.0`; a dev build is the last version plus
  `-dev`, e.g. `1.2.0-dev (build 140)`. The build number is the CI run number and is also the `versionCode`, so
  every build is an update of the previous one.
- `app/build.gradle.kts` (`versionNameFor`) works this out: the release tag from the workflow (`RELEASE_TAG`),
  else the newest `v*` tag from `git describe`. CI checks out the full history with tags (`fetch-depth: 0`);
  without tags the name falls back to a fixed version in that function.

## Changelog

`CHANGELOG.md` has one section per version (`## 1.2.0`), in plain language, English and Dutch, written from the
closed tickets of the release. New work goes under `## Unreleased / Nog niet uitgebracht`. The GitHub release of a
version gets the text of its section; when the section is missing, CI falls back to GitHub's generated notes and
warns.

## Publishing a version

**Never a version without a "go" from the owner.** The dev link stays next to the versions (same key).

1. The owner says "release".
2. Claude checks that everything is green: core tests, CI on `main`, no open tickets of the milestone left.
3. Claude writes the changelog: renames `Unreleased` to the new version (`## 1.2.0`, Dutch and English), commits it
   to `main` and shows the owner the text and the version number.
4. After the owner's "go", the version is made on that commit:
   - **Actions → Build → Run workflow** on `main`, release tag e.g. `v1.2.0`. The run creates the tag itself.
     The owner starts this run; Claude may start it when it has the rights to (the GitHub tools can trigger a
     workflow run).
   - Or push a tag: `git tag v1.2.0 && git push origin v1.2.0`.
5. CI builds, tests and publishes the release `GYM-8020 v1.2.0` with `gym-8020-v1.2.0.apk` and the changelog text.
   Claude checks the release page and the version under "About".

## Switching from a dev build to a signed release

Android only installs an update when it is signed with the same key. The dev builds use the debug key,
the releases your own key, so the first switch needs a one-time move of your data:

1. In the old app: **Settings → Export all data**; keep the JSON file somewhere safe.
2. Uninstall the old app.
3. Install the signed release.
4. **Settings → Import** and choose the JSON file.

After that, every signed release installs over the previous one.
