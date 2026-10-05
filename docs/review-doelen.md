# Review: doelen, voorstellen, materiaal en "laatste keer"

Aanleiding: feedback uit de training van 5 oktober 2026 (na v0.6.2-dev). De app gedraagt zich als beslisser
waar hij adviseur moet zijn. Uitgangspunt van de eigenaar (D-97):

> **Ik bepaal wat het doel voor volgende keer wordt. De app stelt voor en adviseert.**

Per onderdeel staat hieronder wat het nu doet (uit de code), wat er misgaat en een voorstel. De eigenaar
kiest per punt: **houden**, **aanpassen** of **verwijderen**. Nog niets gebouwd. Dit wordt opgepakt na 0.6.2
en na de werkwijze-upgrade (P-1, D-98).

---

## R-A · Automatisch voorstel voor volgende keer (± 2,5)

**Nu:** zodra je reps invult, rekent de app een plan voor volgende keer uit (`GoalAdvisor.planNext`, D-18a):
te licht → één stap zwaarder, te zwaar → één stap lichter, goed → zelfde gewicht. **Pas je niets aan, dan
wordt dat voorstel opgeslagen als je doel** zodra je naar de volgende oefening swipet of de training afrondt.

**Wat misgaat:** het voorstel wordt stilletjes een beslissing. Voorbeeld: vorige keer 80, je zou nooit +2,5
hebben gedaan, en nu staat er 82,5.

**Voorstel:** standaard is het doel **gelijk aan wat je deze keer deed**. Het voorstel staat er als advies
naast ("Advies: 82,5 — te licht") met één tik om het over te nemen. Alleen wat jij invult of overneemt wordt
het doel.

**Keuze:** ☐ houden ☐ aanpassen zoals voorgesteld ☐ verwijderen (geen advies meer)

## R-B · Wijzigingen aan het doel worden niet direct bewaard — *bug (B-6)*

**Nu:** wat je bij "Volgende keer" wijzigt staat alleen in het geheugen van het trainingsscherm. Het wordt pas
bewaard bij swipen naar de volgende oefening of bij Afronden. Ga je tussendoor uit het scherm, dan is het weg.
Kom je terug, dan staat het automatische voorstel er weer, en dat wordt bij swipen **over jouw eerdere doel
heen** opgeslagen.

**Voorstel:** elke wijziging meteen opslaan (geen opslaan-knop nodig). Terugkomen toont altijd wat jij hebt
gezet. Test-first: eerst een test die dit scenario rood laat zien.

**Keuze:** fix (geen discussie nodig, dit is een bug)

## R-C · Warming-up gaat niet mee naar volgende keer — *bug (B-7)*

**Nu:** het plan bewaart een warming-up wel als set, maar zonder gewicht en reps. Bij "Laatste keer" staat de
warming-up niet.

**Voorstel:** warming-up gaat mee met gewicht en reps zoals je hem deed, en staat bij "Laatste keer".

**Keuze:** ☐ zo ☐ anders: …

## R-D · Oefeningen op reps: doelen kloppen niet en zijn niet in te stellen — *bug (B-8), was V-3*

**Nu:** het plan voor volgende keer kent alleen gewicht, geen reps. Bij een oefening op reps (bijv. decline
crunch) toont "Volgende keer" de algemene targets (zwaar 6, licht 16) uit Instellingen. Die zeggen niets, en
je kunt ze niet aanpassen. De kop van de set zegt "Heavy · target 6" terwijl je 14 doet.

**Voorstel:** bij een oefening op reps is het doel **een aantal reps**, standaard het aantal van de laatste
sessie, en zelf aan te passen. De kop toont dat doel ("Doel 14"). De algemene targets 6/16 gelden alleen voor
oefeningen op gewicht.

**Keuze:** ☐ zo ☐ anders: …

## R-E · Melding "Niet mogelijk met je materiaal · dichtstbij …"

**Nu:** typ je een gewicht dat volgens het materiaalprofiel niet kan (bijv. −53 bij pull-up met ondersteuning
per 5), dan toont de app "Niet mogelijk met je materiaal · dichtstbij −55 kg" (D-28).

**Wat misgaat:** het klopt niet altijd met de werkelijkheid, en de toon is betuttelend. Jij bepaalt.

**Voorstel:** **verwijderen.** Hooguit later een neutrale hulp bij het opladen (schijven per kant), zonder oordeel.

**Keuze:** ☐ verwijderen ☐ neutraal herschrijven: …

## R-F · Melding "Buiten het bereik van dit materiaal…"

**Nu:** boven het maximum van het profiel staat onder het gewicht: "Outside the range of this equipment. Fine
if your gym has it; adjust the equipment under Settings." (O-2, v0.6.1).

**Voorstel:** **verwijderen.** Het profiel past zich vanzelf aan als je er bovenuit gaat, of we laten het
helemaal los (zie R-G).

**Keuze:** ☐ verwijderen ☐ houden

## R-G · Materiaalprofielen in het algemeen

**Nu:** elk profiel heeft een stap en een bereik. Dat bepaalt +/−, het opladen (schijven per kant), de
meldingen R-E en R-F en het voorstel R-A ("één haalbare stap").

**Vraag:** wat wil je ervan houden? Opties:
1. alleen de schijvenhulp (wat moet erop per kant), verder niets;
2. ook +/− in haalbare stappen (wel handig, nooit blokkerend);
3. alles weg, +/− met de gewichtsstap uit Instellingen.

**Keuze:** ☐ 1 ☐ 2 ☐ 3

## R-H · "Laatste keer": wat is dat precies?

**Nu, per plek:**

| Plek | Wat "laatste keer" nu is |
|---|---|
| Trainingsscherm, regel "Laatste keer" | de laatste **afgeronde training waarin deze oefening voorkwam** (niet overgeslagen) |
| Plan voor volgende keer | wat bij Afronden / swipen is bewaard (R-A, R-B), los van welke training |
| Samenvatting, doelen gehaald | gewicht: het geplande gewicht gehaald; reps: minstens zoveel reps als de laatste keer van die oefening |
| Samenvatting, heatmap "t.o.v. vorige keer" | **per oefening** de vorige keer van die oefening; per spier het gewogen gemiddelde van de procentuele verandering van de oefeningen die je **vandaag** deed |
| Vergelijkbare oefeningen (D-68) | bij een nieuwe oefening: de laatste keer van oefeningen met dezelfde spieren |

**Wat misgaat:** sla je twee armoefeningen over, dan tellen die niet mee. De armspieren krijgen dan een
percentage van andere oefeningen (bijv. pull-up voor de biceps), of verschijnen als "gelijk". Er staat alleen
een percentage, geen absoluut getal.

**Voorstel, eerst samen vastleggen:**
1. "Laatste keer" van een **oefening** = de laatste sessie waarin je hem echt deed. (Zo is het nu.)
2. Vergelijking van een **workout** = deze sessie tegenover de vorige sessie van dezelfde workout.
3. Vergelijking per **spier** = totaal volume per spier deze sessie tegenover de vorige sessie van dezelfde
   workout; overgeslagen oefeningen tellen als 0, zichtbaar als "overgeslagen".
4. Overal **absoluut én percentage** (bijv. "+120 kg volume · +4%").

**Keuze:** ☐ zo ☐ anders: …

## R-I · Plan bijwerken bij het achteraf aanpassen van een training

**Nu:** pas je een afgeronde training aan en is dat de laatste training met die oefening, dan rekent de app
het plan opnieuw uit (`recomputePlan`). Daarmee overschrijft het automatische voorstel (R-A) wat jij eerder
als doel had gezet.

**Voorstel:** nooit een doel overschrijven dat jij zelf hebt gezet. Hooguit het advies bijwerken.

**Keuze:** ☐ zo ☐ anders: …

---

## Samenhang

- R-A, R-B, R-D en R-I gaan alle vier over één ding: **het doel voor volgende keer is van jou**. Bouwen we
  samen, met één aanpassing aan de database (doel per set met gewicht én reps, "zelf gezet" of "advies"). Dat
  past in de migratie van 0.7 (schema v3, A-1).
- R-E, R-F en R-G gaan over materiaal. Eerst R-G kiezen, dan volgen R-E en R-F vanzelf.
- R-H gaat over definities. Eerst vastleggen in de SPEC (woordenlijst), dan bouwen. Raakt V-2 en O-1.
