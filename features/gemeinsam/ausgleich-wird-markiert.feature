# language: de
Funktionalität: Monat als ausgeglichen markieren
  Als wir
  möchte ich einen Monat nach der Überweisung als ausgeglichen markieren
  damit festgehalten ist, welche Monate erledigt sind

  Regel: Ein Monat kann als ausgeglichen markiert werden und bleibt es; erneutes Markieren überschreibt nichts
  Regel: Der Verlauf zeigt je Monat, ob er ausgeglichen ist

    @G7
    Szenario: Monat wird als ausgeglichen markiert
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn der Monat November 2026 als ausgeglichen markiert wird
      Dann zeigt der Ausgleich November 2026 den Monat als ausgeglichen
      Und schuldet Kim Alex 30,00 Euro

    @G7
    Szenario: Erneutes Markieren bleibt ausgeglichen
      Angenommen die Personen "Alex" und "Kim" existieren
      Und der Monat November 2026 als ausgeglichen markiert wurde
      Wenn der Monat November 2026 erneut als ausgeglichen markiert wird
      Dann zeigt der Ausgleich November 2026 den Monat als ausgeglichen

    @G8
    Szenario: Verlauf zeigt ausgeglichene Monate
      Angenommen der aktuelle Monat Dezember 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Wenn der Monat November 2026 als ausgeglichen markiert wird
      Dann zeigt der Ausgleichsverlauf der letzten 2 Monate die Monate
        | Monat   | Ausgeglichen |
        | 2026-11 | true         |
        | 2026-12 | false        |
