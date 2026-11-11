# language: de
Funktionalität: Einträge bearbeiten
  Als ich
  möchte ich Tippfehler korrigieren und falsche Einträge entfernen können
  damit meine Übersichten stimmen

  Regel: Einträge lassen sich löschen — Kontostand und Salden passen sich automatisch an

    @R6
    Szenario: Eintrag wird gelöscht
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und ein Eintrag mit Datum 15.11.2026, Betrag -12,50 Euro und Kategorie "Essen" von "Alex" auf dem Konto "Girokonto" erfasst wird
      Wenn dieser Eintrag gelöscht wird
      Dann existiert kein Eintrag
      Und zeigt die Kontenübersicht das Konto "Girokonto" mit Saldo 3.450,00 Euro
      Und wird ein erneutes Löschen abgelehnt
