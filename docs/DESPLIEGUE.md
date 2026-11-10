# Publicación en la nube

Los tres servicios y la base de datos corren en un servidor Linux (VPS) con Docker. Cada servicio se publica con un túnel de Cloudflare, que entrega una dirección `https://….trycloudflare.com` sin abrir puertos en el firewall.

```
Internet ─ HTTPS ─► Túnel de Cloudflare ─► 127.0.0.1:18000  contenedor broker
                                       ─► 127.0.0.1:13001  contenedor productos
                                       ─► 127.0.0.1:18082  contenedor pedidos
                                           127.0.0.1:13306  contenedor MySQL 8.4 (sin acceso público)
```

- Los contenedores usan la red del servidor (`--network host`) y escuchan solo en `127.0.0.1`. Así funcionan aunque Docker tenga desactivadas sus redes propias.
- Cada servicio usa su propia base de datos dentro del contenedor de MySQL: `productos_db`, `pedidos_db` y `broker_db`.
- El acceso público es solo por los túneles. Productos y pedidos tienen dirección pública para consultar su Swagger, pero sus rutas `/api` exigen la clave interna. Postman siempre usa la dirección del broker.

## Requisitos del servidor

Linux con Docker, `openssl`, `curl` y `systemd`, y acceso como root.

## Desplegar

```bash
git clone <repositorio> /opt/ecommerce-arquitectura/src
cd /opt/ecommerce-arquitectura/src
bash despliegue/desplegar-vps.sh
```

El script:

1. Genera claves aleatorias la primera vez y las guarda en `/opt/ecommerce-arquitectura/env/` (permisos 600). La contraseña del usuario `admin` del broker queda en `broker.env`.
2. Crea el contenedor `ecommerce-mysql`. La primera vez ejecuta los scripts de `sql/`.
3. Construye las imágenes de los tres servicios con sus `Dockerfile` y crea los contenedores con reinicio automático.
4. Espera a que `/health` responda en los tres.
5. Crea los servicios `ecommerce-tunel-broker`, `ecommerce-tunel-productos` y `ecommerce-tunel-pedidos` y muestra las direcciones públicas.

Para publicar cambios: `git pull` y volver a ejecutar el script. Se conservan los datos, las claves y las direcciones.

## Consultar las direcciones

```bash
bash despliegue/urls.sh
```

La dirección de un túnel cambia solo si ese túnel se reinicia, por ejemplo al reiniciar el servidor. En ese caso, vuelva a consultarla y actualice `broker_url` en el entorno **Ecommerce - Nube** de Postman.

## Comandos útiles

```bash
docker ps --filter name=ecommerce            # estado de los contenedores
docker logs --tail 50 ecommerce-pedidos      # registro de un servicio
systemctl status ecommerce-tunel-broker      # estado de un túnel
```
