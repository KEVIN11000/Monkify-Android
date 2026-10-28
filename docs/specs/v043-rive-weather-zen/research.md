# Investigación Técnica: Módulo Rive (.riv) y Clima en Tiempo Real para Monkify

**Feature:** `v043-rive-weather-zen`  
**Fecha:** 2026-10-27  
**Estado:** Completado (Fase 0 de Planificación)

---

## 1. Contexto y Objetivos
Monkify cuenta con un modo de visualización ambiental Zen que actualmente implementa fondos procedurales dinámicos (Zen Froggy y Cosmos HD) sobre Jetpack Compose Canvas. El objetivo de esta fase es conectar la experiencia visual al clima en tiempo real del usuario y habilitar el motor de animaciones vectoriales interactivas Rive (`.riv`), integrando las escenas de clima de Google Weather Froggy extraídas del ecosistema oficial y documentadas en `https://github.com/R0rt1z2/Froggy.git`.

---

## 2. Hallazgos de Investigación y Decisiones de Arquitectura

### 2.1 Proveedor de Clima: Open-Meteo API
- **Decisión:** Utilizar el endpoint público gratuito de [Open-Meteo](https://open-meteo.com/en/docs):
  `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=weather_code,temperature_2m,is_day&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto&forecast_days=1`
- **Justificación:**
  - No requiere API key, tarjeta de crédito ni registro previo.
  - Límites de uso amplios (hasta 10,000 llamadas diarias sin costo para uso no comercial).
  - Códigos meteorológicos estandarizados por la OMM (Organización Meteorológica Mundial / WMO).
  - Incluye horarios astronómicos precisos de salida (`sunrise`) y puesta (`sunset`) del sol según latitud y longitud.
- **Alternativas descartadas:**
  - *OpenWeatherMap:* Requiere API key, tarjetas para planes modernos y tiene límites estrictos de peticiones por minuto.
  - *WeatherAPI:* Requiere gestión de tokens y renovación de credenciales.

### 2.2 Estrategia de Ubicación y Privacidad
- **Decisión:** Arquitectura híbrida de 3 niveles:
  1. **Nivel 1 (Preferido):** Ubicación aproximada (`ACCESS_COARSE_LOCATION`) mediante `FusedLocationProviderClient` de Google Play Services.
  2. **Nivel 2 (Fallback sin permisos):** Si el usuario deniega permisos o no están disponibles los servicios de Google, geolocalización IP pasiva o selector de ciudad manual utilizando el endpoint gratuito de Open-Meteo Geocoding (`https://geocoding-api.open-meteo.com/v1/search?name={query}&count=5`).
  3. **Nivel 3 (Modo offline / Default seguro):** Si no hay red ni ubicación previa, utilizar la última coordenada almacenada en caché o la hora local del dispositivo para determinar la franja horaria (`dayPart`) con clima templado/despejado por defecto.
- **Justificación:**
  - Cumple estrictamente con las políticas de privacidad de Android y Google Play: no se exige `ACCESS_FINE_LOCATION` innecesariamente (la precisión de celda/ciudad es suficiente para el clima).
  - La app nunca se bloquea ni muestra pantallas de error si el usuario rechaza los permisos.

### 2.3 Estrategia de Caché y Ahorro de Batería
- **Decisión:** Caché con TTL de 2 horas en almacenamiento local (Room / DataStore).
  - Al abrir el reproductor Zen, se verifica si `DateTime.now() - lastFetchTime < 2 hours`.
  - Si los datos son válidos, no se realiza ninguna petición HTTP.
  - Si la petición falla (por falta de conectividad), se utiliza el pronóstico almacenado sin interrumpir la reproducción.
  - El cálculo de la franja horaria (`morning`, `day`, `sunset`, `night`) es puramente local comparando `DateTime.now()` con los campos `sunrise` y `sunset` retornados en la última sincronización.

### 2.4 Motor Gráfico de Animación: Rive (`.riv`) vs Flare (`.flr`)
- **Hallazgo clave sobre los assets de Froggy:**
  - Las animaciones originales de Google Weather Froggy en `R0rt1z2/Froggy` se crearon en **Flare 1.0 (`.flr`)** con fondos en **WebP (`.webp`)**.
  - Flare evolucionó hacia **Rive 2.0+ (`.riv`)**. Las bibliotecas modernas nativas de Android (`app.rive:rive-android:11.2.1`) solo leen y ejecutan archivos binarios `.riv` con máquinas de estados (State Machines), mientras que Flare utilizaba un runtime descontinuado (`flare_flutter`).
- **Decisión de Arquitectura Gráfica:**
  1. **Pipeline de Renderizado por Capas (Layered Zen Visualizer):**
     - **Capa 0 (Fondo Escénico):** Fondos vectoriales WebP de alta fidelidad organizados por tema (`fields`, `hill`, `mushroom`), hora (`morning`, `day`, `sunset`, `night`) y clima (`sunny`, `cloudy`, `hazy`, `rainy`, `snowy`). Peso ultra ligero (~30-50 KB por escena).
     - **Capa 1 (Efectos Atmosféricos y Partículas):** Motor Canvas optimizado de Compose que añade lluvia con ondas dinámicas en el agua, nieve suave, luciérnagas nocturnas, neblina volumétrica y rayos de sol.
     - **Capa 2 (Personaje y Animación Vectorial):** Contenedor compatible con Rive `.riv` mediante el composable `RiveView` de `app.rive:rive-android` para animaciones interactivas basadas en estado (reproduciendo, pausado, pulsos de BPM) y fallback procedural nativo de Compose Canvas cuando no haya un archivo `.riv` activo.
  2. **Intercambiabilidad de Temas:**
     - El usuario podrá elegir en los ajustes del Modo Zen:
       - *Froggy Clima Dinámico* (se adapta al clima real del usuario).
       - *Cosmos HD Dinámico* (se adapta a la noche/día y estado del reproductor).
       - *Aurora Boreal Procedural* (Canvas nativo).

---

## 3. Matriz de Mapeo Meteorológico (WMO Codes → Zen Condition)

| Códigos WMO | Condición Zen | Efecto Atmosférico | Escena Froggy Predeterminada |
|---|---|---|---|
| 0, 1 | `SUNNY` / Despejado | Destellos solares, cielo brillante | Soleado en el campo (`fields_*_sunny`) |
| 2, 3 | `CLOUDY` / Nublado | Nubes suaves en capas desplazándose | Nublado (`fields_*_cloudy`) |
| 45, 48 | `HAZY` / Niebla | Velo difuso con gradiente alfa | Neblina / Hazy (`fields_*_hazy`) |
| 51–67, 80–82, 95–99 | `RAINY` / Lluvia o Tormenta | Gotas cayendo con ondas circulares | Lluvia con paraguas (`fields_*_rainy`) |
| 71–77, 85–86 | `SNOWY` / Nieve | Copos de nieve con deriva oscilante | Nieve con bufanda (`fields_*_snowy`) |

---

## 4. Requisitos de Dependencias en `app/build.gradle.kts`
```kotlin
// Rive Android Runtime (Compose friendly)
implementation("app.rive:rive-android:11.2.1")
// Google Play Services Location (para ubicación aproximada de bajo consumo)
implementation("com.google.android.gms:play-services-location:21.3.0")
```
*Impacto en APK:* ~1.8 MB adicionales por las bibliotecas nativas de Rive C++ (`librive_text.so`, etc.). Totalmente admisible dentro de los 17.7 MB actuales de la app.
