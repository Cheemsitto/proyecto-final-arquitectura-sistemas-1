import os

# Variables de prueba: se definen antes de importar la aplicación.
os.environ["JWT_SECRET"] = "secreto-de-pruebas-con-longitud-suficiente-0123456789"
os.environ["PRODUCTOS_URL"] = "http://productos.test"
os.environ["PEDIDOS_URL"] = "http://pedidos.test"
os.environ["PRODUCTOS_SERVICE_KEY"] = "clave-productos"
os.environ["PEDIDOS_SERVICE_KEY"] = "clave-pedidos"

import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402

from app.main import app  # noqa: E402
from app.seguridad import crear_token  # noqa: E402


@pytest.fixture
def cliente():
    # Sin "with": no se ejecuta el ciclo de vida, así que no se conecta a MySQL.
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture
def token() -> str:
    return crear_token("admin")[0]


@pytest.fixture
def autorizacion(token) -> dict:
    return {"Authorization": f"Bearer {token}"}
