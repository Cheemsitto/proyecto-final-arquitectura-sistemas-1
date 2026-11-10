FROM python:3.13-slim

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

ENV PORT=8000
EXPOSE 8000

ENV HOST=0.0.0.0
CMD ["sh", "-c", "uvicorn app.main:app --host ${HOST} --port ${PORT}"]
