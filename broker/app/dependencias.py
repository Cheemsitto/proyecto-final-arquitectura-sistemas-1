import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.seguridad import decodificar_token

# auto_error=False para responder 401 con un mensaje propio en lugar del 403 por defecto.
esquema_bearer = HTTPBearer(auto_error=False, description="Token obtenido en POST /auth/login")


def _no_autorizado(mensaje: str) -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail=mensaje,
        headers={"WWW-Authenticate": "Bearer"},
    )


def usuario_actual(
    credenciales: HTTPAuthorizationCredentials | None = Depends(esquema_bearer),
) -> str:
    """Valida el token Bearer y devuelve el usuario. Si no es válido, corta la petición con 401."""
    if credenciales is None:
        raise _no_autorizado("Falta el token. Envíe la cabecera Authorization: Bearer <token>")
    try:
        datos = decodificar_token(credenciales.credentials)
    except jwt.ExpiredSignatureError:
        raise _no_autorizado("El token expiró. Inicie sesión de nuevo") from None
    except jwt.InvalidTokenError:
        raise _no_autorizado("Token inválido") from None
    return datos["sub"]
