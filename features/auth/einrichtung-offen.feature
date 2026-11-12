# language: de
Funktionalität: Personenliste während der Ersteinrichtung
  Als frische Installation
  möchte ich die Einrichtungs-Seite sehen, solange noch kein App-Nutzer existiert
  damit die erste Anmeldung überhaupt möglich ist

  Regel: Solange KEIN App-Nutzer existiert, darf die Personenliste ohne Anmeldung abrufbar sein (die Einrichtung braucht sie). Sobald ein App-Nutzer existiert, wird die Liste ohne Anmeldung abgewiesen wie jeder andere geschützte Endpunkt auch (AU6-Lock-Semantik).

    @AU6
    Szenario: Während der Ersteinrichtung ist die Personenliste offen
      Angenommen noch kein Nutzer eingerichtet ist
      Und die Person "Alex" ohne App-Nutzer existiert
      Wenn jemand ohne Anmeldung die Personenliste abruft
      Dann zeigt die Personenliste die Person "Alex"

    @AU6
    Szenario: Nach der Einrichtung ist die Personenliste ohne Anmeldung zu
      Angenommen die Person "Alex" mit Passwort "geheim123" eingerichtet wurde
      Wenn jemand ohne Anmeldung die Personenliste abruft
      Dann wird die Personenliste ohne Anmeldung abgewiesen
