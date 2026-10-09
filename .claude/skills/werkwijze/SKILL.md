---
name: werkwijze
description: Werkwijze voor GYM-8020. Gebruik bij elk nieuw verzoek, elke bug, feature-idee, vraag over planning of release, en voordat je code schrijft. Zegt welke skill van Matt Pocock je per situatie gebruikt, waar wat staat (GitHub Issues, epics, mijlpalen, GLOSSARY, ADR), de check op afhankelijkheden, de tussenweg bij testen, en hoe je oplevert.
---

# Werkwijze GYM-8020

Afspraken tussen de eigenaar en Claude. De skills van Matt Pocock doen het meeste werk; dit document zegt
welke je wanneer gebruikt, en legt vast wat alleen voor deze repo geldt. Bij twijfel gaat dit document voor.

## Communicatie

- **Nederlands**, gewone taal, kort. De eigenaar is geen Android-ontwikkelaar: leg elk vakwoord uit.
- Spraakherkenning geeft soms losse woorden ("Nederlands", "Oké"): negeren.
- Vragen gaan in rondes zoals `grilling` ze voorschrijft: genummerd, elk met een advies.
- Eerst antwoorden op een directe vraag, dan pas de rest.

## Waar wat staat

| Wat | Waar |
|---|---|
| Werk, bugs, vragen | GitHub Issues (`docs/agents/issue-tracker.md`), labels volgens `docs/agents/triage-labels.md` |
| Groot doel | Issue met label `epic`, de tickets eronder als sub-issues |
| Release | Mijlpaal, één epic per release. Overzicht: `docs/ROADMAP.md` |
| Besluit | In het afgesloten ticket. Moeilijk omkeerbaar → ook een ADR in `docs/adr/` |
| Woorden | `GLOSSARY.md` |
| Teststrategie, kritieke flows (F-nummers) | `docs/TESTING.md` |

`docs/SPEC.md` (D-1 t/m D-98) en `docs/BACKLOG.md` zijn archief: lezen mag, er komt niets bij.
Bestaande tickets houden hun code in de titel (T-7, V-4); nieuwe tickets krijgen alleen een issuenummer en een
label `bug` of `enhancement`.

## Nieuwe input

1. **Klein en duidelijk** (een bug, een wens): `triage`. Zoek de oorzaak in de code, zet het issue op
   `ready-for-agent` of vraag de eigenaar wat ontbreekt. Een bug met een lastige oorzaak: `diagnosing-bugs`.
2. **Groot of vaag**: een kaart met `wayfinder`, en per kaart-ticket rondes met `grilling` en
   `domain-modeling`.
3. **Kaart rond**: `to-tickets` maakt er bouwtickets van, onder de juiste epic en mijlpaal.
4. Bouwen alleen als de eigenaar het vraagt. Vastleggen en indelen is de standaard.

## Afhankelijkheden checken

Bij elk nieuw ticket en vóór het bouwen, kijk vooruit. De A-tabel in `docs/ROADMAP.md` houdt ze bij.
- **Database**: vraagt dit, of een latere epic, een nieuw veld? Bundel het in één migratie (A-1). Kies velden
  die latere features ook dienen (een keuzelijst in plaats van ja/nee, A-2).
- **Rekenlogica**: hoort dit in het krachtmodel of een andere centrale plek in `core` (A-3)? Elke formule
  staat op één plek.
- **Gegevens van buiten** (wearables, import): oefeningscodes, tijdstempels en eenheden bruikbaar (A-5, A-6)?
- **Back-up**: groeit het formaat mee, en blijven oude back-ups inleesbaar (A-7)?
- **Onomkeerbaar** (ondertekening, pakketnaam, naam, datamodel van gyms): eerst aan de eigenaar voorleggen.

## Bouwen

Volg `tdd`, met deze tussenweg voor de testplekken (seams). Vier vaste plekken gebruik je zonder te vragen:
1. de rekenlogica in `core`;
2. het opslaan van gegevens (repository);
3. een schermroute uit de risicolijst (F-nummers in `docs/TESTING.md`);
4. een screenshot.

Vraag de eigenaar alleen bij een nieuwe plek, of als iets niet automatisch te testen is (dan komt het op de
handmatige checklist). Zet per bouwticket in één zin wat getest wordt. Daarnaast uit `docs/TESTING.md`:
een bug krijgt eerst een rode test die naar het issue heet, en regels test je met property-tests.

Voor deze repo:
- Logica zoveel mogelijk in `core/` (puur Kotlin): `./gradlew -PcoreOnly :core:test`. De Android-app bouwt
  alleen in GitHub Actions.
- Strings altijd in het Engels én Nederlands (`values/` en `values-nl/`).
- Geen persoonlijke trainingsdata in de repo, tests of screenshots (D-06a).
- Na een databasewijziging: migratietest, schema-JSON committen en het back-upformaat bijwerken.
- Tot 1.0 alleen dev-builds; vanaf 1.0 wordt elke release een versierelease.

## Opleveren

Klaar is: core-tests groen, gepusht naar de werkbranch, CI groen. Rood is oorzaak zoeken en fixen.
Daarna:
1. Het issue sluiten met een reactie (wat is gebouwd, wat getest); de epic en `docs/TESTING.md` bijwerken.
2. Melding aan de eigenaar in gewone taal: wat er veranderd is, wat de eigenaar handmatig moet nalopen, en de open
   vragen (genummerd, met advies).
