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
| R9 | Die Paar-Sicht zeigt NUR gemeinsame Einträge (unabhängig vom Zahler), bei Monatsübersicht und Verlauf. Saldi und persönliche Einträge (inkl. Einnahmen) sind PRIVAT — sie bleiben in der Paar-Ansicht ohne Filter. Feldform identisch; `budgets.ist` wird aus den gemeinsamen Ausgaben berechnet. `person` und `gemeinsam` zusammen: schlicht 400 *(Entscheidung 11.11.2026: „Saldi und Einnahmen sind privat — geteilt werden nur gemeinsame Kategorien")* |
| R10 | Ein Eintrag kann optional in Teile aufgeteilt werden (z. B. Einkauf 30 € = 20 € Essen + 10 € Geschenk). Jeder Teil hat Betrag, Kategorie und eine Zuordnung (Person ODER gemeinsam). Die Teile-Summe muss cent-genau dem Eintrags-Betrag entsprechen; alle Teile zeigen in dieselbe Richtung (Ausgabe/Einnahme). Der Server leitet `kategorie`, `person` und `gemeinsam` des Eintrags aus dem größten Teil ab — clientseitige Angabe dieser Felder wird bei vorhandenen Teilen ignoriert. Teile lassen sich rückwirkend ändern und entfernen (R5 entsprechend; Teile werden beim Speichern komplett ersetzt). Alle kategoriebasierten Rechnungen (Monatsübersicht, Budgets `ist`, Statistik, Prognose, Sparpotenzial, Verlauf) rechnen mit den Teilen; ohne Teile gilt der Eintrag selbst wie bisher. In der Übersicht bleibt ein geteilter Eintrag EINE Zeile; in Person-Sicht/Paar-Sicht zeigt die Zeile den sichtbaren Teilbetrag (ungefiltert: Gesamtbetrag). Der Ausgleich führt jeden gemeinsamen Teil als eigene Position. Der Zahler ist erforderlich, sobald ein Teil gemeinsam ist. *(Entscheidung 12.11.2026: Rest-Teil automatisch, eine Zeile — Teile im Detail; rückwirkend bearbeitbar)* |

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
| R9 | Kim: Freizeit -42,50 € (15.11.2026); Alex: Gehalt +2.000,00 € (30.11.2026); gemeinsam: Essen -60,00 € (15.11.2026, Zahler Alex) | die Monatsübersicht November 2026 in der Paar-Sicht (gemeinsam=true) aufgerufen wird | zeigt NUR das gemeinsame Essen: Ausgaben Essen 60,00 €, keine Einnahmen, 1 Eintrag. Ohne Filter unverändert alles (deckt das bestehende R7-Szenario „Paar-Sicht bleibt unverändert" ab) |
| R9 | aktueller Monat November 2026; September 2026: Gehalt +2.000,00 € (Alex), Freizeit -42,50 € (Kim); Oktober 2026: keine Einträge; November 2026: gemeinsames Essen -60,00 € (Zahler Alex) | der Monatsverlauf der letzten 3 Monate in der Paar-Sicht (gemeinsam=true) abgefragt wird | enthält ältester zuerst: 2026-09 (0,00 / 0,00 / 0,00 — Gehalt und Freizeit sind privat), 2026-10 (0,00 / 0,00 / 0,00), 2026-11 (0,00 / 60,00 / -60,00 — nur das gemeinsame Essen) |
| R9 | — | die Monatsübersicht November 2026 für Person „Alex" UND in der Paar-Sicht aufgerufen wird | wird die Abfrage abgelehnt (400) |

| R10 | noch kein Eintrag erfasst | "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und den Teilen „Essen gemeinsam -20,00" und „Geschenk von Alex -10,00" erfasst | zeigt die Monatsübersicht November 2026 bei „Essen" 20,00 € Ausgaben und bei „Geschenk" 10,00 € Ausgaben, und listet genau EINEN Eintrag mit dem Betrag -30,00 € |
| R10 | ein Eintrag -30,00 € soll mit Teilen der Summe -29,00 € erfasst werden | wird er abgelehnt (400) |
| R10 | ein Teil mit Person UND „gemeinsam" erfasst werden soll | wird er abgelehnt (400) |
| R10 | ein gemeinsamer Teil ohne Zahler erfasst werden soll | wird er abgelehnt (400) |
| R10 | ein geteilter Eintrag wie oben erfasst; Budget „Essen" 25,00 € im November 2026 | die Monatsübersicht November 2026 aufgerufen wird | ist das Ist-Budget für „Essen" 20,00 € |
| R10 | ein geteilter Eintrag wie oben erfasst | der Ausgleich November 2026 aufgerufen wird | führt er nur den gemeinsamen Teil: Kim schuldet Alex 10,00 €, eine Position „Essen" -20,00 € |
| R10 | ein geteilter Eintrag wie oben erfasst | die Monatsübersicht November 2026 für Person „Alex" aufgerufen wird | zeigt sie den Eintrag mit dem sichtbaren Teilbetrag -10,00 €, Ausgaben nur „Geschenk" 10,00 € |
| R10 | ein geteilter Eintrag wie oben erfasst | die Paar-Sicht November 2026 aufgerufen wird | zeigt sie den Eintrag mit dem sichtbaren Teilbetrag -20,00 €, Ausgaben nur „Essen" 20,00 € |
| R10 | ein geteilter Eintrag wie oben erfasst | "Alex" die Teile rückwirkend ändert (Geschenk -5,00 €, neuer Teil „Freizeit von Alex -5,00 €") | zeigt die Übersicht „Geschenk" 5,00 € und „Freizeit" 5,00 €, „Essen" unverändert 20,00 € |
| R10 | ein Eintrag -40,00 € mit den Teilen „Essen gemeinsam -20,00", „Freizeit gemeinsam -15,00", „Geschenk von Alex -5,00" erfasst | der Ausgleich November 2026 aufgerufen wird | führt er 2 Positionen aus und Kim schuldet Alex 17,50 € |
| R10 | ein geteilter Eintrag wie oben erfasst; aktueller Monat November 2026 | der Kategorie-Verlauf der letzten 1 Monate für „Essen" abgefragt wird | enthält er 2026-11 mit 20,00 € |

Hinweis: Die R10-Beispielwerte (12.11.2026, -30,00 €, 20/10-Aufteilung, Budget 25,00 €, -40,00 € mit drei Teilen) sind Vorschläge (12.11.2026) und warten auf Freigabe. Der Zielbetrag -50,00 € im R5-Beispiel ist ein Vorschlag (Aufgabenstellung 11.11.2026); alle übrigen Werte stammen aus bestehenden Beispielen. Die R7/R8-Beispiele nutzen bestehende Werte (-42,50 €, +2.000,00 €, -60,00 €); die Kombination „-42,50 € Freizeit von Kim" ist ein Vorschlag (Aufgabenstellung 11.11.2026), da -42,50 € in den Specs bislang nur als REWE-Essen-Import vorkommt. Der R7-Regeltext (gemeinsame Einträge fließen über den Ausgleich, nicht in persönliche Statistiken) ist als Vorschlag markiert und braucht eure Freigabe.

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
| F9  | Einnahmen-Aufteilung: erlaubt und technisch möglich, aber noch ohne Szenario — explizit testen?                                            | 12.11.2026: offen (Implementation erlaubt es, UI testet nur Ausgaben)                                                                       |
| F10 | Teile auf importierten Einträgen (quelleId): wie verhält sich die Import-Inbox damit?                                                        | 12.11.2026: offen (bis zur Inbox-Story, QF5)                                                                                               |
| F11 | GET /api/eintraege?person filtert nach der abgeleiteten Eintrags-Person (größter Teil) — Teil-Zuordnung wird dort ignoriert. Semantik festlegen? | 12.11.2026: offen (Monatsübersicht ist der maßgebliche Endpunkt)                                                                          |
| F12 | Partner darf beim PUT eines gemeinsamen Eintrags fremde private Teile nicht entfernen (403) — Regel so gewollt?                              | 12.11.2026: umgesetzt als Schutz, wartet auf Freigabe                                                                                      |


## Daraus entstanden

- `features/eintraege/erfasster-eintrag-wird-aufbewahrt.feature`
- `features/eintraege/eintrag-ohne-kategorie-wird-nicht-uebernommen.feature`
- `features/eintraege/monatsuebersicht-zeigt-alles.feature`
- `features/eintraege/monatsuebersicht-person.feature` (R7)
- `features/eintraege/monatsverlauf.feature` (R8)
- `features/eintraege/monatsuebersicht-paar.feature` (R9)
- `features/eintraege/einnahme-zaehlt-nicht-zu-ausgaben.feature`
- `features/eintraege/eintrag-ohne-datum-oder-betrag-wird-nicht-uebernommen.feature`
- `features/eintraege/eintrag-wird-geaendert.feature` (R5)
- `features/eintraege/eintrag-wird-geloescht.feature` (R6)
- `features/eintraege/eintrag-aufteilung.feature` (R10)
- Szenarien tragen den Tag ihrer Regel, z. B. `@R1`
