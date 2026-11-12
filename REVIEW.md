# REVIEW — UX-Smart-Loop + QF6 + Jahresübersicht (12.11.2026)

## Was gebaut wurde

### 1. QF6/QF7: PayPal + Bank zählen nur einmal (Backend, GREEN)
- `SourceController.importEntries`: Zuordnung bestehender Einträge via `uniqueCounterpart` (Betrag exakt inkl. Richtung, Datum ±3 Tage, andere Quelle, genau 1 Treffer → verknüpfen, sonst neuer Eintrag). `ImportResult(neu, uebersprungen, zugeordnet)`.
- `features/quellen/paypal-und-bank-zahlen-einmal.feature` (4 Szenarien), `SourcesSteps`: Zähl-Steps („genau ein/zwei/drei Einträge").
- Fix des RED-Fehlers: `{int}` matcht keine Zahlwörter → explizite Steps für „zwei"/„drei".
- **Beweis:** `mvn test` = 126 Tests, 0 Failures (`/tmp/opencode/build-qf6-green.txt`). **Mutation:** Jahresfilter-Pendant separat; QF6-Matching-Mutationen stehen aus (siehe offene Punkte).

### 2. Jahresübersicht (Statistik, neues Feature)
- Backend `GET /api/statistik/jahr?jahr=&person=&gemeinsam=`: 12 Monate (HistoryMonth: einnahmen/ausgaben/differenz) + Kategorie-Summen des Jahres (`YearStatistics`).
- `StatisticsService.yearOverview`, `StatisticsController.year`.
- Frontend `StatisticsPage`: [Monat|Jahr]-Toggle, Jahres-KPIs (Einnahmen/Ausgaben/Übrig + Ø/Monat + stärkster Monat), 12-Monats-Chart (Einnahmen gut ↑, Ausgaben schlecht ↓ um Mittellinie, aktueller Monat voll), „Ausgaben nach Kategorie" mit Balken, Einnahmen-Liste.
- **Beweis:** ROT 2 UndefinedSteps (`build-jahr-red.txt`) → GRÜN 126 Tests 0 Fehler (`build-jahr-green.txt`). **Mutation:** Monats-Vergleich ohne Jahr → „zählt nur das angefragte Jahr" schlägt fehl (1 Failure, `build-jahr-mutation.txt`), danach zurückgespielt.

### 3. Konto-Anlage mit Bank-Auswahl (Settings)
- Inline-Felder ersetzt durch „Konto anlegen"-Button → Sheet „Bei welcher Bank?" (14 kuratierte Banken + „Andere Bank"), lange Namen dynamisch über volle Breite (col-span-2). Nach Auswahl: Name (vorausgefüllt) + Startsaldo.
- Recherche belegt: Enable Banking `GET /aspsps` liefert die echte Bankliste inkl. Logo-URLs (resizebar) mit App-JWT — sobald Keys existieren, kann die Liste live geladen werden. Heutige Liste ist bewusst kuratiert (Fallback), Hinweis im Sheet: „Die Bankverbindung kommt später dazu — mit dem Bank-Import."
- Fehler-UX gefixt: Feedback erscheint jetzt AM ORT der Aktion (vorher ganz unten, unsichtbar). Duplikat-Vorab-Prüfung mit Klartext („Ein Konto namens „X" gibt es schon."), Felder-Fehler direkt im Sheet.

### 4. Sprach-/Verständlichkeits-Fixes (Laien-UX, User-Runde)
- EntrySheet: Titel „Manuell erfassen", V-Form-Illustration (du jetzt + Bank später → 1 Eintrag), Laiensatz „…wird deine Zeile nur bestätigt — nicht doppelt gezählt."
- „Person" → „Für wen?" (privat), „Bezahlt von" bleibt (gemeinsam).
- Gemeinsam-Zeile konkret: „Je zur Hälfte für dich und Kim" → mit Betrag+Zahler: „Kim schuldet dir 15,00 €" / „Du schuldest Kim 15,00 €". Doppelter Schuld-Hinweis unten entfernt (nur noch im Split-Modus).
- Settings: „Haltung" → „Einstellung" + Halbsätze (ATTITUDE_HINT), „Anteile" → „Eigene Aufteilung", 50/50-Pille nur noch bei abweichender Aufteilung (Platz-Eng-Fix).
- Ausgleich: Positionen nach Kategorie gruppiert („Essen · 3 Zahlungen · 65,50 €", aufklappbar), Button „Alles beglichen" + Bestätigungs-Sheet („…danach ist November erledigt." / „Vermerkt nur — es wird kein Geld bewegt."), Hilfe-Modal angepasst.
- Budget-Zeilen (Übersicht + Kategorie-Sheet): „58,63 € ausgegeben · 28,63 € drüber".
- Sparziele: unverändert (Prognose offen, siehe Fragen).

### 5. Situativer Erfassungs-Prompt (Übersicht)
- Abends (≥17 Uhr) ohne heutige Buchung: Karte „Heute noch nichts erfasst — Einkauf erledigt?" + „Jetzt erfassen" (öffnet Sheet) + X (merkt pro Tag in localStorage).
- Verifiziert via Stunde-Override + Löschung meiner zwei R10-Testeinträge vom 12.11. (IDs 63/64, eigene QA-Artefakte).

## Wie verifiziert
- Backend: `mvn test` 126/126 grün; ROT→GRÜN→Mutation für Jahresübersicht belegt.
- Frontend: `yarn typecheck` + `yarn build` grün.
- Playwright-DOM-Loop: alle 5 Seiten scroll-frei bei 844 px; Bank-Sheet (volSpan 350 px), Sparkasse-Anlegen + Inline-Feedback + Duplikat-Hinweis, Ausgleich-Gruppierung + Confirm-Sheet, Statistik-Jahr-Ansicht (KPIs/Chart/Kategorien, scroll-frei), EntrySheet (V-Form, „Du schuldest Kim 15,00 €", „Für wen?"), Kategorie-Sheet (Budget-Zeile + Einstellungs-Halbsatz), Prompt-Karte.
- Screenshots: `lunefi-ux-statistik-jahr.png`, `lunefi-ux-ausgleich-gruppiert.png`, `lunefi-ux-manuell-vform.png`, `lunefi-ux-bank-auswahl.png`, `lunefi-ux-manuell-erfassen.png`.

## Offene Punkte / Fragen an dich
1. **Notgroschen** (dein Vorschlag): Wieviel man mindestens behalten will; Sparziel-Guthaben zählt nicht als Puffer. Offen: pro Person oder für euch beide? Wo sichtbar (Übersicht unter „Mein Saldo")? → Spec + BDD + UI.
2. **Übersichts-Verlauf aufrüsten**: mehr Monate, Prognose für nächsten Monat (ForecastService vorhanden), wiederkehrende Zahlungen markieren.
3. **Kategorie-Sheet füllen** (A–E aus der UX-Runde noch offen): Budget-Balken, Tempo-Prognose, „In Kategorie erfassen"-Button.
4. **Google-Login** (beschlossen „sofort", noch nicht gebaut): GIS-Button + `/api/auth/google` + Allowlist via `.env` (deine + Annes Mail). Braucht von dir: OAuth-Client-ID in der Cloud Console (Origins: lunefi.duckdns.org + localhost:5174).
5. **QF7-Spec „Erkannte Absender"** (beschlossen: nur Spec): in specs/quellen.md dokumentieren.
6. **QF6-Mutationen** (Fenster ±3, Betrag/Richtung, Eindeutigkeit einzeln killen) noch nicht gelaufen — nachholen.
7. **Echte Bankliste**: mit Enable-Banking-Keys `/aspsps` live laden (statt kuratierter Liste) — inkl. Logos.
8. Demo-Daten: Test-Konten „Sparkonto" (500 €) und „Sparkasse" (1.200 €) von mir angelegt; R10-Testeinträge 63/64 gelöscht. Restliche QA-Reste (Import 1.328,99 € etc.) bleiben, bis du Aufräumen sagst.

## Commit-Plan (auf dein Go)
- `feat: assign matching import entries instead of duplicating (QF6/QF7)` — SourceController, SourcesSteps, paypal-und-bank feature
- `feat: year statistics endpoint + year view on statistics page` — YearStatistics, StatisticsService/Controller, jahresuebersicht.feature, StatisticsSteps, StatisticsPage, types
- `feat: bank picker sheet for account creation with inline feedback` — SettingsPage
- `refactor: plain-language UX fixes across entry, settings, settlement and overview` — EntrySheet, AttitudeSelector, CategorySheet, SettlementPage, Overview, HelpModal, App.tsx
