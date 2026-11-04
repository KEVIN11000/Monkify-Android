# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.7 — Remediación Integral de Auditoría (P1, P2, P3, P4) y Refinamiento Ergonómico en Dispositivo Real
**Fecha de cierre:** 2026-11-02
**Estado del build/tests:** ✅ pasando — Suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`) con 37 suites y 192 tests ejecutados sin fallos (0 failures, BUILD SUCCESSFUL), compilación verificada (`assembleDebug`), e instalación activa en dispositivo real (`10.51.80.168:38239`) bajo la variante debug (`com.k11000.monkify.debug`).

---

## 1. Estado actual

- **P1 - Preservación Íntegra de Datos y Room Database:**
  - Añadidos 6 índices `@Index` en `TrackEntity.kt` (`isFavorite`, `isDownloaded`, `addedAt`, `lastPlayedAt`, `playCount`, `album`).
  - Migración segura Room v1 -> v2 configurada en `MonkifyDatabase.kt` y `DatabaseModule.kt`.
  - Implementado `@Transaction suspend fun upsertRemoteTracks(...)` en `TrackDao.kt` para blindar campos locales (`isFavorite`, `isDownloaded`, `localFilePath`, `playCount`, etc.) de cualquier sobrescritura al sincronizar con el backend.
  - Preservación explícita de `streamUrl` remoto en `TrackEntity.toDomain()`, manteniendo `localFilePath` desacoplado.
- **P2 - Blindaje de Seguridad y Fuga Cero:**
  - Keystore oficial (`monkify-release.jks`) retirado del tracking de Git y añadido a `.gitignore` junto con `*.jks`, `*.keystore` y `keystore.properties`.
  - Hardcoded signing passwords eliminadas de `app/build.gradle.kts`, leyendo de variables de entorno o archivo local no versionado.
  - Validación de host de confianza (`isTrustedMonkifyHost`) endurecida con límites de punto estricto (`.monkify.workers.dev`) en `AuthInterceptor.kt` y `PlayerViewModel.kt`.
  - `AppLogger.kt` con sanitización automática de tokens (`?token=***`, `Bearer ***`) y formato de fecha seguro.
  - Reglas de backup (`backup_rules.xml`, `data_extraction_rules.xml`) configuradas para excluir `datastore/` y `logs/`.
  - Creado `network_security_config.xml` para restringir tráfico claro (cleartext) únicamente a `localhost` y `127.0.0.1`.
- **P3 - Fidelidad Operacional y Cero Efectos Placebo:**
  - `PlaybackCache.kt` y `PlaybackModule.kt` conectados a `SimpleCache` con límite dinámico de almacenamiento (`cacheLimitMb`) y evicción segura por `simpleCache.removeResource(...)` sin borrado en caliente del directorio.
  - Enrutamiento de audio real implementado en `PlayerViewModel.selectDevice()` con `AudioDeviceInfo` (comprobado sin simulaciones).
  - Erradicadas las URLs y referencias de prueba de Rick Astley y pistas falsas de Pixabay.
  - Unificada la lectura de versión oficial desde `BuildConfig.VERSION_NAME` en toda la app.
- **P4 - Froggy Clima Híbrido, UI/UX y Estabilidad 60fps:**
  - Solicitud nativa de `ACCESS_COARSE_LOCATION` en `MainActivity.kt` con refresco automático de `WeatherRepository`.
  - Preferencia persistente `froggyScenery` (`AUTO`, `FIELDS`, `HILL`, `MUSHROOM`) añadida en DataStore y selector en `SettingsScreen.kt`.
  - `ServerStatusMonitor` unificado en `@Singleton` evitando duplicidades de banners.
  - Aislamiento de recomposición a 1Hz del progreso de reproducción en `FullScreenPlayer.kt` y `LockScreenActivity.kt`.
  - Insets de pantalla (`WindowInsets.statusBars` y `navigationBars`) adaptados a edge-to-edge en todas las pantallas.
  - **Refinamiento visual de cabecera (`HomeScreen.kt`):** Saludo, frase y clima envueltos en tarjeta oscura translúcida redondeada (`alpha = 0.65`, `16.dp`), frase motivadora expandida a `maxLines = 3` para eliminar truncamiento, con desvanecimiento fluido en Modo Zen.
  - **Refinamiento de selección múltiple (`LibraryScreen.kt`):** Eliminada la tarjeta flotante inferior que tapaba la lista; acciones de lote movidas a la barra superior contextual (`TopAppBar`) con menú desplegable `⋮`.
  - **Froggy Híbrido Web Live (`ZenBackground.kt`):** Inyección de configuración síncrona en HTML para pre-poblar `flutter.settings` y `flutter.rotate_hint_dismissed = true`, eliminando el modal de landscape, el reloj digital, la rosca de configuración y el aviso de GPS en la WebView.

---

## 2. Decisiones tomadas en esta fase

- **Decisión:** Implementar Froggy en modo Híbrido (Web con Flare oficial cuando hay red + Catálogo local de 60 WebPs en alta resolución como fallback offline).
  **Por qué:** Proporciona la animación fiel y relajante original del repositorio `R0rt1z2/Froggy` sin penalizar la disponibilidad cuando no hay conexión a internet.
- **Decisión:** Inyectar `flutter.settings` serializado directamente en `<head>` vía `loadDataWithBaseURL` y `shouldInterceptRequest`.
  **Por qué:** Flutter Web lee la configuración de `localStorage` al momento de arranque en `main.dart.js`. Configurar los valores antes de cargar `flutter.js` garantiza que el reloj, rosca y modales nunca lleguen a renderizarse en pantalla.
- **Decisión:** Retirar la ranita dibujada en Canvas básico del fallback local de Froggy.
  **Por qué:** La escala vectorial de 30px desentonaba con los paisajes de alta resolución y generaba un aspecto desolado. El fallback ahora conserva los efectos climáticos (lluvia, nieve, niebla, sol, luciérnagas) sobre el fondo WebP limpio.
- **Decisión:** Trasladar las acciones por lote de la biblioteca a la barra superior contextual.
  **Por qué:** La tarjeta flotante inferior se superponía con los últimos ítems de la lista y el miniplayer, interfiriendo con la ergonomía táctil.

---

## 3. Pendientes para la próxima fase

- [x] Ajustar el encuadre/offset horizontal de la escena web de Froggy en modo vertical (Portrait): resuelto mediante `#flutter-container { left: -12vw; width: 124vw; }` en `ZenBackground.kt`, logrando encuadre perfecto del personaje y los elementos de escena (picnic, casa hongo, cometa) sin recorte ni barras negras en pantallas 20:9.
- [x] Implementar y verificar cabecera dinámica por horario del día ("Buenos días" 6-11h, "Buenas tardes" 12-19h, "Buenas noches" 20-5h) con refresco automático cada 60s y al volver del background (`resolveTitle`, `HomeViewModel.refreshGreeting()`).
- [ ] Validar manualmente con el usuario la reproducción remota de catálogo desde el servidor en producción cuando lo requiera.
- [ ] Ejecutar auditoría final de Git (`git status`, `git diff --cached`) y solicitar autorización explícita para generar el commit y tag de versión `v0.4.7`.

---

## 4. Cómo verificar que este handoff sigue vigente

1. **Compilación y suite de pruebas unitarias:**
   ```bash
   ./gradlew testDebugUnitTest --console=plain
   ```
   *Criterio de éxito:* 191 tests pasando, 0 fallos, BUILD SUCCESSFUL.

2. **Compilación de la APK de depuración:**
   ```bash
   ./gradlew assembleDebug --console=plain
   ```
   *Criterio de éxito:* Generación exitosa de `app/build/outputs/apk/debug/app-debug.apk`.

3. **Verificación de conexión con el dispositivo físico:**
   ```bash
   & "C:\Users\kpera\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
   ```
   *Criterio de éxito:* Dispositivo listado en estado `device`.
