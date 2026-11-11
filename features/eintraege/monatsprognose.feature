# language: de
Funktionalität: Monatsprognose
  Als ich
  möchte ich eine Prognose der Ausgaben und Einnahmen für den Folgemonat sehen
  damit ich heute weiß, was der nächste Monat vermutlich übrig lässt

  Regel: Fixe Kosten werden mit ihrem letzten bekannten Monatswert übernommen, variable Ausgaben als Median mit Ausreißer-Schutz, nur regelmäßige Einnahmen zählen, und bei zu wenig Daten wird ehrlich 0 prognostiziert

    @PR1
    Szenario: Fixe Kosten werden mit dem letzten bekannten Monatswert übernommen
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 01.09.2026, Betrag -900,00 Euro und Kategorie "Miete" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 01.10.2026, Betrag -950,00 Euro und Kategorie "Miete" von "Alex" erfasst wird
      Wenn die Monatsprognose abgefragt wird
      Dann gilt die Prognose für den Monat Dezember 2026
      Und prognostiziert sie für die Kategorie "Miete" Ausgaben von 950,00 Euro
      Und beträgt die prognostizierte Differenz -950,00 Euro
      Und ist die Konfidenz "OK"
      Und sind die Basis-Monate August 2026, September 2026, Oktober 2026

    @PR2
    Szenario: Ein Ausreißer-Monat verzerrt die Ausgaben-Prognose nicht
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 05.08.2026, Betrag -150,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 05.09.2026, Betrag -150,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 05.10.2026, Betrag -2.000,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
      Wenn die Monatsprognose abgefragt wird
      Dann prognostiziert sie für die Kategorie "Freizeit" Ausgaben von 150,00 Euro
      Und beträgt die prognostizierte Differenz -150,00 Euro
      Und ist die Konfidenz "OK"

    @PR3
    Szenario: Eine einmalige Einnahme zählt nicht, eine regelmäßige zählt
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 30.09.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 30.10.2026, Betrag +1.900,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 10.09.2026, Betrag +40,00 Euro und Kategorie "Erstattung" von "Alex" erfasst wird
      Wenn die Monatsprognose abgefragt wird
      Dann prognostiziert sie für die Kategorie "Gehalt" Einnahmen von 1.950,00 Euro
      Und enthält sie keine Einnahmen für die Kategorie "Erstattung"
      Und beträgt die prognostizierte Differenz 1.950,00 Euro
      Und ist die Konfidenz "OK"

    @PR4
    Szenario: Bei weniger als zwei Monaten mit Einträgen wird ehrlich 0 prognostiziert
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.09.2026, Betrag -20,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Wenn die Monatsprognose abgefragt wird
      Dann ist die Konfidenz "WENIG_DATEN"
      Und enthält sie keine Ausgaben für die Kategorie "Essen"
      Und beträgt die prognostizierte Differenz 0,00 Euro

    @PR5
    Szenario: Prognose mit Person-Filter und Paar-Sicht zusammen wird abgelehnt
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Wenn die Monatsprognose für Person "Alex" und in der Paar-Sicht abgefragt wird
      Dann wird die Prognose-Abfrage abgelehnt

    @PR5 @R7
    Szenario: Prognose in der Person-Sicht zählt nur eigene Einträge
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 05.09.2026, Betrag -100,00 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und ein Eintrag mit Datum 05.10.2026, Betrag -200,00 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Und ein Eintrag mit Datum 30.09.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Und ein Eintrag mit Datum 30.10.2026, Betrag +1.900,00 Euro und Kategorie "Gehalt" von "Alex" erfasst wird
      Wenn die Monatsprognose für Person "Kim" abgefragt wird
      Dann prognostiziert sie für die Kategorie "Freizeit" Ausgaben von 100,00 Euro
      Und enthält sie keine Einnahmen für die Kategorie "Gehalt"
      Und beträgt die prognostizierte Differenz -100,00 Euro
