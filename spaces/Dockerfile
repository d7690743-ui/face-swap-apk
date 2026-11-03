# Backend Face Swap para Hugging Face Spaces (SDK Docker).
# Spaces exige escuchar en el puerto 7860.
FROM python:3.11-slim

WORKDIR /app

# Libs del sistema que piden opencv-python y onnxruntime.
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libglib2.0-0 libgomp1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

# Los modelos (~800 MB) se descargan solos en el primer uso.
ENV MODELS_DIR=/app/models
ENV FACE_MODEL=buffalo_l

EXPOSE 7860

CMD ["python", "app.py", "serve", "--host", "0.0.0.0", "--port", "7860"]
