# Onderzoek: volume tastbaar maken met gewichten van dingen (issue #101)

> Status: onderzoek, geen besluit. Datum: 2026-11-15. Hoort bij epic #74.

## Methode en betrouwbaarheid

- Repo gelezen: `core/src/main/kotlin/nl/ibs/gym8020/core/logic/Strength.kt` (`EffectiveWeight.of`,
  `Strength.volume`, `Strength.e1rm`), `GLOSSARY.md` ("Volume"), `core/.../library/DefaultLibrary.kt`
  (schema's en `bodyweightFactor` per oefening).
- Web: vanuit deze omgeving konden pagina's **niet direct geopend** worden (WebFetch geblokkeerd, o.a.
  strengthlevel.com). Alle cijfers hieronder komen uit de geïndexeerde tekst van de genoemde pagina's via een
  zoekmachine, met de URL erbij. Daarom per object een betrouwbaarheidsklasse:
  - **A** = primaire bron (fabrikant, overheid, dierentuin, officiële site van het object).
  - **B** = betrouwbare secundaire bron (encyclopedie, vakblad, meerdere bronnen die overeenkomen).
  - **C** = eigen schatting of één zwakke bron: **nog verifiëren** vóór het in de app komt.
- "ton" betekent hier altijd metrische ton = 1000 kg. Amerikaanse bronnen gebruiken vaak "tons" (short ton =
  907 kg); waar dat zo was, is omgerekend.

## Korte antwoorden

1. **Een training is ruwweg 2 tot 16 ton.** Een beginner met lege stang en ondersteunde pull-ups komt op
   ~2–4 ton, een gemiddelde sporter op ~9 ton, een gevorderde op ~15 ton; een benendag met leg press kan
   daarboven uitkomen (~16–20 ton). Per week (3×) is dat ~6–50 ton, per jaar ~300–2500 ton.
2. **De ladder** loopt van een scooter (~100 kg) via piano, Friese koe, auto, nijlpaard, olifant, stadsbus en
   tram tot een Boeing 737 (~41 ton) en een vrachtwagen (50 ton). Voor totalen "sinds je begon": blauwe
   vinvis, Vrijheidsbeeld, Eiffeltoren. Zie de tabel; de stappen zijn steeds ≤ ~1,5×.
3. **Valkuilen:** liever generiek dan merk; dieren met grote spreiding (neem een typisch, iets voorzichtig
   getal); nooit naar boven afronden ("meer dan", "bijna"); onder 1000 kg in kg, daarboven in ton.
4. **Advies:** kies het object met de verhouding volume/gewicht die het "mooist" uitspreekt (bij voorkeur
   1–3×, afgerond naar beneden op een half), trek willekeurig maar stabiel per sessie uit de passende
   kandidaten en sla de laatst gebruikte objecten over.

## 1. Hoeveel volume is een training?

### Hoe de app rekent

- `Strength.volume(set)` = effectief gewicht × reps, alleen voor werksets (`role.isWork`); warming-ups tellen
  niet (`GLOSSARY.md` "Volume").
- Effectief gewicht (`EffectiveWeight.of`): bij een externe last het ingevoerde gewicht; bij lichaamsgewicht
  `lichaamsgewicht × factor + invoer` (invoer negatief = ondersteuning). Factoren uit `DefaultLibrary.kt`:
  pull-up/chin-up 1,0; dips 0,95; push-up 0,65; back extension 0,55; crunches/leg raises 0,30.
- Zonder lichaamsgewicht is het effectief gewicht van een lichaamsgewicht-oefening onbekend (null) en telt de
  set niet mee: bij een gebruiker zonder ingevuld gewicht valt het volume dus lager uit.

### Aannames

- Methode: per oefening één zware set van 6 reps en één lichte set van 16 reps, beide tot falen.
- Gewicht per set afgeleid uit de e1RM met dezelfde formule als de app (Epley): 6 reps = e1RM / 1,2;
  16 reps = e1RM / 1,53. Dan is het volume per oefening ≈ **15 × e1RM** (zware set ≈ 5 × e1RM, lichte set
  ≈ 10 × e1RM: de lichte set levert dus twee derde van het volume). Dit is een eigen afleiding.
- Schema: "Full body" uit `DefaultLibrary.kt` (9 oefeningen: back squat, incline bench press, pull-up, seated
  calf raise, overhead press, seated cable row, decline crunch, cable biceps curl, cable triceps pushdown).
- Krachtniveaus: e1RM's gekozen rond de StrengthLog-standaarden voor mannen (kg):
  squat 58 / 93 / 120 / 147 (beginner / novice / intermediate / advanced),
  bench 54 / 80 / 100 / 120, overhead press 32 / 47 / 57 / 68
  ([squat](https://www.strengthlog.com/squat-strength-standards-kg/),
  [bench](https://www.strengthlog.com/bench-press-strength-standards-kg/),
  [overhead press](https://www.strengthlog.com/overhead-press-strength-standards-kg/)).
  Strength Level gebruikt voor squat verhoudingen 0,75 / 1,25 / 1,75 / 2,25 × lichaamsgewicht
  ([strengthlevel.com](https://strengthlevel.com/strength-standards/squat/kg)). Machine- en kabelgewichten
  zijn **eigen schattingen** (geen betrouwbare standaarden: elke machine is anders).
- Rekenscript: `vol.py` naast dit bestand in de scratchpad.

### Scenario's (Full body, 9 oefeningen)

| Oefening | (a) Net begonnen, 70 kg — zwaar / licht (kg) | volume | (b) Gemiddeld, 75 kg — e1RM | volume | (c) Gevorderd, 85 kg — e1RM | volume |
|---|---|---|---|---|---|---|
| Back squat | lege stang 20 / 20 | 440 | 100 | 1.543 | 160 | 2.470 |
| Incline bench press | 20 / 20 | 440 | 70 | 1.080 | 110 | 1.698 |
| Pull-up (factor 1,0) | 70−40 = 30 / 70−50 = 20 | 500 | 75 (lichaamsgew.) / 75−25 | 1.250 | 85+30 / 85 | 2.050 |
| Seated calf raise | 30 / 20 | 500 | 120 | 1.852 | 200 | 3.087 |
| Overhead press | 20 / 20 | 440 | 50 | 772 | 75 | 1.158 |
| Seated cable row | 25 / 15 | 390 | 80 | 1.235 | 120 | 1.852 |
| Decline crunch (factor 0,3) | 21 / 21 | 462 | 22,5+5 / 22,5 | 525 | 25,5+15 / 25,5 | 651 |
| Cable biceps curl | 10 / 7,5 | 180 | 30 | 463 | 50 | 772 |
| Cable triceps pushdown | 15 / 10 | 250 | 45 | 695 | 70 | 1.080 |
| **Totaal** | | **~3,6 ton** | | **~9,4 ton** | | **~14,8 ton** |

Extra doorrekeningen:

- **Ondergrens:** beginner met maar 6 (de lichtste) oefeningen: **~2,1 ton**. Een beginner die minder dan 16
  reps haalt met de lege stang komt nog iets lager: reken op **~1,5–2 ton** als absolute bodem.
- **Bovengrens:** gevorderde "Legs" (back squat 160, RDL 140, leg press 400, leg curl 80, standing calf
  raise 220, hanging leg raise): **~16 ton**, waarvan de leg press alleen al ~6 ton. Met 9 oefeningen en/of
  een zwaardere sporter kan het naar **~20–25 ton**.
- **Opvallend:** machines met grote gewichten (leg press, calf raise) blazen het volume op tegenover
  stangoefeningen; en lichaamsgewicht-oefeningen tellen flink mee (pull-ups bij 85 kg: 2 ton). Prima voor een
  leuk feitje, maar het getal is geen vergelijking tussen workouts met verschillende oefeningen.

### Bandbreedte waar de ladder op moet passen

| Periode | Ondergrens | Typisch | Bovengrens |
|---|---|---|---|
| Eén training | ~1,5 ton | 5–12 ton | ~20–25 ton |
| Eén week (3–4×) | ~5 ton | 20–40 ton | ~80 ton |
| Eén jaar (~150 trainingen) | ~250 ton | ~1.000–1.500 ton | ~3.500 ton |

Conclusie: het hoofdbereik van de ladder is **~1 tot ~50 ton**; objecten onder 1 ton zijn alleen nodig voor
losse oefeningen of een heel korte eerste training; objecten boven 50 ton voor week- en "sinds je
begon"-totalen.

## 2. De ladder: objecten met gewicht en bron

Gewicht = typisch getal dat de app zou gebruiken (bewust aan de voorzichtige kant), met bandbreedte.
"Stap" = verhouding tot het vorige object in de hoofdladder.

| # | Object (NL) | App-gewicht | Bandbreedte | Stap | Bron | Kl. | Speels? |
|---|---|---|---|---|---|---|---|
| 1 | een scooter | 100 kg | 80–120 kg | — | eigen schatting | C | ja |
| 2 | een sumoworstelaar | 160 kg | 140–192 kg | 1,6* | Japan Sumo Association-handout 2019: gemiddeld 163,9 kg in makuuchi, via [Japan Times](https://japantimes.co.jp/sports/2019/05/22/sumo/sumo-101-large-rikishi) en [nippon.com](https://nippon.com/en/features/jg00073) | B | zeer |
| 3 | een Shetlandpony | 175 kg | 150–200 kg | 1,1 | [Hellabrunn](https://hellabrunn.de/en/animals/europe/shetland-pony): 150–200 kg; stamboek weegt niet | A | zeer |
| 4 | een piano (buffetpiano) | 240 kg | 220–270 kg | 1,4 | Steinway UK, Essex EUP116E: 236–242 kg ([steinway.co.uk](https://steinway.co.uk/?p=1888)); Steinway K-52: 272 kg | A | zeer |
| 5 | een vleugel | 340 kg | 250–350 kg | 1,4 | Steinway Model B ~345 kg, Model S 252 kg ([steinway.co.uk](https://steinway.co.uk/pianos/grand-pianos/)) | A | ja |
| 6 | een concertvleugel | 480 kg | 450–480 kg | 1,4 | Steinway UK FAQ: grootste vleugel 480 kg ([steinway.co.uk](https://steinway.co.uk/?p=1831)) | A | ja |
| 7 | een ijsbeer | 450 kg | mannetje 350–650 kg | 0,95 | [SeaWorld](https://seaworld.org/animals/facts/mammals/polar-bear/) 350–650 kg; [Britannica](https://www.britannica.com/animal/polar-bear) 410–720 kg | A/B | zeer |
| 8 | een Fries paard | 600 kg | 540–635 kg | 1,25 | rasprofiel [Mad Barn](https://madbarn.com/friesian-horse-breed-profile/) (~590 kg); KFPS geeft geen gewicht | C | zeer (NL) |
| 9 | een Friese koe (Holstein-Friesian) | 650 kg | 600–770 kg | 1,1 | encyclopedie 680–770 kg; CBS-factsheet: "1.200 kg … net zoveel als twee koeien" ([CBS 2015](https://www.cbs.nl/-/media/imported/documents/2015/14/factsheet-nieuwe-personenautos-in-nederland.pdf?la=nl-nl)) | B | zeer (NL) |
| 10 | een kuub water (1 m³) | 1.000 kg | exact | 1,5 | definitie (1 liter water ≈ 1 kg) | A | leuk-nerdy |
| 11 | een kleine auto | 1.000 kg | 900–1.150 kg | 1,0 | eigen schatting (A-segment) | C | ja |
| 12 | een gemiddelde Nederlandse auto | 1.250 kg | — | 1,25 | CBS: gemiddeld 1.254 kg op 1-1-2025 ([CBS](https://www.cbs.nl/nl-nl/nieuws/2025/18/personenauto-s-steeds-langer-breder-en-zwaarder)) | A | ja |
| 13 | een giraf | 1.300 kg | 800–1.360 kg | 1,05 | [Smithsonian](https://movementoflife.si.edu/species/giraffe): 1.360 kg (mannetje, max) | A | zeer |
| 14 | een nijlpaard | 1.500 kg | ♂ gem. 1.546, tot 2.065 kg | 1,15 | [San Diego Zoo factsheet](https://ielc.libguides.com/sdzg/factsheets/hippopotamus/characteristics) | A | zeer |
| 15 | een nieuwe auto | 1.550 kg | — | 1,0 | CBS: auto's bouwjaar 2024 gem. 1.554 kg (zelfde CBS-bericht) | A | ja |
| 16 | een SUV | 2.000 kg | 1.700–2.500 kg | 1,3 | eigen schatting | C | ja |
| 17 | een lege zeecontainer (20 voet) | 2.200 kg | 2.050–2.300 kg | 1,1 | ISO-container, tarra op CSC-plaat; o.a. [Ship4wd](https://ship4wd.com/logistics-shipping/20ft-40ft-shipping-containers-specs), [BSL](https://www.bslcontainers.com/faq/container-technical-specifications/) | B | ja (NL/haven) |
| 18 | een neushoorn (witte) | 2.300 kg | ♀ 1.800, ♂ 2.300 kg | 1,05 | [San Diego Zoo](https://ielc.libguides.com/sdzg/factsheets/whiterhino/characteristics) | A | zeer |
| 19 | een volgeladen bestelbus / camper | 3.500 kg | max. voor rijbewijs B | 1,5 | rijbewijs-B-grens 3.500 kg (Rijksoverheid/RDW, algemeen bekend; nog met link verifiëren) | B | ja |
| 20 | een lege zeecontainer (40 voet) | 3.750 kg | 3.620–3.900 kg | 1,07 | zelfde containerbronnen | B | ja |
| 21 | een Aziatische olifant | 4.000 kg | ♂ 4–5 t | 1,07 | [San Diego Zoo factsheet](https://ielc.libguides.com/sdzg/factsheets/asianelephant) (cijfer niet in snippet; 4–5 t uit meerdere bronnen) | B | zeer |
| 22 | een orka | 4.500 kg | ♂ 3,6–5,4 t, record 10 t | 1,1 | [SeaWorld](https://seaworld.org/animals/facts/mammals/killer-whale/); [NOAA](https://www.fisheries.noaa.gov/species/killer-whale): "tot 11 tons" | A | zeer |
| 23 | een Afrikaanse olifant | 6.000 kg | ♂ gem. 6 t; ♀ ~3 t | 1,33 | [San Diego Zoo factsheet](https://ielc.libguides.com/sdzg/factsheets/african_elephant/characteristics): mannetje gem. 6.000 kg | A | zeer |
| 24 | een T. rex | 9.000 kg | 8,4–14 t | 1,5 | Field Museum "SUE", studie 2011: ruim 9 ton ([ScienceDaily](https://www.sciencedaily.com/releases/2011/10/111012185634.htm)) | B | zeer |
| 25 | een stadsbus (12 m, leeg) | 11.000 kg | 10–12,5 t | 1,2 | eigen schatting; kentekendata VDL Citea LLE 9,9–11,5 m elektrisch: 10,3–10,9 t ([voertuig.net](https://voertuig.net/kenteken/72BZT4.pdf)) | C/B | ja |
| 26 | een brandweerauto | 15.000 kg | 13–18 t | 1,35 | eigen schatting | C | zeer |
| 27 | een volle vuilniswagen | 22.000 kg | 18–26 t | 1,45 | eigen schatting | C | ja |
| 28 | een bultrug | 30.000 kg | 25–36 t | 1,35 | [NOAA FAQ](https://www.nefsc.noaa.gov/faq/whales-faq.html): max ~40 short tons ≈ 36 t | A | zeer |
| 29 | een volle zeecontainer (20 voet) | 30.000 kg | max. 30.480 kg bruto | 1,0 | ISO 668-maximum 30.480 kg ([Conexwest](https://www.conexwest.com/blog/shipping-container-weight-capacity)) | B | ja |
| 30 | een tram (Amsterdam, Combino 13G) | 32.500 kg | 30–40 t | 1,08 | [TramTravels](https://tramtravels.com/tram/amsterdam-low-floor-articulated-tram-2014): 32,5 t (één fanbron) | C | ja (NL) |
| 31 | een Boeing 737 (leeg) | 41.000 kg | 737-800: 41,1–41,4 t | 1,26 | [Boeing 737NG Airport Planning](https://www.boeing.com/content/dam/boeing/v2/airports/acaps/737NG_REV_C.pdf) (cijfer via derden: [RocketRoute](https://rocketroute.com/aircraft/boeing-737-800), [AeroCorner](https://aerocorner.com/aircraft/boeing-737-800/)) | A/B | zeer |
| 32 | een volle vrachtwagen | 50.000 kg | wettelijk max. 50 t | 1,22 | [Rijksoverheid](https://www.rijksoverheid.nl/themas/verkeer-en-vervoer/verkeersveiligheid/veilig-rijden-in-de-vrachtwagen) | A | ja |
| 33 | een LZV ("ecocombi") | 60.000 kg | wettelijk max. 60 t | 1,2 | [Rijksoverheid](https://www.rijksoverheid.nl/themas/verkeer-en-vervoer/goederenvervoer/goederenvervoer-over-de-weg) | A | matig |
| 34 | een blauwe vinvis | 100.000 kg | gem. 70–150 t; grootste ≥136 t | 1,67 | NOAA via [Live Science](https://www.livescience.com/64459-blue-whale.html): grootste ≥136 t; [NAMMCO](https://nammco.no/blue-whale/) | A/B | zeer |
| 35 | het Vrijheidsbeeld | 204.000 kg | 204 t (225 short tons) | 2,0 | [NPS](https://www.nps.gov/stli/historyculture/statue-statistics.htm) en NPS-brochure | A | zeer |
| 36 | de Eiffeltoren (alleen ijzer) | 7.300.000 kg | 7.300 t ijzer; 10.100 t totaal | groot | [toureiffel.paris](https://www.toureiffel.paris/en/the-monument/key-figures) | A | zeer |

\* Rij 2/3: de Shetlander en de sumoworstelaar zitten allebei rond 160–175 kg en dichten samen de stap van
100 naar 240 kg (100 → 160 → 175 → 240 = 1,6×, 1,1× en 1,4×). Wie strikt ≤ 1,5× wil, voegt een object rond 130–150 kg
toe (bv. een "volwassen leeuw", ~190 kg mannetje, of een "reuzenpanda", ~100–150 kg; beide nog op te zoeken).
Voor de lange totalen tussen 100 t en 7.300 t zit nog een groot gat; dat vang je op met een vermenigvuldiger
("4 blauwe vinvissen", "12 Vrijheidsbeelden") of met extra objecten (bv. een Airbus A380 of een
binnenvaartschip; niet onderzocht).

Niet in de lijst omdat ze te licht zijn (< 100 kg): wasmachine (~60–85 kg, eigen schatting: geen bron
gevonden), koelkast, bakfiets (~30–50 kg), stroopwafels. Die werken alleen als vermenigvuldiger ("je tilde
120 wasmachines"): leuk, maar grote getallen zeggen minder dan één groot object.

## 3. Valkuilen

- **Merknamen.** "Je tilde een Tesla" gebruikt een merk in beschrijvende zin; dat is meestal geen
  merkinbreuk, maar het kan wel suggereren dat er een band met het merk is, en het gewicht verschilt per
  model en jaar (een Tesla Model 3 en Model X schelen honderden kilo's). Advies: **generiek** ("een kleine
  auto", "een SUV", "een concertvleugel" in plaats van "een Steinway"). Uitzonderingen kunnen bij
  objecten die zelf een unieke naam zijn en geen product dat je koopt (Eiffeltoren, Vrijheidsbeeld) of bij
  een generiek type (Boeing 737 is strikt genomen een merk + type; "een passagiersvliegtuig" is veiliger).
  Dit is geen juridisch advies.
- **Dieren met grote spreiding.** Olifant (koe ~3 t, stier ~6 t), ijsbeer (350–720 kg), orka (3,6–10 t).
  Kies een typisch, liefst iets lager getal en benoem het dier zo dat het klopt ("een olifantenstier",
  "een volwassen nijlpaard"). Liever te laag dan te hoog: "je tilde meer dan een olifant" moet waar zijn.
- **Bronnen die elkaar tegenspreken.** Fries paard, tram, stadsbus en brandweerauto hebben geen primaire
  bron gevonden (klasse C). Verifieer die vóór gebruik, of laat ze weg.
- **Short tons.** Amerikaanse bronnen (NOAA, NPS) gebruiken "tons" = 907 kg. Altijd omrekenen.
- **Afronding en formulering.** Nooit naar boven afronden. Voorbeelden:
  - verhouding 0,9–1,0: "bijna een …"; 1,0–1,2: "een …" of "ruim een …";
  - 1,2–1,8: "anderhalve …" (alleen bij ≥ 1,5) of "meer dan een …";
  - ≥ 2: naar beneden op een heel of half getal: "2,5 keer een …", "drie …".
  - Getallen in het Nederlands met komma (1,5) en punt als duizendtal (7.300).
- **Eenheid.** Onder 1.000 kg in kg ("840 kg"); vanaf 1.000 kg in ton met één decimaal ("4,2 ton"); vanaf
  ~100 ton zonder decimaal. Nooit "tonnen" (spreektaal voor geld).
- **Volume is geen kracht.** De app vergelijkt kracht met e1RM; het feitje gaat over werk (volume). Houd
  het feitje los van de vergelijkingen per spier, zodat het niet als prestatiemaat gelezen wordt.
- **Ontbrekend lichaamsgewicht** maakt het volume lager (sets zonder effectief gewicht tellen niet). Het
  feitje klopt dan nog steeds ("meer dan"), maar onderschat.

## 4. Aanbeveling: welk object kiezen?

**Formule (voorstel).**

1. Bereken voor elk object de verhouding `r = volume / gewicht`.
2. Kandidaten = objecten met `1 ≤ r < 3` (dan is de zin zonder grote getallen te zeggen). Is er geen
   kandidaat (volume boven het zwaarste object), gebruik het zwaarste object met `r` afgerond naar beneden.
   Onder het lichtste object: "bijna een …" bij `r ≥ 0,8`, anders geen feitje.
3. Geef een kleine voorkeur aan `r` dicht bij 1 of bij een mooi getal (1,5; 2; 2,5): die zinnen lezen het
   best.
4. **Afwisseling:** kies willekeurig uit de kandidaten, maar
   - stabiel per sessie (seed = sessie-id), zodat dezelfde samenvatting altijd hetzelfde feitje toont;
   - sla de laatste 3–5 gebruikte objecten over (bewaren in de database of afleiden uit vorige sessies);
   - met trapsgewijze stappen van ≤ 1,5× zijn er bij elk volume in het hoofdbereik meestal 2–4 kandidaten
     (bij `1 ≤ r < 3`), genoeg voor afwisseling.
5. Voor lange totalen ("deze week", "sinds je begon") dezelfde regel, met de zware objecten (bultrug,
   Boeing 737, vrachtwagen, blauwe vinvis, Vrijheidsbeeld, Eiffeltoren) en vermenigvuldigers.

Alternatief "het zwaarste object ≤ volume" is eenvoudiger en altijd waar, maar geeft bij hetzelfde
volumebereik steeds hetzelfde feitje. Met de kandidatenregel + willekeur blijft het verrassend.

**Data:** zet de ladder als vaste lijst in `core/` (naam enkelvoud/meervoud, gewicht in kg, categorie), met
een test die controleert dat de stappen ≤ 1,5× (of 1,75×) zijn. Alleen objecten van klasse A/B in de eerste
versie; klasse C pas na verificatie.

## Open punten

- Klasse-C-gewichten verifiëren (Fries paard bij KFPS, tram bij GVB/CAF/Siemens, stadsbus bij VDL,
  brandweerauto, vuilniswagen, scooter, kleine auto, SUV).
- Gat 100 → 165 kg en boven 200 ton eventueel opvullen.
- Beslissen of het feitje per training, per week en/of "sinds je begon" verschijnt (bepaalt welke delen van
  de ladder nodig zijn).
