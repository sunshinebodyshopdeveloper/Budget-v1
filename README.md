# Sunshine AppSuite v3 (Taller · Android)

**Sunshine AppSuite v3** es la aplicación móvil interna para la operación del taller **Sunshine Body Shop**.  
Está enfocada en el uso por parte del **personal del taller** (no clientes finales).

> Estado: **Desarrollo activo** — base de arquitectura, seguridad, Settings y navegación principal ya montadas.

---

## 🎯 Objetivo

Facilitar la gestión diaria del taller desde un dispositivo Android:

- Trabajar con **órdenes de trabajo (OT)** generadas en el sistema web.
- Usar **códigos QR** para identificar empleados y órdenes.
- Realizar el **inventario/checklist de ingreso** de vehículos.
- Consultar **datos del cliente** y sus **citas**.
- Preparar la base para futuras funciones: asignaciones, fotos de proceso, rastreo en piso, notificaciones, etc.

---

## ✅ Estado actual

### 1) Proyecto Android base
- Namespace: `com.sunshine.appsuite`
- `minSdk = 31`, `targetSdk = 36`
- `versionName = 3.0.0`
- UI en **XML + ViewBinding** (sin Compose)
- Estructura clásica `app/`:
  - `src/main/java/...` (Activities, Fragments, controllers, etc.)
  - `src/main/res/layout` (pantallas)
  - `src/main/res/values` (strings, styles, themes, colors)

### 2) Infraestructura de red y seguridad (Retrofit/OkHttp)
- Base URL desde `BuildConfig.API_BASE_URL`:
  - `https://api.devtesthub.online/`
- HTTP client configurado con:
  - **Retrofit + OkHttp**
  - `AuthInterceptor` para agregar `Authorization: Bearer <token>`
  - `HttpLoggingInterceptor`
    - Nivel `BODY` en debug
    - Nivel `NONE` en release

### 3) Gestión segura del token
- `TokenManager` basado en:
  - `EncryptedSharedPreferences` + `MasterKey` (Android Keystore)
- Token guardado **cifrado** (no texto plano)
- Métodos:
  - `saveToken(token: String)`
  - `getToken(): String?`
  - `clearToken()`
  - `hasToken(): Boolean`

### 4) Login + validación de sesión
- `AuthApi` (`POST /api/token`):
  - Envía: `username`, `password`, `device_name`
  - Recibe: token Bearer
- `LoginActivity`:
  - Solicita credenciales
  - Consume `AuthApi`
  - Guarda token vía `TokenManager`
  - Navega al Home (`MainActivity`) si es correcto

### 5) Flujo de inicio (Splash → Onboarding → Login → App Lock → Home)
- **Onboarding**:
  - Se muestra la primera vez
  - Se marca como completado con un flag local
- **Token**:
  - Si no hay token → `LoginActivity`
- **App Lock** (seguridad):
  - `AppLockActivity` (PIN / biométrico, según preferencias)
  - Si está activo, se solicita antes de entrar al Home

### 6) Validación de token contra backend
- `UserApi`:
  - `GET /api/v1/user`
- Se usa para comprobar que:
  - El token se envía correctamente
  - `Retrofit + AuthInterceptor` están funcionando
  - El Home está conectado al backend y no es solo UI estática

### 7) Navegación base (Home UI)
- `MainActivity` con:
  - **DrawerLayout** (menú hamburguesa)
  - **Bottom Navigation**
  - Controladores dedicados (`MainDrawerController`, `MainBottomNavController`)
- Controles superiores:
  - Botón de menú (abre drawer)
  - Perfil (BottomSheet)

### 8) Settings (estructura por secciones + consistencia)
- `SettingsActivity` como host
- Fragments por sección (ejemplo):
  - Home, Account, Security, Permissions, Support
- Patrón de consistencia:
  - `SettingsHomeFragment` delega navegación al host via interfaz (`SettingsHomeHost`)
  - Evita lógica duplicada y mantiene UX uniforme

### 9) Pantallas informativas
- `AboutActivity`
- `LegalActivity` (condiciones / privacidad estilo Google)
- Support Center:
  - `SettingsSupportFragment` con UI tipo “Google Support” (búsqueda, acordeones, links)

### 10) Strings y documentación
- Textos visibles gestionados vía `strings.xml`
- Documentación de copy:
  - `docs/strings_dictionary.md`
- Objetivo:
  - Evitar hardcode
  - Facilitar mantenimiento y localización

---

## 🧱 Stack tecnológico

- Kotlin
- AndroidX
- Material 3 (DayNight)
- XML + ViewBinding
- Retrofit + OkHttp
- Coroutines
- EncryptedSharedPreferences / MasterKey
- Biometric (App Lock)

---

## 📦 Requisitos

- Android Studio (versión reciente)
- JDK 17 (Android Studio normalmente ya lo incluye)
- Dispositivo/emulador:
  - Android 12+ (minSdk 31)

---

## 🔧 Configuración (API Base URL)

La app usa `BuildConfig.API_BASE_URL`, definido en Gradle por build type.

Ejemplo:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://api.devtesthub.online/\"")
