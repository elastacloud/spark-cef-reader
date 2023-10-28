$versions = @("3.0.1", "3.0.2", "3.1.2", "3.2.1", "3.2.4", "3.3.0", "3.3.1", "3.3.2", "3.4.0", "3.4.1", "3.5.0")
$jarPath = "./target/jars"
$covPath = "./target/coverage"

Write-Host "Clearing existing jar artefacts" -ForegroundColor Green
if (Test-Path $jarPath) {
    Remove-Item -Path $jarPath -Force -Recurse
}

if (Test-Path $covPath) {
    Remove-Item -Path $covPath -Force -Recurse
}

New-Item -Path $jarPath -ItemType Directory
New-Item -Path $covPath -ItemType Directory

foreach ($version in $versions) {
    Write-Host "Building for Spark version: $version" -ForegroundColor Green
    & sbt -DsparkVersion="$version" clean coverageOn compile test coverageReport coverageOff package
}

Write-Host "Copying jar files to $jarPath" -ForegroundColor Green
Get-ChildItem -Filter "spark-cef*.jar" -Path ./target -Recurse | Copy-Item -Destination $jarPath

Write-Host "Copying coverage information from most recent spark version to $covPath" -ForegroundColor Green
$maxVersion = ($versions | Measure-Object -Maximum).Maximum
Get-ChildItem -Path ".\target\spark-$maxVersion" -Recurse -Filter "scoverage-report" -Directory | Copy-Item -Destination .\target\coverage\ -Recurse
Get-ChildItem -Path ".\target\spark-$maxVersion" -Recurse -Filter "cobertura.xml" -File | Copy-Item -Destination .\target\coverage\
