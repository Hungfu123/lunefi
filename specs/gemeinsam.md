# Thema: Gemeinsame Ausgaben (Paar)

Status: Die Regeln sind **Vorschläge** (ausgewählt 2026-11-10 für den Backlog). Es gab noch **kein Example Mapping** mit echten Beispielen — die Beispiele fehlen bewusst, denn Werte werden nicht erfunden. Feature-Dateien entstehen erst, wenn die Regeln bestätigt sind und Beispiele mit echten Zahlen existieren.

## Story (Entwurf)

Als wir (ich und meine Freundin)
möchte ich gemeinsame Ausgaben markieren und ausgleichen
damit am Monatsende klar ist, wer wie viel verauslagt hat und wer wem etwas schuldet

## Regeln (Vorschläge — bestätigen oder ändern)

| ID | Regel | Status |
|----|-------------------|--------|
| G1 | Jeder Eintrag gehört genau einer Person ODER ist gemeinsam | bestätigt 2026-11-10 |
| G2 | Der Ausgleichssaldo ist die Summe der Verauslagungen je Person, gewichtet nach dem Aufteilungsschlüssel | bestätigt 2026-11-10 |
| G3 | Der Aufteilungsschlüssel für gemeinsame Kosten ist wählbar: 50/50 (Standard) oder eigene Anteile, je Kategorie konfigurierbar | bestätigt 2026-11-10 (Anteile pro Kategorie per GF1/GF2) |
| G4 | Wiederkehrende gemeinsame Fixkosten (Miete, Strom) werden nach dem vereinbarten Schlüssel aufgeteilt | **Backlog** (braucht Fixkosten-Erkennung) — Split folgt bereits G3 |
| G5 | Ein Sparziel ist erreicht, wenn die Summe aller Einzahlungen den Zielbetrag erreicht | bestätigt 2026-11-10 |
| G6 | Der Ausgleich ist jederzeit einsehbar (aktueller Monat als „läuft noch" markiert), inklusive Verlauf vergangener Monate | bestätigt 2026-11-10 (ersetzt alten Entwurf „nur nach Monatsende", per GF4) |

## Offene Fragen

| ID  | Frage                                                                                                         | Entschieden am                                                                                                                        |
| --- | ------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| GF1 | Wie heißen die beiden Personen im System (Spitznamen, Klartext, „ich"/„sie")?                                 | 2026-11-10: Jeder gibt seinen Namen beim ersten Öffnen der App selbst ein (Onboarding). Für Beispiele/Test-Szenarien: die Personen „Alex" und „Kim" |
| GF2 | Standard-Schlüssel: 50/50 oder einkommensbasiert? Und wie werden Einkommensanteile gepflegt?                  | 2026-11-10 (delegiert): Standard 50/50; abweichende Anteile je Kategorie einrichtbar |
| GF3 | 2–3 echte Beispiel-Ausgaben mit Zahlen fürs Example Mapping (wer hat was bezahlt, was war gemeinsam)          | 2026-11-10 (delegiert, Nutzer-Freigabe „wie Splitwise"): Alex zahlt den Einkauf -60,00 € (Kategorie „Essen", gemeinsam) → Kim schuldet Alex 30,00 €; Kim zahlt das gemeinsame Kino -30,00 € („Freizeit", gemeinsam) → Saldo 15,00 € für Alex |
| GF4 | Wann wird der Ausgleich berechnet — automatisch zum Monatsende oder auf Klick?                                | 2026-11-10: jederzeit einsehbar, automatisch berechnet, plus Verlauf der vergangenen Monate und Durchschnitt |
| GF5 | Erfolgt der Ausgleich real als Überweisung durch die App (nur lesender FinTS-Zugang!) oder nur als Vorschlag? | 2026-11-10 (delegiert): nur Vorschlag — die App hat nur lesenden Bankzugriff, zahlt nichts |

## Beispiele (bestätigt bzw. freigegeben 2026-11-10)

| Regel | Gegeben | Wenn | Dann |
|-------|---------|------|------|
| G1 | die Person „Alex" existiert | ein Eintrag von Alex mit -60,00 € („Essen") erfasst wird | gehört der Eintrag Alex, nicht „gemeinsam" |
| G1 | die Person „Alex" existiert | ein Eintrag ohne Person und ohne „gemeinsam"-Markierung erfasst werden soll | wird er nicht übernommen |
| G2 | Alex zahlt den Einkauf -60,00 € („Essen", gemeinsam), Alex und Kim teilen 50/50 | der Ausgleich November 2026 aufgerufen wird | schuldet Kim Alex 30,00 € |
| G3 | für die Kategorie „Essen" gilt der Anteil Alex 60 % / Kim 40 % | Alex zahlt eine gemeinsame Ausgabe -100,00 € („Essen") | trägt Kim 40,00 € und Alex 60,00 € davon |
| G5 | das Sparziel „Urlaub" mit 2.000,00 € existiert, eingezahlt sind 500,00 € | eine Einzahlung von 1.500,00 € durch Kim erfolgt | ist das Ziel erreicht |
| G6 | der aktuelle Monat ist November 2026 | der Ausgleich November 2026 aufgerufen wird | ist der Monat als „läuft noch" markiert |

## Daraus entstanden

- `features/gemeinsam/person-wird-angelegt.feature` (Onboarding, GF1-Werte „Alex"/„Kim")
- `features/gemeinsam/eintrag-gehoert-genau-einer-person.feature` (G1-Beispiele + Eintrags-Filter `person`/`monat`/`gemeinsam` aus dem API-Vertrag)
- `features/gemeinsam/ausgleich-halbhalb.feature` (G2-Beispiel)
- `features/gemeinsam/ausgleich-eigene-anteile.feature` (G3-Beispiel + ungültige Anteile + Kategorien-Standard HALBHALB)
- `features/gemeinsam/ausgleich-verlauf.feature` (G6-Beispiele: läuft-noch + Verlauf)
- `features/sparziele/sparziel-erreicht.feature` (G5-Beispiel + Zielbetrag 0,00 abgelehnt)
- `features/budgets/budget-warnung.feature` (F2: Limit 200,00 Essen, Ausgabe 250,00 → Warnung)
- `features/eintraege/ungueltiger-monat-abgelehnt.feature` (R4-Beispiel)

## Ergänzende Entscheidungen (Overnight-Autonomie, 2026-11-11)

- Einträge mit `gemeinsam: true` brauchen ein Feld `zahler` (wer hat verauslagt) — der API-Vertrag führt `zahler` nur im Ausgleich, nicht im Eintrag-POST; ohne Zahler ist G2 nicht berechenbar. `zahler` ist Pflicht bei `gemeinsam: true`, sonst 400. Muss mit dem Frontend abgestimmt werden (API.md ist nicht von mir geändert worden).
- Ungültige Anteile: neben Summe ≠ 100 werden auch leere Personen-Namen, unbekannte `art` und Anteile ≤ 0 mit 400 abgelehnt (Anteile > 0 ohne eigenes Szenario, nur Review).
- Verlauf: `monate` fehlt → Default 6; `monate < 1` → 400.
- Der „aktuelle Monat" für `laeuftNoch` läuft über eine injizierbare Uhr (Clock-Bean); in den BDD-Tests setzt ein Schritt die Uhr auf November 2026, damit die Szenarien zeitstabil sind.
- Gemeinsame Einnahmen (positiver Betrag, `gemeinsam: true`) drehen die Saldo-Wirkung um (Erstattungs-Logik); Spec nennt nur Verauslagungen, Verhalten ist die naheliegende Verallgemeinerung.
