# Referencia y Roadmap: Módulo Clima & Rive para Monkify Zen Engine

## 1. Repositorio de Referencia Oficial
* **URL:** `https://github.com/R0rt1z2/Froggy.git`
* **Descripción:** Implementación Android para extraer y mapear las animaciones, estados del clima y fondos auténticos de Google Weather Froggy en tiempo real.

---

## 2. Puntos Clave para la Fase Post-Release
1. **Tecnología de Animación:**
   - Soporte nativo para `.riv` mediante `app.rive:rive-android:11.2.1`.
   - Máquina de estados interactiva (State Machine) con entradas:
     - `isPlaying: Boolean`
     - `weatherCondition: Int` (Códigos WMO de Open-Meteo)
     - `timeOfDay: Int` (0 = Mañana, 1 = Día, 2 = Tarde, 3 = Noche)
2. **Proveedor de Clima:**
   - Open-Meteo (`https://api.open-meteo.com/v1/forecast`), gratuito y sin API key.
   - Caché de 2 a 3 horas para evitar consumo de red/batería.
3. **Mapeo de Escenas de Froggy:**
   - Soleado, Nubes, Lluvia (con gotas y ondas en el estanque), Nieve, Tormenta, Niebla y Noche estrellada.
