# Releaseplan

Wat er in welke release komt, welke vragen per release nog open staan, en welke keuzes vooruit kijken zodat
een latere feature geen grote verbouwing wordt. Werkwijze: `.claude/skills/werkwijze/SKILL.md`.

**Afspraak:** tot 1.0 blijven we op **dev-builds** (de vaste dev-link). Versiereleases met de eigen sleutel
komen pas bij 1.0, als alles af is wat we nu willen (D-85).

Legenda: B = bug · V = verbetering · O = onderzoek/ontwerp · T = testen · W = wearables · R/D = besluiten in SPEC.
Details per punt staan in `BACKLOG.md`.

---

## Afhankelijkheden en architectuur: vooruit kijken

Keuzes die nú bepalen of latere features makkelijk of duur worden. Per keuze staat erbij in welke release hij
valt en welke latere features erop leunen.

| # | Keuze | Waarom nu | Raakt later |
|---|---|---|---|
| A-1 | **Eén database-migratie (schema v3) voor alle nieuwe velden van 0.7 én 0.8**, in plaats van een migratie per feature. Velden: reps-doel in het plan, "nog over" (RIR) per set, de herkomst van een training, en schijven per materiaalprofiel. | Elke migratie is een risico voor je data. Eén goed geteste stap is veiliger dan vier kleine. | V-3, D-20, V-4, V-5, V-6, W-7 |
| A-2 | **Herkomst van een training als keuzelijst, niet als ja/nee:** `live`, `achteraf`, `geïmporteerd`, `wearable`. | V-4 heeft alleen "achteraf" nodig, maar V-5 (aantekeningen) en W-7 (Garmin-training herkennen) komen er later bij. Met een ja/nee-veld moet het later opnieuw. | V-4, V-5, W-7 |
| A-3 | **Eén krachtmodel in `core`**: e1RM, "effectieve reps" (reps + nog over), volume en de Krachtindex, op één plek gerekend. | Samenvatting, dashboard, records, heatmap en de Krachtindex moeten hetzelfde getal tonen. Verspreide formules gaan uit elkaar lopen. | V-2, O-1, D-20, R-1, W-2 |
| A-4 | **Meerdere gyms: nu kiezen of het ooit komt.** De schijvenvoorraad staat nu in de instellingen (één gym). Komen er meerdere gyms, dan moeten schijven en materiaal per gym in de database. | Het verschil tussen "nu meenemen in schema v3" en "later verhuizen uit de instellingen" is groot. Een losse verhuizing is extra migratiewerk en extra risico. | V-6 |
| A-5 | **Standaard oefeningscodes** in de bibliotheek, naast de naam: de soort oefening volgens Health Connect en volgens Garmin FIT. | Nodig om trainingen naar wearables te sturen en om aantekeningen te herkennen. In de bibliotheek is het klein werk; achteraf voor eigen oefeningen is het lastiger. | W-5, W-8, V-5 |
| A-6 | **Tijdstempels zijn betrouwbaar of leeg, nooit verzonnen.** Achteraf ingevoerde trainingen hebben geen tijd per oefening. | Hartslag per oefening (W-2) rekent met die tijden; een verzonnen tijd geeft een verkeerde hartslag. | W-2, W-3, V-4 |
| A-7 | **Back-upformaat groeit mee.** Elke databasewijziging krijgt een nieuwe versie van het back-upformaat, en oudere back-ups blijven inleesbaar. | Anders werkt een oude back-up niet meer na een update. | alles met A-1 |
| A-8 | **Eén ondertekeningssleutel vóór 1.0.** Dev- en versiebuilds met dezelfde sleutel, met een eenmalige overstap. | Health Connect-toestemmingen en Android-back-ups horen bij de app. Hoe later we overstappen, hoe meer er opnieuw moet. | W-1 t/m W-9, 1.0 |
| A-9 | **UI-tests en screenshots (T-4/T-5) vóór de grote schermwijzigingen van 0.7.** | Dan zien we bij het herontwerp van de samenvatting meteen wat er verandert, en sluipen er geen regressies in. | V-2, V-4, V-6 |
| A-10 | **Bibliotheeknamen tonen via hun vaste sleutel in de gekozen taal**, niet als vaste tekst in de database. | Nu staan ze in de database in de taal van de eerste start; een taalknop vertaalt ze dan niet mee. | V-8, V-5 |
| A-11 | **Nieuwe termen alleen in teksten, niet in code en database.** Een woordenlijst legt het verband. | Code en database hernoemen kost veel, is riskant voor je data en levert de gebruiker niets op. | V-7 |
| A-12 | **Termen en taal (V-7, V-8) vóór de screenshot-referenties (T-5) en vóór de rondleiding (V-9).** | Anders moeten alle referentie-screenshots en alle rondleidingsteksten twee keer. | T-5, V-9 |
| A-14 | **Lichaamsgewicht krijgt een herkomst** (handmatig / wearable) in schema v3 (A-1). | De snelknop (V-10) en automatisch gewicht uit Garmin (W-1) vullen dezelfde gegevens; met de herkomst bepalen we welke voorgaat en kun je handmatig corrigeren. | V-10, W-1 |
| A-13 | **De rondleiding pas bouwen als de schermen van 0.7/0.8 staan**, met UI-tests die controleren dat elke stap zijn onderdeel vindt. | Een rondleiding wijst naar concrete onderdelen; die verschuiven bij een herontwerp. | V-9 |

---

## 0.6.2 · Stabiel, zichtbaar en Engels

*Doel: de laatste bug uit gebruik eruit, termen en taal goed, en de testbasis voor schermen staan (A-9, A-12).*

| Punt | Wat |
|---|---|
| B-5 | Timer kwijt na draaien van het scherm |
| V-7 | Termen gelijk met de fitnesswereld (Workout, Program, Session, Next target, Equipment) |
| V-8 | Engels standaard, Nederlands te kiezen; bibliotheek vertaalt mee (A-10) |
| V-10 | Snelknop op het beginscherm om je lichaamsgewicht in te vullen (voorrang) |
| T-4 | UI-tests (Compose) voor de kritieke flows F-1, F-2, F-3, F-9 |
| T-5 | Screenshottests: referentie-screenshots, verschil maakt de build rood, screenshots als download |
| T-2 | Meer regels testen: doel, back-up heen en terug, schijven |

**Vragen:**
1. **B-5:** mag de app vast staand (portret)? *Advies: ja.*

Plus vragen 19–22 hieronder bij V-7 en V-8 (`onderzoek-terminologie.md`):
19. **Termen:** Workout / Program / Session, of in Hevy-stijl Routine / Program / Workout? *Advies: Workout /
    Program / Session.*
20. **Zwaar/licht** houden, of "Top set / Back-off set" in de app? *Advies: zwaar/licht houden; de vakterm in
    de uitleg.*
21. **"Material" wordt "Equipment"** (NL blijft "Materiaal")? *Advies: ja.*
22. **Engels standaard**, Nederlands te kiezen, en de bibliotheek vertaalt mee? *Advies: ja.*

Bij V-10 (snelknop gewicht):
27. **Vorm:** een kaart "Gewicht vandaag" op het beginscherm die direct een invulvenster opent, voorgevuld
    met je laatste meting? *Advies: ja.*
28. **Herinnering:** de gewichtsherinnering instelbaar maken als dagelijks of wekelijks (standaard
    wekelijks)? *Advies: ja.*

---

## 0.7 · Kracht en datamodel

*Doel: de app laat zien hoe sterk je bent en hoeveel sterker je wordt. Plus één database-migratie voor alles
wat 0.7 en 0.8 nodig hebben (A-1, A-2, A-4).*

| Punt | Wat |
|---|---|
| A-1 | Schema v3 met migratietest en back-upformaat v3 |
| A-3 | Krachtmodel in `core` |
| V-2 | Samenvatting: e1RM en totaal verzet gewicht t.o.v. vorige keer; tijden als gemiddelde |
| O-1 | Krachtindex (100 = je beginniveau), in de samenvatting en het dashboard |
| V-3 | Reps-doel voor oefeningen zonder gewicht (crunches) |
| D-20 | Optioneel "gestopt vóór falen: nog 1 / 2 / 3+" per set |
| R-1 | Spierpercentages: 100 / 50 / 25 (stabilisator), bibliotheek bijstellen |
| A-5 | Standaard oefeningscodes in de bibliotheek |
| T-3 | Mutatietesten op `core`: eerste meting |

**Vragen** (adviezen staan in `onderzoek-spieren-en-zwaarte.md` en `BACKLOG.md`):
2. **O-1:** Krachtindex zoals voorgesteld? *Advies: ja.*
3. **O-1:** DOTS-score erbij als je squat, bench en deadlift doet? *Advies: optioneel, kan later.*
4. **V-3:** reps-doel standaard "vorige keer + 1"? *Advies: ja.*
5. **R-1a:** 100 / 50 houden en 25% stabilisator toevoegen? *Advies: ja.*
6. **R-1b:** bibliotheek bijstellen (biceps 75% bij trekoefeningen; onderrug 25% bij squat; onderarmen en
   trapezius 25% bij deadlift)? *Advies: ja.*
7. **D-20a:** "gestopt vóór falen" als optioneel veld, standaard leeg = falen? *Advies: ja.*
8. **D-20b:** doel voor volgende keer rekent met reps + nog over? *Advies: ja.*
9. **D-20c:** volume en heatmap wegen mee hoe dicht je bij falen zat (1,0 / 0,9 / 0,8 / 0,6)? *Advies: ja.*
10. **A-4 (architectuur, nodig vóór schema v3):** wil je **ooit meerdere gyms** bijhouden? *Advies: ja als
    het waarschijnlijk is; dan nemen we het nu mee in het datamodel, ook al komt het scherm pas in 0.8.*

---

## 0.8 · Invoer en materiaal

*Doel: alles kunnen vastleggen, ook achteraf, en materiaal dat bij jouw gym past.*

| Punt | Wat |
|---|---|
| V-4 | Training achteraf invoeren (dashboard → "+ Training toevoegen") |
| V-6 | Schijven per materiaalprofiel (uitzondering); meerdere gyms als vraag 10 = ja |
| O-2 | Rest: maximum laat meegroeien en/of ruimere standaardprofielen |

**Vragen:**
11. **V-4:** sets voorvullen met het gewicht van toen, of leeg beginnen? *Advies: voorvullen.*
12. **V-4:** achteraf ingevoerde trainingen gewoon meetellen in alle statistieken? *Advies: ja, behalve tijden.*
13. **O-2:** maximum laten meegroeien met één tik ("Maximum ophogen naar 85?")? *Advies: ja.*
14. **O-2:** standaardprofielen ruimer maken (kabel tot 150, pin-stack tot 200)? *Advies: ja.*

---

## 0.9 · Rondleiding door de eerste workout

*Doel: wie de app voor het eerst gebruikt (of aan wie je hem laat zien), snapt de methode tijdens de eerste
workout. Eerst een prototype; bouwen pas als de schermen van 0.7/0.8 staan (A-13).*

| Punt | Wat |
|---|---|
| V-9 | Prototype (klikbaar, HTML op telefoonformaat): stappen, teksten, wiebelen. Kan al eerder, los van de app. |
| V-9 | Rondleiding in de app: markering + pop-up per stap, overslaan en opnieuw bekijken |
| V-9 | Demo-workout om te oefenen of te presenteren, zonder opslaan (als vraag 24 = ja) |
| T-4 | UI-test: de rondleiding vindt elke stap; verschijnt één keer (F-11) |

**Vragen:**
23. **Wanneer:** bij de eerste workout ooit (welke dan ook), één keer, en opnieuw te starten via Instellingen?
    *Advies: ja.*
24. **Demo-workout** om te oefenen of aan iemand te laten zien, zonder dat er iets wordt opgeslagen?
    *Advies: ja.*
25. **Prototype** eerst als klikbare HTML-pagina die je op je telefoon kunt proberen? *Advies: ja, en dat kan
    al vóór 0.7.*

---

## 0.10 · Wearables (Health Connect)

*Doel: gezondheidsdata erin, trainingen eruit. Vooraf: één ondertekeningssleutel (A-8).*

| Punt | Wat |
|---|---|
| A-8 | Eén ondertekeningssleutel voor dev en release; eenmalige overstap met export → import |
| W-1 | Lichaamsgewicht automatisch van weegschaal of Garmin |
| W-2 | Hartslag per oefening (gemiddeld en maximaal) en herstel tijdens het wisselen |
| W-5 | Trainingen naar Health Connect (Fitbit, Samsung, Google, …) |
| W-8 | Proef: training als FIT-bestand om zelf in Garmin Connect te importeren |
| W-7 | Garmin-training herkennen en voorstellen om in te voeren |
| W-3 | Calorieën per training |

**Vragen** (`onderzoek-wearables.md`):
15. **O-3a:** Health Connect als koppeling voor alle wearables? *Advies: ja.*
16. **O-3b:** eerst gewicht (W-1) en hartslag per oefening (W-2)? *Advies: ja.*
17. **O-3c/e:** tweerichting: trainingen naar Health Connect, en de FIT-proef voor Garmin? *Advies: ja.*

---

## 1.0 · Publicatie

| Punt | Wat |
|---|---|
| – | Eerste versierelease met de eigen sleutel |
| – | Screenshots in de README (uit T-5, met voorbeelddata) |
| – | Repo publiek |
| T-6 | Rooktests op een echte emulator bij elke versierelease |

**Vragen:**
18. Wanneer gaat de repo publiek: bij 1.0, of al eerder?
26. **O-5:** blijft de naam GYM-8020, of een nieuwe naam vóór de publicatie?

---

## Later (bewust niet ingepland)

| Punt | Wat |
|---|---|
| V-5 | Importeren uit aantekeningen (eerst een paar voorbeelden van jou nodig) |
| W-4 | Herstel-hint op basis van slaap en rusthartslag (eerst eigen onderzoek) |
| W-6 | Stappen op rustdagen |
| W-9 | Eigen Garmin-horloge-app |
| P-1 | Werkwijze-upgrade naar de aanpak van Matt Pocock (eerst uitzoeken wat we overnemen) |
| O-5 | Naam van de app heroverwegen (vóór 1.0 beslissen, want de naam komt in pakketnaam, icoon en store) |
