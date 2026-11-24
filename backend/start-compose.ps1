# Запуск PostgreSQL и Spring Boot из корня проекта: .\backend\start-compose.ps1
$root = Split-Path -Parent $PSScriptRoot
docker compose --env-file "$root\.env" -f "$PSScriptRoot\docker-compose.yml" up -d --build --wait
