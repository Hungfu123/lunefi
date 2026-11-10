# language: de
Funktionalität: Einträge erfassen
  Als ich
  möchte ich Ausgaben und Einnahmen mit Datum, Betrag und Kategorie erfassen
  damit ich eine Monatsübersicht je Kategorie sehe

  Regel: Jeder Eintrag hat Datum, Betrag und Kategorie

    @R1
    Szenario: Erfasster Eintrag wird aufbewahrt
      Angenommen noch kein Eintrag erfasst wurde
      Wenn ein Eintrag mit Datum 15.11.2026, Betrag -12,50 Euro und Kategorie "Essen" erfasst wird
      Dann existiert genau ein Eintrag mit Datum 15.11.2026, Betrag -12,50 Euro und Kategorie "Essen"
