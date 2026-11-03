# Modelos (descarga en runtime, no van en el repo ni en el APK)

`app.py` descarga solo lo que falta a `app_python/models/` en la primera
ejecución. Si la descarga automática falla, colócalos a mano:

| Archivo | Tamaño real | Origen |
|---|---|---|
| `models/buffalo_l/*.onnx` (pack de detección, se extrae de `buffalo_l.zip`) | ~275 MB el ZIP | [InsightFace v0.7 buffalo_l.zip](https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_l.zip) |
| `models/inswapper_128.onnx` | ~529 MB | [ezioruan/inswapper_128.onnx en HuggingFace](https://huggingface.co/ezioruan/inswapper_128.onnx) (botón Download del archivo) |

Total aproximado en disco: **~800 MB**.

## Colocación manual

```
app_python/models/
├── buffalo_l/            # contenido de buffalo_l.zip extraído aquí
│   ├── 1k3d68.onnx
│   ├── 2d106det.onnx
│   ├── det_10g.onnx
│   ├── genderage.onnx
│   └── w600k_r50.onnx
└── inswapper_128.onnx    # ~529 MB, un solo archivo
```

- El pack alternativo `buffalo_s` (más ligero) se usa con
  `FACE_MODEL=buffalo_s python app.py ...`; se descarga igual, solo.
- Carpeta alternativa: `MODELS_DIR=/otra/ruta python app.py ...`

## En Android (runtime, no build)

Los modelos nunca se meten en el APK. La app los descargaría en la
primera ejecución a su carpeta privada:

```
/data/data/com.example.faceswap/files/models/
├── buffalo_l/
└── inswapper_128.onnx
```

Ver el puente previsto en `MainActivity.kt` (`FaceSwapEngine.modelsDir`)
y el roadmap en el README principal.
