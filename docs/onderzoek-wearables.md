# Inventarisatie: wearables en gezondheidsdata (O-3)

> Status: inventarisatie, nog niets besloten of gebouwd. Beslispunten onderaan.

## De vraag

Kan GYM-8020 koppelen met wearables (Garmin op nummer 1, verder Fitbit, Samsung, Oura, Whoop, slimme
weegschalen, …) om gezondheidsdata in de app te gebruiken? Wat kan, wat kost het, en wat past bij de
uitgangspunten: lokaal, geen eigen server, geen accounts (D-02, D-77)?

## De drie routes

### Route A · Health Connect (Android) — *aanbevolen*

Health Connect is de centrale gezondheidsopslag van Android, **op de telefoon zelf**. Apps van wearables
schrijven erin, andere apps lezen eruit, met toestemming per gegevenssoort.

- **Garmin** levert sinds juni 2025 data aan Health Connect, **alleen in één richting**: Garmin → Health
  Connect. Wat erin komt:
  - **activiteit:** calorieën, afstand, hartslag, stappen, enz.;
  - **welzijn:** hartslag, slaapfases, stappen, gewicht, vetpercentage, verbrande calorieën.

  Niet beschikbaar: Body Battery, Training Status en Training Load, HRV-status en de geavanceerde
  hardloopstatistieken. Die blijven in Garmin Connect.
- **Ook via Health Connect:** Fitbit, Samsung Health, Oura, Whoop, Withings (weegschalen), Xiaomi Mi Fitness,
  Coros en andere. Eén koppeling dekt dus vrijwel alle merken.
- **Past bij onze principes:** geen server, geen account, de data verlaat de telefoon niet. De gebruiker geeft
  per soort toestemming en kan die altijd intrekken.
- **Schrijven kan ook:** GYM-8020 kan zijn trainingen als "krachttraining" in Health Connect zetten, met
  oefeningen, sets en reps. Dan zien Fitbit, Samsung en andere apps je trainingen ook. Garmin neemt geen data
  áán, dus Garmin Connect ziet ze niet.
- **Werk:** middelgroot. Het gaat om een bibliotheek, toestemmingsschermen en een privacyuitleg in de app.

### Route B · Garmin Connect Developer Program (Health/Activity API) — *niet geschikt*

De officiële Garmin-cloud-API.

- **Niet zelf aan te vragen:** je moet een aanvraag indienen en goedgekeurd worden. Het programma is gericht op
  bedrijven. Er zijn meldingen van afgewezen aanvragen en berichten dat het programma tijdelijk gepauzeerd
  is; dat laatste heb ik niet kunnen verifiëren.
- **Werkt via een eigen server:** Garmin stuurt de data naar een server van ons. Dat botst met
  "geen eigen server, geen accounts" en brengt AVG-plichten mee voor gezondheidsdata.
- Voordeel: meer Garmin-specifieke data (Training Load, HRV-status, Body Battery).

### Route C · Eigen Connect IQ-app op het horloge — *later, groot*

Een kleine GYM-8020-app op je Garmin-horloge, geschreven in Garmins eigen taal (Monkey C).

- Je ziet je doel en sets op je pols en tikt reps aan zonder telefoon. Die worden gesynchroniseerd met de app
  op je telefoon.
- Andere apps doen dit al, bijv. Rack, BulkForged en Gym Workout Tracker. Het kan dus.
- **Beperking:** je kunt sets niet netjes in Garmins eigen krachttraining-registratie schrijven. Er bestaat
  geen ondersteunde manier om strength-sets weg te schrijven.
- **Werk:** groot. Het is een tweede app in een andere taal, met eigen tests en eigen publicatie in de
  Connect IQ Store.

## Tweerichtingsverkeer: van GYM-8020 naar je wearable

De wens: niet alleen data ophalen, maar ook wat je in GYM-8020 vastlegt terugsturen naar je wearable of de app
ervan. Dat kan op drie manieren, en niet elke manier werkt voor elk merk.

| Richting | Fitbit, Samsung, Google, Oura, Whoop, … | Garmin |
|---|---|---|
| **Afgeronde training** (oefeningen, sets, reps, gewicht) | **Ja, via Health Connect.** De training verschijnt als krachttraining in die apps. | **Niet via Health Connect** (Garmin neemt niets aan). Wel mogelijk: een **FIT-bestand** dat je zelf in Garmin Connect importeert (*Importeren* op connect.garmin.com). Of Garmin daarin sets en reps netjes toont, moeten we **testen**: dat lukt alleen als de oefeningen goed gecodeerd zijn. |
| **Geplande training** (je doel voor vandaag naar je horloge) | Meestal niet: die apps hebben geen ingang voor workouts van andere apps | Officieel via de **Garmin Training API**: vraagt goedkeuring en een eigen server, en voor krachttraining is die beperkt. Hevy kreeg alleen een beperkte koppeling. Alternatief: onze eigen **horloge-app** (route C) toont je doel op je pols. |
| **Hartslag en gewicht** terug | Niet nodig: die komen juist van de wearable | idem |

**Conclusie:**
- **Voor de meeste merken is tweerichtingsverkeer goed haalbaar via Health Connect.** Je data gaat erheen en
  komt eruit: training en sets erheen, hartslag, gewicht en slaap eruit.
- **Voor Garmin werkt de terugweg alleen met een omweg:** een FIT-bestand dat je zelf importeert (moet getest
  worden) of een eigen horloge-app. Een automatische koppeling met Garmin Connect zit achter het
  goedkeuringsprogramma, met een server, en is voor krachttraining beperkt. Garmin houdt die deur vooralsnog
  dicht voor krachtapps.

| # | Idee (tweerichting) | Werk |
|---|---|---|
| W-5 | Afgeronde trainingen naar Health Connect (Fitbit, Samsung, Google, …) | Middel |
| W-8 | Training als **FIT-bestand** exporteren, om zelf in Garmin Connect te importeren. Eerst een proef: blijven sets en reps behouden? | Middel; proef klein |
| W-9 | Doel van vandaag op je **Garmin-horloge** via een eigen Connect IQ-app (route C), resultaten terug naar de telefoon | Groot |

## Wat zouden we met de data doen?

Gesorteerd van meeste naar minste waarde voor deze app:

| # | Idee | Data (Health Connect) | Waarde |
|---|---|---|---|
| W-1 | **Lichaamsgewicht automatisch** van je weegschaal of Garmin, nooit meer intikken | gewicht, vetpercentage | Hoog: belangrijk voor oefeningen met eigen gewicht (D-16) |
| W-2 | **Hartslag per training en per oefening**: we hebben al tijdstempels per oefening (GO → klaar), dus gemiddelde en maximale hartslag per oefening, en hoe snel je hartslag zakt tijdens het wisselen | hartslag | Hoog: nieuw inzicht dat andere apps niet per oefening geven |
| W-3 | **Calorieën per training** in de samenvatting | calorieën | Middel |
| W-4 | **Herstel-hint vóór de training**: slecht geslapen of hoge rusthartslag → *"Herstel lijkt minder; geen PR-poging vandaag?"*. Alleen informatief, nooit sturend | slaap, rusthartslag, HRV | Middel: past bij "te zwaar / te licht"; moet voorzichtig |
| W-5 | **Trainingen terugschrijven** naar Health Connect, zodat ze in Fitbit, Samsung enz. verschijnen | – (schrijven) | Middel; niet voor Garmin |
| W-6 | **Stappen en activiteit op rustdagen** in het dashboard | stappen | Laag |
| W-7 | **Garmin-activiteit herkennen**: heb je op je horloge "Krachttraining" gestart, dan vraagt de app *"Training van 18:05–19:02 gevonden, invoeren?"* (koppelt aan D-80) | trainingen | Middel: handig als je je telefoon niet bij je had |

## Kanttekeningen

- **Privacy:** gezondheidsdata is gevoelig. Met route A blijft alles op de telefoon. De app vraagt per soort
  toestemming en legt in gewone taal uit waarom. Data uit Health Connect komt niet in de exportbestanden,
  tenzij je dat zelf kiest.
- **Vertraging:** Garmin synchroniseert naar Health Connect als de Garmin Connect-app synchroniseert. De
  hartslag van een training kan dus een paar minuten later binnenkomen. De samenvatting moet dat netjes
  afhandelen, met *"Hartslag volgt zodra Garmin gesynchroniseerd heeft"*.
- **Android 14+:** Health Connect zit ingebouwd. Op Android 8–13 is het een losse app uit de Play Store.
- **Testen** (TESTING.md): de koppeling krijgt een nep-Health-Connect in de tests. Er komen nieuwe flows bij,
  bijv. "gewicht uit Health Connect wordt gebruikt bij pull-ups".

## Advies

1. **Route A, Health Connect**, beginnen met **W-1 (gewicht)** en **W-2 (hartslag per oefening)**. Dat levert
   het meeste op, werkt met Garmin én met vrijwel alle andere merken, en blijft lokaal.
2. Daarna **tweerichting**: W-5 (trainingen naar Health Connect) en een proef met W-8 (FIT naar Garmin).
   Dan W-7 en W-3. W-4 pas na een eigen onderzoek naar wat zinvol en verantwoord is.
3. **Route B niet doen** zolang we geen server willen.
4. **Route C (horloge-app)** op de lange lijst. Leuk, maar een tweede product.

## Beslispunten

| # | Vraag | Mijn advies |
|---|---|---|
| O-3a | Route A (Health Connect) als koppeling voor alle wearables? | Ja |
| O-3b | Eerste stap: automatisch gewicht (W-1) en hartslag per oefening (W-2)? | Ja |
| O-3c | Tweerichtingsverkeer: trainingen naar Health Connect (W-5) | Ja, direct na W-1/W-2 |
| O-3e | Proef met FIT-export voor Garmin Connect (W-8) | Ja, eerst een kleine proef |
| O-3d | Horloge-app voor Garmin (route C)? | Lange lijst |

## Bronnen

- Garmin Training API (workouts naar Garmin Connect): https://developer.garmin.com/gc-developer-program/training-api
- Beperkte Garmin-koppeling voor krachtapps (Hevy): https://the5krunner.com/2026/03/24/garmin-connect-plus-strength-apps/
- Sets en oefeningen in FIT-bestanden: https://forums.garmin.com/developer/fit-sdk/f/discussion/350594/how-to-encode-strength-training-into-fit/1689937
- Sets en reps uit Garmin-FIT: https://forum.intervals.icu/t/pulling-strength-workout-exercise-sets-reps-weight-from-garmin/123171

- Garmin en Health Connect, welke data: https://gadgetsandwearables.com/2025/07/08/garmin-google-health-connect-launch/
- Garmin en Health Connect, aankondiging: https://www.notebookcheck.net/Garmin-and-Health-Connect-integration-on-the-way-to-smartwatch-users.1022327.0.html
- Garmin deelt niet terug vanuit Health Connect: https://tech.yahoo.com/wearables/articles/garmin-wont-share-data-google-173520201.html
- Health Connect (overzicht): https://en.wikipedia.org/wiki/Health_Connect
- Health Connect-apps (Fitbit, Samsung, Oura, Whoop, …): https://androidauthority.com/google-health-connect-3164755
- Garmin Health API (aanvraag en goedkeuring): https://developer.garmin.com/gc-developer-program/health-api/
- Afgewezen aanvragen, Garmin-forum: https://forums.garmin.com/developer/connect-iq/f/discussion/434542/garmin-connect-developer-program---access-request-rejected-without-notification/2025653
- Melding over pauze van het programma (niet geverifieerd): https://tryterra.co/blog/garmin-connect-developer-program-pause
- Connect IQ krachttraining-apps: https://forums.garmin.com/developer/connect-iq/f/showcase/435840/rack-strength-training-companion-for-garmin-ios-android-in-beta-fit-based
- Geen ondersteunde manier om strength-sets weg te schrijven: https://forums.garmin.com/developer/connect-iq/f/discussion/441539/has-anyone-been-able-to-map-strength-exercises-to-garmin-connect/2054961
