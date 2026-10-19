# Implementation Plan: Refinamiento Integral de Experiencia, Reproductor y Biblioteca (v0.4.0)

**Branch**: `refine/v0.4` | **Date**: 2026-10-19 | **Spec**: [spec.md](file:///e:/PG/vcp-s/Monkify-Android-Refine/docs/specs/v04-experience-and-player-refinement/spec.md)  
**Input**: Feature specification from `docs/specs/v04-experience-and-player-refinement/spec.md`

---

## 1. Resumen Técnico y Contexto de Arquitectura

Este plan técnico detalla los contratos de datos, endpoints de API, cambios en persistencia local (Room + DataStore) y arquitectura modular en Jetpack Compose + Media3 para implementar los 19 requerimientos de la versión **v0.4.0**.

### Technical Context
- **Lenguaje / Plataforma Cliente**: Kotlin 2.0, Android SDK 35 (minSdk 26), Jetpack Compose (Material 3), Media3 (`ExoPlayer`, `MediaSessionService`, `MediaController`), Hilt DI, Room Database, Retrofit + Kotlinx Serialization, Coil.
- **Lenguaje / Plataforma Backend**: TypeScript, Cloudflare Workers (Hono framework), Cloudflare D1 (SQLite), Vitest.
- **Testing**: JUnit 4 + Kotlin Coroutines Test en Android (`.\gradlew.bat testDebugUnitTest`); Vitest en Cloudflare Backend (`npm test`).

---

## 2. Contratos de Datos, Esquemas y DTOs

### 2.1 Backend Cloudflare Worker (`cloudflare-backend/src/`)

#### A. `GET /api/v1/tracks` — Soporte de ordenamiento `sort=latest` (Req #5)
- **Parámetros Query**:
  - `skip` (`number`, default `0`)
  - `limit` (`number`, default `100`, max `500`)
  - `sort` (`string`, opcional: `"latest"` | `"default"`)
- **Comportamiento**:
  - Cuando `sort=latest`, la consulta SQL sobre `tracks` ordena por `rowid DESC` (las últimas canciones subidas/insertadas en el servidor).
  - Cuando no se especifica `sort`, mantiene el orden por defecto (`CASE WHEN cover_url IS NOT NULL AND cover_url != '' THEN 0 ELSE 1 END, title ASC`).

#### B. `GET /api/v1/tracks/:id/lyrics` — Obtención de Letras LRC o Texto Estático (Req #2)
- **Respuesta JSON (`200 OK`)**:
  ```json
  {
    "track_id": "string",
    "lyrics": "[00:12.34] Línea sincronizada...\n... o texto plano estático",
    "synced": true,
    "source": "d1 | lrclib | none"
  }
  ```
- **Comportamiento y Manejo de Fallos**:
  1. Busca el track en D1 (`SELECT id, title, artist, album, duration, lyrics FROM tracks WHERE id = ?`).
  2. Si la columna `lyrics` existe y tiene contenido no vacío, la devuelve inmediatamente.
  3. Si no tiene letra almacenada, realiza una consulta HTTP a `https://lrclib.net/api/get` / `https://lrclib.net/api/search?track_name=...&artist_name=...` con timeout de 4500ms.
  4. Prefiere `syncedLyrics` (formato `.lrc`) y si no existe usa `plainLyrics` (texto estático). Si se obtiene letra desde LRCLIB, la retorna en el campo `lyrics`.
  5. Si falla la red externa o no existe letra, responde `200 OK` con `{"track_id": id, "lyrics": null, "synced": false, "source": "none"}` para evitar errores `500` o `404` ruidosos.

#### C. `GET /api/v1/playlists` — Portadas Collage `cover_urls` (Req #13)
- **Respuesta JSON enriquecida por cada playlist**:
  ```json
  {
    "id": "pl-1",
    "name": "Mi Playlist",
    "description": "...",
    "cover_url": "https://...",
    "cover_urls": ["https://.../1.jpg", "https://.../2.jpg", "https://.../3.jpg", "https://.../4.jpg"],
    "is_public": true,
    "track_count": 12
  }
  ```
- **Comportamiento**:
  - Además de `cover_url` principal, extrae hasta las primeras 6 carátulas distintas y no vacías de las canciones asociadas en `playlist_tracks` ordenadas por `position ASC`.

#### D. `POST /api/v1/support/report` — Reporte de Problemas en 1 Clic (Req #18)
- **Request Body JSON**:
  ```json
  {
    "message": "Descripción opcional del usuario",
    "diagnostics": "Bloque de diagnóstico del dispositivo y estado de la app",
    "app_version": "0.4.0",
    "device_model": "Pixel / Samsung ..."
  }
  ```
- **Response JSON (`201 Created`)**:
  ```json
  {
    "ok": true,
    "report_id": "rep_1729360000_abcd",
    "github_issue_url": null
  }
  ```
- **Comportamiento**:
  - Crea automáticamente la tabla `support_reports` en D1 (`CREATE TABLE IF NOT EXISTS support_reports (...)`) e inserta el reporte para garantizar persistencia 100% libre de login.
  - Si la variable de entorno `GITHUB_TOKEN` está configurada en el Worker, abre además un Issue en GitHub de forma transparente desde el servidor.

---

### 2.2 Modelos, Persistencia y Estado en Android (`app/src/main/java/com/monkify/app/`)

#### A. DTOs de Red (`data/remote/dto/TrackDto.kt` y `MonkifyApi.kt`)
- Actualizar `PlaylistSummaryDto` añadiendo:
  ```kotlin
  @SerialName("cover_urls") val coverUrls: List<String> = emptyList()
  ```
- Añadir `SupportReportRequestDto` y `SupportReportResponseDto`:
  ```kotlin
  @Serializable
  data class SupportReportRequestDto(
      @SerialName("message") val message: String,
      @SerialName("diagnostics") val diagnostics: String,
      @SerialName("app_version") val appVersion: String = "0.4.0",
      @SerialName("device_model") val deviceModel: String = ""
  )

  @Serializable
  data class SupportReportResponseDto(
      @SerialName("ok") val ok: Boolean = true,
      @SerialName("report_id") val reportId: String = ""
  )
  ```
- Actualizar `MonkifyApi`:
  - `getTracks(@Query("skip") skip: Int = 0, @Query("limit") limit: Int = 100, @Query("sort") sort: String? = null): List<TrackDto>`
  - `@POST("support/report") suspend fun submitSupportReport(@Body request: SupportReportRequestDto): SupportReportResponseDto`

#### B. Persistencia de Historial y Conteo de Reproducción (`TrackEntity.kt`, `TrackDao.kt`, `MonkifyDatabase.kt`) (Req #6 y #7)
- Añadir columnas a `TrackEntity`:
  - `val playCount: Int = 0`
  - `val lastPlayedAt: Long = 0L`
- Actualizar versión de `MonkifyDatabase` con migración segura (y `fallbackToDestructiveMigration` ya activo como respaldo) y añadir consultas en `TrackDao`:
  - `@Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :trackId") suspend fun recordTrackPlay(trackId: String, timestamp: Long)`
  - `@Query("SELECT * FROM tracks WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit") fun getRecentlyPlayedTracks(limit: Int = 12): Flow<List<TrackEntity>>`
  - `@Query("SELECT * FROM tracks ORDER BY playCount DESC, lastPlayedAt DESC") fun getTracksOrderedByPlayCount(): Flow<List<TrackEntity>>`
- **Preservación en Sincronización**: En `MusicRepository.refreshTracks()`, conservar `isFavorite`, `isDownloaded`, `localAudioPath`, `playCount` y `lastPlayedAt` existentes en Room al hacer `insertTracks` para no sobrescribir el historial ni favoritos del usuario.

#### C. `PlaybackSessionStateHolder` (`@Singleton` en `player/PlaybackSessionStateHolder.kt`) (Req #9 y #17)
- Mantiene en memoria de aplicación (`@Singleton` inyectado tanto en `PlayerViewModel` como en `MonkifyPlaybackService`):
  - `queue: StateFlow<List<TrackDto>>`
  - `currentQueueIndex: StateFlow<Int>`
  - `currentTrackId: StateFlow<String?>`
  - `currentTrackTitle: StateFlow<String>`
  - `currentTrackArtist: StateFlow<String>`
  - `currentTrackCoverUrl: StateFlow<String?>`
- Cuando `PlayerViewModel` se reconecta a `MonkifyPlaybackService` (por ejemplo, tras desbloquear pantalla, volver desde Inicio o abrir `LockScreenActivity`):
  1. Lee inmediatamente el estado vivo de `PlaybackSessionStateHolder`.
  2. Si `controller.currentMediaItem != null`, sincroniza además `currentMediaItem.mediaId`, `mediaMetadata.title`, `mediaMetadata.artist`, `mediaMetadata.artworkUri`, `controller.currentPosition`, `controller.duration` y `controller.isPlaying`.
  3. Si `playQueue()` es invocado mientras `mediaController` aún es `null` (arranque en frío), almacena un `pendingPlayRequest` que se ejecuta automáticamente apenas `controllerFuture` completa su inicialización.

#### D. Enums de Ordenamiento (`TrackSortOption` y `LibrarySortOption`) (Req #1 y #12)
- `TrackSortOption` (para `PlaylistDetailViewModel` y `AlbumDetailViewModel`):
  - `DEFAULT("Orden original")`
  - `TITLE_ASC("Título (A-Z)")`
  - `TITLE_DESC("Título (Z-A)")`
  - `ARTIST_ASC("Artista (A-Z)")`
  - `DURATION_ASC("Duración (menor a mayor)")`
  - `DURATION_DESC("Duración (mayor a menor)")`
- `LibrarySortOption` (para `LibraryViewModel`):
  - `RECENT("Recientes")`
  - `TITLE_ASC("Título (A-Z)")`
  - `TITLE_DESC("Título (Z-A)")`
  - `ARTIST_ASC("Artista (A-Z)")`
  - `DURATION_DESC("Duración / Cantidad")`

---

## 3. Diseño Modular por Componente

### 3.1 Eliminación Total de PiP y Blindaje de `FullScreenPlayer` y `LockScreenActivity` (Req #8, #9, #10, #17)
1. **`AndroidManifest.xml`**: Quitar `android:supportsPictureInPicture="true"` de `.MainActivity`.
2. **`MainActivity.kt`**:
   - Eliminar `isInPipMode`, `pipReceiver`, `enterPipMode()`, `onPictureInPictureModeChanged()`, `buildPipParams()`, `PipPlayerContent()` y las llamadas a `enterPipMode` en `onUserLeaveHint()`.
   - Añadir contadores de re-selección `libraryScrollTrigger` y `searchResetTrigger` en `MainActivity` -> `BottomNavBar` -> `MonkifyNavGraph`.
3. **`FullScreenPlayer.kt`**:
   - Eliminar el parámetro `onEnterPip` y el botón con ícono `PictureInPictureAlt`.
   - Añadir en el `Box` raíz de `FullScreenPlayer` un modificador `.pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent() } } }` para bloquear por completo los toques hacia las pantallas inferiores (#10).
4. **`MonkifyPlaybackService.kt` y `LockScreenActivity.kt`**:
   - En `MonkifyPlaybackService`, registrar tanto `Intent.ACTION_SCREEN_OFF` como `Intent.ACTION_SCREEN_ON` (verificando `KeyguardManager.isKeyguardLocked`) para lanzar `LockScreenActivity` desde el primer bloqueo si `exoPlayer.isPlaying` es verdadero.
   - En `LockScreenActivity.onCreate()`, invocar `configureLockScreenFlags()` antes de `setContent`, activando `setShowWhenLocked(true)` y `FLAG_SHOW_WHEN_LOCKED` para que se superponga en el primer bloqueo sin requerir desbloquear y volver a bloquear.

### 3.2 Letras Sincronizadas (`.lrc`) y Estáticas (Req #2)
1. **`LrcParser.kt`**:
   - Si el texto contiene al menos una línea con timestamp `[mm:ss.xx]`, parsea las líneas sincronizadas como hasta ahora.
   - Si el texto NO contiene timestamps válidos pero tiene texto plano, convierte cada línea del texto en `LyricLine(timestampMs = -1L, text = trimmedLine)` para que nunca devuelva `emptyList()` ante letras estáticas.
2. **`LyricsView.kt`**:
   - Detecta si la letra es sincronizada (`lines.any { it.timestampMs >= 0L }`).
   - En modo sincronizado: resalta la línea activa y hace auto-scroll.
   - En modo estático (`timestampMs == -1L`): muestra una etiqueta `"Letra"` y renderiza todas las líneas con tipografía legible en blanco/esmeralda claro sin resaltado por tiempo.
3. **`PlayerViewModel.kt`**:
   - En `loadLyricsForTrack(trackId)`: consulta `monkifyApi.getLyrics(trackId)`. Si devuelve vacío o falla, realiza un fallback directo desde el cliente a `https://lrclib.net/api/get` con el título y artista actuales para máxima resiliencia.

### 3.3 Nuevos Motores Visuales en `ZenBackground.kt` y `SettingsScreen.kt` (Req #16)
- Soportar 5 identificadores en `SettingsRepository`, `SettingsViewModel`, `SettingsScreen` y `ZenBackground`:
  1. `"AURORA"` -> `AuroraCanvas()` (existente).
  2. `"VISUALIZER"` -> `VisualizerCanvas()` (existente).
  3. `"FROGGY"` -> `FroggyWeatherCanvas()`: Paisaje nocturno/atardecer inspirado en Froggy de Google Weather con colinas suaves, estanque con ondas sutiles, silueta/personaje de ranita sobre hoja de loto, luciérnagas animadas y estrellas parpadeantes.
  4. `"COSMOS"` -> `CosmosCanvas()`: Nebulosa espacial profunda con campo estelar parallax y polvo cósmico reactivo al estado de reproducción.
  5. `"NONE"` -> `Box(Modifier.fillMaxSize().background(Color.Black))` sin animaciones de Canvas (ahorro máximo de batería/OLED).

### 3.4 Rediseño de `HomeScreen.kt` y `HomeViewModel.kt` (Req #3, #4, #5, #6, #7, #19)
- **`HomeFilter`**: `ALL("Todo")`, `MUSIC("Música")`, `ALBUMS("Álbumes")`, `PODCASTS("Podcasts")`.
- **`GreetingInfo` (`getDynamicGreeting(hour: Int)`)**:
  - `0..4`: `"Buenas noches"` / `"Música suave para acompañar tu madrugada ✨"`
  - `5..6`: `"Buenos días"` / `"Un nuevo amanecer comienza con buen ritmo 🌅"`
  - `7..11`: `"Buenos días"` / `"Energía y enfoque para conquistar tu mañana ⚡"`
  - `12..13`: `"Buenas tardes"` / `"Una pausa musical para recargar tu mediodía ☀️"`
  - `14..17`: `"Buenas tardes"` / `"Sigue el ritmo, vas por excelente camino 🚀"`
  - `18..19`: `"Buenas tardes"` / `"Relaja la mente mientras cae el atardecer 🌇"`
  - `20..23`: `"Buenas noches"` / `"Desconecta y disfruta tu música favorita 🌙"`
- **`latestUploadedTracks`**: Obtenidos mediante `monkifyApi.getTracks(skip = 0, limit = 6, sort = "latest")` (con fallback a los últimos 6 tracks locales), mostrados en la cuadrícula rápida 2x3 superior.
- **`recentTracks`**: Conectado al flujo `trackDao.getRecentlyPlayedTracks(10)` (con fallback inicial si el usuario aún no ha reproducido pistas).
- **`madeForYouTracks`**: Calculado dinámicamente agrupando el catálogo por los artistas con mayor `playCount` del usuario (y rellenando con pistas destacadas si el historial es nuevo).
- **Eliminación de `"Todas las canciones"`**: Se retira el listado vertical infinito de `HomeScreen.kt`.

### 3.5 Mejoras en `LibraryScreen.kt`, `PlaylistDetailScreen.kt` y `AlbumDetailScreen.kt` (Req #1, #12, #13, #14, #19)
- **`PlaylistCollageArtwork`** en `TrackArtwork.kt`:
  - Recibe `coverUrls: List<String>` y `fallbackCoverUrl: String?`.
  - Si `coverUrls.size >= 6`, renderiza una cuadrícula 3x2 (6 portadas).
  - Si `coverUrls.size in 4..5`, renderiza una cuadrícula 2x2 (las primeras 4 portadas).
  - Si tiene menos de 4 portadas, delega en `TrackArtwork` tradicional.
- **Pestaña `"Todo"` en `LibraryScreen.kt`**:
  - Muestra todas las canciones del catálogo sincronizado en Room (`allTracks`).
  - Incluye en la cabecera dos botones destacados:
    1. **Botón `"Aleatorio"`**: Activa shuffle y reproduce la lista completa mezclada.
    2. **Botón `"Reproducir desde la primera"`**: Desactiva shuffle y reproduce secuencialmente desde el índice `0`.
- **Ordenamiento en `LibraryScreen.kt`**:
  - Al tocar la fila de ordenamiento, abre un `DropdownMenu` con las opciones de `LibrarySortOption`, ordenando reactivamente canciones, playlists o álbumes según la pestaña activa.
- **Buscador y Ordenamiento en `PlaylistDetailScreen.kt` y `AlbumDetailScreen.kt`**:
  - Campo de búsqueda rápida (`OutlinedTextField` compacto) + chip/menú de ordenamiento (`TrackSortOption`) sobre el listado de pistas.

---

## 4. Estructura de Archivos a Modificar / Crear

```text
E:\PG\vcp-s\Monkify\cloudflare-backend\
├── src/routes/tracks.ts                        # Req #2 (GET /:id/lyrics) y Req #5 (?sort=latest)
├── src/routes/playlists.ts                     # Req #13 (cover_urls en listado de playlists)
├── src/routes/support.ts                       # Req #18 (POST /api/v1/support/report)
├── src/index.ts                                # Montaje de /api/v1/support
└── test/index.spec.ts                          # Tests unitarios Vitest para los nuevos endpoints

e:\PG\vcp-s\Monkify-Android-Refine\app\src\main\
├── AndroidManifest.xml                         # Req #8 (remover supportsPictureInPicture)
└── java/com/monkify/app/
    ├── MainActivity.kt                         # Req #8, #14, #15 (remover PiP, triggers de scroll/reset)
    ├── data/
    │   ├── local/
    │   │   ├── MonkifyDatabase.kt              # Bump version para columnas de historial
    │   │   ├── dao/TrackDao.kt                 # Req #6, #7 (recordTrackPlay, getRecentlyPlayedTracks)
    │   │   └── entity/TrackEntity.kt           # Req #6, #7 (playCount, lastPlayedAt)
    │   ├── remote/
    │   │   ├── MonkifyApi.kt                   # Req #5 (sort), Req #18 (submitSupportReport)
    │   │   └── dto/TrackDto.kt                 # Req #13 (coverUrls), Req #18 (SupportReport DTOs)
    │   └── repository/
    │       ├── MusicRepository.kt              # Req #5, #6, #7, #13 (preservar historial, sort=latest)
    │       └── SettingsRepository.kt           # Req #16 (documentar 5 motores de fondo)
    ├── player/
    │   ├── PlaybackSessionStateHolder.kt       # NUEVO: Req #9, #17 (@Singleton estado compartido)
    │   ├── MonkifyPlaybackService.kt           # Req #9, #17 (sincronizar StateHolder y primer bloqueo)
    │   ├── PlayerViewModel.kt                  # Req #2, #6, #9, #17 (reconstruir estado, letras, historial)
    │   └── LrcParser.kt                        # Req #2 (soporte LRC + letras estáticas en texto plano)
    └── ui/
        ├── components/
        │   ├── BottomNavBar.kt                 # Req #14, #15 (callbacks de re-selección)
        │   ├── FullScreenPlayer.kt             # Req #8, #10 (consumir toques, quitar botón PiP)
        │   ├── LyricsView.kt                   # Req #2 (renderizado de letras sincronizadas y estáticas)
        │   ├── TrackArtwork.kt                 # Req #13 (PlaylistCollageArtwork 2x2 / 3x2)
        │   └── ZenBackground.kt                # Req #16 (FroggyWeatherCanvas, CosmosCanvas, NONE)
        ├── lockscreen/
        │   └── LockScreenActivity.kt           # Req #17 (flags antes de setContent + sincronía de cola)
        ├── navigation/
        │   └── MonkifyNavGraph.kt              # Req #14, #15 (pasar triggers a Library y Search)
        └── screens/
            ├── album/
            │   ├── AlbumDetailScreen.kt        # Req #1 (buscador y ordenamiento interno)
            │   └── AlbumDetailViewModel.kt     # Req #1 (searchQuery, sortOption, displayedTracks)
            ├── home/
            │   ├── HomeScreen.kt               # Req #3, #4, #5, #6, #7, #19
            │   └── HomeViewModel.kt            # Req #3, #4, #5, #6, #7, #19
            ├── library/
            │   ├── LibraryScreen.kt            # Req #12, #13, #14, #19 (ordenamiento, collage, botones Todo)
            │   └── LibraryViewModel.kt         # Req #12, #19 (LibrarySortOption y ordenamiento reactivo)
            ├── playlist/
            │   ├── PlaylistDetailScreen.kt     # Req #1, #10, #13 (buscador, ordenamiento, collage)
            │   └── PlaylistDetailViewModel.kt  # Req #1 (searchQuery, sortOption, displayedTracks)
            ├── search/
            │   └── SearchScreen.kt             # Req #11, #15 (placeholder y reset al re-seleccionar)
            └── settings/
                ├── SettingsScreen.kt           # Req #16, #18 (5 fondos visuales y reporte en 1 clic)
                └── SettingsViewModel.kt        # Req #16, #18 (submitOneClickReport)
```
