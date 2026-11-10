"""Reenvío HTTP con el cliente httpx simulado (mock): no se levanta ningún servicio real."""

import asyncio
import json

import httpx
import pytest

from app import proxy


class ClienteFalso:
    def __init__(self, respuesta=None, error=None):
        self.respuesta = respuesta
        self.error = error
        self.llamadas = []

    async def request(self, metodo, url, json=None, headers=None):
        self.llamadas.append({"metodo": metodo, "url": url, "json": json, "headers": headers})
        if self.error:
            raise self.error
        return self.respuesta


@pytest.fixture
def instalar(monkeypatch):
    def _instalar(**opciones):
        falso = ClienteFalso(**opciones)
        monkeypatch.setattr(proxy, "_obtener_cliente", lambda: falso)
        return falso

    return _instalar


def _reenviar(*argumentos, **opciones):
    return asyncio.run(proxy.reenviar(*argumentos, **opciones))


def test_reenvia_con_la_clave_interna_y_devuelve_la_respuesta(instalar):
    falso = instalar(
        respuesta=httpx.Response(201, json={"id": 7}, headers={"location": "/api/productos/7"})
    )

    respuesta = _reenviar("productos", "POST", "/api/productos", "admin", cuerpo={"nombre": "X"})

    assert respuesta.status_code == 201
    assert json.loads(respuesta.body) == {"id": 7}
    assert respuesta.headers["location"] == "/api/productos/7"
    llamada = falso.llamadas[0]
    assert llamada["url"] == "http://productos.test/api/productos"
    assert llamada["json"] == {"nombre": "X"}
    assert llamada["headers"]["X-Service-Key"] == "clave-productos"
    assert llamada["headers"]["X-Usuario"] == "admin"


def test_usa_la_clave_y_la_url_de_pedidos(instalar):
    falso = instalar(respuesta=httpx.Response(200, json=[]))

    _reenviar("pedidos", "GET", "/api/pedidos", "admin")

    assert falso.llamadas[0]["url"] == "http://pedidos.test/api/pedidos"
    assert falso.llamadas[0]["headers"]["X-Service-Key"] == "clave-pedidos"


def test_respeta_los_errores_de_negocio_del_servicio(instalar):
    instalar(respuesta=httpx.Response(409, json={"error": "Existencia insuficiente"}))

    respuesta = _reenviar("pedidos", "POST", "/api/pedidos", "admin", cuerpo={"productoId": 1, "cantidad": 99})

    assert respuesta.status_code == 409
    assert json.loads(respuesta.body) == {"error": "Existencia insuficiente"}


def test_servicio_que_no_responde_a_tiempo_da_504(instalar):
    instalar(error=httpx.ReadTimeout("tiempo agotado"))

    respuesta = _reenviar("productos", "GET", "/api/productos", "admin")

    assert respuesta.status_code == 504
    assert "no respondió a tiempo" in json.loads(respuesta.body)["error"]


def test_servicio_apagado_da_503(instalar):
    instalar(error=httpx.ConnectError("conexión rechazada"))

    respuesta = _reenviar("pedidos", "GET", "/api/pedidos", "admin")

    assert respuesta.status_code == 503


def test_clave_interna_rechazada_da_502(instalar):
    instalar(respuesta=httpx.Response(401, json={"error": "Clave de servicio inválida o ausente"}))

    respuesta = _reenviar("productos", "GET", "/api/productos", "admin")

    assert respuesta.status_code == 502
    assert "no pudo autenticarse" in json.loads(respuesta.body)["error"]
