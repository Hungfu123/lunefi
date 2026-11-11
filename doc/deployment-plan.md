# Deployment-Plan LuneFi (später, auf dem bestehenden VPS)

Erstmal lokal. Wenn LuneFi online geht: **dasselbe Muster wie lune-journal** auf dem VPS (178.104.118.46), nichts neu erfinden.

## Bewährtes Muster (aus lune-journal + softball-lineup übernommen)

- Softball-App ist der Host: ihr **Nginx-Container hält Port 80/443 + alle Let's-Encrypt-Zertifikate** (certbot standalone, DuckDNS-Domains)
- Jede App = eigenes Compose-Projekt, verbunden über das **externe Docker-Netzwerk `proxy`**; Nginx reicht per Container-Name weiter (`resolver 127.0.0.11` wegen Lazy-DNS — aus nginx-lune.conf kopieren)
- Git-Klon unter `/opt/<app>/repo`, `deploy.sh` mit `git reset --hard origin/main` + `docker compose up -d --build`
- **Daten außerhalb des Repos**: `/opt/<app>/data` („kaputter Checkout nimmt nie Daten mit")
- Tägliche Sicherung (`taeglich-sichern.sh` bei lune-journal als Vorlage)

## LuneFi konkret

1. Domain: `lunefi.duckdns.org` (DuckDNS wie die anderen), Zertifikat per certbot
2. `deploy/nginx-lunefi.conf` nach dem Muster von `lune-journal/deploy/nginx-lune.conf` (Mount in /opt/softball/docker-compose.yml beim nginx-Service ergänzen)
3. `docker-compose.prod.yml`: api-Container ins `proxy`-Netz (Container-Name `lunefi-api`), intern lauschen; Postgres-Daten unter `/opt/lunefi/data` (oder Volume), Bank-Secrets aus `/opt/lunefi/repo/.env` (nie committen)
4. Frontend wie bei softball **als static resources ins Spring-Boot-Image backen** (vite build → src/main/resources/static), kein zweiter Web-Container nötig
5. `deploy.sh` nach lune-journal-Vorlage
6. Tägliche Sicherung der Postgres-DB

## Demo-Seed: NUR lokal (Produktionssicher!)

- `DemoSeed` läuft NUR wenn BEIDE Bedingungen erfüllt sind: Profil ≠ prod (`@Profile("!prod")`) UND `LUNEFI_DEMO_SEED=true` (Env/Property).
- Lokal: `./start-dev.sh` setzt das Flag (Passwörter überschreibbar: `LUNEFI_DEMO_PASSWORT_ALEX`/`_KIM`, Standard `lunefi-dev`).
- **Produktion: NIEMALS `LUNEFI_DEMO_SEED` setzen + mit Profil `prod` starten** (`SPRING_PROFILES_ACTIVE=prod` im Compose/deploy) — doppelt abgesichert.
- Seed-Verhalten: nur wenn KEIN App-User existiert und Personen schon angelegt sind; bestehende Passwörter werden nie überschrieben.

## Harte Voraussetzungen VOR dem Online-Gang

- **Login (Google OAuth)** zwingend — sonst liegen die Finanzen offen im Netz. Redirect-URIs gleich mit anlegen: `http://localhost:8080/login/oauth2/code/google` (Entwicklung) + `https://lunefi.duckdns.org/login/oauth2/code/google`
- HTTPS über den Nginx (Zertifikat s.o.)
- Secrets: FinTS-PIN + Enable-Banking-Keys nur in der Server-.env, verschlüsselt at rest, nie in Logs
