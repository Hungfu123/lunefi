# language: de
Funktionalität: Einträge bearbeiten
  Als ich
  möchte ich Tippfehler korrigieren und falsche Einträge entfernen können
  damit meine Übersichten stimmen

  Regel: Einträge lassen sich ändern — gleiche Validierungen wie beim Anlegen, unbekannter Eintrag → 404

    @R5
    Szenario: Eintrag wird geändert
      Angenommen ein Eintrag mit Datum 15.11.2026, Betrag -12,50 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Wenn der Betrag dieses Eintrags zu -50,00 Euro geändert wird
      Dann existiert genau ein Eintrag mit Datum 15.11.2026, Betrag -50,00 Euro und Kategorie "Essen"
      Und zeigt die Monatsübersicht November 2026 bei "Essen" Ausgaben von 50,00 Euro

    @R5
    Szenario: Unbekannter Eintrag wird nicht geändert
      Angenommen noch kein Eintrag erfasst wurde
      Wenn der Eintrag mit der Nummer 999 geändert werden soll
      Dann wird das Ändern abgelehnt
