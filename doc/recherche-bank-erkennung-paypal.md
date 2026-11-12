# Recherche: Smarte Erkennung beim Bank-Import + PayPal-Verknüpfung

Stand 12.11.2026. Quellen: Enable-Banking-API-Referenz + FAQ (docs.enablebanking.com), PayPal Developer (REST/Transaction Search).
**Legende:** [belegt] = in der Quelle gelesen · [Annahme] = plausible Schlussfolgerung, ungeprüft · [offen] = muss geprüft werden.

## 1. Wie Bank-Umsätze aussehen (Enable Banking, PSD2)

Ein Umsatz liefert [belegt, API-Referenz `Transaction`]:

| Feld | Inhalt | Nutzen für Erkennung |
|---|---|---|
| `entry_reference` | Umsatz-ID, über Sessions stabil | **Dedup** (Import nur einmal) |
| `booking_date` / `value_date` / `transaction_date` | Buchungs-/Wertstellungsdatum | Inbox-Sortierung, Abo-Rhythmus |
| `transaction_amount {amount, currency}` | Betrag + Währung | Zuordnung, PayPal-Matching |
| `credit_debit_indicator` | CRDT/DBIT | Einnahme/Ausgabe |
| `status` | `BOOK` / `PDNG` | Pending ausschließen fürs Matching [belegt FAQ] |
| `creditor.name` / `debtor.name` | Empfänger/Zahler-Name | **Händler-Erkennung** |
| `creditor_account.iban` | IBAN des Empfängers | stabile Händler-Identität |
| `merchant_category_code` | MCC (ISO 18245) bei Kartentransaktionen | Branchen-Vorschlag |
| `remittance_information[]` | Verwendungszweck-Zeilen | Händlername, Rechnungsnr., PayPal-Details |
| `bank_transaction_code {code, sub_code, description}` | Bankinterne Art | Abo/Lastschrift/Überweisung |
| `exchange_rate` | bei Fremdwährung | PayPal-Fremdwährungs-Fälle |
| `balance_after_transaction` | Saldo danach | Nebenprodukt |

Typische MCC-Beispiele [belegt, ISO 18245]: 5411 Supermärkte, 5812 Restaurants, 5814 Fast Food, 5541 Tankstellen, 4111 Nahverkehr, 5912 Apotheken, 5732 Elektronik, 7995 Glücksspiel, 5999 sonst. Einzelhandel.

**Wichtige Grenzen** [belegt FAQ]:
- Historie: mit `strategy=longest` „meist 1–3 Jahre"; **aber** voller Rückgriff oft nur **~1 Stunde nach der ersten Autorisierung** – danach begrenzen viele Banken auf ~90 Tage. → **Beim ersten Verbinden sofort `strategy=longest` ziehen.**
- Hintergrund-Abrufe: viele Banken erlauben nur **~4 Abrufe/Tag** ohne Nutzer-Interaktion; mit PSU-Headern (Nutzer aktiv) entfällt das Limit. → Unser „Sync beim Öffnen + Pull-to-Refresh" passt genau.
- Kein Kategorisierungs-Service bei Enable Banking [belegt FAQ] — Erkennung müssen wir selbst machen.
- Gebündelte Umsätze (z. B. Sammelbuchungen) erscheinen nicht einzeln [belegt FAQ].
- `entry_reference` kann fehlen oder doppelt sein; Pending-Refs können sich ändern [belegt FAQ]. → Dedup zusätzlich über (Datum, Betrag, Verwendungszweck-Hash).
- Consent max. ~180 Tage; nach Re-Auth ändern sich **Account-IDs** → Konten über `identification_hash` wiedererkennen [belegt FAQ].

## 2. Smarte Erkennung — Pipeline (Vorschlag)

Ziel: **nicht alles kategorisieren, nicht alles fragen.** Reihenfolge, absteigende Sicherheit:

1. **Exakte Wiederholung** (gelernt): gleiche IBAN oder gleicher normalisierter Händlername → Kategorie aus Lernregel. (Lernregeln bauen wir gerade.)
2. **MCC → Kategorie**: 5411/5412→Lebensmittel, 5812/5814→Restaurants, 5541→Tanken, 4111→ÖPNV, 5912→Gesundheit … nur als *Vorschlag* mit mittlerer Sicherheit, nie automatisch übernehmen.
3. **Händler-Normalisierung**: Gläubiger-Name bereinigen (Kartenterminals wie „REWE SAGT DANKE 4711", Datums-/Referenzsuffixe, „PAYPAL *", „VISA DEBIT" entfernen) → Händlername → Baustein für Lernregeln.
4. **IBAN als Identität**: bekannte IBAN (z. B. Vermieter, Versicherung, Strom) ist der stabilste Schlüssel, oft besser als der Name.
5. **Verwendungszweck-Muster**: „Miete", „Abschlag", „Beitrag", „Rechnung Nr.", Lastschrift-Kennungen → Untertyp (fix/variabel), Abo-Kandidat.
6. **Wiederkehrend-Erkennung**: gleicher Händler/IBAN + ähnlicher Betrag (±10 %) + monatlicher Rhythmus (25–35 Tage) über ≥3 Monate → Abo-Hinweis. Nur Anzeige.
7. **Unsicher → Inbox, aber gebündelt**: pro Händler **eine** Frage, nicht pro Buchung. „REWE, 7 Buchungen — Lebensmittel?" → eine Antwort gilt für alle + speichert Lernregel.

**UX-Prinzipien** (gegen „zu viel"):
- Inbox zeigt eine **kurze Zusammenfassung**: „12 neue Buchungen · 9 erkannt · 3 zu prüfen".
- Erkanntes wird mit hoher Sicherheit nur **bestätigt-gebündelt** angezeigt („Übernehmen"), nicht einzeln abgefragt.
- Nie nach Dingen fragen, die inferierbar sind (bekannter Händler, klarer Verwendungszweck).
- Unscharfe Fälle sichtbar markieren („unsicher") statt still falsch zu kategorisieren.
- Ein Eintrag pro realem Vorgang — Dedup vor der Inbox.

**Neue Bausteine, die wir dafür bräuchten** (nicht gebaut):
- `Haendler`-Tabelle (normalisierter Name, IBAN, Standard-Kategorie, Quelle der Regel).
- `mcc`-Spalte am Eintrag/Quelle + MCC→Kategorie-Mapping.
- Inbox-Endpunkt + Inbox-Screen (QF5).
- Abo-Erkennung (Rhythmus-Analyse) — nur Hinweise.

## 3. PayPal: Was ist möglich?

### 3a. Warum PayPal-Buchungen auf dem Konto „nackt" sind
Auf dem Girokonto steht bei PayPal-Zahlungen meist nur „PayPal Europe S.a.r.l." als Gläubiger; der echte Händler steckt bestenfalls im Verwendungszweck („PAYPAL *REWE", „Ihr Einkauf bei …"). Der Verwendungszweck ist bankabhängig formatiert [Annahme, an echten Umsätzen zu prüfen]. Ohne Zusatzdaten ist PayPal auf dem Konto also ein **Sammelposten**.

### 3b. Weg 1: PayPal als eigene Datenquelle (Transaction Search API)
- `GET /v1/reporting/transactions` liefert PayPal-Transaktionen inkl. Händler-/Payee-Name, Betrag, Datum, Event-Code, Währung [belegt: API existiert].
- **Harte Hürde [belegt]:** „You'll need a **PayPal Business account** to go live with integrations." → Mit reinem Privatkonto ist der Live-API-Zugang nicht möglich. Optionen: (a) auf Geschäftskonto wechseln (kostenlos, aber anderer Kontotyp/AGB — deine Entscheidung), (b) PSD2-Weg über einen Aggregator (s. 3c), (c) ohne PayPal-API arbeiten (s. 3d).

### 3c. Weg 2: PayPal über PSD2-AIS (Enable Banking & Co.)
- PayPal Europe ist eine Bank (Luxemburg) und fällt unter PSD2 [Annahme, allgemein bekannt].
- Ob **Enable Banking** PayPal als ASPSP integriert hat, konnte ich **nicht verifizieren** [offen]: Die ASPSP-Liste kommt nur über `GET /aspsps` (mit unseren Keys). → Sobald Enable-Banking-Zugang da ist: ASPSP-Liste nach „PayPal" filtern. Falls vorhanden, wäre das der saubere Weg (Consent, kein Business-Konto nötig).

### 3d. Weg 3: Ohne PayPal-API (sofort möglich)
- Verwendungszweck-Parsing („PAYPAL *HÄNDLER") + Händler-Normalisierung + Lernregeln.
- Grenze: nur so gut wie der Verwendungszweck der Bank. Wenn dort nur „PayPal Europe" steht, bleibt der Umsatz ein Sammelposten → Inbox fragt einmal („PayPal-Buchung — wofür?"), Lernregel auf Händler-Muster.

### 3e. Wenn Bank + PayPal verbunden sind: Matching-Design
Ziel: **ein** Eintrag pro echter Zahlung, angereichert mit dem echten Händler.

Matching-Kandidat: Bank-Belastung mit Gläubiger „PayPal" ↔ PayPal-Transaktion mit
`betrag ≈ gleich` und `datum ± 0–4 Tage` (Bank bucht später) und Währung passend.

Randfälle (alle relevant):

| Fall | Verhalten | Lösung |
|---|---|---|
| Zwei PayPal-Käufe, gleicher Tag, gleicher Betrag | mehrdeutig | nicht automatisch matchen; beide als „unsicher" in Inbox, oder Händler aus Verwendungszweck nehmen |
| Rückerstattung (Refund) | negativer PayPal-Vorgang | Paar bilden (Kauf + Erstattung) → netto, sonst doppelt |
| Fremdwährung | Bank-Betrag ≠ PayPal-Betrag | über `exchange_rate`/Originalbetrag matchen, Differenz als Gebühr/Kursverlust kennzeichnen |
| PayPal-Guthaben-Zahlung | **kein** Bankumsatz | eigener PayPal-„Konto"-Bestand in der App, sonst fehlt die Ausgabe komplett |
| Auszahlung PayPal → Bank | Bank-Gutschrift, keine Einnahme | als Umbuchung markieren, nicht als Einnahme zählen |
| Teilzahlung (Guthaben + Karte/Bank) | Bankumsatz < Kaufbetrag | Rest als PayPal-Guthaben-Anteil; Matching über Teilbetrag |
| Gebühren | separater kleiner Betrag | eigene Zeile „Gebühren", nicht dem Händler zuordnen |
| Freunde/Familie-Zahlungen | kein Händler | nicht kategorisieren, als „privat" lassen |
| Zeitversatz 1–4 Tage | Datum ≠ | Match-Fenster, nie auf den Tag genau bestehen |
| Doppelzählung Bank+PayPal | beide Quellen | **ein** Eintrag mit zwei Quell-Verweisen; Dedup VOR Anzeige |

**Empfehlung (Reihenfolge):**
1. Jetzt: Import ohne PayPal-API; PayPal-Umsätze über Verwendungszweck + Lernregeln; Inbox fragt einmal pro Muster.
2. Sobald Enable-Banking-Keys da sind: ASPSP-Liste auf PayPal prüfen → wenn ja, PSD2-Weg (bevorzugt, kein Business-Konto).
3. Nur falls 2. scheitert und du es willst: PayPal-Business-Konto + Transaction Search API (Matching-Engine wie 3e).

## 4. Consent & 90 Tage — was der Nutzer wirklich tun muss (beantwortet 12.11.2026)

- **App-Key (nach Registrierung):** läuft nie ab; Request-Tokens erzeugt der Server selbst (JWT, max. 24 h TTL). Kein Nutzereingriff. [belegt]
- **Bank-Consent:** max. ~180 Tage (bankabhängig, `maximum_consent_validity` pro ASPSP) [belegt]. Ablauf → App zeigt „Verbindung erneuern" → ein Bank-Login (SCA) → läuft wieder. Daten in LuneFi bleiben; Consent regelt nur das Abrufen. Vorzeitiges Ablaufen möglich (Wartung, KYC-Umfrage) [belegt FAQ] → EXPIRED_SESSION freundlich abfangen.
- **90-Tage-Fenster:** betrifft nur die HISTORIE-Tiefe (volle Historie ~1 h nach Erst-Autorisierung, danach ~90 Tage je Abruf) [belegt FAQ] — kein Re-Login-Grund. Daher: Erstverbindung zieht sofort `strategy=longest`, laufender Sync nutzt `default` (90 Tage reichen).

## 5. Was das für die Inbox-Story bedeutet

- Erst-Anbindung: `strategy=longest` **sofort** nach Autorisierung, danach `default` bei jedem Öffnen.
- Import-Idempotenz: `entry_reference` + Fallback-Hash; `PDNG` ausschließen.
- Inbox-Gruppierung pro Händler mit Sicherheitsstufe (erkannt/unsicher), eine Frage pro Muster.
- PayPal-Umsätze als eigene Klasse behandeln (Sammelposten → Inbox „wofür?"), später durch Matching ersetzbar.
