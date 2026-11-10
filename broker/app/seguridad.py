import hashlib
import hmac
import os
from datetime import datetime, timedelta, timezone

import jwt

from app.config import config

ALGORITMO_JWT = "HS256"
ITERACIONES = 200_000


def hashear_password(password: str) -> str:
    """PBKDF2-SHA256 con sal aleatoria. Formato: pbkdf2_sha256$iteraciones$sal$hash."""
    sal = os.urandom(16)
    derivada = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), sal, ITERACIONES)
    return f"pbkdf2_sha256${ITERACIONES}${sal.hex()}${derivada.hex()}"


def verificar_password(password: str, guardado: str) -> bool:
    try:
        algoritmo, iteraciones, sal_hex, derivada_hex = guardado.split("$")
        if algoritmo != "pbkdf2_sha256":
            return False
        derivada = hashlib.pbkdf2_hmac(
            "sha256", password.encode("utf-8"), bytes.fromhex(sal_hex), int(iteraciones)
        )
    except (ValueError, AttributeError):
        return False
    return hmac.compare_digest(derivada.hex(), derivada_hex)


def crear_token(usuario: str) -> tuple[str, int]:
    """Devuelve el token y los segundos que dura."""
    ahora = datetime.now(timezone.utc)
    duracion = timedelta(minutes=config.jwt_expira_min)
    datos = {"sub": usuario, "iat": ahora, "exp": ahora + duracion}
    token = jwt.encode(datos, config.jwt_secret, algorithm=ALGORITMO_JWT)
    return token, int(duracion.total_seconds())


def decodificar_token(token: str) -> dict:
    """Lanza jwt.ExpiredSignatureError o jwt.InvalidTokenError si el token no sirve."""
    return jwt.decode(
        token,
        config.jwt_secret,
        algorithms=[ALGORITMO_JWT],
        options={"require": ["sub", "exp"]},
    )
