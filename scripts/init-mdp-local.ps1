Param(
    [string]$ExampleFile = "mdp.example",
    [string]$LocalFile = "mdp.local"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -Path $ExampleFile)) {
    throw "Missing template file: $ExampleFile"
}

if (Test-Path -Path $LocalFile) {
    Write-Host "$LocalFile already exists, keeping current file."
    exit 0
}

Copy-Item -Path $ExampleFile -Destination $LocalFile
Write-Host "Created $LocalFile from $ExampleFile."
Write-Host "Edit $LocalFile with local debug credentials (file is gitignored)."
