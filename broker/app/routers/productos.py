from typing import Any

from fastapi import APIRouter, Body, Depends

from app import proxy
from app.dependencias import usuario_actual
from app.esquemas import RESPUESTAS_PROTEGIDAS

router = APIRouter(prefix="/api/productos", tags=["Productos"], responses=RESPUESTAS_PROTEGIDAS)

SERVICIO = "productos"
EJEMPLOS_PRODUCTO = {
    "valido": {
        "summary": "Producto válido",
        "value": {"nombre": "Teclado mecánico", "descripcion": "Switches rojos", "precio": 450.0, "existencia": 5},
    },
    "precio_invalido": {
        "summary": "Precio en 0 (lo rechaza productos)",
        "value": {"nombre": "Teclado", "precio": 0, "existencia": 5},
    },
}


@router.get("", summary="Listar productos")
async def listar(usuario: str = Depends(usuario_actual)):
    return await proxy.reenviar(SERVICIO, "GET", "/api/productos", usuario)


@router.get("/{producto_id}", summary="Consultar producto")
async def obtener(producto_id: int, usuario: str = Depends(usuario_actual)):
    return await proxy.reenviar(SERVICIO, "GET", f"/api/productos/{producto_id}", usuario)


@router.post("", status_code=201, summary="Crear producto")
async def crear(
    cuerpo: dict[str, Any] = Body(openapi_examples=EJEMPLOS_PRODUCTO),
    usuario: str = Depends(usuario_actual),
):
    return await proxy.reenviar(SERVICIO, "POST", "/api/productos", usuario, cuerpo=cuerpo)


@router.put("/{producto_id}", summary="Actualizar producto")
async def actualizar(
    producto_id: int,
    cuerpo: dict[str, Any] = Body(openapi_examples=EJEMPLOS_PRODUCTO),
    usuario: str = Depends(usuario_actual),
):
    return await proxy.reenviar(SERVICIO, "PUT", f"/api/productos/{producto_id}", usuario, cuerpo=cuerpo)


@router.delete("/{producto_id}", summary="Eliminar producto")
async def eliminar(producto_id: int, usuario: str = Depends(usuario_actual)):
    return await proxy.reenviar(SERVICIO, "DELETE", f"/api/productos/{producto_id}", usuario)
