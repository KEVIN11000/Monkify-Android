# Investigación Técnica: Remediación Integral de Auditoría (`v0.4.7`)

**Feature:** `v047-comprehensive-audit-remediation`  
**Fecha:** 2026-10-30  
**Estado:** Completado

---

## 1. Decisiones Técnicas por Bloque de Prioridad

### 1.1 Prioridad 1 (P1): Preservación de Biblioteca Local y Resiliencia en Room
- **Decision**: Implementar sincronización no destructiva (*Non-Destructive Upsert*) en `TrackDao` y `OfflineFirstTrackRepository`, y separar `remoteStreamUrl` de `localFilePath` en el mapeo a dominio.
- **Rationale**: `TrackDao.insertTracks` usaba `@Insert(onConflict = OnConflictStrategy.REPLACE)`. En SQLite, `REPLACE` hace `DELETE` seguido de `INSERT`, por lo que al mapear `remote.map { it.toEntity(isFavorite = false, isDownloaded = false) }` en `OfflineFirstTrackRepository` se destruían `isFavorite`, `isDownloaded`, `localFilePath`, `playCount` y `lastPlayedAt`. Al delegar la persistencia de lotes remotos a un método transaccional `@Transaction suspend fun upsertRemoteTracks(tracks: List<TrackEntity>)` que preserva los campos de usuario de las filas existentes y añadir `@Index` sobre `isFavorite`, `isDownloaded`, `addedAt`, `lastPlayedAt`, `playCount` y `album`, se garantiza integridad total y lecturas indexadas.
- **Alternatives considered**:
  - `@Upsert` de Room: Descartado porque `@Upsert` sobrescribe todas las columnas de la entidad con los valores del objeto entrante (incluyendo `isFavorite = false`), por lo que una consulta/actualización selectiva o fusión en `@Transaction` es obligatoria.

### 1.2 Prioridad 2 (P2): Blindaje de Credenciales, Firma, Red y Media3
- **Decision**:
  1. Retirar `app/keystore/monkify-release.jks` del índice de Git (`git rm --cached`), ignorar `*.jks`/`*.keystore`/`keystore.properties` en `.gitignore` y leer credenciales desde `System.getenv(...)` o `keystore.properties` local sin contraseñas hardcodeadas en `app/build.gradle.kts`.
  2. Incorporar `androidx.media3:media3-datasource-okhttp` (o inyectar cabeceras dinámicas desde `AuthInterceptor`/`DefaultHttpDataSource.Factory.setDefaultRequestProperties`) y sanitizar URLs en `AppLogger` mediante expresión regular `([?&]token=)[^&\s]+` → `$1[REDACTED]`.
  3. Validar estrictamente el host en `AuthInterceptor.isTrustedMonkifyHost(host)` comparando contra el host de `BuildConfig.BASE_URL` y subdominios legítimos con punto delimitador (`.monkify.workers.dev`), bloqueando `evil-monkify.workers.dev` o `*.workers.dev` genéricos.
  4. Excluir `domain="file" path="datastore/"` y `domain="file" path="logs/"` en `backup_rules.xml` y `data_extraction_rules.xml`, añadir `network_security_config.xml` (permitiendo cleartext solo en `10.0.2.2`, `localhost`, `127.0.0.1`), restringir `MonkifyPlaybackService.onGetSession` a paquetes confiables/propios o del sistema, y actualizar `proguard-rules.pro` (`coil3.**` y `data.local.entity.**`).
- **Rationale**: Elimina de raíz las fugas de secretos hacia GitHub Issues, Logcat, backups en nube y dominios externos sin romper el caché de `SimpleCache` cuando cambia el token.
- **Alternatives considered**:
  - Cifrar todo el archivo `monkify.log`: Innecesario y contraproducente para diagnóstico del usuario si se redactan proactivamente los tokens y cabeceras antes de escribir cualquier línea.

### 1.3 Prioridad 3 (P3): Fidelidad Operacional (Caché, Salida de Audio, Material You, Sin URLs Falsas)
- **Decision**:
  1. **Caché Segura (`SimpleCache`):** En `SettingsRepository.clearMediaCache()`, iterar sobre `simpleCache.keys` y llamar `simpleCache.removeResource(key)` en lugar de borrar la carpeta `media_cache` por debajo de `SimpleCache` en caliente. Configurar el evictor de `SimpleCache` leyendo el límite configurado en `SettingsRepository.getCacheLimitBytesImmediate()`.
  2. **Enrutamiento Real de Salida de Audio (`selectDevice`):** En `PlayerViewModel.selectDevice(deviceId)`, localizar el `AudioDeviceInfo` real en `AudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)` y aplicar el enrutamiento real (vía `AudioManager` / comunicación de dispositivo preferido en el reproductor).
  3. **Material You (`dynamicColor`):** Pasar `dynamicColor = settingsState.dynamicColor` a `MyApplicationTheme` en `MainActivity` y `LockScreenActivity`.
  4. **Eliminación de Rickroll / Pixabay y Versionado Dinámico:** En `SearchScreen`, mostrar snackbar/mensaje cuando el portapapeles esté vacío en lugar de enviar la URL de Rick Astley; en `PlayerViewModel`, no usar el MP3 de Pixabay cuando `streamUrl` esté vacío; y usar `BuildConfig.VERSION_NAME` en `Models.kt`, `SettingsViewModel`, `HomeScreen` y `PlayerViewModel`.
- **Rationale**: Cumple el principio de cero simulaciones y evita corromper el estado de SQLite/disco de `SimpleCache`.
- **Alternatives considered**: Ninguna; los placeholders de prueba en código de producción deben erradicarse por completo.

### 1.4 Prioridad 4 (P4): Fondo Climático Froggy + Rive Completo, Rendimiento I/O y UI/UX Edge-to-Edge
- **Decision**:
  1. **Fondo Climático Froggy + Rive 100% Funcional:**
     - Conservar `app.rive:rive-android` y la estructura multicapa de `ZenBackground.kt`.
     - Solicitar `Manifest.permission.ACCESS_COARSE_LOCATION` junto con `POST_NOTIFICATIONS` en `MainActivity` (y al activar Froggy en Ajustes) y refrescar `WeatherRepository` al concederse el permiso; persistir la ubicación manual en `LocalWeatherStore`.
     - Añadir en `SettingsRepository`, `SettingsViewModel` y `SettingsScreen` el selector de escenario de Froggy (`AUTO`, `FIELDS`, `HILL`, `MUSHROOM`) para desbloquear los **60 fondos oficiales WebP** de `FroggyAssetCatalog`, además de mostrar el resumen climático (`CurrentWeather.temperatureSummary`, ej. `22° · Despejado`) cuando Froggy está activo.
     - En `ZenBackground.kt`, cuando `engine == "FROGGY"`, renderizar sobre los fondos oficiales WebP tanto los efectos atmosféricos dinámicos (`drawFroggyAtmosphericOverlay`) como la animación viva del personaje Froggy / contenedor Rive (`.riv`), y pasar `timeOfDay`, `weatherCondition` y `scenery` también en `LockScreenActivity`.
  2. **Unificación de `ServerStatusMonitor` y Shuffle:** Hacer que el `@Provides @Singleton` de Hilt devuelva `ServerStatusMonitor.shared` (o eliminar la instancia duplicada) para que el cierre manual (`✕`) funcione en toda la app, y dejar el listener de `STATE_ENDED` para Shuffle únicamente en `MonkifyPlaybackService`.
  3. **Hilo Principal Libre de I/O y Recomposiciones Aisladas:** Ejecutar la resolución de archivos locales de `playQueue()` y `totalStorageBytes` en `Dispatchers.IO`, sincronizar el formateo de fecha en `AppLogger`, y corregir `WindowInsets.navigationBars` en `FullScreenPlayer`, `LockScreenActivity` y `LibraryScreen`.
- **Rationale**: Cumple exactamente la visión de producto acordada para el modo Zen climático con Froggy y garantiza fluidez a 60fps sin bloqueos en el hilo principal.
- **Alternatives considered**: Eliminar Rive fue descartado expresamente por decisión de arquitectura y requerimiento del usuario, ya que el sistema climático de Froggy está diseñado para operar con sus recursos y soporte Rive.
