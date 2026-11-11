#!/bin/bash
# Lokaler Entwicklungsstart mit Demo-Seed (Login sofort möglich).
# Der Seed legt für EXISTIERENDEN Personen App-User an, wenn noch keine existieren.
# Passwörter (überschreibbar): LUNEFI_DEMO_PASSWORT_ALEX / LUNEFI_DEMO_PASSWORT_KIM
# Standard-Passwort je Person: lunefi-dev
# WICHTIG: LUNEFI_DEMO_SEED darf in Produktion NIE gesetzt sein!
set -e
docker compose up -d
LUNEFI_DEMO_SEED=true mvn spring-boot:run
