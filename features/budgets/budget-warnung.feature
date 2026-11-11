# language: de
Funktionalität: Budgets
  Als ich
  möchte ich je Kategorie und Monat ein Budget-Limit setzen
  damit ich in der Übersicht gewarnt werde, bevor ich es überschreite

  Regel: Budget-Limits je Kategorie — Warnung in der Übersicht, kein Block

    @F2
    Szenario: Überschrittenes Budget warnt in der Monatsübersicht
      Angenommen die Person "Alex" existiert
      Und ein Eintrag mit Datum 15.11.2026, Betrag -250,00 Euro und Kategorie "Essen" von "Alex" erfasst wird
      Und das Budget für Kategorie "Essen" im November 2026 auf 200,00 Euro gesetzt wird
      Wenn die Monatsübersicht November 2026 aufgerufen wird
      Dann warnt die Monatsübersicht bei Kategorie "Essen"
