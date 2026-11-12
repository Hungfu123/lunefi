# LuneFi

Finanzen für zwei. LuneFi ist eine private App für ein Paar. Jeder sieht sein eigenes Geld. Gemeinsame Ausgaben werden geteilt. Am Monatsende sagt die App, wer wem wie viel schuldet.

## So sieht die App aus

Alles ist fürs Handy gebaut. Jede Seite passt ohne Scrollen auf einen Bildschirm.

| Übersicht | Eintrag erfassen | Ausgleich |
|---|---|---|
| ![Übersicht](docs/screenshots/01-uebersicht.png) | ![Erfassen](docs/screenshots/02-eintrag-erfassen.png) | ![Ausgleich](docs/screenshots/03-ausgleich.png) |

| Statistik Monat | Statistik Jahr | |
|---|---|---|
| ![Statistik Monat](docs/screenshots/04-statistik-monat.png) | ![Statistik Jahr](docs/screenshots/05-statistik-jahr.png) | |

- **Übersicht**: Saldo, Budget, Verlauf und Buchungen auf einen Blick.
- **Eintrag erfassen**: selbst erfasst bleibt eigenständig. Kommt der Umsatz später von der Bank, wird er nur bestätigt, nicht doppelt gezählt.
- **Ausgleich**: wer wem wie viel schuldet, monatlich auf einen Tipp.
- **Statistik**: einmal der Monat im Kreis, einmal das ganze Jahr mit allen Kategorien.

Anmelden tut man sich mit Google, ein Tipp genügt.

## Was die App kann

- Übersicht mit Saldo, Budget, Verlauf und Buchungen
- Manuelle Einträge, auch in mehrere Teile aufgeteilt
- Gemeinsame Ausgaben mit Anteilen und Zahler
- Monatlicher Ausgleich mit einem Tippen
- Sparziele mit Fortschritt
- Statistik für den Monat und für das Jahr
- Budgets je Kategorie mit Warnung
- Bankimport vorbereitet: Quellen, Zuordnung, keine Doppelten

## Wie sie gebaut ist

- Backend: Java mit Spring Boot und PostgreSQL
- Frontend: React mit TypeScript und Tailwind
- Alles ist testgetrieben über BDD

## Alles läuft über BDD

Jede Funktion beginnt als Szenario in deutscher Alltagssprache. Die Szenarien stehen in Gherkin im Ordner `features/`. Zuerst laufen sie rot. Dann wird so lange gebaut, bis sie grün sind. Danach verändert ein gezielter Eingriff in den Code das Ergebnis. Wenn ein Szenario dadurch rot wird, beweist das, dass es wirklich prüft. Das nennt sich Mutationstest.

Deutsch bleiben die Szenarien, die Oberfläche und die Schnittstellen nach außen. Der Code selbst ist englisch.

## Der Agenten-Workflow

Die App entsteht mit KI-Agenten nach einem festen Protokoll. Das Protokoll steht in `PROMPT.md` und gilt für jeden Agenten:

1. Spec und Szenarien zuerst, Code später.
2. Kleine Schritte, immer kompilierbar, immer rot vor grün.
3. Verifizieren mit echten Testläufen, nie mit Behauptungen.
4. Review über drei Achsen: Konventionen, Anforderung, Korrektheit.
5. Ergebnis in `REVIEW.md` festhalten.
6. Committen nur nach Anweisung.

Deshalb entspricht der Code im Repo immer den Szenarien im Repo.

## Lokal starten

```bash
docker compose up -d
LUNEFI_DEMO_SEED=true mvn spring-boot:run
cd frontend && yarn && yarn dev
```

Die Demo-Umgebung füllt die Datenbank mit Beispieldaten und legt die Testnutzer an. Ohne `LUNEFI_DEMO_SEED` startet die App leer und fragt beim ersten Aufruf nach den echten Nutzern.

## Struktur

```
specs/        Anforderungen in kurzen Regeln, für Menschen lesbar
features/     Gherkin-Szenarien, deutsch, eine Datei je Thema
src/          Backend (Spring Boot)
frontend/     Frontend (React, TypeScript)
steps/        Schritt-Definitionen für die Szenarien
docs/         Recherche und Screenshots
PROMPT.md     Das Protokoll für alle Agenten
```
