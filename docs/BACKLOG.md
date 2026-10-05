# Backlog

Punten uit gebruik (na v0.6) en open beslissingen. Per punt: wat er gebeurt, de oorzaak voor zover
gevonden in de code, een voorstel, en wat nog besloten moet worden.

Status: **B** = bug, **V** = verbetering, **O** = onderzoek nodig.

---

## O-2 · Materiaal ondersteunt, maar mag niet beperken (was B-1) — *deels gedaan in v0.6.1*

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

**Gedaan in v0.6.1:** 1 en 2. Buiten het bereik toont de app nu *"Buiten het bereik van dit materiaal"* in
plaats van een correctie (richting 5). Tests: `MaterialRulesTest`. Die vond meteen een tweede fout: − op een
stang kon vanaf een negatief gewicht naar 20 kg springen.

**Nog te besluiten:** 3 (maximum laten meegroeien) en 4 (ruimere standaardprofielen).

---

## B-2 · Toetsenbord springt open na GO — *gedaan in v0.6.1*

**Gedaan in v0.6.1:** focus wissen bij GO en na het aftellen, en het toetsenbord opent niet meer vanzelf als het venster focus krijgt. **Nog geen automatische test** (dat komt met T-4); handmatig nalopen bij de release.

**Wat:** na GO en het aftellen verschijnt het toetsenbord.

**Waarschijnlijke oorzaak:** na het aftellen verdwijnen de GO-knop en de doelkaart. Android geeft de "focus"
dan aan het eerstvolgende invoerveld, het gewicht of de reps van de open set, en opent het toetsenbord.

**Voorstel:** na het aftellen de focus expliciet wissen en het toetsenbord dicht houden. Het toetsenbord komt
alleen als je zelf in een veld tikt. De +/−-knoppen blijven zonder toetsenbord werken.

---

## V-1 · Scherm mag slapen na het aftellen — *gedaan in v0.6.1*

**Gedaan in v0.6.1 (D-84):** scherm aan vóór GO en tijdens het aftellen, daarna vrij; boven het vergrendelscherm alleen zolang het trainingsscherm open is. Handmatig nalopen bij de release.

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

## B-3 · Set toevoegen bij "Volgende keer" is altijd zwaar — *gedaan in v0.6.1*

**Gedaan in v0.6.1:** tik op het label wisselt zwaar → licht → warming-up. Een lichte set neemt het gewicht van je andere lichte set over. Test: `PlanRolesTest`.

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

## B-4 · Pull-ups: "alleen lichaamsgewicht" is geen warming-up — *gedaan in v0.6.1*

**Gedaan in v0.6.1 (D-83):** standaard 50% van je gewicht als ondersteuning, instelbaar in Instellingen → Methode. Per oefening instellen kan nog niet. Test: `MaterialRulesTest`.

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

## V-4 · Training achteraf invoeren (D-80)

**Wat:** een training toevoegen die je niet live in de app hebt bijgehouden. Bijv. telefoon vergeten, of oude
trainingen uit aantekeningen. Je weet niet alle getallen meer precies, maar je wilt wel de aanknopingspunten
vastleggen.

**Voorstel voor de flow:**
1. Dashboard → Trainingen → **"+ Training toevoegen"**.
2. Kies **datum en tijd** (standaard vandaag) en een **schema**, of "leeg" om zelf oefeningen te kiezen.
3. De app maakt een afgeronde training met de oefeningen van het schema. Per oefening staan de sets klaar,
   **voorgevuld met het gewicht van toen**: je laatste gewicht op of vóór die datum, zodat je alleen hoeft
   te corrigeren.
4. Dat opent het bestaande bewerkscherm (D-74): sets aanpassen, oefeningen toevoegen of verwijderen, duur
   invullen als je die weet.

**Onvolledig mag:**
- Een set zonder reps telt niet mee in statistieken, maar blijft staan als aantekening.
- Geen duur ingevuld: de training telt niet mee in de gemiddelde duur en de tijden per oefening.
- Handmatig ingevoerde sets krijgen **geen doel**, dus "doelen gehaald" verandert niet achteraf.

**Hoe het meetelt:**
- Telt mee voor het weekdoel, de dagherkenning, records, grafieken en de heatmap, op de ingevulde datum.
- Lichaamsgewicht: de meting op of vóór die datum.
- Is het je **nieuwste** training van een oefening, dan vraagt de app of het doel voor volgende keer opnieuw
  berekend moet worden (D-75). Bij een oudere datum blijft het doel staan.
- In het logboek krijgt hij een klein label **"achteraf ingevoerd"**. Daarvoor komt er een veld bij de
  training, een kleine database-migratie. Die kan mee met de migratie van V-3.

**Te besluiten:**
- Voorvullen met het gewicht van toen, of liever lege sets?
- Handmatige trainingen apart tonen in statistieken, of gewoon meetellen zoals hierboven?

**Tests (volgens TESTING.md):** nieuwe flow F-10 "training achteraf invoeren → telt mee op de juiste datum",
plus een test dat een oudere datum het doel niet verandert.

---

## V-5 · Importeren uit aantekeningen (D-81, later)

**Wat:** oude trainingen uit je aantekeningen in één keer inlezen in plaats van ze één voor één in te voeren.

**Richting:** plakken of een bestand kiezen met een eenvoudige schrijfwijze die je met de hand kunt typen.
Bijvoorbeeld:

```
2026-08-14 Full body
Squat 80x6, 60x16
Incline bench 50x7, 35x15
Pull-up +10x5, -20x12
```

De app toont eerst een **voorbeeld** van wat hij herkend heeft (oefeningen, sets, onbekende regels in rood).
Pas daarna importeer je. Oefeningsnamen worden gematcht op de bibliotheek, ook in het Nederlands en met
kleine typefouten.

**Eerst nodig:** een paar van je echte aantekeningen als voorbeeld. Die blijven buiten de repo, alleen om de
schrijfwijze op af te stemmen.

---

## O-3 · Wearables en gezondheidsdata (D-82)

**Wat:** koppelen met wearables (Garmin op nummer 1) en gezondheidsdata in de app gebruiken.

**Stand:** inventarisatie klaar in `docs/onderzoek-wearables.md`. Kern: **Health Connect** op Android
verbindt Garmin (sinds 2025, alleen Garmin → telefoon) en vrijwel alle andere merken, lokaal en zonder
server. De Garmin-cloud-API vraagt goedkeuring en een eigen server, en past daarom niet.

**Eerste stap (voorstel):** automatisch lichaamsgewicht (W-1) en hartslag per oefening (W-2).

**Tweerichtingsverkeer (gewenst):** trainingen terug naar Health Connect (W-5) voor Fitbit, Samsung, Google en
anderen. Voor Garmin een proef met FIT-export die je zelf importeert (W-8), en op termijn een eigen
horloge-app (W-9).

**Te besluiten:** O-3a t/m O-3d in het onderzoeksdocument.

---

## B-5 · Timer kwijt na draaien van het scherm

**Wat:** GO geklikt, telefoon kantelt, scherm draait naar liggend en terug, en de timer is weg.

**Oorzaak (vrijwel zeker):** bij draaien bouwt Android het scherm opnieuw op. De aftelling na GO wordt niet
onthouden, en de oefening start officieel (tijdstempel GO) pas aan het einde van de aftelling. Draai je
tijdens die 3 seconden, dan verdwijnt de aftelling en start de timer nooit. Na de aftelling overleeft de timer
een draai wel, want die rekent vanuit de database.

**Voorstel:**
1. App **altijd staand**. Liggend heeft in de gym geen nut, en dan kan het ook niet per ongeluk gebeuren.
2. Aftelling bewaren bij een herbouw (rememberSaveable), voor het geval Android het scherm om een andere reden
   herbouwt (bijv. donkere modus of taal wisselen).

**Test:** UI-test "draaien tijdens aftelling → timer start toch" (komt met T-4). Tot die tijd handmatig.

---

## V-6 · Schijven per materiaal of per gym

**Wat:** eenmalig instellen welke schijven er zijn (bijv. geen 25 kg). Daarna aanpassen in de instellingen,
of per oefening een uitzondering.

**Wat er al is:** Instellingen → Materiaal → Schijven, en stap 3 van de introductie. Daar zet je per gewicht
aan hoeveel schijven je gym heeft; 25 kg uit betekent dat de app er nooit mee rekent.

**Nieuw:**
1. **Per materiaalprofiel een eigen schijvenset** als uitzondering op de standaard van de gym. Bijv. een
   machine met eigen kleine schijfjes van 1, 2 en 5 kg. Per oefening kies je het profiel (dat kan al), zodat de
   uitzondering vanzelf meegaat.
2. **Meerdere gyms** (optioneel, later): per gym eigen schijven en materiaal, kiezen bij de start van een
   training. Handig op vakantie of bij een tweede sportschool.
3. De schijvenset en het materiaal duidelijker vindbaar maken, ook vanuit het oefeningscherm.

**Te besluiten:** alleen 1 en 3, of ook meerdere gyms (2)?

**Test:** regel "een voorgesteld gewicht is altijd op te leggen met de schijven van dát profiel" (T-2).

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
| T-1 | Regel "eerst een rode test, dan de fix" toepassen op alle B-punten hierboven | in gebruik sinds v0.6.1 |
| T-2 | Property-tests op de rekenlogica (regels uit TESTING.md §2) | gestart: + en − (v0.6.1); rest gepland |
| T-3 | Mutatietesten (PIT) op `core/`: eerst meten, dan per release verbeteren | gepland |
| T-4 | UI-tests met Compose via Robolectric voor de kritieke flows F-1, F-2, F-3, F-9 | gepland |
| T-5 | Screenshottests met Roborazzi: referentie-screenshots, verschil maakt de build rood, screenshots als download | gepland |
| T-6 | End-to-end op een emulator (Maestro), 2–3 rooktests bij een versierelease | later |

## V-7 · Termen gelijktrekken met de fitnesswereld (D-87)

**Wat:** "Schema" en "Weekplan" sluiten niet aan bij wat sporters en andere apps gebruiken.

**Onderzoek:** `onderzoek-terminologie.md`. Voorstel:
- Schema → **Workout**
- Weekplan → **Program / Programma**
- een gedane training → **Session** (NL: Training)
- Volgende keer → **Next target**
- Material → **Equipment**

Zwaar/licht blijft; in de uitleg noemen we het "top set + back-off set".

**Hoe:** alleen de teksten (EN + NL), de methodepagina, de introductie en de documentatie. **Niet** de code of
de database (A-11). In de SPEC komt een woordenlijst met het verband tussen intern en scherm.

**Te besluiten:** vragen 19–21 (ROADMAP).

**Test:** screenshottests (T-5) vangen vergeten teksten; een simpele check dat er geen "schema" meer in de
Engelse teksten staat.

---

## V-8 · Engels als standaardtaal, Nederlands te kiezen (D-88)

**Wat:** de app is standaard Engels; Nederlands kies je in Instellingen (en op Android 13+ ook via de
systeeminstellingen per app).

**Nu:** de app volgt de taal van de telefoon, er is geen taalknop.

**Afhankelijkheid (A-10):** standaardoefeningen, -workouts en -programma's staan nu in de database in de taal
van de eerste start. Bij een taalwissel moeten ze meevertalen: namen van bibliotheekitems tonen via hun vaste
sleutel in de gekozen taal. Zelf gewijzigde namen blijven bewaard.

**Te besluiten:** vraag 22.

**Test:** taal wisselen → bibliotheeknamen en teksten wisselen mee, eigen namen niet.

---

## V-9 · Rondleiding door de eerste workout (D-89)

**Wat:** nieuwe gebruikers krijgen bij de eerste start de introductie, maar daarna niet de uitleg van de
training zelf. Bij hun **eerste workout** (meestal Full body) loopt een korte rondleiding mee:
- het onderdeel dat aan de beurt is **wiebelt of pulseert** zacht;
- een kleine pop-up legt het in één of twee zinnen uit;
- tik om naar de volgende stap te gaan;
- overslaan kan altijd, en je kunt de rondleiding opnieuw bekijken via Instellingen.

**Stappen in de "happy flow"** (voorstel, nog te testen in het prototype):
1. Beginscherm: de kaart "Vandaag" → workout starten.
2. Doelkaart: het gewicht voor vandaag en welke schijven erop gaan.
3. Warming-up (optioneel).
4. **GO**: aftellen, en de timer loopt.
5. Reps invullen bij de **zware set**: tot falen, streef naar ±6.
6. Het signaal: te licht / goed / te zwaar.
7. De **lichte set**: tot falen, streef naar ±16.
8. **Volgende doel**: het gewicht voor de volgende keer, met een voorstel van de app.
9. Swipen naar de volgende oefening (het lint aan de zijkant).
10. Training afronden → samenvatting (records, heatmap).

**Ideeën om mee te nemen:**
- **Contextueel:** een stap verschijnt pas als dat onderdeel voor het eerst in beeld komt, niet alles in één
  keer.
- **Hooguit ~10 korte stappen**, nooit iets blokkeren.
- **Demo-workout** om te oefenen of te laten zien aan iemand anders, zonder dat er iets wordt opgeslagen.
  Handig bij het presenteren van de app.

**Eerst een prototype:** een klikbaar prototype op telefoonformaat (HTML), om de stappen, teksten en het
wiebelen te testen vóór we het in de app bouwen.

**Afhankelijkheden:**
- komt ná de schermwijzigingen van 0.7/0.8, anders bouwen we de rondleiding twee keer;
- komt ná V-7 (termen) en V-8 (taal), omdat de teksten daarop leunen;
- UI-tests (T-4) controleren dat elke stap zijn onderdeel vindt.

**Te besluiten:** vragen 23–25 (ROADMAP).

**Test:** nieuwe flow F-11: de rondleiding verschijnt één keer, is over te slaan en opnieuw te starten, en
verschijnt niet bij bestaande gebruikers.

---

## V-10 · Snelknop op het beginscherm: lichaamsgewicht invullen (D-91)

**Wat:** je wilt je gewicht elke dag invullen, zonder eerst een workout te starten of via het dashboard te
gaan.

**Nu:** gewicht invullen kan via Dashboard → Lichaamsgewicht, of via de vraag bij het starten van een workout
(als je meting ouder is dan 7 dagen).

**Voorstel:**
- Op het beginscherm een kleine kaart of chip: **"Gewicht vandaag"**.
  - Nog niet ingevuld: *"Invullen"*.
  - Al ingevuld: *"81,4 kg ✓"*. Tikken = aanpassen.
- Tikken opent direct een klein invulvenster op het beginscherm, **voorgevuld met je laatste meting**, met
  +/− per 0,1. Opslaan, klaar. Twee tikken als het gewicht gelijk is.
- Een klein lijntje met de trend van de laatste dagen (optioneel, als het niet druk wordt).
- Daarna vraagt de workout niet meer naar je gewicht; dat werkt al zo.

**Samenhang:**
- De herinnering "Lichaamsgewicht" (D-76) is nu wekelijks. Met dagelijks wegen past een keuze
  **dagelijks / wekelijks** beter (vraag 28).
- W-1 (gewicht automatisch uit Garmin of weegschaal, 0.10) vult dezelfde gegevens. De knop blijft dan nodig
  om handmatig te corrigeren. Daarom krijgt een meting later een **herkomst** (handmatig / wearable); zie A-14.

**Te besluiten:** vragen 27–28 (ROADMAP).

**Test:** gewicht via de snelknop opslaan → staat in de lijst, de workout vraagt niet meer; tweede keer op
dezelfde dag = aanpassen, geen dubbele meting (regeltest).

---

## O-5 · Naam van de app (later)

De eigenaar denkt erover de app anders te noemen. Voorlopig blijft het **GYM-8020**. Uitzoeken vóór 1.0,
omdat de naam in de pakketnaam, het icoon en de store-vermelding terechtkomt: na publicatie is hij moeilijk te
veranderen.

---

## Proces

| # | Wat | Status |
|---|---|---|
| P-1 | Werkwijze-upgrade naar de aanpak van Matt Pocock: eerst uitzoeken wat hij doet en wat we overnemen | later |
| A-8 | Eén ondertekeningssleutel voor dev- en versiebuilds (eenmalige overstap met export → import) | vóór 0.10 (wearables), zie ROADMAP |

## Volgorde

Zie **`ROADMAP.md`**: welke punten in welke release, de open vragen per release en de afhankelijkheden.
