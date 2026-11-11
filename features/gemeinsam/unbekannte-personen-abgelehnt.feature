# language: de
Funktionalität: Absicherung der Erfassung
  Als wir
  möchten wir Ausgaben und Einnahmen mit Datum, Betrag und Kategorie erfassen
  damit die Monatsübersicht und der Ausgleich stimmen

  Regel: Jeder Eintrag gehört genau einer Person ODER ist gemeinsam
  Regel: Der Aufteilungsschlüssel für gemeinsame Kosten ist wählbar: 50/50 (Standard) oder eigene Anteile, je Kategorie konfigurierbar

    @G1
    Szenario: Gemeinsamer Eintrag mit unbekanntem Zahler wird nicht übernommen
      Angenommen die Person "Alex" existiert
      Wenn ein gemeinsamer Eintrag von der Person "Bla" mit Datum 16.11.2026, Betrag -60,00 Euro und Kategorie "Essen" erfasst werden soll
      Dann existiert kein Eintrag

    @G3
    Szenario: Aufteilung mit einer Person, die es nicht gibt, wird nicht übernommen
      Angenommen die Person "Alex" existiert
      Wenn der Aufteilungsschlüssel für Kategorie "Essen" auf Anteile mit einer unbekannten Person gesetzt werden soll
      Dann existiert keine Kategorie "Essen"

    @GF1
    Szenario: Eine bereits angelegte Person wird nur einmal geführt
      Angenommen die Person "Alex" existiert
      Wenn die Person "Alex" noch einmal angelegt wird
      Dann existiert genau die Person "Alex"
