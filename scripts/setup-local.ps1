$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localRoot = Join-Path $projectRoot '.local'
$jdkRoot = Join-Path $localRoot 'jdk21'
$downloads = Join-Path $localRoot 'downloads'
$gradleHome = Join-Path $localRoot 'gradle'
New-Item -ItemType Directory -Force $downloads, $gradleHome | Out-Null

if (-not (Test-Path (Join-Path $jdkRoot 'bin/javac.exe'))) {
    $archiveName = 'jbrsdk-21.0.11-windows-x64-b1163.116.tar.gz'
    $archive = Join-Path $downloads $archiveName
    $checksum = "$archive.checksum"
    $url = "https://cache-redirector.jetbrains.com/intellij-jbr/$archiveName"
    if (-not (Test-Path $archive)) {
        & curl.exe -fL --retry 3 --connect-timeout 30 $url -o $archive
        if ($LASTEXITCODE -ne 0) { throw 'JDK download failed. Remove the incomplete archive before retrying.' }
    }
    & curl.exe -fL --retry 3 --connect-timeout 30 "$url.checksum" -o $checksum
    if ($LASTEXITCODE -ne 0) { throw 'JDK checksum download failed.' }
    $expected = ((Get-Content $checksum -Raw).Trim() -split '\s+')[0]
    $actual = (Get-FileHash $archive -Algorithm SHA512).Hash
    if ($expected -notmatch '^[a-fA-F0-9]{128}$' -or $actual -ne $expected) {
        throw "JDK checksum mismatch. Remove $archive and retry."
    }
    New-Item -ItemType Directory -Force $jdkRoot | Out-Null
    & tar.exe -xzf $archive -C $jdkRoot --strip-components=1
    if ($LASTEXITCODE -ne 0) { throw 'JDK extraction failed.' }
}

$properties = Join-Path $gradleHome 'gradle.properties'
if (-not (Test-Path $properties)) {
    @(
        'org.gradle.jvmargs=-Xmx4G -Dfile.encoding=UTF-8'
        'org.gradle.workers.max=4'
        'org.gradle.java.installations.fromEnv=JAVA_HOME'
    ) | Set-Content -Encoding ASCII $properties
}

& (Join-Path $jdkRoot 'bin/java.exe') -version
if ($LASTEXITCODE -ne 0) { throw 'The local JDK could not start.' }
& (Join-Path $projectRoot 'gradlew-local.bat') --version
if ($LASTEXITCODE -ne 0) { throw 'Gradle initialization failed.' }
Write-Host 'Ready. Build: .\gradlew-local.bat build; play: .\gradlew-local.bat runClient'
