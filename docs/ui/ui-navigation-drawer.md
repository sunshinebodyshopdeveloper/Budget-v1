# Especificación UI – Navigation Drawer principal

- **Proyecto:** Sunshine AppSuite v3 (Android)
- **Módulo:** Navegación principal (Navigation Drawer)
- **Plataforma:** Android · Material Design 3
- **Ubicación:** `MainActivity` / contenedor principal de la app

---

## 1. Objetivo del componente

Centralizar la navegación principal de AppSuite v3 en un **navigation drawer** lateral que:

- Sea consistente con Material 3.
- Permita acceder rápido a los módulos clave del taller.
- No sature la pantalla principal (mantener la vista de trabajo limpia).
- Sirva como “mapa mental” de todo lo que ofrece AppSuite.

---

## 2. Activación y comportamiento general

- El drawer se abre desde:
  - **Botón hamburguesa** en la AppBar (esquina superior izquierda).
  - **Gestos**: deslizamiento desde el borde izquierdo (cuando aplique).
- Tipo de drawer:
  - **Modal navigation drawer** (ocupa encima del contenido y lo oscurece ligeramente).
- Cierre:
  - Tap fuera del drawer.
  - Botón de back del sistema.
  - Repetir tap en el botón hamburguesa (si la implementación lo permite).

---

## 3. Estructura visual

El drawer se divide en tres zonas:

1. **Header (encabezado)**
2. **Lista principal de opciones**
3. **Zona inferior (acciones secundarias)**

### 3.1 Header

Elementos:

- Logo de Sunshine / AppSuite (vector o PNG).
- Nombre del taller o contexto actual:
  - Ejemplo: `Sunshine Body Shop`
- Información del usuario:
  - Nombre corto: `Juan Pérez`
  - Rol: `Hojalatero`, `Administrador`, `Recepción`, etc.
- Fondo:
  - Color principal de la marca o superficie elevada de M3.
  - Debe mantener buena legibilidad de texto.

Comportamiento:

- Tap en el header puede llevar a:
  - Pantalla de “Mi perfil” **o**
  - Pantalla de “Cambiar taller / cuenta” (pendiente de definir).

> **Nota:** El botón de foto de perfil no va aquí, se manejará en el **menú de perfil** (lado derecho de la AppBar).

---

### 3.2 Opciones principales del drawer

Lista propuesta (puede ajustarse más adelante):

1. **Inicio**
   - Descripción: Panel general / dashboard básico del taller.
   - Destino: `HomeFragment` o similar.

2. **Citas**
   - Función: Ver citas del día, próximas, crear nueva.
   - Relación: Se integra con el módulo de citas actual + API Sunshine.

3. **Órdenes de trabajo**
   - Función: Listado de OTs, filtrar por estatus, búsqueda.
   - Relación: Vista principal de trabajo del taller.

4. **Inventario de ingreso**
   - Función: Checklist de daños, fotos, accesorios, gasolina, etc.
   - Uso: Recepción de vehículos, vinculado a una OT.

5. **Asignaciones**
   - Función: Asignar trabajos a técnicos/equipos.
   - Nota: Nombre visible puede ser algo como **“Asignaciones dinámicas”** o lo que se defina en UX.

6. **Seguimiento en piso**
   - Función: Ver en qué etapa va cada vehículo (proceso).
   - Comentario: En futuras versiones puede fusionarse con otro módulo.

7. **Notificaciones**
   - Función: Centro de notificaciones internas de la app.
   - Ejemplos: “Nueva OT asignada”, “Cita reagendada”, etc.

---

### 3.3 Zona inferior (acciones secundarias)

Siempre fija al final del drawer:

- **Configuración**
  - Preferencias locales de la app (tema, idioma si aplica, sonidos, etc.).

- **Centro de ayuda / Soporte**
  - FAQ, contacto, WhatsApp, enlace a documentación web.

- **Cerrar sesión**
  - Muestra un diálogo de confirmación:
    - Título: `¿Cerrar sesión?`
    - Mensaje: `Se cerrará tu sesión en este dispositivo.`
    - Botones: `Cancelar` / `Cerrar sesión`

---

## 4. Estados visuales de los ítems

Cada opción del drawer debe manejar los estados estándar:

- **Estado normal**
  - Ícono + texto en color on-surface.
  - Fondo transparente.

- **Estado activo / seleccionado**
  - Fondo con color de énfasis (ej. `primaryContainer` o `secondaryContainer`).
  - Ícono y texto con color `onPrimaryContainer` o equivalente.
  - Indicador opcional (barra lateral sutil).

- **Estado deshabilitado (si aplica)**
  - Menor opacidad (ej. 38%).
  - Sin respuesta al toque.

Los íconos deben ser **Material Icons** para mantener coherencia visual.

---

## 5. Comportamiento de navegación

- El drawer siempre navega a un **destino raíz** del gráfico de navegación.
- Al seleccionar un ítem:
  - Se cierra el drawer.
  - Se navega a la pantalla destino.
- No se deben apilar múltiples copias de la misma pantalla en la back stack.
  - Usar `popUpTo` al navegar a raíces principales (Home, Citas, OT, etc.).

---

## 6. Consideraciones de UX

- Evitar más de **7–8 opciones principales**; lo demás va en:
  - Submenús dentro de pantallas.
  - Zona de configuración / ayuda.
- Nombres cortos y claros:
  - Ejemplo correcto: `Órdenes de trabajo`
  - Evitar: `Gestión centralizada de órdenes de trabajo del taller`
- Mantener la jerarquía:
  - Lo que se usa diario: arriba.
  - Lo eventual: abajo o en Configuración.

---

## 7. Pendientes / TODO

- [ ] Validar lista final de módulos con el flujo real del taller.
- [ ] Definir nombres definitivos de cada opción (alineados al onboarding).
- [ ] Elegir íconos definitivos Material Icons por opción.
- [ ] Definir behavior específico cuando el usuario no tiene permisos a un módulo.
- [ ] Documentar IDs de navegación (por ejemplo: `nav_home`, `nav_appointments`, etc.).
