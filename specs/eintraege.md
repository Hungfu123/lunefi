# Thema: Einträge erfassen

Quelle ist das [[example-mapping|Canvas]] — hier steht, was dort auf den Karten steht.

## Story

Als ich
möchte ich Ausgaben und Einnahmen mit Datum, Betrag und Kategorie erfassen und eine Monatsübersicht je Kategorie sehen
damit ich mein Budget im Blick habe

## Regeln

| ID | Regel |
|----|-------|
| R1 | Jeder Eintrag hat Datum, Betrag und Kategorie |
| R2 | Negative Beträge sind Ausgaben, positive Einnahmen. Währung: immer Euro |
| R3 | Die Monatsübersicht zeigt Ausgaben und Einnahmen je Kategorie als Summe und listet jeden Eintrag einzeln (Datum, Betrag) |
| R4 | Die Monatsübersicht lehnt Monate ab, die es nicht gibt |
| R5 | Einträge lassen sich ändern: Betrag, Datum, Kategorie, Person, gemeinsam, zahler und konto. Es gelten die gleichen Validierungen wie beim Anlegen. Ein unbekannter Eintrag wird mit 404 abgelehnt |
| R6 | Einträge lassen sich löschen: danach existiert der Eintrag nicht mehr. Kontostand und Salden passen sich automatisch an, da alles berechnet wird |
| R7 | Die Monatsübersicht kann nach Person gefiltert werden („Person-Sicht"): dann zählen nur persönliche Einträge dieser Person. Gemeinsame Einträge (person=null, gemeinsam=true) gehören NICHT in die Person-Sicht — sie leben in der Paar-Ansicht (ohne Filter, Verhalten unverändert) und im Ausgleich. Budget-Limits bleiben Paar-weit, `ist` wird aus den gefilterten Ausgaben berechnet. *(Vorschlag 11.11.2026: gemeinsame Einträge fließen über den Ausgleich, nicht in persönliche Statistiken)* |
| R8 | Ein Monatsverlauf zeigt je Monat Einnahmen und Ausgaben als absolute Beträge sowie die Differenz (einnahmen − ausgaben; positiv = Plus-Monat). Monate = die letzten N vollen Monate rückwärts vom AKTUELLEN Monat (Uhr), ältester zuerst. `monate` default 3, Werte über 24 werden auf 24 gecappt *(Entscheidung 11.11.2026: cappen statt 400)*. Mit `person` analog R7 gefiltert, ohne = alle Einträge |

## Beispiele

Konkrete Werte, keine Prosa. Jede Regel braucht mindestens eins.

| Regel | Gegeben | Wenn | Dann |
|-------|---------|------|------|
| R1 | noch kein Eintrag erfasst | ein Eintrag mit Datum 15.11.2026, Betrag -12,50 €, Kategorie „Essen" erfasst wird | existiert genau dieser eine Eintrag |
| R1 | noch kein Eintrag erfasst | ein Eintrag ohne Kategorie erfasst werden soll | wird er nicht übernommen |
| R1 | noch kein Eintrag erfasst | ein Eintrag ohne Datum oder ohne Betrag erfasst werden soll | wird er nicht übernommen |
| R2 | ein Eintrag -12,50 € („Essen", 15.11.2026) | die Monatsübersicht November 2026 aufgerufen wird | steht bei „Essen": 12,50 € Ausgaben |
| R2 | ein Eintrag +2.000,00 € („Gehalt", 30.11.2026) | die Monatsübersicht November 2026 aufgerufen wird | zählt er zu den Einnahmen, nicht zu den Ausgaben |
| R3 | Essen -12,50 € (15.11.), Miete -950,00 € (01.11.), Gehalt +2.000,00 € (30.11.) | die Monatsübersicht November 2026 aufgerufen wird | Ausgaben je Kategorie (Essen 12,50 €, Miete 950,00 €), Einnahmen (Gehalt 2.000,00 €) und alle drei Einträge einzeln gelistet |
| R4 | noch kein Eintrag | die Monatsübersicht Monat 13 im Jahr 2026 aufgerufen wird | wird der Monat abgelehnt (kein Serverfehler) |
| R5 | ein Eintrag -12,50 € („Essen", 15.11.2026, von „Alex") erfasst | der Betrag dieses Eintrags zu -50,00 € geändert wird | zeigt die Monatsübersicht November 2026 bei „Essen" 50,00 € Ausgaben, und der Eintrag ist mit -50,00 € gespeichert |
| R5 | kein Eintrag mit der Nummer 999 | der Eintrag mit der Nummer 999 geändert werden soll | wird das Ändern abgelehnt (404) |
| R6 | der Eintrag -12,50 € („Essen", 15.11.2026, von „Alex") auf dem Konto „Girokonto" (Startsaldo 3.450,00 €) erfasst | dieser Eintrag gelöscht wird | existiert der Eintrag nicht mehr, und die Kontenübersicht zeigt den Saldo 3.450,00 € |
| R7 | Kim: Freizeit -42,50 € (15.11.2026); Alex: Gehalt +2.000,00 € (30.11.2026); gemeinsam: Essen -60,00 € (15.11.2026, Zahler Alex) | die Monatsübersicht November 2026 für Person „Alex" aufgerufen wird | zählt nur das Gehalt (Einnahmen 2.000,00 €): Freizeitausgabe fehlt (gehört Kim), gemeinsames Essen fehlt (lebt im Ausgleich) |
| R7 | Kim: Freizeit -42,50 € (15.11.2026); Alex: Gehalt +2.000,00 € (30.11.2026); gemeinsam: Essen -60,00 € (15.11.2026, Zahler Alex) | die Monatsübersicht November 2026 ohne Person-Filter aufgerufen wird | zeigt alles: Ausgaben Freizeit 42,50 € und Essen 60,00 €, Einnahmen Gehalt 2.000,00 € — Paar-Sicht unverändert |
| R8 | aktueller Monat November 2026; September 2026: Gehalt +2.000,00 € (Alex), Freizeit -42,50 € (Kim); Oktober 2026: keine Einträge; November 2026: gemeinsames Essen -60,00 € (Zahler Alex) | der Monatsverlauf der letzten 3 Monate abgefragt wird | enthält ältester zuerst: 2026-09 (Einnahmen 2.000,00 €, Ausgaben 42,50 €, Differenz +1.957,50 €), 2026-10 (0,00 / 0,00 / 0,00), 2026-11 (0,00 / 60,00 / -60,00 € Minus-Monat) |
| R8 | aktueller Monat November 2026; September 2026: Gehalt +2.000,00 € (Alex), Freizeit -42,50 € (Kim); Oktober 2026: keine Einträge; November 2026: gemeinsames Essen -60,00 € (Zahler Alex) | der Monatsverlauf der letzten 3 Monate für Person „Alex" abgefragt wird | enthält ältester zuerst: 2026-09 (2.000,00 / 0,00 / +2.000,00 — nur Alex' Gehalt), 2026-10 (0,00 / 0,00 / 0,00), 2026-11 (0,00 / 0,00 / 0,00 — gemeinsames Essen zählt nicht, Freizeitausgabe gehört Kim) |

Hinweis: Der Zielbetrag -50,00 € im R5-Beispiel ist ein Vorschlag (Aufgabenstellung 11.11.2026); alle übrigen Werte stammen aus bestehenden Beispielen. Die R7/R8-Beispiele nutzen bestehende Werte (-42,50 €, +2.000,00 €, -60,00 €); die Kombination „-42,50 € Freizeit von Kim" ist ein Vorschlag (Aufgabenstellung 11.11.2026), da -42,50 € in den Specs bislang nur als REWE-Essen-Import vorkommt. Der R7-Regeltext (gemeinsame Einträge fließen über den Ausgleich, nicht in persönliche Statistiken) ist als Vorschlag markiert und braucht eure Freigabe.

## Offene Fragen

| ID  | Frage                                                                                                             | Entschieden am                                                                                                                             |
| --- | ----------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| F1  | Wie werden gemeinsame Ausgaben markiert und geteilt (50/50, Einzahler, Anteile)?                                  | 2026-11-10: als „gemeinsam" markiert, Anteil pro Kategorie wählbar (50/50 oder eigene Anteile, wie Splitwise) — Details in `specs/gemeinsam.md` |
| F2  | Budget-Limits je Kategorie — und was passiert bei Überschreitung?                                                 | 2026-11-10: Warnung/Benachrichtigung in der Übersicht, kein Block |
| F3  | Betrag + Kategorie für das Einnahmen-Beispiel (R2-Grenzfall)?                                                     | 2026-11-10: Gehalt +2.000,00 €, Kategorie „Gehalt"                                                                                         |
| F4  | Zwei konkrete Ausgaben für das R3-Beispiel?                                                                       | 2026-11-10: Essen -12,50 €, Miete -950,00 € + Gehalt +2.000,00 € (Übersicht zeigt Ausgaben und Einnahmen)                                  |
| F5  | Step-Werte des R1-Gegenbeispiels (-8,99 € / 16.11.2026, nur im Step-Code, unsichtbar im Szenario) bestätigen?     | 2026-11-10: behalten (unsichtbar im Gherkin, nur Verkabelung) |
| F6  | Monatsübersicht mit ungültigem Monat (z. B. Monat 13) — 400 statt Serverfehler? (Korrektheit-Review: aktuell 500) | 2026-11-10: ja — Abweisung mit 400 (neue Regel R4) |
| F7  | Eintrag mit Betrag 0,00 — Ausgabe, Einnahme oder eigene Gruppe? (aktuell: zählt als Einnahme)                      | 2026-11-10: zählt als Einnahme                                                                                           |
| F8  | G1 verlangt eine Zuordnung je Eintrag — brauchen die bestehenden Szenarien eine Person?                          | 2026-11-11 (Overnight-Freigabe): Wenn-Sätze der bestehenden Szenarien um `von "Alex"` erweitert, Step-Bindung entsprechend |

## Daraus entstanden

- `features/eintraege/erfasster-eintrag-wird-aufbewahrt.feature`
- `features/eintraege/eintrag-ohne-kategorie-wird-nicht-uebernommen.feature`
- `features/eintraege/monatsuebersicht-zeigt-alles.feature`
- `features/eintraege/monatsuebersicht-person.feature` (R7)
- `features/eintraege/monatsverlauf.feature` (R8)
- `features/eintraege/einnahme-zaehlt-nicht-zu-ausgaben.feature`
- `features/eintraege/eintrag-ohne-datum-oder-betrag-wird-nicht-uebernommen.feature`
- `features/eintraege/eintrag-wird-geaendert.feature` (R5)
- `features/eintraege/eintrag-wird-geloescht.feature` (R6)
- Szenarien tragen den Tag ihrer Regel, z. B. `@R1`
