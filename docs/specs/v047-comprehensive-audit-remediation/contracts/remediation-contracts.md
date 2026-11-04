# Contratos de Interfaz, Seguridad y Subsistema Climático (`v0.4.7`)

## 1. Contrato de Sanitización y Red (`AuthInterceptor` & `AppLogger`)

### 1.1 Validación Estricta de Host (`AuthInterceptor.isTrustedMonkifyHost`)
Un `host` solo recibe `Authorization: Bearer <token>` y `X-Monkify-Token: <token>` si y solo si:
1. Coincide exactamente con el host extraído de `BuildConfig.BASE_URL`, o
2. Es `10.0.2.2`, `localhost`, `127.0.0.1`, `monkify-backend.monkify.workers.dev`, `monkify.tudominio.com`, o termina en `.monkify.workers.dev` / `.monkify.tudominio.com` (con punto separador obligatorio).
- **Garantía:** `api.github.com`, `api.open-meteo.com`, `lrclib.net` y cualquier `otro-proyecto.workers.dev` jamás reciben el token del usuario.

### 1.2 Redacción Automática en `AppLogger`
Antes de almacenar en el buffer en memoria, imprimir en Logcat o escribir en `files/logs/monkify.log`:
- Toda subcadena `([?&]token=)[^&\s#]+` se transforma en `$1[REDACTED]`.
- Todo valor tras `Bearer\s+[A-Za-z0-9._\-]+` se transforma en `Bearer [REDACTED]`.

---

## 2. Contrato Visual y Funcional de `ZenBackground` (Froggy + Clima + Rive)

```kotlin
@Composable
fun ZenBackground(
    isPlaying: Boolean,
    engine: String = "AURORA",
    timeOfDay: ZenTimeOfDay = resolveCurrentZenTimeOfDay(),
    weatherCondition: WeatherCondition = WeatherCondition.SUNNY,
    scenery: FroggyScenery = FroggyScenery.FIELDS,
    useOfficialFroggyAssets: Boolean = true,
    riveAnimationRes: Int? = null,
    modifier: Modifier = Modifier
)
```

### Comportamiento Obligatorio cuando `engine == "FROGGY"`:
1. **Capa 0 (Escenario WebP Oficial):** Resuelve mediante `FroggyAssetCatalog.getBackgroundRes(scenery, timeOfDay.toZenDayPart(), weatherCondition)` uno de los **60 fondos oficiales** (`fields_*`, `hill_*`, `mushroom_*`) con transición `Crossfade(800ms)`.
2. **Capa 1 (Efectos Climáticos + Animación Dinámica de Froggy):**
   - Renderiza partículas atmosféricas continuas acordes a `weatherCondition` (`RAINY` -> gotas diagonales y ondas en el agua; `SNOWY` -> copos con deriva sinusoidal; `HAZY` -> velos de neblina; `SUNNY` -> destellos solares; `CLOUDY` -> nubes suaves; `NIGHT` -> luciérnagas brillantes).
   - Renderiza la animación dinámica de Froggy reaccionando al clima (ej. paraguas en lluvia, bufanda en nieve, respiración armónica al ritmo de `isPlaying` y parpadeo natural) integrada armónicamente en la escena.
3. **Capa 2 (Runtime Rive `.riv`):** Mantiene activo el contenedor `RiveAnimationView` cuando `riveAnimationRes != null`.
4. **Sincronización en las 3 Pantallas:** `HomeScreen`, `FullScreenPlayer` y `LockScreenActivity` pasan `timeOfDay`, `weatherCondition` y `scenery` resueltos desde `currentWeather` y `settingsState.froggyScenery`.
5. **Indicador Climático Sutil:** En `HomeScreen` (bajo el saludo/frase zen) y en `SettingsScreen` (sección Modo Zen cuando Froggy está seleccionado), se muestra la píldora climática sutil con `currentWeather.temperatureSummary` (ej. `22° · Despejado`) y selector de escenario (`Automático`, `Campos 🌾`, `Colina 🌳`, `Hongos 🍄`).
