# Recherche: Charts, Finanzguru/Splitwise-Ersatz, Kassenzettel-Scannen

Datum: 11.11.2026 · Auftrag: Recherche ohne Code · LuneFi-Kontext: React 19, TS, Tailwind v4, 390px mobile-first, Bundle ~465 KB / 148 KB gzip (gemessen vor dieser Recherche), Schlankheit ist Design-Wert.

LuneFi-Iststand (aus `specs/`): Einträge manuell (R1–R9), Monatsübersicht je Kategorie + Monatsverlauf (R3, R8), Budgets + Warnung (F2 bestätigt), Spar-Coaching (C1–C3 Vorschlag), Sparziele (G5), Konten/Salden manuell (K1–K3), Ausgleich Paar 50/50 + Anteile je Kategorie, Verlauf, ausgeglichen-Markieren (G2, G3, G6–G8), Bank-Import FinTS/PSD2 mit Kategoris-Lernregel (Q1–Q4 bestätigt, Adapter-Spike offen), Monatsprognose (PR1–PR5 Vorschlag), Login/Session (AU1+), immer EUR (R2).

---

## 1. Chart-Library-Empfehlung

### Messwerte (Bundlephobia-API, Vollpaket ohne Tree-Shaking, abgerufen 11.11.2026)

| Library | Version | gzip (Full Package) | React-19-Peer (npm Registry) | Donut/Bar/Line | Tap auf Segment | Bemerkung |
|---|---|---|---|---|---|---|
| hand-rolled SVG | — | **0 KB** | egal (nur JSX) | selbst | `onClick` auf `<path>` | volle Kontrolle, CSS-Variablen direkt nutzbar |
| chart.js + react-chartjs-2 | 4.6.1 / 5.3.1 | **70 KB** (chart.js voll; mit Tree-Shaking nur Doughnut/Bar/Line realistisch ~45–55 KB, Schätzung) | ✓ (`^19.0.0` explizit) | ✓ | ✓ `onClick`/`getElementAtEvent` | **Canvas** → Farben können nicht direkt aus CSS-Variablen gelesen werden, Theme-Wechsel braucht Re-Render |
| recharts | 3.10.1 | **150 KB** (voll; tree-shakebar, Real-Delta je Import kleiner, exact unbekannt) | ✓ (`^19.0.0` explizit) | ✓ | ✓ `onClick`/`activeShape` | zieht Redux Toolkit + immer + d3 mit (Dependency-Liste s. Quelle) |
| @nivo/pie (+core) | 0.99.0 | **67 KB** (nur Pie; Bar/Line = weitere Pakete) | ✓ (`^19.0.0`) | ✓ | ✓ | react-spring + lodash + d3 im Bundle |
| @visx/shape (+axis/scale) | 4.0.0 | **11 KB** pro Modul (d3-shape inklusive) | ✓ (`^18 || ^19`) | ✓ (Pie vorhanden) | ✓ (eigenes onClick) | Baukasten, keine fertige Interaktion/Animation |
| echarts | 6.1.0 | **368 KB** (voll; via `echarts/core` + nur Pie/Bar kleiner, ~100–150 KB, Schätzung) | egal (framework-agnostisch, Wrapper `echarts-for-react` peer `>=16`) | ✓ | ✓ | überdimensioniert für 3 Chart-Typen |
| apexcharts + react-apexcharts | 8.8.1 / 2.2.0 | **277 KB** | ✓ (`>=16.8`) | ✓ | ✓ | schwerste Option, scheidet aus |

Quellen: Bundlephobia-Größen s. Quellenabschnitt (jeweils `bundlephobia.com/api/size?package=…`, 11.11.2026); Peer-Dependencies via `registry.npmjs.org/<pkg>/latest`.

### Empfehlung: hand-rolled SVG (mit chart.js als einzige ernsthafte Alternative)

Begründung für diesen Fall (390px, 3 simple Chart-Typen, Bundle 148 KB gzip als Design-Wert, Tailwind-v4-Dark-Theme):

1. **Bundle:** Recharts (150 KB gzip voll) würde das Bundle im schlechten Fall verdoppeln; chart.js kostet ~45–70 KB gzip (+30–47 %). Hand-rolled: ~0 KB Bibliotheksanteil.
2. **Theme:** SVG kann `fill="var(--farbe-kategorie-1)"` direkt — Tailwind-v4-CSS-Variablen und der Sonne/Mond-Theme-Wechsel funktionieren ohne Extra-Logik. chart.js (Canvas) braucht die Farben als JS-Werte, d. h. `getComputedStyle()`-Auslesen + Neu-Rendern bei Theme-Wechsel (machbar, aber dauernder Wartungspunkt).
3. **Tap-Pflicht:** Bei hand-rolled ist `onClick` auf `<path>` trivial; bei chart.js/recharts ebenfalls gegeben — kein Unterschied im Können, nur im Preis.
4. **Animation/reduced-motion:** Hand-rolled: Segment-Einblenden per CSS `@keyframes` + `@media (prefers-reduced-motion: reduce)` — eine Zeile pro Fall. Bei chart.js/recharts muss Animation konfiguriert werden; native reduced-motion-Unterstützung ist dort nicht dokumentiert (unverified — müsste beim Bau geprüft/abgefragt werden).
5. **React 19:** Alle Kandidaten mit Wrapper sind formal kompatibel (Peer-Range s. o.) — kein Ausschlusskriterium, nur chart.js/redux-Ballast spricht gegen recharts.

Fallback chart.js, wenn doch fertige Lib gewünscht: kleinste der „fertigen" Optionen, React 19 explizit, Donut mit `offset` (Segment-Ausklappen), `onClick` auf Arcs; aber Canvas/Theme-Nachteil einkalkulieren.

### Aufwandsschätzung hand-rolled (Schätzung, nicht gemessen)

- Donut mit Tap-Detail: **~100–150 Zeilen** — Arc-Pfad-Generator (Bogenwinkel → `path d="M…A…"`-Befehl, ~15–20 Zeilen), Segment-Render mit `onClick` + inaktive Segmente abdimmen (~30), Zentrum-Label (Kategorie + Betrag + %, ~20), Einblend-Animation als CSS keyframes + reduced-motion (~20), Optional Legende (~20–30).
- Kleine Balkenserie (Monatsverlauf): ~60–100 Zeilen (div/SVG-Rects, Höhe = Wert, Tap → Zahl überm Balken).
- Einfache Linie: ~80–120 Zeilen (Punkte → `polyline`/`path`, Tap → nächster Punkt). Kumulativ alle drei: grob **300–500 Zeilen**, einmalig, keine Dependency.

### Tap-Detail-Best-Practice (Donut in Finanz-Apps)

Etabliertes Muster (Belege in Quellen): **Segment-Tap → Kategorie, Betrag und Prozent im Donut-Zentrum**; ungetappt zeigt das Zentrum die Gesamtsumme.

- Kendo-UI-Doku: das hohle Donut-Zentrum ist explizit als Platz für Zusammenfassungen/Kennzahlen vorgesehen („provides space for displaying summary information such as totals").
- Syncfusion-KB-Artikel „How to show the tapped point value at the center of the doughnut chart": genau das Muster als angefragte, unterstützte Interaktion.
- Syncfusion-MAUI-Blog: CenterView im Donut-Zentrum für Aggregate/Totals — dasselbe Muster.
- Praktische Ergänzung (eigene Ableitung, aus den Quellen gestützt): sehr kleine Segmente als „Sonstiges" clustern (sonst zu kleine Tap-Targets), und Balken-/Linien-Charts zeigen die Zahl beim Tap als kleines Label direkt am Punkt statt schwebendem Tooltip (390px-Breite, Finger verdeckt Tooltips).

---

## 2. Finanzguru + Splitwise ersetzen?

### Finanzguru (Stand Website finanzguru.de, 11.11.2026)

Angebotene Features: Konten verbinden (4000+ Banken inkl. PayPal/Krypto/Depot), KI-Auto-Kategorisierung, Vertragsübersicht mit Fixkosten, Verträge in-App kündigen, Budgets (Plus 0,99 €/Woche: individuelle Budgets, Analysen, verfügbares Einkommen, Kontostand-Prognose), Versicherungs-Absicherungscheck, KI-Begleiter-Chat („Wie viel gebe ich für Streaming aus?"), Sparbooster-Cashback, Altersvorsorgedepot, Krankenkassenvergleich. Separate App „Split by Finanzguru" (Gruppen, Salden, Erinnerungen, Überweisung, Buchungen aus Finanzguru übernehmen). Beleg-/Kassenettel-Scan: auf der Website **nicht** beworben (geprüft 11.11.2026) — Finanzguru lebt vom Bank-Import.

| Finanzguru-Feature | LuneFi | Anmerkung |
|---|---|---|
| Konten verbinden (Bank-Import) | **geplant** | specs/quellen.md Q1–Q4 bestätigt (FinTS/PSD2, Dedup, Lern-Kategorie), Adapter-Spike offen |
| Auto-Kategorisierung | **teilweise/geplant** | Lern-Regel Q4 (Verwendungszweck→Kategorie) bestätigt — einfacher als Finanzgurus KI, reicht für Haushaltsbuch |
| Budgets | **ja** | F2 bestätigt + Warnung; Spar-Coaching (C1–C3) wäre sogar mehr als Finanzguru (Haltungen statt nur Limit), kostenlos |
| Statistiken/Analysen | **teilweise** | Monatsübersicht/Verlauf als Zahlen (R3/R8); **Charts fehlen noch** — der Auslöser dieses Auftrags |
| Prognose | **geplant** | Monatsprognose PR1–PR5 als Vorschlag bestätigt-pending; Finanzguru prognostiziert Kontostand (Plus) |
| Abos/Verträge erkennen + kündigen | **nein** | Fixkosten nur als Konstante für Prognose (PR1); keine Vertrags-/Abo-Erkennung; Kündigung bewusst out of scope |
| KI-Chat, Cashback, Depot, Versicherung | **nein** | Monetarisierungs-Features eines Startups — für private Paar-App irrelevant |
| Mehrpersonen/Paar | **nein** | Finanzguru ist Single-User; Paar-Gebrauch braucht die separate Split-App |
| Sparziele | **ja** | G5 (auf finanzguru.de nicht beworben — geprüft 11.11.2026) |

Was LuneFi hat, was Finanzguru nicht hat: **Paar-Ausgleich integriert** (Schlüssel je Kategorie, Ausgleichsverlauf, ausgeglichen-Markieren), **Privatsphäre** (eigene Daten, kein Konto-Aggregator, kein Cloud-Zwang, self-hosted), kostenlos ohne Upsell, zwei Personen mit klaren Privatsphäre-Regeln (R7/R9).

### Splitwise (Stand splitwise.com, 11.11.2026)

Kern-Features lt. Website: Gruppen/Freunde, IOU, gleiche/ungleiche Splits (% oder Anteile), Gesamt-Salden, „simplify debts", wiederkehrende Ausgaben, Offline-Modus, Cloud-Sync, Ausgaben-Summen, Kategorien, 100+ Währungen, Zahlungs-Integrationen, unbegrenzte Ausgaben, **Receipt Scanning / Itemization / Charts = Pro-Features**.

| Splitwise-Feature | LuneFi (Paar-Fall) | Anmerkung |
|---|---|---|
| Ausgaben erfassen + zuordnen (wer hat gezahlt) | **ja** | zahler-Feld am Eintrag |
| Aufteilen (gleich/ungleich, %/Anteile) | **teilweise** | 50/50-Standard + abweichende Anteile **je Kategorie** (G3) — nicht je Eintrag frei konfigurierbar; für ein Paar mit konstanten Schlüsseln ausreichend |
| Salden/„wer schuldet wem" | **ja** | G2, jederzeit + Verlauf (G6, G8) |
| Simplify debts | überflüssig | bei 2 Personen entsteht genau ein Saldo |
| Settle up festhalten | **ja (einfacher)** | G7: Monat idempotent als ausgeglichen markieren; einzelne Rückzahlungen als eigene Buchungen fehlen |
| Wiederkehrende Ausgaben | **geplant** | G4 Backlog (Fixkosten nach Schlüssel), fehlt noch |
| Währungen | überflüssig | R2: immer EUR; nur Urlaub mit Fremdwährung relevant |
| Erinnerungen/Push | **nein** | kleinste Lücke — Paar sieht es gemeinsam in der App |
| Beleg-Scan | **nein (Pro)** | siehe Teil 3 — nicht empfohlen |
| Gruppen, Freundesnetzwerk, Payment-Integration | überflüssig | Paar-Anwendungsfall braucht keine Gruppen |

### Top-3-Lücken, um beide wirklich zu ersetzen

1. **Bank-Import (FinTS/PSD2-Spike)** — das Herzstück von Finanzguru. Ohne echte Umsätze ersetzt LuneFi nur das manuelle Haushaltsbuch, nicht die Automatik. (Spec bestätigt, Umsetzung offen.)
2. **Charts/Statistik-Ansichten** — Monats-Kategorie-Donut + Monatsverlauf-Balken sind bereits durch R3/R8 als Daten vorhanden, nur visuell fehlen sie (der Auslöser dieses Auftrags).
3. **Wiederkehrende Ausgaben/Abos** — G4 (Fixkosten-Split) + Erkennung wiederkehrender Umsätze nach Bank-Import; das ersetzt Finanzgurus Vertragsübersicht in dem Maß, das ein Paar braucht (Fixkosten sind der Hauptteil gemeinsamer Kosten).

Fazit: **Ja, ersetzbar für den Paar-Anwendungsfall** — nach diesen 3 Lücken. Finanzgurus Geschäftsmodell-Features (Cashback, Depot, Kündigungsservice, KI-Chat) muss LuneFi nicht nachbauen; das ist kein Verwendungs-, sondern ein Verwertungs-Feature-Set.

---

## 3. Kassenzettel-Scannen: braucht man das?

**Was der Markt tut:** Splitwise bietet Receipt Scanning als **Pro**-Feature; Finanzguru bewirbt **keinen** Beleg-Scan (Website geprüft 11.11.2026) — der Marktfilter-Bankimport macht Belege fürs Eintragen redundant. Beleg-OCR ist Kern-Feature von Spesen-Apps (dort _muss_ der Bon als Nachweis archiviert werden — bei einer privaten Budget-App nicht der Fall).

**Technik-Optionen:**

| Option | Größe/Kosten | Eignung für Kassenbon |
|---|---|---|
| Tesseract.js (Browser, WASM) | JS-Wrapper nur ~4 KB gzip, **aber** Core-WASM + Sprachmodell (deu) als zusätzliche MB-Downloads beim Erstnutzen (Projekt selbst: v5 „54 % smaller for English"; Repo sagt ausdrücklich: kein Accuracy-Tuning, „does not modify the Tesseract recognition model to improve accuracy") | Kassenbons (Thermopapier, verwaschene Schrift, schiefe Fotos) sind der härteste OCR-Fall — roher Tesseract liefert dort Textsalat, den man zeilenweise nachkorrigieren muss |
| Google ML Kit Text Recognition v2 | kostenlos, on-device — **aber nur Android/iOS-SDK, kein Web-Weg** | Receipts explizit als Use-Case genannt; fällt für eine Web-App raus |
| Cloud Vision API (Google) | 1.000 Bilder/Monat frei, dann $1.50/1000 Bilder (Text Detection) | Accuracy deutlich besser als Tesseract, aber: Bon-Fotos in Google-Cloud = widerspricht dem Privatsphäre-Prinzip der App, plus API-Key-Betrieb/Kosten |
| Azure Document Intelligence „prebuilt-receipt" | existiert genau deshalb, weil generisches OCR nicht reicht: OCR + Deep-Learning-Modell für strukturierte Felder (Merchant, Datum, Total, Steuern) | zeigt den Aufwand, den „Bon zuverlässig lesen" wirklich kostet — ein eigenes trainiertes Modell, kein fertiges OSS-Bauteil |

**Urteil: lieber nicht bauen.** Drei Gründe:

1. **Redundanz:** Nach dem Bank-Import (Lücke Nr. 1) steht der Betrag samt Verwendungszweck in jedem Umsatz — genau deshalb hat auch Finanzguru keinen Bon-Scan. Der Bon liefert nur Mehrwert, wo Beträge *nicht* schon elektronisch vorliegen (Bargeld-Kasse) — dafür ist manuelles Eintragen (Sekunden) schneller als Foto + OCR-Korrektur.
2. **Aufwand ≠ Nutzen:** on-device = MB-Downloads + miese Bon-Accuracy; cloud = bessere Accuracy, aber Kosten, API-Keys und Privacy-Verstoß gegen den eigenen Kernwert.
3. **Kein Nachweis-Bedarf:** Private App ohne Spesenabrechnung muss Belege nicht strukturiert auslesen.

**Light-Alternative, falls Bedarf entsteht:** Foto-Anhang am Eintrag (reines Speichern, kein OCR) — Büroklammer-Icon, Bild im Datensatz, null ML-Aufwand, Privacy bleibt gewahrt (nur eigener Server). Erst wenn sich herausstellt, dass Bargeld-Bons tatsächlich häufig manuell getippt werden, lohnt der Blick auf Cloud-Receipt-APIs.

---

## Quellen (abgerufen 11.11.2026)

**Bundle-Größen (Bundlephobia API, Vollpaket/kein Tree-Shaking):**
- recharts 3.10.1: https://bundlephobia.com/api/size?package=recharts (561 KB / gzip 149,9 KB)
- chart.js 4.6.1: https://bundlephobia.com/api/size?package=chart.js (205 KB / gzip 69,6 KB)
- echarts 6.1.0: https://bundlephobia.com/api/size?package=echarts (1,11 MB / gzip 368 KB)
- apexcharts 8.8.1: https://bundlephobia.com/api/size?package=apexcharts (947 KB / gzip 277 KB)
- @nivo/pie 0.99.0: https://bundlephobia.com/api/size?package=@nivo/pie (206 KB / gzip 67 KB)
- @visx/shape 4.0.0: https://bundlephobia.com/api/size?package=@visx/shape (35,6 KB / gzip 10,7 KB)
- tesseract.js 7.0.0 (JS-Wrapper): https://bundlephobia.com/api/size?package=tesseract.js

**React-19-Peer-Dependencies (npm Registry):**
- https://registry.npmjs.org/recharts/latest (react `^16.8||^17||^18||^19`)
- https://registry.npmjs.org/react-chartjs-2/latest (react inkl. `^19.0.0`)
- https://registry.npmjs.org/react-apexcharts/latest (react `>=16.8.0`)
- https://registry.npmjs.org/echarts-for-react/latest (react `>=16`)
- @visx/shape 4.0.0: react `^18 || ^19`; @nivo/pie 0.99.0: react inkl. `^19.0`

**Donut/Tap-Best-Practice:**
- Syncfusion KB „Show tapped point value at the center of doughnut chart": https://support.syncfusion.com/kb/article/11377/how-to-show-the-tapped-point-value-at-the-center-of-the-doughnut-chart
- Kendo UI Donut-Doku (Zentrum = Platz für Totals/Summary): https://www.telerik.com/kendo-angular-ui/components/charts/series-types/donut
- Syncfusion MAUI Doughnut Customization (CenterView für Aggregate): https://www.syncfusion.com/blogs/post/dotnet-maui-doughnut-chart-customization
- Chart.js Doughnut-Doku (Segmente, offset/explode): https://www.chartjs.org/docs/latest/charts/doughnut.html

**Finanzguru:**
- Startseite/Features/FAQ (Konten 4000+, KI-Kategorisierung, Verträge kündigen, Budgets/Plus 0,99 €/Woche, KI-Begleiter, kostenlos): https://finanzguru.de/
- Split by Finanzguru (Gruppen, Salden, Erinnerung, Überweisung, Finanzguru-Integration): https://finanzguru.de/split

**Splitwise:**
- Homepage/Featureliste (splits, simplify debts, recurring, currencies, receipt scanning/itemization/charts als PRO): https://www.splitwise.com/

**OCR/Kassenzettel:**
- Tesseract.js (WASM-Port, kein Accuracy-Tuning, v5-Größenreduktion): https://github.com/naptha/tesseract.js
- Google ML Kit Text Recognition v2 (nur iOS/Android; receipts als Use-Case): https://developers.google.com/ml-kit/vision/text-recognition/v2
- Google Cloud Vision Pricing (Text Detection: 1.000 frei, dann $1.50/1000): https://cloud.google.com/vision/pricing
- Azure Document Intelligence prebuilt-receipt (OCR + Deep-Learning für strukturierte Bon-Felder): https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence/concept-receipt
