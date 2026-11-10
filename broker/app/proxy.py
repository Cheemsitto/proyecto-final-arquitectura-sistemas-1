"""Reenvío de peticiones. El broker no aplica reglas de negocio: solo decide a qué servicio va cada ruta."""

import logging
from typing import Any

import httpx
from fastapi import Response
from fastapi.responses import JSONResponse

from app.config import config

log = logging.getLogger("broker.proxy")


def _destinos() -> dict[str, tuple[str, str]]:
    return {
        "productos": (config.productos_url, config.productos_service_key),
        "pedidos": (config.pedidos_url, config.pedidos_service_key),
    }


_cliente_compartido: httpx.AsyncClient | None = None


def _obtener_cliente() -> httpx.AsyncClient:
    """Un solo cliente para todo el broker: reutiliza conexiones en lugar de abrir una por petición."""
    global _cliente_compartido
    if _cliente_compartido is None or _cliente_compartido.is_closed:
        _cliente_compartido = httpx.AsyncClient(timeout=config.timeout_s)
    return _cliente_compartido


async def cerrar_cliente() -> None:
    if _cliente_compartido is not None:
        await _cliente_compartido.aclose()


def _error(estado: int, mensaje: str) -> JSONResponse:
    return JSONResponse(status_code=estado, content={"error": mensaje})


async def reenviar(
    servicio: str,
    metodo: str,
    ruta: str,
    usuario: str,
    cuerpo: Any = None,
) -> Response:
    base, clave = _destinos()[servicio]
    cabeceras = {
        "X-Service-Key": clave,
        "X-Usuario": usuario,
        "Accept": "application/json",
    }

    try:
        respuesta = await _obtener_cliente().request(metodo, f"{base}{ruta}", json=cuerpo, headers=cabeceras)
    except httpx.TimeoutException:
        log.warning("%s %s: %s no respondió a tiempo", metodo, ruta, servicio)
        return _error(504, f"El servicio de {servicio} no respondió a tiempo")
    except httpx.RequestError as error:
        log.warning("%s %s: %s no disponible (%s)", metodo, ruta, servicio, error)
        return _error(503, f"El servicio de {servicio} no está disponible")

    # Si el servicio rechaza la clave interna, el problema es de configuración del broker,
    # no del token del cliente. Por eso no se devuelve 401.
    if respuesta.status_code in (401, 403):
        log.error("%s rechazó la clave de servicio del broker", servicio)
        return _error(502, f"El broker no pudo autenticarse con el servicio de {servicio}")

    cabeceras_respuesta = {}
    if "location" in respuesta.headers:
        cabeceras_respuesta["Location"] = respuesta.headers["location"]

    return Response(
        content=respuesta.content,
        status_code=respuesta.status_code,
        media_type=respuesta.headers.get("content-type", "application/json"),
        headers=cabeceras_respuesta,
    )
