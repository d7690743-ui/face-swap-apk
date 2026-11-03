# Face Swap APK

App de intercambio de caras con **InsightFace buffalo** (detección) +
**inswapper_128** (swap), solo CPU. Consta de dos piezas:

- `app_python/` — núcleo Python real y funcional (CLI + backend Flask).
  Descarga los modelos solo en la primera ejecución (~800 MB).
- `app/` — app Android nativa mínima (`com.example.faceswap`): elige
  foto fuente y destino, botón Swap. El swap en el móvil es hoy un
  **placeholder** con el puente de integración listo (ver Roadmap).

Los modelos **nunca** van en el repo ni dentro del APK.

## Estructura

```
.
├── app_python/
│   ├── app.py              # get_face, swap, Flask /swap, descarga auto
│   ├── requirements.txt    # insightface, onnxruntime, opencv, numpy, flask
│   └── README_MODELOS.md   # links, pesos y colocación manual
├── app/                    # módulo Android (Kotlin, sin dependencias)
├── .github/workflows/build-apk.yml
├── build.gradle / settings.gradle / gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/
└── README.md
```

## Modelos

| Archivo | Tamaño | Origen |
|---|---|---|
| `models/buffalo_l/*.onnx` | ~275 MB (ZIP) | [InsightFace v0.7](https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_l.zip) |
| `models/inswapper_128.onnx` | ~529 MB | [HuggingFace ezioruan/inswapper_128.onnx](https://huggingface.co/ezioruan/inswapper_128.onnx) |

Instalación automática (PC con Python 3.9+):

```bash
cd app_python
pip install -r requirements.txt
python app.py swap fuente.jpg destino.jpg salida.jpg   # descarga lo que falte
```

Colocación manual si la descarga falla: ver `app_python/README_MODELOS.md`.
En Android irían a `/data/data/com.example.faceswap/files/models/`.

Backend para el móvil (en el PC, mismo WiFi):

```bash
cd app_python
python app.py serve --port 5000   # POST /swap con archivos source+target
```

## Compilar el APK en local

Necesitas JDK 17 y Android SDK con `platforms;android-34`:

```bash
./gradlew assembleDebug
# sale en app/build/outputs/apk/debug/app-debug.apk
```

## Descargar el APK desde GitHub

1. Sube el repo (ver abajo): cada push compila solo.
2. Abre **Actions** → workflow **Build APK (debug)** → ejecución en verde.
3. Descarga **Artifacts → app-debug** (ZIP con el `.apk` dentro).
4. Instálalo en el móvil permitiendo orígenes desconocidos.

## Subir con git push

```bash
git init
git add .
git commit -m "face swap apk"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/face-swap-apk.git
git push -u origin main
```

## Roadmap Android

Sin Chaquopy a propósito: insightface/onnxruntime no tienen wheels
para Android y los modelos pesan ~800 MB, así que Python-en-APK no es
viable para este caso. Vías reales:

1. **Backend Flask** (corto plazo): la app envía las 2 fotos a
   `POST /swap` del PC y muestra el JPG devuelto.
2. **Nativo con ONNX Runtime** (largo plazo): reescribir la inferencia
   en Kotlin con `onnxruntime-android` + OpenCV SDK, descargando los
   `.onnx` a `files/models/` en primera ejecución.

## Versiones fijadas

- Java 17 (Temurin), Gradle 8.9, AGP 8.5.2, Kotlin 1.9.24
- `compileSdk`/`targetSdk` 34, `minSdk` 24 (Android 7.0+, ARM64)
