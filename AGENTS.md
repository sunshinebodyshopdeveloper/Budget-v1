# AGENTS.md — Reglas para Codex (y humanos)

## Roles
- Codex: implementa en ramas, hace commits, abre PRs, corre builds/tests.
- Lía/ChatGPT: define plan, criterios de listo (DoD), y revisa PRs/diffs.

## Regla de oro
- 1 tarea = 1 rama = 1 PR = 1 dueño.
- Nadie toca los mismos archivos al mismo tiempo (si hay riesgo, se “lockea” el archivo por PR).

## Commits
- Cada cambio debe terminar en commit.
- Mensaje de commit: una sola línea (sin párrafos).

## Estructura / Convenciones (ajusta a tu proyecto)
- Paquetes: respetar la organización existente (no reubicar cosas “porque sí”).
- UI: seguir Material 3 / tokens del proyecto (si aplica).

## Comandos obligatorios antes de PR
- Build: ./gradlew assembleDebug
- Tests: ./gradlew test
- (Opcional) Lint: ./gradlew lint

## Qué NO hacer
- No borrar cosas “porque se ven viejas” (todo vive en git).
- No tocar archivos fuera del alcance de la tarea.
- Si falta contexto, levantar duda en el PR en vez de inventar.
