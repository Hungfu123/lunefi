# Thema: Statistik

Quelle: Aufgabenstellung 11.11.2026 (Nutzer-Wunsch: „Statistiken wären nice … sagen was am meisten auffällt oder wo die längsten Ausgaben waren … gucken wo man was sparen kann, aber SINNVOLL — Miete geht ja nicht zum Beispiel"). **Alle ST-Regeln und Beispielwerte sind Vorschläge vom 11.11.2026** und brauchen Freigabe.

## Story

Als ich
möchte ich Statistiken über meine Ausgaben sehen — Kategorie-Verläufe für Charts und ehrliche Spar-Hinweise
damit ich erkenne, wo ich ungewöhnlich viel ausgebe — ohne dass fixe Kosten wie die Miete vorgeschlagen werden

## Regeln

| ID  | Regel                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|-----|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ST1 | **Kategorie-Verlauf:** `GET /api/statistik/kategorie/{kategorie}?monate=6[&person=X\|&gemeinsam=true]` zeigt je Monat die Summe der AUSGABEN der Kategorie (nur Beträge < 0, als positiver Wert; Einnahmen werden ignoriert), ältester Monat zuerst. Monate = die letzten N Monate rückwärts vom AKTUELLEN Monat (Uhr, wie Verlauf R8, inkl. aktuellem Monat). Dazu `median` und `durchschnitt` der Monatswerte (Monate ohne Eintrag zählen als 0,00 € — wie Prognose PR2). `monate` default 6, unter 1 → 400, über 24 → auf 24 gecappt (wie Verlauf). Sicht-Filter wie R7/R9: `person` nur persönliche Einträge dieser Person, `gemeinsam=true` nur gemeinsame; beide zusammen → 400; `person` fremd → 403 |
| ST2 | **Sparpotenzial — nur sinnvolle Hinweise:** `GET /api/statistik/sparpotenzial?monat=YYYY-MM[&person=X\|&gemeinsam=true]`. Für jede NICHT-FIXE Kategorie (Fixe-Kosten-Konstante aus `PrognoseService` wiederverwenden — Miete, Strom, Internet, Telefon, Versicherung, Abo werden NIEMALS vorgeschlagen): `ist` = Ausgaben im angefragten Monat; `ueblich` = Median der Ausgaben der letzten N=3 Monate VOR dem angefragten Monat (gleiche Sicht-Filter; Monate ohne Eintrag zählen als 0,00 €). Ein Hinweis entsteht nur wenn: `ist` > `ueblich` UND (`ist` − `ueblich`) ≥ 10,00 € UND `ueblich` > 0 — Kategorien ohne üblichen Wert (neu, ohne Historie) erzeugen KEINEN Hinweis, kein Rauschen. Text (paarweise einfache Sätze, ganze €): `"{kategorie}: {ist} € — sonst meist {ueblich} €."` Ungültiger `monat` → 400 |
| ST3 | **Basis-Historie ehrlich (wie Prognose PR4):** Wenn weniger als 2 Monate mit Einträgen VOR dem angefragten Monat existieren (in der gewählten Sicht): `{"hinweise":[],"basis":"WENIG_DATEN"}` — sonst `"basis":"OK"`. Keine Empfehlung aus einem Monat |

Alle Endpunkte: nur angemeldet (Security-Chain erledigt das).

## Beispiele

Konkrete Werte — **alle Zahlen sind Vorschläge vom 11.11.2026**.

| Regel   | Gegeben                                                                                                                                                            | Wenn                                                    | Dann                                                                                                                                                                                    |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ST1     | aktueller Monat November 2026; Kim: Freizeit −30,00 € (16.09.), Freizeit −30,00 € (16.10.); Alex: Gehalt +2.000,00 € (30.09.)                                    | der Kategorie-Verlauf der letzten 3 Monate für „Freizeit" abgefragt wird | ältester zuerst: 2026-09 (30,00 €), 2026-10 (30,00 €), 2026-11 (0,00 €); Median 30,00 €; Durchschnitt 20,00 €. Das Gehalt zählt nicht (Einnahme)                                        |
| ST1/R7  | Kim: Freizeit −42,50 € (16.09.); Alex: Freizeit −100,00 € (16.09.); gemeinsam: Freizeit −60,00 € (15.11., Zahler Alex)                                              | der Kategorie-Verlauf für „Freizeit" für Person „Kim" abgefragt wird | nur Kims 42,50 € im September; Alex' Ausgabe und das gemeinsame Essen zählen nicht                                                                                                       |
| ST1     | —                                                                                                                                                                  | der Kategorie-Verlauf für Person „Alex" von Kim abgefragt wird | wird der Zugriff abgelehnt (403)                                                                                                                                                         |
| ST1     | —                                                                                                                                                                  | der Kategorie-Verlauf mit 0 Monaten abgefragt wird      | wird die Abfrage abgelehnt (400)                                                                                                                                                         |
| ST2     | Alex: Freizeit −25,00 € je Juli/August/September 2026, dann −90,00 € im Oktober 2026                                                                              | das Sparpotenzial für Oktober 2026 abgefragt wird       | genau ein Hinweis für Freizeit: ist 90,00 €, üblich 25,00 €, Text „Freizeit: 90 € — sonst meist 25 €."; Basis OK                                                                          |
| ST2     | Alex: Miete −950,00 € je Juli/August/September 2026, dann −1.200,00 € im Oktober 2026 (fixe Kategorie)                                                            | das Sparpotenzial für Oktober 2026 abgefragt wird       | KEIN Hinweis — Miete ist fix und wird nie vorgeschlagen; Basis OK                                                                                                                        |
| ST2     | Alex: Freizeit −25,00 € je Juli/August/September 2026, dann −30,00 € im Oktober 2026                                                                              | das Sparpotenzial für Oktober 2026 abgefragt wird       | KEIN Hinweis — die Differenz (5,00 €) liegt unter 10,00 €; Basis OK                                                                                                                       |
| ST2     | —                                                                                                                                                                  | das Sparpotenzial für Monat 13 im Jahr 2026 abgefragt wird | wird die Abfrage abgelehnt (400)                                                                                                                                                         |
| ST3     | Alex: Essen −20,00 € (15.09.2026), Essen −80,00 € (15.10.2026) — nur ein Monat vor dem angefragten Monat                                                          | das Sparpotenzial für Oktober 2026 abgefragt wird       | keine Hinweise, Basis „WENIG_DATEN" — ehrlich, keine Empfehlung aus einem Monat                                                                                                           |

## Offene Fragen

| ID  | Frage                                                                                                    | Entschieden am |
|-----|----------------------------------------------------------------------------------------------------------|----------------|
| SF1 | Zählen Monate ohne Kategorie-Eintrag als 0 im Median/Durchschnitt (ST1/ST2, wie Prognose PF1)?             | Vorschlag 11.11.2026: ja — konsistent mit Prognose PR2 |
| SF2 | Sollte sparpotenzial Kategorien mit `ueblich` = 0 und `ist` > 0 vorschlagen (neue Kategorie)?              | Vorschlag 11.11.2026: nein — könnte einfach neu sein, kein Rauschen (Aufgabenstellung) |
| SF3 | ST3 zählt Monate mit Einträgen gesamt oder je Sicht (wie Prognose PF3)?                                    | Vorschlag 11.11.2026: je Sicht (gefilterte Einträge) |
| SF4 | Durchschnitt im Kategorie-Verlauf inklusive Nullmonate?                                                     | Vorschlag 11.11.2026: ja — zeigt den echten Schnitt über den Zeitraum |

## Daraus entstanden

- `features/statistik/kategorie-verlauf.feature` (@ST1)
- `features/statistik/sparpotenzial.feature` (@ST2)
- `features/statistik/basis.feature` (@ST3)
- Endpunkte `GET /api/statistik/kategorie/{kategorie}` und `GET /api/statistik/sparpotenzial` — dokumentiert in `frontend/API.md`
