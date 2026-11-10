"""Login con la sesión de base de datos simulada (mock)."""

from unittest.mock import MagicMock

from app.database import obtener_sesion
from app.main import app
from app.models import Usuario
from app.seguridad import decodificar_token, hashear_password


def _usar_sesion_con(usuario):
    sesion = MagicMock()
    sesion.scalar.return_value = usuario
    app.dependency_overrides[obtener_sesion] = lambda: sesion
    return sesion


def _usuario(activo=True):
    return Usuario(usuario="admin", password_hash=hashear_password("Admin123*"), activo=activo)


def test_login_correcto_entrega_token(cliente):
    sesion = _usar_sesion_con(_usuario())

    respuesta = cliente.post("/auth/login", json={"usuario": "admin", "password": "Admin123*"})

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["token_type"] == "bearer"
    assert decodificar_token(cuerpo["access_token"])["sub"] == "admin"
    sesion.scalar.assert_called_once()


def test_password_incorrecta_responde_401(cliente):
    _usar_sesion_con(_usuario())

    respuesta = cliente.post("/auth/login", json={"usuario": "admin", "password": "mala"})

    assert respuesta.status_code == 401
    assert respuesta.json() == {"error": "Usuario o contraseña incorrectos"}


def test_usuario_inexistente_responde_401(cliente):
    _usar_sesion_con(None)

    respuesta = cliente.post("/auth/login", json={"usuario": "nadie", "password": "x"})

    assert respuesta.status_code == 401


def test_usuario_inactivo_responde_401(cliente):
    _usar_sesion_con(_usuario(activo=False))

    respuesta = cliente.post("/auth/login", json={"usuario": "admin", "password": "Admin123*"})

    assert respuesta.status_code == 401


def test_login_sin_datos_responde_400(cliente):
    _usar_sesion_con(None)

    respuesta = cliente.post("/auth/login", json={})

    assert respuesta.status_code == 400
    assert respuesta.json()["error"] == "Datos inválidos"


def test_perfil_devuelve_el_usuario_del_token(cliente, autorizacion):
    respuesta = cliente.get("/auth/perfil", headers=autorizacion)

    assert respuesta.status_code == 200
    assert respuesta.json() == {"usuario": "admin"}
