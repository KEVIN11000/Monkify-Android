# Checklist de Tareas Atómicas: v0.4.6 Recuperación de Producción, Aviso Flotante y Estados de Carga

- [x] **Tarea 1: Blindaje de ProGuard / R8 y Contrato de Serialización en DTOs (RCA-01 & RCA-06)**
  - **Archivos:** `app/proguard-rules.pro`, `app/src/main/java/com/k11000/monkify/data/remote/dto/Models.kt`, `app/src/test/java/com/k11000/monkify/data/remote/dto/DtoSerializationContractTest.kt`
  - **Acción:** Corregir el paquete en `proguard-rules.pro` (`com.k11000.monkify.data.remote.dto.**` y `com.k11000.monkify.domain.model.**`), añadir `@SerializedName` explícito a todas las propiedades de `BackendTrackDto` y demás DTOs en `Models.kt`, y crear `DtoSerializationContractTest` para verificar la deserialización completa con Gson y la presencia de `@SerializedName` en todos los campos de los DTOs.
  - **Criterio de Aceptación:** `DtoSerializationContractTest` pasa al 100% en terminal.

- [x] **Tarea 2: Monitor Global `ServerStatusMonitor`, Eliminación de Race Condition en `AuthInterceptor` y Observabilidad en Red (RCA-02 & RCA-04)**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/data/remote/api/ServerStatusMonitor.kt`, `app/src/main/java/com/k11000/monkify/data/repository/SettingsRepository.kt`, `app/src/main/java/com/k11000/monkify/di/NetworkModule.kt`, `app/src/main/java/com/k11000/monkify/data/repository/NetworkTrackRepository.kt`
  - **Acción:** Crear `ServerStatusMonitor` para emitir estados globales (`Idle`, `Connecting`, `Connected`, `Unauthorized`, `Error`), implementar `getServerAccessTokenImmediate()` en `SettingsRepository` conectado a `AuthInterceptor`, y reportar cada ciclo de petición/error HTTP (`401`, `5xx`, `Timeout`, `IOException`) desde `NetworkTrackRepository` tanto a `ServerStatusMonitor` como a `AppLogger`.
  - **Criterio de Aceptación:** Pruebas unitarias de repositorio y monitor en verde; el token está disponible síncronamente desde la primera llamada HTTP y ningún error de red queda silenciado.

- [x] **Tarea 3: Aviso Flotante Global (`FloatingServerStatusBanner`), Interfaces de Carga Prioritarias y Reconexión Reactiva (RCA-03, RCA-04 & RCA-05)**
  - **Archivos:** `app/src/main/java/com/k11000/monkify/ui/components/FloatingServerStatusBanner.kt`, `app/src/main/java/com/k11000/monkify/MainActivity.kt`, `app/src/main/java/com/k11000/monkify/ui/screens/HomeScreen.kt`, `app/src/main/java/com/k11000/monkify/ui/screens/SettingsScreen.kt`, `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/HomeViewModel.kt`, `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/LibraryViewModel.kt`, `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/SettingsViewModel.kt`, `app/src/main/java/com/k11000/monkify/playback/PlayerViewModel.kt`, `app/src/test/java/com/k11000/monkify/ui/viewmodel/SettingsViewModelTest.kt`
  - **Acción:** Crear `FloatingServerStatusBanner` (con soporte para estados `Connecting`, `Unauthorized 401`, `Error` y `Connected` + acciones `"Ir a Ajustes"` y `"Reintentar"`), integrarlo en `MainActivity`, agregar estado de carga dedicado en `HomeScreen` mientras `isLoading == true`, añadir prueba activa de conexión en `SettingsScreen`/`SettingsViewModel`, y activar recarga automática en `HomeViewModel` y `LibraryViewModel` al cambiar `serverAccessToken`.
  - **Criterio de Aceptación:** `SettingsViewModelTest` y `HomeViewModelTest` pasan al 100%, y las vistas (`FloatingServerStatusBanner` y estados de carga) superan el Gate Visual de Compose Preview.

- [x] **Tarea 4: Compilación Release con R8, Auditoría Forense de Bytecode (`usage.txt`) y Cierre de Calidad**
  - **Archivos:** `app/build.gradle.kts` (bump a `0.4.6` / `versionCode = 7`)
  - **Acción:** Ejecutar la suite completa `testDebugUnitTest`, compilar `assembleRelease` y auditar `app/build/outputs/mapping/release/usage.txt` para demostrar empíricamente que ningún campo de `BackendTrackDto` ni de los demás DTOs fue eliminado por R8.
  - **Criterio de Aceptación:** 100% de tests unitarios en verde y APK de Release verificado sin pérdida de campos en DTOs.
