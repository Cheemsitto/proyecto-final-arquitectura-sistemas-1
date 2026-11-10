from datetime import datetime, timedelta, timezone

import jwt
import pytest

from app.config import config
from app.seguridad import crear_token, decodificar_token, hashear_password, verificar_password


def test_hash_y_verificacion_de_password():
    guardado = hashear_password("Secreta123")

    assert guardado.startswith("pbkdf2_sha256$")
    assert "Secreta123" not in guardado
    assert verificar_password("Secreta123", guardado)
    assert not verificar_password("otra", guardado)


def test_cada_hash_usa_una_sal_distinta():
    assert hashear_password("igual") != hashear_password("igual")


@pytest.mark.parametrize("guardado", ["", "texto-plano", "md5$1$aa$bb", "pbkdf2_sha256$x$zz$yy"])
def test_hash_mal_formado_no_valida(guardado):
    assert not verificar_password("lo-que-sea", guardado)


def test_token_contiene_el_usuario():
    token, segundos = crear_token("ana")

    assert decodificar_token(token)["sub"] == "ana"
    assert segundos == config.jwt_expira_min * 60


def test_token_expirado_se_rechaza():
    vencido = jwt.encode(
        {"sub": "ana", "exp": datetime.now(timezone.utc) - timedelta(minutes=1)},
        config.jwt_secret,
        algorithm="HS256",
    )
    with pytest.raises(jwt.ExpiredSignatureError):
        decodificar_token(vencido)


def test_token_firmado_con_otra_clave_se_rechaza():
    falso = jwt.encode(
        {"sub": "ana", "exp": datetime.now(timezone.utc) + timedelta(minutes=5)},
        "otra-clave-distinta-con-longitud-suficiente-000000",
        algorithm="HS256",
    )
    with pytest.raises(jwt.InvalidTokenError):
        decodificar_token(falso)
