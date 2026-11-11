# Thema: Quellen (Bank- und PayPal-Anbindung)

Quelle: Nutzertext 11.11.2026 („beides machen, dynamisch, Bank wechseln/hinzufügen können"). Beispielwerte mit * sind Vorschläge, im Chat bestätigen.

## Story

Als ich
möchte ich Kontoquellen (Banken per FinTS, PayPal per PSD2) anbinden und ihre Umsätze automatisch importieren
damit Einträge von selbst entstehen und ich beim Bankwechsel nur die Quelle austauschen kann

## Regeln

| ID | Regel | Status |
|----|-------|--------|
| Q1 | Jede Quelle hat eine Art (FinTS, PSD2) und einen Zustand („offen" bis verbunden, danach „verbunden") | bestätigt 11.11.2026 |
| Q2 | Ein Import erzeugt aus jedem Umsatz genau einen Eintrag mit Datum, Betrag und Verwendungszweck | bestätigt 11.11.2026 |
| Q3 | Derselbe Umsatz (gleiche externe Kennung) wird bei wiederholtem Import nicht doppelt übernommen | bestätigt 11.11.2026 (Re-Import-Schutz, Backlog seit Tag 1) |
| Q4 | Wurde für eine Verwendungszweck-Kennung einmal eine Kategorie gelernt, bekommen künftige Umsätze mit diesem Verwendungszweck automatisch dieselbe Kategorie | bestätigt 11.11.2026 (automatische Kategorisierung) |

Hinweis: Die Adapter (FinTS via hbci4java, PSD2 via GoCardless) füllen den Import später mit echten Umsätzen — die Pipeline ist von der Quelle unabhängig getestet (Scenarios speisen den Import direkt). Zustandswechsel auf „verbunden" passiert beim echten Verbindungsvorgang (Spike folgt).

## Beispiele

| Regel | Gegeben | Wenn | Dann |
|-------|---------|------|------|
| Q1 | noch keine Quelle | die Quelle „Commerzbank" der Art „FinTS" für das Konto „Girokonto" angelegt wird | zeigt die Quellenübersicht „Commerzbank" mit Zustand „offen" |
| Q2 | die Quelle „Commerzbank" liefert Umsätze | ein Import mit den Umsätzen „Gehalt +2.000,00 € (01.11.2026)" und „REWE -42,50 € (02.11.2026)" startet | existieren genau zwei neue Einträge mit diesen Beträgen und Daten |
| Q3 | der Import lief | derselbe Import erneut startet | existieren weiterhin genau zwei Einträge (keine Duplikate) |
| Q4 | die Lernregel „REWE" → „Essen" gespeichert ist | ein Import den Umsatz „REWE SAGT DANKE -42,50 €" enthält | hat der Eintrag die Kategorie „Essen" |

## Offene Fragen

| ID | Frage |
|----|-------|
| QF1 | Umbuchungs-Erkennung (PayPal-Lastschrift von der Bank = keine Ausgabe): eigene Regel, sobald zwei Quellen gleichzeitig laufen — jetzt erst Backlog |
| QF2 | Import-Einträge ohne gelernte Kategorie: Kategorie „Import" als Ablage, ja? (Vorschlag) |
| QF3 | Nachträgliche Bearbeitung: Einträge umbenennen/kategorisieren/löschen — eigener Slice |
| QF5 | Import-Inbox: unbekannte Umsätze (z. B. „Amazon 42,99 €") fragen den Nutzer „wo gehört das hin?"; Antwort wird Lernregel (Nutzer-Idee 12.11.2026). Bau, sobald echter Import läuft |

## Daraus entstanden

- `features/quellen/quelle-wird-angelegt.feature`
- `features/quellen/umsaetze-werden-eintraege.feature`
- `features/quellen/wiederholter-import-ueberspringt-doppelte.feature`
- `features/quellen/kategorie-wird-gelernt.feature`
