package ar.utn.donatrack.donaciones.exceptions.seguridadExceptions;

import ar.utn.donatrack.donaciones.exceptions.AccesoDenegadoException;

/**
 * La cuenta todavía no tiene un perfil en Donaciones (el token no trae perfilId):
 * primero tiene que completar el registro. Responde 403.
 */
public class CuentaSinPerfilException extends AccesoDenegadoException {

    public CuentaSinPerfilException(String mensaje) {
        super(mensaje);
    }
}
