# language: de
Funktionalität: Anmeldung
  Als Partner
  möchte ich mich mit Namen und Passwort anmelden
  damit niemand fremde persönliche Finanzdaten einsehen oder ändern kann

  Regel: Nutzer melden sich mit Name und Passwort an; falsche Zugangsdaten und ungemeldete Zugriffe werden abgelehnt; beim Anlegen einer Person ist ein Passwort Pflicht

    @AU1
    Szenario: Anmeldung nach Einrichtung klappt
      Angenommen die Person "Nova" mit Passwort "geheim123" eingerichtet wurde
      Wenn sich "Nova" mit dem Passwort "geheim123" anmeldet
      Dann ist die Anmeldung erfolgreich

    @AU1
    Szenario: Falsches Passwort wird abgelehnt
      Angenommen die Person "Nova" mit Passwort "geheim123" eingerichtet wurde
      Wenn sich "Nova" mit dem Passwort "falsches-passwort" anmeldet
      Dann wird die Anmeldung abgelehnt

    @AU1
    Szenario: Abgemeldeter Zugriff wird abgelehnt
      Wenn jemand ohne Anmeldung die Einträge abruft
      Dann wird der Zugriff wegen fehlender Anmeldung abgelehnt

    @AU1
    Szenario: Abmeldung beendet den Zugang
      Angenommen "Alex" angemeldet ist
      Wenn sich "Alex" abmeldet
      Dann wird der Zugriff wegen fehlender Anmeldung abgelehnt

    @AU1
    Szenario: Person ohne Passwort wird nicht übernommen
      Angenommen "Alex" angemeldet ist
      Wenn "Alex" die Person "Nova" ohne Passwort anlegt
      Dann wird das Anlegen abgelehnt

    @AU1
    Szenario: Zu kurzes Passwort wird nicht übernommen
      Angenommen "Alex" angemeldet ist
      Wenn "Alex" die Person "Nova" mit dem Passwort "kurz" anlegt
      Dann wird das Anlegen abgelehnt

    @AU6
    Szenario: Erste Einrichtung ist ohne Anmeldung möglich
      Angenommen noch kein Nutzer eingerichtet ist
      Wenn jemand ohne Anmeldung die Person "Nova" mit dem Passwort "geheim123" anlegt
      Dann ist die Person "Nova" eingerichtet

    @AU6
    Szenario: Nach der Einrichtung ist die Ersteinrichtung geschlossen
      Angenommen die App eingerichtet ist
      Wenn jemand ohne Anmeldung die Person "Nova" mit dem Passwort "geheim123" anlegt
      Dann wird das Anlegen wegen fehlender Anmeldung abgelehnt
