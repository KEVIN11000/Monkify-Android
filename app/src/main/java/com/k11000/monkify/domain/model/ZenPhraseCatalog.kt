package com.k11000.monkify.domain.model

/**
 * Catálogo estructurado de frases zen, poéticas y musicales clasificadas por
 * condición climática y franja solar astronómica.
 *
 * Fuentes y principios de curación declarados en docs/specs/v044-weather-phrases-zen/spec.md:
 * - Aforismos de presencia plena y literatura zen (D.T. Suzuki, Thich Nhat Hanh).
 * - Filosofía de música ambiental y escucha consciente (Brian Eno).
 * - Reflexiones estoicas de armonía con el entorno (Marco Aurelio).
 */
object ZenPhraseCatalog {

    fun resolveTitle(dayPart: ZenDayPart): String {
        return when (dayPart) {
            ZenDayPart.DAWN -> "Buen amanecer"
            ZenDayPart.DAY -> "Buenos días"
            ZenDayPart.SUNSET -> "Buenas tardes"
            ZenDayPart.NIGHT -> "Buenas noches"
        }
    }

    private val phrasesCatalog: Map<Pair<WeatherCondition, ZenDayPart>, List<String>> = mapOf(
        // ==================== SOLEADO (SUNNY) ====================
        (WeatherCondition.SUNNY to ZenDayPart.DAWN) to listOf(
            "Rayos dorados y primeros acordes para despertar tu calma",
            "La luz entra suave: momento de respirar y sintonizar",
            "Un nuevo inicio con la claridad de tus melodías",
            "El sol asoma lento: sintoniza tu paz matutina"
        ),
        (WeatherCondition.SUNNY to ZenDayPart.DAY) to listOf(
            "Cielo despejado: ritmo luminoso para inspirar tu jornada",
            "Melodías brillantes bajo el calor sereno del sol",
            "La energía de hoy fluye con buena música y mente clara",
            "Luz en el camino: dale brillo a cada momento"
        ),
        (WeatherCondition.SUNNY to ZenDayPart.SUNSET) to listOf(
            "Atardecer dorado: baja las revoluciones y agradece el día",
            "El sol se oculta en un compás cálido y sereno",
            "Tarde naranja para saborear cada nota musical",
            "Colores en el horizonte y acordes que apaciguan la mente"
        ),
        (WeatherCondition.SUNNY to ZenDayPart.NIGHT) to listOf(
            "Noche estrellada y silencio para reencontrar tu propio sonido",
            "Claridad en el cielo, paz absoluta en tu reproductor",
            "Deja que la música cierre un día radiante con gratitud",
            "Bajo las estrellas, la armonía fluye sin interrupciones"
        ),

        // ==================== NUBLADO (CLOUDY) ====================
        (WeatherCondition.CLOUDY to ZenDayPart.DAWN) to listOf(
            "Amanecer tenue: calma y suavidad para iniciar el día",
            "Nubes tranquilas abren paso a tus pensamientos sin prisa",
            "La luz difusa acompaña un despertar pausado y consciente",
            "Respirar hondo: el día comienza con suavidad gris"
        ),
        (WeatherCondition.CLOUDY to ZenDayPart.DAY) to listOf(
            "Cielo gris, alma cálida: el refugio de una buena melodía",
            "Tarde templada para desconectar y subir el volumen",
            "Nubes en calma: armonía perfecta para concentrarse y crear",
            "La suavidad del cielo nublado invita a la introspección"
        ),
        (WeatherCondition.CLOUDY to ZenDayPart.SUNSET) to listOf(
            "El día cae entre nubes suaves y melodías reconfortantes",
            "Matices violetas para cerrar la tarde en completa serenidad",
            "La caída del sol invita a un compás pausado y presente",
            "Tarde apacible: deja que el sonido disuelva cualquier cansancio"
        ),
        (WeatherCondition.CLOUDY to ZenDayPart.NIGHT) to listOf(
            "Manto de nubes protegiendo tu descanso sonoro",
            "Silencio exterior, melodías íntimas en tus auriculares",
            "La noche se viste de calma para apaciguar la mente",
            "Descanso profundo arropado por acordes suaves"
        ),

        // ==================== NIEBLA (HAZY) ====================
        (WeatherCondition.HAZY to ZenDayPart.DAWN) to listOf(
            "Mañana entre bruma: misterio y suavidad para despertar",
            "El mundo amanece en silencio: sintoniza tu centro",
            "Respirar hondo en la niebla con tu música preferida",
            "Bruma fresca: un lienzo en blanco para tu día"
        ),
        (WeatherCondition.HAZY to ZenDayPart.DAY) to listOf(
            "Atmósfera en calma: melodías envolventes para perderse",
            "La bruma suaviza el paisaje: afina tus sentidos y escucha",
            "Caminar despacio y dejar que el sonido guíe cada paso",
            "Mundo difuso, mente enfocada en el presente sonoro"
        ),
        (WeatherCondition.HAZY to ZenDayPart.SUNSET) to listOf(
            "Atardecer difuso: tonos suaves que apagan el ajetreo",
            "La niebla abraza el final del día en perfecta serenidad",
            "Tiempo suspendido en un acorde infinito de calma",
            "El atardecer se desvanece en una atmósfera pacífica"
        ),
        (WeatherCondition.HAZY to ZenDayPart.NIGHT) to listOf(
            "Noche silenciosa y profunda entre la bruma",
            "Un velo de calma para escuchar en la intimidad",
            "Paz absoluta cuando el mundo se desvanece afuera",
            "Música tenue en una noche de quietud absoluta"
        ),

        // ==================== LLUVIA (RAINY) ====================
        (WeatherCondition.RAINY to ZenDayPart.DAWN) to listOf(
            "Llovizna matinal: café caliente y acordes que reconfortan",
            "Gotas en la ventana al compás de tu despertar",
            "Mañana fresca de lluvia para comenzar con serenidad",
            "El sonido del agua limpia el aire y prepara la mente"
        ),
        (WeatherCondition.RAINY to ZenDayPart.DAY) to listOf(
            "Día de lluvia: el ritmo perfecto para acompañar las gotas",
            "La melodía del agua baila con tus canciones favoritas",
            "Deja que la lluvia lave el ruido y quédate con el compás",
            "Gotas cayendo suavemente: pausa, escucha y respira"
        ),
        (WeatherCondition.RAINY to ZenDayPart.SUNSET) to listOf(
            "Tarde lluviosa: cobijo, tranquilidad y música en calma",
            "El cielo gotea tranquilidad mientras cae la tarde",
            "Refugio sonoro frente al murmullo continuo de la lluvia",
            "Cadencia de gotas en el crepúsculo: armonía natural"
        ),
        (WeatherCondition.RAINY to ZenDayPart.NIGHT) to listOf(
            "Lluvia nocturna: el mejor sonido ambiente para descansar",
            "Gotas constantes que arrullan tus pensamientos y fatigas",
            "Cierra los ojos y fluye con el agua y la música",
            "Noche húmeda y acogedora: descanso profundo en tus oídos"
        ),

        // ==================== NIEVE / FRÍO (SNOWY) ====================
        (WeatherCondition.SNOWY to ZenDayPart.DAWN) to listOf(
            "Amanecer blanco: frescura pura y melodías cristalinas",
            "El frío despierta la mente, la música abriga el espíritu",
            "Silencio de nieve para empezar el día en completa paz",
            "Copos lentos al despertar: calma que purifica"
        ),
        (WeatherCondition.SNOWY to ZenDayPart.DAY) to listOf(
            "Frío afuera, calidez inagotable en tus auriculares",
            "La nieve cae en compás lento: tómate tu tiempo y disfruta",
            "Notas suaves que derriten cualquier prisa o tensión",
            "Un paisaje invernal que se llena de luz con tus canciones"
        ),
        (WeatherCondition.SNOWY to ZenDayPart.SUNSET) to listOf(
            "La tarde blanca se tiñe de calma y buenas canciones",
            "Viento frío que invita a cobijarse en una buena pista",
            "Serenidad invernal en cada acorde que despide el día",
            "Crepúsculo helado, refugio cálido en tu música"
        ),
        (WeatherCondition.SNOWY to ZenDayPart.NIGHT) to listOf(
            "Noche gélida, corazón tibio con tus temas preferidos",
            "Silencio absoluto de invierno para soñar despierto",
            "La música es el mejor refugio en la noche fría",
            "Copos en reposo: paz invernal para descansar profundamente"
        )
    )

    private val defaultPhrases = listOf(
        "Que la música impulse tu día",
        "Pausa consciente para disfrutar de tus melodías",
        "Relájate y déjate llevar por el sonido"
    )

    /**
     * Retorna todas las frases disponibles para la combinación indicada.
     */
    fun getPhrases(condition: WeatherCondition, dayPart: ZenDayPart): List<String> {
        return phrasesCatalog[condition to dayPart] ?: defaultPhrases
    }

    /**
     * Resuelve de forma determinista y segura una frase rotativa mediante offset.
     */
    fun resolvePhrase(condition: WeatherCondition, dayPart: ZenDayPart, offset: Int = 0): String {
        val phrases = getPhrases(condition, dayPart)
        if (phrases.isEmpty()) return defaultPhrases.first()
        val positiveIndex = Math.floorMod(offset, phrases.size)
        return phrases[positiveIndex]
    }
}
