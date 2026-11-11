# Recherche: Bank-Import-Sicherheit für LuneFi (FinTS vs. PSD2-Aggregatoren)

Datum: 2026-11-11 · Recherche per Web-Suche, alle Aussagen mit Quellen; Unverifiziertes explizit markiert.

## Kernergebnis vorab (TL;DR)

1. **GoCardless Bank Account Data ist für NEUE Projekte nicht mehr verfügbar.** Neuanmeldungen sind deaktiviert, das Produkt wird abgewickelt (Primärquelle: GoCardless-eigene Seite, s. Quellen [G1]). Die bisherige Planungsannahme („Aggregator = GoCardless, free tier") kollidiert damit.
2. **Der beste Ersatz für den_privaten Use Case ist Enable Banking** (Finnland): kostenloses „Restricted Production" für die eigenen Konten, FIN-FSA-lizenzierter AISP, ISO 27001, und laut eigener Doku **keine Speicherung/Caching von Umsätzen** — die beste Privacy-Story aller Aggregatoren.
3. **FinTS PIN/TAN bleibt der privacy-stärkste Standard-Weg** (direkt, kein Dritter). Die alte Commerzbank-Sicherheitsdatei (RAH) ist ein Auslaufmodell-Nischenprodukt — **Empfehlung ändert sich: nicht mehr als Standard vorsehen.**
4. Das alte HBCI-Modell (Schlüsseldatei, INI-Brief) war kryptografisch tatsächlich stärker als heutiges PIN/TAN — wurde aber per DK-Beschluss unter PSD2 für kopierbare Dateien verboten und von den Banken aus Kostengründen zurückgedrängt.

---

## 1. FinTS-Sicherheitsmodell: heute (PIN/TAN) vs. früher (RDH/RAH)

### Wie FinTS heute sichert (PIN/TAN-Profil)

- **Transport:** TLS zwischen Client und Bank-Server (Banken haben alte unverschlüsselte HBCI-Zugänge 2024 abgeschaltet, s. [Q6]).
- **Nachrichten:** Beim PIN/TAN-Profil gibt es **keine zusätzliche Ende-zu-Ende-Verschlüsselung oder Signatur der FinTS-Nachrichten** — die Sicherheit des Kanals ist TLS; die Hibiscus-Community bestätigt: „Bei PIN/TAN kannst du die FinTS-eigene Verschlüsselung komplett weglassen" [Q1]. Die PIN wandert also TLS-geschützt, aber als Geheimnis *im* Tunnel, und die Nachricht selbst wird nicht signiert.
- **Authentifizierung:** PIN (Wissen) + seit PSD2 **SCA** (Starke Kundenauthentifizierung) mit TAN-Verfahren (chipTAN, pushTAN, photoTAN …) bei Login/Überweisungen — zwei unabhängige Faktoren [Q4].

### Wie das alte HBCI-Modell (RDH/RAH, „Sicherheitsdatei") funktionierte

- **Lokales RSA-Schlüsselpaar** des Nutzers; der Fingerabdruck (Hash) des öffentlichen Schlüssels wird auf dem **INI-Brief ausgedruckt** und auf Papierweg an die Bank geschickt — die Bank prüft den Fingerprint gegen die ihr per PIN/TAN im Online-Banking übermittelten Daten. Das ist eine **gegenseitige Authentifizierung inkl. Man-in-the-Middle-Schutz**: Ein gefälschtes Bank-Zertifikat würde auffallen, weil der Schlüssel-Fingerprint nicht übereinstimmt [Q3][Q7].
- **Jede Nachricht wird elektronisch signiert und Ende-zu-Ende verschlüsselt**: historisch DES-MAC + 2-Key-Triple-DES (RDH alt, 112 Bit effektive Stärke) [Q2]; moderne Profile RDH-10/RAH-10 mit RSA bis 4096 Bit [Q3]. FinTS 3.0 mit Schlüsseldatei signiert Aufträge elektr. — „TAN-Nummern werden nicht mehr benötigt, nur die Schlüsseldatei" [Q3].
- **Kein gemeinsames Geheimnis** geht über die Leitung (anders als die PIN): Die Sicherheit beruht auf asymmetrischer Kryptografie, nicht auf einem abfischbaren Passwort.

### Fazit Vergleich + warum wurde es zurückgedrängt?

| | PIN/TAN (heute Standard) | RDH/RAH Schlüsseldatei (alt) |
|---|---|---|
| Authentifizierung | PIN + SCA (Wissen + Besitz) | asymmetrische Signatur, gegenseitig, INI-Brief |
| Nachrichtenschutz | nur TLS (Transport) | TLS + E2E-Signatur + E2E-Verschlüsselung |
| Phishing-Resistenz | mittel (PIN/SCA abfischbar, Trojaner) | hoch (Fingerprint-Papiervergleich) |
| Wartungsaufwand Bank | gering (Standard-Login-Infrastruktur) | hoch (Schlüsselverwaltung je Kunde) |

**Ja, das alte Modell ist kryptografisch stärker** (gegenseitige Authentifizierung, E2E-Signatur statt Transport-only). Zurückgedrängt wurde es durch:
1. **PSD2/SCA + DK-Beschluss:** Die Deutsche Kreditwirtschaft entschied, dass unter PSD2 die **kopierbare Schlüsseldatei nicht mehr erlaubt** ist und RDH-10 ersetzt werden muss (durch Signatur-Chipkarte oder PIN/TAN) [Q3][Q5]. Konkret dokumentiert für Hypovereinsbank/StarMoney [Q5].
2. **Aufwand/Kosten** der Schlüsselverwaltung pro Kunde und die Vereinheitlichung mit dem Browser-Banking. Nur wenige Banken bieten noch Schlüsseldatei an — Hibiscus-Wiki: „nur von sehr wenigen Banken" [Q8].
3. Commerzbank hat RDH abgeschaltet und auf **RAH mit Sicherheitsdatei** umgestellt [Q9], bietet aber inzwischen auch FinTS PIN/TAN (muss freigeschaltet werden) [Q10].

### Verbleibende Risiken bei FinTS PIN/TAN + bekannte Vorfälle

- **Endpunkt-Angriffe:** Banking-Trojaner (Zeus-Familie; auf macOS Retefe/Dok, das Online-Banking-Websites imitiert) zielen auf die Eingabe, nicht auf das Protokoll [Q11][Q12]. Auch die anwaltliche Fachkritik hält das „HBCI = trojanersicher"-Marketing für unzutreffend, weil Credential-Eingabe und PC kompromittierbar bleiben [Q13].
- **Credential-Speicherung in der Software:** hbci4java speichert Passports (inkl. PIN/TAN-Daten) **AES-256-verschlüsselt mit Passphrase** [Q14] — d.h. die Qualität hängt von der gewählten Passphrase ab; Keylogger lesen die Eingabe direkt. hbci4java selbst hat **keinen prominenten bekannten CVE/Sicherheitsvorfall** (unverifiziert-formal: negative Evidenz lässt sich nicht belegen), wird aber von Wenigen gepflegt (Community-Library, kein Hersteller-Support).
- **Kein Angriff auf das FinTS-Protokoll selbst** ist öffentlich bekannt — die dokumentierte Angriffsfläche ist immer der Endpunkt.

---

## 2. GoCardless Bank Account Data & PSD2-Aggregatoren

### Lizenz & Datenschutz-Bedeutung

- GoCardless operiert in der EEA als **GoCardless SAS (Frankreich), autorisierter AISP**, in UK als GoCardless Ltd (FCA, Reg.-Nr. 597190) [G2][G7]. AISP-Lizenz = Aufsicht durch eine Finanzaufsicht (FCA/ACPR), DSGVO-Verantwortlichkeit, Pflichten zu Konsens, Datensicherheit und Löschung.
- **ISO 27001 seit September 2016**, regelmäßig extern auditiert [G3].
- **Aber — Datenschutz konkret (Privacy Centre „Account holders"):** GoCardless nutzt Umsatzdaten nicht nur als Durchleitungsstelle. Sie **organisieren und kategorisieren** Rohdaten (Kategorien wie „Lebensmittel"), **reichern Umsätze an** (Empfänger/Filial-Standort), treffen automatisierte Entscheidungen darüber und nutzen die Daten für Support + Produktverbesserung/ML-Modelle [G4]. D.h.: **GoCardless sieht und speichert/verarbeitet Umsätze — kein reiner Konduit.**
- Datenspeicherung EU: keine Aussage gefunden, dass BAD-Daten ausschließlich in der EU gehalten werden (UK-Unternehmen; EEA-Entity existiert). **Unverifiziert, nicht ausgeschlossen.**

### Kern-Punkt: Dritter sieht alles — ehrliche Bewertung

| | FinTS direkt | Aggregator (GoCardless) |
|---|---|---|
| Wer kann Umsätze lesen | Bank + eigene App | Bank + Aggregator + eigene App |
| Persistenz beim Dritten | — | ja (Kategorisierung/Enrichment laut Privacy Policy) |
| Vertrags-/ToS-Risiko | — | ja (Preis, Scope, **Stilllegung** des Produkts) |
| Regulatorische Aufsicht | Bank (BaFin) | AISP-Aufsicht (FCA/ACPR) + DSGVO |
| Angriffsfläche | Bank-Endpoint + eigener Server | zusätzlich Infrastruktur des Dritten (breach target mit Daten Tausender Kunden) |

Für eine Paar-Finanz-App mit Privatsphäre als **Kernprinzip** wiegt „Dritter sieht und speichert alle Umsätze" schwer: Der Aggregator ist zum Zeitpunkt des Scans ein Cache voller Bewegungsdaten Vieler — attraktives Angriffsziel — und die Nutzungsbedingungen können sich jederzeit ändern. **Das extremsste Beispiel ist bereits eingetreten:** GoCardless hat das freie Nordigen-Erbe abgewickelt.

### Status-Änderung (entscheidend!)

- **Nordigen wurde am 01.07.2022 von GoCardless übernommen** [G5], hieß dann „GoCardless Bank Account Data" (BAD).
- **Seit Mitte 2026 sind Neuanmeldungen für BAD deaktiviert**; das Produkt wird abgewickelt: „New signups for Bank Account Data are currently disabled" [G1], Support-Aussage: „We are no longer offering Bank Account Data as a standalone product to new customers" [G6]. Bestandsintegrationen (z. B. Actual-Budget-Nutzer mit Free Tier, 50 Verbindungen/Monat [G8]) laufen weiter, aber **für LuneFi als Neuprojekt ist dieser Weg versperrt.**

---

## 3. Alternativen zu GoCardless

| Anbieter | EU/DE-Abdeckung | AISP-Lizenz | Preise für Privat | API für self-hosted | Privacy-Story |
|---|---|---|---|---|---|
| **Enable Banking** (FI) | 2.500–2.700+ ASPSPs, 30 Länder [E1][E2] | ja, FIN-FSA [E3] | **kostenlos** im „Restricted Production"-Modus (nur eigene, verlinkte Konten — exakt der LuneFi-Fall); Voll-Produktion volumentarifiziert + Vertrag/KYB [E4] | ja, self-serve Signup, REST+JWT | **bestens dokumentiert:** „does not store, cache, or process data for any purpose other than delivering it to the application" [E4]; ISO 27001 [E2]; Nutzerverwaltung eigener Consents [E4] |
| **figo** (DE/Hamburg) | 3.100+ Institute, DE ~99 % relevant [F1] | ja (BaFin; erster dt. „Banking Service Provider") | historisch Dev-Programm kostenlos [F2]; heute B2B, Preise on request (**aktuelle Free-Tier-Situation unverifiziert**) | API gut dokumentiert, aber Fokus B2B | deutscher Anbieter = dt. DSGVO-Aufsicht; datenspeichernde PFM-Herkunft (Kategorisierung im Produkt) |
| **GoCardless BAD** (ex-Nordigen) | EEA+UK, uniform | ja | Free Tier 50 Verbindungen — **nur Bestandskunden, Neuanmeldung gesperrt** [G1][G6][G8] | beste Doku der Branche | schwächer: Kategorisierung/Enrichment laut Privacy Policy [G4] |
| **Klarna Kosma** (DE) | breit EU | ja | B2B-Preise (Community-Bericht: <€2k/Monat-Range) [K1] | self-hosted möglich, aber Sales | schwedisch-deutsche Gruppe; wenig Privacy-Detail |
| **Tink** (Visa, SE) | 3.400+ Institute [K2] | ja | Enterprise/Sales, kein self-serve | nein (für Privates) | US-Mutterkonzern (Visa) |
| **TrueLayer** (UK) | UK+EU stark | ja | Development-Tier nur Test; Scale = Monatsgebühr + per-use [T1] | nur bedingt | UK-Unternehmen; Pay-by-Bank-Fokus |
| **Plaid** (US) | EU-Coverage vorhanden | ja | Sandbox gratis; Production-Trial ~10 Items, dann paid [T2] | ja, self-serve | US-Anbieter, US-Datenmodell (Plaid US ist Datenschutz-Diskussionsthema) |
| **Salt Edge** (MD/EU) | 5.000+ global | ja | „On request"/quote-basiert [S1] | dokumentiert, aber Enterprise-Bias | moldawische Wurzeln, EU-Compliance-Arm |
| **Nordigen** | = GoCardless BAD | — | — | — | Marke eingestellt |
| **Open Banking Solutions** (openbankingsolutions.com) | — | — | — | — | **Achtung:** diese Firma ist ein Oracle-Apex-Banking-Core-Anbieter, **nicht** der gesuchte PSD2-Aggregator [O1]; ein deutscher Aggregator „Open Banking Solutions GmbH" (BankingAPI) ließ sich in dieser Recherche nicht belastbar verifizieren |

**Warum ein Privatmann nicht direkt die PSD2-API jeder Bank nutzt (belegt):**

- Banken identifizieren Drittanbieter **ausschließlich über eIDAS-Qualifizierte Zertifikate** (QWAC für TLS/mTLS, QSeal für Request-Signierung), ausgestellt von QTSPs nach Audit [P1][P2]. Die EBA-Framework-Vorgabe: ASPSPs sind nicht verpflichtet, TPPs anders anzuerkennen [P3].
- Kosten: **QWAC grob 3.000–8.000 €/Jahr** [P4] bzw. 5.000–15.000 €/Jahr inkl. Seals [P5]; **AISP-Zulassung** bei der NCA (BaFin): Erlaubnisantrag mit Governance-/Risiko-Programm, Haftpflichtversicherung, **6–12 Monate Verfahren**, Anwalts-/Compliance-Kosten 20.000–50.000 €, laufende Aufsichtskosten [P5][P6].
- Hinzu: je Bank eigene Integration-Spezifika trotz Berlin-Group-Standard [P5].
- **Genau diese Kluft ist der Geschäftsgrund der Aggregatoren** [P5]. Der „dritte Weg" — zertifikatfrei, direkt zwischen eigenem Server und Bank, nur eigene Daten — ist legal und **exakt das FinTS-Modell** für Selbstbetreiber [P5].

---

## 4. Credentials/Secrets in der eigenen Spring-Boot-App (Best Practices, kurz)

1. **Nie im Klartext in DB oder Config.** Verschlüsselung at rest mit AES-256/GCM; Master-Key **nie** in derselben Datei wie die Ciphertexte, sondern injiziert via Environment/VM-Keystore [C1][C3].
2. **Konfigurationswerte:** Jasypt `ENC(...)`-Properties (spring-boot-starter) mit Master-Key aus env [C1] — für eine Single-Host-App ausreichend; HashiCorp Vault als Overkill-Eskalation [C3]. Hinweis: Jasypt-Bibliothek ist kaum noch gepflegt — StackOverflow empfiehlt Alternativen wie Vault oder eigene JCE-Verschlüsselung prüfen [C2].
3. **DB-Spalten für Bank-PIN/Aggregator-Tokens:** Column-Level-Encryption (Jasypt-Converter oder eigener `AttributeConverter` mit AES/GCM) [C1][C3].
4. **hbci4java-Passport:** Datei ist AES-256-passphrase-verschlüsselt [Q14] → starke Passphrase wählen, Datei mit 0600 auf einem Volume außerhalb des Git, Backup verschlüsselt.
5. **Enable-Banking-Auth:** JWT signiert mit eigenem RSA-Private-Key — der Private Key nie rausgeben, nur beim Request signieren [E4].
6. **Logging:** Credentials/Tokens/PIN niemals loggen; Logback-Pattern-Masking für fatale Felder (iban, pin, token).
7. **Betrieb:** Datenbank-Volumen verschlüsselt (LUKS), Docker-Netz isoliert, App nur lokal/vpn erreichbar.

---

## 5. Sicherheits-Matrix (Lieferable 1)

| Kriterium | **FinTS PIN/TAN** (hbci4java) | **FinTS RAH/Sicherheitsdatei** (Commerzbank) | **PSD2-Aggregator** (Enable Banking / GoCardless) |
|---|---|---|---|
| Authentifizierungsstärke | PIN + SCA (2 Faktoren), anfällig für Endpunkt-Phishing | **stärkste:** RSA-Signatur + gegenseitige Authentifizierung (INI-Brief), E2E-Verschlüsselung jeder Nachricht | SCA bei der Bank (2 Faktoren), Zugriff delegiert an lizenzierten AISP |
| Wer sieht Umsätze | Bank + eigene App (**kein Dritter**) | Bank + eigene App (**kein Dritter**) | Bank + **Aggregator** (+ eigene App) |
| Angriffsfläche | Trojaner/Keylogger auf dem Host; Passport-Datei (AES-256); Third-Party-Library-Unterhaltung (hbci4java, wenige Maintainer) | wie PIN/TAN, plus: Schlüsseldatei-Diebstahl (geschützt durch Datei-Passphrase), plus: **Abhängigkeit von bankenseitiger Abschaltung** | Endpunkt + Infrastruktur des Aggregators (Breach-Ziel, ToS-Änderungen, **Produkt-Stilllegung** — reales Beispiel GoCardless BAD) |
| Nutzer-Aufwand | gering (PIN, SCA im App-Freigabe-Flow) | mittel-hoch: Bank freischalten, INI-Brief ausdrucken/einschicken, Warten | gering (Konsent-Flow alle ~180 Tage erneuern) |
| Dynamik (alle Banken gleich?) | **nein** — je Bank anderes TAN-Verfahren/Config | **nein** — nur einzelne Banken (Commerzbank, teils Genobanken via Chipkarte) | **ja** — eine uniforme REST-API, eine Integration, alle PSD2-Banken eines Landes; PayPal nur bei einzelnen Aggregatoren (GoCardless: ja; Enable: unverifiziert) |
| Ausfallsicherheit | Bank + eigene Software (voll unter eigener Kontrolle) | Bank kann Verfahren jederzeit einstellen (RDH wurde bereits大面积 abgeschaltet) | Aggregator-Geschäftsmodell (GoCardless-Fall zeigt das Risiko) |
| Regulatorischer Schutz | Bankenaufsicht | Bankenaufsicht | AISP-Lizenz + DSGVO (Löschungsrechte, Consent-Verwaltung) |

---

## 6. Ehrliche Empfehlung für LuneFi (Lieferable 2)

**Standard: FinTS PIN/TAN direkt (Weg a) — bleibt, mit Bank-für-Bank-Adapter (hbci4java).**
Begründung: Kernprinzip Privatsphäre = kein Dritter im Datenfluss; self-hosted endet an der eigenen Firewall; PSD2-konform ohne App-Lizenz (Zertifikat-frei, weil Verbindung Kunde↔Bank direkt ist [P5]).

**Upgrade/Dynamik-Weg: Enable Banking „Restricted Production"** als zweiter Import-Kanal:
- kostenlos für eigene verlinkte Konten (der KYB-Vertrag entfällt genau dann, wenn nur eigene Konten verlinkt sind — Passend für 2-Personen-App) [E4],
- FIN-FSA-lizenzierter AISP, ISO 27001 [E2][E3],
- **explizite Nicht-Speicherung** von Umsätzen in der eigenen Doku („does not store, cache, or process data for any purpose other than delivering it") [E4] — die quantifizierbar beste Privacy-Story im Aggregator-Markt,
- eine uniforme API für alle Banken + SCA über die Bank-Apps,
- Fallback, falls eine Bank FinTS für Dritt-Software einstellt oder kaputt macht.

**GoCardless BAD: raus aus der Planung** (Neuanmeldung gesperrt, Produkt wird abgewickelt [G1][G6]). Nur relevant, falls ein bestehender Account existiert.

**Commerzbank-Sicherheitsdatei (RAH): Empfehlung wird ZURÜCKGEZOGEN.**
Sie ist kryptografisch das stärkste Verfahren, aber: Nische (nur wenige Banken [Q8]), Abwicklungs-Risiko (RDH wurde schon per DK-Beschluss verboten [Q3], Commerzbank hat gerade erst umgestellt [Q9] — jedes weitere Verfahren-Update kann es treffen), INI-Brief-Papier-Aufwand pro Einrichtung, null Dynamik (kein zweiter Bankzugang mit demselben Code-Pfad). Für LuneFi gibt Commerzbank PIN/TAN per FinTS (freischaltbar [Q10]) dieselbe Privatsphäre bei weniger Betriebsrisiko. Sicherheitsdatei nur noch als dokumentierte Option für den Fall, dass PIN/TAN bei der Commerzbank abgeschaltet würde.

**Trade-off, der benannt werden muss (unbequem):** Der dynamische Weg (Aggregator) kostet prinzipbedingt Privatsphäre — ein Dritter steht im Datenfluss, auch bei Enable Banking (dort wenigstens ohne Speicherung). Umgekehrt kostet der direkte Weg Dynamik — je Bank eigenes TAN-Chaos, hbci4java-Fehlerbilder. Für LuneFi mit 2 Nutzern und überschaubarem Bank-Set ist der Privatsphäre-Trade-off klar wichtiger als die Wartung von 2–4 Bank-Adaptern.

---

## 7. Anbieter-Ranking Top 3 für LuneFi (Lieferable 3)

| # | Anbieter | Begründung |
|---|---|---|
| 1 | **Enable Banking** | Kostenloser Restricted-Production-Zugang für eigene Konten (KYB-frei), FIN-FSA-AISP + ISO 27001, **vertrags-/dokumentationsseitig keine Umsatzspeicherung**, self-serve, gute Docs incl. Consent-Renewal-Logik. Der exakte Ersatz für das tote GoCardless-Free-Tier [E1–E4]. |
| 2 | **figo** | Deutscher AISP (BaFin-Aufsicht, Datenhaltung DE), 3.100+ Institute, gute API; Preis für Privat/Dev heute on request — für LuneFi nur attraktiv, wenn Enable nicht abdeckt [F1][F2]. |
| 3 | **GoCardless BAD** (nur falls Bestandskonto) | Beste Doku + PayPal-Anbindung + gratis Free Tier; aber Neuanmeldung gesperrt, Produkt in Abwicklung, schwächere Privacy-Story (Kategorisierung/Enrichment der Umsätze) [G1][G4][G6][G8]. |

(Ehrenvolle Erwähnung: Klarna Kosma als deutsche, kosmopolitische Option falls später mal Voll-Produktion mit Vertrag nötig wird [K1].)

---

## 8. Quellen (Lieferable 4)

**FinTS/HBCI:**
- [Q1] Forum: PIN/TAN ohne FinTS-eigene Verschlüsselung — https://homebanking-hilfe.de/forum/topic.php?t=24809
- [Q2] Linux-Magazin 2003, HBCI-Protokoll-Sicherheitsqualitäten (DES-MAC, 2K3DES) — https://www.linux-magazin.de/ausgaben/2003/08/geld-transport/
- [Q3] Reiner SCT Wiki: Schlüsseldatei/RDH/RAH-Profile, INI-Brief, DK-Entscheidung zu RDH-10 — https://www.reiner-sct.com/en/wiki-en/key-file/
- [Q4] knowledgelib: PSD2-SCA/XS2A-Grundlagen — https://knowledgelib.io/compliance/financial/psd2-open-banking/2026
- [Q5] StarMoney-Forum: DK-Beschluss RDH-Sicherheitsdatei nicht mehr erlaubt — https://www.starmoney.de/forum/viewtopic.php?t=41484
- [Q6] VR-Kennung Blog: Abschaltung alter FinTS/HBCI-Server (TLS) — https://vrkennung.de/hbci/
- [Q7] Wikipedia HBCI (INI-Brief, RSA-Schlüssel, Sicherheitsklassen) — https://de.wikipedia.org/wiki/Homebanking_Computer_Interface
- [Q8] Hibiscus-Wiki zu PSD2/Schlüsseldatei („nur sehr wenige Banken") — https://willuhn.de/wiki/doku.php?id=psd2
- [Q9] ALF-BanCo: Commerzbank-Umstellung RDH → RAH mit Sicherheitsdatei — https://www.alf-banco.de/download/support/Commerzbank%20Umstellung%20auf%20RAH%20mit%20Sicherheitsdatei.pdf + Hibiscus-Wiki: https://www.willuhn.de/wiki/doku.php?id=support%3Alist%3Abanken%3Amisc%3Ardh
- [Q10] ALF-BanCo: Commerzbank FinTS PIN/TAN (freischaltbar) — https://www.alf-banco.de/faq_fragen_und_antworten/konto-einrichtung/commerzbank/
- [Q11] ZDNET: Dok/Retefe macOS-Banking-Trojaner — https://www.zdnet.com/article/watch-out-for-this-money-stealing-macos-malware-which-mimics-your-online-bank/
- [Q12] F5 Labs: Banking-Trojaner-Referenz — https://www.f5.com/labs/articles/banking-trojans-a-reference-guide-to-the-malware-family-tree
- [Q13] ilex: „Wie sicher ist HBCI-Banking?" (Kritik am Sicherheitsmarketing) — https://www.ilex-recht.de/nachricht-im-detail/wie-sicher-ist-hbci-banking.html
- [Q14] Hibiscus-FAQ: hbci4java-Passports AES-256-verschlüsselt — https://www.willuhn.de/wiki/doku.php?id=support%3Afaq

**GoCardless/Nordigen:**
- [G1] GoCardless: New signups disabled — https://bankaccountdata.gocardless.com/new-signups-disabled
- [G2] GoCardless BAD Overview (AISP GoCardless SAS/EEA) — https://developer.gocardless.com/bank-account-data/overview
- [G3] GoCardless GDPR-Seite (ISO 27001 seit 2016) — https://gocardless.com/legal/gdpr bzw. https://gocardless.com/privacy/en-gdpr
- [G4] GoCardless Privacy Centre „Account holders" (Kategorisierung/Enrichment/Nutzung) — https://gocardless.com/privacy/account-holders
- [G5] GoCardless PR: Akquisition Nordigen 01.07.2022 — https://gocardless.com/blog/gocardless-acquire-open-banking-platform-nordigen
- [G6] Reddit r/selfhosted: „no longer offering Bank Account Data as a standalone product to new customers" — https://www.reddit.com/r/selfhosted/comments/1mfs6ro/actual_budget_cannot_sign_up_to_gocardless_bank/
- [G7] GoCardless Ltd FCA-Registrierung (Footer Legal) — https://gocardless.com/privacy/account-holders
- [G8] Actual-Budget-Doku: GoCardless Free Tier (50 Verbindungen) — https://actualbudget.org/docs/advanced/bank-sync/gocardless/

**Enable Banking:**
- [E1] Enable Banking Startseite (2.500+ ASPSPs, 30 Länder) — https://enablebanking.com/
- [E2] The Paypers: Enable Banking (FIN-FSA, ISO 27001) — https://thepaypers.com/fintech/news/finton-goes-live-with-enable-bankings-open-banking-infrastructure
- [E3] Enable Banking Terms (EBA-Register-Link AISP FIN-FSA) — https://enablebanking.com/terms/
- [E4] Enable Banking FAQ (Preise, Restricted Production, keine Speicherung, Consent-Verwaltung) — https://enablebanking.com/docs/faq/ + https://enablebanking.com/knowledge-base

**Alternativen:**
- [F1] figo/Cloud Services Made in Germany (3.100 Banken, Dev-nutzung) — https://www.cloud-services-made-in-germany.de/mehr-als-100-developer-nutzen-figos-banken-api
- [K1] Fynteq: finAPI vs Tink vs Klarna Kosma (Preis-Range DE) — https://fynteq.com/insights/open-banking-providers-germany
- [K2] openbankingcompare: Tink 3.400+ Institute — https://www.openbankingcompare.com/blog/best-open-banking-api-providers-for-developers-2026
- [T1] TrueLayer-Pricing-Dreiteilung (Dev free = testing only) — https://blog.finexer.com/truelayer-pricing-uk/
- [T2] openbankingtracker Free-API-Guide (Plaid ~10 Items Trial, Enable restricted, GoCardless closed) — https://www.openbankingtracker.com/guides/free-open-banking-apis
- [S1] Softwr: Salt Edge „On request" — https://www.softwr.com/pricing/salt-edge
- [O1] Open Banking Solutions (Oracle-Apex-Firma, NICHT der dt. Aggregator) — https://www.openbankingsolutions.com/

**PSD2-Direktzugriff:**
- [P1] Yapily Docs: eIDAS QWAC/QSeal nötig — https://docs.yapily.com/getting-started/integration-setup/prerequisites/european-prereqs
- [P2] dev.to: QWAC erklärt (kein normales TLS-Zertifikat) — https://dev.to/johnfrandsen/the-cheapest-open-banking-apis-for-small-businesses-and-indie-builders-in-2026-5cab
- [P3] Konsentus: ASPSPs müssen TPP nur über eIDAS-Zertifikat anerkennen — https://www.konsentus.com/are-eidas-certificates-sufficient-for-psd2-open-banking/
- [P4] dev.to (QWAC ~3–8k €/Jahr) — https://dev.to/johnfrandsen/the-cheapest-open-banking-apis-for-small-businesses-and-indie-builders-in-2026-5cab
- [P5] dev.to „eIDAS Certificate Tax" (QWAC 5–15k €, AISP 6–12 Monate, 20–50k €, Aggregator-Begründung, zertifikatfreier self-hosted Weg) — https://dev.to/johnfrandsen/the-eidas-certificate-tax-why-eu-open-banking-still-runs-through-aggregators-2c52
- [P6] BaFin Erlaubnis & Registrierung Zahlungsdienste — https://www.bafin.de/DE/Aufsicht/ZahlungsdienstePSD2/ZulassungsverfahrenundLaufendeAufsicht/ZulassungsverfahrenundLaufendeAufsicht_node.html

**Spring-Boot-Secrets:**
- [C1] GeeksforGeeks: Column-Level Encryption mit Jasypt — https://www.geeksforgeeks.org/springboot/spring-boot-enhancing-data-security-column-level-encryption/
- [C2] StackOverflow: Jasypt-Maintenance-Diskussion + Alternativen — https://stackoverflow.com/questions/72198393/jasypt-out-of-maintenance-what-to-use-for-encryption-with-spring-boot
- [C3] Mindful Chase: Jasypt vs. Vault in Spring Boot — https://www.mindfulchase.com/deep-dives/spring-boot-microservices/securing-sensitive-data-with-encryption-and-vault-in-spring-boot.html

**Offene Punkte / Unverifiziertes:**
- PayPal-Anbindung bei Enable Banking: unklar (GoCardless hatte sie). Vor Architektur-Entscheidung Bank-/ASPSP-Liste von Enable Banking gegen die eigenen Institute prüfen (https://enablebanking.com/open-banking-apis).
- figo-Preismodell 2026: on request; Free-Tier-Status nicht verifiziert.
- GoCardless-Datenhaltung EU-only: keine öffentliche Aussage gefunden.
