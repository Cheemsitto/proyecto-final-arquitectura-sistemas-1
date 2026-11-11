from typing import Any

from fastapi import APIRouter, Body, Depends

from app import proxy
from app.dependencias import usuario_actual
from app.esquemas import RESPUESTAS_PROTEGIDAS

router = APIRouter(prefix="/api/pedidos", tags=["Pedidos"], responses=RESPUESTAS_PROTEGIDAS)

SERVICIO = "pedidos"
EJEMPLOS_PEDIDO = {
    "valido": {"summary": "Pedido de 2 unidades", "value": {"productoId": 1, "cantidad": 2}},
    "sin_stock": {"summary": "Más unidades de las que hay", "value": {"productoId": 1, "cantidad": 1000}},
}


@router.get("", summary="Listar pedidos")
async def listar(usuario: str = Depends(usuario_actual)):
    return await proxy.reenviar(SERVICIO, "GET", "/api/pedidos", usuario)


@router.get("/{pedido_id}", summary="Consultar pedido")
async def obtener(pedido_id: int, usuario: str = Depends(usuario_actual)):
    return await proxy.reenviar(SERVICIO, "GET", f"/api/pedidos/{pedido_id}", usuario)


@router.put(
    "/{pedido_id}",
    summary="Actualizar la cantidad de un pedido",
    description="Si la cantidad sube, pedidos descuenta la diferencia en productos; si baja, la devuelve.",
)
async def actualizar(
    pedido_id: int,
    cuerpo: dict[str, Any] = Body(openapi_examples={"tres": {"summary": "Cambiar a 3 unidades", "value": {"cantidad": 3}}}),
    usuario: str = Depends(usuario_actual),
):
    return await proxy.reenviar(SERVICIO, "PUT", f"/api/pedidos/{pedido_id}", usuario, cuerpo=cuerpo)


@router.post(
    "",
    status_code=201,
    summary="Crear pedido",
    description="Pedidos consulta el producto, valida la existencia, la descuenta y registra el pedido.",
)
async def crear(
    cuerpo: dict[str, Any] = Body(openapi_examples=EJEMPLOS_PEDIDO),
    usuario: str = Depends(usuario_actual),
):
    return await proxy.reenviar(SERVICIO, "POST", "/api/pedidos", usuario, cuerpo=cuerpo)
