param(
    [switch]$RemoveVolumes
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $ProjectRoot "deploy\docker-compose.test.yml"

if ($RemoveVolumes) {
    docker compose -f $ComposeFile down -v
} else {
    docker compose -f $ComposeFile down
}
