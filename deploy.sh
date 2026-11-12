#!/bin/bash
# Deploy-Script auf dem VPS: /opt/lunefi/deploy.sh
# Vorlage im Repo — nach Änderungen auf den VPS kopieren:
#   scp deploy.sh root@<vps>:/opt/lunefi/deploy.sh
#
# Verzeichnisse auf dem Server:
#   /opt/lunefi/repo/   Git-Klon (dieses Repo), enthält auch die .env
#   /opt/lunefi/data/   PostgreSQL-Daten
set -e

cd /opt/lunefi/repo
# Hart auf den Remote-Stand ziehen statt zu mergen: der Klon ist eine reine
# Kopie, lokale Abweichungen sollen hier nie entstehen.
git fetch --prune origin
git reset --hard origin/master

docker compose -f docker-compose.prod.yml up -d --build

docker image prune -f
echo 'Deploy done!'
