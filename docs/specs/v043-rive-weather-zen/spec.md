# Especificación de Requerimiento (Spec): Módulo Rive (.riv) y Clima en Tiempo Real

**Feature:** `v043-rive-weather-zen`  
**Directorio:** `docs/specs/v043-rive-weather-zen/`  
**Fecha:** 2026-10-27  
**Estado:** Fase 0 — Especificación y Arquitectura

---

## 1. Resumen Ejecutivo y Alcance
Esta especificación define la integración de un sistema de fondos ambientales reactivos al clima en tiempo real para el modo Zen de Monkify Android, acompañado por la incorporación del motor de animaciones vectoriales interactivas Rive (`.riv`) y la incorporación del catálogo escénico inspirado en Google Weather Froggy (`https://github.com/R0rt1z2/Froggy.git`).

---

## 2. Casos de Uso del Usuario

### Caso de Uso 1: Visualización del Modo Zen Sincronizado con el Clima
* **Actor:** Usuario escuchando música en Monkify que activa el modo Zen o abre el reproductor a pantalla completa.
* **Precondición:** El usuario tiene habilitado el fondo "Froggy Clima" en los ajustes Zen.
* **Flujo Principal:**
  1. La aplicación consulta el estado del clima en caché.
  2. Si la caché ha expirado (> 2 horas), solicita de forma asíncrona y transparente el clima actual a Open-Meteo mediante las coordenadas aproximadas.
  3. El visualizador Zen presenta la escena de paisaje (`fields`, `hill` o `mushroom`) correspondiente a la hora astronómica local (amanecer, día, atardecer o noche) y condición meteorológica (despejado, nubes, lluvia, nieve o niebla).
  4. Los efectos atmosféricos procedimentales (gotas de lluvia con ondas en el estanque, nieve flotante, luciérnagas o rayos de sol) se reproducen de manera suave, continua e infinita al ritmo de la música.
* **Flujo Alternativo (Sin conexión a internet):**
  - Si no hay red, la aplicación utiliza el último pronóstico guardado o deduce la franja horaria mediante la hora local del sistema con clima despejado/templado por defecto. La reproducción nunca se interrumpe ni muestra avisos molestos.

### Caso de Uso 2: Privacidad y Manejo de Ubicación
* **Actor:** Usuario que abre la app por primera vez o rechaza permisos de ubicación.
* **Flujo:**
  - La aplicación solicita únicamente el permiso de ubicación aproximada (`ACCESS_COARSE_LOCATION`).
  - Si el usuario deniega el permiso, la app continúa funcionando sin insistir, utilizando una ciudad predeterminada configurable en los ajustes Zen o geolocalización IP pasiva.

### Caso de Uso 3: Selector de Temas Zen (Froggy vs Cosmos HD vs Aurora)
* **Actor:** Usuario que prefiere fondos espaciales o abstractos.
* **Flujo:**
  - En la barra o menú de ajustes Zen, el usuario puede alternar entre:
    - *Froggy Clima*: Reactivo a la meteorología local.
    - *Cosmos HD*: Galaxia espiral y nebulosas profundas.
    - *Aurora Boreal*: Luces del norte generativas.
  - La selección se persiste instantáneamente en DataStore/SharedPreferences.

---

## 3. Entradas y Salidas Exactas

### Entradas
- Coordenadas geográficas (`lat`, `lon` con resolución aproximada).
- Respuestas HTTP JSON de `https://api.open-meteo.com/v1/forecast`.
- Estado del reproductor de audio: `isPlaying: Boolean`, `trackDuration: Long`, `currentPosition: Long`.
- Selección de tema y escena en preferencias locales.

### Salidas
- `CurrentWeather` estructurado y validado.
- Capa escénica renderizada en Compose con fondos WebP de alta definición y partículas vectoriales reactivas.
- Contenedor de animación Rive / Canvas interactivo que respira al ritmo del audio.
- Indicador sutil de clima opcional (temperatura actual, ej. `22° · Despejado`) con auto-ocultamiento para mantener el diseño Zen minimalista.

---

## 4. Criterios de Aceptación (Acceptance Criteria)
1. **Consumo Eficiente de Batería y Red:** Las consultas meteorológicas deben estar limitadas estrictamente por un TTL de 2 horas. No debe existir sondeo continuo (polling).
2. **Zero Glitches / Bucles Continuos:** Las animaciones visuales (gotas, nubes, luciérnagas, estrellas) deben utilizar funciones periódicas continuas (\(\sin\), \(\cos\)) sin cortes o saltos visibles en el reinicio del ciclo.
3. **Velocidad Zen Relajada:** El ritmo de respiración y movimiento debe mantenerse calmado (ciclos de 30 a 50 segundos), evitando animaciones hiperactivas.
4. **Degradación Elegante:** La denegación de permisos de ubicación o la falta de conectividad debe resultar en un fallback visual estético sin crasheos ni bloqueos.
5. **No Afectación al APK:** El tamaño del APK no debe incrementarse en más de 3 MB en total tras la inclusión del runtime Rive y los assets escénicos optimizados en WebP.

---

## 5. Fuera de Alcance (No-Scope)
- **Monkify Connect:** Permanece explícitamente excluido según la instrucción del usuario.
- **Predicción horaria extendida / Gráficos de pronóstico de 7 días:** Monkify es un reproductor de música zen, no una aplicación meteorológica compleja de pronóstico a largo plazo.
- **Widgets de escritorio para la pantalla de inicio:** Fuera de alcance para este ciclo.
- **Permisos intrusivos (`ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`):** Estrictamente prohibidos.
