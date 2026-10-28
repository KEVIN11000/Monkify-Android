# Guía de Autenticación y Protección de Servidor (Monkify)

Esta guía explica el funcionamiento, configuración y verificación del sistema de token de acceso al servidor (**Server Access Token**) implementado para proteger la cuota de Cloudflare Workers frente a accesos no autorizados y scrapers.

---

## 1. Arquitectura y Principio de Funcionamiento

El backend en Cloudflare Worker expone el catálogo y el streaming de audio desde Cloudflare R2 y D1. Para evitar que terceros que descubran la URL pública consuman la cuota diaria de peticiones (100,000 req/día en la capa gratuita):

1. **Modo Retrocompatible (Sin Token en Servidor):**
   - Si la variable de entorno `SERVER_ACCESS_TOKEN` **no** está definida en el Worker, el servidor responde con normalidad a cualquier cliente. Esto permite desarrollo local y pruebas sin fricción.
2. **Modo Blindado (Token Activo en Servidor):**
   - Cuando se define `SERVER_ACCESS_TOKEN` en Cloudflare, el middleware intercepta toda petición a `/api/v1/*` (incluyendo `/api/v1/stream/:track_id`).
   - El Worker valida que la petición presente el token en:
     - Cabecera HTTP `X-Monkify-Token: <token>`
     - Cabecera HTTP `Authorization: Bearer <token>`
     - Parámetro de URL `?token=<token>` (utilizado por el reproductor ExoPlayer)
   - Si el token falta o es incorrecto, el Worker responde inmediatamente con **HTTP 401 Unauthorized** en ~2 ms, sin realizar operaciones de base de datos D1 ni transferencias de R2.
   - Rutas públicas esenciales como `/health` y los preflights CORS (`OPTIONS`) permanecen abiertas sin requerir token.

---

## 2. Dónde ver el Token Actual

### En la App Android:
1. Abre **Monkify** y ve a la pantalla de **Ajustes** (ícono de engranaje).
2. Desplázate hacia abajo hasta la sección **"SEGURIDAD Y CONEXIÓN DE SERVIDOR"**.
3. En el campo **"Token de acceso al servidor"**:
   - Por defecto se muestra enmascarado (`••••••••`).
   - Toca el **ícono del ojo** para alternar entre ver el token en texto claro o enmascarado.
   - Si no has configurado un token, el badge superior mostrará: `Sin configurar (acceso libre o servidor sin token)`.
   - Si ya está configurado y guardado, el badge mostrará: `Activo y protegido`.

### En Cloudflare (Servidor):
1. Inicia sesión en [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. Dirígete a **Workers & Pages** > selecciona el Worker **`monkify-backend`**.
3. Ve a la pestaña **Settings** > **Variables and Secrets**.
4. Allí verás `SERVER_ACCESS_TOKEN`:
   - Si se guardó como secreto cifrado (*Secret*), Cloudflare no permite leer el texto plano por políticas de seguridad de clave privada, pero puedes modificarlo o redefinirlo cuando desees.

---

## 3. Cómo Configurar el Token en el Servidor (Cloudflare)

Tienes dos alternativas para definir el token en el servidor:

### Opción A: Usando Wrangler CLI (Recomendado)
Desde la carpeta del backend en tu máquina:
```bash
cd Monkify/cloudflare-backend
npx wrangler secret put SERVER_ACCESS_TOKEN
```
La terminal solicitará ingresar el secreto:
```text
✔ Enter a secret value: ********************
✨ Success! Uploaded secret SERVER_ACCESS_TOKEN
```

### Opción B: Desde el Dashboard Web de Cloudflare
1. Ve a **Workers & Pages** > `monkify-backend` > **Settings** > **Variables and Secrets**.
2. Haz clic en **Add** (o **Edit** si ya existe).
3. Nombre de la variable: `SERVER_ACCESS_TOKEN`.
4. Valor: Escribe tu clave secreta personalizada.
5. Selecciona el tipo **Secret** (cifrado) o **Plaintext**.
6. Haz clic en **Save and Deploy**.

---

## 4. Cómo Conectar y Autenticar la App Android

Una vez definido el token en Cloudflare:
1. Abre la app Monkify en tu dispositivo Android.
2. Ve a **Ajustes** > **"SEGURIDAD Y CONEXIÓN DE SERVIDOR"**.
3. Escribe exactamente el mismo token en el campo **Token de acceso al servidor**.
4. Pulsa el botón **"Guardar"**.
5. Verás el mensaje de confirmación *"Token de servidor guardado"* y el indicador cambiará a *"Activo y protegido"*.

### Desconexión o Modo Libre:
Si retiras el token del servidor o deseas que la app no envíe ningún token, simplemente pulsa el botón **"Limpiar"** en Ajustes.

---

## 5. Prevención de Fugas de Seguridad (Host Restriction)
La app cuenta con un filtro estricto en `AuthInterceptor`:
- El token únicamente viaja a servidores autorizados de Monkify (`*.workers.dev`, `monkify.tudominio.com`, `10.0.2.2`, `localhost`).
- Si la app realiza peticiones a APIs externas de terceros (como Open-Meteo para el widget del clima o GitHub para verificar nuevas versiones), el token **nunca** se transmite a esos servidores.
