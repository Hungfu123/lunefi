# language: de
Funktionalität: Konten und Gesamtsaldo
  Als ich
  möchte ich meine Bankkonten mit Startsaldo anlegen und meinen Gesamtsaldo sehen
  damit ich weiß, wie viel Geld ich insgesamt habe

  Regel: Ein Eintrag, der einem Konto zugeordnet ist, verändert dessen Saldo um seinen Betrag

    @K3
    Szenario: Zugeordneter Eintrag verändert den Kontostand
      Angenommen die Person "Alex" existiert
      Und das Konto "Girokonto" mit Startsaldo 3.450,00 Euro von "Alex" existiert
      Wenn ein Eintrag mit Datum 30.11.2026, Betrag +2.000,00 Euro und Kategorie "Gehalt" von "Alex" auf dem Konto "Girokonto" erfasst wird
      Dann zeigt die Kontenübersicht das Konto "Girokonto" mit Saldo 5.450,00 Euro
