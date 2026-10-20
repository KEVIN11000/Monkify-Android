# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.0 — Refinamiento Integral de Experiencia, Reproductor y Biblioteca (19 Requerimientos, Release v0.4.0 y Cierre de Issue #1)  
**Fecha de cierre:** 2026-10-20  
**Estado del build/tests:** ✅ pasando — 33/33 tests en Vitest en `cloudflare-backend` (`npm test`), Worker desplegado en producción (`https://monkify-backend.monkify.workers.dev`, Version ID `374be678-8a1e-41a8-ba58-2f996a7cfae0`), y suite unitaria + build de Release de Android pasando al 100% en `e:\PG\vcp-s\Monkify-Android-Refine` (`.\gradlew.bat testDebugUnitTest assembleRelease` -> `BUILD SUCCESSFUL`). Rama `refine/v0.4` fusionada a `origin/main` y tag `v0.4.0` publicado con release en GitHub.

---

## 1. Estado actual del proyecto

- **Backend Cloudflare Worker (`cloudflare-backend`):**
  - Desplegado y verificado en producción con `npm run deploy` (endpoints `/tracks?sort=latest`, `/tracks/:id/lyrics` con proxy LRCLIB en vivo, `/playlists` con `cover_urls` para collage y `/support/report` para reportes en 1 clic).
  - Tabla `support_reports` autoinicializada en Cloudflare D1 (`monkify-db`).
- **Cliente Android (`Monkify-Android`):**
  - Los 19 requerimientos de la especificación `v04-experience-and-player-refinement` implementados y probados.
  - Eliminado por completo Picture-in-Picture (PiP) y blindado `FullScreenPlayer` contra toques fantasma.
  - Creado `PlaybackSessionStateHolder` (singleton) y sincronizado con `PlayerViewModel`, `MonkifyPlaybackService` y `LockScreenActivity` (superposición inmediata en el primer bloqueo).
  - Soporte de letras dual (`.lrc` sincronizado y texto plano con scroll manual).
  - Inicio rediseñado: saludo dinámico en 7 etapas horarias con frases breves motivacionales, chip de Álbumes, cuadrícula rápida de los últimos 6 subidos al servidor, y exclusión de la lista general de canciones.
  - Biblioteca y detalles: buscador interno y selector de ordenamiento (`TrackSortOption` y `LibrarySortOption`), botones "Aleatorio" y "Desde la primera" en pestaña "Todo", carátulas automáticas en collage (mosaicos 2x2 y 3x2), y reselect de navegación en `BottomNavBar` (scroll al tope en Biblioteca y reset en Buscar).
  - Ajustes: nuevos fondos visuales `Froggy`, `Cosmos` y `Ninguno` (negro puro OLED), y reporte de soporte directo en 1 clic.
  - Release `v0.4.0` publicado en GitHub Releases con APK descargable `Monkify-v0.4.0.apk`.
  - Issue #1 en `KEVIN11000/Monkify-Android` cerrado formalmente como completado.

---

## 2. Decisiones tomadas en esta fase

- **Decisión:** Sustituir la dependencia estricta de archivos `.lrc` locales por consulta dinámica en tiempo real a LRCLIB con fallback a texto plano (`timestampMs = -1L`).  
  **Por qué:** Evita cargar y sincronizar manualmente miles de archivos de letras en el almacenamiento, permitiendo letras sincronizadas inmediatas para la gran mayoría del catálogo y lectura estática en canciones sin marcas temporales.

- **Decisión:** Eliminar el modo PiP y delegar el control en segundo plano exclusivamente a `MediaSession` y notificaciones de sistema.  
  **Por qué:** PiP en reproductores de solo audio era innecesario, consumía recursos y provocaba discrepancias táctiles con la cola de reproducción.

- **Decisión:** Implementar `PlaybackSessionStateHolder` como singleton en la capa de playback.  
  **Por qué:** Garantiza que ni el reproductor ni la cola se vacíen al cambiar de app, bloquear la pantalla o recibir llamadas.

- **Decisión:** Repositorio `Monkify-Android` configurado como Público en GitHub.  
  **Por qué:** Permite que las consultas de actualización in-app (`GET /releases/latest`) y la descarga de APKs funcionen para cualquier usuario sin requerir tokens personales en los teléfonos móviles.

---

## 3. Pendientes críticos para la próxima versión (v0.4.1 / v0.5.0)

1. **Keystore persistente en GitHub Actions (Error de firma / Conflicto de paquetes al actualizar):**
   - **Diagnóstico:** El workflow `.github/workflows/release.yml` actualmente ejecuta `keytool -genkey` en cada corrida del runner de GitHub. Como cada build en CI genera un certificado efímero diferente, Android rechaza la actualización directa sobre una app previamente instalada (`INSTALL_FAILED_UPDATE_INCOMPATIBLE` / "error con el paquete").
   - **Acción requerida:**
     1. Crear un Keystore de release oficial único (`monkify-release.keystore`).
     2. Subir el keystore codificado en Base64 a los Secrets del repositorio en GitHub (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
     3. Actualizar `release.yml` para decodificar y firmar con esta clave fija, asegurando que todos los releases compartan siempre la misma firma criptográfica.

2. **Estrategia y auditoría de carátulas faltantes:**
   - **Diagnóstico:** Aún existen pistas en el catálogo (especialmente ingestas manuales de YouTube o pistas huérfanas) que tienen `cover_url` nulo o vacío.
   - **Acción requerida:**
     1. Ejecutar una consulta en D1 para aislar las canciones sin carátula: `SELECT id, title, artist, album, file_key FROM tracks WHERE cover_url IS NULL OR cover_url = ''`.
     2. Implementar un resolvedor secundario de carátulas en `spotify-harvester` o en el Worker (consultando endpoints de Deezer, iTunes Search API o thumbnails de YouTube `i.ytimg.com/vi/...`).

3. **Revisión de reportes y tickets de soporte:**
   - **Acción requerida:** Crear un script de consulta o panel en el Worker para auditar periódicamente los reportes acumulados en la tabla `support_reports` de Cloudflare D1.

---

## 4. Archivos modificados en esta sesión

| Archivo | Resumen de Cambios |
|---|---|
| `cloudflare-backend/src/routes/tracks.ts` | Parámetro `?sort=latest` y endpoint `/api/v1/tracks/:id/lyrics` con proxy LRCLIB |
| `cloudflare-backend/src/routes/playlists.ts` | Extracción de `cover_urls` (mosaico 4-6 portadas) |
| `cloudflare-backend/src/routes/support.ts` | Endpoint `POST /api/v1/support/report` con tabla D1 autoinicializada |
| `Monkify-Android-Refine/.../TrackEntity.kt` | Campos `playCount` y `lastPlayedAt` |
| `Monkify-Android-Refine/.../PlaybackSessionStateHolder.kt` | Singleton centralizado de cola y estado activo |
| `Monkify-Android-Refine/.../FullScreenPlayer.kt` | Remoción de PiP y escudo táctil antipenetración |
| `Monkify-Android-Refine/.../LockScreenActivity.kt` | Flags de encendido y superposición al primer bloqueo |
| `Monkify-Android-Refine/.../ZenBackground.kt` | Motores de canvas `FROGGY`, `COSMOS` y `NONE` |
| `Monkify-Android-Refine/.../HomeScreen.kt` | Saludos 7 franjas, chip Álbumes, Top 6 últimos, sin lista vertical |
| `Monkify-Android-Refine/.../LibraryScreen.kt` | Ordenamiento global, acciones Todo ("Aleatorio", "Desde la primera"), reselect scroll |
| `Monkify-Android-Refine/.../PlaylistDetailScreen.kt` | Búsqueda interna, menú de orden y mosaicos collage |
| `Monkify-Android-Refine/.../AlbumDetailScreen.kt` | Búsqueda interna y menú de orden |
| `Monkify-Android-Refine/.../SearchScreen.kt` | Placeholder "¿Qué te gustaría escuchar?" y reselect reset |
| `Monkify-Android-Refine/RELEASE_NOTES.md` | Notas formales de la versión v0.4.0 |
