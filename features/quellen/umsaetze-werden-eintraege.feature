# language: de
Funktionalität: Umsätze importieren
  Als ich
  möchte ich Kontoquellen (Banken, PayPal) anbinden und ihre Umsätze automatisch importieren
  damit Einträge von selbst entstehen

  Regel: Ein Import erzeugt aus jedem Umsatz genau einen Eintrag

    @Q2
    Szenario: Umsätze werden Einträge
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und die Quelle "Commerzbank" der Art "FinTS" für das Konto "Girokonto" existiert
      Wenn der Import der Quelle "Commerzbank" mit den Umsätzen startet
        | externeKennung | datum     | betrag   | verwendungszweck        |
        | UMS-1          | 01.11.2026 | +2.000,00 | Gehalt November        |
        | UMS-2          | 02.11.2026 | -42,50    | REWE SAGT DANKE        |
      Dann existieren genau zwei Einträge im November 2026 von "Alex"

  Regel: Derselbe Umsatz wird bei wiederholtem Import nicht doppelt übernommen

    @Q3
    Szenario: Wiederholter Import überspringt Doppelte
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und die Quelle "Commerzbank" der Art "FinTS" für das Konto "Girokonto" existiert
      Und der Import der Quelle "Commerzbank" mit dem Umsatz "UMS-1" vom 01.11.2026 über +2.000,00 Euro bereits gelaufen ist
      Wenn derselbe Import erneut startet
      Dann existiert genau ein Eintrag im November 2026 von "Alex"

  Regel: Gelernte Kategorien werden automatisch angewendet

    @Q4
    Szenario: Kategorie wird gelernt
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und die Quelle "Commerzbank" der Art "FinTS" für das Konto "Girokonto" existiert
      Und die Lernregel für "REWE" auf Kategorie "Essen" gespeichert ist
      Wenn der Import der Quelle "Commerzbank" mit den Umsätzen startet
        | externeKennung | datum     | betrag | verwendungszweck |
        | UMS-3          | 02.11.2026 | -42,50 | REWE SAGT DANKE  |
      Dann hat der Eintrag vom 02.11.2026 über -42,50 Euro die Kategorie "Essen"
