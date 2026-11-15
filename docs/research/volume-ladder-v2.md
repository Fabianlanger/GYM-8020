# Onderzoek: een gelijkmatige ladder van ~60 tekenbare objecten (100 kg – 10.000 ton)

> Status: onderzoek, geen besluit. Datum: 2026-11-15. Vervolg op `docs/research/volume-tastbaar.md` (issue #101).

## Waarom een nieuwe ladder

De eerste ladder (36 objecten) was scheef: tussen 100 kg en 6 ton zaten veel dingen dicht op elkaar, en
boven 50 ton kwamen nog maar drie objecten (blauwe vinvis, Vrijheidsbeeld, Eiffeltoren) met sprongen van ×2 en
×36. Met drie soorten badges (per training ~1,5–25 t, per week ~5–80 t, sinds je begon tot ~5.000–10.000 t)
moet de hele schaal van 100 kg tot 10.000 t gelijkmatig gevuld zijn.

Doel: elke stap ongeveer ×1,2 (log-schaal), geen stap groter dan ×1,4, onder 1 ton geen stap kleiner dan ×1,1.
Van 100 kg naar 10.000.000 kg is ×100.000; met ×1,2 per stap zijn dat ~63 stappen. Deze ladder heeft er **59
objecten (58 stappen), gemiddeld ×1,22**.

## Methode en betrouwbaarheid

- Webpagina's kon ik vanuit deze omgeving **niet openen** (WebFetch geblokkeerd, net als bij het eerste
  onderzoek). Alle cijfers komen uit de **geïndexeerde tekst van zoekresultaten**, met de URL van de pagina
  erbij. Ik heb de pagina's zelf dus niet gelezen; bij twijfel staat dat in de bronkolom.
- Klassen zoals in het eerste onderzoek:
  **A** = primaire bron (fabrikant, overheid, dierentuin, officiële site van het object);
  **B** = betrouwbare secundaire bron of meerdere bronnen die overeenkomen;
  **C** = eigen schatting, één zwakke bron of bronnen die elkaar tegenspreken: **verifiëren vóór gebruik**.
- "t" = metrische ton (1.000 kg). Amerikaanse "tons" (907 kg) en "pounds" (0,454 kg) zijn omgerekend.
- **App-gewicht** = het getal voor de app: typisch en bewust aan de voorzichtige kant (liever te laag, zodat
  "je tilde meer dan een …" waar is). Waar ik bewust onder het gemiddelde zit, staat dat in de bandbreedte.
- **Stap** = app-gewicht gedeeld door dat van de regel erboven. Rekenscript: `gen.py` naast dit bestand.
- **Tekenen**: per object 1–2 kenmerken die in een art-deco-lijnicoon zichtbaar moeten zijn om het te
  herkennen én van de buren te onderscheiden.

## De ladder (gesorteerd op gewicht)

| # | Object | App-gewicht | Stap | Bandbreedte | Bron | Kl. | Tekenen |
|---|---|---|---|---|---|---|---|
| 1 | een reuzenpanda | 100 kg | — | ♂ tot ~113 kg, ♀ ~100 kg | [Smithsonian's National Zoo, backgrounder](https://nationalzoo.si.edu/sites/default/files/documents/giantpandasbackgrounder_2021.08.20.pdf); [SDZ factsheet](https://ielc.libguides.com/sdzg/factsheets/giantpanda/summary) | A | zwarte oogvlekken en oren op witte kop; bamboestengel in de poot |
| 2 | een struisvogel | 115 kg | 1,15 | ♂ 100–130 kg | [San Diego Zoo](https://animals.sandiegozoo.org/node/118) | A | lange kale nek met klein kopje; dikke donsveren-romp op twee lange poten |
| 3 | een sumoworstelaar | 140 kg | 1,22 | gem. topdivisie 164 kg; 120–200+ | Japan Sumo Association via [Japan Times](https://japantimes.co.jp/sports/2019/05/22/sumo/sumo-101-large-rikishi) | B | haarknot (chonmage) en mawashi-gordel; brede hurkhouding |
| 4 | een Shetlandpony | 160 kg | 1,14 | 150–200 kg | [Tierpark Hellabrunn](https://hellabrunn.de/en/animals/europe/shetland-pony) | A | korte dikke poten, lange warrige manen en pony over de ogen |
| 5 | een leeuw | 180 kg | 1,12 | ♂ 150–225 kg, gem. 190 | [SDZ factsheet](https://ielc.libguides.com/sdzg/factsheets/lions/characteristics) | A | grote manen rond de kop; staart met kwastje |
| 6 | een piano (buffetpiano) | 230 kg | 1,28 | 236–272 kg | [Steinway UK](https://steinway.co.uk/?p=1888) (eerste onderzoek) | A | rechtopstaande kast met klavier; kandelaarsarmpjes of open klep |
| 7 | een zebra | 270 kg | 1,17 | ♂ 220–373 kg | [SDZ factsheet vlaktezebra](https://ielc.libguides.com/sdzg/factsheets/plains_zebra/characteristics) | A | zwart-witte strepen; rechtopstaande borstelmanen |
| 8 | een dolfijn (tuimelaar) | 300 kg | 1,11 | 136–635 kg; meeste dieren lichter | [NPS Channel Islands](https://www.nps.gov/chis/learn/nature/bottlenose-dolphin.htm) (max ≥ 635 kg); [NOAA](https://www.fisheries.noaa.gov/species/common-bottlenose-dolphin) (cijfer niet in snippet) | C | korte snavel en gebogen rugvin, springend in een boog boven een golf |
| 9 | een ijsbeer | 400 kg | 1,33 | ♂ 350–650 kg | [SeaWorld](https://seaworld.org/animals/facts/mammals/polar-bear/) (eerste onderzoek) | A | lange nek en kleine kop; staand op ijsschots |
| 10 | een concertvleugel | 480 kg | 1,20 | 450–480 kg | [Steinway UK FAQ](https://steinway.co.uk/?p=1831) (eerste onderzoek) | A | vleugelvormige kast met opengezette klep op stok; drie poten |
| 11 | een Fries paard | 560 kg | 1,17 | 540–635 kg | [Mad Barn rasprofiel](https://madbarn.com/friesian-horse-breed-profile/) (eerste onderzoek) | C | golvende lange manen en 'sokken' (behang) boven de hoeven; gebogen hals |
| 12 | een koe | 650 kg | 1,16 | 600–770 kg | [CBS-factsheet](https://www.cbs.nl/-/media/imported/documents/2015/14/factsheet-nieuwe-personenautos-in-nederland.pdf?la=nl-nl) (eerste onderzoek) | B | zwartbonte vlekken, uier en koebel |
| 13 | een bizon | 800 kg | 1,23 | ♂ tot ~900 kg, ♀ ~450 kg | [NPS Bison Facts](https://www.nps.gov/articles/bison-facts.htm) | A | hoge schouderbult met dikke wollige vacht; korte gebogen hoorntjes |
| 14 | een kuub water | 1 t | 1,25 | exact | definitie (1 L water ≈ 1 kg) | A | kubus met golfjes op het bovenvlak en '1 m³' |
| 15 | een giraf | 1,2 t | 1,20 | ♀ ~830, ♂ tot 1.360 kg | [Smithsonian](https://movementoflife.si.edu/species/giraffe) (eerste onderzoek) | A | heel lange nek met vlekkenpatroon; hoorntjes (ossicones) |
| 16 | een nijlpaard | 1,5 t | 1,25 | ♂ gem. 1.546 kg | [SDZ factsheet](https://ielc.libguides.com/sdzg/factsheets/hippopotamus/characteristics) (eerste onderzoek) | A | enorme opengesperde bek met slagtanden; oogjes en oortjes bovenop |
| 17 | een zeeolifant | 2 t | 1,33 | ♂ ~2.000–2.300 kg | [NOAA Sanctuaries](https://sanctuaries.noaa.gov/magazine/5/monterey-bay) (4.400 lb); [NPS](https://www.nps.gov/places/northern-elephant-seal.htm) (tot 5.000 lb) | A | hangende 'slurf'-neus; rechtopgerichte voorkant van het lijf |
| 18 | een neushoorn | 2,3 t | 1,15 | ♀ 1.800, ♂ 2.300 kg | [SDZ factsheet](https://ielc.libguides.com/sdzg/factsheets/whiterhino/characteristics) (eerste onderzoek) | A | twee hoorns op de neus; brede vierkante lip |
| 19 | een traumahelikopter | 2,9 t | 1,26 | max. startgewicht 2.980–3.100 kg (leeg ~1.500) | [Airbus H135-datasheet](https://mediaassets.airbus.com/pm_38_379_379543-7neugrgrhp.pdf) | A | rotorbladen bovenop, staartrotor in een ring (fenestron), ski's |
| 20 | een volgeladen bestelbus | 3,5 t | 1,21 | max. voor rijbewijs B | rijbewijs-B-grens (eerste onderzoek; URL nog toevoegen) | B | hoge kastvorm met schuifdeur opzij |
| 21 | een orka | 4,2 t | 1,20 | ♂ 3,6–5,4 t | [SeaWorld](https://seaworld.org/animals/facts/mammals/killer-whale/); [NOAA](https://www.fisheries.noaa.gov/species/killer-whale) (eerste onderzoek) | A | zwart-wit met witte oogvlek; hoge rechte rugvin |
| 22 | een monstertruck | 5,4 t | 1,29 | 4,5–5,4 t (min. 10.000 lb) | [Monster Jam fun facts](https://www.monsterjam.com/education/fun-facts/) (12.000 lb) | A | manshoge wielen onder een klein pick-uplijfje; vering zichtbaar |
| 23 | een olifant (Afrikaanse stier) | 6 t | 1,11 | ♂ gem. 6 t, ♀ ~3 t | [SDZ factsheet](https://ielc.libguides.com/sdzg/factsheets/african_elephant/characteristics) (eerste onderzoek) | A | slurf, slagtanden en heel grote oren |
| 24 | de grote klok van de Dom | 8 t | 1,33 | 8.095–8.227 kg | [Wikipedia Domtoren (tabel klokken)](https://en.wikipedia.org/wiki/Dom_Tower_of_Utrecht); [Bossche Encyclopedie](https://www.bossche-encyclopedie.nl/overig/klokken/documenten/gvw1.pdf) | B | klokvorm met klepel en jukbalk; luidwiel ernaast |
| 25 | een T. rex | 9 t | 1,12 | 8,4–14 t (schatting) | Field Museum 'SUE' via [ScienceDaily](https://www.sciencedaily.com/releases/2011/10/111012185634.htm) (eerste onderzoek) | B | grote kop met tanden, piepkleine armpjes |
| 26 | de Hubble-ruimtetelescoop | 11,1 t | 1,23 | 10,9–12,2 t (na reparaties ~27.000 lb) | [NASA](https://science.nasa.gov/missions/hubble/a-look-at-the-numbers-as-nasas-hubble-space-telescope-enters-its-25th-year/) (24.500 lb) | A | cilinder met open klepje aan één kant; twee zonnepanelen als vleugels |
| 27 | een moai (Paaseiland-beeld) | 12,5 t | 1,13 | gem. 12,5 t, enkele 80 t | [Wikipedia Moai](https://en.wikipedia.org/wiki/Moai); kanttekening [Easter Island Statue Project](https://www.eisp.org/544/) | C | lang hoofd met zware wenkbrauwen en lange neus; rode hoed (pukao) |
| 28 | een maanlander | 15 t | 1,20 | 14,7–16,4 t (volgetankt) | [Wikipedia Apollo LM](https://en.wikipedia.org/wiki/Apollo_Lunar_Module); geen NASA-primaire bron in resultaten | B | vier gespreide poten met schotelvoeten; hoekige cabine met goudfolie |
| 29 | een walvishaai | 18 t | 1,20 | tot ~18,7 t (20,6 short tons) | [Georgia Aquarium](https://georgiaaquarium.org/animal/whale-shark) | B | brede platte bek aan de voorkant; witte stippenpatroon |
| 30 | een steen van Stonehenge | 22 t | 1,22 | gem. 25 tons; Heel Stone ~30 tons | [English Heritage](https://www.english-heritage.org.uk/visit/places/stonehenge/history-and-stories/building-stonehenge/) | A | twee staande stenen met een dwarssteen erop (trilithon) |
| 31 | een bultrug | 30 t | 1,36 | 25–36 t | [NOAA FAQ](https://www.nefsc.noaa.gov/faq/whales-faq.html) (eerste onderzoek) | A | extreem lange witte borstvinnen; knobbels op de kop, staartvin met witte onderkant |
| 32 | een passagiersvliegtuig (Boeing 737) | 41 t | 1,37 | 737-800: 41,1–41,4 t | [Boeing 737NG Airport Planning](https://www.boeing.com/content/dam/boeing/v2/airports/acaps/737NG_REV_C.pdf) (eerste onderzoek) | A/B | twee motoren onder de vleugels, opstaande wingtips (winglets); korte romp |
| 33 | een volle vrachtwagen | 50 t | 1,22 | wettelijk max. 50 t | [Rijksoverheid](https://www.rijksoverheid.nl/themas/verkeer-en-vervoer/verkeersveiligheid/veilig-rijden-in-de-vrachtwagen) (eerste onderzoek) | A | trekker met oplegger, veel wielen |
| 34 | een Argentinosaurus | 65 t | 1,30 | 60–96 t (schattingen) | Mazzetta 2004 / Benson 2014 via [SCMP](https://www.scmp.com/news/world/article/1506698/argentinosaurus-tops-list-scientists-calculate-weight-426-dinosaurs); [Annals of Carnegie Museum 2017](https://bioone.org/journals/annals-of-carnegie-museum/volume-85/issue-4/007.085.0403/Determining-the-Largest-Known-Land-Animal—A-Critical-Comparison/10.2992/007.085.0403.short) | B | heel lange nek én staart, zuilpoten (langnek-dino) |
| 35 | een spaceshuttle | 78 t | 1,20 | leeg 75–78 t | [ESA Shuttle technical facts](https://www.esa.int/Science_Exploration/Human_and_Robotic_Exploration/Space_Shuttle/Shuttle_technical_facts) | B | deltavleugel met zwarte buik en neus; laadruimdeuren |
| 36 | een elektrische locomotief | 86 t | 1,10 | 83–86 t | [Spoorwegmuseum, NS 1768](https://www.spoorwegmuseum.nl/?p=30132) | A | stompe neus met twee ruiten, pantograaf (stroomafnemer) op het dak |
| 37 | een blauwe vinvis | 100 t | 1,16 | N-Atl./N-Pac. > 45 t; Antarctisch > 150 t | [NOAA Fisheries](https://www.fisheries.noaa.gov/species/blue-whale/resources) | A/B | extreem lang slank lijf met piepklein rugvinnetje; spuitwolk |
| 38 | een zeppelin (Hindenburg) | 130 t | 1,30 | 193 t incl. 59 t diesel; 215 t bruto | 1937-krant via [Papers Past](https://paperspast.natlib.govt.nz/newspapers/NZH19370508.2.73); [airships.net](https://airships.net/hindenburg/size-speed/); 130 t = eigen aftreksom | C | sigaarvormige ballon met vinnen; gondel eronder |
| 39 | een jumbojet (Boeing 747) | 175 t | 1,35 | 747-400 leeg ~179 t | [Boeing 747-400 Airport Planning](https://www.boeing.com/content/dam/boeing/boeingdotcom/commercial/airports/acaps/747-400_REV_E.pdf) (cijfer via derden) | A/B | bult bovenop de voorkant van de romp; vier motoren |
| 40 | het Vrijheidsbeeld | 204 t | 1,17 | 204 t (225 short tons) | [NPS Statue Statistics](https://www.nps.gov/stli/historyculture/statue-statistics.htm) | A | fakkel omhoog, stralenkroon, tablet in de arm |
| 41 | een Airbus A380 | 275 t | 1,35 | leeg 276–285 t | [aerospaceweb](https://aerospaceweb.org/aircraft/jetliner/a380); Airbus-document niet gevonden | B | twee volledige rijen ramen boven elkaar over de hele romp; bolle neus |
| 42 | een hogesnelheidstrein | 380 t | 1,38 | leeg 383 t (Thalys) – 435 t (ICE 3) | [eurobahn Thalys](https://www.eurobahn.de/en/eurobahn/our-trains/thalys/); [DB ICE 3](https://int.bahn.de/en/trains/long-distance-trains/ice_3) | B | lange spitse 'kogel'-neus, gestroomlijnde wagons |
| 43 | het ruimtestation ISS | 420 t | 1,11 | 419.725 kg zonder bezoekende schepen | [NASA Space Station Facts and Figures](https://www.nasa.gov/international-space-station/space-station-facts-and-figures/) | A | H-vorm: lange dwarsbalk met grote zonnepanelen; modules in het midden |
| 44 | een stapelwolk | 500 t | 1,19 | schatting voor 1 km³ wolk | [USGS Water Science School](https://www.usgs.gov/water-science-school/science/how-much-does-a-cloud-weigh) | A (schatting) | bloemkoolvormige wolk met platte onderkant |
| 45 | al het goud van De Nederlandsche Bank | 600 t | 1,20 | 612,4 t | [DNB persbericht 2026](https://www.dnb.nl/algemeen-nieuws/persbericht-2026/dnb-verbetert-verhandelbaarheid-goudvoorraad/) | A | piramide van goudstaven (trapeziumvorm) met glans |
| 46 | een windturbine (5 MW) | 690 t | 1,15 | rotor 110 + gondel 240 + toren 347 t | NREL TP-500-38060 [rapport](https://www.nrel.gov/docs/fy09osti/38060.pdf), cijfers via [NREL-forum](https://forums.nrel.gov/t/5mw-oc3-hywind-weight/344) | A/B | drie slanke wieken aan een gondel op een hoge taps toelopende mast |
| 47 | een Ariane 5-raket | 750 t | 1,09 | 746–780 t bij lancering | [ESA Ariane 5 ECA](https://www.esa.int/Enabling_Support/Space_Transportation/Ariane/Ariane_5_ECA) | A | witte middentrap met twee dikke boosters ernaast (drie 'pijpen') |
| 48 | een klapdeel van de Tower Bridge | 1.000 t | 1,33 | > 1.000–1.200 t per klapdeel | [Structurae (FR)](https://structurae.net/fr/ouvrages/tower-bridge); [Golden Tours](https://www.goldentours.com/travelblog/tower-bridge-the-largest-opening-bridge-in-the-world) | B | schuin opengeklapt wegdek tussen twee gotische torens |
| 49 | een reuzensequoia (General Sherman) | 1.250 t | 1,25 | stam ~1.120–1.260 t; hele boom nat ~1.900 t | [NPS](https://www.nps.gov/SEKI/LEARN/NATURE/SHERMAN.HTM) (volume); gewicht via [Wikipedia](https://en.wikipedia.org/wiki/General_Sherman_Tree) | B/C | kolossale roodbruine stam met kleine kroon; mensje ernaast |
| 50 | een containerkraan | 1.600 t | 1,28 | super-post-Panamax 1.600–2.000 t | [Wikipedia Container crane](https://en.wikipedia.org/wiki/Container_crane); [KHL](https://www.khl.com/news/Size-matters-developments-in-port-lifting/1128269.article) (1.650 t, Rotterdam) | B | A-vormig portaal op poten met uitstekende giek over het water; hangende container |
| 51 | het reuzenrad London Eye | 2.100 t | 1,31 | 2.100 t (rad + gondels) | [London Eye persmap](https://www.londoneye.com/media/q2nlmboz/le_presspack_aw_03.pdf) | A | rad met eivormige glazen gondels aan de buitenkant; schuine steunpoot |
| 52 | een vol olympisch zwembad | 2.500 t | 1,19 | 50 × 25 × 2 m = 2.500 m³ | rekensom (minimale wedstrijddiepte 2 m; reglement niet geopend) | A (rekensom) | zwembad met baanlijnen en startblokken |
| 53 | een Saturn V-maanraket | 2.800 t | 1,12 | 2.800–2.970 t volgetankt | [NASA](https://solarsystem.nasa.gov/news/382/10-things-rockets-we-love-saturn-v) | A | slanke toren met zwart-wit blokpatroon en vier vinnen onderaan |
| 54 | een tunnelboormachine | 3.500 t | 1,25 | ruim 3.000 – 3.520 t (Groene Hart) | [Tunnelbuilder](https://tunnelbuilder.com/News/NFM-Technologies-TBM-Breaks-Through-Green-Heart-Tunnel-in-the-Netherlands.aspx); [RWS-symposium](https://open.rijkswaterstaat.nl/publish/pages/125614/symposium_levensduur_en_onderhoud_van_de_groene_harttunnel.pdf) | A/B | rond snijwiel met beitels en spaken, vooraan een lange buis |
| 55 | de koepel van het Pantheon | 4.500 t | 1,29 | 4.535–5.000 t (schatting) | [Monolithic Dome Institute](https://monolithicdome.com/pantheon-a-temple-to-all-gods); [archeoroma](https://www.archeoroma.org/?p=148) | C | halve bol met rond gat (oculus) boven een zuilenportiek |
| 56 | het hefdeel van de Botlekbrug | 5.000 t | 1,11 | ca. 5.000 t per beweegbaar deel | [Bruggenstichting](https://www.bruggenstichting.nl/images/bruggen2014/botlekbrug.pdf) | B | wegdek tussen twee hoge heftorens, opgehesen |
| 57 | de Erasmusbrug | 6.800 t | 1,36 | 6.800 t (één databank) | [Structurae](https://structurae.net/de/bauwerke/erasmus-bruecke) | B | geknikte witte pyloon ('de Zwaan') met tuikabels |
| 58 | de nieuwe boog van de Van Brienenoordbrug | 8.500 t | 1,25 | ca. 8.500–8.600 t | [Rijkswaterstaat 2026](https://www.rijkswaterstaat.nl/nieuws/archief/2026/10/rijkswaterstaat-en-samen-tekenen-contract-voor-transport-en-installatie-nieuwe-boogbruggen-van-brienenoordbrug); [RWS 2025](https://www.rijkswaterstaat.nl/nieuws/archief/2025/11/start-aanbesteding-voor-transport-en-installatie-bogen-van-brienenoordbrug) | A | stalen boog over het wegdek met hangstangen |
| 59 | de Eiffeltoren | 10.100 t | 1,19 | 10.100 t totaal (7.300 t ijzer) | [Eiffeltoren persmap 2024](https://sete.toureiffel.paris/sites/default/files/2024-07/The%20Eiffel%20Tower%20Press%20Kit%20-%20July%202024.pdf) | A | vakwerktoren met vier gebogen poten en boog onderaan |
## Controle van de stappen

- **Aantal:** 59 objecten, 100 kg → 10.100 t. Gemiddelde stap ×1,22.
- **Grootste stap: ×1,38** (A380 275 t → hogesnelheidstrein 380 t). Daarna ×1,37 (bultrug → Boeing 737),
  ×1,36 (Stonehenge → bultrug; Botlekbrug → Erasmusbrug) en ×1,35 (A380 na Vrijheidsbeeld). **Geen stap boven
  ×1,4.**
- **Kleinste stap onder 1 ton: ×1,11** (zebra → dolfijn). Alle stappen onder 1 ton liggen tussen ×1,11 en
  ×1,33. **Geen stap onder ×1,1 in het lage deel.**
- **Kleinste stap overall: ×1,09** (windturbine 690 t → Ariane 5 750 t). Verder ×1,10 (elektrische
  locomotief, ISS, T. rex). Dat zijn alleen nog kleine stappen bovenin, waar de regel "≥ ×1,1" niet gold;
  wie toch strak wil, kan de Ariane 5 schrappen (stap wordt dan ×1,45: dus dan een vervanger nodig).
- **Verdeling over de badges:** per training (1,5–25 t) valt in regels 16–30 (15 objecten); per week (5–80 t)
  in regels 22–35 (14 objecten); "sinds je begon" (250–10.000 t) in regels 41–59 (19 objecten). Onder 1 ton
  (regels 1–13) alleen voor heel korte trainingen of losse oefeningen.
- **Betrouwbaarheid:** 38 objecten klasse A of A/B, 16 klasse B of B/C, **5 klasse C**: dolfijn, Fries paard,
  moai, zeppelin, Pantheon-koepel. Die vijf eerst verifiëren of vervangen door een reserve (zie onder).

## Wat er veranderd is tegenover de eerste ladder

- **Hergebruikt** (betrouwbaar en passend): buffetpiano, concertvleugel, ijsbeer, Fries paard, koe, giraf,
  nijlpaard, neushoorn, bestelbus, orka, olifant, T. rex, bultrug, Boeing 737, vrachtwagen, blauwe vinvis,
  Vrijheidsbeeld, Eiffeltoren, sumoworstelaar, Shetlandpony, kuub water.
- **Eruit** wegens dubbel uiterlijk of te dicht op een ander: vleugel (lijkt op concertvleugel), Aziatische
  olifant (lijkt op Afrikaanse), 20- en 40-voets container (twee keer hetzelfde blok; en tegenover de
  vrachtwagen), LZV (lijkt op vrachtwagen), kleine auto/gemiddelde auto/nieuwe auto/SUV (vier auto's), scooter
  (klasse C).
- **Eruit** als klasse C zonder verificatie: stadsbus, brandweerauto, vuilniswagen, tram.
- **Nieuw** vooral boven 10 t: Hubble, moai, maanlander, walvishaai, Stonehenge, Argentinosaurus, spaceshuttle,
  locomotief, zeppelin, Boeing 747, A380, hogesnelheidstrein, ISS, wolk, DNB-goud, windturbine, Ariane 5, Tower
  Bridge, sequoia, containerkraan, London Eye, zwembad, Saturn V, tunnelboormachine, Pantheon, Botlekbrug,
  Erasmusbrug, Van Brienenoordbrug. Onderin nieuw: reuzenpanda, struisvogel, leeuw, zebra, dolfijn, bizon,
  zeeolifant, traumahelikopter, monstertruck, Dom-klok.

## Lijkt iets op elkaar? (aandacht bij het tekenen)

- **Drie vliegtuigen:** 737 (klein, twee motoren, winglets), 747 (bult), A380 (twee volle rijen ramen). Als
  icoon blijven ze verwarrend. Advies: schrap de 737 en neem op 41 t een reserve, of houd hem en maak de
  verschillen heel duidelijk. (De spaceshuttle is door de deltavleugel en zwarte buik wel anders.)
- **Twee raketten:** Saturn V (één slanke toren, zwart-wit) en Ariane 5 (drie "pijpen" naast elkaar). Andere
  silhouetten, maar wel allebei "raket".
- **Vier bruggen bovenin:** Tower Bridge (gotische torens, klapdeel), Botlekbrug (hefbrug, twee kale
  torens), Erasmusbrug (witte zwaan met kabels), Van Brienenoordbrug (boog). Elk eigen silhouet, maar de
  diversiteit lijdt eronder. Er is tussen 4,5 en 6,8 kt weinig anders te vinden met een bron (zie open
  punten).
- **Vijf zeedieren:** dolfijn, orka, walvishaai, bultrug, blauwe vinvis. Kenmerken (snavel / wit-zwart / stippen
  en brede bek / lange witte vinnen / extreem lang en slank) moeten duidelijk getekend worden.
- **Twee treinen:** elektrische locomotief (stompe neus, stroomafnemer) en hogesnelheidstrein (spitse neus).

## Merken en namen

- Algemene namen waar het kan: "een traumahelikopter" (niet het type), "een monstertruck" (de bron is Monster
  Jam, de naam niet), "een hogesnelheidstrein" (niet Thalys/ICE), "een elektrische locomotief" (de bron is een
  NS-loc), "een windturbine", "een containerkraan", "een tunnelboormachine" (bron: de boor van de Groene
  Harttunnel).
- Vliegtuigen en raketten dragen hun typenaam (Boeing 737/747, Airbus A380, Ariane 5, Saturn V). Dat zijn
  merk + type; "een jumbojet" en "een dubbeldekker-vliegtuig" zijn veiliger als naam in de app. Geen juridisch
  advies.
- Unieke bouwwerken en dingen met een eigennaam (Eiffeltoren, Vrijheidsbeeld, London Eye, Erasmusbrug,
  Hubble, ISS, Dom-klok, Stonehenge, Pantheon) zijn geen product en mogen volgens de opdracht.
- Geen wapens: de Tsaar-kanon, tanks en oorlogsschepen zijn bewust niet gebruikt.

## Reserves (gevonden, niet in de ladder)

| Object | Gewicht | Bron | Kl. | Waarom niet / waar bruikbaar |
|---|---|---|---|---|
| een walrus | ~1.200 kg (♂ gem. > 2.700 lb) | USFWS via [NSIDC](https://eloka-arctic.org/communities/elders/maps/pacific-walrus-habitat.html); [SeaWorld](https://seaworld.org/animals/all-about/walrus/characteristics/) | B | zelfde plek als giraf |
| een Stegosaurus | 1.560 kg ("Sophie") | [Imperial College / NHM](https://www.imperial.ac.uk/news/164246/scientists-reveal-body-weight-worlds-most/) | A | te dicht op nijlpaard |
| een gemiddelde auto | 1.254 kg | [CBS](https://www.cbs.nl/nl-nl/nieuws/2025/18/personenauto-s-steeds-langer-breder-en-zwaarder) | A | te dicht op giraf |
| de klok Big Ben | 13,7 t | [UK Parliament](https://webtest.parliament.uk/about/living-heritage/building/palace/big-ben/facts-figures/great-bell) | A | tweede klok naast de Dom-klok |
| het anker van de Titanic | ~15–16 t | [Encyclopedia Titanica](https://encyclopedia-titanica.org/titanic/anchors); [Sheffield Forgemasters](https://www.sheffieldforgemasters.com/news-and-insights/news/08/forgemasters-recreates-history-with-titanic-anchor-project/) | B | botst met maanlander; leuk alternatief |
| de grootste hunebedsteen (D27) | ~20 t | [Hunebedcentrum via museum.nl](https://www.museum.nl/en/hunebedcentrum/showpiece/hunebed-d27) | A | Nederlands alternatief voor Stonehenge |
| een Brachiosaurus (Giraffatitan) | ~23 t | Sellers 2012, [Biology Letters](https://dx.doi.org/10.1098/rsbl.2012.0263) | B | tweede langnek naast Argentinosaurus |
| een gietijzeren vuurtoren (Lange Jaap) | 506 t | [HMdb](https://www.hmdb.org/m.asp?m=304603) | C | botst met wolk; NL-alternatief |
| het Christusbeeld (Rio) | 635 t | o.a. [Google Arts & Culture](https://artsandculture.google.com/entity/m03gytw) | B | botst met DNB-goud/windturbine |
| VOC-schip Batavia (reconstructie) | 646–650 t | [Hart van Nederland](https://www.hartvannederland.nl/entertainment/evenementen/artikelen/bataviaschip-keert-terug-naar-lelystad); [Scientias](https://scientias.nl/de-indrukwekkende-batavia-aan-land-zo-ging-dit-huzarenstukje-in-zijn-werk/) | B | idem; heel NL, goede ruil voor DNB-goud |
| de Tsaarklok (Kremlin) | ~202 t | [Guinness](https://www.guinnessworldrecords.com/world-records/heaviest-(largest)-bell) | B | botst met Vrijheidsbeeld |
| het Atomium | 2.400 t (2.500 na 2006) | [atomium.be](https://atomium.be/the_atomium_in_figures) | A | ruil voor zwembad of London Eye |
| de spaceshuttle op het lanceerplatform | ~2.000 t | [NASA](https://www.nasa.gov/reference/the-space-shuttle) | A | te dicht op London Eye |
| een sluisdeur van de zeesluis IJmuiden | 3.000 t | [NH Nieuws](https://www.nhnieuws.nl/nieuws/283067/de-grootste-zeesluis-ter-wereld-heeft-nu-ook-deuren-een-enorme-klus) | B | slecht herkenbaar als icoon |
| de Space Needle (zonder fundering) | ~3.350 t (3.700 short tons; bronnen spreken elkaar tegen) | [Wikipedia](https://www.wikipedia.org/wiki/Space_Needle) | C | botst met tunnelboormachine |
| de Gateway Arch (alleen staal) | 4.644 t | [NPS fact sheet](https://www.nps.gov/jeff/planyourvisit/gateway-arch-fact-sheet.htm) | A | betrouwbaarder dan Pantheon, maar ook een boog (zoals Van Brienenoord) |
| de Tower Bridge (staal) | > 11.000 t | [ICE](https://ice.org.uk/what-is-civil-engineering/infrastructure-projects/tower-bridge) | B | boven 10.000 t; botst met klapdeel |
| de Toren van Pisa | ~14.500 t | [Ingenia](https://ingenia.org.uk/articles/the-stabilisation-of-the-leaning-tower-of-pisa/) | B | eventueel als bonus boven de Eiffeltoren (stap ×1,44) |

Niet gevonden of te onzeker: gewicht van een Hollandse molen, Euromast (alleen de fundering: 2.400 t),
Elizabeth Tower (het vaak genoemde 8.667 t heeft geen bron), Maeslantkering-arm (15.000 t alleen in een blog;
Rijkswaterstaat noemt 680 t voor het bolscharnier), de Sfinx (10.000–20.000 t, onbetrouwbaar).

## Open punten

- Klasse-C-objecten verifiëren of ruilen: dolfijn (NOAA-pagina zelf lezen), Fries paard (KFPS), moai (gebruikt
  een bekend maar door het EISP betwist gemiddelde), zeppelin (130 t is mijn eigen aftreksom), Pantheon-koepel
  (schatting; alternatief Gateway Arch).
- Het **olympisch zwembad** rekent met 2 m diepte (minimum voor wedstrijden). Het reglement van World Aquatics
  zelf heb ik niet geopend; als het bad 3 m diep is, is het 3.750 t.
- De **walvishaai** (18 t) zit dicht bij het maximum van de bron; "een grote walvishaai" is eerlijker.
- De **traumahelikopter** gebruikt het maximale startgewicht (2,9–3,1 t); leeg is hij ~1,5 t. Noem hem in de
  app "een volgetankte traumahelikopter" of kies een lager getal.
- Bij de **bestelbus** ontbreekt nog een URL voor de rijbewijs-B-grens.
- Beslissen: 737 schrappen (drie vliegtuigen)? Eén brug minder bovenin (dan is een ander object rond 5–7 kt
  nodig)?
