# Plan de Tareas Atómicas (Tasks): Módulo Rive y Clima en Tiempo Real

**Feature:** `v043-rive-weather-zen`  
**Directorio:** `docs/specs/v043-rive-weather-zen/`  
**Fecha:** 2026-10-27  
**Estado:** Fase 2 — Construcción Modular Guiada por Tareas

---

## Checklist de Tareas

- [x] **T001: Configuración de Dependencias de Rive y Ubicación**
  - **Descripción:** Incorporar `app.rive:rive-android:11.2.1` y `com.google.android.gms:play-services-location:21.3.0` en `app/build.gradle.kts`. Configurar permisos de ubicación aproximada `ACCESS_COARSE_LOCATION` en `AndroidManifest.xml`.
  - **Criterio de Aceptación:** El proyecto compila sin errores de resolución ni colisiones de dependencias nativas.
  - **Prueba/Verificación:** `.\gradlew.bat compileDebugKotlin` exitoso.

- [x] **T002: Modelos de Dominio y Mapeo Meteorológico OMM (WMO)**
  - **Descripción:** Crear `WeatherCondition`, `ZenDayPart`, `CurrentWeather` y las funciones de cálculo astronómico de franjas horarias basadas en `sunrise`/`sunset` y mapeo de códigos OMM.
  - **Criterio de Aceptación:** Mapeo exhaustivo para códigos de sol, nubes, niebla, lluvia y nieve; cálculo exacto de franja diurna/nocturna con fallback por reloj local.
  - **Prueba/Verificación:** `.\gradlew.bat testDebugUnitTest --tests "com.k11000.monkify.domain.model.WeatherConditionTest"` pasando al 100%.

- [x] **T003: Fuente Remota Open-Meteo y Caché Local con TTL (2 Horas)**
  - **Descripción:** Implementar `OpenMeteoRemoteDataSource` (invocando `https://api.open-meteo.com/v1/forecast`) y `LocalWeatherStore` con persistencia de timestamp, condición y temperaturas.
  - **Criterio de Aceptación:** Las consultas no se repiten si la caché tiene menos de 2 horas de antigüedad; soporte de deserialización tolerante.
  - **Prueba/Verificación:** Tests unitarios de serialización/deserialización y verificación de vigencia de TTL (`OpenMeteoDtoTest`).

- [x] **T004: Repositorio Meteorológico Resiliente y Manejo de Ubicación**
  - **Descripción:** Implementar `WeatherRepositoryImpl` coordinando `CoarseLocationDataSource`, caché local y fallback de ciudad predeterminada si el usuario rechaza permisos de ubicación.
  - **Criterio de Aceptación:** Si falla la red o no hay permisos, la app entrega el último dato válido o el fallback sintético sin crashear.
  - **Prueba/Verificación:** `.\gradlew.bat testDebugUnitTest --tests "com.k11000.monkify.data.repository.WeatherRepositoryTest"` pasando al 100%.

- [x] **T005: Integración del Catálogo Escénico WebP (Campos, Colinas, Hongos)**
  - **Descripción:** Integrar los fondos optimizados en WebP (`fields_*`, `hill_*`, `mushroom_*`) clasificados por franja horaria (`morning`, `day`, `sunset`, `night`) y condición meteorológica (`sunny`, `cloudy`, `hazy`, `rainy`, `snowy`) en `app/src/main/res/drawable/`.
  - **Criterio de Aceptación:** Transición suave con `Crossfade` entre fondos y resolución automática de escena según el clima actual.
  - **Prueba/Verificación:** Tests unitarios de resolución de recursos (`FroggyAssetCatalogTest`).

- [x] **T006: Contenedor Rive / Canvas Reactivo con Efectos Atmosféricos y Partículas**
  - **Descripción:** Enlazar `ZenBackground.kt` con el estado meteorológico. Si llueve, proyectar lluvia con ondas en el agua; si nieva, copos oscilantes; si es de noche, luciérnagas y estrellas. Añadir soporte para el renderizado del runtime Rive `.riv`.
  - **Criterio de Aceptación:** Animación continua, infinita y sin saltos ni costuras perceptibles; velocidad zen calmada (30-50s); respeto del selector de tema en ajustes.
  - **Prueba/Verificación:** Previews de Compose (`@Preview`) y simulación visual interactiva (`froggy_weather_visualizer.html`).

- [x] **T007: Verificación Global, Linters y Compilación de Release**
  - **Descripción:** Ejecutar suite completa de pruebas unitarias (`testDebugUnitTest`), auditoría de higiene de git (`git status`) y verificación de compilación de release APK (`assembleRelease`).
  - **Criterio de Aceptación:** 100% de tests pasando, APK compilado sin errores y sin fugas de secretos o archivos temporales.
  - **Prueba/Verificación:** `.\gradlew.bat testDebugUnitTest assembleRelease` exitoso.
