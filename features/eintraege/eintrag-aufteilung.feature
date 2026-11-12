# language: de
Funktionalität: Einträge in Teile aufteilen
  Als ich
  möchte ich einen Einkauf auf mehrere Kategorien aufteilen
  damit ich weiß, was wofür war — ohne die Übersicht zu müllen

  Regel: Ein geteilter Eintrag bleibt eine Zeile; alle Kategorie-Rechnungen nutzen die Teile. Der Server leitet die Eintrags-Felder aus dem größten Teil ab. Teile sind rückwirkend änderbar.

    @R10
    Szenario: Aufteilung zählt je Kategorie, der Eintrag bleibt eine Zeile
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -10,00 | Geschenk  | Alex   | nein       |
      Wenn die Monatsübersicht November 2026 aufgerufen wird
      Dann zeigt die Monatsübersicht die Ausgaben je Kategorie
        | Kategorie | Ausgaben |
        | Essen     | 20,00    |
        | Geschenk  | 10,00    |
      Und listet die Monatsübersicht 1 Einträge mit Datum und Betrag
      Und zeigt die Monatsübersicht einen Eintrag mit dem Betrag -30,00 Euro

    @R10
    Szenario: Teile müssen cent-genau den Betrag treffen
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst werden soll:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -9,00  | Geschenk  | Alex   | nein       |
      Dann existiert kein Eintrag

    @R10
    Szenario: Ein Teil gehört entweder einer Person oder dem Paar
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst werden soll:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     | Alex   | ja         |
        | -10,00 | Geschenk  |        | nein       |
      Dann existiert kein Eintrag

    @R10
    Szenario: Ein gemeinsamer Teil braucht einen Zahler
      Angenommen die Personen "Alex" und "Kim" existieren
      Wenn ein Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen ohne Zahler erfasst wird:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -10,00 | Geschenk  | Alex   | nein       |
      Dann existiert kein Eintrag

    @R10
    Szenario: Budgets zählen nur den Teil seiner Kategorie
      Angenommen die Personen "Alex" und "Kim" existieren
      Und das Budget für Kategorie "Essen" im November 2026 auf 25,00 Euro gesetzt wird
      Und "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -10,00 | Geschenk  | Alex   | nein       |
      Wenn die Monatsübersicht November 2026 aufgerufen wird
      Dann ist das Ist-Budget für "Essen" 20,00 Euro

    @R10
    Szenario: Der Ausgleich führt nur die gemeinsamen Teile
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -10,00 | Geschenk  | Alex   | nein       |
      Wenn der Ausgleich November 2026 aufgerufen wird
      Dann schuldet Kim Alex 10,00 Euro
      Und führt der Ausgleich genau eine Position mit der Kategorie "Essen" und dem Betrag -20,00 Euro aus

    @R10
    Szenario: Die Person-Sicht zeigt nur den sichtbaren Teilbetrag
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -10,00 | Geschenk  | Alex   | nein       |
      Wenn die Monatsübersicht November 2026 für Person "Alex" aufgerufen wird
      Dann listet die Monatsübersicht 1 Einträge mit Datum und Betrag
      Und zeigt die Monatsübersicht einen Eintrag mit dem Betrag -10,00 Euro
      Und zeigt die Monatsübersicht die Ausgaben je Kategorie
        | Kategorie | Ausgaben |
        | Geschenk  | 10,00    |

    @R10
    Szenario: Die Paar-Sicht zeigt nur den gemeinsamen Teilbetrag
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -10,00 | Geschenk  | Alex   | nein       |
      Wenn die Monatsübersicht November 2026 in der Paar-Sicht aufgerufen wird
      Dann listet die Monatsübersicht 1 Einträge mit Datum und Betrag
      Und zeigt die Monatsübersicht einen Eintrag mit dem Betrag -20,00 Euro
      Und zeigt die Monatsübersicht die Ausgaben je Kategorie
        | Kategorie | Ausgaben |
        | Essen     | 20,00    |

    @R10
    Szenario: Teile lassen sich rückwirkend ändern
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -10,00 | Geschenk  | Alex   | nein       |
      Wenn "Alex" die Teile des Eintrags vom 12.11.2026 ändert zu:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -5,00  | Geschenk  | Alex   | nein       |
        | -5,00  | Freizeit  | Alex   | nein       |
      Dann zeigt die Monatsübersicht November 2026 bei "Geschenk" Ausgaben von 5,00 Euro
      Und zeigt die Monatsübersicht November 2026 bei "Freizeit" Ausgaben von 5,00 Euro

    @R10
    Szenario: Statistik und Verlauf zählen nur die Teile ihrer Kategorie
      Angenommen der aktuelle Monat November 2026 ist
      Und die Personen "Alex" und "Kim" existieren
      Und "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -30,00 Euro und Teilen erfasst:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -10,00 | Geschenk  | Alex   | nein       |
      Wenn der Kategorie-Verlauf der letzten 1 Monate für "Essen" abgefragt wird
      Dann enthält der Kategorie-Verlauf die Monate
        | Monat   | Betrag |
        | 2026-11 | 20,00  |

    @R10
    Szenario: Der Ausgleich führt jeden gemeinsamen Teil als eigene Position
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" einen Eintrag mit Datum 12.11.2026, Betrag -40,00 Euro und Teilen erfasst:
        | Betrag | Kategorie | Person | Gemeinsam |
        | -20,00 | Essen     |        | ja         |
        | -15,00 | Freizeit  |        | ja         |
        | -5,00  | Geschenk  | Alex   | nein       |
      Wenn der Ausgleich November 2026 aufgerufen wird
      Dann schuldet Kim Alex 17,50 Euro
      Und führt der Ausgleich 2 Positionen aus
