# language: de
Funktionalität: Ausgleich jederzeit und im Verlauf einsehen
  Als wir
  möchte ich den Ausgleich jederzeit einsehen, auch vergangener Monate
  damit wir unabhängig vom Monatsende den Stand kennen

  Regel: Der Ausgleich ist jederzeit einsehbar (aktueller Monat als „läuft noch" markiert), inklusive Verlauf vergangener Monate

    @G6
    Szenario: Aktueller Monat läuft noch
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Wenn der Ausgleich November 2026 aufgerufen wird
      Dann ist der Monat als "läuft noch" markiert

    @G6
    Szenario: Verlauf zeigt den Ausgleich vergangener Monate
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn der Ausgleichsverlauf der letzten 2 Monate abgefragt wird
      Dann enthält der Verlauf die Monate
        | Monat   | Vorschlag                 |
        | 2026-10 | keine                     |
        | 2026-11 | Kim schuldet Alex 30,00 € |
      Und zeigt der Verlauf keine einzelnen Ausgaben
