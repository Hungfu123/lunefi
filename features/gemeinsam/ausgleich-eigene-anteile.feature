# language: de
Funktionalität: Aufteilungsschlüssel je Kategorie
  Als wir
  möchte ich für gemeinsame Kosten den Aufteilungsschlüssel je Kategorie wählen
  damit der Ausgleich unserem vereinbarten Schlüssel folgt

  Regel: Der Aufteilungsschlüssel für gemeinsame Kosten ist wählbar: 50/50 (Standard) oder eigene Anteile, je Kategorie konfigurierbar

    @G3
    Szenario: Eigene Anteile je Kategorie
      Angenommen die Personen "Alex" und "Kim" existieren
      Und der Aufteilungsschlüssel für Kategorie "Essen" beträgt
        | Person | Anteil in Prozent |
        | Alex   | 60                |
        | Kim    | 40                |
      Und "Alex" eine gemeinsame Ausgabe mit -100,00 Euro und Kategorie "Essen" erfasst
      Wenn der Ausgleich November 2026 aufgerufen wird
      Dann trägt Kim 40,00 Euro und Alex 60,00 Euro davon

    @G3
    Szenario: Ungültige Anteile werden abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn der Aufteilungsschlüssel für Kategorie "Essen" auf ungültige Anteile gesetzt werden soll
        | Person | Anteil in Prozent |
        | Alex   | 60                |
        | Kim    | 50                |
      Dann existiert keine Kategorie "Essen"

    @G3
    Szenario: Kategorien entstehen durch Nutzung mit hälftiger Aufteilung
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Dann existiert die Kategorie "Essen" mit Aufteilung "HALBHALB"
