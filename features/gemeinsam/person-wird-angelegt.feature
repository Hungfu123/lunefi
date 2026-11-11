# language: de
Funktionalität: Personen anlegen
  Als wir
  möchte ich beide Partner mit Namen hinterlegen
  damit jeder Eintrag eine Person oder die Gemeinschaft zugeordnet werden kann

  Regel: Jeder Eintrag gehört genau einer Person ODER ist gemeinsam

    @G1
    Szenario: Person wird angelegt
      Angenommen noch keine Person angelegt wurde
      Wenn die Person "Alex" angelegt wird
      Dann existiert genau die Person "Alex"

    @G1
    Szenario: Person ohne Name wird nicht übernommen
      Angenommen noch keine Person angelegt wurde
      Wenn die Person ohne Name angelegt werden soll
      Dann existiert keine Person
