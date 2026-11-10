import os

from dotenv import load_dotenv

# Carga el archivo .env de la carpeta del broker. Las variables reales del sistema tienen prioridad.
load_dotenv()


def _entero(nombre: str, por_defecto: int) -> int:
    try:
        return int(os.getenv(nombre, str(por_defecto)))
    except ValueError:
        return por_defecto


class Configuracion:
    def __init__(self) -> None:
        self.puerto = _entero("PORT", 8000)

        # Base de datos propia del broker (usuarios)
        self.db_host = os.getenv("DB_HOST", "localhost")
        self.db_port = _entero("DB_PORT", 3306)
        self.db_name = os.getenv("DB_NAME", "broker_db")
        self.db_user = os.getenv("DB_USER", "root")
        self.db_password = os.getenv("DB_PASSWORD", "")
        self.db_ssl = os.getenv("DB_SSL", "false").lower() == "true"

        # Token JWT que entrega el broker
        self.jwt_secret = os.getenv("JWT_SECRET", "")
        self.jwt_expira_min = _entero("JWT_EXPIRA_MIN", 60)

        # Servicios a los que reenvía
        self.productos_url = os.getenv("PRODUCTOS_URL", "http://localhost:3001").rstrip("/")
        self.pedidos_url = os.getenv("PEDIDOS_URL", "http://localhost:8082").rstrip("/")
        self.productos_service_key = os.getenv("PRODUCTOS_SERVICE_KEY", "")
        self.pedidos_service_key = os.getenv("PEDIDOS_SERVICE_KEY", "")
        # Debe ser mayor que PRODUCTOS_TIMEOUT_MS de pedidos para que el 504 de pedidos llegue al cliente.
        self.timeout_s = float(os.getenv("TIMEOUT_S", "10"))

        # Usuario que se crea al iniciar si no existe
        self.admin_usuario = os.getenv("ADMIN_USUARIO", "admin")
        self.admin_password = os.getenv("ADMIN_PASSWORD", "")

    def validar(self) -> None:
        faltantes = [
            nombre
            for nombre, valor in (
                ("JWT_SECRET", self.jwt_secret),
                ("PRODUCTOS_SERVICE_KEY", self.productos_service_key),
                ("PEDIDOS_SERVICE_KEY", self.pedidos_service_key),
            )
            if not valor
        ]
        if faltantes:
            raise RuntimeError("Faltan variables de entorno: " + ", ".join(faltantes))
        if len(self.jwt_secret) < 32:
            raise RuntimeError("JWT_SECRET debe tener al menos 32 caracteres")


config = Configuracion()
