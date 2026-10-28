# HANDOFF — Monkify

**Fase cerrada:** Fase v0.4.3 — Integración de Clima en Tiempo Real (Open-Meteo) y Motor Rive con Google Weather Froggy HD
**Fecha de cierre:** 2026-10-27
**Estado del build/tests:** ✅ pasando — Suite de pruebas unitarias de Android pasando al 100% y build de Release de Android pasando al 100% en `e:\PG\vcp-s\Monkify-Android-Refine` (`.\gradlew.bat testDebugUnitTest assembleRelease` -> `BUILD SUCCESSFUL`, APK generado: `app/build/outputs/apk/release/app-release.apk` de 47.4 MB con catálogo completo de 60 WebPs y runtime de Rive).

---

## 1. Estado actual

- **Integración de Clima en Tiempo Real con Open-Meteo:**
  - Implementado `OpenMeteoRemoteDataSource` invocando `https://api.open-meteo.com/v1/forecast` de forma gratuita y sin API key.
  - Mapeo completo de códigos de la OMM (WMO) a 5 categorías principales: `SUNNY`, `CLOUDY`, `HAZY`, `RAINY`, `SNOWY` en `WeatherCondition.kt`.
  - Cálculo solar astronómico preciso: resolución dinámica de franjas horarias (`DAWN`, `DAY`, `SUNSET`, `NIGHT`) mediante `sunriseEpochMs` y `sunsetEpochMs` con fallback al reloj del sistema si no hay datos satelitales. Cubierto por tests unitarios en `WeatherConditionTest.kt` y `OpenMeteoDtoTest.kt`.
- **Caché Local de Clima Resiliente y Bajo Consumo (TTL 2 Horas):**
  - Implementado `LocalWeatherStore` sobre DataStore Preferences. Almacena temperaturas, condición, horario solar y timestamp.
  - Si la caché tiene menos de 2 horas de antigüedad, no se realizan llamadas HTTP, protegiendo batería y datos móviles.
  - En caso de desconexión o fallo de red, se mantiene el último pronóstico guardado o se genera un clima sintético seguro sin interrumpir la reproducción. Cubierto por tests en `WeatherRepositoryTest.kt`.
- **Ubicación Aproximada con Fallback Seguro:**
  - `CoarseLocationDataSource` consulta la última ubicación conocida mediante Google Play Services (`ACCESS_COARSE_LOCATION`).
  - Si el usuario rechaza permisos de ubicación, la app utiliza las coordenadas predeterminadas de respaldo sin insistir ni bloquear la interfaz.
- **Catálogo Escénico Oficial de Google Weather Froggy (60 Ilustraciones WebP):**
  - 60 ilustraciones oficiales en alta resolución extraídas de Google Weather (`R0rt1z2/Froggy.git`) e integradas como recursos en `app/src/main/res/drawable/`.
  - Mapeador estático de alta velocidad `FroggyAssetCatalog.kt` que resuelve la combinación exacta según Escenario (`FIELDS`, `HILL`, `MUSHROOM`), Franja Horaria (`DAWN`, `DAY`, `SUNSET`, `NIGHT`) y Condición Climática (`SUNNY`, `CLOUDY`, `HAZY`, `RAINY`, `SNOWY`).
  - Transición fluida con `Crossfade` entre fondos y validación del 100% de combinaciones en `FroggyAssetCatalogTest.kt`.
- **Contenedor Rive (`.riv`) y Partículas Atmosféricas Vivas:**
  - Runtime de Rive Android (`app.rive:rive-android:11.2.1`) configurado y compatible en `ZenBackground.kt` mediante `AndroidView(RiveAnimationView)`.
  - Efectos atmosféricos reactivos en Compose Canvas superpuestos al fondo escénico:
    - **Lluvia:** 42 gotas en ángulo con ondas de agua expandiéndose en la zona del estanque.
    - **Nieve:** 36 copos oscilantes con deriva sinusoidal armónica (\(\sin\)).
    - **Niebla:** Velo translúcido con gradiente alfa flotando en el horizonte medio.
    - **Soleado:** Rayos de sol tenues pulsando al ritmo del audio.
    - **Noche:** 14 luciérnagas titilando y derivando suavemente en la pradera.
- **Simulador Interactivo Visual (Generative UI):**
  - Artefacto `froggy_weather_visualizer.html` actualizado para cargar los 60 fondos WebP reales y simular en vivo todas las transiciones y estados climáticos.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Integrar los 60 fondos escénicos WebP directamente como recursos de Android (`app/src/main/res/drawable/`) en vez de cargarlos dinámicamente por red o assets.
  **Por qué:** Garantiza carga instantánea sin latencia, caché gestionada por el SO, compatibilidad directa con Compose `@Preview` y cero consumo de datos móviles al cambiar de escena.
- **Decisión:** Utilizar `ACCESS_COARSE_LOCATION` en lugar de `ACCESS_FINE_LOCATION`.
  **Por qué:** El clima no requiere precisión GPS a nivel de metros; la ubicación por celda o ciudad es suficiente y respeta la privacidad del usuario sin agotar la batería.
- **Decisión:** Mantener el runtime nativo de Rive (`app.rive:rive-android:11.2.1`) junto con un motor de efectos atmosféricos Canvas nativos en Compose.
  **Por qué:** Permite cargar archivos vectoriales `.riv` interactivos si se proveen en el futuro, manteniendo al mismo tiempo una experiencia visual viva y reactiva sin depender exclusivamente de assets binarios externos.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `gradle/libs.versions.toml` | Incorporación de dependencias `rive = 11.2.1` y `playServicesLocation = 21.3.0`. |
| `app/build.gradle.kts` | Implementación de `libs.rive.android` y `libs.play.services.location`. |
| `app/src/main/AndroidManifest.xml` | Configuración del permiso `ACCESS_COARSE_LOCATION`. |
| `app/.../domain/model/WeatherCondition.kt` | Entidades `WeatherCondition`, `ZenDayPart`, `FroggyScenery`, `CurrentWeather` y funciones astronómicas de cálculo solar. |
| `app/.../domain/repository/WeatherRepository.kt` | Contrato de interfaz reactiva para el repositorio meteorológico. |
| `app/.../data/remote/dto/OpenMeteoDto.kt` | DTOs de respuesta JSON para Open-Meteo y mappers a modelos de dominio. |
| `app/.../data/remote/api/OpenMeteoRemoteDataSource.kt` | Fuente remota que consulta la API de Open-Meteo de forma tolerante. |
| `app/.../data/local/LocalWeatherStore.kt` | Persistencia y caché local en DataStore Preferences con validación de TTL de 2 horas. |
| `app/.../data/location/CoarseLocationDataSource.kt` | Detección de coordenadas por Google Play Services con fallback a valores por defecto. |
| `app/.../data/repository/WeatherRepositoryImpl.kt` | Implementación completa del repositorio con manejo de red, caché y degradación offline. |
| `app/.../di/NetworkModule.kt` & `RepositoryModule.kt` | Inyección de dependencias para Gson, WeatherRepository y LocationDataSource. |
| `app/.../ui/components/FroggyAssetCatalog.kt` | Catálogo tipado para resolución de los 60 fondos WebP. |
| `app/.../ui/components/ZenBackground.kt` | Capas de fondo escénico, efectos atmosféricos (lluvia, nieve, niebla, rayos solares, luciérnagas) y contenedor Rive. |
| `app/.../playback/PlayerViewModel.kt` | Inyección de `WeatherRepository`, exposición de `currentWeather: StateFlow` y refresco automático. |
| `app/.../ui/screens/HomeScreen.kt` & `FullScreenPlayer.kt` | Vinculación del fondo Zen con la condición climática y franja solar en tiempo real. |
| `app/src/main/res/drawable-nodpi/*_bg.webp` | 60 fondos escénicos oficiales de Google Weather importados y optimizados sin sobreescalado de memoria (1:1 pixel ratio). |
| `app/src/test/.../*Test.kt` | Suites de pruebas unitarias para `WeatherConditionTest`, `OpenMeteoDtoTest`, `WeatherRepositoryTest` y `FroggyAssetCatalogTest`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Evaluar la creación o conversión de animaciones interactivas de Froggy a formato `.riv` nativo en el Rive Editor para incrustar la rana animada vectorial en `res/raw/`.
- [ ] Incorporar selector de Escenario Zen (`Campos`, `Colina`, `Hongos`) en el menú de Ajustes de Monkify (`SettingsScreen.kt`).
- [ ] Desarrollar widgets de pantalla de inicio o control ambiental si el usuario lo solicita.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Tamaño del APK aumentado a 47.4 MB debido a los 60 fondos WebP y las librerías nativas C++ de Rive (`librive-android.so`).
  **Impacto:** Mayor tiempo de descarga para usuarios con redes lentas.
  **Mitigación sugerida:** Generar Android App Bundles (`.aab`) para producción en Google Play o aplicar ABI splits por arquitectura de CPU (`arm64-v8a`).

## 6. Cómo verificar que este handoff sigue vigente

```powershell
# En el cliente Android:
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
