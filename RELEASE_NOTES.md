# Monkify Android v0.4.0 — Release Notes

**Versión:** 0.4.0  
**Fecha:** 20 de Octubre de 2026  
**Repositorios:**
- Android App: [`Monkify-Android`](https://github.com/KEVIN11000/Monkify-Android)
- Backend: [`Monkify_backend`](https://github.com/KEVIN11000/Monkify_backend)

---

## 🌟 Resumen de la Versión

Monkify v0.4.0 representa un salto de calidad enfocado en la estabilidad del reproductor, la ergonomía de navegación, la personalización visual zen y el refinamiento de la biblioteca y pantallas de detalle. Esta versión resuelve 19 puntos críticos solicitados por los usuarios, eliminando comportamientos erráticos en pantalla de bloqueo y estados de ciclo de vida, incorporando nuevos fondos visuales interactivos y optimizando la experiencia de reproducción continua.

---

## 🚀 Novedades y Mejoras Clave

### 1. Reproductor & Pantalla de Bloqueo
- **Eliminación Total de Picture-in-Picture (PiP):** Retirado el soporte PiP del manifiesto y del código. La app ahora delega su control en segundo plano a la bandeja de notificaciones nativa de Android y a `MediaSession`.
- **Blindaje contra Toques Fantasma:** `FullScreenPlayer` consume todos los eventos táctiles para impedir que toques accidentales sobre áreas vacías traspasen a listas inferiores y cambien la canción de forma inesperada.
- **Persistencia de Sesión (`PlaybackSessionStateHolder`):** La canción actual, metadatos y cola de reproducción ya no se pierden al salir de la app, bloquear el teléfono o cambiar de tarea.
- **Sincronización Inmediata en Pantalla de Bloqueo:** `LockScreenActivity` se superpone desde el **primer bloqueo** sin requerir ciclos adicionales de bloqueo/desbloqueo.
- **Soporte de Letras Dual (.lrc y Texto Plano):** Soporte para letras sincronizadas y texto estático con fallback automático al endpoint `/lyrics` del backend y proveedor LRCLIB.

### 2. Pantalla de Inicio (`HomeScreen`)
- **Saludo Dinámico con Motivación:** Banner con 7 etapas horarias (Madrugada, Amanecer, Mañana, Mediodía, Tarde, Atardecer, Noche) acompañado de frases breves y motivadoras.
- **Filtro de Álbumes:** Nuevo chip interactivo "Álbumes" junto a "Música" para explorar el catálogo en cuadrícula.
- **Últimos 6 Subidos:** La cuadrícula rápida superior muestra dinámicamente los últimos 6 temas incorporados al servidor vía `?sort=latest`.
- **"Escuchado Recientemente" & "Hecho para ti":** Conectados reactivamente al historial local de Room (`playCount`, `lastPlayedAt`) para sugerir pistas de los artistas más reproducidos.
- **Inicio Limpio y Enfocado:** Eliminada la lista vertical infinita de "Todas las canciones" de Inicio, concentrando el descubrimiento y las recomendaciones.

### 3. Biblioteca y Pantallas de Detalle
- **Búsqueda y Ordenamiento Interno:** Tanto `PlaylistDetailScreen` como `AlbumDetailScreen` cuentan con barra de búsqueda rápida y selector de ordenamiento (`Título A-Z`, `Título Z-A`, `Artista`, `Duración`).
- **Carátulas Automáticas en Collage:** Las listas sin portada personalizada generan dinámicamente un mosaico de 4 (2x2) o 6 canciones (3x2).
- **Ordenamiento Global de Biblioteca:** Selector interactivo de ordenamiento disponible en todas las pestañas de `LibraryScreen` (`Todo`, `Favoritos`, `Descargas`, `Playlists`, `Álbumes`).
- **Acciones Rápidas en Pestaña "Todo":** Botones dedicados para **"Aleatorio" (Shuffle)** y **"Desde la primera"** para reproducir el catálogo completo con un solo toque.

### 4. Navegación & Soporte
- **Navegación Rápida con Reselect:**
  - Tocar la pestaña activa de **"Biblioteca"** desplaza suavemente la lista hasta el inicio (`Scroll to top`).
  - Tocar la pestaña activa de **"Buscar"** limpia la búsqueda y restaura la vista de categorías y exploración inicial.
- **Placeholder Amigable:** El buscador ahora utiliza la frase `"¿Qué te gustaría escuchar?"`.
- **Nuevos Fondos Visuales Zen:** Incorporados los motores `Froggy` (paisaje dinámico de Google Weather), `Cosmos` (nebulosa en movimiento) y `Ninguno` (negro puro para pantallas AMOLED).
- **Reporte de Fallos en 1 Clic:** Envío directo de problemas y comentarios desde Ajustes hacia la API de soporte del backend sin necesidad de iniciar sesión en GitHub.

---

## 🛠️ Matriz de Archivos Técnicos Modificados

| Componente | Archivo Principal | Propósito |
| :--- | :--- | :--- |
| **Backend** | `src/routes/tracks.ts` | Filtro `?sort=latest` y proxy LRCLIB para letras |
| **Backend** | `src/routes/playlists.ts` | Extracción de `cover_urls` para mosaicos |
| **Backend** | `src/routes/support.ts` | Endpoint de soporte `/api/v1/support/report` |
| **Persistencia** | `TrackEntity.kt` / `TrackDao.kt` | Contadores `playCount` y `lastPlayedAt` |
| **Reproductor** | `PlaybackSessionStateHolder.kt` | Singleton centralizado de estado y cola |
| **Reproductor** | `FullScreenPlayer.kt` | Touch shield y remoción de controles PiP |
| **Pantalla Bloqueo** | `LockScreenActivity.kt` | Flags de encendido y superposición instantánea |
| **Letras** | `LrcParser.kt` | Parser con fallback a texto plano (`timestampMs = -1L`) |
| **Fondos Zen** | `ZenBackground.kt` | Motores de canvas `FROGGY`, `COSMOS` y `NONE` |
| **Ajustes** | `SettingsScreen.kt` | Envío de reporte en 1 clic y selector de fondos |
| **Inicio** | `HomeScreen.kt` / `HomeViewModel.kt` | Saludos 7 franjas, chip Álbumes, Top 6 últimos |
| **Biblioteca** | `LibraryScreen.kt` / `LibraryViewModel.kt` | Ordenamiento global, acciones Todo, reselect scroll |
| **Detalles** | `PlaylistDetailScreen.kt` / `AlbumDetailScreen.kt` | Buscador interno, menú de orden y mosaicos collage |
| **Búsqueda** | `SearchScreen.kt` | Placeholder localizado y reselect reset |
