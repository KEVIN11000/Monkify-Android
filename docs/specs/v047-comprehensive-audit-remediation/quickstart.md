# Quickstart & Guía de Verificación Empírica (`v0.4.7`)

## 1. Verificación de Pruebas Unitarias (P1–P4)
Ejecutar la suite completa de tests unitarios:
```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```
**Resultado esperado:** `BUILD SUCCESSFUL` con tests verificando:
- Preservación de `isFavorite`, `isDownloaded`, `localFilePath` y `playCount` tras `upsertRemoteTracks` en `OfflineFirstTrackRepository`.
- Redacción de `?token=` y `Bearer` en `AppLogger` y validación estricta de host en `AuthInterceptor`.
- Unicidad de estado en `ServerStatusMonitor` al descartar el banner offline (`dismiss(byUser = true)`).
- Cobertura de los 60 recursos WebP en `FroggyAssetCatalogTest` y resolución de escenarios (`AUTO`, `FIELDS`, `HILL`, `MUSHROOM`).
- Tests reales sobre `PlayerViewModel` en `PlayerControlsTest`.

## 2. Verificación de Higiene de Seguridad y Git
```powershell
git status -s
git ls-files app/keystore/
```
**Resultado esperado:** `app/keystore/monkify-release.jks` no aparece rastreado en Git y `app/build.gradle.kts` no contiene contraseñas en texto plano.

## 3. Verificación Visual (Gate Visual de UI)
1. En **Ajustes > Modo Zen y Fondo Ambiental**, seleccionar `Froggy 🐸` y alternar entre los escenarios `Automático 🔄`, `Campos 🌾`, `Colina 🌳` y `Hongos 🍄`.
2. Verificar en `HomeScreen`, `FullScreenPlayer` y `LockScreenActivity` que el fondo oficial WebP de Froggy cambia según el escenario y el clima actual, mostrando las partículas atmosféricas, la animación de Froggy y el indicador `temperatureSummary`.
3. Verificar en `FullScreenPlayer`, `LockScreenActivity` y `LibraryScreen` (con selección por lotes activa) que ningún botón inferior se solapa con la barra de navegación del sistema.
