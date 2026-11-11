# language: de
Funktionalität: Gemeinsamer Paar-Raum
  Als Partner
  möchten wir gemeinsame Ausgaben, den Ausgleich, Budgets, Sparziele, Kategorien und Coaching beide nutzen
  damit alles Gemeinsame für beide offen bleibt — nur Persönliches ist geschützt

  Regel: Der Paar-Raum ist für beide Partner offen

    @AU3
    Szenario: Paar-Sicht ist für beide offen
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn "Kim" die Monatsübersicht November 2026 in der Paar-Sicht aufruft
      Dann zeigt die Monatsübersicht die Ausgaben je Kategorie
        | Kategorie | Ausgaben |
        | Essen     | 60,00    |

    @AU3
    Szenario: Ausgleich ist für beide offen
      Angenommen die Personen "Alex" und "Kim" existieren
      Und "Alex" eine gemeinsame Ausgabe mit -60,00 Euro und Kategorie "Essen" erfasst
      Wenn "Kim" den Ausgleich November 2026 als ausgeglichen markiert
      Dann zeigt der Ausgleich November 2026 den Monat als ausgeglichen
