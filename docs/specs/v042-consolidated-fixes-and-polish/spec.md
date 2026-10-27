# Especificación de Requerimientos: Consolidated Fixes & UX Polish (v0.4.2)

**Feature:** `v042-consolidated-fixes-and-polish`  
**Directorio de especificación:** `docs/specs/v042-consolidated-fixes-and-polish/`  
**Fecha:** 2026-10-27  
**Estado:** Fase 0 — Especificación (Modo Planificador)

---

## 1. Visión y Alcance

Este release consolidado (*Batch Release*) atiende los tickets reales de soporte reportados por los usuarios en producción (desde dispositivos con v0.4.0) y subsana bugs críticos detectados en navegación y reproducción multimedia, unificando mejoras de estabilidad, resiliencia y calidad visual para evitar la fragmentación de versiones y la fatiga de actualización (*Update Fatigue*).

---

## 2. Requerimientos Funcionales y Casos de Uso

### CU-01: Corrección de Carga de Canciones en Detalle de Álbum (`AlbumDetailScreen`)
- **Problema:** Al hacer clic en un álbum desde Inicio o Biblioteca (el cual indica tener $N$ canciones), la pantalla de detalle del álbum se abre vacía con 0 canciones.
- **Causa Raíz:** En `Destinations.albumDetail`, el nombre del álbum se codifica con `URLEncoder.encode(..., "UTF-8")` transformando espacios en signos `+` (ej. `Queen+II`). En Navigation Compose, `backStackEntry.arguments?.getString("album")` conserva los `+` literales sin decodificar. Al invocar Retrofit `MonkifyApi.getAlbumTracks(album)` o Room `localTrackRepository.getTracksByAlbum(album)`, se consulta por `WHERE album = 'Queen+II'` en vez de `'Queen II'`, retornando 0 canciones tanto en API como en fallback local.
- **Comportamiento Esperado:** 
  1. Decodificación simétrica y robusta en el punto de navegación y en el ViewModel (`URLDecoder.decode(album, "UTF-8")`).
  2. Normalización de espacios y caracteres especiales en la consulta tanto en cliente Android como en el endpoint backend de Cloudflare (`WHERE TRIM(album) = TRIM(?) COLLATE NOCASE`).
  3. Carga inmediata de las canciones del álbum tanto online como desde la base de datos local.

### CU-02: Corrección del Flujo de Reproducción Aleatoria (Shuffle) al Final de Playlist
- **Problema (Ticket `rep_1793031577998_qp7kfv`):** Al reproducir una playlist en modo aleatorio (Shuffle activado), la reproducción avanza normalmente, pero al llegar a la última canción física de la lista, el reproductor se cuelga/detiene en lugar de saltar a las canciones que aún no se han reproducido en el orden aleatorio.
- **Comportamiento Esperado:**
  1. En `MonkifyPlaybackService` y `PlayerViewModel`, verificar que `player.shuffleModeEnabled` utilice un gestor de orden aleatorio circular (`DefaultShuffleOrder` o ciclo de cola aleatorizada).
  2. Cuando una pista termina y `hasNextMediaItem()` es falso según la lista física pero verdadero en el orden de reproducción aleatoria, o cuando se completa el ciclo sin repetición, Media3 debe avanzar fluidamente sin congelar la interfaz ni dejar el estado en *buffering infinito*.

### CU-03: Zen Froggy Diurno Dinámico & Cosmos HD
- **Problema (Ticket `rep_1793031498656_wr4pdo` - Parte 1):** La animación del fondo de Froggy es estática con respecto al ciclo del día (se muestra de noche incluso de día).
- **Comportamiento Esperado:**
  1. `ZenFroggyView` detecta la franja horaria local del dispositivo (Amanecer: 06:00-08:59, Día: 09:00-17:59, Atardecer: 18:00-20:59, Noche: 21:00-05:59) y conmuta la paleta atmosférica, color de cielo y gradiente lumínico en tiempo real.
  2. En el motor Zen `Cosmos`, incorporar variantes visuales nítidas y fluidas con estética astronómica profunda de alta resolución espacial.

### CU-04: Selección Múltiple y Acciones por Lote en Biblioteca
- **Problema (Ticket `rep_1793031498656_wr4pdo` - Parte 2):** Los usuarios no pueden seleccionar múltiples pistas o listas en la biblioteca para realizar operaciones en lote.
- **Comportamiento Esperado:**
  1. En `LibraryScreen`, permitir activar el "Modo Selección" (vía pulsación prolongada / Long-press o botón de barra de herramientas).
  2. Marcar/desmarcar elementos individuales con checkbox táctil y contador en cabecera ("$M$ seleccionados").
  3. Barra de acciones flotante/inferior para: Descargar en lote, Eliminar de favoritos / biblioteca en lote, y Añadir a Playlist.

### CU-05: Resiliencia de Descargas Locales ante Actualizaciones / Reinstalación
- **Problema (Ticket `rep_1793044207135_4erb70`):** Al actualizar la app mediante APK externo o reinstalar por conflicto de paquetes, el sandbox privado de Android borra los audios descargados.
- **Comportamiento Esperado:**
  1. Configurar la política de almacenamiento de descargas para utilizar opcionalmente almacenamiento compartido estándar (`Environment.DIRECTORY_MUSIC / Monkify`) mediante SAF o MediaStore cuando los permisos lo permitan, preservando la música descargada si la app es desinstalada.
  2. Implementar un mecanismo de auto-reconocimiento (escaneo de biblioteca local) que detecte si los archivos de audio ya existen en el disco tras una reinstalación, re-vinculándolos a Room sin necesidad de volver a descargarlos.

---

## 3. Lo que Queda Excluido (No-Scope de esta Fase)

- **Monkify Connect:** Excluido explícitamente por el usuario; se trasladará y desarrollará en el repositorio inicial de Desktop.
- **Reescritura completa del motor de audio:** Se mantiene la integración probada con Media3/ExoPlayer, sin reemplazar la arquitectura base del servicio.
- **Sincronización Cloud de base de datos multi-usuario:** Monkify opera con catálogo público en backend y persistencia local sin cuentas de usuario.

---

## 4. Criterios de Aceptación Globales

1. Entrar a cualquier álbum desde Inicio o Biblioteca muestra de inmediato la lista de canciones con sus títulos, duración y opciones de reproducción.
2. La reproducción con Shuffle activado transiciona sin interrupciones entre todas las canciones de la lista, sin colgarse en los extremos.
3. El fondo Zen de Froggy cambia su tonalidad según la hora del sistema (mañana, tarde, noche).
4. El usuario puede seleccionar múltiples canciones en la biblioteca y eliminarlas o descargarlas con un solo toque.
5. Los tests unitarios de Android y backend pasan al 100% y `assembleRelease` compila exitosamente.
