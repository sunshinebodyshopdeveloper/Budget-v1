# Diccionario de strings – Sunshine AppSuite

Este documento centraliza los textos (`strings`) usados en la app y define:
- **Dónde se usan** (pantalla o contexto).
- **Si son genéricos** (reutilizables) o **específicos** de una pantalla.

La idea es mantener sincronizados:
- `app/src/main/res/values/strings.xml`
- `docs/strings_dictionary.md`

---

## Convenciones de nombres

- Prefijo por contexto:
  - `action_…` → acciones / botones (`Iniciar sesión`, `Omitir`, etc.).
  - `label_…` → etiquetas de campos (`Usuario`, `Contraseña`, etc.).
  - `onboarding_…` → textos específicos del onboarding.
  - `login_…` → textos específicos de la pantalla de login.
- `Tipo`:
  - **Genérico** → puede usarse en varias pantallas.
  - **Pantalla específica** → pensado para una pantalla / flujo concreto.

---

## Strings genéricos

| ID                   | Texto                          | Uso / Contexto                                  | Tipo       |
|----------------------|--------------------------------|-------------------------------------------------|-----------|
| `app_name`           | Sunshine AppSuite              | Nombre general de la aplicación                 | Genérico  |
| `action_login`       | Iniciar sesión                 | Títulos o botones relacionados al login         | Genérico  |
| `action_access`      | Acceder                        | Botón principal de acceso en login / otras pantallas | Genérico  |
| `label_username`     | Usuario                        | Campo de usuario en formularios                 | Genérico  |
| `label_password`     | Contraseña                     | Campo de contraseña en formularios              | Genérico  |
| `action_skip`        | Omitir                         | Acción para saltar onboarding u otros pasos     | Genérico  |
| `action_finish`      | Finalizar                      | Acción para terminar un flujo (onboarding, wizard, etc.) | Genérico  |
| `label_version_full` | Sunshine AppSuite v3.0         | Texto de versión en la pantalla de login (footer) | Genérico / Info app |

---

## Strings – Pantalla de Login

### Descripción

Textos exclusivos de la pantalla de inicio de sesión.

| ID               | Texto                                                                                  | Uso / Contexto                        | Tipo                |
|------------------|----------------------------------------------------------------------------------------|---------------------------------------|---------------------|
| `login_subtitle` | Accede con tu usuario de AppSuite para empezar a gestionar las órdenes de trabajo.    | Subtítulo dentro de la tarjeta de login | Pantalla específica |

---

## Strings – Onboarding

El onboarding actual cuenta con 4 pantallas (items) con título y subtítulo específicos.

### Onboarding – Pantalla 1

| ID                      | Texto                                                                      | Uso / Contexto                   | Tipo                |
|-------------------------|----------------------------------------------------------------------------|----------------------------------|---------------------|
| `onboarding_title_1`    | Bienvenido a SunShine AppSuite                                            | Título de la primera pantalla    | Pantalla específica |
| `onboarding_subtitle_1` | AppSuite te ayuda a controlar las órdenes de trabajo y el flujo del taller desde tu dispositivo. | Subtítulo de la primera pantalla | Pantalla específica |

### Onboarding – Pantalla 2

| ID                      | Texto                                                                      | Uso / Contexto                   | Tipo                |
|-------------------------|----------------------------------------------------------------------------|----------------------------------|---------------------|
| `onboarding_title_2`    | Verificación por código QR                                                | Título de la segunda pantalla    | Pantalla específica |
| `onboarding_subtitle_2` | Escanea el QR SBS de la OT para ver el detalle del vehículo, cliente y avance de reparación. | Subtítulo de la segunda pantalla | Pantalla específica |

### Onboarding – Pantalla 3

| ID                      | Texto                                                                      | Uso / Contexto                   | Tipo                |
|-------------------------|----------------------------------------------------------------------------|----------------------------------|---------------------|
| `onboarding_title_3`    | Asignación rápida y eficaz                                                 | Título de la tercera pantalla    | Pantalla específica |
| `onboarding_subtitle_3` | Usa el QR de empleado para registrar quién trabaja cada unidad y llevar control de cargas de trabajo. | Subtítulo de la tercera pantalla | Pantalla específica |

### Onboarding – Pantalla 4

| ID                      | Texto                                                                      | Uso / Contexto                   | Tipo                |
|-------------------------|----------------------------------------------------------------------------|----------------------------------|---------------------|
| `onboarding_title_4`    | Seguimiento en tiempo real                                                 | Título de la cuarta pantalla     | Pantalla específica |
| `onboarding_subtitle_4` | Registra inventarios, fotos de proceso y ubicación de las unidades, todo ligado al mismo QR SBS. | Subtítulo de la cuarta pantalla  | Pantalla específica |

---

## Notas para mantenimiento

- Cada vez que se agregue un nuevo `string` en `strings.xml`, **registrarlo aquí** con:
  - ID
  - Texto
  - Pantalla / contexto
  - Tipo (Genérico o Pantalla específica)
- Si un texto específico empieza a usarse en más de una pantalla, actualizar su `Tipo` a **Genérico** y ajustar la descripción de uso.

