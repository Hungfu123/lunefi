# language: de
Funktionalität: Monatsübersicht in der Paar-Sicht
  Als Paar
  möchten wir nur sehen, was uns gemeinsam gehört
  damit private Saldi und Einnahmen privat bleiben

  Regel: Die Paar-Sicht zeigt nur gemeinsame Einträge — Saldi und persönliche Einträge (inklusive Einnahmen) sind privat

    @R9
    Szenario: Paar-Sicht zeigt nur gemeinsame Einträge
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.11.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und ein Eintrag mit Datum 30.11.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn die Monatsübersicht November 2026 in der Paar-Sicht aufgerufen wird
      Dann zeigt die Monatsübersicht die Ausgaben je Kategorie
        | Kategorie | Ausgaben |
        | Essen     | 60,00    |
      Und zeigt die Monatsübersicht die Einnahmen je Kategorie
        | Kategorie | Einnahmen |
      Und listet die Monatsübersicht 1 Einträge mit Datum und Betrag

    @R9
    Szenario: Verlauf der Paar-Sicht zählt nur Gemeinsames
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.09.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 16.09.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn der Monatsverlauf der letzten 3 Monate in der Paar-Sicht abgefragt wird
      Dann enthält der Monatsverlauf die Monate
        | Monat   | Einnahmen | Ausgaben | Differenz |
        | 2026-09 | 0,00      | 0,00     | 0,00      |
        | 2026-10 | 0,00      | 0,00     | 0,00      |
        | 2026-11 | 0,00      | 60,00    | -60,00    |

    @R9
    Szenario: Person und Paar-Sicht zusammen werden abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn die Monatsübersicht November 2026 für Person "Alex" und in der Paar-Sicht aufgerufen wird
      Dann wird die Abfrage abgelehnt
