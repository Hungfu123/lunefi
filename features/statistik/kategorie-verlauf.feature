# language: de
Funktionalität: Kategorie-Verlauf
  Als ich
  möchte ich sehen, wie sich die Ausgaben einer Kategorie über die Monate entwickeln
  damit ich Trends für Charts erkenne

  Regel: Der Kategorie-Verlauf zeigt je Monat die Ausgaben-Summe der Kategorie, ältester Monat zuerst, dazu Median und Durchschnitt; Einnahmen zählen nicht

    @ST1
    Szenario: Verlauf zeigt die Ausgaben einer Kategorie je Monat
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 16.09.2026, Betrag -30,00 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und ein Eintrag mit Datum 16.10.2026, Betrag -30,00 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und ein Eintrag mit Datum 30.09.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Wenn der Kategorie-Verlauf der letzten 3 Monate für "Freizeit" abgefragt wird
      Dann enthält der Kategorie-Verlauf die Monate
        | Monat   | Betrag |
        | 2026-09 | 30,00  |
        | 2026-10 | 30,00  |
        | 2026-11 | 0,00   |
      Und ist der Median im Kategorie-Verlauf 30,00 Euro
      Und ist der Durchschnitt im Kategorie-Verlauf 20,00 Euro

    @ST1 @R7
    Szenario: Kategorie-Verlauf in der Person-Sicht zählt nur eigene Ausgaben
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 16.09.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und ein Eintrag mit Datum 16.09.2026, Betrag -100,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Freizeit" erfasst
      Wenn der Kategorie-Verlauf der letzten 3 Monate für "Freizeit" für Person "Kim" abgefragt wird
      Dann enthält der Kategorie-Verlauf die Monate
        | Monat   | Betrag |
        | 2026-09 | 42,50  |
        | 2026-10 | 0,00   |
        | 2026-11 | 0,00   |

    @ST1
    Szenario: Kategorie-Verlauf für eine andere Person wird abgelehnt
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Wenn "Kim" den Kategorie-Verlauf der letzten 3 Monate für "Freizeit" für Person "Alex" abfragt
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt

    @ST1
    Szenario: Kategorie-Verlauf ohne Monate wird abgelehnt
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Wenn der Kategorie-Verlauf für "Freizeit" mit 0 Monaten abgefragt wird
      Dann wird die Kategorie-Verlauf-Abfrage abgelehnt
