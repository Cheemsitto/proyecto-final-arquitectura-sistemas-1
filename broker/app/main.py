import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app import proxy
from app.config import config
from app.database import inicializar_bd
from app.routers import auth, pedidos, productos

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

DESCRIPCION = """
Punto único de entrada al ecommerce. No tiene lógica de negocio:

1. **POST /auth/login** entrega un token JWT.
2. Las rutas **/api/productos** y **/api/pedidos** exigen `Authorization: Bearer <token>`.
3. Si el token es válido, reenvía la petición al servicio que corresponde y agrega la
   clave interna `X-Service-Key`. Sin token válido responde **401** y no reenvía nada.

En Swagger, use el botón **Authorize** y pegue el token.
"""


@asynccontextmanager
async def ciclo_de_vida(_app: FastAPI):
    config.validar()
    inicializar_bd()
    yield
    await proxy.cerrar_cliente()


app = FastAPI(
    title="Broker - Ecommerce",
    description=DESCRIPCION,
    version="1.0.0",
    lifespan=ciclo_de_vida,
)


@app.exception_handler(StarletteHTTPException)
async def error_http(_request: Request, error: StarletteHTTPException) -> JSONResponse:
    return JSONResponse(
        status_code=error.status_code,
        content={"error": error.detail},
        headers=getattr(error, "headers", None),
    )


@app.exception_handler(RequestValidationError)
async def error_validacion(_request: Request, error: RequestValidationError) -> JSONResponse:
    detalles = []
    for problema in error.errors():
        campo = ".".join(str(parte) for parte in problema["loc"] if parte not in ("body", "path"))
        detalles.append(f"{campo}: {problema['msg']}" if campo else problema["msg"])
    return JSONResponse(status_code=400, content={"error": "Datos inválidos", "detalles": detalles})


@app.get("/health", tags=["Estado"], summary="Estado del broker")
def estado() -> dict:
    return {"servicio": "broker", "estado": "ok"}


app.include_router(auth.router)
app.include_router(productos.router)
app.include_router(pedidos.router)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=config.puerto)
