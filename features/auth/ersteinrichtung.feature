# language: de
Funktionalität: Nachträgliche Passwort-Vergabe
  Als Partner
  möchte ich für bereits bestehende Personen Passwörter nachrüsten
  damit die offen angelegten Daten einen Login bekommen, ohne die App neu aufzubauen

  Regel: Die Nachrüstung ist nur erlaubt, solange noch kein App-Nutzer existiert; jede übergebene Person muss existieren, jedes Passwort braucht mindestens 6 Zeichen — sonst wird komplett abgewiesen

    @AU7
    Szenario: Nachträgliche Passwort-Vergabe klappt und Anmeldung danach funktioniert
      Angenommen die Personen "Alex" und "Kim" ohne Passwort existieren
      Wenn jemand ohne Anmeldung die Passwörter vergibt
        | Person | Passwort  |
        | Alex   | geheim123 |
        | Kim    | geheim456 |
      Dann ist die nachträgliche Einrichtung erfolgreich
      Und kann sich "Alex" danach mit dem Passwort "geheim123" anmelden
      Und kann sich "Kim" danach mit dem Passwort "geheim456" anmelden

    @AU7
    Szenario: Bereits eingerichtete App weist die Nachrüstung ab
      Angenommen die App eingerichtet ist
      Wenn jemand ohne Anmeldung die Passwörter vergibt
        | Person | Passwort  |
        | Alex   | geheim123 |
      Dann wird die Nachrüstung wegen bestehender Einrichtung abgewiesen

    @AU7
    Szenario: Unbekannte Person wird nicht übernommen
      Angenommen die Person "Alex" ohne Passwort existiert
      Wenn jemand ohne Anmeldung die Passwörter vergibt
        | Person    | Passwort  |
        | Alex      | geheim123 |
        | Unbekannt | geheim789 |
      Dann wird die Nachrüstung wegen ungültiger Angaben abgewiesen

    @AU7
    Szenario: Zu kurzes Passwort wird nicht übernommen
      Angenommen die Person "Alex" ohne Passwort existiert
      Wenn jemand ohne Anmeldung die Passwörter vergibt
        | Person | Passwort |
        | Alex   | kurz     |
      Dann wird die Nachrüstung wegen ungültiger Angaben abgewiesen
