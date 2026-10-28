# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.5 — Hardening de Seguridad, Protección de Servidor Cloudflare y Optimización R8
**Fecha de cierre:** 2026-10-28
**Estado del build/tests:** ✅ pasando — Suite completa de pruebas unitarias de Android pasando al 100% (`testDebugUnitTest` -> 33 tasks exitosas), suite de Vitest de Cloudflare Worker pasando al 100% (55/55 tests), compilación y empaquetado de Release Android con R8 pasando al 100% (`assembleRelease` -> APK reducido a 32 MB), y Release v0.4.5 publicada exitosamente en GitHub Actions.

---

## 1. Estado actual

- **Middleware de Seguridad y Protección de Cuota en Cloudflare Worker:**
  - Implementado middleware en `cloudflare-backend/src/index.ts` interceptando `/api/v1/*`.
  - Si `SERVER_ACCESS_TOKEN` está configurado en el Worker, exige cabeceras `X-Monkify-Token`, `Authorization: Bearer <token>` o query param `?token=<token>`.
  - Peticiones no autorizadas reciben HTTP 401 en el edge en ~2 ms con cero consumo de R2 o D1.
  - Endpoints públicos (`/health` y preflights `OPTIONS`) permanecen accesibles sin token.
  - Desplegado y verificado en producción en `https://monkify-backend.monkify.workers.dev`.
  - Cubierto por 55 pruebas en Vitest (`index.spec.ts` y `connect.spec.ts`).
- **Gestión de Token de Servidor en la App Android:**
  - Persistencia de `serverAccessToken` en DataStore en `SettingsRepository.kt`.
  - Expuesto reactivamente en `SettingsViewModel.kt` e interfaz en `SettingsScreen.kt` con máscara de contraseña y botones Guardar / Limpiar.
  - Cubierto por pruebas en `SettingsViewModelTest.kt` (5/5 pruebas aprobadas).
- **Aislamiento de Red contra Fugas (`AuthInterceptor`):**
  - En `AuthInterceptor.kt`, inyección del token restringida estrictamente a hosts de confianza (`*.workers.dev`, `monkify.tudominio.com`, `10.0.2.2`, `localhost`, `127.0.0.1`).
  - Bloqueo de transmisión de token a APIs externas (Open-Meteo, GitHub Releases).
  - Cubierto por pruebas unitarias en `AuthInterceptorTest.kt` (6/6 pruebas aprobadas).
- **Streaming Protegido en ExoPlayer:**
  - En `PlayerViewModel.kt`, se anexa `?token=<token>` a la URL de audio si el destino es un host de Monkify y existe un token configurado.
- **Saneamiento de Permisos y Respaldo:**
  - Eliminado `REQUEST_INSTALL_PACKAGES` de `AndroidManifest.xml`.
  - Exclusión de base de datos Room y credenciales locales en `backup_rules.xml` y `data_extraction_rules.xml`.
- **R8 / ProGuard y Release v0.4.5 Publicada:**
  - Reglas completas creadas en `app/proguard-rules.pro` (Gson, Room, Retrofit, Media3, Coil, Rive nativo, Play Services).
  - Activado `isMinifyEnabled = true` e `isShrinkResources = true`.
  - Binario APK reducido de ~50 MB a 32 MB.
  - Tag `v0.4.5` y Release oficial publicada en GitHub Actions (`Monkify-v0.4.5.apk`).

## 2. Decisiones tomadas en esta fase

- **Decisión:** Implementar autenticación a nivel de Cloudflare Worker vía variable de entorno / Secret `SERVER_ACCESS_TOKEN`.
  **Por qué:** Evita exponer cuota de 100k req/día a scrapers sin incurrir en costos de servicios de auth de terceros.
  **Alternativas descartadas:** Cloudflare Zero Trust Access (requiere autenticación interactiva web pesada incompatible con streaming nativo en ExoPlayer) y TOTP Google Authenticator (códigos rotativos cada 30 segundos requieren sincronización temporal y backend con base de usuarios).
- **Decisión:** Propagar el token a ExoPlayer mediante query parameter `?token=<token>`.
  **Por qué:** ExoPlayer no permite inyectar cabeceras HTTP de forma trivial sin implementar un `HttpDataSource.Factory` personalizado; el query param es estándar y nativo.
- **Decisión:** Mantener compatibilidad permisiva si `SERVER_ACCESS_TOKEN` no está configurado en el Worker.
  **Por qué:** Permite desarrollo local sin obligar a definir credenciales si no se requiere.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `cloudflare-backend/src/index.ts` | Middleware de autenticación y verificación de token en `/api/v1/*`. |
| `cloudflare-backend/src/types.ts` | Adición de `SERVER_ACCESS_TOKEN?: string` a `Bindings`. |
| `cloudflare-backend/test/index.spec.ts` | 8 nuevas pruebas unitarias de autenticación en Vitest (55 tests en total). |
| `cloudflare-backend/README.md` | Documentación técnica de configuración y headers para el Worker. |
| `app/src/main/java/com/k11000/monkify/data/repository/SettingsRepository.kt` | Métodos `serverAccessToken: Flow<String>` y `setServerAccessToken(token)`. |
| `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/SettingsViewModel.kt` | Estado `serverAccessToken` y acción para persistir clave de servidor. |
| `app/src/main/java/com/k11000/monkify/ui/screens/SettingsScreen.kt` | UI de Clave de Acceso al Servidor con visibilidad alternable y guardado. |
| `app/src/main/java/com/k11000/monkify/data/remote/api/AuthInterceptor.kt` | Filtro de hosts Monkify para evitar fugas de token a APIs externas. |
| `app/src/main/java/com/k11000/monkify/di/NetworkModule.kt` | Inyección dinámica de `SettingsRepository` en `AuthInterceptor`. |
| `app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt` | Inyección de `?token=<token>` al resolver URI de streaming para ExoPlayer. |
| `app/src/main/AndroidManifest.xml` | Eliminación de permiso `REQUEST_INSTALL_PACKAGES`. |
| `app/src/main/res/xml/backup_rules.xml` | Exclusión de SQLite y sharedpref de respaldos no cifrados. |
| `app/src/main/res/xml/data_extraction_rules.xml` | Reglas de extracción seguras para Android 12+. |
| `app/proguard-rules.pro` | Reglas de preservación para R8/ProGuard. |
| `app/build.gradle.kts` | `versionCode = 6`, `versionName = "0.4.5"`, `isMinifyEnabled = true`, `isShrinkResources = true`. |
| `docs/SERVER_AUTH_GUIDE.md` | Guía exhaustiva de arquitectura, configuración y resolución de problemas. |
| `docs/specs/v045-security-hardening/*` | Artefactos SDD: `spec.md`, `plan.md`, `tasks.md`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Diagnosticar y resolver flujo de conexión con token reportado por el usuario: implementar botón y feedback activo de "Probar Conexión" (`Ping` / `Test Connection`) contra `/api/v1/tracks` en `SettingsScreen` con respuesta visual inmediata (Conectado / Error 401 / Error de Red).
- [ ] Incorporar selector de Escenario Zen (`Campos`, `Colina`, `Hongos`, `Cosmos`) en `SettingsScreen.kt`.
- [ ] Explorar conexión con endpoint APOD de la NASA (`api.nasa.gov`) en el backend para alimentar fondo `COSMOS`.
- [ ] Integrar animaciones vectoriales `.riv` en `res/raw/` para ranas interactivas en el modo Zen.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** El usuario reporta que tras ingresar la clave en Ajustes la app no conecta al guardar.
  **Impacto:** El catálogo no carga y el streaming puede fallar con HTTP 401 si hay desalineación entre el secret en Cloudflare y el valor en la app, o si no se reintenta la conexión inmediatamente tras guardar.
  **Mitigación sugerida:** Implementar un botón "Probar Conexión" en `SettingsScreen` que haga una llamada a `/api/v1/tracks?limit=1` con el token actual y muestre el código de error HTTP exacto recibido, eliminando la incertidumbre para el usuario.
- **Riesgo:** Pérdida de keystore de producción en CI/CD si no se configuran los secrets en GitHub Actions.
  **Impacto:** El pipeline actual genera un debug keystore si no existe variable de entorno `KEYSTORE_FILE`.
  **Mitigación sugerida:** Almacenar el keystore de producción en Base64 en un GitHub Actions Secret (`RELEASE_KEYSTORE_BASE64`) para firmas de producción formales.

## 6. Cómo verificar que este handoff sigue vigente

```powershell
# En el cliente Android:
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease

# En el backend Cloudflare:
cd E:\PG\vcp-s\Monkify\cloudflare-backend
npm test
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
