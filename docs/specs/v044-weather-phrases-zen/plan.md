# Implementation Plan: Reactividad Climática para Frases de Cabecera y Enriquecimiento del Compendio Zen

**Feature ID:** `v044-weather-phrases-zen`  
**Fecha:** 2026-10-28  
**Estado:** Fase 0 — Arquitectura y Planificación  

---

## 1. Contexto Técnico y Arquitectura Modular

### 1.1 Diagrama de Flujo de Datos

```mermaid
flowchart TD
    subgraph Dominio y Repositorio
        WR[WeatherRepository] -->|currentWeather: StateFlow<CurrentWeather>| HVM[HomeViewModel]
        ZPC[ZenPhraseCatalog] -->|resolveGreeting(weather, offset)| HVM
    end

    subgraph Presentación
        HVM -->|greeting: StateFlow<GreetingInfo>| HS[HomeScreen Header]
        HS -->|onClick Header| HVM[cycleNextPhrase()]
    end
```

---

## 2. Contratos de Datos y Definiciones de Tipos

### 2.1 Modelo `ZenPhraseCatalog`
```kotlin
package com.k11000.monkify.domain.model

object ZenPhraseCatalog {
    fun resolveTitle(dayPart: ZenDayPart): String {
        return when (dayPart) {
            ZenDayPart.DAWN -> "Buen amanecer"
            ZenDayPart.DAY -> "Buenos días"
            ZenDayPart.SUNSET -> "Buenas tardes"
            ZenDayPart.NIGHT -> "Buenas noches"
        }
    }

    fun getPhrases(condition: WeatherCondition, dayPart: ZenDayPart): List<String>
    fun resolvePhrase(condition: WeatherCondition, dayPart: ZenDayPart, offset: Int = 0): String
}
```

### 2.2 Estructura del Compendio de Frases
El compendio abarcará las 20 combinaciones posibles (5 condiciones climáticas × 4 franjas horarias), con 3 a 5 variantes por celda, sumando más de 70 frases zen, poéticas y musicales:

1. **Soleado (`SUNNY`):**
   - *Amanecer:* Rayos dorados y primeros acordes para despertar tu calma; La luz entra suave: momento de respirar y sintonizar; Un nuevo inicio con la claridad de tus melodías.
   - *Día:* Cielo despejado: ritmo luminoso para inspirar tu jornada; Melodías brillantes bajo el calor del sol; La energía de hoy fluye con buena música.
   - *Atardecer:* Atardecer dorado: baja las revoluciones y agradece el día; El sol se oculta en un compás cálido y sereno; Tarde naranja para disfrutar cada nota.
   - *Noche:* Noche estrellada y silencio para encontrar tu propio sonido; Claridad en el cielo, paz en tu reproductor; Deja que la música cierre un día brillante.

2. **Nublado (`CLOUDY`):**
   - *Amanecer:* Amanecer tenue: calma y suavidad para iniciar el día; Nubes tranquilas abren paso a tus pensamientos; La luz difusa acompaña un despertar sin prisa.
   - *Día:* Cielo gris, alma cálida: el refugio de una buena melodía; Tarde templada para desconectar y subir el volumen; Nubes en calma: armonía perfecta para concentrarse.
   - *Atardecer:* El día cae entre nubes suaves y melodías reconfortantes; Matices grises y violetas para cerrar la tarde en paz; La caída del sol invita a un compás pausado.
   - *Noche:* Manto de nubes protegiendo tu descanso sonoro; Silencio exterior, melodías íntimas en tus auriculares; La noche se viste de calma para descansar la mente.

3. **Lluvia (`RAINY`):**
   - *Amanecer:* Llovizna matinal: café caliente y acordes que reconfortan; Gotas en la ventana al ritmo de tu despertar; Mañana fresca de lluvia para comenzar con serenidad.
   - *Día:* Día de lluvia: el ritmo perfecto para acompañar las gotas; La melodía del agua baila con tu música favorita; Deja que la lluvia lave el ruido y quédate con el ritmo.
   - *Atardecer:* Tarde lluviosa: un libro, una taza y música en calma; El cielo gotea tranquilidad mientras cae la tarde; Refugio sonoro frente al murmullo de la lluvia.
   - *Noche:* Lluvia nocturna: el mejor sonido ambiente para descansar; Gotas constantes que arrullan tus pensamientos; Cierra los ojos y fluye con el agua y el sonido.

4. **Niebla (`HAZY`):**
   - *Amanecer:* Mañana entre bruma: misterio y suavidad para despertar; El mundo despierta en silencio, sintoniza tu paz; Respirar hondo en la niebla con tu música favorita.
   - *Día:* Atmósfera en calma: melodías envolventes para perderse; La bruma suaviza el paisaje, afina tus sentidos; Caminar despacio y dejar que el sonido guíe el paso.
   - *Atardecer:* Atardecer difuso: tonos suaves que apagan el ajetreo; La niebla abraza el final del día en perfecta serenidad; Tiempo suspendido en un acorde infinito.
   - *Noche:* Noche silenciosa y profunda entre la bruma; Un velo de calma para escuchar en la intimidad; Paz absoluta cuando el mundo se desvanece afuera.

5. **Nieve / Frío (`SNOWY`):**
   - *Amanecer:* Amanecer blanco: frescura pura y melodías cristalinas; El frío despierta la mente, la música abriga el espíritu; Silencio de nieve para empezar en blanco y en paz.
   - *Día:* Frío afuera, calidez inagotable en tus auriculares; La nieve cae en compás lento: tómate tu tiempo; Notas suaves que derriten cualquier prisa.
   - *Atardecer:* La tarde blanca se tiñe de calma y buenas canciones; Viento frío que invita a cobijarse en una buena pista; Serenidad invernal en cada acorde.
   - *Noche:* Noche gélida, corazón tibio con tus temas preferidos; Silencio absoluto de invierno para soñar despierto; La música es el mejor refugio de la noche.

---

## 3. Integración en `HomeViewModel`

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val localTrackRepository: LocalTrackRepository,
    private val downloadRepository: DownloadRepository? = null,
    private val syncScheduler: SyncScheduler? = null,
    private val weatherRepository: WeatherRepository? = null
) : ViewModel() {

    private val _phraseIndexOffset = MutableStateFlow(0)

    val greeting: StateFlow<GreetingInfo> = combine(
        weatherRepository?.currentWeather ?: flowOf(WeatherRepositoryImpl.DEFAULT_WEATHER),
        _phraseIndexOffset
    ) { weather, offset ->
        val title = ZenPhraseCatalog.resolveTitle(weather.dayPart)
        val phrase = ZenPhraseCatalog.resolvePhrase(weather.condition, weather.dayPart, offset)
        GreetingInfo(title = title, phrase = phrase)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GreetingInfo(
            title = ZenPhraseCatalog.resolveTitle(ZenDayPart.DAY),
            phrase = ZenPhraseCatalog.resolvePhrase(WeatherCondition.SUNNY, ZenDayPart.DAY, 0)
        )
    )

    fun cycleNextPhrase() {
        _phraseIndexOffset.value += 1
    }
}
```

---

## 4. Estrategia de Pruebas Unitarias (TDD)
1. **`ZenPhraseCatalogTest.kt`:**
   - Verificar que para las 20 combinaciones de `(WeatherCondition, ZenDayPart)` la lista de frases tenga al menos 3 elementos y ninguno sea vacío o en blanco.
   - Verificar que `resolvePhrase(..., offset)` rote cíclicamente de forma determinista mediante módulo `offset % phrases.size` sin lanzar excepciones por índices fuera de rango.
   - Verificar que `resolveTitle(dayPart)` devuelva títulos coherentes para las 4 franjas solares.
2. **`HomeViewModelGreetingTest.kt`:**
   - Verificar que `HomeViewModel` emita el saludo correspondiente al clima emitido por el `WeatherRepository`.
   - Verificar que al invocar `cycleNextPhrase()`, el estado de `greeting` se actualice con la siguiente frase de la misma categoría.
