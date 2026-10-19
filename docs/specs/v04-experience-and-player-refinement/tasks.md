# Tasks: Refinamiento Integral de Experiencia, Reproductor y Biblioteca (v0.4.0)

**Input**: Documentos de diseño en `docs/specs/v04-experience-and-player-refinement/` (`spec.md`, `plan.md`)  
**Regla de Ejecución (Fase 2 - Modo Constructor)**: Ejecutar **UNA tarea atómica por turno**, verificando que sus pruebas pasen en la terminal antes de marcar `[x]` y avanzar a la siguiente tarea.

---

## Phase 1: Backend Cloudflare Worker (API Aditiva para v0.4.0)

- [x] **T001** [US2, US3, US5, US6] Actualizar el backend en `E:\PG\vcp-s\Monkify\cloudflare-backend` para soportar los 4 contratos nuevos de v0.4.0 y sus tests unitarios:
  - En `src/routes/tracks.ts`: añadir soporte para `?sort=latest` en `GET /api/v1/tracks` (Req #5) y crear el endpoint `GET /api/v1/tracks/:id/lyrics` con consulta a D1 + fallback a LRCLIB (Req #2).
  - En `src/routes/playlists.ts`: incluir el arreglo `cover_urls` (hasta las primeras 6 portadas no vacías de `playlist_tracks`) en `GET /api/v1/playlists` (Req #13).
  - Crear `src/routes/support.ts` y montarlo en `src/index.ts` bajo `/api/v1/support/report` (`POST`) con persistencia en tabla D1 `support_reports` y creación opcional de Issue en GitHub si existe `GITHUB_TOKEN` (Req #18).
  - Añadir pruebas en `test/index.spec.ts`.
  - **Criterio de Aceptación / Verificación**: Ejecutar `npm test` en `E:\PG\vcp-s\Monkify\cloudflare-backend` y confirmar que el 100% de las suites Vitest pasan sin errores.

---

## Phase 2: Capa de Datos, Persistencia Room y Contratos de Red en Android

- [x] **T002** [US2, US3, US6] Actualizar DTOs, API Retrofit, base de datos Room (`TrackEntity`, `TrackDao`, `MonkifyDatabase`) y `MusicRepository`:
  - En `app/src/main/java/com/monkify/app/data/remote/dto/TrackDto.kt`: agregar `coverUrls: List<String> = emptyList()` a `PlaylistSummaryDto`, y crear `SupportReportRequestDto` y `SupportReportResponseDto` (Req #13, #18).
  - En `app/src/main/java/com/monkify/app/data/remote/MonkifyApi.kt`: añadir parámetro `@Query("sort") sort: String? = null` a `getTracks()` y el método `@POST("support/report") suspend fun submitSupportReport(...)` (Req #5, #18).
  - En `TrackEntity.kt`, `TrackDao.kt` y `MonkifyDatabase.kt`: añadir `playCount: Int = 0` y `lastPlayedAt: Long = 0L`, junto con `recordTrackPlay()`, `getRecentlyPlayedTracks()` y `getTracksOrderedByPlayCount()` (Req #6, #7).
  - En `MusicRepository.kt`: preservar `isFavorite`, `isDownloaded`, `localAudioPath`, `playCount` y `lastPlayedAt` al refrescar pistas desde el servidor, exponer `fetchLatestUploadedTracks(limit: Int = 6)`, `recordTrackPlayed(trackId: String)`, `recentlyPlayedTracks` y mapear `coverUrls` en `PlaylistSummaryDto`.
  - **Criterio de Aceptación / Verificación**: Ejecutar `.\gradlew.bat testDebugUnitTest` y confirmar compilación y tests exitosos.

---

## Phase 3: Estabilidad del Reproductor, Pantalla de Bloqueo, Cero PiP y Letras

- [ ] **T003** [US1, US5] Implementar `PlaybackSessionStateHolder`, corregir pérdida de estado/cola en `PlayerViewModel` y `MonkifyPlaybackService`, sincronizar `LockScreenActivity` desde el primer bloqueo y soportar letras `.lrc` + texto plano:
  - Crear `app/src/main/java/com/monkify/app/player/PlaybackSessionStateHolder.kt` (`@Singleton`) para persistir y compartir la canción activa, metadatos y cola entre `PlayerViewModel`, `MonkifyPlaybackService` y `LockScreenActivity` (Req #9, #17).
  - En `PlayerViewModel.kt`: reconstruir estado e historial al conectar `MediaController`, encolar solicitudes si el controlador aún se está inicializando, registrar cada reproducción en `musicRepository.recordTrackPlayed(track.id)` (Req #6, #9) y añadir fallback LRCLIB para letras en `loadLyricsForTrack()` (Req #2).
  - En `LrcParser.kt` y `LyricsView.kt`: soportar letras en texto plano estático (`timestampMs = -1L`) además de `.lrc` sincronizado, y añadir tests unitarios en `LrcParserTest.kt` (Req #2).
  - En `MonkifyPlaybackService.kt` y `LockScreenActivity.kt`: asegurar activación inmediata en el primer bloqueo (`ACTION_SCREEN_OFF` / `Keyguard` y `configureLockScreenFlags()` previo a `setContent`) sincronizando con `PlaybackSessionStateHolder` (Req #17).
  - **Criterio de Aceptación / Verificación**: Ejecutar `.\gradlew.bat testDebugUnitTest` verificando que `LrcParserTest` (con casos LRC y texto estático) y todas las pruebas unitarias pasen.

- [ ] **T004** [US1] Eliminar Picture-in-Picture (PiP) por completo y blindar `FullScreenPlayer` contra toques fantasma:
  - En `app/src/main/AndroidManifest.xml`: eliminar `android:supportsPictureInPicture="true"` (Req #8).
  - En `app/src/main/java/com/monkify/app/MainActivity.kt`: eliminar toda la lógica, receptores y UI de PiP (`isInPipMode`, `pipReceiver`, `enterPipMode`, `PipPlayerContent`) (Req #8).
  - En `app/src/main/java/com/monkify/app/ui/components/FullScreenPlayer.kt`: eliminar el botón e ícono de PiP y agregar consumo de eventos táctiles en el `Box` raíz de `FullScreenPlayer` para que ningún toque en zonas vacías atraviese hacia la pantalla inferior y cambie la canción fuera de la playlist activa (Req #8, #10).
  - **Criterio de Aceptación / Verificación**: Ejecutar `.\gradlew.bat testDebugUnitTest` y confirmar cero referencias residuales a PiP y compilación limpia.

---

## Phase 4: Fondos Visuales (Froggy, Cosmos, Negro Puro) y Reporte en 1 Clic

- [ ] **T005** [US5, US6] Implementar los nuevos fondos visuales (`FROGGY`, `COSMOS`, `NONE`) en `ZenBackground.kt` y el envío de reportes en 1 clic en `SettingsScreen.kt` y `SettingsViewModel.kt`:
  - En `app/src/main/java/com/monkify/app/ui/components/ZenBackground.kt`: añadir los motores `"FROGGY"` (`FroggyWeatherCanvas` con paisaje nocturno/atardecer, estanque, ranita Froggy, luciérnagas y estrellas), `"COSMOS"` (`CosmosCanvas` con nebulosa y estrellas en parallax) y `"NONE"` (fondo negro puro `#000000` sin animaciones) (Req #16).
  - En `SettingsRepository.kt`, `SettingsViewModel.kt` y `SettingsScreen.kt`: habilitar la selección de los 5 fondos visuales y añadir el envío directo de reportes en 1 clic (`submitOneClickReport`) hacia `POST /api/v1/support/report` con campo opcional de mensaje y feedback visual inmediato sin requerir login en GitHub (Req #16, #18).
  - **Criterio de Aceptación / Verificación**: Ejecutar `.\gradlew.bat testDebugUnitTest` y confirmar que compila y pasa todas las pruebas.

---

## Phase 5: Rediseño de Inicio (`HomeScreen`), Collage de Carátulas y Navegación

- [ ] **T006** [US2, US3] Implementar componente `PlaylistCollageArtwork` y rediseñar `HomeViewModel.kt` y `HomeScreen.kt`:
  - En `app/src/main/java/com/monkify/app/ui/components/TrackArtwork.kt`: crear `PlaylistCollageArtwork` capaz de renderizar un mosaico 2x2 (4 carátulas) o 3x2 (6 carátulas) a partir de las primeras canciones de una playlist (Req #13).
  - En `HomeViewModel.kt` y `HomeScreen.kt`:
    - Añadir `HomeFilter.ALBUMS("Álbumes")` junto a `"Música"` y su vista de cuadrícula de álbumes (Req #3).
    - Implementar `getDynamicGreeting(hour: Int)` para las 7 etapas del día (`Madrugada`, `Amanecer`, `Mañana`, `Mediodía`, `Tarde`, `Atardecer`, `Noche`) con saludo y frase motivadora breve (Req #4).
    - Mostrar en la cuadrícula rápida superior los últimos 6 temas subidos al servidor (`latestUploadedTracks`) (Req #5).
    - Conectar `"Escuchado recientemente"` al flujo reactivo de historial de reproducción y `"Hecho para ti"` a las canciones de los artistas más escuchados por el usuario (Req #6, #7).
    - Eliminar la sección `"Todas las canciones"` de `HomeScreen.kt` (Req #19).
  - Añadir pruebas unitarias para `getDynamicGreeting` cubriendo las 7 franjas horarias.
  - **Criterio de Aceptación / Verificación**: Ejecutar `.\gradlew.bat testDebugUnitTest` y verificar que los nuevos tests de saludo horario y toda la suite pasen.

---

## Phase 6: Biblioteca, Detalle de Playlist/Álbum y Barra de Navegación

- [ ] **T007** [US3, US4] Implementar búsqueda y ordenamiento interno en `PlaylistDetailScreen` y `AlbumDetailScreen`, ordenamiento en todas las categorías de `LibraryScreen`, botones `"Aleatorio"` / `"Reproducir desde la primera"` en `"Todo"`, y navegación rápida en `BottomNavBar` / `SearchScreen`:
  - En `PlaylistDetailViewModel.kt`, `PlaylistDetailScreen.kt`, `AlbumDetailViewModel.kt` y `AlbumDetailScreen.kt`: añadir barra de búsqueda interna, selector de ordenamiento (`TrackSortOption`), portada collage en playlists y reproducción respetando la cola de la playlist/álbum (Req #1, #10, #13).
  - En `LibraryViewModel.kt` y `LibraryScreen.kt`: añadir `LibrarySortOption` funcional para todas las categorías (`Todo`, `Favoritas`, `Descargas`, `Playlists`, `Álbumes`), usar `PlaylistCollageArtwork` en los ítems de playlist, y agregar en la pestaña `"Todo"` los botones `"Aleatorio"` y `"Reproducir desde la primera"` sobre el catálogo completo (Req #12, #13, #19).
  - En `BottomNavBar.kt`, `MonkifyNavGraph.kt`, `MainActivity.kt`, `LibraryScreen.kt` y `SearchScreen.kt`: implementar scroll al tope al re-seleccionar `"Tu Biblioteca"` (Req #14), reseteo de búsqueda al re-seleccionar `"Buscar"` (Req #15) y actualizar el placeholder de `SearchScreen` a `"¿Qué te gustaría escuchar?"` (Req #11).
  - **Criterio de Aceptación / Verificación**: Ejecutar `.\gradlew.bat testDebugUnitTest` y `.\gradlew.bat assembleRelease` confirmando build exitoso y 100% de tests en verde.
