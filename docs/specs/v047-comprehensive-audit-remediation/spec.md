# Feature Specification: Remediación Integral de Auditoría v0.4.7 (Datos, Seguridad, Estabilidad y UI/UX)

**Feature Branch**: `refine/v0.4`

**Created**: 2026-10-30

**Status**: Draft

**Input**: User description: "ATENDAMOS LOS CUATRO PUNTOS POR ORDEN DE PRIORIDAD"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Preservación Íntegra de Biblioteca Local y Resiliencia de Base de Datos (Priority: P1)

El usuario que navega por el catálogo en línea, realiza búsquedas o sincroniza pistas desde el servidor conserva intactos sus favoritos marcados, canciones descargadas en el dispositivo, rutas de archivos locales y contadores/fechas de reproducción histórica sin que las consultas remotas sobrescriban su estado local ni que actualizaciones futuras de esquema destruyan su biblioteca.

**Why this priority**: Actualmente (`CRIT-03`), cada consulta exitosa al catálogo remoto inserta las pistas en la base de datos local con `isFavorite = false` e `isDownloaded = false` usando reemplazo destructivo (`REPLACE`), borrando silenciosamente los favoritos, descargas e historial del usuario mientras usa la aplicación con internet. Además (`CRIT-04`), la base de datos carece de exportación de esquema y tiene migración destructiva activada.

**Independent Test**: Marcar una pista como favorita, descargarla localmente e incrementar su contador de reproducción; luego ejecutar una búsqueda y recarga del catálogo online para esa misma pista y comprobar que sigue figurando como favorita, descargada y con su contador intacto.

**Acceptance Scenarios**:

1. **Given** una pista previamente marcada como favorita (`isFavorite = true`), descargada (`isDownloaded = true`, `localFilePath != null`) o con reproducciones registradas (`playCount > 0`), **When** la aplicación recibe esa misma pista desde el servidor remoto al cargar Inicio, Biblioteca o Búsqueda, **Then** el sistema actualiza únicamente los metadatos del catálogo remoto (título, artista, álbum, duración, carátula y URL remota) preservando íntegramente `isFavorite`, `isDownloaded`, `localFilePath`, `addedAt`, `playCount` y `lastPlayedAt`.
2. **Given** una pista descargada cuyo archivo local fue eliminado externamente del almacenamiento antes de ejecutarse la reconciliación, **When** el usuario intenta reproducirla, **Then** el sistema conserva la URL de transmisión remota original y realiza el fallback automático a streaming en línea sin fallar por ruta local inexistente.
3. **Given** el primer arranque de la aplicación sin conexión a internet y con la base de datos local vacía, **When** se consultan las pistas recientes, **Then** el flujo de datos emite inmediatamente una lista vacía permitiendo que la interfaz finalice el estado de carga en lugar de quedar esperando indefinidamente.
4. **Given** operaciones de sincronización de catálogo o consultas por álbum, **When** se actualizan o leen lotes de pistas, **Then** las escrituras se ejecutan en lote dentro de una transacción atómica y las búsquedas por álbum e índices se resuelven directamente en el motor de base de datos sin cargar toda la tabla en memoria.

---

### User Story 2 - Blindaje de Credenciales, Firma de Producción y Fronteras de Seguridad (Priority: P2)

El usuario y el administrador del sistema operan con garantías de que ni el certificado privado de firma de la aplicación ni el token de acceso al servidor (`server_access_token`) se exponen en el repositorio, en archivos de registro locales, en reportes públicos de GitHub, en copias de seguridad en la nube, ni ante dominios externos o aplicaciones de terceros instaladas en el dispositivo.

**Why this priority**: La presencia del keystore y contraseñas en el árbol de código (`CRIT-01`) compromete la cadena de confianza de actualizaciones, mientras que adjuntar `?token=...` en URLs registradas en texto plano en `monkify.log` y prellenadas en Issues públicos de GitHub (`CRIT-02`) expone la clave privada del servidor del usuario.

**Independent Test**: Reproducir pistas remotas, generar un enlace de reporte de error para GitHub con logs adjuntos e inspeccionar `monkify.log` y la URL generada, comprobando que el valor del `server_access_token` nunca aparece en texto plano.

**Acceptance Scenarios**:

1. **Given** el árbol de código fuente y configuración de compilación, **When** se auditan los archivos rastreados por control de versiones y los scripts de compilación, **Then** no existen archivos `.jks`/`.keystore` versionados ni contraseñas de firma hardcodeadas como fallback en los scripts de construcción.
2. **Given** una sesión de reproducción activa con `server_access_token` configurado, **When** el reproductor cambia de pista o registra eventos en `AppLogger` y el usuario genera un reporte hacia GitHub Issues, **Then** cualquier parámetro `token` o encabezado sensible es redactado (`[REDACTED]`) antes de escribirse en memoria, consola, archivo de log o URL de soporte, y el flujo de streaming autentica mediante cabeceras HTTP seguras en dominios estrictamente verificados.
3. **Given** un dominio externo en Cloudflare Workers o un dominio similar no perteneciente al servidor configurado, **When** el cliente realiza peticiones HTTP o de streaming, **Then** el sistema valida estrictamente el host contra el dominio base configurado antes de adjuntar credenciales.
4. **Given** las políticas de respaldo de Android y configuración de red/ofuscación, **When** el sistema operativo ejecuta un backup o compila en modo Release, **Then** los directorios de `datastore/` y `logs/` quedan excluidos de la extracción en nube, el tráfico en texto plano (`cleartext`) solo se permite hacia direcciones locales de desarrollo, el servicio de reproducción valida los controladores externos y las reglas de R8 preservan correctamente los paquetes reales de entidades e imágenes.

---

### User Story 3 - Fidelidad Operacional de Ajustes, Caché y Reproducción sin Efectos Placebo (Priority: P3)

El usuario que interactúa con los controles de la aplicación (vaciar caché de audio, cambiar límite de caché, activar Material You, elegir dispositivo de salida de audio o importar enlaces desde el portapapeles) obtiene el comportamiento real prometido por la interfaz, sin corromper el caché activo ni disparar enlaces o audios falsos de prueba.

**Why this priority**: Existen funcionalidades simuladas o destructivas en producción (`CRIT-05`, `CRIT-06`, `CRIT-07`): borrar el caché elimina la carpeta en disco por debajo de la instancia activa de caché provocando fallos de lectura/escritura; elegir salida de audio, color dinámico o límite de caché no surte efecto real; y pegar un portapapeles vacío envía un enlace hardcodeado de Rick Astley al servidor de producción.

**Independent Test**: Seleccionar un dispositivo de salida de audio, activar Tema Dinámico en Android 12+, pulsar "Pegar enlace" con el portapapeles vacío y ejecutar "Vaciar Caché de Audio" mientras se reproduce música, verificando que cada acción aplica su efecto real de forma segura.

**Acceptance Scenarios**:

1. **Given** el reproductor de música en ejecución, **When** el usuario pulsa "Vaciar Caché de Audio" en Ajustes, **Then** el sistema libera las entradas del caché a través de la API segura del gestor de caché sin corromper su índice ni romper las reproducciones subsiguientes, y respeta el límite de tamaño (`cacheLimitMb`) configurado por el usuario al inicializar el caché.
2. **Given** el selector "Salidas de Audio" en el reproductor a pantalla completa, **When** el usuario selecciona un dispositivo físico o Bluetooth de la lista, **Then** el motor de reproducción enruta efectivamente la salida de audio hacia el dispositivo elegido.
3. **Given** el ajuste "Tema Dinámico (Material You)" en dispositivos compatibles, **When** el usuario activa o desactiva el interruptor, **Then** el tema global de la aplicación y de la pantalla de bloqueo recibe y aplica el estado configurado.
4. **Given** la pantalla de búsqueda/ingesta y el reproductor, **When** el usuario pulsa "Pegar enlace" con el portapapeles vacío o intenta reproducir una pista sin fuente válida, **Then** la aplicación informa claramente que el portapapeles está vacío o que la pista no tiene fuente disponible, sin enviar jamás URLs hardcodeadas de terceros (Rick Astley / Pixabay) ni reportar versiones obsoletas (`0.4.0` en lugar de la versión real).

---

### User Story 4 - Sincronización de Estado, Fondo Climático Froggy Completo, Fluidez Visual (60fps) y Ergonomía Edge-to-Edge (Priority: P4)

El usuario experimenta el fondo climático dinámico de **Froggy** funcionando al 100% (con clima real basado en su ubicación, rotación/selección de los 3 escenarios oficiales `FIELDS`, `HILL` y `MUSHROOM` —60 fondos WebP— junto con sus capas de animación atmosférica y soporte Rive `.riv` conservado, sincronizado tanto en Inicio como en el Reproductor y en la Pantalla de Bloqueo), en una interfaz fluida sin tirones en el hilo principal, donde el aviso flotante de conexión respeta el cierre manual, la cola en modo aleatorio no sufre saltos dobles al terminar y los controles respetan las barras de navegación del sistema (`WindowInsets`).

**Why this priority**: Aunque los 60 fondos oficiales de `R0rt1z2/Froggy` y la infraestructura de `Open-Meteo` y `Rive` fueron añadidos en `v0.4.3`, en la práctica el fondo de Froggy quedó parcialmente desconectado: (1) nunca se solicita el permiso `ACCESS_COARSE_LOCATION` en tiempo de ejecución, cayendo siempre a coordenadas fijas; (2) 40 de los 60 fondos (`HILL` y `MUSHROOM`) son inalcanzables porque `scenery` está fijo en `FIELDS`; (3) `LockScreenActivity` no le pasa el clima ni la franja solar a `ZenBackground`; y (4) existen problemas de estado dividido en `ServerStatusMonitor` (`HIGH-05`), doble listener de Shuffle (`HIGH-06`), I/O en el hilo principal (`HIGH-04`), recomposiciones de pantalla completa (`HIGH-07`) y solapamiento con la barra de navegación (`HIGH-08`).

**Independent Test**: Seleccionar el motor visual "Froggy", alternar o rotar entre los escenarios (`Campos`, `Colina`, `Hongos`), conceder ubicación aproximada (o ciudad manual) para ver el cambio de escena y partículas climáticas en `HomeScreen`, `FullScreenPlayer` y `LockScreenActivity`, descartar manualmente el banner offline y comprobar que ningún control inferior queda tapado por la barra del sistema.

**Acceptance Scenarios**:

1. **Given** el motor visual "Froggy 🐸" activo en Ajustes Zen, **When** el usuario visualiza `HomeScreen`, `FullScreenPlayer` o `LockScreenActivity`, **Then** las tres pantallas reciben el clima en tiempo real (`WeatherCondition`), la franja astronómica (`ZenTimeOfDay`) y el escenario Froggy (`FIELDS`, `HILL` o `MUSHROOM`), renderizando los 60 fondos oficiales de Froggy con sus animaciones atmosféricas dinámicas (lluvia con ondas, nieve, neblina, rayos de sol, luciérnagas y ranita animada/contenedor Rive `.riv` conservado) y solicitando `ACCESS_COARSE_LOCATION` cuando corresponda (con selector de escenario y soporte de ubicación/indicador climático en Ajustes Zen).
2. **Given** el monitor de estado del servidor y el reproductor en modo Shuffle, **When** el usuario descarta manualmente el aviso offline o la cola llega al final (`STATE_ENDED`), **Then** existe una única fuente de verdad para el estado del banner en toda la aplicación y un único controlador responsable del reinicio aleatorio sin condiciones de carrera.
3. **Given** el inicio de reproducción de una cola o la apertura de la Biblioteca y el registro de logs, **When** se verifican archivos locales o se escriben registros en disco, **Then** todas las operaciones de entrada/salida de disco se ejecutan fuera del hilo principal (`Main Thread`) de forma segura entre hilos (`thread-safe`).
4. **Given** el reproductor a pantalla completa (`FullScreenPlayer`) y los fondos animados (`ZenBackground`), **When** avanza el progreso de la canción o se anima el fondo climático, **Then** las actualizaciones de progreso solo recomponen la barra de tiempo/progreso y las animaciones se ejecutan sin recomponer innecesariamente toda la jerarquía de pantalla.
5. **Given** cualquier pantalla de la aplicación (`FullScreenPlayer`, `LockScreenActivity`, `LibraryScreen`, `PlaylistDetailScreen`, `AlbumDetailScreen`), **When** se visualiza en dispositivos con navegación por 3 botones o gestos, **Then** todos los controles inferiores respetan los márgenes del sistema (`WindowInsets`), los campos de búsqueda no recortan el texto, el indicador de carga de letras (`isLoadingLyrics`) se muestra mientras descarga y el resaltado de pista activa en Biblioteca compara por identificador único (`track.id`).

---

### Edge Cases

- ¿Qué ocurre si el usuario cambia su `server_access_token` mientras existen pistas almacenadas en el caché de streaming? Las claves de caché de audio no deben depender del parámetro `?token=` en la URL para evitar invalidar todo el caché ni filtrar el token en las URIs de los medios.
- ¿Qué ocurre cuando el usuario activa la selección por lotes en `LibraryScreen` y desplaza la lista hasta el final? La lista debe añadir margen inferior dinámico suficiente para que las últimas canciones nunca queden ocultas detrás de la barra flotante de acciones por lotes, la cual también debe permitir añadir las pistas seleccionadas a una playlist.
- ¿Qué ocurre si el usuario activa el fondo climático Zen pero nunca se le solicitó el permiso de ubicación en tiempo de ejecución? La aplicación debe solicitar el permiso `ACCESS_COARSE_LOCATION` de forma contextual cuando corresponda o permitir el fallback limpio sin bloquear la experiencia.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001 (Prioridad 1 - Base de Datos)**: El sistema DEBE realizar operaciones de *upsert* o fusión no destructiva al guardar pistas provenientes del servidor remoto en `OfflineFirstTrackRepository` y `TrackDao`, garantizando que las columnas locales (`isFavorite`, `isDownloaded`, `localFilePath`, `addedAt`, `playCount`, `lastPlayedAt`) jamás sean restablecidas a sus valores por defecto cuando la pista ya exista en la base de datos local.
- **FR-002 (Prioridad 1 - Base de Datos)**: El modelo de dominio `Track` y su mapeo desde `TrackEntity` DEBEN conservar la URL de streaming remota original separada de la verificación de archivo local, garantizando el fallback transparente a streaming si el archivo local deja de existir.
- **FR-003 (Prioridad 1 - Base de Datos)**: `TrackEntity` DEBE definir índices en las columnas de filtrado y ordenamiento frecuente (`isFavorite`, `isDownloaded`, `addedAt`, `lastPlayedAt`, `playCount`, `album`), ejecutar las sincronizaciones de catálogo en lote dentro de una transacción (`@Transaction`), habilitar la exportación de esquema de Room y asegurar que `getRecentTracks()` emita siempre una lista vacía cuando tanto la caché local como la respuesta remota estén vacías.
- **FR-004 (Prioridad 2 - Seguridad)**: El sistema DEBE eliminar del control de versiones cualquier archivo keystore (`.jks`/`.keystore`), añadir su exclusión en `.gitignore` y retirar las contraseñas de firma hardcodeadas de los archivos Gradle (requiriendo variables de entorno o propiedades locales ignoradas para el firmado de release).
- **FR-005 (Prioridad 2 - Seguridad)**: El servicio de reproducción (`MonkifyPlaybackService`) DEBE autenticar las peticiones de streaming mediante `OkHttpDataSource.Factory` utilizando el cliente `OkHttpClient` autenticado por cabeceras (en lugar de inyectar `?token=` en las URIs de `MediaItem`), y `AppLogger` junto con `HttpLoggingInterceptor` DEBEN sanitizar/redactar cualquier token o cabecera de autorización antes de registrar en consola, disco o enlaces de GitHub Issues.
- **FR-006 (Prioridad 2 - Seguridad)**: La validación de hosts de confianza en `AuthInterceptor` y componentes de red DEBE verificar coincidencia exacta de host o límite estricto de subdominio contra el host configurado en `BuildConfig.BASE_URL`, excluyendo dominios genéricos `*.workers.dev` de terceros.
- **FR-007 (Prioridad 2 - Seguridad)**: Las reglas de respaldo (`backup_rules.xml` y `data_extraction_rules.xml`), configuración de seguridad de red (`network_security_config.xml`), `file_paths.xml`, validación de sesión en `MonkifyPlaybackService` y reglas de ProGuard/R8 (`coil3.**`, `data.local.entity.**`) DEBEN actualizarse para impedir la filtración de `datastore/` y `logs/`, restringir tráfico HTTP en claro a entornos locales y proteger los modelos reales en compilaciones minificadas.
- **FR-008 (Prioridad 3 - Estabilidad y Funcionalidad Real)**: `SettingsRepository.clearMediaCache()` DEBE liberar los recursos a través de la instancia compartida de `SimpleCache` sin corromper su estado en caliente, y la inicialización de `SimpleCache` DEBE respetar el límite configurado por el usuario en `cacheLimitMb`.
- **FR-009 (Prioridad 3 - Estabilidad y Funcionalidad Real)**: `PlayerViewModel.selectDevice()` DEBE enrutar la salida de audio hacia el `AudioDeviceInfo` seleccionado en el reproductor; `MyApplicationTheme` DEBE recibir y aplicar el estado `dynamicColor` configurado por el usuario; y todos los reportes/cabeceras DEBEN usar dinámicamente `BuildConfig.VERSION_NAME` en lugar de `"0.4.0"`.
- **FR-010 (Prioridad 3 - Estabilidad y Funcionalidad Real)**: Se DEBEN eliminar por completo la URL hardcodeada de Spotify (Rick Astley) en `SearchScreen` (mostrando aviso de portapapeles vacío) y el audio fallback de Pixabay en `PlayerViewModel` (manejando la ausencia de URL de streaming como error controlado de pista no disponible).
- **FR-011 (Prioridad 4 - Rendimiento, Estado y UI/UX)**: `ServerStatusMonitor` DEBE consolidarse en una única instancia real en toda la aplicación (eliminando la divergencia entre la instancia de Hilt y `ServerStatusMonitor.shared`), y la lógica de reinicio de cola en modo Shuffle (`STATE_ENDED`) DEBE residir en un único punto sin duplicación entre servicio y ViewModel.
- **FR-012 (Prioridad 4 - Rendimiento, Estado y UI/UX)**: Las operaciones de sistema de archivos en `DownloadRepositoryImpl`, `PlayerViewModel.playQueue()`, `LibraryViewModel` y `AppLogger` DEBEN ejecutarse en `Dispatchers.IO` con formateo de fecha seguro entre hilos (`thread-safe`), y se DEBE limpiar el código muerto real (`MockTrackRepository`, `ApkInstaller`, `SettingsViewModel` redundante en `MiniPlayer`), **conservando íntegramente `rive-android` y la arquitectura de capas de `ZenBackground`**.
- **FR-013 (Prioridad 4 - Fondo Climático Froggy Completo y Ergonomía UI/UX)**:
  1. **Completitud del Sistema Froggy + Clima + Rive:** Se DEBE solicitar el permiso `ACCESS_COARSE_LOCATION` de forma segura en tiempo de ejecución para que `CoarseLocationDataSource` obtenga el clima real del usuario (y persistir en `LocalWeatherStore` cualquier ubicación manual de respaldo); exponer en `SettingsRepository`/`SettingsScreen` la selección o rotación de los 3 escenarios oficiales de Froggy (`FIELDS`, `HILL`, `MUSHROOM` para habilitar los 60 fondos WebP de `FroggyAssetCatalog`); mostrar el resumen climático (`temperatureSummary`) cuando Froggy esté activo; sincronizar `LockScreenActivity` pasando `timeOfDay`, `weatherCondition` y `scenery` a `ZenBackground`; y enriquecer la animación dinámica de Froggy sobre el escenario climático manteniendo activo el contenedor Rive (`.riv`).
  2. **Fluidez y Ergonomía Compose:** Se DEBEN aislar las lecturas de estado de progreso (`1Hz`) en `FullScreenPlayer` para evitar recomposiciones globales; corregir los `WindowInsets` inferiores en `FullScreenPlayer`, `LockScreenActivity` y `LibraryScreen`; conectar `isLoadingLyrics`, comparación por `track.id`, acción por lotes "Añadir a Playlist", filtro "Música", altura/color de buscadores y tamaños táctiles mínimos (`>= 48dp`).

### Key Entities *(include if feature involves data)*

- **TrackEntity / Track**: Entidad persistida localmente en SQLite (`favorite_tracks`) que combina metadatos del catálogo remoto (`id`, `title`, `artist`, `album`, `durationMs`, `coverUrl`, `streamUrl`) con el estado personal del usuario (`isFavorite`, `isDownloaded`, `localFilePath`, `addedAt`, `playCount`, `lastPlayedAt`).
- **FroggyScenery / CurrentWeather / ZenBackground**: Subsistema ambiental Zen que combina el clima en tiempo real de Open-Meteo (`SUNNY`, `CLOUDY`, `HAZY`, `RAINY`, `SNOWY`), la franja solar astronómica (`DAWN`, `DAY`, `SUNSET`, `NIGHT`), los 60 escenarios oficiales de Froggy (`FIELDS`, `HILL`, `MUSHROOM`), partículas atmosféricas vectoriales y soporte de animaciones Rive (`.riv`).
- **ServerStatusMonitor**: Monitor centralizado de estado de conectividad y autenticación frente al backend de Monkify.
- **SimpleCache / AudioDeviceInfo**: Componentes de caché de medios y enrutamiento físico de salida de audio gestionados por el motor de reproducción.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las pistas marcadas como favoritas, descargadas o reproducidas por el usuario mantienen su estado local intacto tras múltiples sincronizaciones, búsquedas y recargas del catálogo en línea.
- **SC-002**: Cero (`0`) apariciones en texto plano del `server_access_token` o contraseñas de keystore en el repositorio, en `monkify.log`, en Logcat o en las URLs generadas para GitHub Issues.
- **SC-003**: El 100% de los controles de configuración y reproducción auditados (selector de salida de audio, vaciado y límite de caché, tema dinámico, pegado de portapapeles vacío y versión de aplicación reportada) ejecutan su acción real sin comportamientos simulados ni corrupción de caché.
- **SC-004**: El 100% de los 60 escenarios climáticos de Froggy (`FIELDS`, `HILL`, `MUSHROOM` × 4 franjas × 5 climas) son alcanzables y se muestran dinámicamente animados con el clima real del usuario en `HomeScreen`, `FullScreenPlayer` y `LockScreenActivity`, con cero (`0`) accesos síncronos a disco en el hilo principal y 100% de la suite de pruebas unitarias superada con éxito.

## Assumptions

- El servidor backend de Monkify soporta autenticación mediante las cabeceras `Authorization: Bearer <token>` y `X-Monkify-Token: <token>` en el endpoint de streaming de audio cuando las peticiones provienen de `OkHttpDataSource` a través del `OkHttpClient` configurado.
- En entornos locales o de CI donde no se definan variables de entorno para el keystore de release, la compilación de release utilizará propiedades locales no versionadas (`keystore.properties` ignorado en git) o fallará de forma explícita y segura al intentar firmar un artefacto de producción sin credenciales, sin afectar las compilaciones ni pruebas `debug`.
