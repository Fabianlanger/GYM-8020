# Releases and signing

## Builds

| What | When | Signed with | Where |
|------|------|-------------|-------|
| Dev build | every push to `main` or `claude/**` | debug key (`app/debug.keystore`, in the repo) | [`releases/download/dev/gym-8020.apk`](https://github.com/Fabianlanger/GYM-8020/releases/download/dev/gym-8020.apk) |
| Version release | pushing a tag `v*` (e.g. `v0.5.0`) | your own release key if the secrets below are set, otherwise the debug key | its own GitHub release |

The debug key is public on purpose: it only makes sure every dev build installs over the previous one.
Anyone could sign an APK with it, so a public version should use your own key.

## Setting up your own release key (once)

1. Create a key on your computer (needs a JDK; `keytool` is part of it):

   ```sh
   keytool -genkeypair -v -keystore gym-8020-release.jks -alias gym8020 \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

   Choose a strong password. **Keep the `.jks` file and the password safe** (password manager plus a backup).
   If you lose them you can never publish an update that installs over existing installations.

2. Turn the key into text:

   ```sh
   base64 -w0 gym-8020-release.jks > keystore.txt      # Linux
   base64 -i gym-8020-release.jks -o keystore.txt      # macOS
   ```

3. In GitHub: **Settings → Secrets and variables → Actions → New repository secret**, add:

   | Secret | Value |
   |--------|-------|
   | `KEYSTORE_BASE64` | the contents of `keystore.txt` |
   | `KEYSTORE_PASSWORD` | the keystore password |
   | `KEY_ALIAS` | `gym8020` (or the alias you chose) |
   | `KEY_PASSWORD` | the key password (by default the same as the keystore password) |

   Delete `keystore.txt` afterwards. Never commit the `.jks` file.

## Publishing a version

```sh
git tag v0.5.0
git push origin v0.5.0
```

CI builds, tests and publishes the release with `gym-8020-v0.5.0.apk`.

## Switching from a dev build to a signed release

Android only installs an update when it is signed with the same key. The dev builds use the debug key,
the releases your own key, so the first switch needs a one-time move of your data:

1. In the old app: **Settings → Export all data**; keep the JSON file somewhere safe.
2. Uninstall the old app.
3. Install the signed release.
4. **Settings → Import** and choose the JSON file.

After that, every signed release installs over the previous one.
