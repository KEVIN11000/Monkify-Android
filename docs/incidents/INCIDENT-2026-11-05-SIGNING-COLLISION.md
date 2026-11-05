# Reporte de Incidente Post-Mortem: Conflicto de Firmas y Certificados Efímeros en Releases

**Identificador:** INC-2026-11-05-SIGNING-COLLISION  
**Fecha:** 2026-11-05  
**Severidad:** Alta (Bloqueante de Actualización)  
**Módulos Afectados:** CI/CD (`.github/workflows/release.yml`), Compilación Gradle (`app/build.gradle.kts`), Distribución GitHub Releases  
**Estado:** ✅ Resuelto y Blindado  

---

## 1. Resumen Ejecutivo
Al intentar actualizar la aplicación móvil en dispositivos reales desde GitHub Releases, el sistema operativo Android rechazó la instalación notificando *"Conflicto de paquetes: Ya existe un paquete con el mismo nombre y una firma en conflicto"* (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).

La investigación empírica mediante `apksigner` reveló que las versiones publicadas en GitHub Actions (v0.4.6 y v0.4.7) contenían firmas criptográficas completamente distintas entre sí y distintas del almacén de producción oficial.

---

## 2. Evidencia Empírica de Huellas Criptográficas

La auditoría forense con `apksigner verify --verbose --print-certs` arrojó las siguientes firmas discrepantes:

| Origen del APK | Certificado DN | Huella SHA-256 |
|---|---|---|
| **Almacén Oficial Permanente** (`monkify-release.jks`) | `CN=Monkify Release, OU=Mobile, O=Monkify, L=Buenos Aires, ST=BA, C=AR` | `62:ED:E6:3F:FB:1E:55:0F:86:8E:76:00:02:52:F6:4E:33:94:08:45:38:5B:6D:30:7E:34:26:67:AA:74:4B:5F` |
| **GitHub Release v0.4.6** (`Monkify-v0.4.6.apk`) | `CN=Android Debug, O=Android, C=US` | `dd:ef:7d:bf:ca:8d:ba:1e:da:5d:ac:67:a5:10:4b:86:a3:e9:d6:d0:26:de:b6:ec:56:4b:1a:22:73:26:e0:76` |
| **GitHub Release v0.4.7 Inicial** (`Monkify-v0.4.7.apk`) | `CN=Android Debug, O=Android, C=US` | `f3:16:53:f3:0a:44:ed:1e:60:1f:de:c9:fb:06:95:1a:b0:64:f7:46:14:b0:a1:23:fd:ce:c2:16:6d:6f:d3:d0` |

---

## 3. Análisis de Causa Raíz (RCA)

1. **Ausencia del Secreto en Repositorio Remoto:**  
   El almacén permanente `app/keystore/monkify-release.jks` está correctamente protegido en `.gitignore` para cumplir la política de fuga cero. Sin embargo, su representación codificada no había sido cargada en los secretos de GitHub Actions (`RELEASE_KEYSTORE_BASE64`).
2. **Trampa de Fallback Silencioso en CI:**  
   El workflow `.github/workflows/release.yml` contenía una bifurcación tolerante:
   ```bash
   else
     echo "Warning: RELEASE_KEYSTORE_BASE64 not set; falling back to debug keystore."
     mkdir -p ~/.android
     keytool -genkey -v -keystore ~/.android/debug.keystore ...
   ```
   Al ejecutarse en un entorno virtual efímero de Ubuntu que se inicializa en blanco para cada tag de release, `keytool -genkey` generaba un certificado de desarrollo nuevo, impredecible y con huella aleatoria en cada build de CI.
3. **Falsa Sensación de Éxito (`BUILD SUCCESSFUL`):**  
   El pipeline de GitHub Actions completaba en verde, creaba la release y adjuntaba el APK como si fuese una versión oficial válida, ocultando el defecto criptográfico hasta el momento en que el usuario final intentaba la actualización en su dispositivo.

---

## 4. Acciones de Remediación Ejecutadas

1. **Carga Segura de Credenciales en GitHub Actions:**  
   El usuario cargó en GitHub Actions Secrets la variable `RELEASE_KEYSTORE_BASE64` con el contenido base64 del almacén `monkify-release.jks`.
2. **Eliminación Definitiva de Claves Efímeras en CI:**  
   Se modificó `.github/workflows/release.yml` para erradicar `keytool -genkey`. Si el secreto `RELEASE_KEYSTORE_BASE64` o el archivo restaurado faltan o están vacíos, el workflow aborta inmediatamente (`exit 1`), garantizando que jamás se publique un binario con firma corrupta o efímera.
3. **Resolución Robusta en Gradle:**  
   Se actualizó `app/build.gradle.kts` para buscar el keystore mediante resolución multicamino (`rootProject.file` y `file`) asegurando portabilidad tanto en ejecución local como en CI.
4. **Sustitución en Caliente del Asset en GitHub Releases:**  
   Se descargó y eliminó el APK efímero de la Release v0.4.7 y se subió el APK oficial `app-release.apk` firmado con la clave permanente (`62:ED:E6:3F:...`).

---

## 5. Medidas Preventivas y Lección Aprendida

1. **Prohibición de Fallbacks Placebo en Releases:** Ningún pipeline de entrega o release debe contemplar fallbacks que generen credenciales falsas o efímeras. Si los requisitos criptográficos de producción no están satisfechos, el build debe fallar ruidosamente.
2. **Auditoría Criptográfica Obligatoria:** Todo entregable móvil debe verificarse con `apksigner verify --verbose --print-certs` antes de darse por completada la fase o publicarse al usuario.
