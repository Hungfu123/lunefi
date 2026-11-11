# language: de
Funktionalität: Einträge gehören genau einer Person oder der Gemeinschaft
  Als wir
  möchte ich jeden Eintrag einer Person oder der Gemeinschaft zuordnen
  damit der Ausgleich am Monatsende eindeutig ist

  Regel: Jeder Eintrag gehört genau einer Person ODER ist gemeinsam

    @G1
    Szenario: Eintrag gehört einer Person und ist nicht gemeinsam
      Angenommen die Person "Alex" existiert
      Wenn ein Eintrag mit Datum 15.11.2026, Betrag -60,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Dann gehört der Eintrag "Alex" und ist nicht gemeinsam

    @G1
    Szenario: Eintrag ohne Person und ohne "gemeinsam"-Markierung wird nicht übernommen
      Angenommen die Person "Alex" existiert
      Wenn ein Eintrag ohne Person und ohne "gemeinsam"-Markierung erfasst werden soll
      Dann existiert kein Eintrag

    @G1
    Szenario: Eintrag mit Person und "gemeinsam"-Markierung wird nicht übernommen
      Angenommen die Person "Alex" existiert
      Wenn ein Eintrag von "Alex" mit "gemeinsam"-Markierung erfasst werden soll
      Dann existiert kein Eintrag

    @G1
    Szenario: Nur Einträge einer Person und eines Monats
      Angenommen die Person "Alex" existiert
      Und ein Eintrag mit Datum 15.11.2026, Betrag -12,50 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn nur die Einträge von "Alex" im November 2026 abgefragt werden
      Dann existiert im Ergebnis genau ein Eintrag mit Betrag -12,50 Euro und Kategorie "Essen"

    @G1
    Szenario: Nur gemeinsame Einträge
      Angenommen die Person "Alex" existiert
      Und ein Eintrag mit Datum 15.11.2026, Betrag -12,50 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn nur die gemeinsamen Einträge abgefragt werden
      Dann existiert im Ergebnis genau ein Eintrag mit Betrag -60,00 Euro und Kategorie "Essen"
