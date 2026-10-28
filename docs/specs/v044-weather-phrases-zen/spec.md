# Feature Specification: Reactividad Climática para Frases de Cabecera y Enriquecimiento del Compendio Zen

**Feature ID:** `v044-weather-phrases-zen`  
**Fecha:** 2026-10-28  
**Estado:** Fase 0 — Especificación y Arquitectura  

---

## 1. Contexto y Justificación
Monkify Android cuenta con una cabecera dinámica en `HomeScreen.kt` que saluda al usuario con un título y una frase motivadora. Hasta ahora, estas frases dependían únicamente de la hora del reloj del dispositivo en `HomeViewModel.kt`, sin considerar el clima ni el estado atmosférico que el fondo animado de Froggy ya refleja.

Con la incorporación del módulo de clima en tiempo real (`WeatherRepository`, Open-Meteo API y cálculo solar astronómico), las frases pueden sincronizarse de forma reactiva, orgánica y poética con el entorno meteorológico y la franja solar del usuario, profundizando la inmersión zen y musical de la aplicación.

---

## 2. Alcance (In-Scope)

1. **Compendio Enriquecido de Frases Zen y Musicales (`ZenPhraseCatalog`):**
   - Creación de un catálogo tipado de frases inspiradoras, relajantes y musicales clasificadas por:
     - Condición meteorológica: `SUNNY`, `CLOUDY`, `HAZY`, `RAINY`, `SNOWY`.
     - Franja solar: `DAWN` (Amanecer), `DAY` (Día), `SUNSET` (Atardecer), `NIGHT` (Noche).
   - Mínimo de 3 a 5 frases distintivas por combinación climática-solar (más de 60 frases cuidadosamente redactadas en español), evitando repetición monótona.
   - Selección determinista o pseudo-aleatoria rotativa con semilla horaria o por sesión para frescura diaria.

2. **Reactividad al Clima en `HomeViewModel`:**
   - Inyección de `WeatherRepository` en `HomeViewModel`.
   - Transformación reactiva de `weatherRepository.currentWeather` combinada con la hora/franja astronómica para emitir `greeting: StateFlow<GreetingInfo>`.
   - Actualización fluida sin parpadeos cuando el clima cambie o cuando se refresque la pantalla de inicio.

3. **Interacción de Usuario y Rotación de Frases:**
   - Permitir al usuario tocar la cabecera (título/frase) en `HomeScreen` para rotar manualmente a la siguiente frase del compendio para el estado actual.

4. **Degradación Elegante y Modo Offline (Zero-Latency):**
   - Catálogo 100% local, embebido en código (cero llamadas de red para obtener texto, cero uso de IA en caliente, respuesta instantánea).
   - Si no hay señal de GPS o red, utiliza el estado por defecto o la franja horaria del reloj sin romper la interfaz.

---

## 3. Lo que Queda Excluido (No-Scope)
- **Generación dinámica con APIs de LLM en tiempo real:** Todas las frases residen en la aplicación para asegurar latencia cero, cero costo, privacidad total y funcionamiento offline.
- **Modificaciones al reproductor o backend de Cloudflare:** El cambio se circunscribe estrictamente a la capa de presentación y dominio de cabecera de la app Android.
- **Alertas meteorológicas extremas o notificaciones push de clima:** No se emitirán alertas push de tormenta ni widgets de clima adicionales en esta fase.

---

## 4. Casos de Uso y Criterios de Aceptación

### UC1: Saludo inicial al abrir la app con clima soleado matutino
- **Dado** que el usuario abre Monkify a las 09:00 con cielo soleado (`SUNNY`, `DAY`),
- **Cuando** carga `HomeScreen`,
- **Entonces** el título muestra "Buenos días" y la frase proyecta una reflexión luminosa y rítmica (ej. *"Cielo despejado: dale ritmo y brillo a tu mañana"*).

### UC2: Transición a tarde lluviosa
- **Dado** que el usuario tiene la app abierta o regresa a la tarde mientras llueve (`RAINY`, `DAY` / `SUNSET`),
- **Cuando** el repositorio de clima detecta lluvia,
- **Entonces** la cabecera transiciona a un saludo de tarde con frase acogedora de lluvia (ej. *"Tarde de lluvia: el ritmo perfecto para acompañar las gotas"*).

### UC3: Noche con nieve o frío invernal
- **Dado** que es de noche (`NIGHT`) y el clima reporta nieve o frío (`SNOWY`),
- **Cuando** el usuario entra a la pantalla principal,
- **Entonces** el título muestra "Buenas noches" y la frase evoca calidez y calma (ej. *"Frío afuera, calidez y calma en tus auriculares"*).

### UC4: Toque manual en la cabecera
- **Dado** que el usuario ve una frase en la cabecera,
- **Cuando** pulsa sobre el texto de la cabecera,
- **Entonces** la frase cambia a otra alternativa correspondiente al mismo clima y franja del día.


---

## 5. Declaración y Registro de Fuentes de Recursos (Textuales y Gráficos)

| Recurso | Tipo | Fuente / Origen Declarado | Licencia / Atribución |
| :--- | :--- | :--- | :--- |
| **Fondos Froggy (60 WebPs)** | Gráfico | Repositorio [`R0rt1z2/Froggy.git`](https://github.com/R0rt1z2/Froggy.git) (extracción de los assets de Google Weather en Android). | Recursos de la app de sistema de Google / Uso personal e ilustrativo en Monkify. |
| **API de Clima en Tiempo Real** | Datos | Open-Meteo Forecast API (`https://open-meteo.com/`). | Open Database License (ODbL 1.0) / Acceso público sin API key. |
| **Compendio de Frases Zen** | Texto | Curación y adaptación de literatura zen y mindfulness (*D.T. Suzuki*, *Thich Nhat Hanh*, filosofía de música ambiental de *Brian Eno* y aforismos estoicos de *Marco Aurelio* orientados al ritmo cotidiano). | Dominio público / Redacción propia en español adaptada al clima y la música. |
| **Fondos Cosmos / Futuros NASA** | Gráfico | NASA Open APIs — Astronomy Picture of the Day (`https://api.nasa.gov/`) y NASA Image Library. | Dominio Público (NASA Media Guidelines). |

---

## 6. Arquitectura para Actualizaciones Dinámicas y Fondos Remotos (Over-The-Air)

1. **Compendio de Frases Desacoplado:**
   - La lista base reside en `ZenPhraseCatalog` con un mapa estructurado para arranque instantáneo sin red.
   - Es compatible con carga dinámica desde JSON para permitir incorporar nuevas frases remotamente mediante Cloudflare Worker o GitHub RAW sin necesidad de compilar un nuevo APK.

2. **Soporte de Fondos Híbridos (`ZenBackgroundSource`):**
   - Abstracción sellada en `ZenBackground`:
     - `LocalResource(@DrawableRes val id: Int)` para WebP/drawables empaquetados.
     - `RemoteUrl(val url: String, @DrawableRes val fallbackId: Int)` para imágenes remotas (ej. NASA APOD o wallpapers en CDN de Cloudflare R2).
   - Coil realiza el almacenamiento en caché de disco local de forma transparente. Si no hay conexión o falla la descarga, la app recurre al fallback local de inmediato sin parpadeo.
