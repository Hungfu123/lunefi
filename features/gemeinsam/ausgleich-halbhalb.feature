# language: de
Funktionalität: Ausgleich gemeinsamer Ausgaben
  Als wir
  möchte ich den Ausgleich gemeinsamer Ausgaben einsehen
  damit am Monatsende klar ist, wer wem etwas schuldet

  Regel: Der Ausgleichssaldo ist die Summe der Verauslagungen je Person, gewichtet nach dem Aufteilungsschlüssel

    @G2
    Szenario: Gemeinsame Ausgabe wird hälftig geteilt
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn der Ausgleich November 2026 aufgerufen wird
      Dann schuldet Kim Alex 30,00 Euro
