# language: de
Funktionalität: Monatsübersicht
  Als ich
  möchte ich Ausgaben und Einnahmen mit Datum, Betrag und Kategorie erfassen
  damit ich eine Monatsübersicht je Kategorie sehe

  Regel: Die Monatsübersicht zeigt Ausgaben und Einnahmen je Kategorie als Summe und listet jeden Eintrag einzeln (Datum, Betrag)

    @R3
    Szenario: Monatsübersicht zeigt Ausgaben, Einnahmen und alle Einträge
      Angenommen ein Eintrag mit Datum 15.11.2026, Betrag -12,50 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 01.11.2026, Betrag -950,00 Euro und Kategorie "Miete" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 30.11.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Wenn die Monatsübersicht November 2026 aufgerufen wird
      Dann zeigt die Monatsübersicht die Ausgaben je Kategorie
        | Kategorie | Ausgaben |
        | Essen     | 12,50    |
        | Miete     | 950,00   |
      Und zeigt die Monatsübersicht die Einnahmen je Kategorie
        | Kategorie | Einnahmen |
        | Gehalt    | 2.000,00  |
      Und listet die Monatsübersicht 3 Einträge mit Datum und Betrag
