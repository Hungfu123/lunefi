# language: de
Funktionalität: Wer bin ich
  Als Partner
  möchte ich vom Server erfahren, wer gerade angemeldet ist
  damit das Frontend die Person aus der Anmeldung bezieht und die bisherige lokale Auswahl erspart

  Regel: Nur für Angemeldete: der Server nennt den Namen des angemeldeten Nutzers; ungemeldete Abfragen werden abgewiesen

    @AU8
    Szenario: Angemeldeter erhält den eigenen Namen
      Angenommen "Kim" angemeldet ist
      Wenn "Kim" abfragt, wer angemeldet ist
      Dann lautet der gemeldete Name "Kim"

    @AU8
    Szenario: Ungemeldete Abfrage wird abgewiesen
      Wenn jemand ohne Anmeldung abfragt, wer angemeldet ist
      Dann wird die Abfrage wegen fehlender Anmeldung abgewiesen
