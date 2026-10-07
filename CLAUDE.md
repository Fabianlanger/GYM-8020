# GYM-8020

Android-app voor krachttraining (Kotlin, Jetpack Compose, Room). Lokaal, geen server. Communicatie met de
eigenaar in het **Nederlands**.

**Volg altijd de werkwijze in `.claude/skills/werkwijze/SKILL.md`**: input vastleggen in `docs/BACKLOG.md` en
`docs/SPEC.md`, releases indelen in `docs/ROADMAP.md` met genummerde vragen per release, afhankelijkheden
checken, bugs test-first.

Snel:
- `core/` = pure Kotlin-logica met tests: `./gradlew -PcoreOnly :core:test`.
- `app/` = Android; bouwt alleen in GitHub Actions (Google Maven is hier niet bereikbaar).
- Tot 1.0 alleen dev-builds (vaste dev-link); versiereleases pas bij 1.0.
- Geen persoonlijke trainingsdata in de repo.

## Agent skills

### Issue tracker

Issues en specs staan in GitHub Issues van Fabianlanger/GYM-8020. Zie `docs/agents/issue-tracker.md`.

### Triage labels

De vijf standaardrollen, label = naam (needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix). Zie `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` en `docs/adr/` in de hoofdmap. Zie `docs/agents/domain.md`.
