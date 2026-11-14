# Bij lichaamsgewicht-oefeningen is het doel een effectief gewicht

Bij oefeningen met lichaamsgewicht (pull-ups, dips) is het doel niet het plus-of-min-gewicht dat je vorige keer
invulde, maar het effectieve gewicht: lichaamsgewicht maal het aandeel van de oefening, plus of min wat je
erbij hangt (ADR 0001). Je lichaamsgewicht schommelt; ben je 5 kg lichter, dan moet je er ongeveer 5 kg bij
hangen om even zwaar te trainen. De app rekent het doel daarom om naar wat je vandaag erbij hangt of aan
ondersteuning nodig hebt, met je lichaamsgewicht van die dag, afgerond op wat je materiaal toelaat; precies
hetzelfde lukt zelden, en dat is goed. Records tonen het effectieve gewicht dat je verplaatste, niet
"lichaamsgewicht".

Alternatief was het plus-of-min-gewicht als doel bewaren (eenvoudiger, zo werkte het tot 1.0); dat laat een
verandering van je lichaamsgewicht onzichtbaar meetellen.

Opslag (#87): het doel staat als effectief gewicht in dezelfde kolommen als eerst (`plan_sets`, `target_sets`),
zonder nieuwe databaseversie; alleen de betekenis voor lichaamsgewicht-oefeningen verandert. Een eenmalige stap in
de app rekent bestaande doelen om met het lichaamsgewicht van hun training, en wacht zolang er nog geen
lichaamsgewicht bekend is. Het sessiedoel per set (`goalWeightKg`) blijft het plus-of-min-gewicht van die dag,
afgerond op het materiaal: wat je erbij hangt. Een back-up zegt of zijn doelen al effectief gewicht zijn.

Besloten in de Wayfinder-kaart release 1.0, ticket "Open meldingen: opladen-hulp, standaard-doel en records op reps" (#76).
