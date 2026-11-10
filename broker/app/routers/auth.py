from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database import obtener_sesion
from app.dependencias import usuario_actual
from app.esquemas import Error, LoginSolicitud, TokenRespuesta
from app.models import Usuario
from app.seguridad import crear_token, verificar_password

router = APIRouter(prefix="/auth", tags=["Autenticación"])


@router.post(
    "/login",
    response_model=TokenRespuesta,
    summary="Iniciar sesión y obtener un token",
    responses={401: {"model": Error, "description": "Usuario o contraseña incorrectos"}},
)
def login(datos: LoginSolicitud, sesion: Session = Depends(obtener_sesion)) -> TokenRespuesta:
    usuario = sesion.scalar(select(Usuario).where(Usuario.usuario == datos.usuario))
    if usuario is None or not usuario.activo or not verificar_password(datos.password, usuario.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Usuario o contraseña incorrectos")

    token, segundos = crear_token(usuario.usuario)
    return TokenRespuesta(access_token=token, expira_en=segundos)


@router.get(
    "/perfil",
    summary="Ver el usuario del token",
    responses={401: {"model": Error, "description": "Falta el token, es inválido o expiró"}},
)
def perfil(usuario: str = Depends(usuario_actual)) -> dict:
    return {"usuario": usuario}
