# Vormgeving — GYM-8020

> Uitwerking van SPEC sectie 14. Bron: IBS-logo (`assets/branding/ibs-logo.webp`) en screenshots van i-b-s.nl (materiaal van IBS, waar de eigenaar werkt; screenshots met personen worden niet in de repo opgenomen), aangevuld met een art-deco-laag op verzoek van de eigenaar.

## 1. Analyse IBS-huisstijl

| Kenmerk | Wat we zien | Vertaling naar de app |
|---|---|---|
| **Logo** | Vier gele trapeziumvlakken die samen een vierkant vormen, met witte naden, iets gedraaid (≈ −5°). Woordmerk "IBS" in crème, geometrisch schreefloos. | Knipoog: de arm in het app-icoon komt uit dit merk; het merk zelf klein in "Over" (zie 5). |
| **Kleur** | Warm geel `#F9C852` (gemeten uit het logo); crème `#FEF9ED` voor het woordmerk. | Geel = enige accentkleur. Crème i.p.v. puur wit voor tekst: warmer, minder hard. |
| **Achtergrond** | Bijna-zwart `#171717`, kaarten in donkergrijs (`#232323` … `#3A3A3A`). | Donker thema met drie lagen oppervlak. |
| **Gloed** | Zachte radiale gloed: amber/bruin (`#613F0F`) rechtsboven, geel links. | Subtiele radiale gloed achter kopteksten en achter de timer. |
| **Kaarten** | Grote hoekradius (±24 px), geen randen, lichte tint-verloop. | Kaarten met radius 24 dp, geen schaduw, oppervlak-kleur. |
| **Typografie** | Geometrisch schreefloos; koppen **licht/regular** en groot, niet vet. Kleine labels in HOOFDLETTERS met ruime letterspatiëring, grijs, vet. | Koppen Regular/Light, groot. Labels (bijv. `ZWAAR · TARGET 6`) in caps met spatiëring. Alleen de **cijfers** (gewicht, timer) vet. |
| **Knoppen** | Zwarte knop met witte omlijning (2 px), ronde hoeken, chevron `›`. Tekstlinks in geel ("Meer info"). Ronde gele zwevende knop. | Secundaire knoppen: omlijnd. Primaire actie (GO): gevuld geel, zwarte tekst. Links/tertiair: gele tekst. |
| **Iconen** | Witte lijn-iconen in een afgeronde vierkante omlijning. | Lijn-iconen, 2 dp lijndikte, crème. |
| **Scheidingslijn** | Dubbele dunne goudlijn links en rechts van een klein geel vierkant (het logo-merk). | Al bijna art deco — wordt ons standaard sectiescheider-ornament. |

## 2. Kleurtokens

| Token | Waarde | Gebruik |
|---|---|---|
| `bg` | `#171717` | schermachtergrond |
| `surface` | `#232323` | kaarten |
| `surfaceHigh` | `#3A3A3A` | verhoogde kaart, invoervelden |
| `primary` | `#F9C852` | IBS-geel: GO-knop, doelgewicht, actieve status |
| `onPrimary` | `#171717` | tekst op geel |
| `text` | `#FEF9ED` | hoofdtekst (crème) |
| `textMuted` | `#9A958C` | labels, secundaire tekst |
| `gold` | `#C9A24A` | art-deco-lijnwerk (gedempter dan `primary`, zodat ornament nooit concurreert met data) |
| `glow` | `#613F0F` | radiale achtergrondgloed |
| `positive` | `#7FB77E` | vooruitgang (heatmap, records) |
| `negative` | `#D9725B` | achteruitgang (heatmap) — spaarzaam, D-22: motiverend |
| `advice` | `#8FB3D9` | advies van de app (stippel-chip in "Volgend doel", #63), los van het geel van je doel |

Contrast: `text` op `bg` en `onPrimary` op `primary` ruim boven WCAG AA.

## 3. Typografie

- **Poppins** (OFL): Light/Regular voor koppen, Regular voor tekst, **SemiBold/Bold alleen voor cijfers**.
- Cijfers met *tabular figures*, zodat timers niet verspringen.
- Schaal: doelgewicht 72 sp · timer 96 sp · koppen 28–32 sp · tekst 16 sp · labels 12 sp caps, letterspatiëring 0,15 em.

## 4. Art-deco-laag

Eigen vectortekeningen (Compose Canvas / VectorDrawable), geen overgenomen afbeeldingen. Lijnwerk in `gold`, dun (1–1,5 dp), symmetrisch, op zwart — zoals het klassieke zwart-goud art deco.

**Verhouding:** ongeveer **80 % strak** (dubbele lijnen, schuine knikken, getrapte vormen, lijnen die elkaar kruisen en voorbij de hoek doorlopen, ruiten, waaiers) en **20 % gebogen, floraal en speels** (krullen, bladvormen, bogen). Het mag rijk en uitbundig zijn. Op de trainingsschermen blijft het ondergeschikt aan de cijfers.

Preview: https://claude.ai/artifact/3jfeGc8Sqtak3Zvh5so4WX

| Ornament | Vorm | Waar |
|---|---|---|
| **Scheider** | Dubbele lijn ← ◆ → met in het midden het gedraaide IBS-vierkantje of een ruit. | Tussen secties, boven kopteksten. |
| **Zonnewaaier** | Halve waaier van stralen vanuit één punt (sunburst). | Achter de 3-2-1-aftelling en de timer; groot op het "Done for today"- en samenvattingsscherm; opflitsend bij een **record**. |
| **Getrapte hoeken** | Hoekornament met drie getrapte lijnen. | Hoeken van de doelgewicht-kaart en van de samenvatting. |
| **Ruitenpatroon** | Fijn diagonaal ruitrooster met waaiertjes, 4–6 % dekking. | Achtergrond van opstartscherm, beginscherm-header, samenvatting. |
| **Schijven** | Halterschijven als getrapte, symmetrische balken (art-deco-silhouet). | Schijvenverdeling onder het doelgewicht (v0.2). |
| **Lichaamsfiguur** | Lijnfiguur met gouden contour, spieren als vlakken. | Heatmap (v0.4). |

**Terughoudendheid:** trainingsschermen blijven rustig en leesbaar op armlengte — daar alleen de scheider en de zonnewaaier achter de timer. Ornament mag nooit een cijfer of knop verstoren.

## 5. App-icoon en merk

De app staat los van IBS, met een subtiele knipoog ernaar (oktober 2026).

- **De arm** (`assets/branding/arm-mark.svg`, `ic_arm_mark`): het IBS-merk zonder het bovenste vlak, met drie punten verplaatst. Het linker vlak is onderarm en hand, het onderste de bovenarm (de hoek bij het rechter vlak omhoog: de biceps), het rechter de schouder (rechtsboven omlaag).
- **Het app-icoon** (`ic_launcher_*`, `ic_app_emblem`): de arm in een halterschijf: een dubbele gouden ring met 8 streepjes en 8 mini-IBS-merkjes die met de ring meedraaien. Plus een eenkleurige versie voor Android-thema's.
- **Gebruik:** het embleem op het opstartscherm, de introductie en "Over"; de arm klein op het beginscherm.
- **IBS:** het volledige IBS-merk staat alleen klein in "Over" (Instellingen), met een link naar i-b-s.nl.

## 6. Componenten (kort)

- **GO-knop**: groot, gevuld geel, volle breedte onderaan, radius 24 dp.
- **Doelkaart**: label in caps (`ZWAAR · TARGET 6`), gewicht groot in geel, schijven klein eronder in `textMuted`, getrapte hoeken.
- **Invoer reps**: grote stepper (− / +) met tikvlakken ≥ 56 dp.
- **Knoppen secundair**: omlijnd crème, chevron `›`, zoals op de website.
