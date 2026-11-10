import logging
import re
import ssl

from sqlalchemy import create_engine, select, text
from sqlalchemy.engine import URL
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.config import config

log = logging.getLogger("broker.bd")


class Base(DeclarativeBase):
    pass


def _url(con_base: bool = True) -> URL:
    return URL.create(
        "mysql+pymysql",
        username=config.db_user,
        password=config.db_password,
        host=config.db_host,
        port=config.db_port,
        database=config.db_name if con_base else None,
    )


def _argumentos_conexion() -> dict:
    if not config.db_ssl:
        return {}
    contexto = ssl.create_default_context()
    contexto.check_hostname = False
    contexto.verify_mode = ssl.CERT_NONE
    return {"ssl": contexto}


engine = create_engine(
    _url(),
    pool_pre_ping=True,
    pool_recycle=280,
    connect_args=_argumentos_conexion(),
)
SesionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


def obtener_sesion():
    sesion = SesionLocal()
    try:
        yield sesion
    finally:
        sesion.close()


def _crear_base_si_no_existe() -> None:
    if not re.fullmatch(r"[A-Za-z0-9_]+", config.db_name):
        raise RuntimeError("DB_NAME solo admite letras, números y guion bajo")
    servidor = create_engine(_url(con_base=False), connect_args=_argumentos_conexion())
    try:
        with servidor.connect() as conexion:
            conexion.execute(text(f"CREATE DATABASE IF NOT EXISTS `{config.db_name}`"))
    except Exception as error:  # el usuario de la nube puede no tener permiso
        log.warning("No se pudo crear la base %s: %s", config.db_name, error)
    finally:
        servidor.dispose()


def _crear_admin_si_falta() -> None:
    from app.models import Usuario
    from app.seguridad import hashear_password

    if not config.admin_password:
        log.warning("ADMIN_PASSWORD está vacío; no se crea el usuario administrador")
        return

    with SesionLocal() as sesion:
        existe = sesion.scalar(select(Usuario).where(Usuario.usuario == config.admin_usuario))
        if existe is None:
            sesion.add(
                Usuario(
                    usuario=config.admin_usuario,
                    password_hash=hashear_password(config.admin_password),
                )
            )
            sesion.commit()
            log.info("Usuario %s creado", config.admin_usuario)


def inicializar_bd() -> None:
    import app.models  # noqa: F401  registra la tabla usuarios en Base.metadata

    _crear_base_si_no_existe()
    Base.metadata.create_all(engine)
    _crear_admin_si_falta()
