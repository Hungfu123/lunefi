# Thema: Login und Autorisierung

Quelle: Aufgabenstellung 11.11.2026 (Login mit Spring Security + echte Autorisierung). Die Privatsphäre-Regeln R7/R9 (Saldi/Einnahmen privat, nur gemeinsame Daten geteilt) werden damit **server-seitig** erzwungen — nicht mehr über client-gesteuerte Parameter vertrauenswürdig. Die „Wer bist du"-Auswahl im Frontend war nur zu Testzwecken.

## Story

Als Partner
möchte ich mich mit Namen und Passwort anmelden
damit niemand fremde persönliche Finanzdaten einsehen oder ändern kann

## Regeln

| ID | Regel |
|----|-------|
| AU1 | Nutzer melden sich mit Name + Passwort an (Spring Security, Session-Cookie). Passwörter werden bcrypt-gehasht in der DB gespeichert (eigene `app_user`-Entität: username = Personenname, Passwort-Hash; konzeptionell mit Person verknüpft, ohne Fremdschlüssel). Falsche Zugangsdaten → Abweisung; ungemeldete API-Zugriffe → Abweisung. Beim Onboarding (Personen anlegen) wird JE Person ein initial-Passwort mitgegeben — ohne Passwort → Abweisung, Passwort unter 6 Zeichen → Abweisung |
| AU2 | Endpoints mit Personen-Bezug leiten die Person vom angemeldeten Nutzer ab: der `person`-Parameter darf nur den eigenen Namen enthalten, fremde Namen → Abweisung. Ohne Parameter gilt das bisherige Verhalten unverändert (nur der Fremd-Zugriff ist neu verboten) |
| AU3 | Der Paar-Raum ist für BEIDE angemeldete Nutzer offen: gemeinsam-Sichten, Ausgleich (alle Endpunkte), Budgets, Sparziele, Kategorien, Coaching — unverändert, nur eben hinter dem Login |
| AU4 | Persönliche Einträge (Anlegen, Ändern, Löschen) dürfen nur von der eigenen Person erfasst/bearbeitet werden — fremde Person → Abweisung. Gemeinsame Einträge gehören beiden (auch mit dem Partner als Zahler) |
| AU5 | Konten sind privat: die Kontenübersicht mit fremdem `person`-Parameter → Abweisung; Konten anlegen nur für den eigenen Namen, für den Partner → Abweisung |
| AU6 | Die Ersteinrichtung (Personen anlegen ohne Login) bleibt offen, solange NOCH KEIN App-Nutzer existiert; sobald ein App-Nutzer existiert, ist sie für Unangemeldete geschlossen (die App ist eingerichtet) |
| AU7 | Bestehende Personen OHNE App-Nutzer können ihre Passwörter nachrüsten: ungemeldete Übergabe von Name → Passwort je Person. Nur erlaubt, solange NOCH KEIN App-Nutzer existiert (Schloss-Semantik wie AU6) — existiert bereits einer, wird die Nachrüstung abgewiesen. Existiert eine übergebene Person nicht oder ist ein Passwort unter 6 Zeichen, wird die Nachrüstung komplett abgewiesen (nichts wird angelegt). Die Passwörter werden bcrypt-gehasht als App-User angelegt (Vorschlag 11.11.2026 für die Demo-DB mit bereits angelegten Personen) |
| AU8 | Der Server teilt dem Angemeldeten mit, wer er ist (Identitäts-Auskunft): der Name des angemeldeten Nutzers; ungemeldet wird abgewiesen. Das Frontend leitet die Person davon ab und ersetzt damit die bisherige lokale Personen-Auswahl — der Angemeldete IST die Person |

Technische Entscheidungen (dokumentiert, kein fachlicher Inhalt):

- **UserDetailsService über eigenes Repository** statt `JdbcUserDetailsManager`: die App hat JPA-Entities, kein Schema-Management — ein kleines `AppUserRepository` + `AppUserDetailsService` passt zum existing Stil und vermeidet Tabellen-Namenskonventionen des Jdbc-Managers.
- **CSRF**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` + Session-Cookie; das Frontend liest das `XSRF-TOKEN`-Cookie und schickt `X-XSRF-TOKEN` (SPA-Muster der Spring-Security-Doku). Ungültiges/fehlendes Token → 403.
- **401 statt Redirect** für unauthentifizierte API-Zugriffe (AuthenticationEntryPoint), 403 bei fehlender Berechtigung (Standard).
- **`/api/prognose?person=…` wird analog AU2 geschützt** — die Monatsprognose nutzt denselben R7/R9-Sicht-Filter und gibt persönliche Einnahmen/Ausgaben preis. (Vorschlag 11.11.2026, aus R7/R9 gefolgert — die Aufgabenstellung zählt Prognose nicht explizit auf.)

## Beispiele

Konkrete Werte, keine Prosa. Jede Regel braucht mindestens eins.

| Regel | Gegeben | Wenn | Dann |
|-------|---------|------|------|
| AU1 | die Person Nova mit Passwort „geheim123" eingerichtet wurde | sich Nova mit dem Passwort „geheim123" anmeldet | ist die Anmeldung erfolgreich |
| AU1 | die Person Nova mit Passwort „geheim123" eingerichtet wurde | sich Nova mit dem Passwort „falsches-passwort" anmeldet | wird die Anmeldung abgelehnt |
| AU1 | — | jemand ohne Anmeldung die Einträge abruft | wird der Zugriff wegen fehlender Anmeldung abgelehnt |
| AU1 | Alex angemeldet ist | sich Alex abmeldet und danach die Einträge abruft | wird der Zugriff wegen fehlender Anmeldung abgelehnt |
| AU1 | Alex angemeldet ist | Alex die Person Nova ohne Passwort anlegt | wird das Anlegen abgelehnt |
| AU1 | Alex angemeldet ist | Alex die Person Nova mit dem Passwort „kurz" anlegt | wird das Anlegen abgelehnt |
| AU2 | die Personen Alex und Kim existieren | Alex die Monatsübersicht November 2026 von Kim aufruft | wird der Zugriff wegen fehlender Berechtigung abgelehnt |
| AU2 | die Personen Alex und Kim existieren | Alex den Monatsverlauf von Kim abruft | wird der Zugriff wegen fehlender Berechtigung abgelehnt |
| AU2 | die Personen Alex und Kim existieren | Alex die Einträge von Kim abfragt | wird der Zugriff wegen fehlender Berechtigung abgelehnt |
| AU2 | die Personen Alex und Kim existieren | Alex die Monatsprognose von Kim abruft | wird der Zugriff wegen fehlender Berechtigung abgelehnt |
| AU2 | Kim eine persönliche Ausgabe erfasst hat *(bestehende R7-Szenarien, jetzt mit Kims Anmeldung)* | Kim die eigene Monatsübersicht abruft | funktioniert unverändert (bestehende Szenarien bleiben grün) |
| AU3 | Alex eine gemeinsame Ausgabe Essen −60,00 € (15.11.2026) erfasst hat | Kim die Monatsübersicht November 2026 in der Paar-Sicht aufruft | sieht Kim die gemeinsame Ausgabe (Essen 60,00 €) |
| AU3 | Alex eine gemeinsame Ausgabe Essen −60,00 € (15.11.2026) erfasst hat | Kim den Ausgleich November 2026 als ausgeglichen markiert | zeigt der Ausgleich den Monat als ausgeglichen |
| AU4 | die Personen Alex und Kim existieren | Alex eine Ausgabe von −42,50 € (Kategorie „Freizeit") für Kim erfasst | existiert kein Eintrag |
| AU4 | Alex eine gemeinsame Ausgabe Essen −60,00 € (15.11.2026) erfasst hat | Kim diesen Eintrag zu −50,00 € ändert | ist der Eintrag mit −50,00 € gespeichert |
| AU4 | Kim eine persönliche Ausgabe Freizeit −42,50 € (15.11.2026) erfasst hat | Alex diesen Eintrag zu −50,00 € ändert | wird der Zugriff wegen fehlender Berechtigung abgelehnt |
| AU4 | Kim eine persönliche Ausgabe Freizeit −42,50 € (15.11.2026) erfasst hat | Alex diesen Eintrag löscht | wird der Zugriff wegen fehlender Berechtigung abgelehnt |
| AU5 | die Personen Alex und Kim existieren | Alex die Konten von Kim abruft | wird der Zugriff wegen fehlender Berechtigung abgelehnt |
| AU5 | die Personen Alex und Kim existieren | Alex ein Konto für Kim anlegt | wird der Zugriff wegen fehlender Berechtigung abgelehnt |
| AU6 | noch kein Nutzer eingerichtet ist | jemand ohne Anmeldung die Person Nova mit Passwort „geheim123" anlegt | ist die Person Nova eingerichtet |
| AU6 | die App eingerichtet ist (mindestens ein App-Nutzer existiert) | jemand ohne Anmeldung die Person Nova mit Passwort „geheim123" anlegt | wird der Zugriff wegen fehlender Anmeldung abgelehnt |
| AU7 | die Personen Alex und Kim ohne App-Nutzer existieren | jemand ohne Anmeldung die Passwörter „geheim123" für Alex und „geheim456" für Kim übergibt | ist die Nachrüstung erfolgreich und die Anmeldung mit den neuen Passwörtern klappt |
| AU7 | die App eingerichtet ist (mindestens ein App-Nutzer existiert) | jemand ohne Anmeldung die Passwörter „geheim123" für Alex übergibt | wird die Nachrüstung wegen bestehender Einrichtung abgewiesen |
| AU7 | die Person Alex ohne App-Nutzer existiert | jemand ohne Anmeldung die Passwörter „geheim123" für Alex und „geheim789" für die nicht existierende Person Unbekannt übergibt | wird die Nachrüstung wegen ungültiger Angaben abgewiesen |
| AU7 | die Person Alex ohne App-Nutzer existiert | jemand ohne Anmeldung das Passwort „kurz" für Alex übergibt | wird die Nachrüstung wegen ungültiger Angaben abgewiesen |
| AU8 | Kim angemeldet ist | Kim abfragt, wer angemeldet ist | lautet der gemeldete Name „Kim" |
| AU8 | — | jemand ohne Anmeldung abfragt, wer angemeldet ist | wird die Abfrage wegen fehlender Anmeldung abgewiesen |

Hinweise zu Werten (Vorschlags-Markierung):

- Alle Passwörter („geheim123", „falsches-passwort", „kurz") sind **Test-Vorschlagswerte** (Aufgabenstellung 11.11.2026), absichtlich als Klartext in den Szenarien — für die private Test-App okay, niemals aus Env-Dateien.
- Auch „geheim456" (Kim) und „geheim789" (unbekannte Person) in den AU7-Beispielen sind **Vorschlagswerte** (11.11.2026) — freigewählt, um je Person unterscheidbare Passwörter zu zeigen; keine Vorgabe aus der Aufgabenstellung.
- Test-Hinweis AU7: Der AuthSteps-`@Before` legt vor JEDEM Szenario Alex und Kim als App-User an. Die AU7-Szenarien brauchen den Zustand „Personen existieren, aber KEIN App-User" — der Given-Step legt die Personen direkt an und leert dafür die `app_user`-Tabelle (dokumentiert erlaubt). Damit das Fixture danach wieder hält, setzt der `@Before` die Passwörter von Alex und Kim je Szenario neu (Upsert statt Nur-anlegen).
- Die Namen Alex/Kim stammen aus den bestehenden Specs (R7-Beispiele).
- Der Betrag −42,50 € für Kims Freizeitausgabe folgt dem R7-Beispiel; −50,00 € als Änderungsbetrag folgt dem R5-Muster.

## Offene Fragen

| ID | Frage | Entschieden am |
|----|-------|----------------|
| AU-F1 | Soll `GET /api/personen` die Namen-Liste auch für beide Partner sichtbar bleiben (Status quo), oder gilt das als Metadaten-Leak? | offen — Aufgabenstellung nennt den Endpoint nicht; Verhalten unverändert gelassen |
| AU-F2 | Sparziel-Einzahlungen tragen ein `person`-Feld, das aktuell beide füllen dürfen (Paar-Daten, AU3). So lassen? | 2026-11-11: ja — Sparziele sind Paar-Raum, keine persönliche Sicht |
| AU-F3 | `GET /api/konten` ohne `person`-Parameter zeigt weiterhin ALLE Konten beider Partner (Verhalten unverändert laut AU2). Wollen wir das künftig pro Partner filtern? | offen — bewusst nicht geändert, da Aufgabenstellung „Verhalten unverändert" sagt |
| AU-F4 | Prognose-Endpoint unter AU2-Schutz stellen? | 2026-11-11: ja (gleicher R7/R9-Sicht-Filter) — als Vorschlag markiert, zur Freigabe |

## Daraus entstanden

- `features/auth/login.feature` (AU1: Anmeldung erfolgreich/falsch, unauthentifizierter Zugriff, Abmeldung, Onboarding-Passwortregeln; AU6)
- `features/auth/sichtschutz.feature` (AU2/AU5: fremder `person`-Parameter → Abweisung auf Monatsübersicht, Verlauf, Einträgen, Prognose, Konten; Konto für Partner)
- `features/auth/paar-raum.feature` (AU3: Paar-Sicht und Ausgleich für Kim nutzbar)
- `features/auth/eintrag-schutz.feature` (AU4: Ausgabe für Partner, gemeinsamen Eintrag ändern, fremde persönliche Einträge ändern/löschen)
- `features/auth/ersteinrichtung.feature` (AU7: Nachrüstung klappt + Anmeldung danach, bereits eingerichtet → Abweisung, unbekannte Person / zu kurzes Passwort → Abweisung)
- `features/auth/wer-bin-ich.feature` (AU8: gemeldeter Name des Angemeldeten, ungemeldete Abfrage → Abweisung)
- Szenarien tragen den Tag ihrer Regel, z. B. `@AU1`
