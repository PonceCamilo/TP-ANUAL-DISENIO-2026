package ar.utn.donatrack.logistica.exceptions;

/** La persona tiene el rol, pero el recurso no es suyo (403). */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
