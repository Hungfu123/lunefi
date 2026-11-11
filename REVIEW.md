# REVIEW — Overnight-Backendausbau LuneFi (2026-11-11)

## Was gebaut

1. **Persistenz auf PostgreSQL:** docker-compose.yml (postgres:17-alpine, db/user/pass `lunefi`, Named Volume), application.yml (JPA ddl-auto update), Eintrag als JPA-Entity + Spring-Data-Repository, EintragSpeicher entfernt, Controller-Logik erhalten. BDD-Tests laufen gegen Testcontainers-PostgreSQL (`@Testcontainers` + `@Container` + `@ServiceConnection` in CucumberSpringConfiguration), Testcontainers-BOM 1.21.4 explizit (Boot 4.1.2 verwaltet keine Testcontainers-Versionen mehr).
2. **Personen + G1:** POST/GET /api/personen, Eintrag mit `person`/`gemeinsam` (+ `zahler`, siehe Entscheidungen), G1-Validierung (genau eine Zuordnung, sonst 400), GET /api/eintraege mit Filtern `monat`/`person`/`gemeinsam`. Bestehende 6 Szenarien um `von "Alex"` erweitert (freigegeben, als F8 in specs/eintraege.md dokumentiert).
3. **Ausgleich G2/G3/G6:** GET /api/ausgleich/{jahr}-{monat} (saldi, vorschlag, positionen mit anteilen, laeuftNoch), GET /api/ausgleich/verlauf?monate=N (ältester zuerst, ohne positionen), PUT /api/kategorien/{k}/split (HALBHALB/ANTEILE, Summe-100-Validierung), GET /api/kategorien (Kategorien durch Nutzung, Standard HALBHALB). Uhr als Clock-Bean injiziert, im Test setzbar.
4. **Sparziele G5:** POST/GET /api/sparziele, POST /api/sparziele/{id}/einzahlungen, `erreicht` bei Summe ≥ Ziel.
5. **Budgets F2:** PUT /api/budgets, in Monatsübersicht integriert (`budgets` mit `limit`/`ist`/`warnung`, Warnung bei ist > limit).
6. **R4/F6:** Ungültiger Monat (2026-13) → 400 statt 500 (Monatsübersicht und Ausgleich).

## Wie verifiziert

Jeder BDD-Zyklus: Feature aus bestätigtem Beispiel → Lauf ROT (undefined) → Steps binden → Lauf ROT am Verhalten → minimal implementieren → GRÜN → Mutation (Erwartung verbiegen → ROT mit echter Ausgabe → zurückbauen → GRÜN). Echte Ausgaben in /tmp/opencode/build-lunefi-be-*.txt.

**Final: `mvn test` → Tests run: 23, Failures: 0, Errors: 0, BUILD SUCCESS** (6 Bestands-Szenarien + 17 neue).

Smoke-Test: Backend gegen docker-compose-PostgreSQL gestartet, POST/GET /api/personen → 201/200, Monatsübersicht Monat 13 → 400. App danach gestoppt, DB-Container läuft weiter.

## Review-Findings / offene Risiken

- **`zahler`-Feld ist eine Vertrags-Erweiterung** (API.md führt es nur im Ausgleich): gemeinsam-Einträge brauchen den Zahler, sonst ist G2 nicht berechenbar. Mit Frontend abstimmen — API.md habe ich nicht geändert (frontend/ unangetastet).
- Ohne eigenes Szenario (bewusst, Review statt Test): Split-Anteile ≤ 0 → 400, Einzahlung auf unbekanntes Sparziel → 404, Verlauf-Default `monate=6`, ungültiger Monat im Eintrag-Filter → 400, ungültiges `monat`/leere Kategorie in PUT /api/budgets → 400.
- Gemeinsame Einnahmen (positiv + gemeinsam) drehen die Saldo-Wirkung (Erstattungs-Logik) — Verallgemeinerung über die Spec hinaus.
- Rounding: Anteile werden auf 2 Stellen gerundet, Rest bekommt der größte Anteil — kein Szenario mit krummen Werten vorhanden.
- Parallel-Ausführung der Tests nicht geplant (bekannter offener Punkt), @SelectClasspathResource-Warnung kosmetisch (bekannt).
- F7 (0,00 = Einnahme) ist unverändert korrekt (`signum() < 0` → Ausgaben) — nur dokumentiert.

## Getroffene autonome Entscheidungen

Siehe specs/gemeinsam.md Abschnitt „Ergänzende Entscheidungen" und Chat-Report.
