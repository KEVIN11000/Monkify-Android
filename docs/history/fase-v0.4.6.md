# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.6 — Recuperación de Producción, Resiliencia R8, Aviso Flotante Global y Skeletons de Carga
**Fecha de cierre:** 2026-10-29
**Estado del build/tests:** ✅ pasando — Suite completa de pruebas unitarias de Android pasando al 100% (`testDebugUnitTest` -> 34 tasks exitosas), suite de Vitest de Cloudflare Worker pasando al 100% (55/55 tests), compilación y optimización R8 de Release Android verificada empíricamente al 100% (`assembleRelease` -> bytecode auditado sin eliminación de campos en DTOs, APK 32 MB), y Previews de UI validadas visualmente con Generative UI.

---

## 1. Estado actual

- **Keystore Oficial Permanente y Cero Fricción en Actualizaciones:**
  - Creado almacén criptográfico permanente `app/keystore/monkify-release.jks` con validez de 10.000 días (~27 años).
  - Huella digital criptográfica oficial verificada con `apksigner`:
    - `DN: CN=Monkify Release, OU=Mobile, O=Monkify, L=Buenos Aires, ST=BA, C=AR`
    - `SHA-256: 62ede63ffb1e550f868e76000252f64e33940845385b6d307e342667aa744b5f`
  - En `.github/workflows/release.yml`, eliminado el paso efímero `keytool -genkey` que causaba firmas aleatorias entre versiones en CI. A partir de v0.4.6, todas las APKs se firman con este certificado unificado, permitiendo actualizaciones directas sin conflicto de firmas.
- **Resiliencia R8 y Blindaje de Serialización de DTOs (`RCA-01` resuelta):**
  - Corregida regla de preservación en `app/proguard-rules.pro` a `com.k11000.monkify.data.remote.dto.**` y `com.k11000.monkify.domain.model.**`.
  - Anotados todos los campos de `BackendTrackDto`, `TrackDto`, `PlaylistSummaryDto`, `ActionResponseDto`, etc., con `@SerializedName(...)` explícito.
  - Creado test de contrato por reflexión `DtoSerializationContractTest.kt` que valida la presencia de `@SerializedName` en todos los campos y la hidratación libre de `NullPointerException`.
  - Verificado en el reporte de optimización R8 `app/build/outputs/mapping/release/usage.txt` y `mapping.txt` que ningún campo o getter es eliminado ni inlined a null.
- **Eliminación de Condición de Carrera en `AuthInterceptor` (`RCA-02` resuelta):**
  - En `SettingsRepository.kt`, implementación de caché volátil en memoria y lectura síncrona/inmediata en frío `getServerAccessTokenImmediate()`.
  - En `NetworkModule.kt`, `AuthInterceptor` consulta el snapshot síncrono del token, garantizando que la primera petición HTTP al arrancar la app ya viaje autenticada.
- **Aviso Flotante Global Inteligente (`ServerStatusMonitor` & `FloatingServerStatusBanner.kt`):**
  - Implementado `ServerStatusMonitor` centralizado (`@Singleton`) con política Anti-Spam (cero avisos durante navegación normal saludable; solo interviene ante HTTP 401, error de red o reconexiones explícitas).
  - Píldora superior animada `FloatingServerStatusBanner` integrada en `MainActivity.kt`:
    - `HTTP 401`: Alerta destacada con mensaje explicativo y botón directo de acción rápida **"Ajustes"**.
    - `Modo Offline / Red`: Aviso descriptivo con botón **"Reintentar"** y botón **"✕"** que silencia alertas repetitivas de red en segundo plano mientras el usuario escucha música local.
    - `Recuperación / Conectado`: Aviso verde efímero que se auto-desvanece a los 3 segundos únicamente tras salir de un fallo o probar la conexión.
  - Cubierto por pruebas unitarias en `ServerStatusMonitorTest.kt` (3/3 pruebas aprobadas).
- **Esqueletos de Carga Shimmer (`Modifier.shimmerPlaceholder` & `ShimmerSkeleton.kt`):**
  - Modificador animado de barrido de luz metálico nativo en Compose.
  - Componentes `SkeletonQuickPlayItem` y `SkeletonAlbumCard` integrados en `HomeScreen.kt` mientras `isLoading == true`, eliminando el salto prematuro al botón *"Play Test Track"* y dando predictibilidad física visual de qué contenido se está descargando.
  - Fallback en estado final vacío con botón dual: *"Reintentar conexión"* + *"Play Test Track"*.
- **Diagnóstico Activo "Probar Conexión" y Reconexión Reactiva (`RCA-03` resuelta):**
  - En `SettingsViewModel.kt` y `SettingsScreen.kt`, botón **"Guardar y Conectar"** con feedback en vivo (`Testing` -> `✓ Conectado (Catálogo activo)` o `✗ Error HTTP 401`).
  - En `HomeViewModel.kt` y `LibraryViewModel.kt`, observación reactiva de cambios en `serverAccessToken` (`distinctUntilChanged`) para recargar automáticamente el catálogo sin reiniciar la app.
  - En `PlayerViewModel.kt`, respaldo síncrono de token y diagnóstico específico de `HTTP 401` en `parsePlaybackError`.

---

## 2. Decisiones tomadas en esta fase

- **Decisión:** Implementar política anti-spam en `ServerStatusMonitor`.
  **Por qué:** Para evitar saturar al usuario con avisos verdes o spinners fugaces durante peticiones rutinarias de paginación o búsqueda cuando el sistema está sano.
- **Decisión:** Skeletons nativos en Compose con `rememberInfiniteTransition` y `linearGradient`.
  **Por qué:** No introduce dependencias de librerías externas pesadas y se adapta dinámicamente a la paleta oscura de Monkify.
- **Decisión:** Proteger por contrato todos los DTOs en `DtoSerializationContractTest`.
  **Por qué:** Impide que futuros desarrollos agreguen propiedades a modelos JSON sin `@SerializedName`, evitando regresiones silenciosas en builds ofuscados de Release.

---

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/proguard-rules.pro` | Reglas corregidas para preservar `data.remote.dto.**` y `domain.model.**`. |
| `app/src/main/java/com/k11000/monkify/data/remote/dto/Models.kt` | `@SerializedName` explícito en todos los campos de DTOs. |
| `app/src/test/java/com/k11000/monkify/data/remote/dto/DtoSerializationContractTest.kt` | Test de contrato por reflexión para DTOs frente a R8. |
| `app/src/main/java/com/k11000/monkify/data/remote/api/ServerStatusMonitor.kt` | Monitor reactivo global de conexión con política anti-spam. |
| `app/src/test/java/com/k11000/monkify/data/remote/api/ServerStatusMonitorTest.kt` | Tests unitarios de estados y silenciamiento de errores descartados. |
| `app/src/main/java/com/k11000/monkify/data/repository/SettingsRepository.kt` | `cachedServerToken` y `getServerAccessTokenImmediate()`. |
| `app/src/main/java/com/k11000/monkify/di/NetworkModule.kt` | Inyección de token síncrono en `AuthInterceptor` para cold starts. |
| `app/src/main/java/com/k11000/monkify/data/repository/NetworkTrackRepository.kt` | Cero silent catch: reporte de errores a `ServerStatusMonitor` y `AppLogger`. |
| `app/src/main/java/com/k11000/monkify/ui/components/FloatingServerStatusBanner.kt` | Aviso flotante superior interactivo (Conectando / 401 / Offline / Conectado). |
| `app/src/main/java/com/k11000/monkify/ui/components/ShimmerSkeleton.kt` | Modificador `shimmerPlaceholder` y esqueletos para QuickPlay y Álbumes. |
| `app/src/main/java/com/k11000/monkify/MainActivity.kt` | Integración de `FloatingServerStatusBanner` sobre la navegación. |
| `app/src/main/java/com/k11000/monkify/ui/screens/HomeScreen.kt` | Reemplazo de botón test por skeletons shimmer durante carga + reintentar conexión. |
| `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/HomeViewModel.kt` | Observación reactiva de `serverAccessToken` y reporte de carga de álbumes. |
| `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/LibraryViewModel.kt` | Observación reactiva de `serverAccessToken` para recarga automática. |
| `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/SettingsViewModel.kt` | Estado de verificación en vivo de conexión con Cloudflare. |
| `app/src/main/java/com/k11000/monkify/ui/screens/SettingsScreen.kt` | UI con botón "Guardar y Conectar" y badge dinámico de estado del servidor. |
| `app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt` | Fallback síncrono de token y diagnóstico HTTP 401 en reproductor. |
| `app/src/test/java/com/k11000/monkify/ui/viewmodel/SettingsViewModelTest.kt` | Pruebas de verificación activa de conexión al guardar token. |
| `app/build.gradle.kts` | Bump a versión `0.4.6` (`versionCode = 7`). |
| `docs/specs/v046-server-connection-fix/*` | Artefactos SDD: `spec.md`, `plan.md`, `tasks.md`. |

---

## 4. Pendientes explícitos para la próxima fase

- [ ] Incorporar selector de Escenario Zen (`Campos`, `Colina`, `Hongos`, `Cosmos`) en `SettingsScreen.kt`.
- [ ] Explorar conexión con endpoint APOD de la NASA (`api.nasa.gov`) en el backend para alimentar fondo `COSMOS`.
- [ ] Integrar animaciones vectoriales `.riv` en `res/raw/` para ranas interactivas en el modo Zen.

---

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Si el usuario no ingresa la clave de servidor en Ajustes, el Worker responderá 401.
  **Mitigación completada:** La app ahora muestra inmediatamente el aviso flotante *"🔒 Error 401: Clave de servidor inválida o no configurada"* con botón directo *"Ajustes"* y la tarjeta de Ajustes valida la clave en vivo con Cloudflare.
- **Riesgo:** Entornos CI/CD sin secret `KEYSTORE_FILE`.
  **Mitigación sugerida:** Almacenar el keystore de producción en Base64 en `RELEASE_KEYSTORE_BASE64` en GitHub Actions Secrets.

---

## 6. Cómo verificar que este handoff sigue vigente

```powershell
# En el cliente Android:
.\gradlew.bat testDebugUnitTest --no-configuration-cache
.\gradlew.bat assembleRelease --no-configuration-cache

# En el backend Cloudflare:
cd E:\PG\vcp-s\Monkify\cloudflare-backend
npm test
```
