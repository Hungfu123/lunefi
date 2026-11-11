# language: de
Funktionalität: Sparziele
  Als wir
  möchte ich auf ein Sparziel einzahlen
  damit wir sehen, wann das Ziel erreicht ist

  Regel: Ein Sparziel ist erreicht, wenn die Summe aller Einzahlungen den Zielbetrag erreicht

    @G5
    Szenario: Sparziel wird erreicht
      Angenommen das Sparziel "Urlaub" mit 2.000,00 Euro existiert
      Und 500,00 Euro eingezahlt wurden
      Wenn eine Einzahlung von 1.500,00 Euro durch "Kim" erfolgt
      Dann ist das Sparziel erreicht

    @G5
    Szenario: Sparziel ohne Zielbetrag wird nicht übernommen
      Angenommen noch kein Sparziel existiert
      Wenn das Sparziel "Urlaub" mit 0,00 Euro erstellt werden soll
      Dann existiert kein Sparziel
