# LuneFi — API-Vertrag (Backend ↔ Frontend)

Verbindlich für beide Seiten. Änderungen nur gemeinsam mit Specs (`specs/eintraege.md`, `specs/gemeinsam.md`, `specs/auth.md`). Währung immer Euro, Beträge als JSON-Zahlen. Datums-Format im JSON immer ISO (`2026-11-15`), Monate als `2026-11`.

## Login & Autorisierung (`specs/auth.md`, Regeln AU1–AU6)

- **Anmeldung:** `POST /api/login` mit `Content-Type: application/x-www-form-urlencoded`, Body `username=<Name>&password=<Passwort>` → `200` + Session-Cookie (`JSESSIONID`). Falsche Zugangsdaten → `401`.
- **Abmeldung:** `POST /api/logout` (CSRF-Token nötig) → `200`, Session ist danach ungültig.
- **Ungemeldet:** jeder API-Aufruf (außer `POST /api/login` und die offene Ersteinrichtung, s. u.) → `401` (kein Redirect).
- **CSRF:** `XSRF-TOKEN`-Cookie (nicht HttpOnly) wird mit jeder Antwort gesetzt/erneuert. Das Frontend liest den Cookie-Wert und schickt ihn bei allen mutating Requests (POST/PUT/DELETE) als Header `X-XSRF-TOKEN` mit — fehlt/ist ungültig → `403`. Wichtig: nach dem Login löscht der Server das alte Token-Cookie; das Frontend sollte nach Login/Logout das Token einmal per GET neu holen.
- **Autorisierung (server-seitig erzwungen):**
  - `person`-Parameter nur mit dem eigenen Namen: `GET /api/eintraege?person=…`, `GET /api/monatsuebersicht/…?person=…`, `GET /api/verlauf?person=…`, `GET /api/prognose?person=…`, `GET /api/konten?person=…` — fremder Name → `403`. Ohne Parameter: unverändertes Verhalten.
  - `POST /api/eintraege` mit `person`: nur der eigene Name → fremder → `403`. `PUT/DELETE /api/eintraege/{id}`: persönliche Einträge nur vom Eigentümer (fremd → `403`), gemeinsame Einträge dürfen beide bearbeiten.
  - `POST /api/konten`: nur `person` = eigener Name (fremd → `403`).
  - Paar-Raum offen für beide: `gemeinsam=true`-Sichten, Ausgleich (alle Endpunkte), Budgets, Sparziele, Kategorien, Coaching.
- **Passwörter:** bcrypt-gehasht in eigener `app_user`-Tabelle (username = Personenname). Die „Wer bist du"-Auswahl im Frontend entfällt — die Identität kommt aus der Session.
- **Ersteinrichtung (AU6):** solange noch kein App-Nutzer existiert, ist `POST /api/personen` ohne Anmeldung offen; danach für Unangemeldete → `401`.

## Personen (Onboarding)

- `POST /api/personen` Body `{"name": "Alex", "passwort": "geheim123"}` → `201 {"id": 1, "name": "Alex"}`; Name leer → `400`; `passwort` fehlt oder kürzer als 6 Zeichen → `400`. Existiert die Person schon → `200` (idempotent, Passwort wird gesetzt)
- `GET /api/personen` → `[{"id": 1, "name": "Alex"}, {"id": 2, "name": "Kim"}]` (angemeldet, für beide Partner sichtbar)

## Einträge

- `POST /api/eintraege` Body `{"datum": "2026-11-15", "betrag": -12.50, "kategorie": "Essen", "person": "Alex", "gemeinsam": false}`
  - Persönlicher Eintrag: `person` (Name) gesetzt, `gemeinsam` false/fehlt
  - Gemeinsamer Eintrag: `gemeinsam: true` **plus `zahler`** (Name der Person, die bezahlt hat) — ohne `zahler` → `400`
  - `person` gesetzt UND `gemeinsam: true` zusammen → `400` (Regel G1: genau eines)
  - `datum`, `betrag`, `kategorie` Pflicht, sonst `400` (Regel R1)
  - → `201` mit dem gespeicherten Eintrag (inkl. `id`, Felder: `id, datum, betrag, kategorie, person, gemeinsam, zahler`)
- `GET /api/eintraege?monat=2026-11` (Filter optional: `monat`, `person`, `gemeinsam`) → Liste
- `PUT /api/eintraege/{id}` Body wie `POST` (voller Eintrag: `datum`, `betrag`, `kategorie` + `person`/`gemeinsam`-Zuordnung, optional `zahler`, `konto`)
  - gleiche Validierungen wie `POST` (Regeln R1/G1, unbekanntes Konto) → sonst `400`
  - unbekannte `id` → `404`
  - → `200` mit dem geänderten Eintrag (Regel R5)
- `DELETE /api/eintraege/{id}` → `204`, danach ist der Eintrag weg (Liste, Monatsübersicht, Kontostand/Salden rechnen sich neu); unbekannte `id` → `404` (Regel R6)

## Monatsübersicht

- `GET /api/monatsuebersicht/{jahr}-{monat}` →
  ```json
  {
    "monat": "2026-11",
    "ausgabenJeKategorie": {"Essen": 12.50},
    "einnahmenJeKategorie": {"Gehalt": 2000.00},
    "eintraege": [{"id": 1, "datum": "2026-11-01", "betrag": -950.00, "kategorie": "Miete", "person": "Alex", "gemeinsam": false}],
    "budgets": {"Essen": {"limit": 200.00, "ist": 12.50, "warnung": false}}
  }
  ```
- `GET /api/monatsuebersicht/{jahr}-{monat}?person=Alex` → Person-Sicht (Regel R7): gleiche Struktur, aber nur persönliche Einträge dieser Person (`person == "Alex"`)
  - Gemeinsame Einträge (`person: null, gemeinsam: true`) sind NICHT Teil der Person-Sicht — sie bleiben in der Paar-Ansicht (ohne Filter) und im Ausgleich.
  - `budgets`: Limits bleiben Paar-weit, `ist` wird aus den gefilterten Ausgaben berechnet.
- `GET /api/monatsuebersicht/{jahr}-{monat}?gemeinsam=true` → Paar-Sicht (Regel R9): gleiche Struktur, aber nur Einträge mit `gemeinsam: true` (unabhängig vom Zahler). Saldi und persönliche Einträge (inkl. Einnahmen) sind privat.
  - `budgets`: Limits bleiben Paar-weit, `ist` wird aus den gemeinsamen Ausgaben berechnet.
- `person` und `gemeinsam` zusammen in einer Anfrage → `400` (schlicht abgelehnt, keine Ausnahme-Logik).
- Ungültiger Monat (z. B. `2026-13`) → `400`, kein Serverfehler (Regel R4)
- Betrag 0,00 zählt als Einnahme (F7)

## Monatsverlauf (Regel R8)

- `GET /api/verlauf?monate=3[&person=Alex]` → Liste der letzten N Monate, ältester zuerst:
  ```json
  [{"monat": "2026-09", "einnahmen": 2000.00, "ausgaben": 42.50, "differenz": 1957.50}]
  ```
  - `differenz` = `einnahmen` − `ausgaben` (Beträge absolut, Differenz positiv = Plus-Monat).
  - Monate = die letzten N vollen Monate rückwärts vom AKTUELLEN Monat (Server-Uhr).
  - `monate`: default 3; Werte über 24 werden auf 24 gecappt; Werte unter 1 → `400`.
  - Mit `person` analog zur Person-Sicht (R7) gefiltert — gemeinsame Einträge zählen nicht; ohne = alle Einträge.
  - Mit `gemeinsam=true` analog zur Paar-Sicht (R9) gefiltert — nur gemeinsame Einträge zählen, unabhängig vom Zahler. `person` und `gemeinsam` zusammen → `400`.

## Monatsprognose (PR1–PR4, `specs/prognose.md`)

- `GET /api/prognose?monate=3[&person=Alex | gemeinsam=true]` →
  ```json
  {"monat": "2026-12", "ausgabenJeKategorie": {"Miete": 950.00, "Essen": 180.00}, "einnahmenJeKategorie": {"Gehalt": 2000.00}, "differenz": 870.00, "konfidenz": "OK", "basisMonate": ["2026-08", "2026-09", "2026-10"]}
  ```
  - `monat` = Folgemonat des AKTUELLEN Monats (Server-Uhr); `basisMonate` = die N vollen Monate **vor** dem aktuellen Monat, ältester zuerst.
  - `differenz` = Summe Einnahmen − Summe Ausgaben.
  - `monate`: default 3; unter 2 → `400`; über 24 → auf 24 gecappt (wie Verlauf).
  - Sicht wie R7/R9: `person` nur persönliche Einträge, `gemeinsam=true` nur gemeinsame; beide zusammen → `400`.
  - **PR1 Fixe Kosten zählen immer:** fixe Kategorien (Miete, Strom, Internet, Telefon, Versicherung, Abo — Backend-Konstante) werden mit ihrem letzten bekannten Monatswert übernommen (jüngster Basismonat mit Ausgaben in der Kategorie).
  - **PR2 Variable Ausgaben = Median mit Ausreißer-Schutz:** alle übrigen Ausgaben-Kategorien = Median der Monatessummen über die Basismonate (Monate ohne Eintrag zählen als 0,00 — Einmalkäufe verschwinden so). Median bei gerader Anzahl: Mittel der beiden mittleren Werte.
  - **PR3 Nur regelmäßige Einnahmen:** Einnahme-Kategorie zählt nur, wenn sie in ≥ 2 Basismonaten vorkommt UND ihre Spannweite ≤ 50 % des größeren Werts ist; Prognosewert = Median der vorkommenden Monatswerte. Sonst fließt sie nicht ein (Erstattungen, Einmaliges).
  - **PR4 Wenig Daten = ehrlich:** unter 2 Monate mit Einträgen (in der gewählten Sicht) → `"konfidenz": "WENIG_DATEN"`, leere Kategorien-Maps, `differenz` 0,00 — keine Schätzung aus einem Monat. Sonst `"konfidenz": "OK"`.

## Ausgleich (Paar, Regel G2/G3/G6/G7/G8)

- `GET /api/ausgleich/{jahr}-{monat}` →
  ```json
  {
    "monat": "2026-11",
    "laeuftNoch": true,
    "ausgeglichen": false,
    "saldi": {"Alex": 15.00, "Kim": -15.00},
    "vorschlag": "Kim schuldet Alex 15,00 €",
    "positionen": [{"id": 1, "kategorie": "Essen", "betrag": -60.00, "zahler": "Alex", "anteile": {"Alex": 30.00, "Kim": 30.00}}]
  }
  ```
  - Nur `gemeinsam`-Einträge des Monats zählen in den Ausgleich.
  - `saldi`: + = hat mehr verauslagt als geschuldet (bekommt Geld), − = schuldet.
  - `laeuftNoch`: true, wenn der Monat der aktuelle Kalendermonat ist.
  - `ausgeglichen`: true, wenn der Monat als ausgeglichen markiert wurde (Regel G7).
  - `vorschlag`: null, wenn Saldo-Unterschied 0,00 €.
- `GET /api/ausgleich/verlauf?monate=6` → Liste der Ausgleiche der letzten N Monate (ältester zuerst), gleiche Struktur ohne `positionen`; je Monat inkl. `ausgeglichen` true/false (Regel G8)
- `POST /api/ausgleich/{jahr}-{monat}/ausgeglichen` → `200`; markiert den Monat als ausgeglichen (Regel G7)
  - Erneutes Markieren: idempotent, ebenfalls `200`, überschreibt nichts.
  - Ungültiger Monat → `400`. Rücksetzen/Löschen der Markierung gibt es (noch) nicht.

## Split-Konfiguration je Kategorie (Regel G3)

- `PUT /api/kategorien/{kategorie}/split` Body `{"art": "HALBHALB"}` oder `{"art": "ANTEILE", "anteile": {"Alex": 60, "Kim": 40}}` (Prozent, Summe 100) → `200` mit der gespeicherten Konfiguration
- `GET /api/kategorien` → `[{"name": "Essen", "split": {"art": "HALBHALB", "anteile": null}, "haltung": "FLEXIBEL"}]` (Kategorien entstehen durch Nutzung; Standard: HALBHALB, Haltung FLEXIBEL)
- Ungültige Anteile (Summe ≠ 100, leere Person, unbekannte Art) → `400`

## Spar-Coaching: Haltung je Kategorie (Regeln C1–C3, `specs/budgets.md` — Vorschläge, noch nicht bestätigt)

- `PUT /api/kategorien/{kategorie}/haltung` Body `{"haltung": "SPAREN"|"FLEXIBEL"|"BEWUSST_MEHR"}` → `200` mit `{"haltung": "<gespeicherte Haltung>"}`
  - Ungültige Haltung → `400`
  - Default für neue Kategorien: `FLEXIBEL` (auch Kategorien nur aus Einträgen, ohne konfigurierte Zeile)
  - Kategorie ohne Budget darf die Haltung trotzdem haben — sie wirkt erst, wenn ein Budget existiert
  - Haltungen sind Paar-Daten (C3): kein Person-Filter, beide Partner sehen/dasselbe; ein späterer `PUT …/split` überschreibt die Haltung nicht
- `GET /api/coaching?monat=2026-11` →
  ```json
  {
    "monat": "2026-11",
    "budgetsUebrig": {"Freizeit": 15.00},
    "empfehlungen": [{"kategorie": "Freizeit", "haltung": "BEWUSST_MEHR", "ist": 25.00, "limit": 40.00, "spielraum": 15.00, "tipp": "Freizeit verträgt 15,00 € mehr — bewusst genießen"}],
    "gutGespart": [{"kategorie": "Miete", "haltung": "SPAREN", "ist": 950.00, "limit": 950.00, "spielraum": 0.00}]
  }
  ```
  - Nur Kategorien mit Budget im Monat; `ist` = Summe aller Ausgaben der Kategorie im Monat (persönliche und gemeinsame — Paar-weit).
  - `spielraum` = max(`limit` − `ist`; 0).
  - Haltung `BEWUSST_MEHR` mit `spielraum` > 0 → Eintrag in `empfehlungen` (mit Tipp-Text; Ermutigung nur, weil der Nutzer die Haltung selbst gewählt hat).
  - Haltung `SPAREN` mit `ist` ≤ `limit` → Eintrag in `gutGespart` (Lob, ohne Tipp; `spielraum` 0,00 möglich).
  - Haltung `FLEXIBEL` → nur Eintrag in `budgetsUebrig` (Zahl, keine Wertung — Prinzip „emotional neutral").
  - Über-Limit-Kategorien (`ist` > `limit`) erscheinen in keiner der drei Listen (die F2-Budget-Warnung existiert schon).
  - Kein `person`/`gemeinsam`-Parameter (C3): Coaching ist reine Paar-Sicht, beide Partner sehen dieselben Haltungen und Tipps.
  - Ungültiger/fehlender `monat` → `400`.

## Sparziele (Regel G5)

- `POST /api/sparziele` Body `{"name": "Urlaub", "zielbetrag": 2000.00}` → `201` mit `{"id": 1, "name": "Urlaub", "zielbetrag": 2000.00, "eingezahlt": 0.00, "erreicht": false, "einzahlungen": []}`
- `GET /api/sparziele` → Liste (mit `eingezahlt`, `erreicht`, `einzahlungen: [{"id": 1, "betrag": 500.00, "person": "Alex", "datum": "2026-11-10"}]`)
- `POST /api/sparziele/{id}/einzahlungen` Body `{"betrag": 100.00, "person": "Kim", "datum": "2026-11-12"}` → `201`; `erreicht` wird true, sobald Summe ≥ Zielbetrag; Zielbetrag ≤ 0 oder Einzahlung ≤ 0 → `400`

## Budgets (F2)

- `PUT /api/budgets` Body `{"monat": "2026-11", "kategorie": "Essen", "limit": 200.00}` → `200` mit dem gespeicherten Budget; `limit` ≤ 0 → `400`
- Sichtbar in der Monatsübersicht: `budgets` mit `warnung: true`, sobald `ist > limit`

## Konten und Gesamtsaldo (K1–K3)

- `POST /api/konten` Body `{"name": "Girokonto", "startSaldo": 3450.00, "person": "Alex"}` → `201` mit `{"id":1,"name":"Girokonto","person":"Alex","startSaldo":3450.00,"saldo":3450.00}`
  - `name`/`person`/`startSaldo` Pflicht, `person` muss existieren, `name` eindeutig → sonst `400`
- `GET /api/konten?person=Alex` → Liste mit aktuellem `saldo` (= Startsaldo + Summe der zugeordneten Einträge); ohne Filter alle Konten
- Gesamtsaldo einer Person = Summe der `saldo` ihrer Konten (Frontend rechnet, kein eigener Endpoint)
- `POST /api/eintraege` akzeptiert optional `konto` (Name eines existierenden Kontos) → verändert den Kontosaldo um den Betrag; unbekanntes Konto → `400`

## Quellen und Import (Q1–Q4)

- `POST /api/quellen` Body `{"name": "Commerzbank", "art": "FinTS", "konto": "Girokonto"}` → `201` mit `{"id":1,"name":"Commerzbank","art":"FinTS","konto":"Girokonto","zustand":"offen"}`
  - `art`: `"FinTS"` (Bank via FinTS/HBCI) oder `"PSD2"` (Bank/PayPal via PSD2-Anbieter wie GoCardless)
  - `name`/`art`/`konto` Pflicht, `konto` muss existieren, `name` eindeutig → sonst `400`
  - `zustand`: `"offen"` bis die echte Verbindung hergestellt ist, danach `"verbunden"` (Wechsel folgt mit den Adapter-Spikes)
- `GET /api/quellen` → Liste aller Quellen
- `POST /api/quellen/{id}/import` Body `{"umsaetze": [{"externeKennung":"UMS-1","datum":"01.11.2026","betrag":"+2.000,00","verwendungszweck":"Gehalt November"}]}` → `200` mit `{"neu":2,"uebersprungen":0}`
  - Erzeugt je Umsatz genau einen Eintrag: Datum, Betrag (deutsches Format), Person = Konto-Inhaber, Konto verknüpft
  - `betrag` deutsches Format mit Vorzeichen: `"+2.000,00"`, `"-42,50"`
  - `datum` als `dd.MM.uuuu`
  - Kategorie: gelernte Lernregel (treffer in Verwendungszweck) oder `"Import"`
  - Re-Import-Schutz: gleiche `externeKennung` je Quelle wird übersprungen (nicht doppelt)
  - Unbekannte Quellen-ID → `404`
- `POST /api/lernregeln` Body `{"kennung": "REWE", "kategorie": "Essen"}` → `201`
  - Import prüft, ob die `kennung` (Groß-/Kleinschreibung egal) im Verwendungszweck vorkommt → erste Trefferregel gewinnt

## Betrieb

- Backend: Port 8080, `docker compose up -d` startet PostgreSQL (db/user/pass: `lunefi`), dann Backend starten
- Fehlerkörper: leeres JSON mit `400` (keine Pflicht am Body)

## Nachrüst-Login & Identität (`specs/auth.md`, Regeln AU7–AU8)

- **Passwörter nachrüsten:** `POST /api/ersteinrichtung` (ungemeldet erlaubt, CSRF-Pflicht wie überall) Body `{"passwoerter": {"Alex": "geheim123", "Kim": "geheim456"}}` → `200`
  - Nur solange NOCH KEIN App-Nutzer existiert; existiert bereits einer → `403` (gleiche Schloss-Semantik wie die offene Ersteinrichtung, AU6)
  - Jeder Name muss eine existierende Person sein, jedes Passwort mindestens 6 Zeichen → sonst `400` (komplett abgewiesen, es wird nichts angelegt)
  - Legt für jede genannte Person einen App-User mit bcrypt-Hash an; danach ist die App „eingerichtet" (AU6 schließt die offene Ersteinrichtung, AU7 selbst bleibt danach ebenfalls zu)
- **Wer bin ich:** `GET /api/wer` → `200 {"name": "Alex"}` — der Name des ANGEMELDETEN Nutzers; ungemeldet → `401`
  - Das Frontend leitet die Person hiervon ab (ersetzt die bisherige localStorage-Auswahl: der Angemeldete IST die Person)
