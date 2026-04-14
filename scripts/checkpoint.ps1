param(
  [Parameter(Mandatory=$true)]
  [string]$Message
)

$ErrorActionPreference = "Stop"

# 1) No permitir en main
$branch = (git branch --show-current).Trim()
if ($branch -eq "main") {
  Write-Host "🚫 No puedes hacer checkpoint en 'main'. Crea/usa una rama (feat/, fix/, wip/)." -ForegroundColor Red
  exit 1
}

# 2) Ver si hay cambios reales
$status = git status --porcelain
if (-not $status) {
  Write-Host "😴 No hay cambios para commitear. Todo limpio." -ForegroundColor Yellow
  exit 0
}

# 3) Stage + commit + push
git add -A
git commit -m $Message
git push

Write-Host "✅ Checkpoint listo en '$branch' (commit + push)." -ForegroundColor Green
