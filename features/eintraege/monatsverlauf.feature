# language: de
Funktionalität: Monatsverlauf
  Als ich
  möchte ich sehen, wie viel jeden Monat übrig bleibt
  damit ich Plus- und Minus-Monate im Blick habe

  Regel: Der Verlauf zeigt je Monat Einnahmen und Ausgaben als absolute Beträge und die Differenz (positiv = Plus-Monat), für die letzten N Monate ab heute, ältester zuerst

    @R8
    Szenario: Verlauf zeigt Plus und Minus je Monat
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.09.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 16.09.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn der Monatsverlauf der letzten 3 Monate abgefragt wird
      Dann enthält der Monatsverlauf die Monate
        | Monat   | Einnahmen | Ausgaben | Differenz |
        | 2026-09 | 2.000,00  | 42,50    | 1.957,50  |
        | 2026-10 | 0,00      | 0,00     | 0,00      |
        | 2026-11 | 0,00      | 60,00    | -60,00    |

    @R8
    Szenario: Verlauf gefiltert nach Person
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.09.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 16.09.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn der Monatsverlauf der letzten 3 Monate für Person "Alex" abgefragt wird
      Dann enthält der Monatsverlauf die Monate
        | Monat   | Einnahmen | Ausgaben | Differenz |
        | 2026-09 | 2.000,00  | 0,00     | 2.000,00  |
        | 2026-10 | 0,00      | 0,00     | 0,00      |
        | 2026-11 | 0,00      | 0,00     | 0,00      |
