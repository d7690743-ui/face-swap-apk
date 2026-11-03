# Subir el backend a Hugging Face Spaces (gratis)

El Space necesita estos 4 archivos en su raíz:

| Archivo del Space | Copiar desde este repo |
|---|---|
| `app.py` | `app_python/app.py` |
| `requirements.txt` | `app_python/requirements.txt` |
| `Dockerfile` | `spaces/Dockerfile` |
| `README.md` | `spaces/README.md` (lleva la config YAML de HF) |

## Opción A: web (sin git)

1. Crea cuenta en <https://huggingface.co> y pulsa **New Space**.
2. Nombre (ej: `face-swap-backend`), SDK **Docker**, plantilla **Blank**, Create.
3. En el Space: **Files → Add file → Upload** los 4 archivos de la tabla.
   Para `README.md`, edita el que crea HF y pega el contenido de
   `spaces/README.md` (el bloque `---` de arriba es obligatorio).
4. Espera a que el build termine (pestaña **Logs**, ~5-15 min la primera
   vez: instala dependencias).
5. Copia la URL (`https://TU-USUARIO-face-swap-backend.hf.space`),
   pégala en la app Android y pulsa Swap.

## Opción B: git

```bash
git clone https://huggingface.co/spaces/TU-USUARIO/face-swap-backend
cd face-swap-backend
cp <este-repo>/app_python/app.py .
cp <este-repo>/app_python/requirements.txt .
cp <este-repo>/spaces/Dockerfile .
cp <este-repo>/spaces/README.md README.md
git add .
git commit -m "backend face swap"
git push
```

## Notas

- El primer Swap descarga ~800 MB de modelos: tarda varios minutos.
- En plan gratis el Space duerme sin uso; si un Swap falla tras mucho
  rato, abre la URL en el navegador, espera ~1 min y reintenta.
- Verifica que vive con `GET /health` en el navegador:
  `https://TU-SPACE.hf.space/health`
