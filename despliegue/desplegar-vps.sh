#!/usr/bin/env bash
# Despliega MySQL y los tres servicios con Docker en un servidor Linux y los publica
# con túneles de Cloudflare (HTTPS, sin abrir puertos).
#
# Los contenedores usan la red del servidor (--network host) y escuchan solo en
# 127.0.0.1, así funcionan aunque Docker no tenga redes propias habilitadas.
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
VERSION="$(date +%Y%m%d%H%M)"
CLOUDFLARED="$BASE/bin/cloudflared"

# Puertos locales del servidor
PUERTO_MYSQL=13306
PUERTO_PRODUCTOS=13001
PUERTO_PEDIDOS=18082
PUERTO_BROKER=18000

paso() { printf '\n== %s\n' "$*"; }
secreto() { openssl rand -hex "$1"; }

esperar() {
  local nombre=$1 comando=$2
  for intento in $(seq 1 90); do
    if eval "$comando" >/dev/null 2>&1; then
      echo "$nombre listo"
      return 0
    fi
    sleep 2
  done
  echo "$nombre no respondió; revise: docker logs ecommerce-$nombre" >&2
  exit 1
}

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
HOST=127.0.0.1
PORT=$PUERTO_PRODUCTOS
SERVICE_KEY=$CLAVE_INTERNA
DB_HOST=127.0.0.1
DB_PORT=$PUERTO_MYSQL
DB_NAME=productos_db
DB_USER=root
DB_PASSWORD=$CLAVE_MYSQL
DB_SYNC=true
EOF

  cat > "$ENV_DIR/pedidos.env" <<EOF
SERVER_ADDRESS=127.0.0.1
PORT=$PUERTO_PEDIDOS
SERVICE_KEY=$CLAVE_INTERNA
DB_HOST=127.0.0.1
DB_PORT=$PUERTO_MYSQL
DB_NAME=pedidos_db
DB_USER=root
DB_PASSWORD=$CLAVE_MYSQL
PRODUCTOS_URL=http://127.0.0.1:$PUERTO_PRODUCTOS
PRODUCTOS_TIMEOUT_MS=3000
PRODUCTOS_SERVICE_KEY=$CLAVE_INTERNA
EOF

  cat > "$ENV_DIR/broker.env" <<EOF
HOST=127.0.0.1
PORT=$PUERTO_BROKER
DB_HOST=127.0.0.1
DB_PORT=$PUERTO_MYSQL
DB_NAME=broker_db
DB_USER=root
DB_PASSWORD=$CLAVE_MYSQL
JWT_SECRET=$SECRETO_JWT
JWT_EXPIRA_MIN=60
ADMIN_USUARIO=admin
ADMIN_PASSWORD=$CLAVE_ADMIN
PRODUCTOS_URL=http://127.0.0.1:$PUERTO_PRODUCTOS
PEDIDOS_URL=http://127.0.0.1:$PUERTO_PEDIDOS
PRODUCTOS_SERVICE_KEY=$CLAVE_INTERNA
PEDIDOS_SERVICE_KEY=$CLAVE_INTERNA
TIMEOUT_S=10
EOF
  chmod 600 "$ENV_DIR"/*.env
  echo "Creadas en $ENV_DIR (la contraseña de admin está en broker.env)"
else
  echo "Se conservan las de $ENV_DIR"
fi

paso "Base de datos"
if ! docker container inspect ecommerce-mysql >/dev/null 2>&1; then
  # Los scripts de sql/ se ejecutan solo la primera vez, con el volumen vacío.
  docker run -d --name ecommerce-mysql --network host --restart unless-stopped \
    --env-file "$ENV_DIR/mysql.env" \
    -v ecommerce-mysql:/var/lib/mysql \
    -v "$SRC/sql:/docker-entrypoint-initdb.d:ro" \
    --memory 768m mysql:8.4 \
    --port="$PUERTO_MYSQL" --bind-address=127.0.0.1 --mysqlx=OFF >/dev/null
fi
esperar mysql "docker exec ecommerce-mysql sh -c 'mysql -h127.0.0.1 -P$PUERTO_MYSQL -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -e \"SELECT 1\"'"

paso "Imágenes (versión $VERSION)"
for servicio in productos pedidos broker; do
  docker build -q --network host -t "ecommerce-$servicio:$VERSION" -t "ecommerce-$servicio:actual" "$SRC/$servicio"
done

paso "Contenedores"
lanzar() {
  local servicio=$1 memoria=$2
  docker rm -f "ecommerce-$servicio" >/dev/null 2>&1 || true
  docker run -d --name "ecommerce-$servicio" --network host --restart unless-stopped \
    --env-file "$ENV_DIR/$servicio.env" \
    --memory "$memoria" "ecommerce-$servicio:actual" >/dev/null
}
lanzar productos 256m
lanzar pedidos 640m
lanzar broker 256m
esperar productos "curl -fsS http://127.0.0.1:$PUERTO_PRODUCTOS/health"
esperar pedidos "curl -fsS http://127.0.0.1:$PUERTO_PEDIDOS/health"
esperar broker "curl -fsS http://127.0.0.1:$PUERTO_BROKER/health"

paso "Túneles públicos"
if [ ! -x "$CLOUDFLARED" ]; then
  curl -fsSL -o "$CLOUDFLARED" https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64
  chmod +x "$CLOUDFLARED"
fi
for par in "broker:$PUERTO_BROKER" "productos:$PUERTO_PRODUCTOS" "pedidos:$PUERTO_PEDIDOS"; do
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
