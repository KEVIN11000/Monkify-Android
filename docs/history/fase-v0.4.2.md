# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.2 — Release Consolidado de Correcciones y Mejoras UX
**Fecha de cierre:** 2026-10-27
**Estado del build/tests:** ✅ pasando — 47/47 tests en Vitest en `cloudflare-backend` (`npm test`) y suite unitaria + build de Release de Android pasando al 100% en `e:\PG\vcp-s\Monkify-Android-Refine` (`.\gradlew.bat testDebugUnitTest assembleRelease` -> `BUILD SUCCESSFUL`, APK generado: `app/build/outputs/apk/release/app-release.apk` de 17.7 MB).

---

## 1. Estado actual

- **Carga y decodificación de canciones por álbum corregida:** URLs con caracteres especiales (`%20`, `+`) decodificadas con `URLDecoder.decode(..., "UTF-8")` en `MonkifyNavGraph.kt`, sanitización de nombres en `AlbumDetailViewModel.kt`, y tolerancia en `LocalTrackRepository.kt` y backend Cloudflare `src/routes/albums.ts`. Cubierto por tests unitarios en `AlbumDetailViewModelTest.kt` y `test/index.spec.ts`.
- **Transición y finalización de cola en modo aleatorio (Shuffle) corregida:** `MonkifyPlaybackService.kt` y `PlayerViewModel.kt` resuelven la selección circular cuando `firstIndex == currentIndex`. Cubierto por tests unitarios en `PlayerControlsTest.kt`.
- **Botón "Aleatorio" en Biblioteca activa explícitamente shuffle:** `playerViewModel.setShuffleEnabled(true)` activado al pulsar "Aleatorio" en `LibraryScreen.kt`.
- **Ciclos dinámicos Día/Noche en Zen Froggy y Cosmos HD:** `ZenBackground.kt` con gradientes de cielo, sol/luna procedural con pulso suave, nubes esponjosas estilo Google Weather, iluminación de colinas y estanque, nenúfares, Froggy con mejillas sonrojadas y destellos en los ojos, y partículas contextuales (rocío al amanecer, semillas de diente de león de día, ascuas al atardecer, luciérnagas de noche).
- **Animaciones zen infinitas y continuas:** Reemplazo de operaciones módulo `%` por armónicos sinusoidales puros (\(\sin\), \(\cos\)) para eliminar cualquier costura o salto perceptible en el reinicio del ciclo. Velocidades reducidas a 30-50s y pulso respiratorio de 6.5-12s. Verificado con `@Preview` en Compose y simulación interactiva en `zen_visualizer_preview.html`.
- **Cosmos HD optimizado:** Galaxia espiral inclinada a -26°, 4 capas de nebulosas, 72 estrellas con picos de difracción de 4 puntas y meteoros sinusoidales suaves sin saltos perceptibles.
- **Selección múltiple por lote en Biblioteca:** Modo de selección con encabezado contextual, indicador circular por fila, pulsación prolongada con respuesta háptica (`combinedClickable`), y barra de acciones inferior flotante para descarga masiva, eliminación de favoritos masiva y borrado de descargas masivo en `LibraryScreen.kt` y `LibraryViewModel.kt`. Cubierto por 5 tests unitarios en `LibraryViewModelTest.kt`.
- **Reconciliación automática y resiliente de descargas:** `DownloadRepositoryImpl.kt` escanea el almacenamiento físico (`musicDir`, almacenamiento interno y carpetas heredadas) en segundo plano al inicializarse, reconciliando pistas existentes con la base de datos Room si fue purgada tras reinstalación. Cubierto por test unitario en `DownloadRepositoryTest.kt`.
- **Build de producción Release generado:** `app/build/outputs/apk/release/app-release.apk` (17.7 MB, 17,766,562 bytes) compilado exitosamente.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Agrupar todos los tickets de soporte del servidor y mejoras en un Release Consolidado (*Release Train* v0.4.2) en lugar de múltiples versiones menores incrementales.
  **Por qué:** Evitar fatiga de actualizaciones en los usuarios y entregar un salto de calidad integral en una sola versión estable.
- **Decisión:** Desacoplar y archivar la especificación borrador de Monkify Connect en la rama `backup/monkify-connect-draft`.
  **Por qué:** El usuario especificó no avanzar en Monkify Connect hasta nuevo aviso para concentrar el esfuerzo en pulir la app principal y atender reportes de usuarios reales.
- **Decisión:** Modularizar la integración de Rive (`.riv`) y clima en tiempo real para la siguiente fase inmediata, creando la hoja de ruta en `docs/specs/v042-consolidated-fixes-and-polish/weather_froggy_roadmap.md`.
  **Por qué:** El usuario solicitó terminar primero todos los pendientes del release v0.4.2 y luego abordar Rive + Weather con la biblioteca Open-Meteo y los assets del repositorio `https://github.com/R0rt1z2/Froggy.git`.
- **Decisión:** Usar funciones armónicas trigonométricas continuas (\(\sin\), \(\cos\)) para las animaciones cíclicas de Canvas en `ZenBackground.kt` en vez de interpolación basada en tiempo y módulo `%`.
  **Por qué:** El operador de módulo introduce un salto discontinuo perceptible en el reinicio del ciclo; las funciones periódicas puras garantizan derivadas continuas y un bucle infinito imperceptible.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/.../MonkifyNavGraph.kt` | Decodificación UTF-8 de parámetros de álbum/playlist en navegación para resolver el ticket de álbumes vacíos con espacios o signos más. |
| `app/.../viewmodel/AlbumDetailViewModel.kt` | Sanitización de nombres de álbum (`cleanAlbum`) y fallback query para evitar listas vacías. |
| `app/.../data/repository/LocalTrackRepository.kt` | Comparación tolerante de álbumes ignorando espacios/signos más y nuevo método `removeFavorite` preservando descargas e historial. |
| `cloudflare-backend/src/routes/albums.ts` | Normalización de `+` a espacios con `COLLATE NOCASE` en la base D1. |
| `app/.../MonkifyPlaybackService.kt` | Solución al bloqueo circular de selección aleatoria al final de la cola en modo shuffle. |
| `app/.../playback/PlayerViewModel.kt` | Agregado de `setShuffleEnabled(Boolean)` y desacople de lógica de selección aleatoria. |
| `app/.../ui/screens/LibraryScreen.kt` | Activación de shuffle en botón "Aleatorio", selección múltiple por lote con barra de acciones inferior flotante y soporte de pulsación prolongada háptica. |
| `app/.../ui/components/ZenBackground.kt` | Ciclos dinámicos Día/Noche en Froggy, nubes esponjosas, partículas contextuales, animaciones armónicas continuas sin costura y Cosmos HD con galaxia espiral. |
| `app/.../domain/repository/DownloadRepository.kt` | Declaración de `suspend fun reconcileDownloads(): Int`. |
| `app/.../data/repository/DownloadRepositoryImpl.kt` | Implementación de reconciliación automática de descargas en segundo plano al inicializarse escaneando almacenamiento físico. |
| `app/.../ui/screens/viewmodel/LibraryViewModel.kt` | Estado y acciones de selección múltiple (descarga por lote, eliminación de favoritos por lote, borrado de descargas por lote). |
| `app/src/test/.../*Test.kt` | Suites de pruebas unitarias para `PlayerControlsTest`, `AlbumDetailViewModelTest`, `LibraryViewModelTest` y `DownloadRepositoryTest`. |
| `docs/specs/v042-consolidated-fixes-and-polish/*` | Especificaciones completas SDD (`spec.md`, `plan.md`, `tasks.md`, `weather_froggy_roadmap.md`). |

## 4. Pendientes explícitos para la próxima fase

- [ ] Implementar la Fase Rive (`.riv`) + Clima en tiempo real (Open-Meteo API) siguiendo `weather_froggy_roadmap.md`.
- [ ] Agregar dependencia `app.rive:rive-android:11.2.1` en `app/build.gradle.kts`.
- [ ] Descargar e integrar los assets vectoriales `.riv` de Froggy desde `https://github.com/R0rt1z2/Froggy.git` en `app/src/main/res/raw/`.
- [ ] Implementar `WeatherRepository` con llamadas a `https://api.open-meteo.com/v1/forecast` y caché local Room/DataStore de 2-3 horas para optimizar consumo de red y batería.
- [ ] Mapear códigos meteorológicos WMO a las animaciones de Froggy / Zen según corresponda (soleado, lluvia, nieve, tormenta, nublado).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Peso de la app al agregar el runtime de Rive (`app.rive:rive-android`).
  **Impacto:** El APK podría aumentar entre 1.5 y 2.5 MB debido a las librerías nativas C++ de renderizado vectorial de Rive.
  **Mitigación sugerida:** Utilizar ABI splits o App Bundles (`.aab`) en producción para empaquetar únicamente las arquitecturas necesarias (`arm64-v8a`, `armeabi-v7a`).
- **Riesgo:** Permisos de ubicación para el clima en tiempo real.
  **Impacto:** Solicitar permisos de ubicación fina (`ACCESS_FINE_LOCATION`) puede generar rechazo de usuarios preocupados por la privacidad.
  **Mitigación sugerida:** Usar solo ubicación aproximada (`ACCESS_COARSE_LOCATION`) con fallback manual o selector de ciudad sin requerir GPS mandatorio.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En el cliente Android:
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleRelease

# En el backend de Cloudflare:
cd "../Monkify/cloudflare-backend" && npm test
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
