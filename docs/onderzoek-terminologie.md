# Onderzoek: termen en taal (O-4)

> Status: voorstel, nog niets gebouwd. Beslispunten staan onderaan.

## Aanleiding

De app gebruikt nu "Schema", "Weekplan" en "Training". In de fitnesswereld zijn andere termen gangbaar. Wie
uit een andere app komt, moet ons begrip "schema" vertalen. De woorden moeten aansluiten bij wat sporters en
andere apps gebruiken. Daarnaast wordt **Engels de standaardtaal**, met Nederlands als keuze.

## Wat de fitnesswereld gebruikt

| Begrip | Betekenis | Gebruikt door |
|---|---|---|
| **Exercise** | één oefening (bench press) | iedereen |
| **Set / rep** | een reeks / een herhaling | iedereen |
| **Warm-up set** | opwarmset, telt niet mee | iedereen |
| **Working set** | set die telt | algemeen |
| **Top set / back-off set** | de zwaarste set van een oefening, gevolgd door een of meer lichtere sets. Precies onze zwaar → licht. | krachtsport (powerlifting) |
| **Workout** | een verzameling oefeningen voor één training ("Full body", "Push") | NASM, de meeste apps |
| **Routine** | Hevy: het *plan* van een workout; "workout" is daar de gelogde training | Hevy |
| **Program / programme** | gestructureerd plan: welke workouts, wanneer, en hoe ze over de tijd veranderen | NASM, algemeen |
| **Split** | hoe je de training over de week verdeelt (full body, upper/lower, push/pull/legs) | algemeen |
| **Microcycle / mesocycle / macrocycle** | periodisering: een week / een blok van 3–12 weken / een seizoen of jaar | sportwetenschap |
| **Session** | één keer trainen, zoals het in je logboek staat | algemeen |
| **Progression / next target** | wat je de volgende keer gaat doen | algemeen |
| **Equipment** | materiaal: stang, machine, kabel, dumbbells | algemeen |

## Hoe onze begrippen zich daartoe verhouden

| Nu in de app (EN · NL) | Wat het is | Voorstel (EN · NL) |
|---|---|---|
| Exercise · Oefening | één oefening | **Exercise · Oefening** (blijft) |
| Heavy / Light · Zwaar / Licht | top set + back-off set, beide tot falen | **Heavy / Light · Zwaar / Licht** (blijft, duidelijker). In de uitleg van de methode: "top set + back-off set". |
| Schema · Schema | de oefeningen van één training (Full body, Push) | **Workout · Workout** |
| Week plan · Weekplan | hoe vaak per week, welke workouts je afwisselt, eventueel op vaste dagen | **Program · Programma** (het soort indeling heet "split") |
| Training (een gedane training) | één keer getraind, in het logboek | **Session · Training**. In de lijst: "Sessions" / "Trainingen". |
| Next time · Volgende keer | gewicht en sets voor de volgende keer | **Next target · Volgende doel** |
| Goal · Doel | het doelgewicht van een set | **Target · Doel** |
| Material · Materiaal | stang, schijven, machine, kabel | **Equipment · Materiaal** |
| Plates in your gym · Schijven | aanwezige schijven | **Available plates · Beschikbare schijven** |
| Library · Bibliotheek | standaardoefeningen | **Exercise library · Oefeningenbibliotheek** |

**Waarom niet Routine?** Hevy gebruikt "routine" voor het plan en "workout" voor wat je doet. De meeste
andere apps en vakorganisaties (zoals NASM) noemen het plan juist "workout". Bij ons doe je de training altijd vanuit een plan,
dus is **Workout (plan) → Session (uitgevoerd)** het duidelijkst. *Start workout* leest dan ook natuurlijk.

## Taal

- **Nu:** de app volgt de taal van de telefoon. Er is geen taalknop.
- **Gewenst:** standaard Engels, Nederlands te kiezen in Instellingen. Op Android 13+ kan dat ook via de
  systeeminstellingen per app.
- **Afhankelijkheid (A-10):** de standaardoefeningen, -workouts en -programma's worden nu bij de eerste start
  **in de taal van dat moment** in de database gezet. Een taalknop vertaalt die dan niet mee. Oplossing:
  voor bibliotheekitems de naam niet opslaan maar tonen in de gekozen taal, via hun vaste sleutel. Een naam
  die je zelf wijzigt blijft natuurlijk wel bewaard.
- **Afhankelijkheid (A-11):** de nieuwe termen zitten in alle teksten. Hernoem **alleen de teksten en de
  documentatie**, niet de code of de database (die heten intern `schemas`, `week_plans`, `sessions`). Een
  woordenlijst in de SPEC legt het verband. Code en database hernoemen kost veel en levert de gebruiker niets
  op.

## Beslispunten

| # | Vraag | Advies |
|---|---|---|
| 19 | Termen: **Workout / Program / Session** (voorstel) of in Hevy-stijl **Routine / Program / Workout**? | Workout / Program / Session |
| 20 | Zwaar/licht houden, of "Top set / Back-off set" in de app? | Zwaar/licht houden; de vakterm in de uitleg |
| 21 | "Material" wordt "Equipment" (NL blijft "Materiaal")? | Ja |
| 22 | Engels standaard, Nederlands te kiezen, en de bibliotheek vertaalt mee (A-10)? | Ja |

## Bronnen

- Hevy, workouts vs routines: https://help.hevyapp.com/hc/en-us/articles/33703513582871-Workouts-vs-Routines-in-Hevy-What-They-Mean-and-How-to-Use-Them
- NASM, workout vs program: https://support.nasm.org/what-is-the-difference-between-a-workout-and-a-program
- Workout programming (programma, split): https://www.dotmoovs.com/articles/workout-programming
- Top set en back-off set: https://www.fitbudd.com/insights/what-is-a-back-off-set-top-set-vs-back-off-set-guide
- Back-off sets: https://barbend.com/back-off-sets/
- Micro-, meso- en macrocycli: https://www.setforset.com/blogs/news/macrocycle-mesocycle-microcycle-explained
