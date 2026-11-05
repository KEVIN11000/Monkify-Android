# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.7 — Remediación Integral de Auditoría, Refinamiento Ergonómico, Froggy Portrait y Release v0.4.7
**Fecha de cierre:** 2026-11-05
**Estado del build/tests:** ✅ pasando — Suite de pruebas unitarias (`./gradlew testDebugUnitTest`) con 37 suites y 192 tests ejecutados sin fallos (0 failures, BUILD SUCCESSFUL), compilación verificada (`assembleRelease`) con firma oficial (`SHA-256: 62ede63ffb1e550f868e76000252f64e33940845385b6d307e342667aa744b5f`), e instalación probada en dispositivo físico (`10.51.80.168:38239`).

---

## 1. Estado actual

- Preservación íntegra de base de datos Room: 6 índices añadidos en `TrackEntity.kt`, migración Room v1 -> v2 configurada y `@Transaction upsertRemoteTracks` protegiendo favoritos, rutas locales y descargas de sobrescrituras al sincronizar con el backend.
- Blindaje de seguridad en release: almacén criptográfico permanente `monkify-release.jks` fuera de Git con exclusión en `.gitignore`, passwords desacopladas de Gradle, `network_security_config.xml` configurado y validación estricta de host en `AuthInterceptor.kt` y `PlayerViewModel.kt`.
- Operación de caché y audio verificado: `PlaybackCache.kt` conectado a `SimpleCache` con límite dinámico en MB y evicción nativa, más enrutamiento de salida física de audio mediante `AudioDeviceInfo`.
- Encuadre portrait de Froggy Web: `#flutter-container { left: -12vw; width: 124vw; }` en `ZenBackground.kt` centra el personaje y elementos de escena en pantallas 20:9 sin recorte ni bordes negros.
- Cabecera y saludo dinámico: `ZenPhraseCatalog.resolveTitle` conmuta entre "Buenos días" (6-11h), "Buenas tardes" (12-19h) y "Buenas noches" (20-5h), con recálculo automático cada 60s en `HomeViewModel.kt` y al volver a primer plano.
- Release v0.4.7 publicado en GitHub Actions: `versionCode = 8`, `versionName = "0.4.7"`, tag `v0.4.7` pusheado y firmado con certificado permanente compatible con instalaciones previas.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Desplazar `#flutter-container` con CSS `@media (orientation: portrait) { left: -12vw; width: 124vw; }` en la WebView.
  **Por qué:** Flutter Web renderiza en un canvas 16:9 con la ranita situada en el 60% derecho; el offset de -12vw recentra la ranita en pantallas 20:9 conservando el fondo continuo sin bandas negras.
  **Alternativas descartadas:** Usar solo el fallback local estático (perdía las animaciones Flare originales) o escalar a más de 140vw (recortaba los elementos izquierdos como la casa hongo).
- **Decisión:** Inyectar `hourProvider: () -> Int` en `HomeViewModel.kt` y parámetro opcional `hour: Int? = null` en `ZenPhraseCatalog.resolveTitle`.
  **Por qué:** Permite pruebas unitarias deterministas sin depender del reloj del sistema y sin romper compatibilidad.
- **Decisión:** Inyectar variables de entorno de firma de release en `.github/workflows/release.yml`.
  **Por qué:** Garantiza que los builds de release generados en GitHub Actions utilicen la clave criptográfica oficial permanente y coincidan con las versiones instaladas previamente en los dispositivos de los usuarios.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts` | Incremento a `versionCode = 8`, `versionName = "0.4.7"` y configuración robusta de `signingConfigs.release`. |
| `.github/workflows/release.yml` | Inyección de secretos de firma en el paso `assembleRelease` para firmas idénticas en CI. |
| `app/src/main/java/com/k11000/monkify/ui/components/ZenBackground.kt` | Ajuste de viewport CSS en modo vertical (`left: -12vw; width: 124vw`). |
| `app/src/main/java/com/k11000/monkify/domain/model/ZenPhraseCatalog.kt` | Mapeo horario estricto para "Buenos días", "Buenas tardes" y "Buenas noches". |
| `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/HomeViewModel.kt` | Ticker de actualización de saludo a 1 minuto y método `refreshGreeting()`. |
| `app/src/main/java/com/k11000/monkify/ui/screens/HomeScreen.kt` | Refresco de saludo al volver a foco y tarjeta de cabecera visualmente refinada. |
| `app/src/main/java/com/k11000/monkify/data/local/dao/TrackDao.kt` | `@Transaction upsertRemoteTracks` preservando datos locales frente a sincronizaciones de servidor. |
| `app/src/main/java/com/k11000/monkify/playback/PlaybackCache.kt` | Integración física con `SimpleCache` de Media3 y evicción segura por tamaño configurado. |
| `app/src/main/java/com/k11000/monkify/ui/screens/LibraryScreen.kt` | Acciones por lote movidas a la barra contextual superior `TopAppBar`. |
| `.github/workflows/release.yml` | Blindaje de firma en CI: restauración estricta de `RELEASE_KEYSTORE_BASE64`, erradicación total de debug keystore efímero y aborto con error si falta el secreto. |
| `app/build.gradle.kts` | Resolución multicamino de ruta de keystore (`rootProject.file` y `file`) para release signing. |
| `docs/history/fase-v0.4.7.md` | Registro histórico archivado de la fase v0.4.7. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Validar en dispositivo real la instalación por actualización (`Monkify-v0.4.7.apk` sobre versión oficial previa) confirmando compatibilidad de firma y retención de base de datos.
- [ ] Validar manualmente con el usuario la reproducción en streaming y descarga de canciones remotas desde el backend de producción.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Conectividad intermitente en WebView al cargar `https://weather.r0rt1z2.com/` si el dispositivo carece de conexión en frío.
  **Impacto:** Puede demorar la visualización del fondo animado hasta que active el fallback local de WebP.
  **Mitigación sugerida:** El fallback offline local ya está integrado y probado para entrar en acción si la carga web no responde.

## 6. Cómo verificar que este handoff sigue vigente

Comandos para revalidar el estado de compilación, suite de pruebas y estado de git:

```bash
./gradlew testDebugUnitTest --console=plain
./gradlew assembleRelease --console=plain
git status
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*