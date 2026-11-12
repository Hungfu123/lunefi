# language: de
Funktionalität: Google-Login
  Als Partner
  möchte ich mich mit meinem Google-Konto anmelden
  damit ich mir kein Passwort merken muss und trotzdem nur wir beide rein kommen

  Regel: Der Server prüft das Google-ID-Token und lässt nur die zwei hinterlegten Mail-Adressen hinein (AU9). Gmail-Schreibweisen werden normalisiert (googlemail.com gilt wie gmail.com, Punkte zählen nicht). Fremde oder nicht verifizierte Adressen und ungültige Token werden abgewiesen, ohne dass eine Sitzung entsteht.

    @AU9
    Szenario: Hinterlegte Mail-Adresse meldet den richtigen Partner an
      Angenommen die App mit den Nutzern Alex und Kim eingerichtet ist
      Und die Google-Zuordnung "alex@gmail.com=Alex,kim@googlemail.com=Kim" hinterlegt ist
      Wenn sich jemand mit einem gültigen Google-Token für "alex@gmail.com" anmeldet
      Dann ist die Anmeldung erfolgreich und der gemeldete Name lautet "Alex"

    @AU9
    Szenario: Gmail-Schreibweisen werden normalisiert
      Angenommen die App mit den Nutzern Alex und Kim eingerichtet ist
      Und die Google-Zuordnung "alex@gmail.com=Alex" hinterlegt ist
      Wenn sich jemand mit einem gültigen Google-Token für "a.l.e.x@googlemail.com" anmeldet
      Dann ist die Anmeldung erfolgreich und der gemeldete Name lautet "Alex"

    @AU9
    Szenario: Nicht hinterlegte Mail-Adresse kommt nicht rein
      Angenommen die App mit den Nutzern Alex und Kim eingerichtet ist
      Und die Google-Zuordnung "alex@gmail.com=Alex" hinterlegt ist
      Wenn sich jemand mit einem gültigen Google-Token für "fremde@mail.de" anmeldet
      Dann wird die Anmeldung abgewiesen und es entsteht keine Sitzung

    @AU9
    Szenario: Nicht verifizierte Mail-Adresse kommt nicht rein
      Angenommen die App mit den Nutzern Alex und Kim eingerichtet ist
      Und die Google-Zuordnung "alex@gmail.com=Alex" hinterlegt ist
      Wenn sich jemand mit einem Google-Token für die nicht verifizierte Adresse "alex@gmail.com" anmeldet
      Dann wird die Anmeldung abgewiesen und es entsteht keine Sitzung

    @AU9
    Szenario: Ungültiges Token wird abgewiesen
      Angenommen die App mit den Nutzern Alex und Kim eingerichtet ist
      Und die Google-Zuordnung "alex@gmail.com=Alex" hinterlegt ist
      Wenn sich jemand mit dem ungültigen Google-Token "kaputtes-token" anmeldet
      Dann wird die Anmeldung abgewiesen und es entsteht keine Sitzung
