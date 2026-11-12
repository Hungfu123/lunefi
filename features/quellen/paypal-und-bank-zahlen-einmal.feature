# language: de
Funktionalität: PayPal und Bank zahlen nur einmal
  Als ich
  möchte ich PayPal und Girokonto gleichzeitig verbunden haben
  damit ein Kauf, den PayPal vom Konto einzieht, nur einmal zählt

  Regel: Ein realer Vorgang aus zwei Quellen wird einem bestehenden Eintrag zugeordnet statt doppelt angelegt — bei Betrag + Richtung + Zeitfenster (±3 Tage) und nur wenn der Treffer eindeutig ist (QF6/QF7, Entscheidung 12.11.2026)

    @QF6
    Szenario: Bank zieht einen PayPal-Kauf ein — nur ein Eintrag
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und die Quelle "PayPal" der Art "PSD2" für das Konto "Girokonto" existiert
      Und die Quelle "Commerzbank" der Art "FinTS" für das Konto "Girokonto" existiert
      Wenn der Import der Quelle "PayPal" mit den Umsätzen startet
        | externeKennung | datum      | betrag  | verwendungszweck |
        | PP-1           | 05.11.2026 | -60,00  | REWE via PayPal  |
      Und der Import der Quelle "Commerzbank" mit den Umsätzen startet
        | externeKennung | datum      | betrag  | verwendungszweck       |
        | CB-1           | 07.11.2026 | -60,00  | PayPal Europe Lastschrift |
      Dann existiert genau ein Eintrag im November 2026 von "Alex"

    @QF6
    Szenario: Eine Erstattung wird nicht mit dem Kauf verwechselt
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und die Quelle "PayPal" der Art "PSD2" für das Konto "Girokonto" existiert
      Und die Quelle "Commerzbank" der Art "FinTS" für das Konto "Girokonto" existiert
      Wenn der Import der Quelle "PayPal" mit den Umsätzen startet
        | externeKennung | datum      | betrag  | verwendungszweck   |
        | PP-1           | 05.11.2026 | -60,00  | REWE via PayPal    |
        | PP-2           | 06.11.2026 | +60,00  | REWE Erstattung    |
      Und der Import der Quelle "Commerzbank" mit den Umsätzen startet
        | externeKennung | datum      | betrag  | verwendungszweck       |
        | CB-1           | 07.11.2026 | -60,00  | PayPal Europe Lastschrift |
      Dann existieren genau zwei Einträge im November 2026 von "Alex"

    @QF6
    Szenario: Mehrdeutige Kandidaten werden nicht blind zusammengelegt
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und die Quelle "PayPal" der Art "PSD2" für das Konto "Girokonto" existiert
      Und die Quelle "Commerzbank" der Art "FinTS" für das Konto "Girokonto" existiert
      Wenn der Import der Quelle "PayPal" mit den Umsätzen startet
        | externeKennung | datum      | betrag  | verwendungszweck   |
        | PP-1           | 04.11.2026 | -60,00  | Kauf bei A        |
        | PP-2           | 06.11.2026 | -60,00  | Kauf bei B        |
      Und der Import der Quelle "Commerzbank" mit den Umsätzen startet
        | externeKennung | datum      | betrag  | verwendungszweck       |
        | CB-1           | 05.11.2026 | -60,00  | PayPal Europe Lastschrift |
      Dann existieren genau drei Einträge im November 2026 von "Alex"

    @QF6
    Szenario: Manuell erfasster Einkauf wird durch die Bank bestätigt, nicht doppelt gezählt
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und die Quelle "Commerzbank" der Art "FinTS" für das Konto "Girokonto" existiert
      Und ein Eintrag mit Datum 05.11.2026, Betrag -42,50 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Wenn der Import der Quelle "Commerzbank" mit den Umsätzen startet
        | externeKennung | datum      | betrag  | verwendungszweck |
        | CB-1           | 06.11.2026 | -42,50  | REWE SAGT DANKE  |
      Dann existiert genau ein Eintrag im November 2026 von "Alex"
      Und zeigt die Monatsübersicht November 2026 bei "Essen" Ausgaben von 42,50 Euro
