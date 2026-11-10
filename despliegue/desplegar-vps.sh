#!/usr/bin/env bash
# Despliega MySQL y los tres servicios con Docker en un servidor Linux y los publica
# con túneles de Cloudflare (HTTPS, sin abrir puertos).
#
# Uso, como root y desde la carpeta del proyecto:
#   bash despliegue/desplegar-vps.sh
#
# Se puede ejecutar de nuevo para publicar cambios: reconstruye las imágenes y
# reemplaza los contenedores, pero conserva la base de datos, las claves y las URLs.
set -euo pipefail

BASE=/opt/ecommerce-arquitectura
SRC="$(cd "$(dirname "$0")/.." && pwd)"
ENV_DIR="$BASE/env"
RED=ecommerce-red
VERSION="$(date +%Y%m%d%H%M)"
CLOUDFLARED="$BASE/bin/cloudflared"

paso() { printf '\n== %s\n' "$*"; }
secreto() { openssl rand -hex "$1"; }

mkdir -p "$ENV_DIR" "$BASE/bin"
chmod 700 "$ENV_DIR"

paso "Variables de entorno"
if [ ! -f "$ENV_DIR/mysql.env" ]; then
  CLAVE_INTERNA="$(secreto 24)"
  CLAVE_MYSQL="$(secreto 16)"
  SECRETO_JWT="$(secreto 32)"
  CLAVE_ADMIN="Umes-$(secreto 4)"

  printf 'MYSQL_ROOT_PASSWORD=%s\n' "$CLAVE_MYSQL" > "$ENV_DIR/mysql.env"

  cat > "$ENV_DIR/productos.env" <<EOF
PORT=3001
SERVICE_KEY=$CLAVE_INTERNA
DB_HOST=ecommerce-mysql
DB_PORT=3306
DB_NAME=productos_db
DB_USER=root
DB_PASSWORD=$CLAVE_MYSQL
DB_SYNC=true
EOF

  cat > "$ENV_DIR/pedidos.env" <<EOF
PORT=8082
SERVICE_KEY=$CLAVE_INTERNA
DB_HOST=ecommerce-mysql
DB_PORT=3306
DB_NAME=pedidos_db
DB_USER=root
DB_PASSWORD=$CLAVE_MYSQL
PRODUCTOS_URL=http://ecommerce-productos:3001
PRODUCTOS_TIMEOUT_MS=3000
PRODUCTOS_SERVICE_KEY=$CLAVE_INTERNA
EOF

  cat > "$ENV_DIR/broker.env" <<EOF
PORT=8000
DB_HOST=ecommerce-mysql
DB_PORT=3306
DB_NAME=broker_db
DB_USER=root
DB_PASSWORD=$CLAVE_MYSQL
JWT_SECRET=$SECRETO_JWT
JWT_EXPIRA_MIN=60
ADMIN_USUARIO=admin
ADMIN_PASSWORD=$CLAVE_ADMIN
PRODUCTOS_URL=http://ecommerce-productos:3001
PEDIDOS_URL=http://ecommerce-pedidos:8082
PRODUCTOS_SERVICE_KEY=$CLAVE_INTERNA
PEDIDOS_SERVICE_KEY=$CLAVE_INTERNA
TIMEOUT_S=10
EOF
  chmod 600 "$ENV_DIR"/*.env
  echo "Creadas en $ENV_DIR (la contraseña de admin está en broker.env)"
else
  echo "Se conservan las de $ENV_DIR"
fi

paso "Red y base de datos"
docker network inspect "$RED" >/dev/null 2>&1 || docker network create "$RED" >/dev/null
if ! docker container inspect ecommerce-mysql >/dev/null 2>&1; then
  # Los scripts de sql/ se ejecutan solo la primera vez, con el volumen vacío.
  docker run -d --name ecommerce-mysql --network "$RED" --restart unless-stopped \
    --env-file "$ENV_DIR/mysql.env" \
    -v ecommerce-mysql:/var/lib/mysql \
    -v "$SRC/sql:/docker-entrypoint-initdb.d:ro" \
    --memory 768m mysql:8.4 >/dev/null
fi
for intento in $(seq 1 90); do
  if docker exec ecommerce-mysql sh -c 'mysql -h127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1" >/dev/null 2>&1'; then
    echo "MySQL listo"
    break
  fi
  [ "$intento" -eq 90 ] && { echo "MySQL no respondió" >&2; exit 1; }
  sleep 2
done

paso "Imágenes (versión $VERSION)"
for servicio in productos pedidos broker; do
  docker build -q -t "ecommerce-$servicio:$VERSION" -t "ecommerce-$servicio:actual" "$SRC/$servicio"
done

paso "Contenedores"
lanzar() {
  local servicio=$1 interno=$2 local_host=$3 memoria=$4
  docker rm -f "ecommerce-$servicio" >/dev/null 2>&1 || true
  docker run -d --name "ecommerce-$servicio" --network "$RED" --restart unless-stopped \
    --env-file "$ENV_DIR/$servicio.env" \
    -p "127.0.0.1:$local_host:$interno" \
    --memory "$memoria" "ecommerce-$servicio:actual" >/dev/null
}
lanzar productos 3001 13001 256m
lanzar pedidos 8082 18082 640m
lanzar broker 8000 18000 256m

for par in productos:13001 pedidos:18082 broker:18000; do
  servicio=${par%%:*}; puerto=${par##*:}
  for intento in $(seq 1 90); do
    if curl -fsS "http://127.0.0.1:$puerto/health" >/dev/null 2>&1; then
      echo "$servicio responde en 127.0.0.1:$puerto"
      break
    fi
    [ "$intento" -eq 90 ] && { echo "$servicio no respondió; revise: docker logs ecommerce-$servicio" >&2; exit 1; }
    sleep 2
  done
done

paso "Túneles públicos"
if [ ! -x "$CLOUDFLARED" ]; then
  curl -fsSL -o "$CLOUDFLARED" https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64
  chmod +x "$CLOUDFLARED"
fi
for par in broker:18000 productos:13001 pedidos:18082; do
  servicio=${par%%:*}; puerto=${par##*:}
  cat > "/etc/systemd/system/ecommerce-tunel-$servicio.service" <<EOF
[Unit]
Description=Ecommerce Arquitectura - acceso público a $servicio
After=network-online.target docker.service
Wants=network-online.target

[Service]
ExecStart=$CLOUDFLARED tunnel --no-autoupdate --protocol http2 --url http://127.0.0.1:$puerto
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
done
systemctl daemon-reload
for servicio in broker productos pedidos; do
  # Solo se inicia si no está activo, para no cambiar la URL pública.
  systemctl enable "ecommerce-tunel-$servicio.service" >/dev/null 2>&1
  systemctl is-active --quiet "ecommerce-tunel-$servicio.service" || systemctl start "ecommerce-tunel-$servicio.service"
done

paso "Direcciones públicas"
for intento in $(seq 1 30); do
  faltan=0
  for servicio in broker productos pedidos; do
    journalctl -u "ecommerce-tunel-$servicio" --no-pager -o cat | grep -qE 'https://[a-z0-9-]+\.trycloudflare\.com' || faltan=1
  done
  [ "$faltan" -eq 0 ] && break
  sleep 2
done
bash "$SRC/despliegue/urls.sh"
