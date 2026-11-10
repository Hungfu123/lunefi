# language: de
Funktionalität: Einträge erfassen
  Als ich
  möchte ich Ausgaben und Einnahmen mit Datum, Betrag und Kategorie erfassen
  damit ich eine Monatsübersicht je Kategorie sehe

  Regel: Negative Beträge sind Ausgaben, positive Einnahmen. Währung: immer Euro

    @R2
    Szenario: Einnahme zählt zu den Einnahmen und nicht zu den Ausgaben
      Angenommen ein Eintrag mit Datum 30.11.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" erfasst wird
      Wenn die Monatsübersicht November 2026 aufgerufen wird
      Dann zählt der Eintrag zu den Einnahmen und nicht zu den Ausgaben
