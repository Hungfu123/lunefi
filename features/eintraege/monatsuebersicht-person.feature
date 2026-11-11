# language: de
Funktionalität: Monatsübersicht aus eigener Sicht
  Als ich
  möchte ich die Monatsübersicht nach Person gefiltert sehen
  damit ich weiß, was MEINS ist

  Regel: Die Person-Sicht zählt nur persönliche Einträge dieser Person; gemeinsame Einträge gehören nicht hinein — sie leben in der Paar-Ansicht und im Ausgleich

    @R7
    Szenario: Person-Sicht zeigt nur eigene Einträge
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.11.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und ein Eintrag mit Datum 30.11.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn die Monatsübersicht November 2026 für Person "Alex" aufgerufen wird
      Dann zeigt die Monatsübersicht die Einnahmen je Kategorie
        | Kategorie | Einnahmen |
        | Gehalt    | 2.000,00  |
      Und zeigt die Monatsübersicht die Ausgaben je Kategorie
        | Kategorie | Ausgaben |
      Und listet die Monatsübersicht 1 Einträge mit Datum und Betrag

    @R7
    Szenario: Paar-Sicht bleibt unverändert
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.11.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und ein Eintrag mit Datum 30.11.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn die Monatsübersicht November 2026 aufgerufen wird
      Dann zeigt die Monatsübersicht die Ausgaben je Kategorie
        | Kategorie | Ausgaben |
        | Freizeit  | 42,50    |
        | Essen     | 60,00    |
      Und zeigt die Monatsübersicht die Einnahmen je Kategorie
        | Kategorie | Einnahmen |
        | Gehalt    | 2.000,00  |
      Und listet die Monatsübersicht 3 Einträge mit Datum und Betrag
