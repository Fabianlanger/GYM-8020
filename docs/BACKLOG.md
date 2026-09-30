# Backlog

Punten uit gebruik (na v0.6) en open beslissingen. Per punt: wat er gebeurt, de oorzaak voor zover
gevonden in de code, een voorstel, en wat nog besloten moet worden.

Status: **B** = bug, **V** = verbetering, **O** = onderzoek nodig.

---

## B-1 · "Volgende keer": + bij zwaar gaat van 85 naar 80

**Wat:** bij de zware set in "Volgende keer" op + tikken zet het gewicht lager in plaats van hoger.

**Oorzaak (gevonden):** + vraagt het materiaal om "het volgende haalbare gewicht boven 85". Bestaat dat niet,
omdat 85 boven het maximum van het materiaal ligt, dan geeft de rekenregel het **maximum** terug, en dat is
lager (`WeightCalculator.up`, `core/.../material/Material.kt`). Voorbeelden waar dat kan gebeuren:
- Kabel: standaardprofiel "Kabel per 2,5 kg" heeft een maximum van **80 kg**. Dit past precies bij 85 → 80.
- Pin-stack: maximum 120 kg. Dumbbells: maximum 40 kg.
- Te weinig schijven aangevinkt in de introductie of onder Materiaal.

**Voorstel:**
1. + en − gaan nooit de verkeerde kant op. Boven het maximum blijft + staan.
2. Staat het gewicht boven wat het materiaal kan, dan toont de app dat: *"Boven het maximum van Kabel (80 kg).
   Pas het materiaal aan?"*, met een link naar het profiel.
3. Unittest voor precies dit geval.

**Te checken met jou:** welke oefening was het, en welk materiaal staat erbij? Dan weten we zeker dat dit de
oorzaak is.

---

## B-2 · Toetsenbord springt open na GO

**Wat:** na GO en het aftellen verschijnt het toetsenbord.

**Waarschijnlijke oorzaak:** na het aftellen verdwijnen de GO-knop en de doelkaart. Android geeft de "focus"
dan aan het eerstvolgende invoerveld, het gewicht of de reps van de open set, en opent het toetsenbord.

**Voorstel:** na het aftellen de focus expliciet wissen en het toetsenbord dicht houden. Het toetsenbord komt
alleen als je zelf in een veld tikt. De +/−-knoppen blijven zonder toetsenbord werken.

---

## V-1 · Scherm mag slapen na het aftellen

**Wat:** nu blijft het scherm tijdens de hele training aan. Na het aftellen mag het uit, maar je wilt snel
weer verder zodra je de telefoon pakt.

**Voorstel:**
- Scherm aan tot en met het aftellen. Daarna geeft de app het scherm vrij en gaat het uit volgens de
  gewone Android-instelling.
- Tijdens een lopende training toont de app zich **boven het vergrendelscherm**. Knop indrukken of scherm
  aantikken, en je zit direct in je training, zonder pincode. Na de training (of als je de app verlaat) is
  dat weer uit. Dit is hoe hardloop- en navigatie-apps het doen.
- De timers lopen gewoon door; die rekenen met tijdstempels.

**Te besluiten:**
- Boven het vergrendelscherm tonen, ja of nee? Het nadeel is dat iemand met je telefoon je training kan zien
  en aanpassen, maar niet de rest van je telefoon.
- Tijdens de warming-up (vóór GO) scherm aan houden? Mijn advies: ja.

---

## B-3 · Set toevoegen bij "Volgende keer" is altijd zwaar

**Wat:** een toegevoegde set staat op Z(waar) en je kunt hem niet op L(icht) zetten. Tikken op de Z doet niets.

**Oorzaak (gevonden):** in de planner voor volgende keer is het rol-label alleen een plaatje, geen knop. In de
set-editor van de training kan wisselen wel.

**Voorstel:** tik op het label: zwaar → licht → warming-up → zwaar. Dat is hetzelfde gedrag als in het nieuwe
bewerkscherm, dus overal gelijk. Het gewicht van een nieuwe lichte set wordt dan meteen een voorstel op basis
van de vorige lichte set.

---

## V-2 · Samenvatting: kracht in plaats van tijd per oefening

**Wat:** "Tijd per oefening" in het samenvattingsscherm zegt je weinig. Je wilt zien hoeveel sterker je bent
geworden, en hoe sterk je bent.

**Voorstel voor de samenvatting:**

1. **Per oefening** één regel met de verandering t.o.v. de vorige keer:
   - **e1RM** van de zware set (geschatte 1-rep-max, Epley). Bijv. *Squat 112 kg (+2,5 kg · +2%)*.
   - **Totaal verzet gewicht** (gewicht × reps, werksets). Bijv. *1.460 kg (+80 kg)*.
   - Bij oefeningen zonder gewicht: reps t.o.v. vorige keer.
2. **Tijden samengevat** in één regel: gemiddelde trainingstijd per oefening en gemiddelde wisseltijd.
3. **Eén totaalgetal voor kracht**, zie O-1.

---

## O-1 · Eén getal voor totale kracht

**Vraag:** kan er één geaggregeerd krachtgetal komen? Is daar onderzoek naar, of moeten we het slim bedenken?

**Wat er bestaat:**
- **Powerlifting-scores** zoals Wilks, DOTS en IPF GL: het totaal van squat + bench + deadlift, gecorrigeerd
  voor lichaamsgewicht (en geslacht). Goed onderbouwd, maar alleen voor die drie oefeningen. Jouw Full body
  heeft geen deadlift en een incline bench.
- **Relatieve kracht** (e1RM ÷ lichaamsgewicht) per oefening, met normtabellen (beginner → elite). Handig
  per oefening, maar niet als één getal.

**Voorstel (zelf bedacht, eenvoudig uit te leggen): "Krachtindex"**
- Per oefening: je huidige e1RM (beste zware set, gemiddeld over de laatste 2 keer tegen ruis) gedeeld door
  je e1RM van je **eerste 4 weken** met die oefening.
- De index is het **meetkundig gemiddelde** van die verhoudingen over alle oefeningen die je de laatste
  8 weken deed, × 100.
- **100 = je beginniveau.** 112 betekent gemiddeld 12% sterker dan toen je begon. Een nieuwe oefening telt
  pas mee na 4 weken, dus die verpest het getal niet.
- Met het meetkundige gemiddelde telt elke oefening even zwaar, of het nu een squat van 120 kg is of een
  curl van 20 kg.
- **Optioneel** daarnaast een **DOTS-score** als je squat, bench en deadlift doet en je lichaamsgewicht is
  ingevuld: het getal waarmee je je vergelijkt met anderen.

**Te besluiten:** Krachtindex zoals voorgesteld? En DOTS erbij, ja of nee? In de samenvatting komt dan
*"Krachtindex 112 (+0,8 t.o.v. vorige training)"*, in het dashboard een lijn door de tijd.

---

## V-3 · Crunches: reps als doel voor volgende keer

**Wat:** bij oefeningen die op herhalingen gaan (crunch, decline crunch, hanging leg raise) kun je bij
"Volgende keer" geen aantal reps als doel zetten.

**Oorzaak:** het plan voor volgende keer slaat per set alleen een **gewicht** op. Bij rep-oefeningen toont de
planner daarom alleen "tot falen" of de standaard-streefwaarde.

**Voorstel:**
- Per set een **reps-doel** bij rep-oefeningen. Standaard is dat vorige keer + 1. Het is aan te passen met
  +/−, net als gewicht.
- In de training toont de doelkaart dan *"Doel: 22 reps"*, en "doel gehaald" vergelijkt met dat doel in plaats
  van met vorige keer.
- Hiervoor is een kleine database-aanpassing nodig: schema versie 3, een kolom in het plan. De migratie gaat
  automatisch en je data blijft behouden.

---

## B-4 · Pull-ups: "alleen lichaamsgewicht" is geen warming-up

**Wat:** bij optrekken zegt de warming-upknop "Alleen lichaamsgewicht". Voor pull-ups is dat al een zware set.

**Oorzaak:** de warming-uptip voor alle oefeningen met eigen gewicht is "alleen lichaamsgewicht". Bij dips en
push-ups klopt dat ongeveer, bij pull-ups niet.

**Voorstel:**
- Oefeningen met eigen gewicht krijgen een warming-up met **ondersteuning**. Met het profiel "Assisted
  machine": ongeveer de helft van je lichaamsgewicht als tegengewicht, afgerond op een haalbare stap.
  Bijv. *"Warming-up: −40 kg ondersteuning"*.
- Zonder assisted machine: *"Warming-up: lat pulldown licht, of een elastiek"*.
- Push-ups en crunches houden "alleen lichaamsgewicht" of "rustig tempo".

**Te besluiten:** is de helft van je lichaamsgewicht als ondersteuning een goede vuistregel, of liever een vast
aantal kilo's dat je per oefening instelt?

---

## Nog open uit eerder

| # | Onderwerp | Stuk |
|---|---|---|
| R-1 | Spierpercentages: 100 / 50 houden + 25% stabilisator, bibliotheek bijstellen | `docs/onderzoek-spieren-en-zwaarte.md` |
| D-20 | Optioneel "gestopt vóór falen" per set; doel en volume rekenen daarmee | idem |
| – | Screenshots voor de README; repo publiek maken | – |

## Voorgestelde volgorde

1. **v0.6.1 (snelle fixes):** B-1, B-2, B-3, B-4, V-1. Kleine wijzigingen, direct merkbaar in de gym.
2. **v0.7 (kracht):** V-2, O-1, V-3, samen met R-1 en D-20. Die raken allemaal hoe de app kracht en
   vooruitgang rekent, dus die ontwerp je in één keer.
