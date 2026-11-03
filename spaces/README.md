---
title: Face Swap Backend
emoji: 🔄
colorFrom: blue
colorTo: purple
sdk: docker
app_port: 7860
pinned: false
---

# Face Swap Backend (buffalo + inswapper, CPU)

Backend Flask para la app Android Face Swap. Endpoints:

- `GET /health` → `{"status":"ok","model":"buffalo_l"}`
- `POST /swap` (multipart `source` + `target`) → JPG con el swap

Los modelos (~800 MB) se descargan solos a `./models` en el primer uso.
En plan gratis el Space se duerme sin uso y tarda ~1 min en despertar:
si el primer Swap falla, espera y reintenta.
