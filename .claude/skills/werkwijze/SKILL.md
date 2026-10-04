---
name: werkwijze
description: Werkwijze voor GYM-8020. Gebruik bij elk nieuw verzoek, elke bug, feature-idee, vraag over planning of release, en voordat je code schrijft. Beschrijft hoe input wordt vastgelegd (SPEC, BACKLOG, ROADMAP), hoe releases worden ingedeeld met open vragen per release, de check op afhankelijkheden, test-first bij bugs, en hoe gebouwd en opgeleverd wordt.
---

# Werkwijze GYM-8020

Afspraken tussen de eigenaar en Claude over hoe we samen aan deze app werken. Bij twijfel gaat dit document
voor op gewoonte.

## Communicatie

- **Nederlands**, gewone taal, kort. Geen jargon zonder uitleg; de eigenaar is geen Android-ontwikkelaar.
- Spraakherkenning geeft soms losse woorden ("Nederlands", "Oké"): negeren.
- **Vragen zijn genummerd en hebben een advies**, zodat de eigenaar kort kan antwoorden ("1 ja, 2 nee").
- Eerst antwoorden op een directe vraag, dan pas de rest.

## Documenten (alles in `docs/`)

| Bestand | Waarvoor |
|---|---|
| `SPEC.md` | Besluiten (D-nummers) en voortgang per fase. Alles wat besloten is staat hier. |
| `BACKLOG.md` | Alle punten met uitwerking: wat, oorzaak, voorstel, te besluiten, test. |
| `ROADMAP.md` | Welke punten in welke release, de open vragen per release, en de afhankelijkheden (A-nummers). |
| `TESTING.md` | Teststrategie, risicolijst met kritieke flows (F-nummers). |
| `onderzoek-*.md` | Onderzoek met bronnen, eindigend met beslispunten. |
| `methode.md`, `DESIGN.md`, `RELEASE.md` | De methode, de vormgeving, releases en ondertekening. |

Nummering: **B** bug · **V** verbetering · **O** onderzoek/ontwerp · **T** testen · **W** wearables ·
**A** architectuur/afhankelijkheid · **F** kritieke flow · **D** besluit · **R** onderzoeksvraag · **P** proces.
Nummers worden nooit hergebruikt; een punt dat van soort verandert, houdt een verwijzing ("was B-1").

## 1. Nieuwe input vastleggen

Bij elke bug, wens of idee van de eigenaar:
1. **Uitzoeken vóór vastleggen**: lees de code, zoek de oorzaak. "Oorzaak (gevonden)" alleen als het zeker is,
   anders "waarschijnlijke oorzaak".
2. Zet het in `BACKLOG.md`: wat, oorzaak, voorstel, te besluiten, test.
3. Een nieuwe feature of principe ook als **besluit in `SPEC.md`** (D-nummer, status BESLOTEN, LATER of
   ONDERZOEK).
4. **Deel het in in `ROADMAP.md`**, met de open vragen bij de release waar ze nodig zijn.
5. Niet bouwen tenzij de eigenaar dat vraagt. Vastleggen en indelen is de standaard.

## 2. Releases indelen

- Elke release heeft een **doel in één zin**, een lijst punten en de **genummerde vragen die vóór die release
  beantwoord moeten zijn**.
- Snelle fixes en kleine dingen gaan in een kleine tussenrelease (0.x.y). Samenhangende features gaan samen
  in één release (0.x).
- Tot 1.0 alleen **dev-builds** (D-85). De eigenaar test via de vaste dev-link.

## 3. Afhankelijkheden checken (bij elke nieuwe feature)

Voordat iets wordt ingedeeld of gebouwd, kijk vooruit:
- **Database:** vraagt dit of een latere feature een nieuw veld? Bundel migraties (A-1). Kies velden die latere
  features ook dienen (bijv. een keuzelijst in plaats van ja/nee, A-2).
- **Rekenlogica:** hoort dit in het gedeelde krachtmodel of een andere centrale plek in `core` (A-3)? Nooit
  dezelfde formule op twee plekken.
- **Gegevens van buiten** (wearables, import): zijn oefeningscodes, tijdstempels en eenheden bruikbaar (A-5, A-6)?
- **Back-up:** groeit het back-upformaat mee, en blijven oude back-ups inleesbaar (A-7)?
- **Onomkeerbare keuzes** (ondertekening, pakketnaam, datamodel van gyms): eerst aan de eigenaar voorleggen (A-4, A-8).

Nieuwe afhankelijkheden komen in de tabel bovenaan `ROADMAP.md`.

## 4. Bouwen

- **Bugs: eerst een test die de bug nabootst en faalt, dan de fix** (TESTING.md §1). De test heet naar het
  backlognummer. Kan het (nog) niet automatisch, zet dat in de backlog en op de handmatige checklist.
- **Regels** testen naast voorbeelden (property-tests, TESTING.md §2).
- Logica zoveel mogelijk in `core/` (puur Kotlin). Lokaal testen met `./gradlew -PcoreOnly :core:test`.
  De Android-app bouwt alleen in GitHub Actions; deze omgeving kan Google's Maven niet bereiken.
- Strings altijd in het **Engels én Nederlands** (`values/` en `values-nl/`).
- Geen persoonlijke trainingsdata in de repo, tests of screenshots (D-06a).
- Na een databasewijziging: migratietest, schema-JSON committen (CI print nieuwe schema's) en back-upformaat
  bijwerken.

## 5. Opleveren

1. Lokaal: core-tests groen.
2. Commit met een duidelijke beschrijving; push naar de werkbranch.
3. Wacht op CI (GitHub Actions). Pas bij groen melden dat het klaar is. Rood: oorzaak zoeken en fixen, niet
   melden als klaar.
4. Docs bijwerken: SPEC-voortgang, BACKLOG-status ("gedaan in …"), ROADMAP, TESTING-risicolijst.
5. Melding aan de eigenaar:
   - wat er veranderd is, in gewone taal;
   - wat handmatig nagelopen moet worden;
   - welke vragen er nog openstaan, genummerd en met advies.
