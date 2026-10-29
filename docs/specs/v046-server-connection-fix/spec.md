# Especificación Técnica: v0.4.6 Recuperación de Producción, Resiliencia R8, Aviso Flotante Global y Estados de Carga

## 1. Contexto y Análisis de Causa Raíz (RCA)
Tras el lanzamiento de la versión `v0.4.5` (Hardening de Seguridad, Protección de Servidor Cloudflare y Optimización R8), el flujo en producción quedó roto ("sin conexión / no conecta tras ingresar el token") y sin retroalimentación visual sobre qué estaba ocurriendo en segundo plano.

La auditoría forense de los binarios de Release (`app/build/outputs/mapping/release/usage.txt` y `mapping.txt`), el código cliente y el backend Cloudflare reveló **6 causas raíz concurrentes**:

1. **RCA-01 (CRÍTICA - Bloqueo Total en Release APK por R8/Gson):**
   En [`app/proguard-rules.pro`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/proguard-rules.pro#L13), la regla de preservación se escribió apuntando a un paquete inexistente (`-keep class com.k11000.monkify.data.remote.model.** { *; }`), mientras que los DTOs reales residen en `com.k11000.monkify.data.remote.dto.**`. Además, en [`BackendTrackDto`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/data/remote/dto/Models.kt#L19-L28), campos vitales como `id`, `title`, `artist` y `album` carecían de `@SerializedName`. Como el constructor se invoca por reflexión desde Gson, R8 consideró que los campos nunca eran asignados, los eliminó del bytecode (`usage.txt` líneas 51338-51363) y optimizó los getters para devolver `null` constante. Al recibir el JSON del servidor, Gson no pudo hidratar los campos y `it.toDomain()` lanzó `NullPointerException`, el cual fue silenciado por `catch (e: Exception) { emptyList() }` en `NetworkTrackRepository`.
2. **RCA-02 (ALTA - Condición de Carrera en `AuthInterceptor` al Arrancar):**
   En [`NetworkModule.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/di/NetworkModule.kt#L35-L43), `AuthInterceptor` inicializa `tokenRef = AtomicReference("")` y lanza una corrutina asíncrona en `Dispatchers.IO` para leer DataStore. Sin embargo, `HomeViewModel` dispara `loadData()` inmediatamente en su `init {}`, ejecutando las peticiones HTTP a `/api/v1/tracks` y `/api/v1/albums` antes de que DataStore emita el token guardado, resultando en respuestas `401 Unauthorized` en el arranque.
3. **RCA-03 (ALTA - Ausencia de Reconexión Reactiva al Guardar el Token):**
   Cuando el usuario guarda su `Server Access Token` en `SettingsScreen`, `SettingsViewModel.setServerAccessToken()` actualiza DataStore pero **no dispara una recarga** del catálogo en `HomeViewModel`, `LibraryViewModel` ni `SyncScheduler`, dejando las pantallas vacías ("sin conexión") hasta que se cierre o fuerce la actualización manual.
4. **RCA-04 (CRÍTICA UX - Silencio Visual, Falta de Aviso Flotante Global y Estados de Carga Incompletos):**
   Los bloques `catch (_: Exception) { emptyList() }` en `NetworkTrackRepository`, `OfflineFirstTrackRepository` y `HomeViewModel` ocultaron por completo tanto el `HTTP 401` como el `NullPointerException` de R8 o la falta de red. Peor aún, en [`HomeScreen.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/ui/screens/HomeScreen.kt#L396-L411), mientras `isLoading == true` y `quickItems` está vacío, la interfaz muestra directamente un botón estático `"Play Test Track"` en lugar de una interfaz de carga clara (skeletons/indicador de sincronización), y no existe ningún **aviso flotante global** que informe al usuario si la app está intentando conectar, sincronizando el catálogo, o si falló por un motivo específico (ej. `HTTP 401 Clave de Servidor faltante o inválida`, `Timeout`, `Modo Offline`).
5. **RCA-05 (MEDIA - ExoPlayer sin Diagnóstico Explícito de HTTP 401 ni Fallback Síncrono de Token):**
   En [`PlayerViewModel.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt), si `currentServerToken` aún no cargó al armar la cola de reproducción, la URL sale sin `?token=` y `parsePlaybackError` devuelve un mensaje genérico `ERROR_CODE_IO_BAD_HTTP_STATUS` sin explicar que el servidor rechazó el token (`401`).
6. **RCA-06 (PROCESO - Ausencia de Test de Contrato R8/Gson):**
   No existía un test automatizado que verificara por reflexión que todos los campos de los DTOs tienen `@SerializedName` y sobreviven a la ofuscación/optimización de R8.

---

## 2. Alcance (In-Scope) y Fase 1: Contratos de Interfaz Visual y UX

### 2.1 Reparación Quirúrgica de R8 / ProGuard y DTOs (`Release-Safe Serialization`)
- Actualizar [`app/proguard-rules.pro`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/proguard-rules.pro) para preservar explícitamente:
  - `-keep class com.k11000.monkify.data.remote.dto.** { *; }`
  - `-keep class com.k11000.monkify.domain.model.** { *; }`
- Anotar con `@SerializedName` explícito todos los campos de las clases DTO en [`Models.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/data/remote/dto/Models.kt).

### 2.2 Monitor Global de Conexión (`ServerStatusMonitor`) y Sincronización Síncrona del Token
- Crear `ServerStatusMonitor` (`@Singleton`) que centraliza el estado en tiempo real de todas las operaciones contra el backend Cloudflare:
  - `Idle`: Estado inicial.
  - `Connecting(val label: String)`: Petición en curso (`"Conectando con el servidor..."` / `"Sincronizando catálogo..."`).
  - `Connected(val message: String, val timestampMs: Long)`: Conexión verificada (`"Conectado al servidor"`).
  - `Unauthorized401(val detail: String)`: Rechazo de autenticación (`"Error 401: Clave de servidor ausente o inválida"`).
  - `NetworkError(val reason: String)`: Fallo de red o servidor (`"Sin conexión al servidor (Modo local): <motivo>"`).
- En [`SettingsRepository.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/data/repository/SettingsRepository.kt) y [`NetworkModule.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/di/NetworkModule.kt):
  - Garantizar que `AuthInterceptor` tenga acceso inmediato al token almacenado en DataStore (`getServerAccessTokenImmediate()`) para eliminar la condición de carrera al arranque.

### 2.3 Contrato Visual Fase 1: Aviso Flotante Global (`FloatingServerStatusBanner`) e Interfaces de Carga Prioritarias
1. **Aviso Flotante Global (`FloatingServerStatusBanner.kt`):**
   - Ubicado en la parte superior de la jerarquía visual en [`MainActivity.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/MainActivity.kt), visible sobre cualquier pestaña sin bloquear la navegación inferior ni el MiniPlayer.
   - **Estados Visuales del Aviso Flotante:**
     - **Cargando / Conectando (`Connecting`):** Cápsula flotante translúcida con spinner animado y texto `"Conectando con el servidor..."` o `"Sincronizando catálogo..."`.
     - **Error de Autorización (`Unauthorized401`):** Aviso flotante destacado (tono ámbar/rojo de alerta) con el motivo exacto (`"Acceso denegado (HTTP 401): Configura o verifica tu Clave de Servidor"`) y botón de acción rápida **"Ir a Ajustes"** (o **"Reintentar"**).
     - **Error de Red / Servidor (`NetworkError`):** Aviso flotante informativo (`"Sin conexión al servidor: <motivo detallado> · Usando caché local"`) con botón **"Reintentar"** y opción de descartar temporalmente (`✕`).
     - **Recuperación / Conectado (`Connected`):** Tras recuperarse de un error o al validar un token, muestra durante 3.5 segundos un aviso flotante verde `"✓ Conectado al servidor Monkify"` y luego se oculta suavemente con animación (`AnimatedVisibility`) para no estorbar la experiencia musical.
2. **Interfaces de Carga Explícitas en Pantallas (`HomeScreen`, `LibraryScreen`, `SettingsScreen`):**
   - En [`HomeScreen.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/ui/screens/HomeScreen.kt): Mientras `isLoading == true` y no hay canciones cargadas aún, renderizar tarjetas esqueleto/indicador visual (`"Sincronizando biblioteca con el servidor..."`) en lugar de mostrar prematuramente el botón de fallback `"Play Test Track"`. Si terminó de cargar y sigue vacío por un error, mostrar una tarjeta de estado clara con el motivo y botón **"Reintentar conexión"**.
   - En [`SettingsScreen.kt`](file:///e:/PG/vcp-s/Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/ui/screens/SettingsScreen.kt): En la tarjeta de **"Autenticación de Servidor"**, añadir estado de carga en vivo al pulsar **"Guardar y Probar"** (`Probando conexión con Cloudflare...` -> `✓ Conectado (Catálogo accesible)` o `✗ Error 401 / Red`).

---

## 3. Fuera de Alcance (No-Scope)
- Modificación de esquemas de base de datos D1 o buckets R2 en Cloudflare.
- Nuevos motores visuales Zen (Hongos / NASA APOD) que pertenecen a otra feature posterior.

---

## 4. Criterios de Aceptación Verificables
1. **Verificación de Bytecode R8 (`usage.txt`):** Al ejecutar `.\gradlew.bat assembleRelease`, `BackendTrackDto`, `PlaylistSummaryDto` y `AlbumSummaryDto` **no** tienen sus campos `id`, `title`, `artist`, `album` eliminados en `app/build/outputs/mapping/release/usage.txt`.
2. **Eliminación de Race Condition:** La primera llamada de red de `AuthInterceptor` tras iniciar el proceso ya incluye el `serverAccessToken` persistido.
3. **Aviso Flotante Siempre Informado:** En cualquier estado (conectando, error 401, timeout/sin red, o reconexión exitosa), el usuario ve un aviso flotante claro con el motivo exacto y acción directa, sin quedarse colgado en silencio.
4. **Interfaces de Carga y Reconexión Reactiva:** `HomeScreen` muestra interfaz de carga real mientras sincroniza, y al actualizar `serverAccessToken` en Ajustes, `HomeViewModel` y `LibraryViewModel` recargan automáticamente el catálogo.
5. **Gate Visual y Suite de Pruebas Completa:** Previews de Compose renderizadas y verificadas visualmente para todos los estados del aviso flotante y de carga, más 100% de tests unitarios (`testDebugUnitTest`) en verde.
