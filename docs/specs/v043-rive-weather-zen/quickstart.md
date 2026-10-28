# Guía de Inicio Rápido y Validación: Módulo Clima & Rive

**Feature:** `v043-rive-weather-zen`  
**Objetivo:** Validar de extremo a extremo la obtención del clima desde Open-Meteo, el mapeo de condiciones WMO a estados Zen, y el renderizado adaptativo en Compose.

---

## 1. Prerrequisitos
- Dispositivo Android físico o emulador con Android 8.0+ (API 26+).
- Conexión a internet para la primera sincronización meteorológica.
- Entorno de compilación: JDK 17+, Android SDK 35.

---

## 2. Escenarios de Prueba y Verificación

### Escenario 1: Consulta y Mapeo de Open-Meteo (Prueba Unitaria)
* **Objetivo:** Comprobar que el DTO deserializa correctamente la respuesta de Open-Meteo y mapea códigos WMO (`0` -> `SUNNY`, `61` -> `RAINY`, etc.).
* **Comando:**
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests "com.k11000.monkify.data.weather.*"
  ```
* **Resultado Esperado:** 
  `BUILD SUCCESSFUL` con 100% de aserciones válidas para condiciones, temperaturas mín/máx y horarios solares.

### Escenario 2: Resiliencia de Red y Expiración de TTL (Caché Offline)
* **Objetivo:** Verificar que cuando no hay conexión a internet o el servidor no responde:
  1. Si la caché tiene menos de 2 horas, se utiliza directamente sin error.
  2. Si la caché es más antigua pero existe, se retorna como stale sin bloquear la UI.
  3. Si la caché está vacía, se aplica el fallback diurno/nocturno por hora de reloj local sin crashear.
* **Comando:**
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests "com.k11000.monkify.data.repository.WeatherRepositoryTest"
  ```
* **Resultado Esperado:** `BUILD SUCCESSFUL`.

### Escenario 3: Renderizado Visual en Jetpack Compose (`@Preview`)
* **Objetivo:** Validar visualmente en las Previews de Compose o sidecar de inspección que el fondo responde a las 5 condiciones climáticas (`SUNNY`, `CLOUDY`, `HAZY`, `RAINY`, `SNOWY`) en sus 4 franjas horarias (`MORNING`, `DAY`, `SUNSET`, `NIGHT`).
* **Comprobación:**
  - Inspeccionar `@Preview` en `ZenWeatherPreview.kt`.
  - Comprobar que en lluvia aparecen gotas animadas sobre el estanque.
  - Comprobar que en modo noche aparecen luciérnagas/estrellas con tonalidad oscura suave.
  - Comprobar que la velocidad de animación se mantiene calmada (ciclos armónicos de 30–50s).

### Escenario 4: Integración del Runtime Rive
* **Objetivo:** Verificar que la dependencia `app.rive:rive-android:11.2.1` compila sin conflictos de NDK ni duplicación de bibliotecas `.so` nativas.
* **Comando:**
  ```powershell
  .\gradlew.bat assembleDebug
  ```
* **Resultado Esperado:** `BUILD SUCCESSFUL`.
