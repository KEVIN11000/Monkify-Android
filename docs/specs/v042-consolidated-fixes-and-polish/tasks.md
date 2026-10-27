# Lista de Tareas Atómicas: Consolidated Fixes & UX Polish (v0.4.2)

**Feature:** `v042-consolidated-fixes-and-polish`  
**Directorio objetivo:** `e:\PG\vcp-s\Monkify-Android-Refine/`  
**Estado:** Fase 0 (Modo Planificador / Desglose Atómico)  
**Fecha:** 2026-10-27  
**Regla de Ejecución (Fase 2):** Implementar estrictamente **UNA** tarea por turno, ejecutando y validando sus pruebas en la consola antes de marcar `[x]` y avanzar a la siguiente.

---

## Checklist de Implementación

- [x] **T001: Corrección de Codificación/Decodificación y Carga de Pistas de Álbum**
  - **Archivos a modificar:**
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/ui/navigation/MonkifyNavGraph.kt`
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/AlbumDetailViewModel.kt`
    - `cloudflare-backend/src/routes/albums.ts`
    - `cloudflare-backend/test/albums.spec.ts`
  - **Descripción:**
    - En `MonkifyNavGraph.kt`, aplicar `URLDecoder.decode(..., "UTF-8")` al parámetro `album` recuperado de los argumentos de navegación.
    - En `AlbumDetailViewModel.kt`, asegurar sanitización de nombre y consulta tolerante en el fallback local de Room.
    - En `cloudflare-backend/src/routes/albums.ts`, normalizar el parámetro `album` reemplazando posibles signos `+` por espacios y aplicando `COLLATE NOCASE`.
    - Añadir test en Vitest (`cloudflare-backend/test/albums.spec.ts`) validando la recuperación de pistas con nombres complejos.
  - **Criterio de Aceptación / Prueba Verificable:**
    - Ejecutar `npm test` en `cloudflare-backend` pasando al 100%.
    - Ejecutar `./gradlew.bat compileDebugKotlin` sin errores.

- [x] **T002: Corrección de Transición de Cola en Reproducción Aleatoria (Shuffle)**
  - **Archivos a modificar:**
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt`
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/service/MonkifyPlaybackService.kt`
    - `Monkify-Android-Refine/app/src/test/java/com/k11000/monkify/playback/PlayerControlsTest.kt`
  - **Descripción:**
    - En `PlayerViewModel.kt`, robustecer `skipToNext()` para verificar adecuadamente si `shuffleModeEnabled` está activo, garantizando que el salto al siguiente elemento aleatorio o el reinicio de la cola aleatoria no congele la sesión ni detenga la reproducción al llegar al final de la lista física.
    - Manejar el evento `STATE_ENDED` para ciclar correctamente en modo aleatorio cuando no hay repeat activado.
    - Añadir pruebas unitarias en `PlayerControlsTest.kt` que simulen el avance de la última pista física con Shuffle activo.
  - **Criterio de Aceptación / Prueba Verificable:**
    - Ejecutar `./gradlew.bat testDebugUnitTest --tests "com.k11000.monkify.playback.PlayerControlsTest"` pasando en verde.

- [x] **T003: Paisajes Dinámicos Día/Noche en Zen Froggy y Refinamiento Cosmos HD**
  - **Archivos modificados:**
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/ui/components/ZenBackground.kt`
  - **Descripción:**
    - Implementación de franjas horarias automáticas (`ZenTimeOfDay.DAWN`, `DAY`, `SUNSET`, `NIGHT`) con función resolutora `resolveCurrentZenTimeOfDay()`.
    - Adaptación visual integral de Froggy: gradientes atmosféricos, sol/luna procedurales con auras de respiración, nubes Google Weather flotantes, iluminación de colinas, nenúfares, mejillas con blush, destellos en ojos y partículas ambientales contextuales (rocío matinal, semillas de diente de león, ascuas crepusculares y luciérnagas).
    - Motor espacial Cosmos HD: Galaxia espiral elíptica con núcleo brillante inclinada a -26°, 4 capas de nebulosas cósmicas, 72 estrellas en planos de profundidad con picos de difracción en cruz de 4 puntas y meteoros fugaces periódicos.
    - Previews de Compose integrados para cada modo y simulador interactivo.
  - **Criterio de Aceptación / Prueba Verificable:**
    - Ejecutar `./gradlew.bat compileDebugKotlin` sin errores (Verificado: BUILD SUCCESSFUL en 5m 33s).
    - Validación visual completa mediante Generative UI y Previews de Compose.

- [x] **T004: Modo de Selección Múltiple y Acciones por Lote en Tu Biblioteca**
  - **Archivos modificados:**
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/data/repository/LocalTrackRepository.kt`
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/LibraryViewModel.kt`
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/ui/screens/LibraryScreen.kt`
    - `Monkify-Android-Refine/app/src/test/java/com/k11000/monkify/ui/viewmodel/LibraryViewModelTest.kt`
  - **Descripción:**
    - En `LocalTrackRepository.kt`, añadido método seguro `removeFavorite(trackId: String)` preservando metadatos.
    - En `LibraryViewModel.kt`, implementado `isSelectionMode`, `selectedTrackIds`, `toggleTrackSelection`, `selectAll`, `clearSelection`, `batchDownloadSelected`, `batchRemoveFavorites` y `batchDeleteDownloads`.
    - En `LibraryScreen.kt`, cabecera interactiva con contador y botón seleccionar todo, checkboxes circulares animados con soporte de pulsación larga (`combinedClickable`), retroalimentación háptica y barra flotante contextual inferior de acciones por lote.
    - Suite de 5 pruebas unitarias añadidas en `LibraryViewModelTest.kt`.
  - **Criterio de Aceptación / Prueba Verificable:**
    - Ejecutar `./gradlew.bat testDebugUnitTest --tests "com.k11000.monkify.ui.viewmodel.LibraryViewModelTest"` (Verificado: BUILD SUCCESSFUL pasando al 100%).

- [x] **T005: Resiliencia de Descargas Locales y Reconciliación ante Reinstalación**
  - **Archivos modificados:**
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/domain/repository/DownloadRepository.kt`
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/data/repository/DownloadRepositoryImpl.kt`
    - `Monkify-Android-Refine/app/src/main/java/com/k11000/monkify/data/repository/LocalTrackRepository.kt`
    - `Monkify-Android-Refine/app/src/test/java/com/k11000/monkify/data/repository/DownloadRepositoryTest.kt`
  - **Descripción:**
    - En `DownloadRepositoryImpl.kt`, implementado método `reconcileDownloads()` invocado automáticamente al inicializarse el repositorio en background.
    - Escaneo de directorios físicos de música (`musicDir`, carpetas internas y heredadas) para detectar archivos de audio válidos no registrados tras una reinstalación de la app o conflicto de paquetes, re-vinculándolos a Room DB con `isDownloaded = true` y su ruta absoluta.
    - En `LocalTrackRepository.kt`, añadido método `getTrackById(trackId)`.
    - Añadida prueba unitaria en `DownloadRepositoryTest.kt` validando la recuperación de archivos huérfanos.
  - **Criterio de Aceptación / Prueba Verificable:**
    - Ejecutar `./gradlew.bat testDebugUnitTest --tests "com.k11000.monkify.data.repository.DownloadRepositoryTest"` (Verificado: BUILD SUCCESSFUL pasando al 100%).

- [x] **T006: Verificación Integral de Suite de Pruebas, Gate Visual y Build de Release**
  - **Archivos verificados:**
    - Todo el proyecto en Android y Worker Backend
  - **Descripción:**
    - Ejecución de la suite completa de backend en `cloudflare-backend` (`npm test`: 47/47 tests pasando al 100%).
    - Ejecución de la suite completa de tests unitarios de Android (`.\gradlew.bat testDebugUnitTest`: BUILD SUCCESSFUL al 100%).
    - Compilación del APK de producción (`.\gradlew.bat assembleRelease`: BUILD SUCCESSFUL en 10m 57s, generando `app-release.apk` de 17.7 MB).
    - Auditoría visual de animaciones Zen, ciclos de día/noche y modo de selección múltiple.
  - **Criterio de Aceptación / Prueba Verificable:**
    - `npm test` exitoso (0 fallos, 47/47 passing).
    - `./gradlew.bat testDebugUnitTest` y `./gradlew.bat assembleRelease` exitosos (`BUILD SUCCESSFUL`).
    - Artefacto `app-release.apk` disponible en `app/build/outputs/apk/release/`.
    - `npm test` exitoso (0 fallos).
    - `./gradlew.bat testDebugUnitTest` y `./gradlew.bat assembleRelease` exitosos (`BUILD SUCCESSFUL`).
