# language: de
Funktionalität: Quellen verwalten
  Als ich
  möchte ich Kontoquellen (Banken, PayPal) anbinden und ihre Umsätze automatisch importieren
  damit Einträge von selbst entstehen und ich die Bank wechseln kann, ohne die App umzubauen

  Regel: Jede Quelle hat eine Art (FinTS, PSD2) und einen Zustand

    @Q1
    Szenario: Quelle wird angelegt
      Angenommen noch keine Quelle angelegt wurde
      Und die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Wenn die Quelle "Commerzbank" der Art "FinTS" für das Konto "Girokonto" angelegt wird
      Dann zeigt die Quellenübersicht die Quelle "Commerzbank" mit Zustand "offen"
