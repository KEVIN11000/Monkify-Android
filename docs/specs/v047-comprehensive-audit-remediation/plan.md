# Implementation Plan: Remediación Integral de Auditoría v0.4.7 (Datos, Seguridad, Estabilidad y UI/UX + Froggy Clima)

**Branch**: `refine/v0.4` | **Date**: 2026-10-30 | **Spec**: [spec.md](file:///e:/PG/vcp-s/Monkify-Android-Refine/docs/specs/v047-comprehensive-audit-remediation/spec.md)

**Input**: Feature specification from `docs/specs/v047-comprehensive-audit-remediation/spec.md`

---

## Summary

Remediación integral en **4 bloques priorizados** de las inconsistencias detectadas en la auditoría técnica de Monkify Android (`v0.4.6`), más la activación completa de punta a punta del **fondo climático dinámico de Froggy con soporte Rive (`.riv`)**:

1. **Prioridad 1 (P1 - Base de Datos y Capa de Datos):** Reemplazar la inserción destructiva (`OnConflictStrategy.REPLACE` con `isFavorite = false, isDownloaded = false`) en `OfflineFirstTrackRepository` y `TrackDao` por una operación transaccional no destructiva (`upsertRemoteTracks`) que preserve los favoritos, descargas, rutas locales e historial de reproducciones del usuario; añadir índices `@Index` en `TrackEntity`; separar `streamUrl` remoto de la resolución local en `toDomain()`; y asegurar emisión vacía en `getRecentTracks()` ante arranque en frío sin red.
2. **Prioridad 2 (P2 - Seguridad y Privacidad):** Desvincular `app/keystore/monkify-release.jks` del control de versiones e ignorar keystores en `.gitignore`, eliminando contraseñas hardcodeadas en `app/build.gradle.kts`; redactar automáticamente parámetros `?token=` y cabeceras `Bearer` en `AppLogger` y `HttpLoggingInterceptor`; endurecer la validación de host en `AuthInterceptor` y `PlayerViewModel` contra el host de `BuildConfig.BASE_URL`; excluir `datastore/` y `logs/` en `backup_rules.xml` y `data_extraction_rules.xml`; restringir tráfico HTTP en claro a localhost mediante `network_security_config.xml`; validar clientes en `MonkifyPlaybackService.onGetSession`; y actualizar `proguard-rules.pro` (`coil3.**` y `data.local.entity.**`).
3. **Prioridad 3 (P3 - Estabilidad Operacional y Cero Efectos Placebo):** Liberar recursos de `SimpleCache` mediante su API segura (`removeResource`) en `clearMediaCache()` sin destruir su directorio en caliente y conectar `cacheLimitMb` al evictor de `SimpleCache`; implementar el enrutamiento físico real de salida de audio en `PlayerViewModel.selectDevice()`; conectar `dynamicColor` a `MyApplicationTheme` en `MainActivity` y `LockScreenActivity`; eliminar el enlace hardcodeado de Rick Astley en `SearchScreen` y el audio fallback de Pixabay en `PlayerViewModel`; y unificar la versión reportada usando `BuildConfig.VERSION_NAME`.
4. **Prioridad 4 (P4 - Fondo Climático Froggy Completo, Estado Unificado, Rendimiento I/O y UI/UX Edge-to-Edge):**
   - **Froggy + Clima + Rive:** Conservar `app.rive:rive-android`; solicitar `ACCESS_COARSE_LOCATION` en tiempo de ejecución y persistir coordenadas manuales en `LocalWeatherStore`; habilitar en `SettingsRepository`/`SettingsScreen` el selector de escenario de Froggy (`AUTO`, `FIELDS`, `HILL`, `MUSHROOM` para desbloquear los 60 fondos WebP de `FroggyAssetCatalog`); mostrar la píldora climática (`temperatureSummary`) cuando Froggy esté activo; renderizar la animación dinámica de Froggy junto con las partículas climáticas sobre los fondos WebP oficiales en `ZenBackground.kt`; y pasar `timeOfDay`, `weatherCondition` y `scenery` a `ZenBackground` también en `LockScreenActivity`.
   - **Rendimiento y UI/UX:** Unificar `ServerStatusMonitor` en una única instancia real; eliminar el listener duplicado de Shuffle en `PlayerViewModel`; mover accesos a disco de `playQueue()`, `totalStorageBytes` y `AppLogger` a `Dispatchers.IO` con formateo de fecha thread-safe; aislar recomposiciones de `1Hz` en `FullScreenPlayer`; corregir `WindowInsets.navigationBars` en `FullScreenPlayer`, `LockScreenActivity` y `LibraryScreen`; y conectar `isLoadingLyrics`, comparación por `track.id`, acción por lotes "Añadir a Playlist", filtro "Música", altura de `TextField` y tamaños táctiles `>= 48dp`.

---

## Technical Context

**Language/Version**: Kotlin 2.2.10 / JDK 17  
**Primary Dependencies**: Jetpack Compose (BOM 2025.09.00), Media3 ExoPlayer & Session (`1.8.0`), Room (`2.7.2`), Hilt (`2.57.1`), Retrofit (`3.0.0`) + OkHttp (`5.1.0`), Coil 3 (`3.1.0`), Rive Android (`9.13.4`), Play Services Location (`21.3.0`), DataStore Preferences (`1.1.7`)  
**Storage**: Room SQLite (`monkify_local.db`), Jetpack Preferences DataStore (`monkify_settings`, `weather_cache`), Media3 `SimpleCache` (`cacheDir/media_cache`)  
**Testing**: JUnit 4, MockK (`1.13.16`), Kotlinx Coroutines Test (`1.10.2`), Robolectric / Room unit tests (`./gradlew testDebugUnitTest`)  
**Target Platform**: Android API 24 (minSdk) – API 36 (targetSdk / compileSdk)  
**Project Type**: Mobile Application (Android single-module `:app`)  
**Performance Goals**: 60 fps constantes en animaciones Zen y reproductor, 0 lecturas/escrituras de disco en el Main Thread (`Dispatchers.Main`)  
**Constraints**: Offline-first resiliente, preservación estricta de biblioteca local en sincronizaciones remotas, cero fugas de credenciales en logs o backups  
**Scale/Scope**: ~65 archivos fuente Kotlin, 60 fondos WebP climáticos de Froggy, 7 pantallas principales en Compose

---

## Constitution Check (Regla Global del Usuario)

*GATE: Verificado antes de Fase 0 y re-evaluado tras el diseño de Fase 1.*

- [x] **1. Calidad y Verificación Empírica ante Todo:** Cada tarea en `tasks.md` incluye verificación real mediante `./gradlew testDebugUnitTest`, y los cambios visuales de UI/Compose se someterán al Gate Visual obligatorio.
- [x] **2. Política de Fuga Cero e Higiene Estricta de Git:** Prohibición absoluta de `git add .` / `git add -A`. Retiro de `monkify-release.jks` del índice y eliminación de contraseñas hardcodeadas.
- [x] **3. Paridad y Aislamiento:** Todas las pruebas unitarias operan con mocks/fakes controlados sin tocar servidores vivos.
- [x] **4. Ciclo de Vida SDD (`docs/specs/v047-comprehensive-audit-remediation/`):** `spec.md`, `plan.md` y `tasks.md` generados en Fase 0 antes de modificar cualquier archivo `.kt` o `.xml`. Ejecución atómica de una tarea por turno tras la aprobación del usuario.

---

## Project Structure

### Documentation (this feature)

```text
docs/specs/v047-comprehensive-audit-remediation/
├── spec.md                        # Especificación de requerimientos (P1 a P4)
├── plan.md                        # Plan de arquitectura e implementación
├── research.md                    # Decisiones técnicas y análisis de alternativas
├── data-model.md                  # Esquema Room, reglas de upsert y estados
├── quickstart.md                  # Guía de verificación empírica
├── contracts/
│   └── remediation-contracts.md   # Contratos de seguridad, red y ZenBackground
└── tasks.md                       # Lista de tareas atómicas secuenciales
```

### Source Code (repository root)

```text
app/
├── build.gradle.kts
├── proguard-rules.pro
├── src/main/
│   ├── AndroidManifest.xml
│   ├── res/xml/
│   │   ├── backup_rules.xml
│   │   ├── data_extraction_rules.xml
│   │   ├── file_paths.xml
│   │   └── network_security_config.xml
│   └── java/com/k11000/monkify/
│       ├── MainActivity.kt
│       ├── MonkifyPlaybackService.kt
│       ├── data/
│       │   ├── local/
│       │   │   ├── LocalWeatherStore.kt
│       │   │   ├── MonkifyDatabase.kt
│       │   │   ├── dao/TrackDao.kt
│       │   │   └── entity/TrackEntity.kt
│       │   ├── remote/
│       │   │   ├── api/AuthInterceptor.kt
│       │   │   ├── api/ServerStatusMonitor.kt
│       │   │   └── dto/Models.kt
│       │   └── repository/
│       │       ├── DownloadRepositoryImpl.kt
│       │       ├── LocalTrackRepository.kt
│       │       ├── OfflineFirstTrackRepository.kt
│       │       ├── SettingsRepository.kt
│       │       └── WeatherRepositoryImpl.kt
│       ├── di/
│       │   ├── DatabaseModule.kt
│       │   ├── NetworkModule.kt
│       │   └── PlaybackModule.kt
│       ├── lockscreen/LockScreenActivity.kt
│       ├── playback/PlayerViewModel.kt
│       ├── ui/
│       │   ├── components/
│       │   │   ├── AddToPlaylistDialog.kt
│       │   │   ├── FloatingServerStatusBanner.kt
│       │   │   ├── FroggyAssetCatalog.kt
│       │   │   ├── MiniPlayer.kt
│       │   │   ├── QueueBottomSheet.kt
│       │   │   └── ZenBackground.kt
│       │   └── screens/
│       │       ├── AlbumDetailScreen.kt
│       │       ├── FullScreenPlayer.kt
│       │       ├── HomeScreen.kt
│       │       ├── LibraryScreen.kt
│       │       ├── PlaylistDetailScreen.kt
│       │       ├── SearchScreen.kt
│       │       ├── SettingsScreen.kt
│       │       └── viewmodel/
│       │           ├── HomeViewModel.kt
│       │           ├── LibraryViewModel.kt
│       │           └── SettingsViewModel.kt
│       └── util/AppLogger.kt
└── src/test/java/com/k11000/monkify/
```

**Structure Decision**: Se mantiene la arquitectura limpia modular por capas (`data`, `domain`, `di`, `playback`, `lockscreen`, `ui`, `util`) dentro del módulo `:app`, interviniendo de forma quirúrgica los archivos afectados por cada prioridad.
