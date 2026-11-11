# language: de
Funktionalität: Sparpotenzial
  Als ich
  möchte ich Hinweise, wo ich ungewöhnlich viel ausgebe
  damit ich sinnvoll sparen kann — ohne dass fixe Kosten wie die Miete vorgeschlagen werden

  Regel: Für den angefragten Monat wird je Kategorie verglichen, ob die Ausgaben deutlich über dem üblichen liegen — fixe Kategorien werden nie vorgeschlagen, kleine Abweichungen nicht

    @ST2
    Szenario: Eine deutlich erhöhte Ausgabe wird als Sparhinweis gemeldet
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.07.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 15.08.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 15.09.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 15.10.2026, Betrag -90,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Wenn das Sparpotenzial für den Monat Oktober 2026 abgefragt wird
      Dann enthält das Sparpotenzial genau einen Hinweis für "Freizeit" mit Ist 90,00 Euro, üblich 25,00 Euro und dem Text "Freizeit: 90 € — sonst meist 25 €."
      Und ist die Sparpotenzial-Basis "OK"

    @ST2
    Szenario: Eine gestiegene fixe Kategorie wird nie vorgeschlagen
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 01.07.2026, Betrag -950,00 Euro und Kategorie "Miete" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 01.08.2026, Betrag -950,00 Euro und Kategorie "Miete" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 01.09.2026, Betrag -950,00 Euro und Kategorie "Miete" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 01.10.2026, Betrag -1.200,00 Euro und Kategorie "Miete" von "Alex" erfasst wird
      Wenn das Sparpotenzial für den Monat Oktober 2026 abgefragt wird
      Dann enthält das Sparpotenzial keine Hinweise
      Und ist die Sparpotenzial-Basis "OK"

    @ST2
    Szenario: Eine kleine Abweichung wird nicht gemeldet
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.07.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 15.08.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 15.09.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 15.10.2026, Betrag -30,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Wenn das Sparpotenzial für den Monat Oktober 2026 abgefragt wird
      Dann enthält das Sparpotenzial keine Hinweise
      Und ist die Sparpotenzial-Basis "OK"

    @ST2
    Szenario: Ein ungültiger Monat wird abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn das Sparpotenzial für den ungültigen Monat "2026-13" abgefragt wird
      Dann wird die Sparpotenzial-Abfrage abgelehnt
