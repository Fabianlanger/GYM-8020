# GYM-8020

Android app for strength training with a fixed method: per exercise a **heavy** set (aim for ~6 reps) and a **light** set (aim for ~16 reps), straight after each other and both to failure. After each exercise you set the **weight for next time**, so you keep pushing yourself.

*Ontwikkeld onder de vlag van Isatis Business Solutions.*

## Features (v0.1)

- Standard library of ~55 exercises with muscles, plus your own exercises
- Schemas and week plans (Full body, Push/Pull/Legs, Upper/Lower) with day recognition on the home screen
- Training flow: optional warm-up, **GO** → 3-2-1 → exercise timer, swipe to the next exercise
- Smart goal for next time (too light / good / too heavy), plan sets for next time
- Body weight log and effective weight for bodyweight exercises (assisted = negative extra)
- Export and import of all data as one JSON file
- English and Dutch

## Installing

1. Download the APK from the latest build (GitHub Actions → *Build* → artifact `gym-8020-debug-apk`), later from GitHub Releases.
2. On your phone, allow installing apps from unknown sources for your browser or file manager.
3. Open the APK. Updates install over the previous version and keep your data.

> **All data is stored only on your phone.** Uninstalling the app deletes it, unless you have made an export (Settings → Export all data).

## Development

- Kotlin, Jetpack Compose (Material 3), Room, DataStore. Min. Android 8.0.
- `core/` holds the pure-Kotlin logic (goals, effective weight, e1RM, day recognition, library, backup format) with unit tests: `./gradlew :core:test` (add `-PcoreOnly` to build without the Android SDK).
- `app/` is the Android app. `./gradlew :app:assembleDebug`
- The debug build is signed with the committed `app/debug.keystore`, so every build installs over the previous one. Release signing (v0.5) uses a key from GitHub Secrets.

Docs: [`docs/SPEC.md`](docs/SPEC.md) (specification and decisions), [`docs/DESIGN.md`](docs/DESIGN.md) (visual style), `docs/methode.md` (the method, coming).

## License

MIT, see [LICENSE](LICENSE). Poppins font: SIL Open Font License ([assets/fonts/Poppins-OFL.txt](assets/fonts/Poppins-OFL.txt)).
