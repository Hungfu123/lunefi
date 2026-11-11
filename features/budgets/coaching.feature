# language: de
Funktionalität: Spar-Coaching
  Als wir
  möchte ich je Kategorie einstellen, ob wir dort sparen, neutral bleiben oder bewusst mehr ausgeben dürfen
  damit die App beim Sparen ermutigt statt nur warnt — und beide Partner dieselbe Empfehlung sehen

  Regel: C1 — Jede Kategorie hat eine Haltung: SPAREN, FLEXIBEL (Standard) oder BEWUSST_MEHR; ungültige Werte werden abgelehnt
  Regel: C2 — Das Coaching empfiehlt nur bei bewusst gewählter Haltung; flexible Kategorien bleiben neutral; überzogene Kategorien erwähnt es nicht
  Regel: C3 — Haltungen sind Paar-Daten: der Coaching-Aufruf kennt keinen Personen-Filter, beide Partner sehen dieselben Tipps (dokumentiert, kein eigenes Szenario)

  @C1
  Szenario: Haltung einstellen und abfragen
    Angenommen die Person "Alex" existiert
    Und ein Eintrag mit Datum 15.11.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
    Wenn die Haltung für Kategorie "Freizeit" auf "BEWUSST_MEHR" gestellt wird
    Dann zeigt die Kategorien-Liste für "Freizeit" die Haltung "BEWUSST_MEHR"

  @C1
  Szenario: Ungültige Haltung wird abgelehnt
    Angenommen die Person "Alex" existiert
    Wenn die Haltung für Kategorie "Freizeit" auf "SPARSUCHT" gestellt werden soll
    Dann wird das Einstellen der Haltung abgelehnt

  @C1
  Szenario: Neue Kategorie hat standardmäßig die neutrale Haltung
    Angenommen die Person "Alex" existiert
    Und ein Eintrag mit Datum 15.11.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
    Dann hat die neue Kategorie "Freizeit" die Haltung "FLEXIBEL"

  @C2
  Szenario: Bewusst mehr ausgeben wird mit Spielraum empfohlen
    Angenommen die Person "Alex" existiert
    Und die Person "Kim" existiert
    Und "Kim" eine gemeinsame Ausgabe mit -25,00 Euro und Kategorie "Freizeit" erfasst
    Und das Budget für Kategorie "Freizeit" im November 2026 auf 40,00 Euro gesetzt wird
    Und die Haltung für Kategorie "Freizeit" auf "BEWUSST_MEHR" gestellt wird
    Wenn das Spar-Coaching für November 2026 aufgerufen wird
    Dann empfiehlt das Coaching bei "Freizeit" 15,00 Euro mehr auszugeben

  @C2
  Szenario: Sparen im Limit wird gelobt, flexible Haltung bleibt nur eine Zahl
    Angenommen die Person "Alex" existiert
    Und ein Eintrag mit Datum 01.11.2026, Betrag -950,00 Euro und Kategorie "Miete" von "Alex" erfasst wird
    Und ein Eintrag mit Datum 15.11.2026, Betrag -25,00 Euro und Kategorie "Freizeit" von "Alex" erfasst wird
    Und das Budget für Kategorie "Miete" im November 2026 auf 950,00 Euro gesetzt wird
    Und das Budget für Kategorie "Freizeit" im November 2026 auf 40,00 Euro gesetzt wird
    Und die Haltung für Kategorie "Miete" auf "SPAREN" gestellt wird
    Wenn das Spar-Coaching für November 2026 aufgerufen wird
    Dann lobt das Coaching das Sparen bei "Miete"
    Und zeigt das Coaching bei "Freizeit" nur den verbleibenden Rahmen von 15,00 Euro

  @C2
  Szenario: Überzogene Kategorie erwähnt das Coaching nicht
    Angenommen die Person "Alex" existiert
    Und ein Eintrag mit Datum 15.11.2026, Betrag -42,50 Euro und Kategorie "Essen" von "Alex" erfasst wird
    Und das Budget für Kategorie "Essen" im November 2026 auf 30,00 Euro gesetzt wird
    Wenn das Spar-Coaching für November 2026 aufgerufen wird
    Dann erwähnt das Coaching die Kategorie "Essen" nirgends
