# Workflow de contribución

Este documento define el flujo estándar para trabajar cambios en este repositorio:

**Issue → branch → commits → PR → CI**

## Reglas obligatorias

- **Nunca** hacer push directo a `main`.
- Siempre trabajar en una rama y subirla con:

  ```bash
  git push -u origin HEAD
  ```

- Los commits deben tener mensaje de **una sola línea**.
- Para commitear, usar `scripts/commit.ps1`.
- Si existe y aplica, usar `scripts/amend.ps1` para corregir el último commit.

## ¿Cuándo usar `amend` vs commit nuevo?

Usa `amend` (si existe `scripts/amend.ps1`) cuando:
- Solo necesitas corregir el **último** commit local.
- El commit aún no fue compartido o no requiere historial separado.
- Se trata de ajustes pequeños (typos, mensaje, detalle menor).

Usa un commit nuevo cuando:
- Ya hay más cambios posteriores que no deben mezclarse.
- Quieres mantener trazabilidad separada por cambio.
- El commit anterior ya fue revisado/publicado y no conviene reescribir historial.

## Convención de nombres de ramas

Crear ramas con alguno de estos prefijos:

- `feat/` para nuevas funcionalidades.
- `fix/` para corrección de bugs.
- `chore/` para tareas de mantenimiento/documentación/configuración.
- `codex/` para tareas automatizadas o asistidas por agente.

## Flujo estándar (paso a paso)

1. **Crear Issue (Dev task)** y completar contexto, alcance y criterios de listo.
2. **Crear rama fresh desde `origin/main`** (PowerShell recomendado):

   ```powershell
   git fetch --all --prune
   git checkout main
   git reset --hard origin/main
   git checkout -b <tipo>/<descripcion-corta>
   git push -u origin HEAD
   ```

3. **Hacer cambios** limitados al alcance del issue.
4. **Commit** usando `scripts/commit.ps1` (mensaje en una línea).
5. **Push** de la rama:

   ```bash
   git push -u origin HEAD
   ```

6. **Abrir PR hacia `main`** y agregar `Closes #3` en el body para vincular el issue.
7. **Esperar Android CI en verde** (build/tests/checks requeridos).
8. **Merge** del PR una vez aprobado y con CI exitoso.


### En la sección “Nota sobre PR e issues”, cambia a placeholder:
Reemplaza tu bloque por este:
```md
## Nota sobre PR e issues

Para que GitHub cierre el issue automáticamente al mergear, incluir en el body del PR:

```text
Closes #<ISSUE_NUMBER>

```

### En la sección de “amend”, agrega esta nota (debajo de tus bullets)
Pega esto al final de esa sección:
```md
> Nota: si ya hiciste push y reescribes el último commit, el push debe ser con `--force-with-lease`
> (idealmente usando `scripts/amend.ps1` si existe).