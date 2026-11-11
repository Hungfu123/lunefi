# Thema: Budgets — Spar-Coaching

Status: F2 (Budget-Limits + Warnung) ist bestätigt (2026-11-10, siehe `specs/eintraege.md` und `features/budgets/budget-warnung.feature`). C1–C3 sind **Vorschläge** (Nutzer-Wunsch 11.11.2026: „wenn man spart, dass man auch sagt ok welche Kategorie davon kann man mehr ausgeben oder nicht oder will man oder nicht; dann stellt man auch Kategorien ein; der Partner kann das auch sehen wenn es geteilte Kategorien sind"). Alle Werte darin sind **Vorschlagswerte**, noch nicht bestätigt.

## Story (Vorschlag)

Als wir
möchte ich je Kategorie einstellen, ob wir dort sparen wollen, neutral bleiben oder bewusst mehr ausgeben dürfen
damit die App beim Sparen ermutigt statt nur warnt — und beide Partner dieselbe Empfehlung sehen

## Regeln (Vorschläge)

| ID | Regel | Status |
|----|-------|--------|
| C1 | Jede Kategorie hat eine „Haltung" fürs Budget-Coaching: SPAREN (unter dem Limit bleiben ist das Ziel), FLEXIBEL (neutral, Standard) oder BEWUSST_MEHR (man darf/will dort bewusst mehr ausgeben). Einstellbar, ungültige Haltung wird abgelehnt. Default für neue Kategorien: FLEXIBEL. Eine Kategorie ohne Budget kann die Haltung trotzdem haben — sie wirkt erst mit Budget. | Vorschlag 11.11.2026 |
| C2 | Das Spar-Coaching je Monat wertet nur Kategorien mit Budget: Haltung BEWUSST_MEHR mit Spielraum → Empfehlung (mehr ausgeben ermutigt, menschlicher Tipp-Text); Haltung SPAREN innerhalb des Limits → Lob (ohne Tipp-Zwang); Haltung FLEXIBEL → nur die verbleibende Zahl, keine Wertung (Prinzip 5 „emotional neutral": die App drängt niemandem Ausgaben auf — BEWUSST_MEHR-Empfehlungen sind Ermutigungen nur, wenn der Nutzer die Haltung selbst gewählt hat). Über-Limit-Kategorien erscheinen nirgends (keine zusätzliche Negativ-Liste — die Budget-Warnung aus F2 existiert schon). | Vorschlag 11.11.2026 |
| C3 | Haltungen sind Paar-Daten (Kategorien sind paar-weit): der Coaching-Endpunkt hat keinen person/gemeinsam-Filter — beide Partner sehen dieselben Haltungen und Tipps. | Vorschlag 11.11.2026 |

## Beispiele (Vorschlagswerte 11.11.2026)

| Regel | Gegeben | Wenn | Dann |
|-------|---------|------|------|
| C1 | die Kategorie „Freizeit" existiert (durch Nutzung) | die Haltung für „Freizeit" auf BEWUSST_MEHR gestellt wird | zeigt die Kategorien-Liste für „Freizeit" die Haltung BEWUSST_MEHR |
| C1 | die Kategorie „Freizeit" existiert | die Haltung für „Freizeit" auf einen ungültigen Wert („SPARSUCHT") gestellt werden soll | wird das Einstellen abgelehnt |
| C1 | ein Eintrag in der neuen Kategorie „Freizeit" erfasst wird | die Kategorien-Liste abgefragt wird | hat „Freizeit" die Haltung FLEXIBEL (Default) |
| C2 | Budget „Freizeit" Limit 40,00 €, Ausgaben 25,00 €, Haltung BEWUSST_MEHR | das Spar-Coaching November 2026 aufgerufen wird | empfiehlt das Coaching bei „Freizeit" 15,00 € mehr auszugeben |
| C2 | Budget „Miete" Limit 950,00 €, Ausgaben 950,00 €, Haltung SPAREN; Budget „Freizeit" Limit 40,00 €, Ausgaben 25,00 €, Haltung FLEXIBEL | das Spar-Coaching November 2026 aufgerufen wird | lobt das Coaching das Sparen bei „Miete" (Spielraum 0,00 €); „Freizeit" erscheint nur als verbleibender Rahmen von 15,00 € |
| C2 | Budget „Essen" Limit 30,00 €, Ausgaben 42,50 € | das Spar-Coaching November 2026 aufgerufen wird | erwähnt das Coaching die Kategorie „Essen" nirgends |

## Interpretationen (offene Punkte in den Vorschlägen, 11.11.2026)

- Die C2-Beispiel-JSON des Nutzers zeigt „Miete" (ist = limit, Spielraum 0,00) in gutGespart, während der Regeltext „Spielraum > 0" sagt. Aufgelöst als: **gutGespart = SPAREN und ist ≤ limit** (im Limit bleiben ist gespart), Empfehlungen brauchen Spielraum > 0. Offen zur Bestätigung.
- Die C2-Beispiel-JSON zeigt „Essen" (über Limit) in budgetsUebrig mit 0,00 — der Regeltext sagt „Über-Limit-Kategorien erscheinen nirgends". Aufgelöst zugunsten des Regeltexts und der Szenario-Vorgabe (Essen → nirgends): budgetsUebrig enthält nur FLEXIBEL-Kategorien innerhalb des Limits. Offen zur Bestätigung.
- Spielraum in der Antwort = max(limit − ist; 0), als nicht-negative Zahl.
- Haltung für eine Kategorie ohne Eintrag/Kategorie-Zeile: wird angelegt (wie Split-Konfiguration, Upsert).
- Der Tipp-Text („Freizeit verträgt 15,00 € mehr — bewusst genießen") ist ein Formulierungsvorschlag, kein bestätigter Wortlaut.

## Offene Fragen

| ID | Frage | Entscheidung |
|----|-------|--------------|
| CF1 | Spielt „Miete" exakt auf dem Limit in gutGespart (JSON-Beispiel) oder nur mit echtem Spielraum > 0 (Regeltext)? | Vorschlag 11.11.2026: ist ≤ limit zählt als gespart — zu bestätigen |
| CF2 | Kommt eine über-Limit-Kategorie mit 0,00 in budgetsUebrig (JSON-Beispiel) oder nirgends (Regeltext)? | Vorschlag 11.11.2026: nirgends — die F2-Warnung existiert schon |
| CF3 | Endgültiger Wortlaut der Tipp-Texte (DE, Anrede, Betragsformat)? | Vorschlag 11.11.2026: „{Kategorie} verträgt {Betrag} € mehr — bewusst genießen" |
| CF4 | Darf man die Haltung löschen/zurücksetzen (eigener Endpunkt)? | offen — Überschreiben mit jedem gültigen Wert reicht erstmal |

## Daraus entstanden

- `features/budgets/coaching.feature` (C1: Haltung setzen/abfragen, ungültige Haltung, Default FLEXIBEL; C2: Empfehlung BEWUSST_MEHR, gutGespart SPAREN + FLEXIBEL nur budgetsUebrig, über Limit nirgends; C3 ohne eigenes Szenario — reine Paar-Sicht, dokumentiert in `frontend/API.md`)
