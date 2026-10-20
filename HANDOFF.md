# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.1 — Auditoría Técnica y Afinamiento Integral de UX/UI
**Fecha de cierre:** 2026-10-20
**Estado del build/tests:** ✅ pasando — 35/35 tests en Vitest en `cloudflare-backend` (`npm test`) y suite unitaria + build de Release de Android pasando al 100% en `e:\PG\vcp-s\Monkify-Android-Refine` (`.\gradlew.bat testDebugUnitTest assembleRelease` -> `BUILD SUCCESSFUL`).

---

## 1. Estado actual

- **Backend Cloudflare Worker (`cloudflare-backend`):**
  - Aislamiento DDL: `CREATE TABLE IF NOT EXISTS support_reports` movido a `src/db/schema.sql`.
  - Manejo de Range Requests en streaming R2: cálculo de `offset` y `length` para suffix ranges (`bytes=-N`) corregido y verificado en `test/index.spec.ts`.
  - Caché inmutable para streaming de audio (`Cache-Control: public, max-age=31536000, immutable`).
  - Cache-Control público (24h) y negativo (30s) en proxy de letras LRCLIB con fallback restringido estrictamente a 404.
  - Manejadores globales JSON en `src/index.ts` para rutas inexistentes (404) y excepciones no capturadas (500).
  - Validación estricta de tamaño de payload (máximo 64 KB) y sanitización anti-markdown injection en reportes de soporte.
- **Cliente Android (`Monkify-Android-Refine`):**
  - Preservación de métricas de reproducción (`playCount`, `lastPlayedAt`) en `favorite_tracks` al desmarcar favorito o borrar descarga en `LocalTrackRepository.kt`.
  - Prevención de condiciones de carrera en letras con `lyricsJob?.cancel()` y sincronización atómica de cola en `moveQueueItem` y `removeQueueItem` en `PlayerViewModel.kt`.
  - Actualización inmediata de seek en pausa reflejada en `_currentPosition.value`.
  - Registro seguro de receiver con `RECEIVER_NOT_EXPORTED` en `MonkifyPlaybackService.kt` para compatibilidad con Android 14+.
  - Desbloqueo asíncrono en `LockScreenActivity.kt` mediante `KeyguardDismissCallback` y apagado de pantalla en bolsillo (`isCoveredInPocket`).
  - Modo Zen Despejado: eliminación de textos fijos sobre el lienzo, auto-ocultado de píldora y guía tras 3s con `fadeOut()`, y debouncing canónico de doble toque (320 ms) en `HomeScreen.kt`.
  - MiniPlayer en modo Peek: se retrae a 18-20 dp con lengüeta interactiva y repliegue animado de `BottomNavBar` en modo Zen en `MainActivity.kt` y `MiniPlayer.kt`.
  - Vitrola cinemática en `VinylTurntable.kt`: cambio de disco físico deslizándose a la izquierda y entrando por la derecha, rotación a 60 FPS con `graphicsLayer` y desacople seguro del brazo fonocaptor.
  - Microinteracción elástica táctil con `AnimatedPlayButton` (escala spring 0.72x y respuesta háptica) en tarjetas y filas de `LibraryScreen.kt`.
  - Expansión vertical completa de `LyricsView` con `Modifier.weight(1f)` en `FullScreenPlayer.kt`, pausa de auto-scroll al deslizar manualmente y `basicMarquee()` en títulos largos.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Desacoplar la inicialización DDL de la tabla de soporte de las peticiones HTTP y moverla a `schema.sql`.
  **Por qué:** Evita comprobaciones redundantes contra SQLite D1 en cada reporte de soporte en runtime.
  **Alternativas descartadas:** Mantener `db.prepare(CREATE TABLE IF NOT EXISTS...)` en la ruta de ejecución de `POST /api/v1/support/report`.

- **Decisión:** Retener registros en `favorite_tracks` con `isFavorite = false` y `isDownloaded = false` si tienen métricas (`playCount > 0` o `lastPlayedAt > 0L`).
  **Por qué:** Evita perder los contadores de reproducción y la cronología para los algoritmos locales de recomendación ("Escuchado recientemente" y "Hecho para ti").
  **Alternativas descartadas:** Borrado físico incondicional `DELETE FROM favorite_tracks WHERE id = :id`.

- **Decisión:** Implementar debouncing canónico (320 ms) con corrutina cancelable en el gestor de toques del Modo Zen.
  **Por qué:** Evita que el primer toque de un doble toque alterne la reproducción (pausa/play) cuando la intención del usuario es restaurar la interfaz.
  **Alternativas descartadas:** Reaccionar inmediatamente al primer toque o requerir interacción exclusiva con la píldora superior.

- **Decisión:** Retraer el `MiniPlayer` a un modo Peek de 18 dp con lengüeta interactiva en lugar de ocultarlo al 100%.
  **Por qué:** Proporciona un punto de contacto táctil inmediato y discreto en la parte inferior para restaurar la interfaz sin saturar el lienzo visual de la animación ambiental.
  **Alternativas descartadas:** Ocultar completamente el MiniPlayer sin indicador de recuperación.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `cloudflare-backend/src/db/schema.sql` | DDL de `support_reports` incorporado al esquema |
| `cloudflare-backend/src/routes/support.ts` | Límite de 64 KB, sanitización de diagnósticos y eliminación de DDL en runtime |
| `cloudflare-backend/src/routes/stream.ts` | Corrección de cálculo de rangos sufijo `bytes=-N` y cabeceras de caché inmutable |
| `cloudflare-backend/src/routes/tracks.ts` | Cabeceras de caché pública/negativa y restricción de fallback LRCLIB a 404 |
| `cloudflare-backend/src/index.ts` | Handlers JSON globales para errores 404 y 500 |
| `cloudflare-backend/test/index.spec.ts` | Tests para suffix ranges, límites de payload y errores 404/500 |
| `Monkify-Android-Refine/.../LocalTrackRepository.kt` | Preservación de métricas de reproducción en Room |
| `Monkify-Android-Refine/.../PlayerViewModel.kt` | Cancelación de `lyricsJob`, sincronización atómica de cola y seek instantáneo |
| `Monkify-Android-Refine/.../MonkifyPlaybackService.kt` | `ContextCompat.registerReceiver` con `RECEIVER_NOT_EXPORTED` |
| `Monkify-Android-Refine/.../LockScreenActivity.kt` | Desbloqueo asíncrono con `KeyguardDismissCallback` y apagado en bolsillo |
| `Monkify-Android-Refine/.../HomeScreen.kt` | Supresión de títulos sobre el canvas, auto-hide de guías en 3s y debouncing |
| `Monkify-Android-Refine/.../MainActivity.kt` | Repliegue de `BottomNavBar` y modo Peek de MiniPlayer en modo Zen |
| `Monkify-Android-Refine/.../MiniPlayer.kt` | Soporte de `isZenPeek` con lengüeta sutil de 18 dp y `basicMarquee()` |
| `Monkify-Android-Refine/.../VinylTurntable.kt` | Cambio físico de vinilo (slide izquierda/derecha), desacople de aguja y `graphicsLayer` |
| `Monkify-Android-Refine/.../FullScreenPlayer.kt` | Expansión vertical `weight(1f)` para letras y `basicMarquee()` |
| `Monkify-Android-Refine/.../LyricsView.kt` | Pausa de auto-scroll al deslizar manualmente (`!listState.isScrollInProgress`) |
| `Monkify-Android-Refine/.../LibraryScreen.kt` | `AnimatedPlayButton` elástico tipo spring con respuesta háptica |
| `Monkify-Android-Refine/.../LocalDatabaseTest.kt` | Test unitario de preservación de métricas |
| `Monkify-Android-Refine/.../PlayerControlsTest.kt` | Test unitario de cancelación de carrera en letras y cola |

## 4. Pendientes explícitos para la próxima fase

- [ ] Verificar y auditar en emulador o dispositivo físico cambios adicionales de interfaz antes de generar el tag o release final.
- [ ] Evaluar posibles animaciones adicionales o transiciones en detalle de artista/búsqueda.
- [ ] Ejecutar despliegue coordinado de Cloudflare Worker (`npx wrangler deploy`) y fusión hacia `main` tras validación humana final.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Dispositivos antiguos sin sensor de proximidad en `LockScreenActivity`.
  **Impacto:** El estado `isCoveredInPocket` permanecerá en `false`, sin afectar el funcionamiento normal de la pantalla de bloqueo.
  **Mitigación sugerida:** El sensor se obtiene como nullable (`Sensor?`) y ya cuenta con verificación segura `?.let`.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# 1. Verificar Backend Cloudflare (desde e:\PG\vcp-s\Monkify\cloudflare-backend)
npm test

# 2. Verificar Android Unit Tests y Release Packaging (desde e:\PG\vcp-s\Monkify-Android-Refine)
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
