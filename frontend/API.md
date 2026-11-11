# LuneFi — API-Vertrag (Backend ↔ Frontend)

Verbindlich für beide Seiten. Änderungen nur gemeinsam mit Specs (`specs/eintraege.md`, `specs/gemeinsam.md`). Währung immer Euro, Beträge als JSON-Zahlen. Datums-Format im JSON immer ISO (`2026-11-15`), Monate als `2026-11`.

## Personen (Onboarding)

- `POST /api/personen` Body `{"name": "Alex"}` → `201 {"id": 1, "name": "Alex"}`; Name leer → `400`
- `GET /api/personen` → `[{"id": 1, "name": "Alex"}, {"id": 2, "name": "Kim"}]`

## Einträge

- `POST /api/eintraege` Body `{"datum": "2026-11-15", "betrag": -12.50, "kategorie": "Essen", "person": "Alex", "gemeinsam": false}`
  - Persönlicher Eintrag: `person` (Name) gesetzt, `gemeinsam` false/fehlt
  - Gemeinsamer Eintrag: `gemeinsam: true` **plus `zahler`** (Name der Person, die bezahlt hat) — ohne `zahler` → `400`
  - `person` gesetzt UND `gemeinsam: true` zusammen → `400` (Regel G1: genau eines)
  - `datum`, `betrag`, `kategorie` Pflicht, sonst `400` (Regel R1)
  - → `201` mit dem gespeicherten Eintrag (inkl. `id`, Felder: `id, datum, betrag, kategorie, person, gemeinsam, zahler`)
- `GET /api/eintraege?monat=2026-11` (Filter optional: `monat`, `person`, `gemeinsam`) → Liste

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
- Ungültiger Monat (z. B. `2026-13`) → `400`, kein Serverfehler (Regel R4)
- Betrag 0,00 zählt als Einnahme (F7)

## Ausgleich (Paar, Regel G2/G3/G6)

- `GET /api/ausgleich/{jahr}-{monat}` →
  ```json
  {
    "monat": "2026-11",
    "laeuftNoch": true,
    "saldi": {"Alex": 15.00, "Kim": -15.00},
    "vorschlag": "Kim schuldet Alex 15,00 €",
    "positionen": [{"id": 1, "kategorie": "Essen", "betrag": -60.00, "zahler": "Alex", "anteile": {"Alex": 30.00, "Kim": 30.00}}]
  }
  ```
  - Nur `gemeinsam`-Einträge des Monats zählen in den Ausgleich.
  - `saldi`: + = hat mehr verauslagt als geschuldet (bekommt Geld), − = schuldet.
  - `laeuftNoch`: true, wenn der Monat der aktuelle Kalendermonat ist.
  - `vorschlag`: null, wenn Saldo-Unterschied 0,00 €.
- `GET /api/ausgleich/verlauf?monate=6` → Liste der Ausgleiche der letzten N Monate (ältester zuerst), gleiche Struktur ohne `positionen`

## Split-Konfiguration je Kategorie (Regel G3)

- `PUT /api/kategorien/{kategorie}/split` Body `{"art": "HALBHALB"}` oder `{"art": "ANTEILE", "anteile": {"Alex": 60, "Kim": 40}}` (Prozent, Summe 100) → `200` mit der gespeicherten Konfiguration
- `GET /api/kategorien` → `[{"name": "Essen", "split": {"art": "HALBHALB", "anteile": null}}]` (Kategorien entstehen durch Nutzung; Standard: HALBHALB)
- Ungültige Anteile (Summe ≠ 100, leere Person, unbekannte Art) → `400`

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
