# Feature Specification: Refinamiento Integral de Experiencia, Reproductor y Biblioteca (Monkify Android v0.4.0)

**Feature Branch**: `refine/v0.4`  
**Created**: 2026-10-19  
**Status**: Draft (Phase 0 - Pending User Approval)  
**Input**: Cola de 19 requerimientos de usuario consolidada en `cola_de_requerimientos_v04.md`

---

## 1. Alcance y Resumen Ejecutivo

Esta especificación define el alcance funcional, casos de uso, entradas/salidas exactas y contratos de interfaz para la versión **v0.4.0** de **Monkify Android** y su backend en **Cloudflare Workers**. El objetivo es resolver 19 requerimientos puntuales de experiencia de usuario, estabilidad del reproductor (`Media3`), sincronización de pantalla de bloqueo, letras de canciones, ordenamiento/búsqueda en biblioteca y vistas de detalle, fondos visuales (`Froggy`, `Cosmos`, `Negro Puro`) y reporte de fallos en 1 clic.

### Lo que queda EXCLUIDO (No-Scope)
- **No-Scope 1**: Modificación del esquema de autenticación o cuentas multi-usuario (la app continúa operando bajo el modelo actual de cliente directo + API/Worker).
- **No-Scope 2**: Reproductor de video o soporte Picture-in-Picture (explícitamente eliminado por requerimiento #8).
- **No-Scope 3**: Creación o edición manual de archivos `.lrc` desde la app móvil (solo consumo y visualización de letras sincronizadas y estáticas).
- **No-Scope 4**: Alteración de la aplicación de escritorio (`win app`) o del recolector (`spotify-harvester`), salvo los endpoints aditivos en `cloudflare-backend`.

---

## 2. User Scenarios & Testing *(mandatory)*

### User Story 1 - Estabilidad del Reproductor, Cola, Pantalla de Bloqueo y Cero PiP (Prioridad: P1)

El usuario reproduce música desde una playlist, álbum o biblioteca, navega por la interfaz con el reproductor expandido, minimiza la app con el botón Inicio o bloquea la pantalla del dispositivo. En todo momento el reproductor conserva la canción actual, la cola de reproducción activa y los controles responden únicamente a la capa visible sin toques fantasma ni pérdida de estado, mostrando el reproductor de pantalla de bloqueo desde el primer bloqueo y eliminando por completo el modo flotante PiP.

**Why this priority**: Corrige bugs críticos de reproducción y pérdida de estado (#8, #9, #10, #17) que afectan directamente la funcionalidad principal de escuchar música.

**Independent Test**: Iniciar reproducción dentro de una playlist, expandir el reproductor y tocar zonas vacías de la pantalla; verificar que no cambia a canciones fuera de la playlist. Presionar Inicio y bloquear el teléfono por primera vez; verificar que la pantalla de bloqueo de Monkify se superpone inmediatamente con los metadatos y la cola intactos, y que al volver a la app no aparece `"Sin reproducir"` ni se vacía la cola.

**Acceptance Scenarios**:

1. **Given** que el usuario está en `FullScreenPlayer` o presiona el botón Inicio del sistema mientras reproduce audio, **When** la app pasa a segundo plano, **Then** NO entra en modo Picture-in-Picture (PiP) ni existe botón de PiP en la interfaz (#8).
2. **Given** que una canción se está reproduciendo con una cola activa, **When** el usuario presiona Inicio, bloquea el dispositivo y regresa a la aplicación (o recrea la actividad), **Then** `PlayerViewModel` conserva/reconstruye el título, artista, carátula, progreso, duración y lista de reproducción actual sin mostrar estado vacío (#9).
3. **Given** que el usuario reproduce una playlist y despliega `FullScreenPlayer`, **When** toca cualquier área vacía o fondo del reproductor expandido, **Then** el evento táctil es consumido por `FullScreenPlayer` y nunca atraviesa hacia la lista inferior (`HomeScreen`/`LibraryScreen`), y el botón "Siguiente" avanza estrictamente dentro de la cola de la playlist (#10).
4. **Given** que hay música reproduciéndose, **When** el usuario bloquea la pantalla por primera vez (`ACTION_SCREEN_OFF` / `Keyguard`), **Then** `LockScreenActivity` se superpone en el primer bloqueo con todos los datos sincronizados del tema actual y su cola (#17).

---

### User Story 2 - Rediseño de Inicio: Saludo Dinámico, Álbumes, Últimos 6 Subidos, Recientes y Hecho Para Ti (Prioridad: P1)

El usuario abre la pantalla de Inicio (`HomeScreen`) y observa un saludo contextualizado según las 7 franjas del día acompañado de una breve frase motivadora, un filtro de `"Álbumes"` junto a `"Música"`, una cuadrícula superior con las últimas 6 canciones subidas al servidor, una fila reactiva de `"Escuchado recientemente"`, una sección `"Hecho para ti"` basada en sus artistas más escuchados, y una vista limpia sin la lista infinita de `"Todas las canciones"`.

**Why this priority**: Transforma la pantalla principal (#3, #4, #5, #6, #7, #19) para que sea verdaderamente dinámica, personalizada y rápida de explorar.

**Independent Test**: Abrir `HomeScreen` en distintas horas simuladas (madrugada, amanecer, mañana, mediodía, tarde, atardecer, noche), verificar el saludo y frase motivacional; pulsar el chip `"Álbumes"`; comprobar que los primeros 6 ítems corresponden a `sort=latest` del servidor; reproducir 2 canciones y confirmar que `"Escuchado recientemente"` y `"Hecho para ti"` se actualizan en tiempo real; verificar que `"Todas las canciones"` ya no se renderiza en `HomeScreen`.

**Acceptance Scenarios**:

1. **Given** la fila de chips de filtro en `HomeScreen`, **When** se renderiza la pantalla, **Then** aparece el chip `"Álbumes"` justo al lado de `"Música"`, y al seleccionarlo muestra el catálogo de álbumes con carátula, título y artista (#3).
2. **Given** la hora local del dispositivo, **When** se muestra el encabezado de `HomeScreen`, **Then** presenta el saludo correspondiente (`Buenos días`, `Buenas tardes`, `Buenas noches`) y un subtítulo breve de ánimo/motivación acorde a las 7 etapas del día: Madrugada (`00:00–04:59`), Amanecer (`05:00–06:59`), Mañana (`07:00–11:59`), Mediodía (`12:00–13:59`), Tarde (`14:00–17:59`), Atardecer (`18:00–19:59`) y Noche (`20:00–23:59`) (#4).
3. **Given** la cuadrícula rápida de 6 elementos en la parte superior de `HomeScreen`, **When** se cargan los datos del servidor, **Then** muestra exactamente las últimas 6 canciones subidas al servidor (`GET /api/v1/tracks?sort=latest&limit=6`) (#5).
4. **Given** que el usuario reproduce una canción, **When** comienza la reproducción, **Then** se registra `lastPlayedAt` y se incrementa `playCount` localmente, actualizando de forma reactiva la sección `"Escuchado recientemente"` (#6) y recalculando `"Hecho para ti"` con canciones de los artistas más escuchados por el usuario (#7).
5. **Given** la pantalla `HomeScreen` en filtro `"Todo"` o `"Música"`, **When** el usuario hace scroll hasta el final, **Then** la sección vertical `"Todas las canciones"` ya no existe en `HomeScreen` (#19).

---

### User Story 3 - Búsqueda, Ordenamiento Integral, Carátulas Collage y Acciones en Biblioteca y Detalles (Prioridad: P1)

El usuario puede buscar y ordenar canciones dentro de cualquier Playlist o Álbum, ordenar todas las categorías de Tu Biblioteca (`Todo`, `Favoritas`, `Descargas`, `Playlists`, `Álbumes`), visualizar carátulas collage de 4 o 6 imágenes para sus playlists, y reproducir todo el catálogo del servidor desde la sección `"Todo"` de la Biblioteca mediante botones de `"Aleatorio"` y `"Reproducir desde la primera"`.

**Why this priority**: Cubre las mejoras esenciales de organización y descubrimiento en Biblioteca y vistas de detalle (#1, #12, #13, #19).

**Independent Test**: Entrar a un álbum o playlist, filtrar por texto y cambiar el criterio de ordenamiento; ir a `LibraryScreen`, probar el selector de ordenamiento en cada pestaña, verificar el collage de 4/6 portadas en las playlists, y pulsar `"Aleatorio"` y `"Reproducir desde la primera"` en la pestaña `"Todo"`.

**Acceptance Scenarios**:

1. **Given** `PlaylistDetailScreen` o `AlbumDetailScreen`, **When** el usuario escribe en el buscador interno o selecciona un criterio de ordenamiento (Orden original, Título A-Z/Z-A, Artista, Duración), **Then** la lista de pistas mostrada se filtra y reordena instantáneamente (#1).
2. **Given** `LibraryScreen` en cualquiera de sus categorías (`Todo`, `Favoritas`, `Descargas`, `Playlists`, `Álbumes`), **When** el usuario pulsa el encabezado de ordenamiento, **Then** puede elegir el criterio de orden (`Recientes`, `Título A-Z`, `Título Z-A`, `Artista`, `Duración` / `Cantidad de canciones`) y la lista se reordena dinámicamente (#12).
3. **Given** una playlist con canciones que poseen carátula, **When** se muestra en `LibraryScreen` o en el encabezado de `PlaylistDetailScreen`, **Then** genera un collage compuesto por las carátulas de las primeras 4 (cuadrícula 2x2) o 6 (cuadrícula 3x2) canciones de la playlist (#13).
4. **Given** la pestaña `"Todo"` en `LibraryScreen`, **When** el usuario visualiza la parte superior de la lista, **Then** dispone de todas las canciones del catálogo y dos botones de acción: `"Aleatorio"` (inicia reproducción barajada de todo el catálogo) y `"Reproducir desde la primera"` (inicia reproducción secuencial desde la primera canción disponible en el servidor) (#19).

---

### User Story 4 - Navegación Inteligente en Barra Inferior y Localización de Búsqueda (Prioridad: P2)

El usuario utiliza la barra de navegación inferior (`BottomNavBar`) para volver al tope de la lista en Biblioteca o restablecer la pantalla de Búsqueda con un toque, y lee un placeholder natural en español latinoamericano (`"¿Qué te gustaría escuchar?"`).

**Why this priority**: Mejora la ergonomía de navegación diaria y la naturalidad del idioma (#11, #14, #15).

**Independent Test**: En `SearchScreen`, verificar el texto `"¿Qué te gustaría escuchar?"`, realizar una búsqueda y pulsar nuevamente el ícono `"Buscar"` en la barra inferior para comprobar que limpia la búsqueda y vuelve a las categorías. En `LibraryScreen`, hacer scroll hacia abajo y pulsar el ícono `"Tu Biblioteca"` para verificar que sube animadamente al tope.

**Acceptance Scenarios**:

1. **Given** `SearchScreen`, **When** el campo de búsqueda está vacío, **Then** el placeholder muestra exactamente `"¿Qué te gustaría escuchar?"` (#11).
2. **Given** que el usuario ya está en `LibraryScreen` y ha desplazado la lista hacia abajo, **When** vuelve a pulsar el botón `"Tu Biblioteca"` en `BottomNavBar`, **Then** la lista actual se desplaza suavemente hasta el inicio (ítem 0) (#14).
3. **Given** que el usuario está en `SearchScreen` con una consulta activa o categoría seleccionada, **When** vuelve a pulsar el botón `"Buscar"` en `BottomNavBar`, **Then** se limpia la consulta activa y regresa a la vista inicial de exploración de búsqueda (#15).

---

### User Story 5 - Letras de Canciones (LRC y Texto Estático) y Nuevos Fondos Visuales (Froggy, Cosmos y Negro Puro) (Prioridad: P2)

El usuario puede ver la letra de la canción actual tanto si está sincronizada por tiempo (`.lrc`) como si es texto plano estático, y puede elegir en Ajustes entre 5 motores visuales de fondo para el reproductor: `Aurora Boreal`, `Visualizador de Audio`, `Froggy Clima` (inspirado en la rana del clima de Google), `Cosmos Estelar` y `Ninguno (Negro Puro)`.

**Why this priority**: Completa la experiencia inmersiva y de ahorro de batería/OLED en el reproductor (#2, #16).

**Independent Test**: Reproducir una canción con letra LRC y otra con letra en texto plano, abriendo el panel de letras en `FullScreenPlayer`. Ir a `SettingsScreen`, alternar entre los 5 fondos visuales (`AURORA`, `VISUALIZER`, `FROGGY`, `COSMOS`, `NONE`) y verificar su renderizado en `FullScreenPlayer`.

**Acceptance Scenarios**:

1. **Given** el panel de letras en `FullScreenPlayer`, **When** el servidor o proveedor externo devuelve letras en formato `.lrc` o en texto plano sin marcas de tiempo, **Then** `LrcParser` y `LyricsView` renderizan correctamente ambos formatos (con auto-scroll y resaltado para `.lrc`, y lectura desplazable limpia para texto estático) (#2).
2. **Given** la sección `"Fondo del Reproductor"` en `SettingsScreen`, **When** el usuario visualiza las opciones, **Then** puede seleccionar entre `"Aurora Boreal"`, `"Visualizador de Audio"`, `"Froggy Clima"`, `"Cosmos Estelar"` y `"Ninguno (Negro Puro)"`, reflejándose inmediatamente en `ZenBackground` dentro de `FullScreenPlayer` (#16).

---

### User Story 6 - Envío Simplificado de Reportes en 1 Clic (Prioridad: P2)

El usuario desea reportar un problema o frustración desde la pantalla de Ajustes con un solo clic, sin necesidad de iniciar sesión en GitHub ni navegar fuera de la aplicación.

**Why this priority**: Elimina la fricción para recibir feedback real de los usuarios (#18).

**Independent Test**: En `SettingsScreen`, escribir un comentario opcional (o dejar por defecto) y pulsar `"Enviar Reporte en 1 Clic"`; verificar que el envío se realiza mediante `POST /api/v1/support/report` mostrando confirmación inmediata dentro de la app sin abrir el navegador ni pedir login.

**Acceptance Scenarios**:

1. **Given** la tarjeta `"Diagnóstico y Soporte"` en `SettingsScreen`, **When** el usuario escribe un mensaje breve (o usa el diagnóstico automático) y pulsa `"Enviar Reporte en 1 Clic"`, **Then** la app envía el payload de diagnóstico + mensaje al endpoint `POST /api/v1/support/report` del backend y muestra un mensaje de éxito inmediato sin requerir cuenta de GitHub (#18).

---

## 3. Requirements *(mandatory)*

### Functional Requirements

- **FR-001 (Req #1)**: `PlaylistDetailScreen` y `AlbumDetailScreen` DEBEN incluir una barra de búsqueda interna colapsable/integrada y un selector de ordenamiento (`Orden original`, `Título A-Z`, `Título Z-A`, `Artista A-Z`, `Duración`) que filtren y ordenen reactivamente las canciones de la vista.
- **FR-002 (Req #2)**: El sistema DEBE soportar obtención y visualización de letras sincronizadas (`.lrc`) y estáticas (texto plano) mediante el endpoint `GET /api/v1/tracks/:id/lyrics` en el backend (con búsqueda en base de datos D1 y fallback a LRCLIB API) y el parser `LrcParser.kt` en el cliente Android.
- **FR-003 (Req #3)**: `HomeScreen` DEBE incluir el filtro `HomeFilter.ALBUMS("Álbumes")` junto a `"Música"`, mostrando una cuadrícula de álbumes disponibles al seleccionarse.
- **FR-004 (Req #4)**: `HomeScreen` DEBE reemplazar el texto fijo `"Buenas noches"` por un generador contextual según las 7 franjas horarias del día (`Madrugada`, `Amanecer`, `Mañana`, `Mediodía`, `Tarde`, `Atardecer`, `Noche`), mostrando el saludo correspondiente y una frase motivacional breve de 1 línea.
- **FR-005 (Req #5)**: El backend `GET /api/v1/tracks` DEBE aceptar el parámetro `sort=latest` (ordenando por las canciones más recientemente insertadas en el servidor), y la cuadrícula superior de 6 canciones en `HomeScreen` DEBE mostrar esos últimos 6 temas subidos.
- **FR-006 (Req #6)**: La aplicación DEBE registrar dinámicamente cada canción reproducida (`lastPlayedAt`, `playCount` en Room `TrackEntity` / `TrackDao`) para que la sección `"Escuchado recientemente"` en `HomeScreen` se actualice en tiempo real.
- **FR-007 (Req #7)**: La sección `"Hecho para ti"` en `HomeScreen` DEBE generarse dinámicamente priorizando canciones de los artistas más escuchados por el usuario según su historial (`playCount`), con fallback al catálogo general cuando aún no exista historial suficiente.
- **FR-008 (Req #8)**: Se DEBE eliminar por completo el soporte de Picture-in-Picture (PiP) en `AndroidManifest.xml`, `MainActivity.kt` y `FullScreenPlayer.kt`.
- **FR-009 (Req #9)**: El estado de reproducción (canción actual, artista, carátula, progreso, duración y cola activa) DEBE mantenerse sincronizado en un `PlaybackSessionStateHolder` (`@Singleton`) y reconstruirse desde `MediaController` al reconectar, evitando que el reproductor quede en blanco o pierda su cola al bloquear o salir al inicio.
- **FR-010 (Req #10)**: El contenedor raíz de `FullScreenPlayer` DEBE consumir todos los eventos táctiles en su superficie para impedir que los toques atraviesen hacia la pantalla subyacente y cambien a canciones fuera de la playlist activa.
- **FR-011 (Req #11)**: El placeholder del buscador en `SearchScreen` DEBE decir exactamente `"¿Qué te gustaría escuchar?"`.
- **FR-012 (Req #12)**: `LibraryScreen` DEBE permitir ordenar los elementos en todas sus pestañas (`Todo`, `Favoritas`, `Descargas`, `Playlists`, `Álbumes`) mediante un menú selector en la cabecera de ordenamiento.
- **FR-013 (Req #13)**: Las playlists DEBEN exponer hasta las primeras 6 URLs de carátula de sus canciones (`cover_urls`) y renderizar un collage automático de 4 (2x2) o 6 (3x2) portadas en `LibraryScreen` y `PlaylistDetailScreen`.
- **FR-014 (Req #14)**: Pulsar el botón `"Tu Biblioteca"` en `BottomNavBar` cuando ya se está en `LibraryScreen` DEBE desplazar la lista activa nuevamente hasta el tope.
- **FR-015 (Req #15)**: Pulsar el botón `"Buscar"` en `BottomNavBar` cuando ya se está en `SearchScreen` DEBE limpiar la búsqueda activa y devolver al usuario a la vista principal de búsqueda.
- **FR-016 (Req #16)**: `SettingsScreen` y `ZenBackground` DEBEN ofrecer 5 opciones de fondo para el reproductor: `"AURORA"` (Aurora Boreal), `"VISUALIZER"` (Visualizador de Audio), `"FROGGY"` (Froggy Clima), `"COSMOS"` (Cosmos Estelar) y `"NONE"` (Ninguno / Negro Puro `#000000`).
- **FR-017 (Req #17)**: `MonkifyPlaybackService` y `LockScreenActivity` DEBEN configurar los flags de ventana y receptor de bloqueo para que el reproductor en pantalla de bloqueo aparezca desde el primer bloqueo de pantalla y comparta el estado de `PlaybackSessionStateHolder`.
- **FR-018 (Req #18)**: `SettingsScreen` DEBE permitir enviar reportes de error o feedback en 1 solo clic hacia `POST /api/v1/support/report` sin requerir cuenta ni login de GitHub.
- **FR-019 (Req #19)**: Se DEBE retirar la sección `"Todas las canciones"` de `HomeScreen` y asegurar que la pestaña `"Todo"` en `LibraryScreen` muestre el catálogo completo con dos botones de cabecera: `"Aleatorio"` y `"Reproducir desde la primera"`.

---

## 4. Estados de Interfaz Visual y Contratos UX (Fase 1 Integrada)

| Pantalla / Componente | Estado Loading | Estado Empty | Estado Error / Fallback | Estado Success / Interactivo |
| :--- | :--- | :--- | :--- | :--- |
| **`HomeScreen` (Saludo, Chips, Últimos 6, Recientes, Hecho para ti)** | Shimmer/indicador circular esmeralda mientras sincroniza catálogo inicial. | Si no hay historial previo en `"Escuchado recientemente"`, muestra sugerencias iniciales o mensaje discreto; en `"Álbumes"` sin datos muestra estado vacío con botón Reintentar. | Banner no intrusivo conservando caché Room offline. | Saludo + frase motivacional de 1 línea, chip `"Álbumes"`, cuadrícula 2x3 con últimos 6 subidos, carruseles reactivos y sin lista infinita inferior. |
| **`PlaylistDetailScreen` & `AlbumDetailScreen`** | Indicador de carga centrado. | Si la búsqueda interna no arroja coincidencias: `"No se encontraron canciones para '[query]'"`. | Mensaje de error de red con opción de reintento. | Barra de búsqueda interna + botón de ordenamiento (BottomSheet/Dropdown) + collage de portada en playlists. |
| **`FullScreenPlayer` (Letras, Fondos y Bloqueo de Toques)** | Spinner dentro de `LyricsView` mientras consulta letras. | `"Letra no disponible para esta canción"` centrado con ícono musical. | Fallback silencioso a letra local/LRCLIB si el endpoint falla. | Letras `.lrc` con resaltado activo o letras estáticas desplazables; fondo `AURORA`, `VISUALIZER`, `FROGGY`, `COSMOS` o `NONE` (`#000000`); superficie opaca a eventos táctiles. |
| **`LibraryScreen` (Ordenamiento, Collage y Botones en `"Todo"`)** | Indicador de progreso si el catálogo está sincronizando. | Mensaje contextual según la pestaña (`Favoritas`, `Descargas`, `Playlists`, `Álbumes`). | Conserva catálogo local en Room (`TrackEntity`). | En `"Todo"`: fila superior con botones `"Aleatorio"` y `"Reproducir desde la primera"`. Cabecera de ordenamiento interactiva en todas las pestañas. |
| **`SettingsScreen` (Fondos y Reporte 1-Clic)** | Botón `"Enviando reporte..."` deshabilitado con spinner durante el `POST`. | Campo de comentario opcional con placeholder `"Describe brevemente qué ocurrió (opcional)..."`. | Si falla la red al enviar reporte, ofrece fallback inmediato por correo/portapapeles sin perder el texto. | Confirmación visual (`"¡Reporte enviado con éxito! Gracias por ayudarnos a mejorar."`) en un solo clic. |

---

## 5. Success Criteria *(mandatory)*

- **SC-001**: `.\gradlew.bat testDebugUnitTest` y `.\gradlew.bat assembleRelease` compilan sin errores y con el 100% de los tests unitarios en verde.
- **SC-002**: `npm test` en `cloudflare-backend` pasa el 100% de los tests (incluyendo pruebas para `sort=latest`, `GET /api/v1/tracks/:id/lyrics`, `cover_urls` en playlists y `POST /api/v1/support/report`).
- **SC-003**: Cero fugas de eventos táctiles a través de `FullScreenPlayer` y cero pérdidas de estado de reproducción/cola al minimizar, bloquear o desbloquear el dispositivo.
- **SC-004**: El usuario puede enviar un reporte de fallo desde Ajustes en 1 clic sin autenticarse en GitHub.
