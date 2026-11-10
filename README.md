# Ecommerce con broker - Proyecto final de Arquitectura de Sistemas I

Tres proyectos independientes que funcionan juntos. Toda petición del cliente (Postman) entra por el broker.

```
Postman ──► Broker :8000 ──(X-Service-Key)──► Productos :3001 ──► productos_db
            (token JWT)  └─(X-Service-Key)──► Pedidos   :8082 ──► pedidos_db
                                                  │
                                                  └─(X-Service-Key)──► Productos
broker_db ◄── Broker (usuarios)
```

| Carpeta | Servicio | Tecnología | Arquitectura | Puerto | Base de datos | ORM |
| --- | --- | --- | --- | --- | --- | --- |
| `broker/` | Broker | Python, FastAPI | Proxy sin lógica de negocio | 8000 | `broker_db` | SQLAlchemy |
| `productos/` | Productos | Node.js, Express | MVC | 3001 | `productos_db` | Sequelize |
| `pedidos/` | Pedidos | Java, Spring Boot | Capas | 8082 | `pedidos_db` | Spring Data JPA (Hibernate) |

Otras carpetas: `sql/` (scripts de las tres bases), `postman/` (colección y entornos), `despliegue/` (scripts para el servidor) y `docs/` (publicación en la nube).

## Seguridad

- **Cliente → broker:** `POST /auth/login` entrega un token JWT. Las rutas `/api/productos` y `/api/pedidos` exigen `Authorization: Bearer <token>`. Sin token válido, el broker responde 401 y no reenvía la petición.
- **Entre servicios:** el broker y pedidos agregan la cabecera `X-Service-Key`. Productos y pedidos rechazan con 401 cualquier petición que no la traiga, así que nadie puede llamarlos directamente sin pasar por el broker.

## Requisitos

- MySQL 8
- Node.js 20 o superior
- JDK 17 o superior (IntelliJ ya trae uno en `C:\Users\<usuario>\.jdks`)
- Python 3.12 o superior

## Cómo correrlo en local

### 1. Bases de datos

Ejecute en MySQL Workbench los scripts de `sql/` en orden: `01_productos_db.sql`, `02_pedidos_db.sql`, `03_broker_db.sql`.

### 2. Variables de entorno

Cada servicio lee un archivo `.env` en su carpeta. Ya vienen creados; solo falta escribir la contraseña de MySQL en `DB_PASSWORD` de los tres.
La clave interna (`SERVICE_KEY`, `PRODUCTOS_SERVICE_KEY`, `PEDIDOS_SERVICE_KEY`) debe ser la misma en los tres archivos. Si los crea de nuevo, copie `.env.example` a `.env`.

### 3. Iniciar los servicios (una terminal para cada uno)

Productos:

```bash
cd productos
npm install
npm start
```

Pedidos (o en IntelliJ: Maven → Plugins → spring-boot → `spring-boot:run`):

```bash
cd pedidos
./mvnw spring-boot:run
```

Broker:

```bash
cd broker
python -m venv .venv
.venv\Scripts\pip install -r requirements-dev.txt
.venv\Scripts\python -m app.main
```

Al iniciar, el broker crea el usuario `admin` con la contraseña de `ADMIN_PASSWORD`.

### 4. Probar con Postman

1. Importe `postman/Ecommerce.postman_collection.json` y `postman/Local.postman_environment.json`.
2. Seleccione el entorno **Ecommerce - Local**.
3. Ejecute las carpetas 01 a 05 en orden con el Collection Runner. El login guarda el token y lo usan las demás peticiones.

| Carpeta | Qué demuestra |
| --- | --- |
| 01 Autenticación | Login correcto e incorrecto |
| 02 Seguridad del broker | Rechazo sin token o con token inválido |
| 03 Productos | CRUD y validaciones de precio y existencia mayores a 0 |
| 04 Pedidos | Pedido de 2 unidades sobre 5 (queda en 3), existencia insuficiente, producto inexistente |
| 05 Eliminar producto | Eliminación |
| 06 Fallas entre servicios | Productos no responde a tiempo, rechaza la autenticación o está apagado |

La carpeta 06 necesita preparar el escenario y reiniciar pedidos; cada petición explica cómo:

| Escenario | Cambio en `pedidos/.env` | Respuesta |
| --- | --- | --- |
| Productos no responde a tiempo | `PRODUCTOS_URL=http://10.255.255.1:3001` | 504 después de 3 s |
| Productos rechaza la autenticación | `PRODUCTOS_SERVICE_KEY` con otro valor | 502 |
| Productos apagado | Ninguno; detenga el servicio de productos | 503 |

## Documentación Swagger

| Servicio | URL |
| --- | --- |
| Broker | http://localhost:8000/docs |
| Productos | http://localhost:3001/api-docs |
| Pedidos | http://localhost:8082/swagger-ui.html |

En el Swagger del broker use el botón **Authorize** con el token del login. En los de productos y pedidos, **Authorize** pide la clave interna.

## Pruebas unitarias con mocks

```bash
cd productos && npm test
```

```bash
cd pedidos && ./mvnw test
```

```bash
cd broker && .venv\Scripts\python -m pytest
```

| Servicio | Pruebas | Qué se simula |
| --- | --- | --- |
| Productos (Jest) | 41 | Modelo Sequelize y transacción |
| Pedidos (JUnit + Mockito) | 33 | Repositorio JPA, cliente HTTP, servicio y servidor de productos |
| Broker (pytest) | 36 | Sesión de base de datos, reenvío HTTP y cliente httpx |

## Publicación en la nube

Los servicios corren con Docker en un servidor Linux y se publican con túneles de Cloudflare. Vea [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).

| Servicio | Dirección pública | Swagger |
| --- | --- | --- |
| Broker | https://manuals-zip-resident-produce.trycloudflare.com | `/docs` |
| Productos | https://manhattan-dot-montana-let.trycloudflare.com | `/api-docs` |
| Pedidos | https://mambo-territory-convergence-recipe.trycloudflare.com | `/swagger-ui.html` |

Para probar en la nube, use en Postman el entorno **Ecommerce - Nube** con la dirección del broker. Si el servidor se reinicia, las direcciones cambian; se consultan con `bash despliegue/urls.sh`.
