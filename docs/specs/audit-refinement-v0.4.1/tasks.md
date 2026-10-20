# Lista de Tareas Atómicas: Auditoría y Afinamiento Integral v0.4.1 (Revisada)

## Checklist de Implementación Secuencial

- [x] **Tarea 1: Backend — Aislamiento DDL de soporte, validación de payload y manejadores globales JSON**
  - **Archivos:** `cloudflare-backend/src/db/schema.sql`, `cloudflare-backend/src/routes/support.ts`, `cloudflare-backend/src/index.ts`
  - **Detalle:** Mover `CREATE TABLE IF NOT EXISTS support_reports` a `schema.sql`. Limitar el payload del reporte a 64 KB, sanitizar/truncar cadenas, escapar triple-backticks en diagnósticos y configurar manejadores JSON para 404 y 500 en `index.ts`.
  - **Criterio de Aceptación:** `npm test --prefix "e:\PG\vcp-s\Monkify\cloudflare-backend"` aprueba sin regresiones.

- [x] **Tarea 2: Backend — Suffix Ranges en streaming R2 y protección anti-hammering en letras**
  - **Archivos:** `cloudflare-backend/src/routes/stream.ts`, `cloudflare-backend/src/routes/tracks.ts`
  - **Detalle:** Corregir el cálculo de `offset` y `length` para `range.suffix` en `stream.ts` y añadir cabeceras `Cache-Control`. En `tracks.ts`, restringir la búsqueda de fallback únicamente cuando el primer intento devuelva 404 (no en 429/5xx) y agregar cabeceras de caché públicas y negativas.
  - **Criterio de Aceptación:** `npm test --prefix "e:\PG\vcp-s\Monkify\cloudflare-backend"` aprueba al 100%.

- [x] **Tarea 3: Android Core — Preservación de métricas de reproducción en Room**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/data/repository/LocalTrackRepository.kt`
  - **Detalle:** En `toggleFavorite` y `removeDownload`, verificar si `existing.playCount > 0 || existing.lastPlayedAt > 0L`. Si es verdadero, solo actualizar los flags booleanos correspondientes (`isFavorite = false`, `isDownloaded = false`) en lugar de borrar la fila física de `favorite_tracks`.
  - **Criterio de Aceptación:** `.\gradlew.bat testDebugUnitTest` compila y aprueba.

- [x] **Tarea 4: Android Core — Cancelación de carrera en letras y sincronización de cola**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt`
  - **Detalle:** Introducir `private var lyricsJob: Job? = null` en `PlayerViewModel` y cancelar la tarea previa en `loadLyricsForTrack`. Invocar `stateHolder?.updateQueue(mutable)` en `moveQueueItem` y `removeQueueItem`. Reflejar la nueva posición de forma inmediata en `_currentPosition.value` al ejecutar `seekTo()` en pausa.
  - **Criterio de Aceptación:** Pruebas unitarias de ViewModel ejecutadas con éxito.

- [x] **Tarea 5: Android Core — Registro seguro de receiver y desbloqueo asíncrono**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/MonkifyPlaybackService.kt`, `app/src/main/java/com/k11000/monkify/lockscreen/LockScreenActivity.kt`
  - **Detalle:** Usar `ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)` en el servicio. En `LockScreenActivity.unlockAndDismiss()`, invocar `KeyguardDismissCallback` y solo ejecutar `finish()` tras `onDismissSucceeded()`.
  - **Criterio de Aceptación:** Compilación limpia y sin excepciones de seguridad en Android 14+.

- [x] **Tarea 6: Android UI — Modo Zen Despejado: eliminación de textos fijos, auto-hide de guía en 3s y doble toque canónico**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/ui/screens/HomeScreen.kt`
  - **Detalle:** En `ZenGestureOverlay`, suprimir por completo los textos de título y artista sobre la imagen. La píldora superior y el texto de guía de gestos se ocultan automáticamente tras 3 segundos con `delay(3000)` y `AnimatedVisibility(fadeOut)`. El doble toque utiliza `detectTapGestures` para evitar pausar o reanudar al primer toque.
  - **Criterio de Aceptación:** Verificación visual: pantalla 100% limpia sin textos superpuestos tras 3s; doble toque restaura la interfaz sin alterar la reproducción.

- [x] **Tarea 7: Android UI — MiniPlayer en modo Peek asomando inferiormente en Modo Zen**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/MainActivity.kt`, `app/src/main/java/com/k11000/monkify/ui/components/MiniPlayer.kt`
  - **Detalle:** Al activarse `isZenCleared`, la `BottomNavBar` se desliza fuera de pantalla y el `MiniPlayer` se traslada hacia abajo dejando asomando un tirador sutil de 18dp. Al pulsar ese tirador o tocar el lienzo, la interfaz se restaura.
  - **Criterio de Aceptación:** MiniPlayer asoma discretamente sin tapar la animación ambiental Zen.

- [x] **Tarea 8: Android UI — Animación cinemática de cambio de vinilo y rotación a 60 FPS**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/ui/components/VinylTurntable.kt`
  - **Detalle:** Encapsular el disco en `AnimatedContent(targetState = coverUrl)` con transición de salida (fade + escala suave) y entrada deslizante del nuevo disco. Migrar la rotación continua a `Modifier.graphicsLayer { rotationZ = ... }`.
  - **Criterio de Aceptación:** Al cambiar de pista se observa la transición visual del disco; rotación fluida con 0 recomposiciones por segundo.

- [x] **Tarea 9: Android UI — Microinteracción animada del botón de Play en Biblioteca**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/ui/screens/LibraryScreen.kt`
  - **Detalle:** Implementar `AnimatedPlayButton` sobre las tarjetas e ítems de álbum y playlist con animación de escala tipo resorte (`spring`), feedback táctil háptico y distinción de estado en reproducción.
  - **Criterio de Aceptación:** Los botones de play en playlists y álbumes responden con rebote elástico al pulsar y muestran estado activo.

- [x] **Tarea 10: Android UI — Expansión de letras, pausa de auto-scroll al deslizar y Marquee en títulos largos**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/ui/screens/FullScreenPlayer.kt`, `app/src/main/java/com/k11000/monkify/ui/components/LyricsView.kt`, `app/src/main/java/com/k11000/monkify/ui/components/MiniPlayer.kt`
  - **Detalle:** Permitir que `LyricsView` se expanda en todo el alto disponible en `FullScreenPlayer`. Pausar el auto-scroll si `listState.isScrollInProgress` es verdadero. Aplicar `basicMarquee` en títulos largos.
  - **Criterio de Aceptación:** Letras legibles en formato vertical amplio; scroll manual respetado; títulos largos se desplazan suavemente.

- [x] **Tarea 11: Verificación integral final y suite de pruebas**
  - **Archivos:** Suite completa en ambos proyectos
  - **Detalle:** Ejecutar `npm test` en backend, `testDebugUnitTest` y `assembleRelease` en Android.
  - **Criterio de Aceptación:** 100% de pruebas unitarias pasando y build de release limpio.
