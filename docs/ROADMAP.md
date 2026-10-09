# Releaseplan

Welke epic in welke release komt, en welke keuzes vooruit kijken zodat een latere feature geen grote verbouwing
wordt. Het werk zelf staat in GitHub Issues: per release een mijlpaal, per doel een issue met label `epic` en de
tickets eronder. Werkwijze: `.claude/skills/werkwijze/SKILL.md`.

Tot 1.0 alleen **dev-builds** (de vaste dev-link). Vanaf 1.0 wordt elke release een versierelease. Releases zijn
klein: één epic per release, en elke afgeronde ticketgroep levert meteen een dev-build op.

## Epics en releases (vastgesteld oktober 2026)

| Release | Epic | Doel |
|---|---|---|
| 0.6.2 | – | Stabiel, zichtbaar en Engels. *Gebouwd.* |
| 0.7 | [E1 · Het doel is van jou](https://github.com/Fabianlanger/GYM-8020/issues/54) | Trainen zonder ergernis: jij beslist, de app helpt. Rondt de kaart #1 af. |
| 0.8 | [E2 · Datamodel (schema v3)](https://github.com/Fabianlanger/GYM-8020/issues/55) | Eén database-aanpassing voor alles wat erop wacht (A-1). |
| 1.0 | [E9 · Publicatie](https://github.com/Fabianlanger/GYM-8020/issues/56) | Naam, eigen sleutel, versierelease, README, repo publiek. |
| 1.1 | [E6 · Eerste indruk](https://github.com/Fabianlanger/GYM-8020/issues/57) | Een nieuwe gebruiker snapt het meteen (rondleiding). |
| 1.2 | [E3 · Zie hoe je sterker wordt](https://github.com/Fabianlanger/GYM-8020/issues/58) | Kracht en vooruitgang die je gelooft. |
| 1.3 | [E4 · Jouw gym](https://github.com/Fabianlanger/GYM-8020/issues/59) | De app past bij je gym, en bij een andere gym. |
| 1.4 | [E5 · Invoer en geschiedenis](https://github.com/Fabianlanger/GYM-8020/issues/60) | Alles erin, ook wat je niet in de app deed. |
| 1.5 | [E7 · Wearables](https://github.com/Fabianlanger/GYM-8020/issues/61) | Garmin en Health Connect, in twee richtingen. |
| Later | – | W-4, W-6, W-9 (onder E7, nog niet ingepland). |

**Kwaliteit** is geen eigen epic: T-7 zit in E1, T-5 en T-6 in E9. De losse testpunten gaan mee waar ze passen:
T-4 (UI-tests F-2, F-3) in 0.7, T-2 (property-tests, o.a. back-up) in 0.8, T-3 (mutatietesten) in 1.2.

---

## Afhankelijkheden en architectuur: vooruit kijken

Keuzes die nú bepalen of latere features makkelijk of duur worden. Per keuze staat erbij in welke release hij
valt en welke latere features erop leunen.

| # | Keuze | Waarom nu | Raakt later |
|---|---|---|---|
| A-1 | **Eén database-migratie (schema v3, epic E2) voor alle nieuwe velden**, in plaats van een migratie per feature. Velden: doel per set met gewicht én reps en "zelf gezet", standaard-warming-up, materiaal per sessie, "nog over" (RIR) per set, de herkomst van een training, tijdstip en herkomst van lichaamsgewicht, schijven per materiaalprofiel; `maxKg` eruit. | Elke migratie is een risico voor je data. Eén goed geteste stap is veiliger dan vier kleine. | V-3, D-20, V-4, V-5, V-6, W-7 |
| A-2 | **Herkomst van een training als keuzelijst, niet als ja/nee:** `live`, `achteraf`, `geïmporteerd`, `wearable`. | V-4 heeft alleen "achteraf" nodig, maar V-5 (aantekeningen) en W-7 (Garmin-training herkennen) komen er later bij. Met een ja/nee-veld moet het later opnieuw. | V-4, V-5, W-7 |
| A-3 | **Eén krachtmodel in `core`**: e1RM, "effectieve reps" (reps + nog over), volume en de Krachtindex, op één plek gerekend. | Samenvatting, dashboard, records, heatmap en de Krachtindex moeten hetzelfde getal tonen. Verspreide formules gaan uit elkaar lopen. | V-2, O-1, D-20, R-1, W-2 |
| A-4 | **Meerdere gyms: nu kiezen of het ooit komt.** De schijvenvoorraad staat nu in de instellingen (één gym). Komen er meerdere gyms, dan moeten schijven en materiaal per gym in de database. | Het verschil tussen "nu meenemen in schema v3" en "later verhuizen uit de instellingen" is groot. Een losse verhuizing is extra migratiewerk en extra risico. | V-6 |
| A-5 | **Standaard oefeningscodes** in de bibliotheek, naast de naam: de soort oefening volgens Health Connect en volgens Garmin FIT. | Nodig om trainingen naar wearables te sturen en om aantekeningen te herkennen. In de bibliotheek is het klein werk; achteraf voor eigen oefeningen is het lastiger. | W-5, W-8, V-5 |
| A-6 | **Tijdstempels zijn betrouwbaar of leeg, nooit verzonnen.** Achteraf ingevoerde trainingen hebben geen tijd per oefening. | Hartslag per oefening (W-2) rekent met die tijden; een verzonnen tijd geeft een verkeerde hartslag. | W-2, W-3, V-4 |
| A-7 | **Back-upformaat groeit mee.** Elke databasewijziging krijgt een nieuwe versie van het back-upformaat, en oudere back-ups blijven inleesbaar. | Anders werkt een oude back-up niet meer na een update. | alles met A-1 |
| A-8 | **Eén ondertekeningssleutel bij 1.0, vóór de wearables.** Dev- en versiebuilds met dezelfde sleutel, met een eenmalige overstap. | Health Connect-toestemmingen en Android-back-ups horen bij de app. Hoe later we overstappen, hoe meer er opnieuw moet. | W-1 t/m W-9, 1.0 |
| A-9 | **UI-tests en screenshots (T-4/T-5) meenemen bij de schermwijzigingen.** Screenshots worden al gemaakt; vergelijken in CI komt bij 1.0. | Dan zien we bij het herontwerp van de samenvatting meteen wat er verandert, en sluipen er geen regressies in. | V-2, V-4, V-6 |
| A-10 | **Bibliotheeknamen tonen via hun vaste sleutel in de gekozen taal**, niet als vaste tekst in de database. | Nu staan ze in de database in de taal van de eerste start; een taalknop vertaalt ze dan niet mee. | V-8, V-5 |
| A-11 | **Nieuwe termen alleen in teksten, niet in code en database.** Een woordenlijst legt het verband. | Code en database hernoemen kost veel, is riskant voor je data en levert de gebruiker niets op. | V-7 |
| A-12 | **Termen, taal en invulvelden (V-7, V-8, V-11) vóór de screenshot-referenties (T-5) en vóór de rondleiding (V-9).** | Anders moeten alle referentie-screenshots en alle rondleidingsteksten twee keer. | T-5, V-9 |
| A-14 | **Lichaamsgewicht krijgt een tijdstip en een herkomst** (handmatig / wearable) in schema v3 (A-1); meerdere metingen per dag (V-14). | De snelknop (V-10) en automatisch gewicht uit Garmin (W-1) vullen dezelfde gegevens; met de herkomst bepalen we welke voorgaat en kun je handmatig corrigeren. | V-10, W-1 |
| A-13 | **De rondleiding (1.1) pas bouwen als de schermen van 0.7 en 0.8 staan**, met UI-tests die controleren dat elke stap zijn onderdeel vindt. | Een rondleiding wijst naar concrete onderdelen; die verschuiven bij een herontwerp. | V-9 |
