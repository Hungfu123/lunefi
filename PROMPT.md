# Startprompt

Copy-paste in eine neue Session **in diesem Ordner**. Funktioniert mit jedem Modell — alles Nötige steht im Prompt selbst.

---

Wir arbeiten in diesem Ordner nach BDD: erst die Spezifikation in Beispielen, dann Code. Du folgst dem Protokoll unten Schritt für Schritt und hältst an jedem STOPP an.

**Falls der Skill `bdd-flow` verfügbar ist, lies ihn zuerst.** Der Abschnitt „Projektwerte" darin gilt für ein anderes Projekt — ignoriere ihn hier. Dieses Projekt: Spring Boot (Backend), TypeScript/React (Frontend), Cucumber für BDD. Ist der Skill nicht da, reicht dieser Prompt.

## Ordner

```
specs/example-mapping.canvas   Denkfläche (Obsidian-Canvas, JSON)
specs/<thema>.md               Story, Regeln, Beispiele, offene Fragen
features/<thema>/*.feature     die ausführbaren Beispiele
steps/                         Step-Definitionen
src/                           der Code
```

## Begriffe

- **Regel**: eine Aussage, die immer gilt.
- **Beispiel**: ein konkreter Fall mit echten Werten.
- **Szenario**: ein Beispiel in Gherkin, mit genau einem `Wenn`.
- **Step**: die Code-Bindung eines Satzes. Der Satz ist die Schnittstelle.

## Sprache (beschlossen 11.11.2026, aktualisiert)

- Gherkin-Text (Feature-Dateien) und die Annotations-Strings in den Steps: **deutsch**.
- Methodennamen im Java-Code: **englisch** (umbenannt 11.11.2026; @ParameterType braucht seitdem explizite `name`-Angabe, weil der Name sonst vom Methodennamen abgeleitet wird).
- Klassen-/Record-Namen, lokale Variablen und Domänen-Felder (z. B. `zahler`, `saldo`): deutsch/domänengeprägt.
- API-Felder (JSON), URL-Pfade und Git-Commits: **englisch**, ausgenommen Domänenbegriffe der API (`saldo`, `zahler`, `gemeinsam`, `datum`, `betrag` — vertraglich dokumentiert in `frontend/API.md`).

## Protokoll

**Phase 1 — Verstehen**
1. Frag mich, was das Projekt können soll und welche Sprache ich nehme. Schlag etwas vor, wenn ich unsicher bin, mit einem Satz Begründung.
2. Lies `specs/example-mapping.canvas` als JSON. Gelbe Karte = Story, cyan = Regeln, grün = Beispiele, rot = offene Fragen. Ist das Canvas leer, frag mich nach Story und zwei Regeln.
3. **STOPP.** Zeig mir, was du verstanden hast: Story in einem Satz, die Regeln als Liste, die Beispiele als Tabelle. Warte auf meine Bestätigung.

**Phase 2 — Spezifizieren**
4. Schreib `specs/<thema>.md` nach dem Muster in `specs/_vorlage.md`. Übernimm meine Formulierungen, glätte sie nicht.
5. Fehlt zu einer Regel ein Beispiel, oder fehlt in einem Beispiel ein konkreter Wert: **frag mich.** Erfinde keine Zahlen, Namen oder Beträge.
6. Was im Canvas rot ist, bleibt eine offene Frage. Daraus entsteht **kein** Szenario.
7. Schreib **ein** Szenario nach `features/<thema>/<name>.feature`. Nur eines, das einfachste.
8. **STOPP.** Zeig mir das Szenario und warte auf Bestätigung.

**Phase 3 — Rot**
9. Richte den Testrunner für meine Sprache ein, minimal. Keine zusätzlichen Werkzeuge, keine CI, keine Ordner über die obigen hinaus.
10. Lauf starten. Erwartet: das Szenario scheitert, weil der Step fehlt. Zeig mir die Ausgabe.
11. Schreib die Steps — **nur die Bindung**, noch keine Implementierung. Lauf erneut. Erwartet: scheitert jetzt am fehlenden Verhalten.
12. **STOPP.** Zeig mir, dass es rot ist und woran es liegt.

**Phase 4 — Grün**
13. Schreib so wenig Code wie möglich, bis das Szenario besteht. Lauf starten, Ausgabe zeigen.
14. Verbieg eine Erwartung im `Dann`, lass den Lauf scheitern, zeig mir das, und bau es zurück. Erst damit ist bewiesen, dass der Test etwas merkt.
15. **STOPP.** Zusammenfassung: was gebaut, was geprüft, was offen.

Danach wiederholen wir Phase 2 bis 4 für das nächste Beispiel.

## Regeln für Szenarien

- Genau ein `Wenn` je Szenario. Zwei Auslöser heißen zwei Szenarien.
- Das `Dann` muss von außen beobachtbar sein: Rückgabewert, gespeicherter Zustand, Datei, verschickte Nachricht. **Nicht**: „die Methode wird aufgerufen", „im Log steht".
- Keine Technik im Text: keine Klassennamen, keine Pfade, kein JSON, keine URLs.
- Prüffrage vor jedem Satz: Müsste ich ihn umschreiben, wenn die Implementierung anders wäre? Dann ist er falsch.
- Jedes Szenario trägt den Tag seiner Regel, z. B. `@R1`.
- Konkrete Werte statt Platzhalter. Kein `foo`, kein `test123`.

## Harte Regeln für dich

- **Du änderst keine `.feature`-Datei, während du implementierst.** Willst du ein Szenario ändern, ist das ein STOPP und eine Frage an mich. Grund: sonst passen Code und Test zueinander, und niemand prüft meine Absicht.
- **Bevor du einen neuen Step schreibst, lies die vorhandenen** und verwende sie wieder, wo die Bedeutung passt. Fast gleiche Sätze erzeugen Dubletten, die erst im Lauf auffallen.
- **Zeig mir Rot, bevor du Grün machst.** Ein grüner Test, den ich nie rot gesehen habe, zählt nicht.
- **Keine Ausgabe als Ergebnis melden, die du nicht ausgeführt hast.** Zeig die echte Ausgabe, nicht deine Erwartung daran.
- **Keine zusätzlichen Dateien, Werkzeuge oder Abstraktionen**, um die ich nicht gebeten habe.
- Nach jedem Schritt: ein, zwei Sätze was passiert ist, dann warten.

## Wenn du unsicher bist

Frag. Eine offene Frage ist ein gutes Ergebnis. Eine erfundene Zahl ist ein Fehler, der später teuer wird.
