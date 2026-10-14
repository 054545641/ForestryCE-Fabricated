$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localRoot = Join-Path $projectRoot '.local'
$jdkRoot = Join-Path $localRoot 'jdk25'
$downloads = Join-Path $localRoot 'downloads'
$gradleHome = Join-Path $localRoot 'gradle-26.2'
New-Item -ItemType Directory -Force $downloads, $gradleHome | Out-Null

$archiveName = 'OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1.zip'
$expected = '00c847d804f4a78e9f04f2683faf14fed898535b177b7fc704486cb0284e9283'
$archive = Join-Path $downloads $archiveName
$url = "https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/$archiveName"
if (-not (Test-Path (Join-Path $jdkRoot 'bin/javac.exe'))) {
    if (-not (Test-Path $archive)) {
        & curl.exe -fL --retry 3 --connect-timeout 30 $url -o "$archive.partial"
        if ($LASTEXITCODE -ne 0) { throw 'JDK download failed. Rerun setup to retry.' }
        if ((Get-FileHash "$archive.partial" -Algorithm SHA256).Hash -ne $expected) {
            throw 'JDK checksum mismatch. The archive was not installed.'
        }
        Move-Item -LiteralPath "$archive.partial" -Destination $archive
    }
    if ((Get-FileHash $archive -Algorithm SHA256).Hash -ne $expected) {
        throw "JDK checksum mismatch: $archive"
    }
    $staging = Join-Path $localRoot 'jdk25-extract'
    New-Item -ItemType Directory -Force $staging | Out-Null
    & tar.exe -xf $archive -C $staging
    if ($LASTEXITCODE -ne 0) { throw 'JDK extraction failed.' }
    $extracted = Join-Path $staging 'jdk-25.0.4.1+1'
    if (-not (Test-Path (Join-Path $extracted 'bin/javac.exe'))) { throw 'Unexpected JDK archive layout.' }
    if (Test-Path $jdkRoot) { throw "Incomplete JDK exists at $jdkRoot; move it aside and retry." }
    $boundary = [IO.Path]::GetFullPath($localRoot) + [IO.Path]::DirectorySeparatorChar
    foreach ($candidate in @($extracted, $jdkRoot)) {
        if (-not [IO.Path]::GetFullPath($candidate).StartsWith($boundary, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'JDK installation path escaped the project-local directory.'
        }
    }
    Move-Item -LiteralPath $extracted -Destination $jdkRoot
}

$release = Get-Content (Join-Path $jdkRoot 'release') -Raw
if ($release -notmatch 'JAVA_VERSION="25\.0\.4\.1"' -or $release -notmatch 'IMPLEMENTOR="Eclipse Adoptium"') {
    throw 'The local JDK does not match the pinned Temurin 25.0.4.1 toolchain.'
}
$properties = Join-Path $gradleHome 'gradle.properties'
if (-not (Test-Path $properties)) {
    @(
        'org.gradle.jvmargs=-Xmx4G -Dfile.encoding=UTF-8'
        'org.gradle.workers.max=4'
        'org.gradle.java.installations.fromEnv=JAVA_HOME'
        'org.gradle.java.installations.auto-download=false'
    ) | Set-Content -Encoding ASCII $properties
}

& (Join-Path $jdkRoot 'bin/java.exe') -version
if ($LASTEXITCODE -ne 0) { throw 'The local JDK could not start.' }
& (Join-Path $projectRoot 'gradlew-local.bat') --version
if ($LASTEXITCODE -ne 0) { throw 'Gradle initialization failed.' }
Write-Host 'Ready. Verify: .\gradlew-local.bat :environment:build; client: .\gradlew-local.bat :environment:runClient'
