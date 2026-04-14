param(
  [Parameter(Mandatory=$true)]
  [string]$Title,

  [Parameter(Mandatory=$true)]
  [string]$Body
)

$ErrorActionPreference = "Stop"

# 1) No permitir desde main (release se hace desde la rama del feature)
$branch = (git branch --show-current).Trim()
if ($branch -eq "main") {
  Write-Host "🚫 Estás en 'main'. Cambia a tu rama (feat/..., fix/...) para hacer release." -ForegroundColor Red
  exit 1
}

# 2) Asegurar que no haya cambios sin commit (para no perder nada)
$status = git status --porcelain
if ($status) {
  Write-Host "🚫 Tienes cambios sin commit. Primero corre checkpoint:" -ForegroundColor Red
  Write-Host "   .\scripts\checkpoint.ps1 ""tu mensaje""" -ForegroundColor Red
  exit 1
}

# 3) Traer lo último de main y actualizar la rama (evita PR con base vieja)
git fetch --all --prune
git merge origin/main

# 4) Push por si el merge trajo algo
git push

# 5) Crear PR (si ya existe, solo lo abre)
$createOut = ""
try {
  $createOut = gh pr create --base main --head $branch --title $Title --body $Body 2>&1
  Write-Host $createOut
} catch {
  $msg = $_.Exception.Message
  if ($msg -match "already exists") {
    Write-Host "ℹ️ Ya existe un PR para esta rama. Lo abrimos." -ForegroundColor Yellow
  } else {
    Write-Host "❌ Error creando PR: $msg" -ForegroundColor Red
    exit 1
  }
}

# 6) Merge PR + borrar rama
gh pr merge --merge --delete-branch

# 7) Volver a main y actualizar local
git checkout main
git pull origin main

Write-Host "✅ Release completado: PR mergeado a main y rama borrada." -ForegroundColor Green
