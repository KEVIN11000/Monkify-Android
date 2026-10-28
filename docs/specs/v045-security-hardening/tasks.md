# Checklist de Tareas Atómicas: v0.4.5 Hardening y Protección de Servidor

- [x] **Tarea 1: Almacenamiento y UI del Token de Acceso al Servidor en Android**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/data/repository/SettingsRepository.kt`, `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/SettingsViewModel.kt`, `app/src/main/java/com/k11000/monkify/ui/screens/SettingsScreen.kt`
  - **Acción:** Agregar soporte para `serverAccessToken` en DataStore, exponerlo en `SettingsViewModel` e implementar la sección "SEGURIDAD Y CONEXIÓN DE SERVIDOR" en `SettingsScreen`.
  - **Criterio de Aceptación:** El usuario puede configurar, ver/ocultar y persistir su token de servidor desde Ajustes. (Superado: 5/5 unit tests en `SettingsViewModelTest` aprobados).

- [x] **Tarea 2: Blindaje de Red en `AuthInterceptor` y Tests Unitarios**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/data/remote/api/AuthInterceptor.kt`, `app/src/test/java/com/k11000/monkify/data/remote/api/AuthInterceptorTest.kt`
  - **Acción:** Inyectar automáticamente el token en hosts de Monkify y bloquearlo en APIs públicas (Open-Meteo, GitHub). Crear suite de pruebas unitarias.
  - **Criterio de Aceptación:** `.\gradlew.bat testDebugUnitTest --tests "com.k11000.monkify.data.remote.api.AuthInterceptorTest"` pasa al 100%. (Superado: 6/6 tests aprobados).

- [x] **Tarea 3: Propagación de Token a ExoPlayer en Reproducción de Audio**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt`
  - **Acción:** Adjuntar `?token=<token>` al URI de streaming si existe un token configurado en las preferencias del usuario y el destino es un host seguro de Monkify.
  - **Criterio de Aceptación:** Los streams generados incorporan el token para autorización en el Worker. (Superado: Suite de compilación y unit tests verificados).

- [x] **Tarea 4: Middleware de Autenticación en Cloudflare Worker**
  - **Archivos:** `cloudflare-backend/src/index.ts`, `cloudflare-backend/src/types.ts`, `cloudflare-backend/test/index.spec.ts`
  - **Acción:** Crear middleware de protección en `/api/v1/*` que valide `SERVER_ACCESS_TOKEN` vía headers (`X-Monkify-Token`, `Authorization: Bearer`) o query parameter (`?token=`).
  - **Criterio de Aceptación:** Pruebas locales de endpoints validando respuesta 401 sin token y 200 con token. (Superado: 55/55 unit tests en Vitest aprobados al 100%).

- [x] **Tarea 5: Limpieza de Manifiesto y Reglas de Respaldo**
  - **Archivos:** `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`
  - **Acción:** Remover `REQUEST_INSTALL_PACKAGES` y excluir base de datos Room y credenciales de `adb backup`.
  - **Criterio de Aceptación:** Manifiesto limpio y reglas de extracción seguras. (Superado: Verificación exitosa vía `processDebugManifest`).

- [x] **Tarea 6: ProGuard / R8 y Compilación de Release Verificada**
  - **Archivos:** `app/proguard-rules.pro`, `app/build.gradle.kts`
  - **Acción:** Crear reglas de preservación completas y activar `isMinifyEnabled = true` e `isShrinkResources = true`.
  - **Criterio de Aceptación:** `.\gradlew.bat assembleRelease` compila exitosamente con R8 sin errores de ofuscación. (Superado: APK de release compilado con éxito en 32 MB).
