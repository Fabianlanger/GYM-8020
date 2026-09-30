# Specificatie — GYM-8020

> Status: **v0.5 — goedgekeurd** (zie sectie 18 voor wat nog openstaat) — nog niet goedgekeurd. Er wordt niets gebouwd voordat de eigenaar dit document heeft goedgekeurd.
> Datum: 2026-09-28
> Eigenaar: Fabian Langer · ontwikkeld onder de vlag van Isatis Business Solutions (IBS)

## Hoe dit document te lezen

Elk besluit heeft een nummer (`D-xx`) en een status:

- **[BESLOTEN]** — door de eigenaar zo gevraagd.
- **[VOORSTEL]** — nog niet bevestigd; dit is het standaardantwoord dat gebouwd wordt tenzij de eigenaar het aanpast. Reageer met bijv. *"D-12 anders: …"*.
- **[OPEN]** — hier is input van de eigenaar nodig; zonder die input kan dit niet goed gebouwd worden.

---

## 1. Doel en uitgangspunten

Een Android-app (APK) die krachttraining bijhoudt volgens een vaste methode: per oefening sets tot failure, en na elke oefening een **doel voor de volgende keer** vastleggen, zodat je jezelf eerlijk houdt en ziet of je jezelf pusht.

| # | Uitgangspunt | Status |
|---|---|---|
| D-01 | Android-app, geleverd als **APK** die lokaal geïnstalleerd wordt (geen Play Store). | [BESLOTEN] |
| D-02 | **Alle data lokaal op de telefoon.** Geen server, geen account, geen login. | [BESLOTEN] |
| D-03 | Code zo opzetten dat later een externe database/sync gekoppeld kan worden. Die optie zit dan **verstopt in Instellingen** en valt verder nergens lastig. | [BESLOTEN] |
| D-04 | De app moet **de training van de eigenaar snel en direct** ondersteunen (sectie 13), maar is **generiek genoeg om publiek te delen**: methode-instellingen zijn standaardwaarden, geen hardcoded regels. | [BESLOTEN] |
| D-05 | Repo onder het persoonlijke GitHub-account van de eigenaar. **Tijdens ontwikkeling privé, later publiek.** Alles wordt vanaf dag 1 ingericht alsof de repo publiek is (geen geheimen, geen persoonsgegevens, MIT-licentie). | [BESLOTEN] |
| D-06 | Iteratief bouwen, in fases (sectie 16). Elke fase levert een installeerbare APK op. | [BESLOTEN] |
| D-06a | **Geen persoonsgegevens en geen persoonlijke trainingsdata in de repo.** Materiaal, gewichten, lichaamsgewicht, doelen en historie voert elke gebruiker zelf in de app in. **Wel** meegeleverd: generieke standaardoefeningen, standaardschema's en standaard-weekplannen (sectie 9) — zonder gewichten, zonder naam van een persoon. | [BESLOTEN] |

---

## 2. Begrippen

**Hiërarchie:** Weekplan → Schema → Oefening → Set → reps.

| Begrip | Betekenis |
|---|---|
| **Weekplan** | Je weekindeling: welke schema's je in een week doet, een target (aantal trainingen per week) en optioneel op welke dagen. Zie sectie 9. |
| **Oefening** | Een beweging, bijv. *Bench press*, *Assisted pull-up*, *Crunch*. Heeft een belastingstype, materiaal en spiergroepen. |
| **Set** | Eén reeks herhalingen tot failure. Heeft een **rol** (zwaar/licht), een gewicht en een aantal reps. |
| **Rol** | *Zwaar* (standaard target 6 reps) of *Licht* (standaard target 16 reps). Later ook *Warm-up*. |
| **Rep-target** | Het aantal reps dat je bij die rol hoort te halen. Je gaat altijd tot failure; de target zegt achteraf of het gewicht goed gekozen was. |
| **Doel (volgende keer)** | Wat je na afloop van een oefening vastlegt voor de volgende keer: meestal een gewicht per set, soms een aantal reps. |
| **Schema** | Een pakket oefeningen in vaste volgorde, bijv. *Push*, *Pull*, *Benen*. |
| **Training (sessie)** | Eén keer sporten: een schema doorlopen, met tijdmeting. |
| **Effectief gewicht** | Het gewicht dat je werkelijk verplaatst, inclusief (een deel van) je lichaamsgewicht. Zie sectie 5. |
| **Materiaalprofiel** | Beschrijving van waar het gewicht vandaan komt: stang + schijven, pin-stack, dumbbells, … Zie sectie 7. |

---

## 3. De trainingsmethode (context)

> Uitgebreide uitleg komt in `docs/methode.md` (door de eigenaar te schrijven). README en app verwijzen daarnaar. Deze sectie is alleen de context die nodig is om de app goed te bouwen.

- Per oefening standaard **2 sets**, direct na elkaar met **zo min mogelijk pauze**.
- Set 1 is **zwaar** (weinig reps), set 2 is **licht** (veel reps). Elke set **tot failure**.
- De rep-targets (6 / 16) zijn **streefwaarden**, geen opdracht. Je gaat altijd tot failure; het aantal reps *ontstaat*. Je kiest het gewicht zo dat je in de buurt van 6 resp. 16 uitkomt. Ga je er ver boven, dan pak je de volgende keer zwaarder. **Vooruit plannen gebeurt op gewicht, niet op reps.** Dit is de kern van de methode.
- Soms een **3e (of 4e) set**; de app moet sets kunnen toevoegen.
- Omdat de sets direct na elkaar gaan, moet het wisselen van zwaar naar licht **snel** kunnen, bij voorkeur door **één schijf (per kant) eraf te halen**.
- Na de oefening leg je een **doel voor de volgende keer** vast (bijv. zwaarder pakken als je energie over had).
- Optioneel een **warm-up**: alleen reps, zonder (extra) gewicht (sectie 12).

| # | Besluit | Status |
|---|---|---|
| D-07 | Standaard rep-targets: **Zwaar = 6**, **Licht = 16**. | [BESLOTEN] |
| D-08 | Standaard 2 sets per oefening (zwaar, licht); knop **"+ Set"** om **tijdens de oefening, on the fly** extra sets toe te voegen (bijv. een klein extra setje als het gewenste gewicht niet lukte). | [BESLOTEN] |
| D-09 | Rep-targets en standaard aantal sets zijn **globaal instelbaar in Instellingen** en **per oefening te overschrijven**. | [BESLOTEN] |
| D-10 | Een extra set krijgt standaard de rol **Zwaar** (target 6); de rol is per set om te zetten naar Licht. | [BESLOTEN] |

---

## 4. Oefeningen en belastingstypes

Elke oefening heeft precies één belastingstype, en daarnaast een instelling **progressie op gewicht of op reps**:

**Invoer bij *Lichaamsgewicht ±*:** één gewichtsveld met een **+/−-schakelaar** ervoor. Ondersteuning (assisted machine) is gewoon een negatief getal, extra gewicht een positief getal, 0 = puur lichaamsgewicht. Typ je een `-` terwijl de schakelaar op + staat, dan wordt het getal negatief (en springt de schakelaar om). Zo hoef je geen apart type te kiezen als je van assisted naar weighted doorgroeit: de progressie loopt als één doorlopende lijn van bijv. −30 kg naar +10 kg.

| Type | Voorbeeld | Effectief gewicht | Wat je invult |
|---|---|---|---|
| **Extern** | Bench press, leg press, cable row | = ingevuld gewicht | gewicht, reps |
| **Lichaamsgewicht ±** | Pull-up, dips, push-up; assisted (−) of met dipriem/vest (+) | = lichaamsgewicht × factor **+ extra** (extra is positief, negatief of 0) | extra gewicht met teken (± kg), reps |

**Lichaamsgewichtfactor** — het deel van je lichaamsgewicht dat je verplaatst. Voorgestelde standaardwaarden (per oefening aanpasbaar):

**Progressie op reps (variant van *Lichaamsgewicht ±*):** voor oefeningen die je typisch zonder gewicht doet (crunches, leg raises, planks-achtigen). Het ±-veld staat dan op 0 en is **ingeklapt**; je vult alleen reps in. Tik je het veld open en vul je toch gewicht in, dan kan dat gewoon. Zolang het extra gewicht 0 is, gaan doel, slim voorstel en vooruitgang over **reps**; zodra je gewicht toevoegt, over gewicht.

| Oefening | Factor | Toelichting |
|---|---|---|
| Pull-up / chin-up / (assisted) | 1,00 | je hangt volledig |
| Dips / (assisted) | 0,95 | onderarmen/handen dragen niet mee |
| Push-up | 0,65 | literatuurwaarde ~64% |
| Crunch / sit-up / leg raise | 0,30 | ruwe schatting (bovenlichaam), alleen voor volume in heatmap/dashboard; instelbaar |

| # | Besluit | Status |
|---|---|---|
| D-11 | Twee belastingstypes: **Extern** en **Lichaamsgewicht ±** (ondersteuning/extra als één getal met teken, +/−-schakelaar; 0 = puur lichaamsgewicht). "Alleen reps" is geen apart type meer maar een variant. | [BESLOTEN] |
| D-12 | Buikspieroefeningen zijn *Lichaamsgewicht ±* met **progressie op reps** (±-veld ingeklapt, 0 kg). Extra gewicht blijft mogelijk. | [BESLOTEN] |
| D-12a | Standaardfactor buikspieroefeningen 0,30 (schatting, alleen voor volume). | [BESLOTEN] |
| D-13 | Eigen oefeningen aanmaken, bewerken, archiveren (niet verwijderen als er historie is). | [BESLOTEN] |

---

## 5. Lichaamsgewicht en effectief gewicht

- De gebruiker logt zijn **lichaamsgewicht** met datum (zo vaak als gewenst).
- Bij elke set met een lichaamsgewichttype wordt het **lichaamsgewicht op dat moment** gebruikt (de meest recente meting op of vóór de trainingsdatum) en **vastgelegd bij de set**, zodat historie niet verschuift als je later een nieuw gewicht invult.

**Voorbeeld (van de eigenaar):** Assisted pull-up met 20 kg ondersteuning.
- Lichaamsgewicht 80 kg → effectief 80 − 20 = **60 kg**
- 10 kg afgevallen, nog steeds 20 kg ondersteuning → 70 − 20 = **50 kg**

De app laat dus zien dat de *prestatie* is gedaald (of gelijk gebleven relatief), ook al is de machine-instelling hetzelfde.

| # | Besluit | Status |
|---|---|---|
| D-14 | Lichaamsgewicht bijhouden met datum; grafiek op het dashboard. | [BESLOTEN] |
| D-15 | Effectief gewicht wordt overal gebruikt voor statistiek (dashboard, heatmap, vooruitgang). | [BESLOTEN] |
| D-16 | Bij het starten van een training, als de laatste meting ouder is dan **7 dagen**: een kleine, overslaanbare vraag "Wat weeg je vandaag?". Daarnaast kun je altijd zelf een meting toevoegen. | [BESLOTEN] |
| D-17 | Eenheid standaard **kg**; in **Instellingen** kan de gebruiker een andere eenheid kiezen (lbs). Intern wordt in kg opgeslagen en alleen de weergave omgerekend. | [BESLOTEN] |

---

## 6. Doelen (verwachting voor de volgende keer)

Kern van de app: na elke oefening zet je een **doelgewicht** voor de volgende keer; de volgende keer zie je dat als eerste. Er wordt **niet op reps getarget**: reps ontstaan door tot failure te gaan.

**Vorm van het doel**
- **Per set** een doelgewicht: zwaar-set doelgewicht, licht-set doelgewicht.
- Bij oefeningen met progressie op reps (bijv. crunches, 0 kg) is er geen doelgewicht; de app toont **"vorige keer: 22 reps"** als referentie om te verslaan.
- De rep-target (6 / 16) staat er altijd bij; die volgt uit de rol, die hoef je niet in te vullen.
- Optionele korte **notitie** (bijv. "had energie over").

**Slim voorstel** — na het invullen van de sets stelt de app een doel voor, dat je met één tik accepteert of met +/− aanpast:

| Resultaat t.o.v. rep-target | Signaal | Voorstel volgende keer |
|---|---|---|
| ≥ target + 2 reps | **Te licht** | één stap zwaarder (volgende haalbare gewicht) |
| target − 1 … target + 1 | **Goed** | gewicht gelijk houden; probeer meer reps te halen, zodat je de keer daarna zeker zwaarder kunt |
| ≤ target − 2 reps | **Te zwaar** | één stap lichter |

"Eén stap" = het volgende gewicht dat met het materiaalprofiel haalbaar is (sectie 7). Het voorstel is een hulpmiddel: was het binnen de target maar *echt* zwaar, dan zet je gewoon hetzelfde gewicht als doel.

**Doel vs. resultaat**
- Tijdens de oefening zie je per set het doel.
- Na invullen: per set een visuele indicator of je het **doelgewicht gehaald** hebt, plus het signaal te licht / goed / te zwaar als input voor het volgende doel.
- Toon: **motiverend, niet bestraffend** — nadruk op wat goed ging (records, zwaarder dan vorige keer, reeksen gehaalde doelen); "te zwaar" is informatie, geen falen.
- Op het dashboard: gehaalde doelen en signalen over tijd, op de drie niveaus oefening / spier / spiergroep.

| # | Besluit | Status |
|---|---|---|
| D-18 | Doel wordt vastgelegd **na elke oefening** en getoond **bij de volgende keer dezelfde oefening** (ongeacht schema). | [BESLOTEN] |
| D-18a | Bij het plannen van de volgende keer kun je **sets toevoegen en weghalen** (+ / −). **Standaard** neemt het plan de sets over die je **net gedaan hebt** (zelfde aantal, rollen en warm-up), met het slimme gewichtsvoorstel. | [BESLOTEN] |
| D-19 | Doel is **per set een gewicht** + optionele notitie. Geen reps-doel; bij progressie op reps alleen "vorige keer" als referentie. | [BESLOTEN] |
| D-20 | Geen RPE/zwaarte-score op doelniveau (voor nu; kan later). | [BESLOTEN] |
| D-21 | Slim voorstel volgens de tabel hierboven; gebruiker beslist altijd zelf. Marge (±1 / ±2) instelbaar. | [BESLOTEN] |
| D-22 | Visuele indicator doelgewicht gehaald + signaal per set, motiverend van toon; overzicht op dashboard op drie niveaus. | [BESLOTEN] |

---

## 7. Materiaal en schijvenverdeling

Onder het doelgewicht toont de app **welke schijven je moet pakken**. Het **totaalgewicht staat groot** in beeld; de schijven **klein** eronder.

**Slim laden:** het zware gewicht wordt opgebouwd als *licht gewicht + extra schijven aan de buitenkant*. Voor de lichte set haal je alleen de buitenste schijf (per kant) eraf.

```
            ZWAAR · target 6
                 60 kg
      per kant:  [10] [10]   · stang 20

            LICHT · target 16
                 40 kg
      haal eraf: buitenste 10 (per kant)
```

**Materiaaltypes**

De laadhulp (schijvenverdeling, laadvolgorde) is vooral bedoeld voor **stang** en **plate-loaded machines**: daar kost wisselen tijd. Bij een pin-stack is wisselen al snel en toont de app alleen de pin-stand.


| Type | Instellingen | Weergave |
|---|---|---|
| **Stang (barbell)** | stanggewicht, schijvenset | schijven **per kant** |
| **Plate-loaded machine** | basisgewicht (vaak 0 / onbekend), schijvenset, 1 of 2 laadpunten | schijven per laadpunt |
| **Pin-stack machine / kabel** | stapgrootte (bijv. 5 kg), min/max, optioneel klein extra gewicht | "pin op 45 kg" |
| **Dumbbells** | vaste reeks (bijv. 2–40 kg per 2 kg) *of* verstelbaar met schijfjes | "2× 22 kg" |
| **Lichaamsgewicht + extra** | schijven/dumbbell aan dipriem of vest | zoals stang, zonder "per kant" |
| **Assisted machine** | stapgrootte van de ondersteuning | "ondersteuning 20 kg" |

**Schijvenvoorraad**
- Eén **gym-standaardset** instellen (bijv. 25 / 20 / 15 / 10 / 5 / 2,5 / 1,25 kg, met aantal per maat).
- **Materiaalprofielen** zijn herbruikbaar ("Olympische stang 20 kg", "Leg press gym X") en worden per oefening gekozen; per oefening zijn uitzonderingen mogelijk.

**Afronden en onhaalbare gewichten**
- Doelen invullen gaat met een stepper die **alleen haalbare gewichten** aanbiedt.
- Als zwaar → licht niet met één schijf per kant kan: de app zoekt de oplossing met **zo min mogelijk schijfwissels** en toont duidelijk wat eraf moet. Bij gelijke opties wint het gewicht dat het dichtst bij het doel ligt.

| # | Besluit | Status |
|---|---|---|
| D-23 | Schijvenverdeling tonen onder het doelgewicht; totaal groot, schijven klein. | [BESLOTEN] |
| D-24 | Laadvolgorde geoptimaliseerd zodat zwaar → licht zo min mogelijk wissels kost (liefst 1 schijf per kant). | [BESLOTEN] |
| D-25 | Materiaal per oefening instelbaar in een instellingenpagina. | [BESLOTEN] |
| D-26 | Materiaaltypes en weergave zoals in de tabel. | [BESLOTEN] |
| D-27 | Gym-standaardset + herbruikbare materiaalprofielen + uitzonderingen per oefening. | [BESLOTEN] |
| D-28 | Doelen alleen als haalbare gewichten invoerbaar; bij onmogelijke 1-schijf-wissel: minimale wissels tonen. | [BESLOTEN] |

---

## 8. Training uitvoeren (de flow)

**Stappen**

1. Kies een schema → **Start training**. De **totale trainingstimer** start.
2. Scherm van oefening 1: naam, **doelen per set** (groot gewicht, kleine schijven, rep-target), notitie van vorige keer.
3. *(Optioneel)* tik op **Warm-up** → de warm-up-timer loopt; je doet je warm-up-reps (sectie 12).
4. Je laadt het gewicht en tikt op de grote **GO**-knop → **3-2-1**-aftelling (met geluid/trilling) → **oefeningtimer** loopt. Je legt je telefoon weg.
5. Je doet alle sets direct na elkaar — geen tweede GO tussen de sets.
6. Zodra je het **eerste getal invult**, stopt de oefeningtimer (je bent klaar met trainen, je bent aan het invullen).
7. **Achteraf** alles invullen: reps van warm-up en werksets (gewicht voorgevuld met het doel), eventueel **+ Set**, doel voor volgende keer bevestigen/aanpassen.
8. **Swipe** naar de volgende oefening → terug naar stap 2.
9. Na de laatste oefening → **Training afronden** → samenvatting (sectie 10 en 11).

**Tijdmeting** — per oefening worden deze tijdstempels vastgelegd:

| Tijdstempel | Moment |
|---|---|
| `shown_at` | oefening verschijnt in beeld (na swipe) |
| `warmup_at` | tik op Warm-up (optioneel) |
| `go_at` | einde van de 3-2-1-aftelling |
| `first_input_at` | eerste getal ingevuld |
| `done_at` | swipe naar de volgende oefening |

Afgeleid:
- **Oefentijd** = `first_input_at − go_at` (de tijd dat je daadwerkelijk traint)
- **Wisseltijd** = `go_at − shown_at` (doelen lezen, gewicht laden, naar het apparaat lopen)
- **Invultijd** = `done_at − first_input_at`
- **Totale duur** = einde − start training

Zo zie je ook of je later in de training trager wordt.

**Overig tijdens de training**
- Scherm blijft aan tijdens een training.
- Volgorde aanpassen / oefening overslaan / oefening toevoegen tijdens de training.
- Terug-swipen naar een vorige oefening om te corrigeren (zonder de tijden te verpesten).
- Een training die de app gesloten heeft of die > 4 uur openstaat, kan hervat of afgesloten worden.

| # | Besluit | Status |
|---|---|---|
| D-29 | Schema's = vaste reeks oefeningen; tijdens de training swipe je van oefening naar oefening. | [BESLOTEN] |
| D-30 | Totale trainingstijd + tijd per oefening meten. | [BESLOTEN] |
| D-31 | Eerste ingevulde getal = einde van de oefening. | [BESLOTEN] |
| D-32 | 3-2-1-aftelling vóór de oefeningtimer. | [BESLOTEN] |
| D-33 | Aftelling start met een **GO-knop** (niet automatisch na swipe), zodat je rustig doelen kunt lezen en gewicht kunt laden. | [BESLOTEN] |
| D-34 | Wisseltijd en invultijd apart vastleggen en tonen. | [BESLOTEN] |
| D-35 | Oefening overslaan / toevoegen / volgorde wijzigen tijdens training. | [BESLOTEN] |

---

## 9. Schema's en weekplannen

**Schema's**
- Schema aanmaken met naam, oefeningen in volgorde (slepen om te ordenen).
- Per oefening in het schema optioneel afwijkend aantal sets.

**Weekplan** (laag boven schema's)
- Een weekplan bundelt schema's die bij elkaar horen, met:
  - een **target**: aantal trainingen per week (bijv. 3);
  - optioneel een **dag per schema** (bijv. ma = Push, wo = Pull, vr = Benen).
- Precies één weekplan is **actief**; je kunt wisselen of een eigen weekplan maken.
- **Dagherkenning op het beginscherm** (flexibel, *life is chaotic*):
  1. **Al getraind vandaag** → *"Done for today"* met een korte samenvatting. Je kunt toch nog een training starten.
  2. **Schema gepland voor vandaag** → groot voorgesteld: *"Vandaag: Push"*.
  3. **Rustdag** → *"Rustdag"*; je kunt toch een training starten. Voorstel: het eerstvolgende schema uit het weekplan dat je deze week nog niet gedaan hebt.
  4. Een overgeslagen dag hoeft niet ingehaald te worden; standaard maximaal één training per dag. Je kunt het wel aangeven.
  5. Altijd met één tik een ander schema kiezen.
- Voortgang op het beginscherm: *"2 / 3 deze week"*. Week begint op maandag.

**Standaardinhoud** — wordt meegeleverd, volledig aanpasbaar, dupliceerbaar en verwijderbaar (zie bijlage A):

| Weekplan | Target | Dagen (standaard) | Schema's |
|---|---|---|---|
| **Full body** | 3× | ma · wo · vr | Full body (compound) |
| **Push / Pull / Legs** | 3× | ma · wo · vr | Push · Pull · Legs |
| **Upper / Lower** | 4× | ma · di · do · vr | Upper A · Lower A · Upper B · Lower B |

| # | Besluit | Status |
|---|---|---|
| D-36 | Schema's aanmaken/bewerken/dupliceren. | [BESLOTEN] |
| D-37 | ~~Beginscherm met rotatievoorstel~~ → vervangen door D-62. | [VERVALLEN] |
| D-60 | Hiërarchie **Weekplan → Schema → Oefening → Set → reps**. | [BESLOTEN] |
| D-61 | Weekplan met target per week en optionele dagindeling; één actief weekplan. | [BESLOTEN] |
| D-62 | Dagherkenning op het beginscherm volgens de regels hierboven (done for today / vandaag / rustdag), altijd overschrijfbaar. | [BESLOTEN] |
| D-63 | Standaard-weekplannen en -schema's meeleveren zoals in de tabel en bijlage A. | [BESLOTEN] |
| D-64 | Invulling van *Push/Pull/Legs* en *Upper/Lower* zoals voorgesteld in bijlage A. | [BESLOTEN] |
| D-65 | Geen pushmeldingen/herinneringen in v0.1 (eventueel later, uit te zetten). | [BESLOTEN] |

---

## 10. Dashboard

| Onderdeel | Inhoud |
|---|---|
| **Lichaamsgewicht** | lijngrafiek over tijd, met trendlijn (voortschrijdend gemiddelde) |
| **Vooruitgang per oefening** | effectief gewicht zware set + geschatte 1RM over tijd; bij progressie op reps: reps over tijd |
| **Per spier** | volume en aantal werksets per week per spier, trend over tijd; welke oefeningen eraan bijdragen |
| **Per spiergroep** | hetzelfde, opgeteld per spiergroep (bijv. *Rug* = lats + trapezius + midden rug + onderrug); balans tussen groepen (bijv. duwen vs. trekken, boven- vs. onderlichaam) |
| **Frequentie** | trainingen per week |
| **Duur** | trainingsduur per training; gemiddelde oefentijd per positie in de training (vermoeidheid) |
| **Doelen** | % gehaalde doelen over tijd |
| **Records** | persoonlijke records per oefening |

| # | Besluit | Status |
|---|---|---|
| D-38 | Dashboard met lichaamsgewicht. | [BESLOTEN] |
| D-39 | Overige onderdelen zoals in de tabel. | [BESLOTEN] |
| D-66 | Statistiek wordt apart bijgehouden en getoond op drie niveaus: **oefening**, **spier** en **spiergroep** (zie sectie 11). | [BESLOTEN] |
| D-68 | **Vergelijkbare oefeningen (nice to have, v0.3 of later):** doe je een oefening voor het eerst, dan toont de app als hint wat je het laatst deed bij oefeningen die **dezelfde spieren** trainen (bijv. bij incline bench: je bench press en military press). Geen omrekenfactor, geen berekend voorstel — puur ter referentie, zodat je zelf een gevoel krijgt. | [BESLOTEN] |
| D-67 | Per spier/spiergroep: volume én **aantal werksets per week** (warm-up telt niet mee; secundair telt voorlopig als ½ set, zie R-1). | [BESLOTEN] |

---

## 11. Spierheatmap (na de training)

Na afronden van een training: een figuur van het lichaam, **voorkant en achterkant als losse kaarten**, met kleur per **spier** (optioneel samengevoegd per spiergroep). Twee weergaven:

1. **Totaal** — hoeveel je deze training per spiergroep hebt verplaatst.
2. **T.o.v. vorige keer** — waar je vooruit of achteruit ging.

**Formule (voorstel)**

- Elke oefening heeft per **spier** een aandeel in %. Standaard: **primair = 100%**, **secundair = 50%**. Het datamodel slaat een vrij percentage per spier op, zodat dit later verfijnd kan worden (zie R-1).
  Bijv. bench press: borst 100%, triceps 50%, voorkant schouder 50%.
- **Totaal-kaart:** *volume* per spiergroep = Σ (effectief gewicht × reps × aandeel). Kleur: licht → IBS-geel → donker naarmate volume hoger.
- **Vooruitgang-kaart:** per oefening de **geschatte 1RM (Epley)** van de **zware set**:
  `e1RM = gewicht × (1 + reps / 30)`
  vergeleken met **de vorige keer dat díe oefening gedaan is**, als % verschil; per spiergroep gewogen gemiddeld (primair 100%, secundair 50%). Kleur: groen = vooruit, grijs = gelijk, rood = achteruit.
- Waarom niet overal 1RM: Epley is betrouwbaar tot ~10–12 reps; voor de lichte set (~16) wordt het onnauwkeurig. Daarom e1RM alleen op de zware set, volume voor de totaal-kaart.
- Oefeningen met progressie op reps tellen in de vooruitgang-kaart mee als % verschil in reps.

**Spieren en spiergroepen**

Oefeningen worden gekoppeld aan **spieren** (primair/secundair). Spieren horen bij één **spiergroep**. Statistiek en heatmap rekenen op spierniveau en tellen op naar spiergroep. Dit staat los van oefeningen: dezelfde spier kan door veel oefeningen geraakt worden.

| Spiergroep | Spieren | Kant |
|---|---|---|
| **Borst** | Borst | voor |
| **Schouders** | Voorkant schouder · Zijkant schouder · Achterkant schouder | voor / voor / achter |
| **Rug** | Brede rugspier (lats) · Trapezius · Midden rug (rhomboïden) · Onderrug | achter |
| **Armen** | Biceps · Triceps · Onderarmen | voor / achter / voor |
| **Core** | Buikspieren · Schuine buikspieren | voor |
| **Benen** | Quadriceps · Hamstrings · Bilspieren · Adductoren · Kuiten | voor / achter / achter / voor / achter |

| # | Besluit | Status |
|---|---|---|
| D-40 | Heatmap na de training, voor- en achterkant, twee weergaven (totaal / t.o.v. vorige keer). | [BESLOTEN] |
| D-41 | Formule zoals hierboven (volume; Epley e1RM zware set; primair/secundair 100/50). | [BESLOTEN] |
| D-42 | "Vorige keer" = vorige keer dat dezelfde **oefening** is gedaan (werkt ook over schema's heen). | [BESLOTEN] |
| R-1 | **Later onderzoek:** betrouwbare activatiepercentages per spier per oefening (literatuur/EMG) en vergelijken met wat andere apps gebruiken; daarna de standaard 100/50 vervangen. | [LATER] |
| D-43 | Lichaamsfiguur wordt **zelf getekend** als vector (geen afbeeldingen met onduidelijke licentie), zodat de repo publiek kan. | [BESLOTEN] |

---

## 12. Warm-up

Een warm-up is niets anders dan **reps doen van dezelfde oefening, zonder (extra) gewicht**: alleen de lege stang, alleen je lichaamsgewicht (als dat niet te zwaar is), of anders het **lichtste gewicht** van het materiaal (laagste pin, lichtste dumbbell). Het gaat alleen om reps.

- Knop **"+ Warm-up"** bij de oefening, net als "+ Set". Tijdens de oefening en bij het plannen van de volgende keer.
- De app toont automatisch het warm-up-gewicht op basis van het materiaalprofiel ("lege stang 20 kg", "alleen lichaamsgewicht", "pin 1 – 5 kg").
- Je vult alleen **reps** in.

| # | Besluit | Status |
|---|---|---|
| D-44 | Warm-up = set-rol **Warm-up**: alleen reps, op lege stang / lichaamsgewicht / lichtste gewicht; toe te voegen met **"+ Warm-up"**, on the fly en bij de planning. | [BESLOTEN] |
| D-45 | Warm-up telt **niet** mee in doelen, heatmap en vooruitgang; de tijd wordt apart vastgelegd (warm-up-tijd vs. oefentijd). | [BESLOTEN] |
| D-45a | Flow met warm-up: **Warm-up-knop** (timer start, tijdstempel `warmup_at`) → warm-up-reps → gewicht laden → **GO** → 3-2-1 → werksets direct na elkaar → alles achteraf invullen. Er is maar **één GO**. Warm-up-tijd = `go_at − warmup_at` (inclusief gewicht laden); oefentijd begint bij GO. | [BESLOTEN] |

---

## 13. Standaardinhoud vs. persoonlijke data

| Laag | Inhoud | Waar |
|---|---|---|
| **Standaard (in de app)** | generieke oefeningenbibliotheek met spiergroepen en factoren; **standaardschema's en -weekplannen** (bijlage A); generieke materiaalprofielen (bijv. olympische stang 20 kg, gangbare schijven); methode-standaarden (2 sets, 6/16). **Geen gewichten, geen persoonsgegevens.** | in de APK, voor iedereen gelijk |
| **Persoonlijk** | eigen oefeningen, eigen/aangepaste schema's en weekplannen, schijvenvoorraad, doelen, historie, lichaamsgewicht, instellingen | alleen lokaal op de telefoon |

- **Export / import** van alle data als één JSON-bestand (back-up, nieuwe telefoon, delen).
- **Schema's en weekplannen** los exporteerbaar/importeerbaar als bestand, zodat gebruikers ze onderling kunnen delen. De repo bevat alleen de generieke standaardschema's uit bijlage A.
- **Eigen schema's maken** is een kernfunctie van de app (sectie 9).

| # | Besluit | Status |
|---|---|---|
| D-46 | Scheiding standaard / persoonlijk zoals hierboven. | [BESLOTEN] |
| D-47 | Compacte, generieke bibliotheek van **±50 gangbare oefeningen**, kwaliteit boven kwantiteit; eigen oefeningen voeg je toe in de app. | [BESLOTEN] |
| D-48 | Export/import van alle data + los exporteerbare schema-pakketten. | [BESLOTEN] |
| D-49 | Materiaal, gewichten en historie van de eigenaar komen **niet** in de repo; de eigenaar voert ze zelf in de app in. Standaardschema's (bijlage A) mogen wel, zonder gewichten en zonder naam. Testdata in de code is fictief. | [BESLOTEN] |

> Waarschuwing die in de README en de app komt: data staat alleen op de telefoon. De app verwijderen = data weg, tenzij je een export hebt gemaakt.

---

## 14. Vormgeving (IBS-huisstijl)

Afgeleid uit het IBS-logo en de website (i-b-s.nl), aangevuld met een eigen **art-deco-laag**. Volledige uitwerking (kleurtokens, typografie, ornamenten, icoon): **`docs/DESIGN.md`**.

| Element | Waarde |
|---|---|
| Primaire kleur | **IBS-geel `#F9C852`** (gemeten uit het logo) |
| Tekst/achtergrond | crème `#FEF9ED` op bijna-zwart `#171717`; kaarten `#232323` / `#3A3A3A` |
| Stijl | strak, donker, grote kaarten met ronde hoeken, zachte amber gloed; koppen licht, **cijfers groot en vet** |
| Lettertype | **Poppins** (open licentie) |
| Thema | **donker thema standaard**; licht thema optioneel |
| Art deco | eigen vector-ornamenten in gedempt goud `#C9A24A`: dubbele scheidingslijnen, zonnewaaier, getrapte hoeken, ruitenpatroon. Terughoudend op trainingsschermen. |

- **Grote cijfers**: doelgewichten en timers zijn het grootste element op het scherm, leesbaar op armlengte.
- **Grote tikvlakken**: bedienbaar met bezwete handen / tussen de sets door.
- **IBS-logo** in de app: op het opstartscherm en in "Over deze app" met de tekst *"Ontwikkeld onder de vlag van Isatis Business Solutions"*. Subtiel, niet op de trainingsschermen.
- App-icoon: eigen icoon in IBS-geel/zwart, geïnspireerd op het IBS-logo (vier gedraaide vlakken) maar herkenbaar als fitness-app.

| # | Besluit | Status |
|---|---|---|
| D-50 | IBS-uitstraling (kleur, stijl) overnemen. | [BESLOTEN] |
| D-51 | IBS-logo in de app, met vermelding dat de app onder IBS-vlag is ontwikkeld. | [BESLOTEN] |
| D-52 | Donker thema standaard in IBS-huisstijl, Poppins, grote cijfers, logo op opstart- en Over-scherm. Eigenaar levert logo + huisstijlafbeeldingen aan (`assets/branding/`). | [BESLOTEN] |
| D-53 | IBS-logo mag in de (publieke) repo en de app. | [BESLOTEN] |
| D-54a | Art-deco-ornamenten als eigen vectoren, uitgewerkt in `docs/DESIGN.md`. | [BESLOTEN] |
| D-54 | Naam van de app: **GYM-8020**. Repo: `Fabianlanger/GYM-8020` (tijdens ontwikkeling privé, later publiek). | [BESLOTEN] |

---

## 15. Techniek

| Onderdeel | Keuze |
|---|---|
| Taal / UI | Kotlin + Jetpack Compose (Material 3, eigen IBS-thema) |
| Opslag | Room (SQLite) lokaal; instellingen in DataStore |
| Architectuur | repository-laag tussen UI en opslag, zodat later een sync/externe database kan worden toegevoegd (D-03) |
| Grafieken | Vico (open source Compose-grafieken) |
| Heatmap | eigen vector-lichaamsfiguur (Compose Canvas / vector drawables) |
| Min. Android | Android 8.0 (API 26) |
| Talen in de app | **Engels + Nederlands**, volgt de taal van de telefoon |
| Tests | unit tests op de rekenkern: effectief gewicht, schijvenverdeling, doelvoorstel, e1RM, heatmap-scores |

**Publicatie**
- GitHub Actions bouwt bij elke tag `v*` een **ondertekende release-APK** en zet die in **GitHub Releases**.
- De ondertekeningssleutel staat in **GitHub Secrets**, nooit in de repo.
- Belangrijk: updates installeren over de oude app (met behoud van data) kan **alleen als de APK met dezelfde sleutel is ondertekend**. De sleutel moet dus bewaard blijven.
- **Tijdens ontwikkeling** wordt elke CI-build ondertekend met een vaste debug-sleutel (`app/debug.keystore`, in de repo). Zo installeert elke nieuwe build over de vorige zonder dataverlies. Voor de publieke release (v0.5) komt er een aparte release-sleutel in GitHub Secrets; die installeert niet over de debug-versie heen (eenmalig export → import).
- Omdat de ontwikkelomgeving geen toegang heeft tot de Android SDK-downloads, wordt de logica in een aparte module `core/` (puur Kotlin) lokaal getest en bouwt GitHub Actions de APK.
- README: wat de app doet, screenshots, installatie-instructies ("installeren van onbekende bronnen"), link naar `docs/methode.md`, IBS-vermelding.
- Licentie: **MIT**.

**Repo-indeling (voorstel)**
```
README.md
LICENSE
docs/
  SPEC.md          ← dit document
  methode.md       ← de trainingsmethode (eigenaar)
assets/branding/   ← IBS-logo
app/               ← Android-project
.github/workflows/ ← build + release
.claude/skills/    ← repo-specifieke Claude-skills (later)
```

| # | Besluit | Status |
|---|---|---|
| D-55 | Techniek zoals in de tabel. | [BESLOTEN] |
| D-56 | Releases via GitHub Actions + GitHub Releases, sleutel in Secrets. | [BESLOTEN] |
| D-57 | App in het Engels + Nederlands. | [BESLOTEN] |
| D-58 | Licentie: **MIT**. | [BESLOTEN] |
| D-59 | In de app een scherm/link **"Over de methode"** dat naar `docs/methode.md` verwijst (zodra die bestaat). | [BESLOTEN] |

---

## 16. Fasering

Elke fase = een werkende, installeerbare APK.

| Fase | Inhoud | Doel |
|---|---|---|
| **v0.1 — Kern** | oefeningenbibliotheek, eigen oefeningen, eigen schema's, **weekplannen + dagherkenning + standaardschema's**, training-flow (GO + 3-2-1, swipe, timers), sets (2 + extra on the fly), **warm-up**, rep-targets 6/16, doelen + slim voorstel + sets plannen (+/−), lichaamsgewicht + effectief gewicht, export/import | direct mee trainen |
| **v0.2 — Materiaal** | materiaalprofielen, schijvenverdeling, geoptimaliseerde laadvolgorde, alleen haalbare gewichten | snel wisselen zwaar → licht |
| **v0.3 — Dashboard** | grafieken lichaamsgewicht, vooruitgang, frequentie, duur, doelen, records | inzicht |
| **v0.4 — Heatmap** | voor/achter-lichaamskaart, totaal + vooruitgang | beloning na de training |
| **v0.5 — Publicatie** | "Over de methode", README, release-workflow, IBS-branding afgerond, onboarding (G-2) | publiek delen |

> Branding (thema, kleuren, lettertype) zit vanaf v0.1 in de app; logo/Over-scherm uiterlijk v0.5.

**Voortgang**

| Fase | Status |
|---|---|
| v0.1 — Kern | gebouwd (dev-builds 1–5) |
| v0.2 — Materiaal | gebouwd: materiaalprofielen, schijvenvoorraad, schijven per kant, laadvolgorde zwaar → licht met minimale wissels, alleen haalbare gewichten, warm-up-gewicht uit het materiaal. Plus feedback v0.1: iconen, compacter trainingsscherm, swipe-lint. |
| v0.3 — Dashboard | gebouwd: vooruitgang per oefening / spier / spiergroep, balans duwen-trekken en boven-benen, frequentie t.o.v. weekdoel, duur en tijd per positie, doelen gehaald, lichaamsgewicht met trendlijn, records; vergelijkbare oefeningen (D-68). Plus feedback v0.2: elegant swipe-lint, achtergrondpatroon per gedeelte (D-72). |
| v0.4 — Heatmap | gebouwd: eigen lichaamsfiguur (art-deco-mozaïek) voor/achter, in de samenvatting met Totaal (volume) en T.o.v. vorige keer (e1RM zware set / reps), tik op een spier voor de waarde; ook in het dashboard (werksets per week). |
| v0.5 — Publicatie | gebouwd: "Over de methode" in de app (offline, NL/EN) en `docs/methode.md`; introductie voor nieuwe gebruikers (methode, lichaamsgewicht, stang + schijven, weekplan; bestaande gebruikers met trainingen slaan hem over, opnieuw te bekijken via Instellingen); release-ondertekening met eigen sleutel via GitHub Secrets (`docs/RELEASE.md`); README bijgewerkt; DB-schema v2 vastgelegd. Open: screenshots voor de README, repo publiek maken. |
| v0.6 — Logboek, herinneringen, back-up | gebouwd: logboek met alle trainingen (dashboard + "Alles"), afgeronde training bewerken vanuit logboek of samenvatting (D-74, D-75); lokale herinneringen (D-76); automatische back-up naar een gekozen map en Android-back-up (D-77). |

| # | Besluit (na feedback v0.1) | Status |
|---|---|---|
| D-69 | Elke oefening heeft een **bewegingsicoon** (squat, hinge, duwen, trekken, …), zelf getekend in art-deco-lijnstijl; zelf te kiezen bij eigen oefeningen. Iconen ook op beginscherm, schema's, lijsten en knoppen voor snelle visuele herkenning. | [BESLOTEN] |
| D-70 | Trainingsscherm compact: sets als één regel, alleen de actieve set open; "Volgende keer" ingeklapt tot je tikt. | [BESLOTEN] |
| D-71 | Geanimeerd art-deco-lint aan de zijkant van het trainingsscherm dat laat zien dat je naar de volgende oefening kunt swipen (tikken kan ook). | [BESLOTEN] |
| D-72 | Elk gedeelte van de app heeft een eigen, vervagend art-deco-achtergrondpatroon: ruitjes met waaiers (home), waaierschubben (training en samenvatting), bogen (overige schermen). | [BESLOTEN] |
| D-73 | Het tabblad Lichaam wordt Dashboard; lichaamsgewicht staat als onderdeel in het dashboard met een knop naar de metingen. | [BESLOTEN] |
| D-74 | **Afgeronde trainingen bewerken**: via het logboek (dashboard → Trainingen) en de knop "Training bewerken" in de samenvatting. Sets (gewicht, reps, zwaar/licht/warming-up), oefeningen toevoegen/verwijderen/terugzetten, datum, starttijd en duur, en de hele training verwijderen. Wijzigingen worden direct opgeslagen. Toegevoegde oefeningen krijgen geen doel, zodat "doelen gehaald" niet achteraf verandert. | [BESLOTEN] |
| D-75 | Na bewerken: is dit de **laatste training** van een gewijzigde oefening, dan vraagt de app of het doel voor volgende keer opnieuw berekend moet worden (notitie blijft). Bij oudere trainingen blijft het doel staan. | [BESLOTEN] |
| D-76 | **Herinneringen** (D-65 uitgewerkt), alleen lokaal, standaard uit, per soort aan te zetten: trainingsdag (schema op die dag in het weekplan), weekdoel (als de rest van de week krap wordt: resterende trainingen ≥ resterende dagen − 1), training staat nog open (3 uur na de start), lichaamsgewicht (wekelijks, alleen bij oefeningen met eigen gewicht in de laatste 30 dagen). Eén dagelijkse tijd (standaard 18:00), ongeveer op tijd (geen exacte-alarm-toestemming). Niets als je die dag al getraind hebt. | [BESLOTEN] |
| D-77 | **Back-up** (D-03 uitgewerkt): (1) Android-back-up via het Google-account staat aan (database + instellingen); (2) optionele automatische back-up na elke training naar een zelfgekozen map (bijv. Google Drive), de nieuwste 7 blijven bewaard. Geen eigen server of accounts; echte cloud-sync blijft buiten scope. | [BESLOTEN] |
| D-78 | **Teststrategie** (zie `docs/TESTING.md`): elke bug krijgt eerst een test die hem nabootst, dan de fix; regels (property-tests) naast voorbeelden; een risicolijst met kritieke flows in plaats van code coverage, aangevuld met mutatietesten; UI- en screenshottests in CI. End-to-end op een emulator later. | [BESLOTEN] |
| D-79 | **Materiaal ondersteunt, maar beperkt nooit**: profielen helpen met haalbare gewichten en schijven, maar zelf invoeren kan altijd en + / − gaan nooit de verkeerde kant op. Uitwerking: backlog O-2. | [BESLOTEN] |

---

## 17. Buiten scope (voor nu)

- Cloud-sync / externe database (wel voorbereid, D-03)
- Accounts, sociale functies
- iOS
- Play Store-publicatie
- Cardio / GPS / hartslag / smartwatch

---

## 18. Openstaande punten — samenvatting

| # | Onderwerp | Wat nodig is |
|---|---|---|
| — | `docs/methode.md` | tekst van eigenaar |
| R-1 | Onderzoek spierpercentages | later |

---

## 19. Generiek vs. specifiek — nog te bevestigen

De app ondersteunt het proces van de eigenaar, maar bevat geen persoonlijke inhoud. Deze punten bepalen waar die grens ligt.

| # | Vraag | Voorstel |
|---|---|---|
| G-1 [BESLOTEN] | Is de methode (2 sets zwaar → licht, 6/16, doel na elke oefening) **de kern van GYM-8020** voor iedereen, of moet de app methode-neutraal zijn? | Methode is de kern en de standaard; de getallen zijn aanpasbaar in Instellingen. |
| G-2 [BESLOTEN, nice to have → v0.5] | Wat ziet een **nieuwe gebruiker** bij de eerste start? | Korte onboarding van 3 schermen: (1) uitleg methode in 3 zinnen, (2) lichaamsgewicht (optioneel, alleen lokaal), (3) stanggewicht + schijven van je gym (voorgevuld met gangbare waarden), (4) kies een weekplan (Full body / PPL / Upper-Lower / leeg). |
| G-3 | Moet de app **voorbeeldschema's** meeleveren? | **Besloten: ja** — zie D-63 en bijlage A. |
| G-4 [BESLOTEN] | Taal van de app. | Engels + Nederlands, volgt de telefoon. |

---

## Bijlage A — Standaardschema's

Standaard 2 sets per oefening (zwaar 6 / licht 16) tenzij anders vermeld. Oefeningen met lichaamsgewicht (pull-up, dips) zijn per gebruiker om te zetten naar *assisted* of *+ extra*.

**Full body (compound)** — volgorde zoals de eigenaar traint [BESLOTEN]
1. Back squat
2. Incline bench press (barbell)
3. Pull-up *(om te zetten naar assisted)*
4. Seated calf raise (met gewicht)
5. Military press (overhead press, barbell)
6. Seated cable row
7. Decline crunch *(progressie op reps)*
8. Cable biceps curl
9. Cable triceps pushdown

**Push / Pull / Legs** [BESLOTEN]

| Push | Pull | Legs |
|---|---|---|
| Bench press | Pull-up | Back squat |
| Overhead press | Barbell row | Romanian deadlift |
| Incline dumbbell press | Lat pulldown | Leg press |
| Lateral raise (dumbbell) | Seated cable row | Leg curl |
| Dips | Face pull | Standing calf raise |
| Cable triceps pushdown | Barbell curl · Hammer curl | Hanging leg raise |

**Upper / Lower** [BESLOTEN]

| Upper A | Lower A | Upper B | Lower B |
|---|---|---|---|
| Bench press | Back squat | Incline dumbbell press | Deadlift |
| Barbell row | Romanian deadlift | Pull-up | Leg press |
| Overhead press | Leg extension | Dumbbell shoulder press | Leg curl |
| Lat pulldown | Standing calf raise | Seated cable row | Seated calf raise |
| Cable biceps curl | Crunch | Lateral raise | Hanging leg raise |
| Cable triceps pushdown | | Hammer curl · Overhead cable triceps extension | |
