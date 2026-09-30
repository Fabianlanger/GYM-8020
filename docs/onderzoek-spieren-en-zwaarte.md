# Onderzoek: spierpercentages (R-1) en zwaarte per set (D-20)

> Status: voorstel, nog niet gebouwd. Beslispunten staan onderaan.

## 1. Hoe telt de app nu?

Elke oefening heeft per spier een aandeel: **primair 100%**, **secundair 50%**. Een werkset telt
voor een spier als `1 × aandeel`. Daarop draaien het dashboard (werksets per spier per week) en
de heatmap.

## 2. Wat zegt het onderzoek over spierpercentages (R-1)?

**De 100/50-regel klopt verrassend goed.** De grootste meta-regressie tot nu toe (Pelland e.a.,
67 studies, 2.058 deelnemers) vergeleek drie manieren om "indirecte" sets te tellen: als 1, als 0,5
of als 0. Tellen als **0,5** voorspelde spiergroei en krachttoename het best. Dat is precies wat de
app nu doet met secundaire spieren.

**Maar niet elke "secundaire" spier groeit ook echt mee.** Wat studies laten zien:

| Oefening | Spier | Bevinding | Gevolg voor de app |
|---|---|---|---|
| Squat | Hamstrings | Groeien nauwelijks van squats (≈0–1%), terwijl quads, billen en adductoren 5–7% groeien | Staat al goed: squat heeft geen hamstrings |
| Bench press | Triceps | Laterale kop groeit goed, **lange kop niet**; een extensie-oefening erbij geeft duidelijk meer groei | 50% klopt; de heatmap kan de triceps niet per kop tonen |
| Lat pulldown / roeien | Biceps | Bij beginners groeit de biceps ongeveer even hard als van curls | 50% is eerder aan de lage kant; 75% is te verdedigen |
| Squat, deadlift | Onderrug (squat), onderarmen en trapezius (deadlift) | Werken vooral statisch (houden), er is weinig bewijs voor groei | Nu 50%; beter **25%** (de onderrug blijft primair bij de deadlift) |

**Kanttekeningen:**
- EMG-metingen (hoe hard een spier "aan" staat) voorspellen groei slecht. Daarom kijk ik vooral naar
  studies die echte spiergroei maten, niet naar EMG-tabellen.
- De meeste studies gebruiken beginners. Bij gevorderden groeien de secundaire spieren vaak minder mee.
- Precieze percentages per oefening bestaan niet in de literatuur. Het blijft een model: goed genoeg om
  balans te zien, niet om op de procent te sturen.

**Voorstel R-1:**
1. **Houd 100 / 50** als basis (onderbouwd door het onderzoek hierboven).
2. Voeg een derde niveau toe: **stabilisator = 25%**, voor onderrug, onderarmen en trapezius bij
   oefeningen waar ze alleen houden (squat, deadlift, roeien, farmer's walk).
3. Pas in de bibliotheek een paar oefeningen aan: biceps bij pull-ups, pulldowns en roeien naar 75%;
   onderrug bij squat van 50% naar 25%.
4. Je kunt het per oefening zelf aanpassen (het datamodel slaat al een vrij percentage op). In het
   oefeningscherm worden het drie knoppen: primair / secundair / stabilisator.

## 3. Wat zegt het onderzoek over zwaarte per set (D-20)?

Bij RPE/RIR geef je aan hoe zwaar een set was. **RIR** ("reps in reserve") is het aantal herhalingen
dat je nog had kunnen doen: 0 = echt falen.

- **Spiergroei neemt toe naarmate je dichter bij falen stopt**, niet in een rechte lijn: de laatste
  paar herhalingen wegen het zwaarst (Refalo e.a. 2023, meta-analyse).
- **Voor kracht maakt het weinig uit** of je tot falen gaat of 1–3 herhalingen overhoudt.
- **Mensen schatten RIR redelijk goed**: gemiddeld ongeveer één herhaling ernaast. Dichter bij falen en
  bij minder dan 12 herhalingen schatten ze beter (Halperin e.a. 2022, meta-analyse). Precies onze
  zware set.

**Het probleem met RPE in deze app:** de methode is *altijd tot falen*. Een RPE-knop per set zou bijna
altijd "10" zijn. Dat zijn extra tikken voor geen informatie. Daarom liet D-20 het terecht weg.

**Wat wel waardevol is:** vastleggen wanneer je **níet** tot falen ging. Bijvoorbeeld bij een pijntje,
zonder spotter bij een zware bench, of als je conditie op was maar je spieren niet.

## 4. Combinatie: R-1 × D-20

Eén klein extraatje per set, standaard leeg: **"Gestopt vóór falen: nog 1 / 2 / 3+ over"**. Geen
invoer betekent falen (RIR 0), dus normaal kost het geen enkele tik.

Die ene waarde doet dan drie dingen:

1. **Eerlijker doel voor volgende keer.** Het signaal te licht / goed / te zwaar rekent met
   `reps + RIR`. 5 reps met nog 2 over is dan "7 tot falen": niet te zwaar, maar goed. Zonder deze
   correctie zou de app ten onrechte lichter voorstellen.
2. **Eerlijker spiervolume.** Een set telt voor een spier als `aandeel × nabijheid tot falen`, met
   een eenvoudige schaal (afgerond op het onderzoek, bewust grof):

   | Gestopt | Telt als |
   |---|---|
   | tot falen (leeg) | 1,0 |
   | nog 1 over | 0,9 |
   | nog 2 over | 0,8 |
   | nog 3+ over | 0,6 |

   Voorbeeld: bench press met nog 2 over telt voor de borst als 1 × 0,8 = 0,8 set, en voor de triceps
   als 0,5 × 0,8 = 0,4 set. Records en kracht (e1RM) rekenen met `reps + RIR`.
3. **Inzicht.** Het dashboard kan laten zien hoe vaak je vóór falen stopte, per oefening. Zo zie je
   bijvoorbeeld dat je bij squats vaak niet tot het einde gaat, en kun je een safety rack overwegen.

## 5. Beslispunten

| # | Vraag | Mijn advies |
|---|---|---|
| R-1a | 100 / 50 houden en 25% (stabilisator) toevoegen? | Ja |
| R-1b | Bibliotheek aanpassen (biceps 75% bij trekken; onderrug 25% bij squat; onderarmen en trapezius 25% bij deadlift)? | Ja; je eigen aanpassingen blijven staan |
| D-20a | Optioneel "gestopt vóór falen" per set (1 / 2 / 3+), standaard leeg = falen? | Ja, in plaats van een RPE-schaal |
| D-20b | Doel-signaal rekent met reps + RIR? | Ja |
| D-20c | Volume en heatmap wegen met nabijheid tot falen (tabel hierboven)? | Ja |

## Bronnen

- Pelland e.a., meta-regressie volume en frequentie (preprint): https://sportrxiv.org/index.php/server/preprint/view/460
- Samenvatting "fractional" tellen: https://train.fitness/personal-trainer-blogs/not-all-sets-are-created-equal
- Refalo e.a. 2023, nabijheid tot falen en spiergroei: https://sportrxiv.org/index.php/server/preprint/download/295/699/654
- Uitleg nabijheid tot falen, kracht en groei: https://biolayne.com/reps/issue-32/the-failure-factor-the-relationship-between-proximity-to-failure-and-strength-and-hypertrophy/
- Halperin e.a. 2022, nauwkeurigheid van RIR-schattingen: https://sportrxiv.org/index.php/server/preprint/view/67
- Squats en hamstrings: https://www.strengthlog.com/do-squats-work-your-hamstrings
- Squat versus hip thrust (hamstrings nauwelijks groei): https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2023.1279170/pdf
- Bench press en de triceps (lange kop): https://www.strengthlog.com/bench-press-is-not-enough-for-maximal-triceps-growth/
- Pulldowns, roeien en de biceps: https://www.strengthlog.com/are-rows-and-lat-pulldowns-enough-for-great-biceps-growth/
