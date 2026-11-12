package ar.utn.donatrack.donaciones.exceptions.seguridadExceptions;

import ar.utn.donatrack.donaciones.exceptions.ConflictoException;

/**
 * La cuenta ya tiene un perfil en Donaciones (el token trae perfilId) y quiere
 * registrar otro. Responde 409: el pedido choca con lo que ya existe.
 */
public class CuentaConPerfilException extends ConflictoException {

    public CuentaConPerfilException(String mensaje) {
        super(mensaje);
    }
}
