param(
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ProjectRoot "deploy\docker-compose.test.yml"

if ($Build) {
    docker compose -f $ComposeFile up -d --build
} else {
    docker compose -f $ComposeFile up -d
}

docker compose -f $ComposeFile ps
