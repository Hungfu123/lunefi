# language: de
Funktionalität: Sparpotenzial-Basis
  Als ich
  möchte ich ehrlich erfahren, wenn noch zu wenig Vergangenheit für Spar-Hinweise da ist
  damit mir keine Empfehlung aus einem einzelnen Monat vorgaukelt wird

  Regel: Ohne mindestens zwei Monate Geschichte vor dem angefragten Monat wird ehrlich WENIG_DATEN gemeldet

    @ST3
    Szenario: Bei weniger als zwei Monaten Geschichte wird ehrlich WENIG_DATEN gemeldet
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.09.2026, Betrag -20,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 15.10.2026, Betrag -80,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Wenn das Sparpotenzial für den Monat Oktober 2026 abgefragt wird
      Dann enthält das Sparpotenzial keine Hinweise
      Und ist die Sparpotenzial-Basis "WENIG_DATEN"
