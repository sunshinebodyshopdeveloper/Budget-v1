# Especificación UI – Menú de perfil (AppBar derecha)

- **Proyecto:** Sunshine AppSuite v3 (Android)
- **Módulo:** Menú de perfil de usuario
- **Plataforma:** Android · Material Design 3
- **Ubicación:** AppBar de la pantalla principal (`MainActivity`)

---

## 1. Objetivo del componente

Proveer un acceso rápido a acciones relacionadas **con la cuenta del usuario**, sin saturar el navigation drawer:

- Ver / editar datos de perfil.
- Cambiar de taller / cuenta (si aplica multi-sucursal).
- Acceder a preferencias personales.
- Cerrar sesión.

Este menú vive del lado **derecho** de la barra superior, como un botón de avatar.

---

## 2. Activación

- Ícono: **Avatar circular** (inicial del usuario o foto, si hay).
- Ubicación: Esquina superior derecha, alineado con la AppBar.
- Interacción:
  - Tap en el avatar → abre un **menu desplegable** tipo `Material3 Menu` o `DropdownMenu`.
  - Tap fuera o back → cierra el menú.

---

## 3. Contenido del menú de perfil

Orden sugerido de opciones:

1. **Mi perfil**
2. **Cambiar taller** (opcional, si hay varios)
3. **Preferencias**
4. **Cerrar sesión**

### 3.1 Mi perfil

- Acciones:
  - Ver nombre completo, rol, teléfono, correo.
  - Futuro: permitir editar algunos datos locales (ej. foto de perfil).
- Destino:
  - Pantalla `ProfileActivity` o `ProfileFragment`.
- Casos de uso:
  - El usuario valida cómo está registrado.
  - Accede a su QR personal (si lo manejamos ahí).

### 3.2 Cambiar taller (si aplica)

- Solo visible si el usuario tiene varios talleres / sucursales asociados.
- Abre un diálogo o pantalla con lista de talleres:
  - Ejemplo:
    - `Sunshine Body Shop – Mitras`
    - `Sunshine Body Shop – Cumbres`
- Al seleccionar:
  - Se actualiza el contexto global de la app (taller activo).
  - Se recargan datos (citas, OTs, etc.) para ese taller.

> **Nota:** Esta opción puede quedar oculta en la primera versión si todavía no hay multi-taller.

### 3.3 Preferencias

- Contenido sugerido:
  - Tema: Claro / Oscuro / Sistema.
  - Sonidos / vibración de notificaciones internas.
  - Idioma (si se implementa cambio manual).
- Destino:
  - Pantalla `SettingsActivity` o `SettingsFragment` enfocada en preferencias de usuario.

### 3.4 Cerrar sesión

- Mismo comportamiento que en la opción del drawer, pero accesible desde el menú de perfil.
- Debe mostrar diálogo de confirmación:
  - Título: `¿Cerrar sesión?`
  - Mensaje: `Saldrás de tu cuenta en este dispositivo.`
  - Botones: `Cancelar` / `Cerrar sesión`
- Al confirmar:
  - Limpiar datos de sesión / tokens.
  - Navegar a la pantalla de login / onboarding según el flujo definido.

---

## 4. Estados visuales y estilo

- Ítem normal:
  - Texto en color on-surface.
  - Sin fondo o fondo muy sutil al pasar el dedo (ripple effect).
- Ítem en foco / presionado:
  - Fondo con leve énfasis (ej. surfaceVariant con baja elevación).
- Separadores:
  - Puede añadirse un divisor entre:
    - Acciones de perfil (`Mi perfil`, `Cambiar taller`, `Preferencias`)
    - Acción crítica (`Cerrar sesión`).

El menú debe respetar el tema claro/oscuro configurado en la app.

---

## 5. Interacciones y reglas de UX

- El menú no debe bloquear la navegación principal:
  - Se cierra al seleccionar cualquier opción.
- Si el usuario está en una pantalla que requiere sesión:
  - Al cerrar sesión, esa pantalla ya no debe ser accesible al volver atrás.
  - Usar `clearTask` / `newTask` o `popUpTo` en navegación para limpiar la stack.

Casos de error / edge cases:

- Si la carga de datos de perfil falla:
  - El avatar puede mostrar solo la inicial genérica (ej. `?`).
  - El menú sigue funcionando, pero “Mi perfil” puede mostrar un mensaje de error o reintento.

---

## 6. Pendientes / TODO

- [ ] Definir exactamente qué datos se mostrarán en “Mi perfil”.
- [ ] Decidir si el cambio de taller está disponible en v1 o se pospone.
- [ ] Diseñar wireframe / mockup visual (figma o similar) del menú.
- [ ] Alinear textos finales con `strings.xml` para internacionalización.
