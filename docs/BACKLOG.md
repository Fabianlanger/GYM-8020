# Backlog

Punten uit gebruik (na v0.6) en open beslissingen. Per punt: wat er gebeurt, de oorzaak voor zover
gevonden in de code, een voorstel, en wat nog besloten moet worden.

Status: **B** = bug, **V** = verbetering, **O** = onderzoek nodig.

---

## O-2 · Materiaal ondersteunt, maar mag niet beperken (was B-1)

**Wat:** bij "Volgende keer" ging + van 85 naar 80. Het was een kabeloefening.

**Oorzaak (bevestigd):** het standaardprofiel "Kabel per 2,5 kg" heeft een maximum van 80 kg. Boven het
maximum geeft + het maximum terug. Volgens de regels van de app is dat logisch, maar **de kabel in jouw gym
gaat verder dan 80**. Het profiel klopt dus niet met de werkelijkheid, en de app gaat dan in de weg zitten.

**Uitgangspunt (afgesproken):** de app is ondersteunend. Materiaalprofielen helpen met haalbare gewichten
en schijven, maar **mogen je nooit tegenhouden**. Niemand weet vooraf welke apparatuur er in elke gym staat.

**Uit te zoeken:** hoe we dat vormgeven. Richtingen:
1. **+ en − gaan nooit de verkeerde kant op.** Boven het maximum gaat + door in dezelfde stap (80 → 82,5 → 85).
2. **Zelf typen mag altijd**, ook buiten het profiel. De app corrigeert niets stilletjes.
3. **Het profiel leert mee:** gebruik je een gewicht boven het maximum, dan vraagt de app één keer
   *"Kabel gaat verder dan 80 kg? Maximum ophogen naar 85?"*. Eén tik, of negeren.
4. **Ruimere standaardprofielen**, of profielen zonder maximum ("onbekend") totdat je iets invult.
5. **Buiten bereik subtiel tonen** in plaats van blokkeren, bijv. een klein label "buiten profiel".

**Test (volgens TESTING.md):** regel "+ geeft nooit een lager gewicht" als property-test, plus een test
met precies dit geval (kabel, max 80, + vanaf 85).

**Te besluiten:** welke combinatie van 1–5. Mijn voorlopige advies is 1 + 2 + 3.

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

## Testen (zie `TESTING.md`)

| # | Wat | Status |
|---|---|---|
| T-1 | Regel "eerst een rode test, dan de fix" toepassen op alle B-punten hierboven | afgesproken, vanaf de volgende fix |
| T-2 | Property-tests op de rekenlogica (regels uit TESTING.md §2) | gepland |
| T-3 | Mutatietesten (PIT) op `core/`: eerst meten, dan per release verbeteren | gepland |
| T-4 | UI-tests met Compose via Robolectric voor de kritieke flows F-1, F-2, F-3, F-9 | gepland |
| T-5 | Screenshottests met Roborazzi: referentie-screenshots, verschil maakt de build rood, screenshots als download | gepland |
| T-6 | End-to-end op een emulator (Maestro), 2–3 rooktests bij een versierelease | later |

## Voorgestelde volgorde

1. **v0.6.1 (snelle fixes):** B-2, B-3, B-4, V-1, plus O-2 zodra besloten. Samen met T-2, T-4 en T-5, zodat
   de fixes meteen volgens de nieuwe teststrategie gaan.
2. **v0.7 (kracht):** V-2, O-1, V-3, samen met R-1 en D-20. Die raken allemaal hoe de app kracht en
   vooruitgang rekent, dus die ontwerp je in één keer. T-3 erbij.
