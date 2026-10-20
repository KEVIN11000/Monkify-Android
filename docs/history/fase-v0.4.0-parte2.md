# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.0 (Parte 2) — Refinamiento Integral de Reproductor, Experiencia y Biblioteca
**Fecha de cierre:** 2026-10-20
**Estado del build/tests:** ✅ pasando — 33/33 tests en Vitest en `cloudflare-backend` (`npm test`), Worker desplegado en producción (`https://monkify-backend.monkify.workers.dev`, Version ID `374be678-8a1e-41a8-ba58-2f996a7cfae0`), y suite unitaria + build de Release de Android pasando al 100% en `e:\PG\vcp-s\Monkify-Android-Refine` (`.\gradlew.bat testDebugUnitTest assembleRelease` -> `BUILD SUCCESSFUL`). Rama `refine/v0.4` fusionada a `origin/main` y tag `v0.4.0` publicado con release oficial en GitHub.

---

## 1. Estado actual

- **Backend Cloudflare Worker (`cloudflare-backend`):**
  - Endpoint `GET /api/v1/tracks?sort=latest` operativo en producción para ordenar novedades del servidor.
  - Endpoint `GET /api/v1/tracks/:id/lyrics` operativo con verificación en tabla D1 y fallback en vivo a LRCLIB API.
  - Endpoint `GET /api/v1/playlists` expone arreglo `cover_urls` con hasta 6 portadas para mosaicos dinámicos.
  - Endpoint `POST /api/v1/support/report` operativo con inserción en tabla D1 `support_reports` y creación de ID de ticket.
- **Cliente Android (`Monkify-Android`, commit `a5da54e` en `main` y tag `v0.4.0`):**
  - Soporte Picture-in-Picture (PiP) eliminado por completo del manifiesto y la UI.
  - Escudo táctil en `FullScreenPlayer` previene cambios involuntarios de canción fuera de la cola de la lista activa.
  - `PlaybackSessionStateHolder` (singleton) mantiene el estado de reproducción y cola al minimizar o bloquear.
  - `LockScreenActivity` se superpone desde el primer bloqueo con flags de pantalla encendida.
  - Soporte de letras sincronizadas (.lrc) y texto plano estático (`timestampMs = -1L`) en `LyricsView` y `LrcParser`.
  - Pantalla de Inicio sin lista general vertical: banner dinámico con 7 saludos horarios y frases motivadoras, chip de Álbumes, cuadrícula rápida de los últimos 6 subidos, y secciones "Escuchado recientemente" y "Hecho para ti" conectadas a Room.
  - Pantallas de detalle de Playlist y Álbum con buscador interno y ordenamiento en memoria (`TrackSortOption`).
  - Carátulas automáticas en collage (mosaicos 2x2 y 3x2) con `PlaylistCollageArtwork` en playlists.
  - Ordenamiento global en todas las categorías de la Biblioteca con `LibrarySortOption`.
  - Pestaña "Todo" en Biblioteca incluye botones "Aleatorio" y "Desde la primera" sobre el catálogo completo.
  - Reselect en barra de navegación: scroll al tope en Biblioteca y reset de búsqueda a la vista inicial de exploración.
  - Fondos visuales `Froggy`, `Cosmos` y `Ninguno` (negro OLED) disponibles en Ajustes.
  - Envío de reporte de soporte en 1 clic desde Ajustes hacia la API de backend.
  - Repositorio `Monkify-Android` configurado como público en GitHub para actualizaciones automáticas.
  - Issue #1 de GitHub cerrado como completado.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Resolver letras en tiempo real vía LRCLIB en el backend en lugar de exigir archivos `.lrc` almacenados para cada canción.
  **Por qué:** Evita cargar y sincronizar manualmente miles de archivos en el bucket R2 y provee letras sincronizadas automáticas para la mayoría del catálogo comercial.
  **Alternativas descartadas:** Requerir subida manual de archivos `.lrc` por cada pista.

- **Decisión:** Eliminar el modo Picture-in-Picture (PiP) en favor de `MediaSession` y notificaciones estándar de Android.
  **Por qué:** PiP es innecesario en un reproductor de solo audio, consumía memoria y causaba discrepancias en eventos táctiles de la cola.
  **Alternativas descartadas:** Mantener ventana flotante PiP con canvas estático.

- **Decisión:** Configurar el repositorio `Monkify-Android` como Público en GitHub.
  **Por qué:** Permite que las consultas de actualización in-app (`GET /releases/latest`) y la descarga de APKs funcionen para cualquier teléfono sin requerir tokens personales de GitHub.
  **Alternativas descartadas:** Integrar un flujo OAuth de GitHub en la app solo para verificar actualizaciones.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `cloudflare-backend/src/routes/tracks.ts` | Parámetro `?sort=latest` y endpoint `/api/v1/tracks/:id/lyrics` con proxy LRCLIB |
| `cloudflare-backend/src/routes/playlists.ts` | Extracción de `cover_urls` (mosaico 4-6 portadas) |
| `cloudflare-backend/src/routes/support.ts` | Endpoint `POST /api/v1/support/report` con tabla D1 autoinicializada |
| `Monkify-Android/.../TrackEntity.kt` / `TrackDao.kt` | Campos y consultas reactivas `playCount` y `lastPlayedAt` |
| `Monkify-Android/.../PlaybackSessionStateHolder.kt` | Singleton centralizado de sesión y cola de reproducción |
| `Monkify-Android/.../FullScreenPlayer.kt` | Touch shield antipenetración y eliminación de controles PiP |
| `Monkify-Android/.../LockScreenActivity.kt` | Flags de encendido y superposición al primer bloqueo |
| `Monkify-Android/.../LrcParser.kt` | Soporte de texto plano (`timestampMs = -1L`) y formato .lrc |
| `Monkify-Android/.../ZenBackground.kt` | Motores de canvas `FROGGY`, `COSMOS` y `NONE` |
| `Monkify-Android/.../HomeScreen.kt` / `HomeViewModel.kt` | Saludos 7 franjas, chip Álbumes, Top 6 últimos, sin lista vertical |
| `Monkify-Android/.../LibraryScreen.kt` / `LibraryViewModel.kt` | Ordenamiento global, acciones Todo ("Aleatorio", "Desde la primera"), reselect scroll |
| `Monkify-Android/.../PlaylistDetailScreen.kt` / `AlbumDetailScreen.kt` | Búsqueda interna, menú de orden y mosaicos collage |
| `Monkify-Android/.../SearchScreen.kt` | Placeholder "¿Qué te gustaría escuchar?" y reselect reset |
| `Monkify-Android/.../BottomNavBar.kt` | Callbacks de reselect para inicio, biblioteca y búsqueda |
| `Monkify-Android/RELEASE_NOTES.md` | Notas formales de la versión v0.4.0 |

## 4. Pendientes explícitos para la próxima fase

- [ ] **Configurar Keystore persistente en GitHub Actions:** Crear un keystore permanente de release (`monkify-release.keystore`), subirlo en Base64 a los Secrets de GitHub (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) y actualizar `.github/workflows/release.yml` para evitar el error de conflicto de firmas (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) al actualizar entre versiones.
- [ ] **Auditar y enriquecer canciones restantes sin carátula:** Ejecutar consulta en D1 (`SELECT id, title, artist FROM tracks WHERE cover_url IS NULL OR cover_url = ''`) e implementar un enriquecedor secundario (Deezer API, iTunes Search API o thumbnails de YouTube `i.ytimg.com/vi/...`).
- [ ] **Auditar tickets de soporte acumulados en D1:** Ejecutar consulta en D1 (`SELECT * FROM support_reports ORDER BY created_at DESC`) para procesar los reportes y sugerencias enviados desde la app.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Conflicto de firmas criptográficas entre compilaciones de GitHub Actions al actualizar la app sin desinstalar.
  **Impacto:** Los usuarios que tengan instalada una versión anterior reciben "error con el paquete" y deben desinstalar la versión previa antes de instalar la nueva.
  **Mitigación sugerida:** Implementar el primer pendiente del punto 4 (Keystore persistente en GitHub Secrets) antes del release v0.4.1.

- **Riesgo:** Canciones huérfanas o sin carátula en el catálogo muestran el arte vectorial genérico de Monkify.
  **Impacto:** Experiencia visual reducida en temas sin metadatos completos.
  **Mitigación sugerida:** Ejecutar enriquecedor secundario hacia APIs de Deezer/iTunes o thumbnails de YouTube.

## 6. Cómo verificar que este handoff sigue vigente

```powershell
# 1. Verificar tests unitarios de Android en la rama principal
cd e:\PG\vcp-s\Monkify-Android-Refine
.\gradlew.bat testDebugUnitTest

# 2. Verificar compilación de Release en Android
.\gradlew.bat assembleRelease

# 3. Verificar tests del Worker en cloudflare-backend
cd e:\PG\vcp-s\Monkify\cloudflare-backend
npm test

# 4. Verificar salud de endpoints en producción
curl.exe -s "https://monkify-backend.monkify.workers.dev/api/v1/tracks?sort=latest&limit=1"
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
