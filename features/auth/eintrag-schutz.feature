# language: de
Funktionalität: Schutz persönlicher Einträge
  Als Partner
  möchte ich, dass persönliche Einträge nur von der eigenen Person erfasst und geändert werden
  damit niemand für den anderen mitzeichnet und private Ausgaben privat bleiben

  Regel: Persönliche Einträge dürfen nur von der eigenen Person erfasst und geändert werden; gemeinsame Einträge gehören beiden

    @AU4
    Szenario: Ausgabe für den Partner wird abgelehnt
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" eine Ausgabe von -42,50 Euro mit Kategorie "Freizeit" für "Kim" erfasst
      Dann existiert kein Eintrag

    @AU4
    Szenario: Gemeinsamer Eintrag darf von beiden geändert werden
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn "Kim" den einzigen Eintrag zu -50,00 Euro ändert
      Dann existiert genau ein Eintrag mit Datum 15.11.2026, Betrag -50,00 Euro und Kategorie "Essen"

    @AU4
    Szenario: Persönlicher Eintrag des Partners darf nicht geändert werden
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.11.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Wenn "Alex" den einzigen Eintrag zu -50,00 Euro ändert
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt

    @AU4
    Szenario: Persönlicher Eintrag des Partners darf nicht gelöscht werden
      Angenommen die Personen "Alex" und "Kim" existieren
      Und ein Eintrag mit Datum 15.11.2026, Betrag -42,50 Euro und Kategorie "Freizeit" von "Kim" erfasst wird
      Wenn "Alex" den einzigen Eintrag löscht
      Dann wird der Zugriff wegen fehlender Berechtigung abgelehnt
