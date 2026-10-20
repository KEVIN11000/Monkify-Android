# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.0 (Parte 1) — Enriquecimiento de Carátulas, Traducción de Títulos No Latinos y Soporte Completo de Álbumes (Issue #1)
**Fecha de cierre:** 2026-10-17
**Estado del build/tests:** ✅ pasando — 29/29 tests en Vitest en `cloudflare-backend` (`npm test`), Worker desplegado en producción (`https://monkify-backend.monkify.workers.dev`, Version ID `e181c95a-0619-475d-af36-735ffaa08867`) con los 18 lotes SQL aplicados en D1, y suite unitaria + build de Android pasando al 100% en `e:\PG\vcp-s\Monkify-Android-Refine` (`.\gradlew.bat testDebugUnitTest assembleRelease` -> `BUILD SUCCESSFUL`, commit `3fb4db8` en rama `refine/v0.4`).

---

## 1. Estado actual

- **Catálogo Enriquecido en `catalog.db` y Cloudflare D1 (`monkify-db`):**
  - 8.619 / 8.619 pistas (100%) actualizadas con `duration_ms > 0` cruzando los CSV originales de `spotify_playlists/*.csv`.
  - 8.604 / 8.619 pistas (99,83%) con `cover_url` oficiales de Spotify resueltos vía oEmbed y cacheados en `spotify-harvester/cover_cache.json`.
  - 100% de artistas normalizados reemplazando `;` por `, ` (`0` pistas con `;`) y limpiando sufijos redundantes (`- Remastered 20XX`, `(Official Video)`).
  - ~129 pistas en alfabetos no latinos (japonés, coreano, chino, cirílico) conservan su nombre en idioma original con su traducción al español entre paréntesis (`Original (Traducción)`, ej. `残響散歌 (Canción de resonancia)`, `アイドル (Ídolo)`).
  - Columna `file_key` preservada intacta en las 8.619 pistas para garantizar compatibilidad de streaming HTTP 206 desde Cloudflare R2.
- **Backend Cloudflare Worker (`cloudflare-backend`):**
  - Nuevos endpoints `GET /api/v1/albums` (con paginación y filtro `?q=`) y `GET /api/v1/albums/tracks?album=...` implementados en `src/routes/albums.ts` y desplegados en producción.
  - `GET /api/v1/tracks` actualizado para buscar también por `OR album LIKE ?` y ordenar priorizando pistas con carátula.
- **App Android (`e:\PG\vcp-s\Monkify-Android-Refine`, rama `refine/v0.4`, commit `3fb4db8`):**
  - Creado componente reutilizable `TrackArtwork.kt` y eliminadas todas las referencias a `"https://via.placeholder.com/..."` (`0` ocurrencias restantes en todo el proyecto).
  - Implementados `AlbumSummaryDto.kt`, `AlbumDetailViewModel.kt` y `AlbumDetailScreen.kt` con visualización de portada, artista, conteo de pistas, botón **"Reproducir Todo"** y botón **"Descargar"** álbum offline.
  - Integrado chip `Álbumes` en `LibraryScreen.kt` y `LibraryViewModel.kt` (con fallback offline agrupando pistas locales cuando no hay red) y navegación a álbumes desde `HomeScreen.kt`.
  - Versión incrementada a `v0.4.0` (`versionCode = 4`, `versionName = "0.4.0"`), **pendiente de publicar Release/Tag en GitHub** hasta incorporar el siguiente lote de mejoras.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Conservar los nombres en sus idiomas originales y añadir la traducción al español entre paréntesis (`Original (Traducción)`), apegándose a los datos de los CSV importados de Spotify.
  **Por qué:** Permite identificar visualmente las canciones en alfabetos no latinos (japonés, coreano, chino, cirílico) sin perder su título original ni alterar el `file_key` de almacenamiento en R2.
  **Alternativas descartadas:** Reemplazar por completo el título original o romanizar fonéticamente sin traducción.

- **Decisión:** Derivar el catálogo de álbumes dinámicamente mediante agrupación SQL (`GROUP BY album`) en D1 (`GET /api/v1/albums`) y agrupación en memoria en `LibraryViewModel` como fallback offline.
  **Por qué:** Evita migraciones destructivas sobre las 8.619 filas activas en producción y permite explorar álbumes descargados incluso sin conexión a internet.
  **Alternativas descartadas:** Crear tablas relacionales separadas de álbumes que requieran re-ingestar el catálogo.

- **Decisión:** Postergar el `git push`, merge a `main` y creación del tag/release `v0.4.0` de Android para la siguiente sesión.
  **Por qué:** El usuario solicitó añadir más mejoras antes de lanzar oficialmente la actualización `v0.4.0`.
  **Alternativas descartadas:** Publicar un release intermedio antes de completar las mejoras adicionales.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `docs/specs/catalog-artwork-albums-refinement/spec.md` | Especificación SDD para carátulas, álbumes y normalización/traducción de títulos. |
| `docs/specs/catalog-artwork-albums-refinement/plan.md` | Arquitectura técnica, contratos de API y estrategia de preservación de `file_key`. |
| `docs/specs/catalog-artwork-albums-refinement/tasks.md` | Checklist de 6 tareas atómicas completadas (`[x]`). |
| `spotify-harvester/enrich_catalog_metadata.py` | Script de cruce con CSVs de Spotify, resolución oEmbed de portadas, traducción CJK y generación de lotes SQL D1. |
| `spotify-harvester/core/extractor.py` | Extracción de `cover_url` y normalización de artistas `, ` para futuras ingestas. |
| `cloudflare-backend/src/routes/albums.ts` | Endpoints `GET /api/v1/albums` y `GET /api/v1/albums/tracks`. |
| `cloudflare-backend/src/routes/tracks.ts` | Inclusión de `OR album LIKE ?` en búsqueda y ordenamiento con prioridad de carátulas. |
| `cloudflare-backend/test/index.spec.ts` | 29 pruebas de integración en Vitest incluyendo endpoints de álbumes. |
| `Monkify-Android-Refine/.../ui/components/TrackArtwork.kt` | Componente Compose con `SubcomposeAsyncImage` y fallback vectorial nativo. |
| `Monkify-Android-Refine/.../ui/screens/AlbumDetailScreen.kt` | Pantalla de detalle de álbum con reproducción de cola y descarga offline completa. |
| `Monkify-Android-Refine/.../ui/screens/viewmodel/AlbumDetailViewModel.kt` | Lógica de carga de pistas de álbum desde API con fallback a Room local. |
| `Monkify-Android-Refine/.../ui/screens/LibraryScreen.kt` | Chip `Álbumes`, listado `LibraryAlbumItem` y búsqueda reactiva de álbumes. |
| `Monkify-Android-Refine/.../ui/screens/viewmodel/LibraryViewModel.kt` | Estado `albums`, `filteredAlbums` y agrupación offline `deriveLocalAlbums`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] **Definir e implementar el siguiente lote de mejoras en Android/Backend (`refine/v0.4`):** Recibir los nuevos requerimientos del usuario para la versión `v0.4.0`, especificarlos bajo SDD e implementarlos sobre `e:\PG\vcp-s\Monkify-Android-Refine`.
- [ ] **Publicación del Release `v0.4.0` de Android y Cierre del Issue #1:** Una vez completadas las mejoras adicionales, fusionar `refine/v0.4` hacia `main`, crear el tag `v0.4.0` para disparar el workflow `.github/workflows/release.yml` (que publica el APK en GitHub Releases) y cerrar el Issue #1 en `KEVIN11000/Monkify-Android`.
- [ ] **Sincronizar vista de Álbumes en Monkify Desktop (`win app/`):** Conectar opcionalmente `/api/v1/albums` en el cliente de escritorio si se requiere paridad con Android.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** 15 pistas sobre 8.619 (0,17%) no devolvieron miniatura en Spotify oEmbed por tratarse de tracks dados de baja regionalmente en Spotify.
  **Impacto:** Mínimo; en esas 15 pistas el componente `TrackArtwork` renderiza automáticamente el arte vectorial nativo de Monkify sin mostrar íconos rotos.
  **Mitigación sugerida:** No requiere acción inmediata; `TrackArtwork` garantiza el fallback visual limpio.

- **Riesgo:** El archivo `spotify-harvester/cover_cache.json` y la carpeta `cloudflare-backend/scripts/d1_enrich_batches/` contienen datos masivos de enriquecimiento local.
  **Impacto:** No deben subirse innecesariamente si se versiona el monorepo raíz.
  **Mitigación sugerida:** Mantenerlos excluidos en `.gitignore` cuando se gestione git en el directorio raíz.

## 6. Cómo verificar que este handoff sigue vigente

```powershell
# 1. Verificar tests unitarios de Android en el worktree refine/v0.4
cd e:\PG\vcp-s\Monkify-Android-Refine
.\gradlew.bat testDebugUnitTest

# 2. Verificar suite de pruebas en Cloudflare Worker
cd e:\PG\vcp-s\Monkify\cloudflare-backend
npm test

# 3. Verificar endpoints de pistas y álbumes en producción
curl.exe -s "https://monkify-backend.monkify.workers.dev/api/v1/albums?limit=3"
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
