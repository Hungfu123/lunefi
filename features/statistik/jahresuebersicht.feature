# language: de
Funktionalität: Jahresübersicht
  Als ich
  möchte ich Einnahmen und Ausgaben über das ganze Jahr sehen
  damit ich Trends erkenne und weiß, wohin unser Geld fließt

  Regel: Die Jahresübersicht zeigt je Monat Einnahmen und Ausgaben des angefragten Jahres (Januar bis Dezember) sowie die Kategorie-Summen des Jahres; Einträge anderer Jahre zählen nicht

    @ST3
    Szenario: Die Jahresübersicht zählt nur das angefragte Jahr
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.03.2026, Betrag -50,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 20.10.2026, Betrag -30,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 10.03.2025, Betrag -100,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Wenn die Jahresübersicht 2026 abgefragt wird
      Dann zeigt die Jahresübersicht 12 Monate
      Und zeigt die Jahresübersicht für Monat "2026-03" Einnahmen 0,00 Euro und Ausgaben 50,00 Euro
      Und zeigt die Jahresübersicht für Monat "2026-10" Einnahmen 0,00 Euro und Ausgaben 30,00 Euro
      Und sind die Jahres-Ausgaben insgesamt 80,00 Euro

    @ST3
    Szenario: Kategorien werden über alle Monate des Jahres summiert
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 05.01.2026, Betrag +1.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 12.01.2026, Betrag -20,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 03.02.2026, Betrag -30,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 28.02.2026, Betrag -50,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Wenn die Jahresübersicht 2026 abgefragt wird
      Dann zeigt die Jahresübersicht Ausgaben 50,00 Euro für Kategorie "Essen"
      Und zeigt die Jahresübersicht Ausgaben 50,00 Euro für Kategorie "Freizeit"
      Und zeigt die Jahresübersicht Einnahmen 1.000,00 Euro für Kategorie "Gehalt"
