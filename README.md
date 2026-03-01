# WallpaperMovil

App Android nativa que reproduce un video local (MP4/MKV) o una URL directa como **Live Wallpaper** animado.

**Stack:** Kotlin · Jetpack Compose Material 3 · AndroidX Media3 ExoPlayer · DataStore · minSdk 26

---

## Construir con Codemagic

### 1. Subir el proyecto a GitHub / GitLab / Bitbucket

> [!IMPORTANT]
> Antes de hacer push, genera el binario `gradle/wrapper/gradle-wrapper.jar`:
>
> ```powershell
> # En la raíz del proyecto (necesitas Gradle instalado globalmente ≥ 8.9)
> gradle wrapper --gradle-version 8.9
> ```
>
> Esto crea el archivo JAR necesario. Después haz commit de todo.

### 2. Conectar a Codemagic

1. Crea cuenta en [codemagic.io](https://codemagic.io).
2. **Add application** → elige tu repositorio.
3. Codemagic detecta `codemagic.yaml` automáticamente.

### 3. Configurar firma (solo para Release)

En el dashboard de Codemagic:

- **App Settings → Code signing → Android keystores**
- Sube tu archivo `.jks` / `.keystore`
- Nómbralo exactamente: **`wallpapermovil_keystore`**

El YAML ya referencia ese nombre y las variables `CM_KEYSTORE_PATH`, `CM_KEYSTORE_PASSWORD`, `CM_KEY_ALIAS` y `CM_KEY_PASSWORD` se inyectan solas.

### 4. Lanzar la build

- **Debug APK** → Workflow `android-debug` → no requiere keystore.
- **Release APK** → Workflow `android-release` → requiere keystore configurado.

El APK se descarga desde la pestaña **Artifacts** al finalizar la build.

---

## Estructura del proyecto

```
WallpaperMovil/
├── codemagic.yaml                  ← CI/CD config
├── gradlew / gradlew.bat           ← Gradle wrapper scripts
├── gradle/wrapper/
│   ├── gradle-wrapper.jar          ← ⚠ Generar con: gradle wrapper
│   └── gradle-wrapper.properties   ← Apunta a Gradle 8.9
└── app/src/main/java/.../
    ├── VideoWallpaperService.kt    ← Motor ExoPlayer
    ├── WallpaperPreferences.kt     ← DataStore
    └── MainActivity.kt             ← UI Compose + Material 3
```

---

## Permisos requeridos

| Permiso                 | Propósito                       |
| ----------------------- | ------------------------------- |
| `INTERNET`              | Reproducir videos desde URL     |
| `READ_MEDIA_VIDEO`      | Leer videos locales (API 33+)   |
| `READ_EXTERNAL_STORAGE` | Leer videos locales (API 26-32) |
| `BIND_WALLPAPER`        | Protege el servicio de fondos   |
