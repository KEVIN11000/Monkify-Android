# Fase v0.4.7 — Remediación Integral de Auditoría (P1-P4), Refinamiento Ergonómico, Froggy Portrait y Release v0.4.7

**Fecha:** 2026-11-04  
**Estado:** ✅ Completado e Integrado (Release v0.4.7 publicado en GitHub)

---

## 1. Logros y Remediaciones Implementadas

### P1 — Preservación Íntegra de Datos y Room Database
- Añadidos 6 índices `@Index` en `TrackEntity.kt` (`isFavorite`, `isDownloaded`, `addedAt`, `lastPlayedAt`, `playCount`, `album`).
- Migración Room v1 -> v2 configurada de forma segura en `MonkifyDatabase.kt` y `DatabaseModule.kt`.
- Implementado `@Transaction suspend fun upsertRemoteTracks(...)` en `TrackDao.kt` para blindar campos locales (`isFavorite`, `isDownloaded`, `localFilePath`, `playCount`, etc.) de sobrescrituras al sincronizar con el backend.
- Preservación explícita de `streamUrl` remoto en `TrackEntity.toDomain()`, manteniendo `localFilePath` desacoplado.

### P2 — Blindaje de Seguridad y Fuga Cero
- Keystore oficial permanente (`app/keystore/monkify-release.jks`) retirado del tracking de Git y añadido a `.gitignore` junto con `*.jks`, `*.keystore` y `keystore.properties`.
- Hardcoded signing passwords eliminadas de `app/build.gradle.kts`, leyendo de variables de entorno o archivo local no versionado, con fallback para CI.
- Validación de host de confianza (`isTrustedMonkifyHost`) endurecida con límites de punto estricto (`.monkify.workers.dev`) en `AuthInterceptor.kt` y `PlayerViewModel.kt`.
- `AppLogger.kt` con sanitización automática de tokens (`?token=***`, `Bearer ***`) y formato de fecha seguro.
- Reglas de backup (`backup_rules.xml`, `data_extraction_rules.xml`) configuradas para excluir `datastore/` y `logs/`.
- Creado `network_security_config.xml` para restringir tráfico claro (cleartext) únicamente a `localhost` y `127.0.0.1`.

### P3 — Fidelidad Operacional y Cero Efectos Placebo
- `PlaybackCache.kt` y `PlaybackModule.kt` conectados a `SimpleCache` con límite dinámico de almacenamiento (`cacheLimitMb`) y evicción segura por `simpleCache.removeResource(...)` sin borrado en caliente del directorio.
- Enrutamiento de audio real implementado en `PlayerViewModel.selectDevice()` con `AudioDeviceInfo` verificado.
- Erradicadas las URLs y referencias de prueba de Rick Astley y pistas falsas de Pixabay.
- Unificada la lectura de versión oficial desde `BuildConfig.VERSION_NAME` en toda la app.

### P4 — Froggy Clima Híbrido, UI/UX y Ergonomía
- **Encuadre de Froggy en Portrait:** Resuelto en `ZenBackground.kt` mediante `@media (orientation: portrait) { #flutter-container { left: -12vw; width: 124vw; } }`. Centra perfectamente la composición 16:9 en pantallas móviles 20:9 sin recortar a la ranita ni generar barras negras en los laterales, verificado en escenas de Picnic, Casa Hongo y Cometa.
- **Saludo Dinámico por Franja Horaria:** `resolveTitle` en `ZenPhraseCatalog.kt` calcula automáticamente:
  - 6:00 a 11:59 -> "Buenos días"
  - 12:00 a 19:59 -> "Buenas tardes"
  - 20:00 a 5:59 -> "Buenas noches"
  - Ticker de 60 segundos en `HomeViewModel.kt` y refresco automático al volver de background (`refreshGreeting()`).
- Solicitud nativa de `ACCESS_COARSE_LOCATION` en `MainActivity.kt` con refresco automático de `WeatherRepository`.
- Preferencia persistente `froggyScenery` (`AUTO`, `FIELDS`, `HILL`, `MUSHROOM`) añadida en DataStore y selector en `SettingsScreen.kt`.
- `ServerStatusMonitor` unificado en `@Singleton` evitando duplicidades de banners.
- Aislamiento de recomposición a 1Hz del progreso de reproducción en `FullScreenPlayer.kt` y `LockScreenActivity.kt`.
- Insets de pantalla (`WindowInsets.statusBars` y `navigationBars`) adaptados a edge-to-edge en todas las pantallas.
- Refinamiento de selección múltiple (`LibraryScreen.kt`): acciones de lote movidas a la barra superior contextual (`TopAppBar`) con menú desplegable `⋮`.

---

## 2. Publicación de Release v0.4.7
- Version Code: `8`
- Version Name: `0.4.7`
- Certificado oficial verificado (`apksigner`): `SHA-256: 62ede63ffb1e550f868e76000252f64e33940845385b6d307e342667aa744b5f`.
- Commits atómicos:
  - `dfb4439` — `feat(refine): remediate audit (P1-P4), fix portrait Froggy framing and add dynamic greeting`
  - `1ca8f12` — `chore(release): bump version to 0.4.7 (versionCode 8)`
- Tag: `v0.4.7` publicado en `origin`.
