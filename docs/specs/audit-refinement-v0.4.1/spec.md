# Especificación Técnica: Auditoría y Afinamiento Integral v0.4.1 (Revisada)

## 1. Visión y Propósito
El objetivo de esta iteración es afinar, pulir la experiencia audiovisual y blindar la aplicación **Monkify** (cliente Android y backend Cloudflare Workers) antes de que los usuarios noten fallas, fricciones o textos innecesarios en pantalla.

A partir de la auditoría técnica de los subagentes y de los requerimientos específicos de UX del usuario, se atienden las correcciones de backend y arquitectura Android, y se perfecciona el **Modo Zen Despejado**, la **animación de cambio de vinilo** y la **animación interactiva de reproducción en tarjetas de la Biblioteca**.

---

## 2. Requerimientos y Alcance (In-Scope)

### A. Experiencia Inmersiva de Modo Zen Despejado (HomeScreen & MainActivity)
1. **Lienzo Zen 100% Limpio (Sin textos superpuestos):** Al activar la pantalla despejada en la pantalla de inicio, se suprime por completo el nombre de la canción y del autor sobre el lienzo. La animación ambiental (`FROGGY`, `COSMOS`, visualizador) debe apreciarse limpia sin estorbos tipográficos.
2. **Auto-ocultado de Instrucciones a los 3 Segundos:** La píldora superior de retorno y el texto guía de gestos se mostrarán brevemente al entrar al modo despejado y se desvanecerán suavemente (`fadeOut(tween(600))`) tras 3 segundos de inactividad, logrando una inmersión visual total.
3. **MiniPlayer Oculto y Asomando (Peek Bottom Bar):** En el modo Zen despejado, la barra de navegación se oculta y el `MiniPlayer` se retrae hacia abajo mediante una animación de desplazamiento (`slideOutVertically` con offset residual), quedando únicamente un borde o lengüeta sutil asomando en el fondo inferior de la pantalla. Al tocar dicha lengüeta o realizar doble toque en cualquier parte del lienzo, la interfaz se restaura de inmediato.
4. **Corrección Canónica de Gestos:** El doble toque restaura la interfaz usando `detectTapGestures(onTap = ..., onDoubleTap = ...)`, eliminando el falso disparo de pausa/reproducción al primer toque.

### B. Animaciones Audiovisuales y Microinteracciones (UI/UX)
1. **Animación de Cambio de Disco de Vinilo (`VinylTurntable.kt`):** Al cambiar de pista (`coverUrl`), el disco experimenta una transición cinemática de cambio de acetato: salida fluida con desvanecimiento/escala y entrada del nuevo disco con su carátula, acompañada por el movimiento reactivo del brazo fonocaptor.
2. **Rotación de Vinilo a 60 FPS sin Recomposiciones:** Migrar la rotación angular a `Modifier.graphicsLayer { rotationZ = ... }` en fase de dibujo de GPU.
3. **Animación del Botón de Play en Biblioteca (`LibraryScreen.kt`):** Convertir el icono estático de reproducción en tarjetas e ítems de álbum y playlist en un botón interactivo animado (botón circular estilizado con escala elástica al pulsar, feedback háptico táctil e indicador visual cuando la lista está en reproducción activa).
4. **Expansión y Scroll de Letras (`FullScreenPlayer.kt` & `LyricsView.kt`):** `LyricsView` se expande en el espacio vertical disponible en lugar de confinarse a un cuadro de 320dp. El auto-scroll se suspende mientras el usuario interactúe con la lista manualmente.
5. **Texto Largo con Marquee:** Títulos de canciones largos se desplazan suavemente con `basicMarquee` en `MiniPlayer.kt` y `FullScreenPlayer.kt`.

### C. Android Core & Persistencia (Estabilidad P0)
1. **Preservación del Historial en Room (`LocalTrackRepository.kt`):** Desmarcar favoritos (`toggleFavorite`) o eliminar descargas (`removeDownload`) no borra la fila física de `favorite_tracks` si la canción tiene reproducciones (`playCount > 0` o `lastPlayedAt > 0L`), salvaguardando las estadísticas del usuario.
2. **Cancelación de Peticiones de Letras Desfasadas (`PlayerViewModel.kt`):** `loadLyricsForTrack` cancela su `Job` previo al cambiar de canción para evitar que respuestas lentas sobreescriban la letra de la pista actual.
3. **Sincronización de Cola y Seek:** Reflejar la posición de inmediato en `_currentPosition.value` al hacer seek en pausa y notificar a `stateHolder` en cambios de cola.
4. **Registro Seguro de BroadcastReceiver:** Declarar `ContextCompat.RECEIVER_NOT_EXPORTED` en `MonkifyPlaybackService` para compatibilidad con Android 14+ (API 34+).

### D. Backend Cloudflare Workers & Seguridad
1. **Aislamiento DDL en Soporte (`support.ts` & `schema.sql`):** Trasladar `CREATE TABLE` fuera de la ruta caliente a `schema.sql`, limitar el payload a 64 KB y escapar caracteres markdown.
2. **Corrección de Suffix Ranges en Streaming (`stream.ts`):** Manejar correctamente `range.suffix` en peticiones `bytes=-N` de ExoPlayer y agregar cabeceras `Cache-Control`.
3. **Protección Anti-Hammering en Letras (`tracks.ts`):** No ejecutar búsqueda secundaria si LRCLIB devuelve 429 o 5xx; fijar cabeceras de caché públicas y negativas.

---

## 3. Criterios de Aceptación
1. En modo Zen despejado, no hay textos de canción/artista fijos sobre el fondo.
2. Las instrucciones del modo Zen se ocultan automáticamente a los 3 segundos.
3. El `MiniPlayer` se retrae hacia el borde inferior mostrando solo una barra peek sutil, restaurándose con un toque.
4. Cambiar de canción en el reproductor de vinilo muestra una transición fluida del disco de acetato.
5. Los botones de reproducción en álbumes y playlists de la Biblioteca reaccionan con microinteracción elástica al pulsar.
6. `.\gradlew.bat testDebugUnitTest` y `assembleRelease` finalizan con `BUILD SUCCESSFUL`.
7. `npm test` en `cloudflare-backend` aprueba el 100% de los tests en Vitest.
