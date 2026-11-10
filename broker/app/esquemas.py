from pydantic import BaseModel, Field


class LoginSolicitud(BaseModel):
    usuario: str = Field(min_length=1, max_length=50, examples=["admin"])
    password: str = Field(min_length=1, max_length=128, examples=["Admin123*"])


class TokenRespuesta(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expira_en: int = Field(description="Segundos de validez del token")


class Error(BaseModel):
    error: str
    detalles: list[str] | None = None


# Respuestas que comparten todas las rutas protegidas (para Swagger)
RESPUESTAS_PROTEGIDAS = {
    401: {"model": Error, "description": "Falta el token, es inválido o expiró"},
    502: {"model": Error, "description": "El servicio destino rechazó la clave interna del broker"},
    503: {"model": Error, "description": "El servicio destino no está disponible"},
    504: {"model": Error, "description": "El servicio destino no respondió a tiempo"},
}
