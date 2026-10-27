# Plan de Arquitectura y Diseño Técnico: Consolidated Fixes & UX Polish (v0.4.2)

**Feature:** `v042-consolidated-fixes-and-polish`  
**Directorio de diseño:** `docs/specs/v042-consolidated-fixes-and-polish/`  
**Fecha:** 2026-10-27  
**Estado:** Fase 0 — Arquitectura (Modo Planificador)

---

## 1. Módulos y Componentes Involucrados

```
┌─────────────────────────────────────────────────────────────┐
│                       Monkify App                           │
├───────────────────────────────┬─────────────────────────────┤
│ UI Layer (Jetpack Compose)    │ Core / Playback Layer       │
│ - AlbumDetailScreen           │ - PlayerViewModel           │
│ - LibraryScreen (Batch UI)    │ - MonkifyPlaybackService    │
│ - ZenFroggyView (Time Cycles) │ - DownloadRepository        │
├───────────────────────────────┴─────────────────────────────┤
│ Data Layer (Room & Retrofit)                                │
│ - MonkifyNavGraph (URL Decoding)                            │
│ - LocalTrackRepository (Reconciliación de descargas)        │
├─────────────────────────────────────────────────────────────┤
│ Backend Layer (Cloudflare Workers & D1)                     │
│ - src/routes/albums.ts (Sanitización y búsqueda robusta)    │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Contratos y Estrategias Técnicas por Módulo

### 2.1. Carga de Álbumes (Frontend & Backend)
- **MonkifyDestinations / MonkifyNavGraph:**
  - Al recibir argumentos en `MonkifyNavGraph.kt`:
    ```kotlin
    val rawAlbum = backStackEntry.arguments?.getString("album") ?: ""
    val decodedAlbum = runCatching { 
        java.net.URLDecoder.decode(rawAlbum, "UTF-8") 
    }.getOrDefault(rawAlbum)
    ```
  - En `AlbumDetailViewModel.kt`:
    - Normalizar `cleanAlbum = decodedAlbum.trim()`
    - En caso de fallback local o remoto, si la búsqueda exacta arroja 0 resultados, reintentar con `LIKE %album%` o reemplazando `+` por espacios para total retrocompatibilidad.
- **Backend `cloudflare-backend/src/routes/albums.ts`:**
  - En el endpoint `GET /api/v1/albums/tracks`:
    - Sanitizar `albumName`: reemplazar `+` por espacio si detecta signos de concatenación no decodificados (`albumName.replace(/\+/g, ' ')`).
    - Consulta D1: `WHERE TRIM(album) = TRIM(?) COLLATE NOCASE` para soportar inconsistencias de mayúsculas/minúsculas.

### 2.2. Robustez de Shuffle en Media3 (Player Service & ViewModel)
- **Causa del bloqueo:** Cuando ExoPlayer está en modo `shuffleModeEnabled = true`, si el usuario llega al último índice del array o avanza manualmente con `seekToNextMediaItem()`, ExoPlayer requiere verificar el orden de reproducción (`shuffleOrder`).
- **Solución en `PlayerViewModel.kt` / `MonkifyPlaybackService.kt`:**
  - Implementar método seguro de avance de pista:
    ```kotlin
    fun safeSkipNext() {
        val player = mediaController ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (player.shuffleModeEnabled && player.mediaItemCount > 0) {
            // Ciclar al primer item del shuffle order en lugar de colgar
            player.seekToDefaultPosition(0)
            player.play()
        }
    }
    ```
  - Suscripción al evento `onPlaybackStateChanged(STATE_ENDED)`: Si shuffle está activo y no hay repeat one, reiniciar la cola en modo aleatorio si el usuario lo tiene configurado en bucle.

### 2.3. Dinámica Temporal en Zen Froggy (Day/Night Cycles)
- **Contrato de Franja Horaria (`TimeOfDay`):**
  ```kotlin
  enum class TimeOfDay {
      DAWN,   // 06:00 - 08:59 (Tonos dorados y niebla suave)
      DAY,    // 09:00 - 17:59 (Cielo azul vibrante y luz brillante)
      DUSK,   // 18:00 - 20:59 (Puesta de sol carmesí y violeta)
      NIGHT   // 21:00 - 05:59 (Obsidiana oscura, luciérnagas y estrellas)
  }
  ```
- **Implementación en `ZenFroggyView.kt`:**
  - Detección reactiva mediante `LocalTime.now().hour` combinada con recomposición animada mediante `animateColorAsState` para que la transición no sea brusca.

### 2.4. Modo Selección Múltiple en `LibraryScreen`
- **Estado en `LibraryViewModel.kt`:**
  ```kotlin
  val isSelectionMode = MutableStateFlow(false)
  val selectedTrackIds = MutableStateFlow<Set<String>>(emptySet())
  ```
- **Acciones en Lote:**
  - `toggleTrackSelection(trackId: String)`
  - `selectAll(trackIds: List<String>)`
  - `clearSelection()`
  - `batchDownloadSelected()`
  - `batchRemoveFavorites()`
  - `batchDeleteDownloads()`
- **UI en `LibraryScreen.kt`:**
  - Al mantener presionada una pista o presionar el botón "Seleccionar", aparece una barra de herramientas superior con conteo y opciones masivas con botones hápticos.

### 2.5. Resiliencia de Almacenamiento de Descargas
- **Estrategia en `DownloadRepositoryImpl.kt`:**
  - Ubicación de almacenamiento: Usar directorio de música persistente o almacenamiento de medios para que no se purgue si el usuario reinstala la app.
  - Función de auto-reconciliación al iniciar: `reconcileDownloads()` verifica los archivos existentes en la carpeta de Monkify e inserta o actualiza el registro en la base de datos Room (`isDownloaded = true`, `localAudioPath = path`).

---

## 3. Plan de Pruebas y Validación Empírica

1. **Pruebas Unitarias de Álbum:**
   - Test en `AlbumDetailViewModelTest.kt` validando que nombres con espacios, comillas y signos `+` carguen las pistas correctas.
2. **Pruebas Unitarias de Shuffle:**
   - Test en `PlayerControlsTest.kt` verificando el comportamiento circular de cola con y sin Shuffle activado.
3. **Pruebas de Biblioteca (Batch Actions):**
   - Test en `LibraryViewModelTest.kt` verificando selección masiva, conteos y despacho de acciones en bloque.
4. **Pruebas de Backend:**
   - Test en `cloudflare-backend/test/albums.spec.ts` validando la recuperación de pistas con espacios y caracteres especiales.
