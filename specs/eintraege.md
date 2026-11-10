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

## Offene Fragen

| ID  | Frage                                                                                                             | Entschieden am                                                                                                                             |
| --- | ----------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| F1  | Wie werden gemeinsame Ausgaben markiert und geteilt (50/50, Einzahler, Anteile)?                                  | Man markiert die Kategorien man kann selber angeben wie der anteil ist oder 50/50 oder bestimmte automatisieren und anpassen wie splitwise |
| F2  | Budget-Limits je Kategorie — und was passiert bei Überschreitung?                                                 | dann einfach benachrichtigung anzeigen oder was ist besser?                                                                                |
| F3  | Betrag + Kategorie für das Einnahmen-Beispiel (R2-Grenzfall)?                                                     | 2026-11-10: Gehalt +2.000,00 €, Kategorie „Gehalt"                                                                                         |
| F4  | Zwei konkrete Ausgaben für das R3-Beispiel?                                                                       | 2026-11-10: Essen -12,50 €, Miete -950,00 € + Gehalt +2.000,00 € (Übersicht zeigt Ausgaben und Einnahmen)                                  |
| F5  | Step-Werte des R1-Gegenbeispiels (-8,99 € / 16.11.2026, nur im Step-Code, unsichtbar im Szenario) bestätigen?     | was ist besser?                                                                                                                            |
| F6  | Monatsübersicht mit ungültigem Monat (z. B. Monat 13) — 400 statt Serverfehler? (Korrektheit-Review: aktuell 500) | gibt kein ungültiger monat. ja mach                                                                                                        |
| F7  | Eintrag mit Betrag 0,00 — Ausgabe, Einnahme oder eigene Gruppe? (aktuell: zählt als Einnahme)                     | ja einnahme                                                                                                                                |

## Daraus entstanden

- `features/eintraege/erfasster-eintrag-wird-aufbewahrt.feature`
- `features/eintraege/eintrag-ohne-kategorie-wird-nicht-uebernommen.feature`
- `features/eintraege/monatsuebersicht-zeigt-alles.feature`
- `features/eintraege/einnahme-zaehlt-nicht-zu-ausgaben.feature`
- `features/eintraege/eintrag-ohne-datum-oder-betrag-wird-nicht-uebernommen.feature`
- Szenarien tragen den Tag ihrer Regel, z. B. `@R1`
