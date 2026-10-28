# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.4 — Reactividad Climática para Frases de Cabecera y Enriquecimiento del Compendio Zen
**Fecha de cierre:** 2026-10-28
**Estado del build/tests:** ✅ pasando — Suite completa de pruebas unitarias de Android pasando al 100% y build de Release de Android pasando al 100% en `e:\PG\vcp-s\Monkify-Android-Refine` (`.\gradlew.bat testDebugUnitTest assembleRelease` -> `BUILD SUCCESSFUL`, APK generado: `app/build/outputs/apk/release/app-release.apk` de 47.4 MB con compendio zen reactivo y catálogo oficial Froggy WebP HD).

---

## 1. Estado actual

- **Compendio de Frases Zen y Musicales Enriquecido (`ZenPhraseCatalog`):**
  - Creado catálogo estructurado en `app/.../domain/model/ZenPhraseCatalog.kt` con más de 70 frases poéticas y musicales en español.
  - Matriz de 20 combinaciones posibles: 5 condiciones meteorológicas (`SUNNY`, `CLOUDY`, `HAZY`, `RAINY`, `SNOWY`) $\times$ 4 franjas solares astronómicas (`DAWN`, `DAY`, `SUNSET`, `NIGHT`).
  - Mínimo de 3 a 5 variantes por celda con resolución cíclica modular determinista y segura para cualquier valor de offset (positivo o negativo).
  - Títulos solares dinámicos: "Buen amanecer", "Buenos días", "Buenas tardes", "Buenas noches".
  - Cubierto por suite de pruebas unitarias en `ZenPhraseCatalogTest.kt` (100% aprobado).
- **Reactividad al Clima en Tiempo Real en `HomeViewModel`:**
  - Inyectado `WeatherRepository` en `HomeViewModel.kt`.
  - Propiedad reactiva `greeting: StateFlow<GreetingInfo>` combinando `currentWeather` y `_phraseIndexOffset` con `SharingStarted.Eagerly`.
  - Cero latencia, funcionamiento 100% offline y fallback automático por hora local si no hay datos de red o GPS.
  - Método `cycleNextPhrase()` para rotar manualmente entre frases disponibles.
  - Cubierto por pruebas unitarias reactivas en `HomeViewModelGreetingTest.kt` y compatibilidad retroactiva en `HomeViewModelTest.kt` (100% aprobado).
- **Interacción Táctil en `HomeScreen`:**
  - En `HomeScreen.kt`, pulsar sobre la cabecera (título y frase) invoca `homeViewModel.cycleNextPhrase()`, permitiendo al usuario cambiar de frase al instante para el clima actual.
- **Trazabilidad y Declaración Formal de Fuentes:**
  - Registrado en `docs/specs/v044-weather-phrases-zen/spec.md`:
    - Frases zen: adaptación de literatura de presencia plena (*D.T. Suzuki*, *Thich Nhat Hanh*, *Brian Eno*, *Marco Aurelio*).
    - Fondos Froggy: `R0rt1z2/Froggy.git` (Google Weather).
    - API Clima: Open-Meteo (ODbL 1.0).
    - Fondos Cosmos / Futuros NASA: compatibilidad declarada para APOD de `api.nasa.gov`.
- **Arquitectura de Actualización Desacoplada (Over-The-Air):**
  - Diseñada la abstracción de fuentes híbridas `ZenBackgroundSource` para admitir en el futuro descargas de imágenes astronómicas de la NASA o wallpapers en CDN de Cloudflare sin necesidad de recompilar la app.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Almacenar el compendio base de frases como objeto de dominio puro (`ZenPhraseCatalog`) con soporte para extensión externa.
  **Por qué:** Asegura rendimiento inmediato (latencia 0ms al abrir la app), funcionamiento sin internet ni permisos especiales, y elimina costos o dependencias de APIs de IA en caliente.
- **Decisión:** Usar `SharingStarted.Eagerly` para `greeting: StateFlow<GreetingInfo>` en `HomeViewModel`.
  **Por qué:** Garantiza que el flujo reactivo comience a recolectar de inmediato tanto en la UI como en las pruebas unitarias sin requerir suscripciones manuales ni jobs asíncronos.
- **Decisión:** Mantener compatibilidad del método `getDynamicGreeting(hour)` en `HomeViewModel`.
  **Por qué:** Evita romper tests de regresión previos y provee un punto de entrada alternativo si se consulta de forma estática por hora.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `docs/specs/v044-weather-phrases-zen/*` | Documentación formal SDD: `spec.md`, `plan.md`, `tasks.md` con trazabilidad de fuentes y arquitectura OTA. |
| `docs/history/fase-v0.4.3.md` | Histórico archivado de la fase anterior de integración Froggy + Rive. |
| `app/.../domain/model/WeatherCondition.kt` | Incorporación del helper `ZenDayPart.fromHour(hour)` en el companion object. |
| `app/.../domain/model/ZenPhraseCatalog.kt` | Catálogo de más de 70 frases poéticas zen y resolvedor cíclico modular. |
| `app/.../ui/screens/viewmodel/HomeViewModel.kt` | Inyección de `WeatherRepository`, flujo reactivo `greeting` con `combine` y método `cycleNextPhrase()`. |
| `app/.../ui/screens/HomeScreen.kt` | Vinculación del evento click de cabecera a `viewModel.cycleNextPhrase()`. |
| `app/.../domain/model/ZenPhraseCatalogTest.kt` | Suite de pruebas unitarias que valida las 20 matrices de clima $\times$ hora solar y rotación de frases. |
| `app/.../ui/screens/viewmodel/HomeViewModelGreetingTest.kt` | Suite de pruebas unitarias reactivas para la cabecera en `HomeViewModel`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Incorporar selector de Escenario Zen (`Campos`, `Colina`, `Hongos`, `Cosmos`) en la pantalla de Ajustes (`SettingsScreen.kt`).
- [ ] Explorar la conexión con el endpoint APOD de la NASA (`api.nasa.gov`) en el backend de Cloudflare para alimentar el fondo `COSMOS` con la imagen astronómica del día.
- [ ] Integrar animaciones vectoriales `.riv` en `res/raw/` si se diseñan ranas animadas interactivas.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Crecimiento del compendio de frases si se amplían a otros idiomas.
  **Mitigación:** Si se agregan múltiples idiomas (inglés, portugués), migrar las cadenas a `res/values/strings.xml` o a un archivo `zen_phrases_<locale>.json` en assets o R2.

## 6. Cómo verificar que este handoff sigue vigente

```powershell
# En el cliente Android:
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
