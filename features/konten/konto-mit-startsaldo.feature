# language: de
Funktionalität: Konten und Gesamtsaldo
  Als ich
  möchte ich meine Bankkonten mit Startsaldo anlegen und meinen Gesamtsaldo sehen
  damit ich weiß, wie viel Geld ich insgesamt habe

  Regel: Jedes Konto gehört einer Person und hat einen Namen und einen Startsaldo

    @K1
    Szenario: Konto wird mit Startsaldo angelegt
      Angenommen noch kein Konto angelegt wurde
      Und die Person "Alex" existiert
      Wenn das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" angelegt wird
      Dann zeigt die Kontenübersicht das Konto "Girokonto" mit Saldo 3.450,00 Euro

  Regel: Der Gesamtsaldo einer Person ist die Summe aller ihrer Konten

    @K2
    Szenario: Gesamtsaldo ist die Summe aller Konten
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Und das Konto "Sparkonto" mit Startsaldo 12.000,00 Euro von "Alex" existiert
      Wenn die Kontenübersicht aufgerufen wird
      Dann beträgt der Gesamtsaldo von "Alex" 15.450,00 Euro
