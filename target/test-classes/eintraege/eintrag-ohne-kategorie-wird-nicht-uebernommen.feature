# language: de
Funktionalität: Einträge erfassen
  Als ich
  möchte ich Ausgaben und Einnahmen mit Datum, Betrag und Kategorie erfassen
  damit ich eine Monatsübersicht je Kategorie sehe

  Regel: Jeder Eintrag hat Datum, Betrag und Kategorie

    @R1
    Szenario: Eintrag ohne Kategorie wird nicht übernommen
      Angenommen noch kein Eintrag erfasst wurde
      Wenn ein Eintrag ohne Kategorie erfasst werden soll
      Dann existiert kein Eintrag
