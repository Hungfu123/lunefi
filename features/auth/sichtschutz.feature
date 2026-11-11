# language: de
Funktionalität: Schutz persönlicher Sichten
  Als Partner
  möchte ich, dass persönliche Daten nur für mich einsehbar sind
  damit Ausgaben und Einnahmen privat bleiben

  Regel: Sichten mit Personen-Bezug erlauben den Personen-Parameter nur mit dem eigenen Namen
  Regel: Konten sind privat — Übersicht und Anlegen nur für den eigenen Namen

    @AU2
    Szenario: Fremde Monatsübersicht wird abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" die Monatsübersicht November 2026 von "Kim" aufruft
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt

    @AU2
    Szenario: Fremder Monatsverlauf wird abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" den Monatsverlauf von "Kim" abruft
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt

    @AU2
    Szenario: Fremde Eintragsliste wird abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" die Einträge von "Kim" abfragt
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt

    @AU2
    Szenario: Fremde Monatsprognose wird abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" die Monatsprognose von "Kim" abruft
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt

    @AU5
    Szenario: Fremde Kontenübersicht wird abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" die Konten von "Kim" abruft
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt

    @AU5
    Szenario: Konto für den Partner wird abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" ein Konto für "Kim" anlegt
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt
