# Onderzoek: advies tegenover doel in Strong, Hevy en Fitbod (issue #10)

> Status: onderzoek, geen besluit. Datum: 2026-10-07.

## Methode en betrouwbaarheid

Bronnen: alleen officiële helpcentra, officiële sites/blogs en app-store-teksten van de apps zelf.
**Let op:** de helpcentra (help.strongapp.io, help.hevyapp.com, help.fitbod.me) waren vanuit deze omgeving
niet direct te openen (netwerk geblokkeerd). De beweringen hieronder komen uit de geïndexeerde tekst van
die officiële pagina's via een zoekmachine, met de pagina-URL erbij. Wat ik niet zo kon terugvinden staat als
**[niet geverifieerd]**. JEFIT en Boostcamp zijn weggelaten: daar niet genoeg officiële documentatie gevonden.

Termen: **Doel** = wat de gebruiker vastzet, **Advies** = voorstel van de app, **Prestatie** = wat je deed,
**Laatste keer** = prestatie in de meest recente sessie met die oefening.

## Korte antwoorden

**1. Wordt een progressie-advies automatisch toegepast? Uit te zetten?**
- **Strong:** geen gedocumenteerde progressie-adviesfunctie gevonden. Wat volgende keer klaarstaat zijn de
  waarden van het template; na een workout kies je zelf "Update Values Only", "Update Template and Values" of
  "Keep Original Template" [S1]. Dat de app zelf nooit +x kg voorstelt: **[niet geverifieerd]**.
- **Hevy (gewone routines):** geen advies, wél automatisch overnemen: de routinewaarden (gewicht/reps)
  worden standaard bijgewerkt met wat je deed, tenzij je bij "save workout" → Routine Settings "Update Routine
  Values" uitzet [H1][H2]. Met een rep range op de oefening worden de waarden níet bijgewerkt [H2].
- **Hevy Trainer (apart programma-onderdeel):** past gewicht **automatisch** aan als je op alle sets de bovenkant
  van de rep range haalt; je ziet een kleine markering bij oefeningen waar progressie is toegepast [H3][H4].
  Losse uitzetknop voor alleen de progressie: **[niet geverifieerd]**.
- **Fitbod:** volledig automatisch: elke workout krijgt aanbevolen sets/reps/gewicht op basis van recente
  prestaties (geschat 1RM, recentere en sterkere prestaties wegen zwaarder) [F1][F2]. Je kunt per set
  aanpassen; uitzetten van de aanbevelingen zelf kan niet (Fitbod ís de aanbeveling) **[niet geverifieerd als
  expliciete uitspraak]**. Na een pauze verlaagt Fitbod zelf de voorstellen [F3].

**2. Hoe staat het advies naast je eigen waarde? Hoe accepteer je?**
- **Strong:** aparte kolom **PREVIOUS** naast de invoervelden kg/reps ("easy ref to previous workouts weights
  and reps") [S2]. Dat vorige waarden als grijze hint in de velden staan en je met het vinkje accepteert, staat
  alleen in een niet-officiële bron: **[niet geverifieerd]**.
- **Hevy:** aparte kolom **PREVIOUS** links in de setregel; **tik op de vorige waarde** om die in de set te
  zetten [H5][H6]. De kolom verschijnt alleen tijdens een live workout, niet bij het bewerken van een routine
  [H6]. Hevy Trainer: kleine markering ("small indication") bij oefeningen met progressie [H4].
- **Fitbod:** de aanbeveling staat direct ín de velden (sets, reps, gewicht) op het Exercise Details-scherm;
  je tikt een veld aan om te wijzigen [F4][F5]. Er is geen apart "jouw waarde vs. advies": het advies is de
  invoer.

**3. Wat is de referentie voor "vorige keer"? Wat als de oefening overgeslagen is?**
- **Strong:** "previous" = vorige keer dat je de oefening deed [S2]; of dat per template of over alle workouts
  gaat: **[niet geverifieerd]**.
- **Hevy:** instelbaar: **Any workout** (laatste keer dat je de oefening deed, ongeacht routine) of **Same
  routine** (laatste keer binnen dezelfde routine) [H1][H5]. Waarden staan per set [H1]. Eerste keer: kolom
  blijft leeg [H6]. Omdat het om de laatste keer dat je de oefening *deed* gaat, telt een sessie waarin je haar
  oversloeg niet mee (afgeleid, niet letterlijk gedocumenteerd).
- **Fitbod:** geen enkele "vorige set" maar je hele recente geschiedenis (geschat 1RM) [F2]. Overslaan,
  vervangen of verwijderen beïnvloedt toekomstige aanbevelingen [F2]; niet-gelogde oefeningen tellen niet
  mee [F6].

**4. Reps-only / lichaamsgewicht en warming-up**
- **Strong:** sets kun je taggen als Warm-up, Drop of Failure; warming-ups tellen niet mee in grafieken en
  metrics [S3]. Of warming-ups in PREVIOUS verschijnen: **[niet geverifieerd]**. Lichaamsgewicht: **[niet
  geverifieerd]**.
- **Hevy:** set types Warm Up / Normal / Failure / Drop [H6]; instelling of warming-ups meetellen in stats
  [H6]. Lichaamsgewicht, assisted en weighted zijn aparte oefeningstypes [H7]; voor lichaamsgewicht toont PREVIOUS
  de reps (en eventueel extra gewicht) [H6].
- **Fitbod:** bij lichaamsgewicht telt alleen reps; extra gewicht via veld "Added Weight". Krijg je ongewild een
  gewichtsadvies, dan komt dat door eerder gelogd gewicht; zet Added Weight op 0 om te resetten [F7].
  Warming-ups via een schakelaar; geen warming-up bij lichaamsgewicht, band, timed, Max Effort of als het
  werkgewicht te laag is [F8].

## Vergelijking per app

| Vraag | Strong | Hevy (routine) | Hevy Trainer | Fitbod |
|---|---|---|---|---|
| Progressie-advies | geen gevonden | geen | ja, automatisch toegepast | ja, automatisch |
| Wat staat volgende keer klaar | templatewaarden; je kiest bij afsluiten of ze bijgewerkt worden [S1] | routinewaarden, standaard overschreven met prestatie; uit te zetten [H2] | waarden met progressie erin [H3] | aanbeveling [F1] |
| Weergave vorige keer | kolom PREVIOUS [S2] | kolom PREVIOUS, per set [H1] | markering bij progressie [H4] | geen aparte kolom; advies ín de velden [F5] |
| Accepteren | invullen/afvinken (details niet geverifieerd) | tik op vorige waarde [H5] | n.v.t. (al toegepast) | gewoon loggen, of veld aanpassen [F4] |
| Referentie | vorige keer (scope niet geverifieerd) | instelbaar: any workout / same routine [H1] | eigen prestaties in programma | hele recente geschiedenis, geschat 1RM [F2] |
| Overgeslagen / eerste keer | niet geverifieerd | leeg bij eerste keer [H6] | niet geverifieerd | overslaan is signaal; niet gelogd = telt niet [F2][F6] |
| Warming-up | tag, telt niet in metrics [S3] | settype, instelbaar of het meetelt [H6] | niet geverifieerd | optioneel, niet bij lichaamsgewicht [F8] |
| Lichaamsgewicht | niet geverifieerd | apart type [H7] | niet geverifieerd | alleen reps + Added Weight [F7] |

## Wat betekent dit voor GYM-8020

1. **Twee uitersten, één middenweg.** Fitbod laat advies het doel zijn (geen eigen doel); Hevy's routines nemen
   je prestatie standaard over als volgende waarde. Strong vraagt bij afsluiten of je het template bijwerkt.
   Ons uitgangspunt "Advies wordt nooit vanzelf Doel" zit het dichtst bij Strong en Hevy zonder Trainer.
2. **Standaard-doel als je niets aanpast:** de gangbare keuze is "wat je de laatste keer deed" (Hevy-routine,
   Strong "Update Values"), niet "laatste keer + advies". Voor ons: niets aangepast = Doel blijft gelijk aan het
   vorige Doel óf wordt Laatste keer — kies er één expliciet; neem het Advies niet stilzwijgend over.
3. **Toon Advies apart, met één tik om over te nemen.** Hevy's aparte kolom met tik-om-in-te-vullen is het
   duidelijkste gedocumenteerde patroon: je eigen waarde blijft in het veld, het voorstel staat ernaast. Een
   markering zoals bij Hevy Trainer werkt voor "hier is progressie mogelijk".
4. **Laatste keer = laatste sessie waarin je de oefening echt deed**, per set; Hevy biedt daarnaast "same
   routine". Dat sluit aan op onze GLOSSARY-definitie; overslaan telt dan vanzelf niet mee. Bij een eerste keer:
   leeg laten, niet raden.
5. **Warming-ups en lichaamsgewicht apart behandelen.** Alle drie houden warming-ups buiten de werksets en
   metrics; bij lichaamsgewicht is reps de maat (Fitbod) en is extra gewicht een apart veld. Advies voor
   warming-ups is nergens gevonden; voor ons dus: geen Advies op warming-upsets.

## Bronnen

- [S1] Strong — Save a Workout as a Template / Update a Template: https://help.strongapp.io/article/177-update-template
- [S2] Strong — App Store-beschrijving: https://apps.apple.com/us/app/strong-workout-tracker-gym-log/id464254577
  en Google Play: https://play.google.com/store/apps/details?id=io.strongapp.strong&hl=en_US
- [S3] Strong — About Warm-up, Drop Sets and Failure Sets: https://help.strongapp.io/article/166-set-tags
- [H1] Hevy — Previous Workout Values Vs. Routine Values: https://help.hevyapp.com/hc/en-us/articles/34105442929943-Previous-Workout-Values-Vs-Routine-Values-How-to-Adjust-in-Settings
- [H2] Hevy — Update Routine vs Keep Original Routine: https://help.hevyapp.com/hc/en-us/articles/38387296276375-Update-Routine-vs-Keep-Original-Routine
- [H3] Hevy — Hevy Trainer Explained: https://help.hevyapp.com/hc/en-us/articles/38385724273047-Hevy-Trainer-Explained-How-It-Builds-Your-Workout-Program
- [H4] Hevy — Hevy Trainer feature-pagina: https://www.hevyapp.com/features/workout-plan-generator/ en aankondiging: https://www.hevyapp.com/announcing-hevy-trainer/
- [H5] Hevy — How to Use Previous Workout Values: https://help.hevyapp.com/hc/en-us/articles/36011896355479-How-to-Use-Previous-Workout-Values-to-Improve-Performance-in-Hevy
- [H6] Hevy — Previous Workout Values (feature-pagina): https://www.hevyapp.com/features/track-exercises/ en Workout Settings: https://www.hevyapp.com/features/workout-settings/
- [H7] Hevy — Bodyweight Exercises in Hevy: https://help.hevyapp.com/hc/en-us/articles/38386262243223-Bodyweight-Exercises-in-Hevy-Bodyweight-vs-Assisted-vs-Weighted
- [F1] Fitbod — How Fitbod Creates Your Workout: https://help.fitbod.me/hc/en-us/articles/360004429814-How-Fitbod-Creates-Your-Workout
- [F2] Fitbod — The Fitbod Algorithm (blog): https://fitbod.me/blog/fitbod-algorithm/ en Algorithm Q&A: https://help.fitbod.me/hc/en-us/articles/16254175592215-Fitbod-s-Algorithm-Q-A
- [F3] Fitbod — Took a Break? Recalibrate: https://help.fitbod.me/hc/en-us/articles/34320813009431-Took-a-Break-Here-s-How-to-Recalibrate-Your-Fitbod-Recommendations
- [F4] Fitbod — Editing Workouts in Fitbod: https://help.fitbod.me/hc/en-us/articles/360006335593-Editing-Workouts-in-Fitbod
- [F5] Fitbod — Exercise Details Screen: Sets, Reps, and Weight Fields: https://help.fitbod.me/hc/en-us/articles/29486697282711-Exercise-Details-Screen-Sets-Reps-and-Weight-Fields-Explained
- [F6] Fitbod — Weekly Set Targets: https://help.fitbod.me/hc/en-us/articles/24739986755223-Weekly-Set-Targets
- [F7] Fitbod — Algorithm Q&A (lichaamsgewicht/Added Weight): https://help.fitbod.me/hc/en-us/articles/16254175592215-Fitbod-s-Algorithm-Q-A
- [F8] Fitbod — Warm-Up Sets: https://help.fitbod.me/hc/en-us/articles/360006337634-Warm-Up-Sets
