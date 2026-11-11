# language: de
Funktionalität: Monatsübersicht
  Als ich
  möchte ich Ausgaben und Einnahmen mit Datum, Betrag und Kategorie erfassen
  damit ich eine Monatsübersicht je Kategorie sehe

  Regel: Die Monatsübersicht lehnt Monate ab, die es nicht gibt

    @R4
    Szenario: Ungültiger Monat wird abgelehnt
      Angenommen noch kein Eintrag erfasst wurde
      Wenn die Monatsübersicht Monat 13 im Jahr 2026 aufgerufen wird
      Dann wird der Monat abgelehnt
