# language: de
Funktionalität: Einträge erfassen
  Als ich
  möchte ich Ausgaben und Einnahmen mit Datum, Betrag und Kategorie erfassen
  damit ich eine Monatsübersicht je Kategorie sehe

  Regel: Jeder Eintrag hat Datum, Betrag und Kategorie

    @R1
    Szenario: Eintrag ohne Datum wird nicht übernommen
      Angenommen noch kein Eintrag erfasst wurde
      Wenn ein Eintrag ohne Datum erfasst werden soll
      Dann existiert kein Eintrag

    @R1
    Szenario: Eintrag ohne Betrag wird nicht übernommen
      Angenommen noch kein Eintrag erfasst wurde
      Wenn ein Eintrag ohne Betrag erfasst werden soll
      Dann existiert kein Eintrag
