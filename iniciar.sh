#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
echo 'GoodWare: http://localhost:8080 — aguarde a inicialização. Ctrl+C encerra.'
exec java -Xmx512m -jar dist/goodware-backend-1.0.0.jar
