#!/usr/bin/env python3
"""Face Swap con InsightFace buffalo (CPU) + inswapper_128.

Los modelos NO están en el repo: se descargan solos a models/ en la
primera ejecución (o colócalos a mano, ver README_MODELOS.md).

Uso local (PC con CPU):
    python app.py swap fuente.jpg destino.jpg salida.jpg
    python app.py serve --port 5000     # backend Flask para la app Android

Variables de entorno:
    MODELS_DIR   carpeta de modelos (defecto: ./models)
    FACE_MODEL   buffalo_l (defecto) o buffalo_s
"""

import argparse
import os
import sys
import urllib.request

APP_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.environ.get("MODELS_DIR", os.path.join(APP_DIR, "models"))
FACE_MODEL = os.environ.get("FACE_MODEL", "buffalo_l")

# Descarga directa del .onnx (la URL del repo en el navegador no sirve para descargar).
INSWAPPER_URL = (
    "https://huggingface.co/ezioruan/inswapper_128.onnx"
    "/resolve/main/inswapper_128.onnx"
)
INSWAPPER_MIN_BYTES = 100 * 1024 * 1024  # ~529 MB reales; menos = descarga corrupta


def download(url, dest):
    """Descarga un archivo con progreso. Solo CPU/red estándar."""
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    tmp = dest + ".part"

    def progress(blocks, block_size, total):
        done = blocks * block_size
        pct = (100.0 * done / total) if total > 0 else 0
        print(f"\r  {os.path.basename(dest)}: {pct:5.1f}%", end="", flush=True)

    print(f"Descargando {url}")
    urllib.request.urlretrieve(url, tmp, reporthook=progress)
    print()
    os.replace(tmp, dest)
    return dest


def ensure_inswapper():
    """Devuelve la ruta de inswapper_128.onnx, descargándolo si falta."""
    path = os.path.join(MODELS_DIR, "inswapper_128.onnx")
    if os.path.isfile(path) and os.path.getsize(path) >= INSWAPPER_MIN_BYTES:
        return path
    if os.path.isfile(path):
        print("inswapper_128.onnx incompleto, se vuelve a descargar.")
        os.remove(path)
    try:
        download(INSWAPPER_URL, path)
    except Exception as e:
        raise RuntimeError(
            f"No se pudo descargar inswapper_128.onnx: {e}\n"
            f"Descárgalo a mano desde https://huggingface.co/ezioruan/inswapper_128.onnx "
            f"y guárdalo como {path}"
        )
    return path


_face_app = None


def get_face_app():
    """FaceAnalysis buffalo en CPU. InsightFace descarga el pack solo si falta."""
    global _face_app
    if _face_app is None:
        from insightface.app import FaceAnalysis

        _face_app = FaceAnalysis(
            name=FACE_MODEL, root=MODELS_DIR, providers=["CPUExecutionProvider"]
        )
        _face_app.prepare(ctx_id=0, det_size=(640, 640))
    return _face_app


def get_face(img_bgr):
    """Devuelve la cara más grande de la imagen (BGR) o None."""
    faces = get_face_app().get(img_bgr)
    if not faces:
        return None

    def area(f):
        x1, y1, x2, y2 = f.bbox
        return (x2 - x1) * (y2 - y1)

    return max(faces, key=area)


_swapper = None


def get_swapper():
    """Modelo inswapper en CPU (descarga el .onnx si falta)."""
    global _swapper
    if _swapper is None:
        import insightface

        _swapper = insightface.model_zoo.get_model(
            ensure_inswapper(), providers=["CPUExecutionProvider"]
        )
    return _swapper


def swap(source_bgr, target_bgr):
    """Pone la cara de source en target. Imágenes BGR (cv2)."""
    src_face = get_face(source_bgr)
    if src_face is None:
        raise ValueError("No se detectó cara en la foto fuente")
    tgt_face = get_face(target_bgr)
    if tgt_face is None:
        raise ValueError("No se detectó cara en la foto destino")
    return get_swapper().get(target_bgr, tgt_face, src_face, paste_back=True)


def create_app():
    """Backend Flask para que la app Android pida swaps por red."""
    from flask import Flask, jsonify, request, Response

    app = Flask(__name__)

    @app.get("/health")
    def health():
        return jsonify({"status": "ok", "model": FACE_MODEL})

    @app.post("/swap")
    def swap_endpoint():
        if "source" not in request.files or "target" not in request.files:
            return jsonify({"error": "Envía 'source' y 'target' como archivos"}), 400
        try:
            import cv2
            import numpy as np
            src = cv2.imdecode(
                np.frombuffer(request.files["source"].read(), np.uint8),
                cv2.IMREAD_COLOR,
            )
            tgt = cv2.imdecode(
                np.frombuffer(request.files["target"].read(), np.uint8),
                cv2.IMREAD_COLOR,
            )
            if src is None or tgt is None:
                return jsonify({"error": "Imagen no válida"}), 400
            out = swap(src, tgt)
            _, buf = cv2.imencode(".jpg", out)
            return Response(buf.tobytes(), mimetype="image/jpeg")
        except ValueError as e:
            return jsonify({"error": str(e)}), 422
        except Exception as e:  # noqa: BLE001 - error controlado al cliente
            return jsonify({"error": f"Fallo interno: {e}"}), 500

    return app


def main(argv=None):
    parser = argparse.ArgumentParser(description="Face Swap buffalo + inswapper (CPU)")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_swap = sub.add_parser("swap", help="Swap local: fuente destino salida")
    p_swap.add_argument("source")
    p_swap.add_argument("target")
    p_swap.add_argument("output")

    p_serve = sub.add_parser("serve", help="Backend Flask para la app Android")
    p_serve.add_argument("--host", default="0.0.0.0")
    p_serve.add_argument("--port", type=int, default=5000)

    args = parser.parse_args(argv)
    if args.cmd == "swap":
        import cv2

        src = cv2.imread(args.source)
        tgt = cv2.imread(args.target)
        if src is None:
            print(f"No se pudo leer {args.source}", file=sys.stderr)
            return 1
        if tgt is None:
            print(f"No se pudo leer {args.target}", file=sys.stderr)
            return 1
        try:
            out = swap(src, tgt)
        except (ValueError, RuntimeError) as e:
            print(f"Error: {e}", file=sys.stderr)
            return 1
        cv2.imwrite(args.output, out)
        print(f"Guardado: {args.output}")
        return 0
    if args.cmd == "serve":
        create_app().run(host=args.host, port=args.port)
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
