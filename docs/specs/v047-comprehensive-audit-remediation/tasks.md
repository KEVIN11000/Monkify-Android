---
description: "Task list for v0.4.7 Comprehensive Audit Remediation & Froggy Weather Completion"
---

# Tasks: Remediación Integral de Auditoría v0.4.7 (Datos, Seguridad, Estabilidad y UI/UX + Froggy Clima)

**Input**: Design documents from `docs/specs/v047-comprehensive-audit-remediation/`

**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/remediation-contracts.md`

**Organization**: Las tareas están agrupadas estrictamente por los 4 bloques de prioridad (P1 → P2 → P3 → P4). Conforme a la **Regla Global §4 (Fase 2)**, cada tarea atómica (`T001` a `T012`) se ejecutará de a **UNA por turno**, verificando en terminal con `./gradlew testDebugUnitTest` antes de marcar `[x]` y avanzar a la siguiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse de forma independiente (sin dependencias pendientes)
- **[Story]**: Historia de usuario/prioridad correspondiente (`[US1]`, `[US2]`, `[US3]`, `[US4]`)

---

## Phase 1: User Story 1 - Preservación Íntegra de Biblioteca Local y Resiliencia de Base de Datos (Priority: P1) 🎯 MVP

**Goal**: Garantizar que ninguna sincronización, búsqueda o consulta online sobrescriba ni borre `isFavorite`, `isDownloaded`, `localFilePath`, `addedAt`, `playCount` o `lastPlayedAt` en Room, optimizar índices y transacciones, y mantener la URL remota separada de la ruta local.

**Independent Test**: `./gradlew testDebugUnitTest --tests "com.k11000.monkify.data.repository.OfflineFirstTrackRepositoryTest"` comprobando que pistas favoritas/descargadas conservan su estado tras recibir actualizaciones del servidor.

- [x] T001 [US1] Añadir índices `@Index` en `app/src/main/java/com/k11000/monkify/data/local/entity/TrackEntity.kt` (`isFavorite`, `isDownloaded`, `addedAt`, `lastPlayedAt`, `playCount`, `album`), preservar `streamUrl` remoto original en `TrackEntity.toDomain()` sin sobrescribirlo con `localFilePath`, e implementar migración segura de índices en `app/src/main/java/com/k11000/monkify/data/local/MonkifyDatabase.kt` y `app/src/main/java/com/k11000/monkify/di/DatabaseModule.kt`.
- [x] T002 [US1] Implementar `@Transaction suspend fun upsertRemoteTracks(tracks: List<TrackEntity>)` y consulta SQL indexada por álbum (`getTracksByAlbumName`) en `app/src/main/java/com/k11000/monkify/data/local/dao/TrackDao.kt`, actualizar `app/src/main/java/com/k11000/monkify/data/repository/OfflineFirstTrackRepository.kt` y `app/src/main/java/com/k11000/monkify/data/repository/LocalTrackRepository.kt` para usar `upsertRemoteTracks` (y emitir `emptyList()` cuando DB local y remoto estén vacíos en `getRecentTracks()`), y crear test unitario en `app/src/test/java/com/k11000/monkify/data/repository/OfflineFirstTrackRepositoryTest.kt`.

**Checkpoint P1**: La biblioteca local del usuario (favoritos, descargas, reproducciones) es 100% inmune a sobrescrituras destructivas al navegar con red.

---

## Phase 2: User Story 2 - Blindaje de Credenciales, Firma de Producción y Fronteras de Seguridad (Priority: P2)

**Goal**: Eliminar keystores y contraseñas hardcodeadas del repositorio, impedir fugas del `server_access_token` en logs, Issues de GitHub o dominios externos, proteger backups de DataStore/logs y actualizar reglas R8/ProGuard y red.

**Independent Test**: `./gradlew testDebugUnitTest --tests "com.k11000.monkify.data.remote.api.AuthInterceptorTest"` y verificación de sanitización en `AppLogger` + `git status` sin `.jks` rastreado.

- [x] T003 [P] [US2] Retirar `app/keystore/monkify-release.jks` del índice de Git (`git rm --cached`), añadir reglas de exclusión (`*.jks`, `*.keystore`, `keystore.properties`) en `.gitignore`, y eliminar contraseñas hardcodeadas de firma en `app/build.gradle.kts` (leyendo desde variables de entorno o `keystore.properties` local).
- [x] T004 [US2] Endurecer `isTrustedMonkifyHost` en `app/src/main/java/com/k11000/monkify/data/remote/api/AuthInterceptor.kt` y `app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt` (validando límite de punto `.monkify.workers.dev` y host de `BuildConfig.BASE_URL`, sin coincidir con `*.workers.dev` genéricos), redactar cabeceras sensibles en `app/src/main/java/com/k11000/monkify/di/NetworkModule.kt`, implementar sanitización automática de `?token=` y `Bearer` + formateo de fecha thread-safe asíncrono en `app/src/main/java/com/k11000/monkify/util/AppLogger.kt`, y añadir pruebas en `app/src/test/java/com/k11000/monkify/data/remote/api/AuthInterceptorTest.kt`.
- [x] T005 [P] [US2] Actualizar `app/src/main/res/xml/backup_rules.xml` y `app/src/main/res/xml/data_extraction_rules.xml` (excluyendo `domain="file"` para `datastore/` y `logs/` y corrigiendo `monkify_local.db`), restringir `app/src/main/res/xml/file_paths.xml`, crear `app/src/main/res/xml/network_security_config.xml` para limitar tráfico cleartext a `localhost`/`10.0.2.2`/`127.0.0.1` en `app/src/main/AndroidManifest.xml`, validar llamadores en `MonkifyPlaybackService.onGetSession`, y corregir `app/proguard-rules.pro` (`coil3.**` y `com.k11000.monkify.data.local.entity.**`).

**Checkpoint P2**: Cero exposición de credenciales en código, logs, GitHub Issues, dominios de terceros o copias de seguridad en la nube.

---

## Phase 3: User Story 3 - Fidelidad Operacional de Ajustes, Caché y Reproducción sin Efectos Placebo (Priority: P3)

**Goal**: Conectar el vaciado seguro y límite de `SimpleCache`, el enrutamiento físico de salida de audio (`selectDevice`), el tema dinámico (`dynamicColor`), la versión real (`BuildConfig.VERSION_NAME`) y erradicar las URLs de prueba de Rick Astley y Pixabay.

**Independent Test**: `./gradlew testDebugUnitTest --tests "com.k11000.monkify.playback.PlayerControlsTest" --tests "com.k11000.monkify.ui.viewmodel.SettingsViewModelTest"` validando comportamiento real sobre las clases de producción.

- [x] T006 [US3] Conectar `SimpleCache` en `app/src/main/java/com/k11000/monkify/di/PlaybackModule.kt` y `app/src/main/java/com/k11000/monkify/data/repository/SettingsRepository.kt` para que el evictor respete `cacheLimitMb` y `clearMediaCache()` libere entradas de forma segura mediante `simpleCache.keys` / `removeResource` sin borrar el directorio en caliente; y conectar `dynamicColor = settingsState.dynamicColor` en `app/src/main/java/com/k11000/monkify/MainActivity.kt` y `app/src/main/java/com/k11000/monkify/lockscreen/LockScreenActivity.kt`.
- [x] T007 [US3] Implementar enrutamiento real de dispositivo de salida de audio en `PlayerViewModel.selectDevice()` (`app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt`), eliminar el fallback de audio de Pixabay cuando no haya fuente válida, eliminar el enlace hardcodeado de Rick Astley en `app/src/main/java/com/k11000/monkify/ui/screens/SearchScreen.kt` (mostrando aviso de portapapeles vacío), unificar `BuildConfig.VERSION_NAME` en `Models.kt`, `SettingsViewModel.kt`, `HomeScreen.kt` y `PlayerViewModel.kt`, y reemplazar los tests tautológicos en `app/src/test/java/com/k11000/monkify/playback/PlayerControlsTest.kt`.

**Checkpoint P3**: Todos los controles de configuración y reproducción ejecutan acciones reales verificadas sin efectos placebo ni datos falsos.

---

## Phase 4: User Story 4 - Fondo Climático Froggy + Rive Completo, Sincronización de Estado, Fluidez 60fps y UI/UX Edge-to-Edge (Priority: P4)

**Goal**: Hacer que el fondo climático dinámico de **Froggy** funcione de extremo a extremo (con ubicación aproximada real, selector/rotación de los 3 escenarios `FIELDS`, `HILL`, `MUSHROOM` = 60 fondos WebP, partículas atmosféricas + rana animada / soporte Rive `.riv`, indicador `temperatureSummary` y sincronización en `HomeScreen`, `FullScreenPlayer` y `LockScreenActivity`), unificar `ServerStatusMonitor`, mover I/O fuera del Main Thread y corregir recomposiciones y `WindowInsets`.

**Independent Test**: `./gradlew testDebugUnitTest` completo y Gate Visual de UI comprobando los escenarios climáticos de Froggy, la barra de estado y los márgenes de navegación.

- [x] T008 [US4] Completar el subsistema de ubicación y escenarios de Froggy: solicitar `Manifest.permission.ACCESS_COARSE_LOCATION` en `app/src/main/java/com/k11000/monkify/MainActivity.kt` (refrescando `WeatherRepository` al concederse), persistir coordenadas manuales en `app/src/main/java/com/k11000/monkify/data/local/LocalWeatherStore.kt` y `app/src/main/java/com/k11000/monkify/data/repository/WeatherRepositoryImpl.kt`, y agregar la preferencia `froggyScenery` (`AUTO`, `FIELDS`, `HILL`, `MUSHROOM`) en `app/src/main/java/com/k11000/monkify/data/repository/SettingsRepository.kt` y `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/SettingsViewModel.kt`.
- [x] T009 [US4] Activar la animación dinámica completa de Froggy y los 60 escenarios climáticos en UI: actualizar `app/src/main/java/com/k11000/monkify/ui/components/FroggyAssetCatalog.kt` y `app/src/main/java/com/k11000/monkify/ui/components/ZenBackground.kt` (renderizando sobre los 60 fondos WebP tanto los efectos atmosféricos de lluvia/nieve/sol/niebla/luciérnagas como el personaje animado de Froggy reaccionando al clima y la música + contenedor Rive `.riv` conservado, sin recomposiciones innecesarias), añadir el selector de escenario Froggy (`Automático 🔄`, `Campos 🌾`, `Colina 🌳`, `Hongos 🍄`) e indicador climático en `app/src/main/java/com/k11000/monkify/ui/screens/SettingsScreen.kt`, y conectar `scenery`, `weatherCondition`, `timeOfDay` y `temperatureSummary` en `HomeScreen.kt`, `FullScreenPlayer.kt` y `LockScreenActivity.kt`.
- [x] T010 [US4] Consolidar `ServerStatusMonitor` en una única instancia `@Singleton` en `app/src/main/java/com/k11000/monkify/data/remote/api/ServerStatusMonitor.kt` (sincronizando el cierre manual `✕` del banner en toda la app), eliminar el listener duplicado de `STATE_ENDED` Shuffle en `PlayerViewModel.kt` (dejándolo únicamente en `MonkifyPlaybackService.kt`), mover las lecturas de archivos locales de `playQueue()`, `LibraryViewModel.totalStorageBytes` y `DownloadRepositoryImpl` a `Dispatchers.IO`, y limpiar archivos sin uso (`MockTrackRepository.kt`, `ApkInstaller.kt`, parámetro `settingsViewModel` sin uso en `MiniPlayer.kt`).
- [x] T011 [US4] Aislar la recomposición de `1Hz` del progreso de reproducción en `app/src/main/java/com/k11000/monkify/ui/screens/FullScreenPlayer.kt` y `app/src/main/java/com/k11000/monkify/lockscreen/LockScreenActivity.kt`, conectar `isLoadingLyrics` a `LyricsView` en `FullScreenPlayer.kt`, y corregir los `WindowInsets` (`navigationBars` y `statusBars`) en `FullScreenPlayer.kt`, `LockScreenActivity.kt` y `LibraryScreen.kt`.
- [x] T012 [US4] Pulir consistencia y ergonomía en pantallas de catálogo: resaltar pista activa por `track.id == currentTrackId` e incorporar acción por lotes `"Añadir a Playlist"` en `app/src/main/java/com/k11000/monkify/ui/screens/LibraryScreen.kt`, implementar el filtro `"Música"` en `app/src/main/java/com/k11000/monkify/ui/screens/HomeScreen.kt`, unificar altura (`>= 52.dp`) y colores de `TextField` buscadores en `SearchScreen.kt`, `PlaylistDetailScreen.kt` y `AlbumDetailScreen.kt`, corregir orden de `.background()` antes de `.clickable()`, asegurar objetivos táctiles `>= 48dp` en `MiniPlayer.kt`/`QueueBottomSheet.kt` y ejecutar verificación integral (`./gradlew testDebugUnitTest` + Gate Visual).

---

## Dependencies & Execution Order

- **Phase 1 (P1 - Base de Datos: `T001`–`T002`)**: Sin dependencias previas; asegura la integridad de datos desde el primer paso.
- **Phase 2 (P2 - Seguridad: `T003`–`T005`)**: Continúa inmediatamente tras P1.
- **Phase 3 (P3 - Estabilidad y Cero Placebos: `T006`–`T007`)**: Continúa tras P2.
- **Phase 4 (P4 - Froggy Clima Dinámico, Rendimiento y UI/UX: `T008`–`T012`)**: Cierra el ciclo de remediación y ejecuta el Gate Visual.
