# Teststrategie

Wat we samen hebben afgesproken over testen, en waar we naartoe willen. Dit document hoort bij
`SPEC.md` (besluiten) en `BACKLOG.md` (werk). Nog niet alles is gebouwd; de status staat per onderdeel.

Status: **afgesproken** = zo werken we · **gepland** = staat op de backlog (T-nummers) · **later** = bewust uitgesteld.

---

## Uitgangspunt

Een bug laat zien waar iets **makkelijk fout gaat**. Juist daar moet een test komen die blijft staan,
zodat het niet opnieuw fout gaat als we later aan die code schroeven.

**Code coverage is geen doel.** Het meet alleen welke regels code zijn uitgevoerd, niet of een test een fout
zou opmerken. We sturen op wat wel iets zegt: de risicolijst en mutatietesten (zie 3).

## Waar we nu staan (v0.6)

| Laag | Wat | Aantal |
|---|---|---|
| Rekenlogica (`core/`) | Unittests: doelen, gewichten, schijven, dagherkenning, dashboard, heatmap, herinneringen, back-upformaat | 50 |
| Database | Migratietest: je data overleeft een update | 1 |
| Statische controle | Android Lint in CI (blokkeert niet) | – |
| Schermen, flows, screenshots | – | 0 |

---

## 1. Bugs: eerst een test die faalt, dan de fix — *afgesproken*

1. De bug krijgt een backlognummer (B-x) met een beschrijving hoe je hem kunt nabootsen.
2. Eerst een test die de bug precies nabootst. Die is **rood**.
3. Dan de fix. De test wordt **groen**.
4. De test draagt het backlognummer in zijn naam, bijv. `` `O-2 plus never lowers the weight` ``, en blijft
   voor altijd staan.

Kan een bug (nog) niet automatisch getest worden, dan zetten we in de backlog waarom niet, en komt hij op de
handmatige releasechecklist (zie 6).

## 2. Regels testen, niet alleen voorbeelden — *afgesproken*

Naast losse voorbeelden ("85 → 87,5") testen we **regels die altijd moeten gelden**. De test probeert dan
duizenden willekeurige invoeren (*property-based testing*). Zo vind je ook de gevallen waar niemand aan dacht.

Voorbeeld: de regel "+ geeft nooit een lager gewicht" had het kabelprobleem (O-2) meteen gevonden.

Regels die we willen vastleggen (T-2):

| Regel | Gebied |
|---|---|
| + geeft nooit een lager gewicht, − nooit een hoger | materiaal · **gebouwd** (`MaterialRulesTest`) |
| Een voorgesteld gewicht is altijd op te leggen met het materiaal, of wordt als "buiten bereik" gemeld | materiaal |
| Het doel gaat nooit de verkeerde kant op (te licht → zwaarder, te zwaar → lichter) | doelen |
| Export → import levert exact dezelfde data op | back-up |
| Schijven per kant tellen op tot het totaal, en je hebt nooit meer schijven nodig dan je hebt | schijven |
| e1RM stijgt als gewicht of reps stijgen | kracht |

## 3. Risicolijst in plaats van coverage — *afgesproken*

We houden een lijst bij van **kritieke flows**. Elke flow moet door minstens één automatische test gedekt
zijn. Nieuwe features voegen hun flow hier toe.

| # | Flow | Gedekt door | Status |
|---|---|---|---|
| F-1 | Training starten → sets invullen → doel voor volgende keer → volgende training toont dat doel | – | open (T-4) |
| F-2 | Warming-up → GO → aftellen → timer loopt → eerste invoer stopt timer | – | open (T-4) |
| F-3 | Training afronden → samenvatting → (bewerken) → logboek klopt | – | open (T-4) |
| F-4 | App-update met databasemigratie: alle data blijft staan | MigrationTest | gedekt |
| F-5 | Exporteren → importeren: alles terug | deels (back-upformaat) | open (T-2) |
| F-6 | Automatische back-up naar map, houdt de nieuwste 7 | – | open |
| F-7 | Dagherkenning en herinneringen | HomeAdvisorTest, RemindersTest | gedekt |
| F-8 | Gewichten en schijven: haalbare gewichten, minimale wissels, materiaal beperkt nooit | MaterialTest, MaterialRulesTest | gedekt |
| F-9 | Introductie bij eerste start; bestaande gebruikers slaan hem over | – | open (T-4) |
| F-10 | Training achteraf invoeren → telt mee op de juiste datum; oudere datum verandert het doel niet | – | open (V-4) |

**Mutatietesten** (T-3, met PIT op `core/`): een tool verandert bewust kleine dingen in de code, bijv. `>` in
`>=` of `+` in `-`, en kijkt of een test dat opmerkt. Het resultaat is het percentage **gevangen** fouten.
Dat getal zegt echt iets over de kwaliteit van de tests. Doel: eerst meten, dan per release iets omhoog.

## 4. Schermtests en screenshots — *afgesproken*

Het Android-equivalent van Playwright, zonder telefoon, gewoon in CI (T-4, T-5).

- **UI-tests (Compose, via Robolectric):** een test tikt op knoppen en controleert wat er gebeurt.
  Voorbeelden: na GO heeft geen invoerveld focus (B-2); tik op Z wordt L (B-3).
- **Screenshottests (Roborazzi):** van elk belangrijk scherm wordt bij elke build een afbeelding gemaakt:
  - in het Nederlands en het Engels, met gewone en grote letters, op een klein en een groot scherm;
  - met vaste voorbeelddata, nooit echte persoonlijke data.
- **Vergelijken:** de afbeeldingen worden vergeleken met goedgekeurde "referentie"-screenshots in de repo.
  Een onbedoeld verschil maakt de build rood, met een verschilplaatje erbij. Is het verschil wel bedoeld, dan
  werken we de referentie bij, en die wijziging is zichtbaar in de commit.
- **Downloaden:** de screenshots komen als download bij elke build. Zo kun je een nieuwe versie bekijken
  zonder te installeren, en ziet Claude wat er gebouwd is.

## 5. End-to-end op een echte emulator — *later*

Een echte Android-emulator in GitHub Actions (bijv. met Maestro, dat stappen doorloopt zoals Playwright).
Het nadeel: ongeveer 10 minuten per run, en gevoeliger voor haperingen. Als we het doen: 2 à 3
rooktests (starten → training → afronden → samenvatting), alleen bij een versierelease (T-6).

## 6. Bugs versus nieuwe features — *afgesproken*

| | Bug | Nieuwe feature |
|---|---|---|
| Start | Backlognummer B-x, plus hoe je het kunt nabootsen | Besluit in SPEC (D-x) met **acceptatiecriteria**: "als ik X doe, dan zie ik Y" |
| Test | Eerst een rode test die de bug nabootst, dan de fix | De acceptatiecriteria worden tests, geschreven vóór of tijdens het bouwen |
| Risicolijst | Blijkt de bug in een flow te zitten die niet op de lijst staat, dan komt die flow erbij | Nieuwe flow komt op de risicolijst |
| Screenshots | Bij een UI-bug: referentie-screenshot bijwerken | Nieuwe schermen krijgen een referentie-screenshot |
| Klaar als | Test groen en blijft staan | Alle criteria groen, screenshots bekeken, op je telefoon gezien |
| Versie | Patch (0.6.1) | Minor (0.7) |

**Releasechecklist** (bij elke versierelease):
1. CI groen: unittests, UI-tests, screenshots, migratietest.
2. Screenshots van gewijzigde schermen bekeken.
3. De kritieke flows F-1 t/m F-3 één keer met de hand op de telefoon doorlopen.
4. Nog niet automatisch geteste bugs uit de backlog met de hand nagelopen.

## 7. Grenzen

- Claude kan in zijn eigen omgeving geen Android-app of emulator draaien. Alles wat Android nodig heeft,
  draait in GitHub Actions.
- De rekenlogica zit bewust in `core/` (puur Kotlin): die is snel en makkelijk te testen. Nieuwe logica gaat
  zoveel mogelijk daarheen en niet in schermen.
- Echte persoonlijke trainingsdata komt nooit in tests of screenshots (D-06a).
