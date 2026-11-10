"""Validación del token y enrutamiento, con el reenvío simulado (mock)."""

from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock

import jwt
import pytest
from fastapi.responses import JSONResponse

from app import proxy
from app.config import config


@pytest.fixture
def reenviar(monkeypatch):
    simulado = AsyncMock(return_value=JSONResponse({"ok": True}))
    monkeypatch.setattr(proxy, "reenviar", simulado)
    return simulado


def test_sin_token_responde_401_y_no_reenvia(cliente, reenviar):
    respuesta = cliente.get("/api/productos")

    assert respuesta.status_code == 401
    assert "Falta el token" in respuesta.json()["error"]
    assert respuesta.headers["www-authenticate"] == "Bearer"
    reenviar.assert_not_called()


def test_token_invalido_responde_401(cliente, reenviar):
    respuesta = cliente.get("/api/pedidos", headers={"Authorization": "Bearer abc.def.ghi"})

    assert respuesta.status_code == 401
    assert respuesta.json() == {"error": "Token inválido"}
    reenviar.assert_not_called()


def test_token_expirado_responde_401(cliente, reenviar):
    vencido = jwt.encode(
        {"sub": "admin", "exp": datetime.now(timezone.utc) - timedelta(seconds=5)},
        config.jwt_secret,
        algorithm="HS256",
    )

    respuesta = cliente.get("/api/productos", headers={"Authorization": f"Bearer {vencido}"})

    assert respuesta.status_code == 401
    assert "expiró" in respuesta.json()["error"]
    reenviar.assert_not_called()


def test_esquema_distinto_de_bearer_responde_401(cliente, reenviar):
    respuesta = cliente.get("/api/productos", headers={"Authorization": "Basic YWRtaW46eA=="})

    assert respuesta.status_code == 401
    reenviar.assert_not_called()


def test_listar_productos_va_al_servicio_de_productos(cliente, autorizacion, reenviar):
    respuesta = cliente.get("/api/productos", headers=autorizacion)

    assert respuesta.status_code == 200
    reenviar.assert_awaited_once_with("productos", "GET", "/api/productos", "admin")


def test_crear_producto_reenvia_el_cuerpo_sin_modificarlo(cliente, autorizacion, reenviar):
    cuerpo = {"nombre": "Mouse", "precio": 0, "existencia": 5}

    cliente.post("/api/productos", json=cuerpo, headers=autorizacion)

    # El broker no valida el precio: eso le toca al servicio de productos.
    reenviar.assert_awaited_once_with("productos", "POST", "/api/productos", "admin", cuerpo=cuerpo)


@pytest.mark.parametrize(
    ("metodo", "ruta", "servicio", "destino"),
    [
        ("get", "/api/productos/3", "productos", "/api/productos/3"),
        ("delete", "/api/productos/3", "productos", "/api/productos/3"),
        ("get", "/api/pedidos", "pedidos", "/api/pedidos"),
        ("get", "/api/pedidos/8", "pedidos", "/api/pedidos/8"),
    ],
)
def test_enrutamiento_por_prefijo(cliente, autorizacion, reenviar, metodo, ruta, servicio, destino):
    getattr(cliente, metodo)(ruta, headers=autorizacion)

    reenviar.assert_awaited_once_with(servicio, metodo.upper(), destino, "admin")


def test_actualizar_producto(cliente, autorizacion, reenviar):
    cuerpo = {"nombre": "Mouse", "precio": 10, "existencia": 1}

    cliente.put("/api/productos/2", json=cuerpo, headers=autorizacion)

    reenviar.assert_awaited_once_with("productos", "PUT", "/api/productos/2", "admin", cuerpo=cuerpo)


def test_crear_pedido_va_al_servicio_de_pedidos(cliente, autorizacion, reenviar):
    cuerpo = {"productoId": 1, "cantidad": 2}

    cliente.post("/api/pedidos", json=cuerpo, headers=autorizacion)

    reenviar.assert_awaited_once_with("pedidos", "POST", "/api/pedidos", "admin", cuerpo=cuerpo)


def test_id_no_numerico_responde_400(cliente, autorizacion, reenviar):
    respuesta = cliente.get("/api/productos/abc", headers=autorizacion)

    assert respuesta.status_code == 400
    reenviar.assert_not_called()


def test_descontar_existencia_no_se_expone_al_cliente(cliente, autorizacion, reenviar):
    respuesta = cliente.patch("/api/productos/1/descontar", json={"cantidad": 1}, headers=autorizacion)

    assert respuesta.status_code in (404, 405)
    reenviar.assert_not_called()


def test_health_no_exige_token(cliente):
    assert cliente.get("/health").status_code == 200
