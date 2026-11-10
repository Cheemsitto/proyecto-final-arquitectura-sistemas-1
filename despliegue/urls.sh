#!/usr/bin/env bash
# Muestra la dirección pública actual de cada servicio.
# Cambia solo si el túnel se reinicia (por ejemplo, al reiniciar el servidor).
for servicio in broker productos pedidos; do
  url=$(journalctl -u "ecommerce-tunel-$servicio" --no-pager -o cat 2>/dev/null \
    | grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' | tail -1)
  printf '%-10s %s\n' "$servicio" "${url:-(todavía sin dirección)}"
done
