# Especificación Técnica: v0.4.5 Hardening de Seguridad, Protección de Servidor y R8

## 1. Contexto y Objetivos
Durante la auditoría técnica post-release v0.4.4 se identificaron vulnerabilidades en el empaquetado y en la arquitectura de red:
1. **Riesgo de Agotamiento de Cuota y Abuso de Servidor Público (SEC-08)**: Al ser el repositorio público, la URL del backend (`monkify.workers.dev`) y el endpoint `/api/v1/stream/:track_id` son visibles. Cualquier scraper o script externo podría iterar y descargar canciones, agotando las 100,000 peticiones diarias gratuitas de Cloudflare Workers.
2. **Ausencia de Ofuscación y Minificación (SEC-03)**: `isMinifyEnabled = false` y falta física de `proguard-rules.pro`. Todo el código Kotlin queda expuesto a descompilación.
3. **Fuga Potencial de Credenciales (SEC-04)**: `AuthInterceptor` no discriminaba el host de destino, pudiendo enviar tokens a APIs ajenas como GitHub o Open-Meteo.
4. **Permisos Privilegiados Obsoletos (SEC-05)**: Presencia innecesaria de `REQUEST_INSTALL_PACKAGES` en `AndroidManifest.xml`.
5. **Respaldo Permisivo (SEC-07)**: Falta de exclusión de base de datos Room y tokens en `backup_rules.xml`.

El objetivo de esta iteración es implementar:
- Configuración de **Clave de Acceso al Servidor / Server Access Token** en la App (Ajustes) y Middleware de validación en Cloudflare Worker para blindar el streaming y la API contra accesos no autorizados.
- Hardening local completo: `proguard-rules.pro` (R8), restricción estricta de host en `AuthInterceptor`, remoción de permisos privileged y hardening de backups.

---

## 2. Alcance (In-Scope)

### 2.1 Módulo Servidor & Red (Protección contra Scrapers y Agotamiento de Cuota)
- **Token de Acceso en Android (`SettingsRepository`)**:
  - Almacenamiento seguro en DataStore de `server_access_token`.
  - Exposición reactiva en `SettingsViewModel` y campo en `SettingsScreen` (con visibilidad oculta/visible tipo contraseña).
- **Inyección Segura en `AuthInterceptor`**:
  - Inyección automática de cabeceras `X-Monkify-Token` y `Authorization: Bearer <token>` **exclusivamente** hacia hosts autorizados de Monkify (`*.workers.dev`, `monkify.tudominio.com`, `10.0.2.2`, `localhost`).
  - Bloqueo total de envío de credenciales hacia `api.github.com`, `api.open-meteo.com` o cualquier servicio externo.
- **Soporte de Token en ExoPlayer MediaSource**:
  - Asegurar que al reproducir `/api/v1/stream/:id`, si existe un token configurado, se adjunte como parámetro `?token=<token>` o cabecera HTTP para que ExoPlayer transmita la credencial al descargar los chunks de audio.
- **Middleware en Backend Cloudflare (`cloudflare-backend/src/index.ts`)**:
  - Middleware de autenticación en `/api/v1/*` que valide `X-Monkify-Token`, `Authorization: Bearer` o `?token=`.
  - Si `SERVER_ACCESS_TOKEN` está configurado en el Worker y la petición no coincide: rechazo inmediato con `401 Unauthorized` (ahorro de cómputo y 0 operaciones en R2/D1).

### 2.2 Hardening de la Aplicación Android (R8, ProGuard, Permisos)
- **SEC-03: Reglas de ProGuard / R8**:
  - Creación de `app/proguard-rules.pro` con directivas exhaustivas de preservación para Gson, Room DB, Retrofit2, OkHttp3, Media3 ExoPlayer, Coil y Rive nativo.
  - Activación de `isMinifyEnabled = true` e `isShrinkResources = true` en `release` en `app/build.gradle.kts`.
- **SEC-05: Limpieza de Manifiesto**:
  - Eliminación de `android.permission.REQUEST_INSTALL_PACKAGES` en `AndroidManifest.xml`.
- **SEC-07: Hardening de Respaldo**:
  - Exclusión explícita de Room (`monkify_database*`) y DataStore en `backup_rules.xml` y `data_extraction_rules.xml`.

---

## 3. Fuera de Alcance (No-Scope)
- Generación de Keystore de Producción y configuración de Secrets en GitHub Actions (se realizará en el Paso 2 de CI/CD).
- Cambios estéticos o de catálogo en Zen Froggy o Aurora.

---

## 4. Criterios de Aceptación y Validación
1. **Pruebas de Red**: Suite de tests unitarios verificando que `AuthInterceptor` inyecta el token en hosts de Monkify y lo omite en GitHub/Open-Meteo.
2. **Pruebas de UI**: La pantalla de Ajustes permite ingresar, guardar, editar y borrar la Clave de Acceso al Servidor de forma persistente.
3. **Pruebas de R8 / Compilación**: `.\gradlew.bat assembleRelease` compila exitosamente con ofuscación R8 y shrink de recursos activado.
4. **Pruebas de Backend**: Petición a `/api/v1/stream/1` sin token devuelve `401 Unauthorized` cuando el secreto está activo, y `200/206` con token válido.
