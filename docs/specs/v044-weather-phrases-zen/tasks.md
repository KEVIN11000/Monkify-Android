# Tasks: Reactividad Climática para Frases de Cabecera y Enriquecimiento del Compendio Zen

**Feature ID:** `v044-weather-phrases-zen`  
**Prerrequisitos:** `docs/specs/v044-weather-phrases-zen/spec.md`, `docs/specs/v044-weather-phrases-zen/plan.md`  

---

## Tareas Atómicas y Secuenciales

- [x] **T001: Pruebas unitarias para el compendio ZenPhraseCatalog**  
  *Objetivo:* Crear `app/src/test/java/com/k11000/monkify/domain/model/ZenPhraseCatalogTest.kt` validando:  
  1) Existencia de al menos 3 frases únicas por cada una de las 20 combinaciones `(WeatherCondition, ZenDayPart)`.  
  2) Títulos coherentes para las 4 franjas horarias solares (`resolveTitle`).  
  3) Resolución modular segura sin excepciones `IndexOutOfBoundsException` para cualquier valor de `offset` negativo o grande.  
  *Criterio de Aceptación:* El test compila y falla inicialmente (Red en TDD). Concluido y verificado con fallo esperado por referencia no resuelta en `compileDebugUnitTestKotlin`.

- [x] **T002: Implementación de ZenPhraseCatalog con el compendio ampliado**  
  *Objetivo:* Crear `app/src/main/java/com/k11000/monkify/domain/model/ZenPhraseCatalog.kt` conteniendo las más de 70 frases poéticas y musicales en español categorizadas por clima y franja solar, junto con los métodos `resolveTitle(dayPart)` y `resolvePhrase(condition, dayPart, offset)`.  
  *Criterio de Aceptación:* `ZenPhraseCatalogTest.kt` pasa al 100% (Green en TDD) con `testDebugUnitTest --tests "com.k11000.monkify.domain.model.ZenPhraseCatalogTest"`. Superado exitosamente con 0 fallos.

- [x] **T003: Inyección de WeatherRepository y flujo reactivo en HomeViewModel**  
  *Objetivo:* Modificar `app/src/main/java/com/k11000/monkify/ui/screens/viewmodel/HomeViewModel.kt`:  
  1) Inyectar `WeatherRepository? = null`.  
  2) Exponer `val greeting: StateFlow<GreetingInfo>` combinando `weatherRepository.currentWeather` y un `MutableStateFlow` interno de `_phraseIndexOffset`.  
  3) Implementar el método público `fun cycleNextPhrase()`.  
  *Criterio de Aceptación:* Compilación exitosa con `compileDebugKotlin`. Superado exitosamente (`BUILD SUCCESSFUL`).

- [x] **T004: Pruebas unitarias para reactividad de cabecera en HomeViewModel**  
  *Objetivo:* Crear `app/src/test/java/com/k11000/monkify/ui/screens/viewmodel/HomeViewModelGreetingTest.kt` simulando emisiones climáticas (`RAINY`, `SUNNY`, `NIGHT`, `DAY`) en un mock de `WeatherRepository` y verificando que `greeting.value` se actualice reactivamente y rote con `cycleNextPhrase()`.  
  *Criterio de Aceptación:* `testDebugUnitTest --tests "com.k11000.monkify.ui.screens.viewmodel.HomeViewModelGreetingTest"` pasa con 0 errores. Superado al 100% en conjunto con `HomeViewModelTest`.

- [x] **T005: Conexión de interacción de toque en HomeScreen**  
  *Objetivo:* En `app/src/main/java/com/k11000/monkify/ui/screens/HomeScreen.kt`, conectar el evento de click sobre la cabecera (título y frase) para invocar `homeViewModel.cycleNextPhrase()`, permitiendo al usuario cambiar de frase al tocar la pantalla de inicio.  
  *Criterio de Aceptación:* Verificación de renderizado en Compose y compilación exitosa sin errores de UI. Superado exitosamente (`BUILD SUCCESSFUL`).

- [x] **T006: Verificación completa de suite, build de Release y actualización de documentación**  
  *Objetivo:* Ejecutar la suite completa de pruebas unitarias (`testDebugUnitTest`), compilar APK release (`assembleRelease`), actualizar `HANDOFF.md` y preparar el gate de commit con Git Hygiene estricto.  
  *Criterio de Aceptación:* Tests pasando al 100%, APK generado correctamente (47.4 MB) y working tree listo para auditoría pre-commit. Superado con éxito.
