# Publicación en la nube

Propuesta sin costo: los tres servicios en **Render** (plan gratis, con Docker) y una base **MySQL gratis en Aiven**.
Cada servicio usa su propia base de datos dentro del mismo servidor MySQL: `productos_db`, `pedidos_db` y `broker_db`.

> En el plan gratis de Render, un servicio se apaga después de unos 15 minutos sin uso. La primera petición lo despierta y puede tardar cerca de un minuto. Antes de la demostración, abra el `/health` de los tres servicios.

## 1. Subir el proyecto a GitHub

Render construye los servicios desde un repositorio. Suba la carpeta `Proyecto final` completa. Los archivos `.env` no se suben porque están en `.gitignore`.

## 2. Crear la base MySQL en Aiven

1. Cree una cuenta en aiven.io y un servicio **MySQL** con el plan **Free**.
2. En la pestaña **Overview** copie: *Host*, *Port*, *User* (`avnadmin`) y *Password*.
3. Conéctese con MySQL Workbench usando esos datos, con SSL en modo `Require`, y ejecute los tres scripts de la carpeta `sql/` en orden. Así quedan las tablas y los productos de ejemplo.

Si no ejecuta los scripts, los servicios crean sus tablas al arrancar, pero sin datos de ejemplo.

## 3. Crear los servicios en Render

1. En render.com elija **New → Blueprint** y seleccione el repositorio. Render lee `render.yaml` y propone tres servicios: `ecommerce-productos`, `ecommerce-pedidos` y `ecommerce-broker`.
2. Llene las variables que pide:

| Variable | Valor |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` | Datos de Aiven (paso 2) |
| `SERVICE_KEY`, `PRODUCTOS_SERVICE_KEY`, `PEDIDOS_SERVICE_KEY` | La **misma** clave interna en todas. Use una cadena larga y aleatoria. |
| `ADMIN_PASSWORD` | Contraseña del usuario `admin` del broker. Use una distinta a `Admin123*`, que es la de ejemplo y aparece en el repositorio público. |
| `PRODUCTOS_URL` (en pedidos y en broker) | `https://ecommerce-productos.onrender.com` |
| `PEDIDOS_URL` (en broker) | `https://ecommerce-pedidos.onrender.com` |

`JWT_SECRET` lo genera Render solo.

3. Si Render asigna otra dirección porque el nombre ya está ocupado, copie la dirección real del panel de cada servicio y corrija `PRODUCTOS_URL` y `PEDIDOS_URL`.

## 4. Probar

1. Abra `https://ecommerce-broker.onrender.com/health`, `https://ecommerce-productos.onrender.com/health` y `https://ecommerce-pedidos.onrender.com/health` hasta que los tres respondan.
2. En Postman, edite el entorno **Ecommerce - Nube**: `broker_url` = dirección del broker y `password` = su `ADMIN_PASSWORD`.
3. Ejecute las carpetas 01 a 05 de la colección con ese entorno.

Swagger en la nube:

- Broker: `https://ecommerce-broker.onrender.com/docs`
- Productos: `https://ecommerce-productos.onrender.com/api-docs`
- Pedidos: `https://ecommerce-pedidos.onrender.com/swagger-ui.html`

## Otra opción

**Railway** también despliega los tres Dockerfiles y ofrece MySQL en el mismo proyecto. Sus servicios no se apagan, pero cobra cuando se termina el crédito de prueba. Las variables son las mismas de esta guía.
