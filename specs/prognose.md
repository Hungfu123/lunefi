# Thema: Monatsprognose

Quelle: Aufgabenstellung 11.11.2026. **Alle PR-Regeln und die Beispielwerte sind Vorschläge vom 11.11.2026** (Werte frei wählbar, aber als Vorschlag gekennzeichnet) und brauchen Freigabe.

## Story

Als ich
möchte ich eine Prognose der Ausgaben und Einnahmen für den Folgemonat sehen
damit ich heute weiß, was der nächste Monat vermutlich übrig lässt

## Regeln

| ID  | Regel |
|-----|-------|
| PR1 | **Fixe Kosten zählen immer:** Kategorien mit festem Charakter (Konstante im Backend: Miete, Strom, Internet, Telefon, Versicherung, Abo) werden mit ihrem letzten bekannten Monatswert in die Prognose übernommen — auch wenn der Zielmonat sie naturgemäß noch nicht hat. „Letzter bekannter Monatswert" = der Wert aus dem jüngsten Basismonat, in dem die Kategorie Ausgaben hat. Hat eine fixe Kategorie in keinem Basismonat Ausgaben, taucht sie nicht in der Prognose auf. Fixe Kategorien gelten nur für Ausgaben; Einnahmen laufen immer über PR3 |
| PR2 | **Variable Ausgaben = Median mit Ausreißer-Schutz:** Alle übrigen Ausgaben-Kategorien: Prognose = Median der Monatessummen der letzten N Monate. N default 3, Parameter `monate` 2–24, Werte über 24 werden auf 24 gecappt (wie Verlauf R8). Monate ohne Eintrag in der Kategorie zählen als 0,00 € — ein Riesen-Einmalkauf (z. B. Urlaub 2.000 € in einem Monat) verfälscht die Prognose so nicht. Median bei gerader Anzahl: Mittel der beiden mittleren Werte. Median statt Mittel: ein Ausreißermonat verschiebt den Median nicht, den Mittelwert schon |
| PR3 | **Nur regelmäßige Einnahmen:** Eine Einnahme-Kategorie zählt nur, wenn sie in mindestens 2 der letzten N Monate vorkommt UND ihre Werte ähnlich sind: Spannweite (größter − kleinster Wert, nur über die Monate mit Vorkommen) ≤ 50 % des größeren Werts — einfach und nachvollziehbar. Prognosewert = Median der vorkommenden Monatswerte. Sonst fließt die Einnahme NICHT in die Prognose (Erstattungen, Einmaliges) |
| PR4 | **Wenig Daten = ehrlich:** Wenn insgesamt weniger als 2 Monate mit Einträgen existieren (in der gewählten Sicht): `konfidenz: "WENIG_DATEN"`, alle Prognosewerte 0 — die Kategorien-Maps sind leer, Differenz 0,00 €. Keine Schätzung aus einem Monat. Sonst `konfidenz: "OK"` |
| PR5 | **Sicht wie R7/R9:** `person=X` zählt nur persönliche Einträge von X, `gemeinsam=true` nur gemeinsame; beide zusammen → 400. `monate` unter 2 → 400 (Prognose aus einem einzigen Monat ist keine Prognose) |

## Zeitbezug (Vorschlag 11.11.2026, deckungsgleich mit dem Antwort-Beispiel der Aufgabenstellung)

- `monat` = der Folgemonat des AKTUELLEN Monats (Uhr).
- `basisMonate` = die N vollen Monate **vor** dem aktuellen Monat (der aktuelle Monat kann unvollständig sein), ältester zuerst. Aktueller Monat November 2026, N=3 → Basis 2026-08, 2026-09, 2026-10; Zielmonat 2026-12.
- `differenz` = Summe Einnahmen − Summe Ausgaben.
- Einträge im aktuellen Monat und in der Zukunft fließen nicht in die Basis ein.

## Beispiele

Konkrete Werte — **alle Zahlen sind Vorschläge vom 11.11.2026**.

| Regel | Gegeben | Wenn | Dann |
|-------|---------|------|------|
| PR1 | aktueller Monat November 2026; Miete September −900,00 €, Oktober −950,00 € (Alex) | die Monatsprognose abgefragt wird | zählt Miete mit dem letzten bekannten Wert 950,00 € (nicht dem Median 900,00 € — die Werte sind so gewählt, dass Median und letzter bekannter Wert auseinanderliegen), Differenz −950,00 €, Konfidenz OK |
| PR2 | aktueller Monat November 2026; Freizeit August −150,00 €, September −150,00 €, Oktober −2.000,00 € (Urlaubs-Einmalkauf, Alex) | die Monatsprognose abgefragt wird | bleibt Freizeit bei 150,00 € — der Median widersteht dem Ausreißer; der Mittelwert läge bei 766,67 € und wäre falsch |
| PR3 | aktueller Monat November 2026; Gehalt September +2.000,00 €, Oktober +1.900,00 € (Alex); Erstattung September +40,00 € (einmalig) | die Monatsprognose abgefragt wird | zählt Gehalt (2 von 3 Basismonaten, Spannweite 100 € ≤ 1.000 €) mit Median 1.950,00 €; Erstattung (nur 1 Monat) fließt NICHT ein |
| PR4 | aktueller Monat November 2026; nur ein Monat mit Einträgen (September: Essen −20,00 €) | die Monatsprognose abgefragt wird | Konfidenz WENIG_DATEN, keine prognostizierten Ausgaben, Differenz 0,00 € |
| PR5 | — | die Monatsprognose für Person „Alex" UND in der Paar-Sicht abgefragt wird | wird die Abfrage abgelehnt (400) |
| PR5/R7 | aktueller Monat November 2026; Kim: Freizeit September −100,00 €, Oktober −200,00 €; Alex: Gehalt September +2.000,00 €, Oktober +1.900,00 € | die Monatsprognose für Person „Kim" abgefragt wird | prognostiziert Freizeit 100,00 € (Median aus 0/100/200 — August ohne Eintrag zählt als 0) und KEINE Einnahmen (Gehalt ist privat von Alex) |

## Offene Fragen

| ID  | Frage                                                                                          | Entschieden am |
|-----|------------------------------------------------------------------------------------------------|----------------|
| PF1 | Zählen Basismonate ohne Kategorie-Eintrag als 0 im Median (PR2)?                                | Vorschlag 11.11.2026: ja — nur so verschwinden Einmalkäufe aus der Prognose |
| PF2 | „Letzter bekannter Monatswert" (PR1) auch aus Monaten vor dem Basisfenster?                     | Vorschlag 11.11.2026: nein — Basisfenster = die N Basismonate |
| PF3 | PR4 zählt Monate mit Einträgen gesamt oder je Sicht?                                            | Vorschlag 11.11.2026: je Sicht (gefilterte Einträge) — ehrlich pro Sicht |
| PF4 | Sollte eine fixe Kategorie mit Einnahmen anders behandelt werden?                               | Vorschlag 11.11.2026: nein — Einnahmen laufen immer über PR3 |

## Daraus entstanden

- `features/eintraege/monatsprognose.feature` (@PR1–@PR5; Sicht-Szenario zusätzlich @R7/@R9)
- Szenarien tragen den Tag ihrer Regel
- Endpoint `GET /api/prognose` — dokumentiert in `frontend/API.md`
