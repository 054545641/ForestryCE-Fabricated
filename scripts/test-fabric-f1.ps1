param(
    [int[]]$Modules = (0..7),
    [switch]$Clients,
    [switch]$DedicatedServers,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$logRoot = Join-Path $repoRoot '.local/f1-validation'
New-Item -ItemType Directory -Path $logRoot -Force | Out-Null
$results = [System.Collections.Generic.List[object]]::new()
$summary = Join-Path $logRoot 'results.json'
if (Test-Path -LiteralPath $summary) { Remove-Item -LiteralPath $summary }

function Invoke-F1Check([string]$Name, [string[]]$GradleArguments, [string[]]$Markers) {
    $log = Join-Path $logRoot "$Name.log"
    $arguments = $GradleArguments + @('--console=plain')
    if ($Offline) { $arguments += '--offline' }
    Write-Host "Running $Name; log: $log"
    & (Join-Path $repoRoot 'gradlew-fabric.bat') @arguments *> $log
    if ($LASTEXITCODE -ne 0) { Get-Content -LiteralPath $log -Tail 45; throw "$Name failed" }
    $text = Get-Content -LiteralPath $log -Raw
    foreach ($marker in $Markers) {
        if (-not $text.Contains($marker)) { throw "$Name did not reach required checkpoint: $marker" }
    }
    if ($text -match "Couldn't parse|Error loading recipe|Failed to load registries|Missing (item model|textures in model).*forestry|Unknown registry key.*forestry") {
        throw "$Name reported a Forestry resource or registry error; inspect $log"
    }
    $results.Add([ordered]@{ name = $Name; arguments = $arguments; passed = $true; log = $log; sha256 = (Get-FileHash -LiteralPath $log -Algorithm SHA256).Hash.ToLowerInvariant() })
    Write-Host "$Name passed"
}

Push-Location $repoRoot
try {
    Invoke-F1Check 'build' @('build', 'gametestClasses') @('FORESTRY_F1_FOUR_JARS_VERIFIED', 'BUILD SUCCESSFUL')
    foreach ($mask in $Modules) {
        if ($mask -lt 0 -or $mask -gt 7) { throw 'Modules must be masks between 0 and 7' }
        Invoke-F1Check "gametest-$mask" @('runGameTest', "-PforestryModules=$mask", '-PwithJade=false', '-PwithJei=false') @('All 7 required tests passed', 'FORESTRY_F1_REGISTERED')
    }
    if ($Clients) {
        foreach ($mask in @(0, 7)) {
            Invoke-F1Check "client-$mask" @('runClientGameTest', "-PforestryModules=$mask") @('FORESTRY_F1_ITEM_MODELS_VERIFIED', 'FORESTRY_F1_JEI_RECIPE_VERIFIED', 'FORESTRY_F1_CLIENT_TEST_PASSED')
        }
    }
    if ($DedicatedServers) {
        foreach ($mask in @(0, 7)) {
            $eula = Join-Path $repoRoot "run/fabric-26.2/server-$mask/eula.txt"
            if (-not (Test-Path -LiteralPath $eula) -or (Get-Content -LiteralPath $eula -Raw) -notmatch '(?m)^eula=true\s*$') {
                throw "Read and accept the Minecraft EULA yourself in $eula before running dedicated-server validation."
            }
            Invoke-F1Check "server-$mask" @('runServer', "-PforestryModules=$mask", '-PserverAutoExit=true', '-PwithJade=false', '-PwithJei=false') @('FORESTRY_F1_SERVER_READY', 'All dimensions are saved', 'BUILD SUCCESSFUL')
        }
    }
    ConvertTo-Json -InputObject @($results.ToArray()) -Depth 5 | Set-Content -LiteralPath $summary -Encoding utf8
    Write-Host "F1 checks passed. Evidence: $summary"
} finally { Pop-Location }
