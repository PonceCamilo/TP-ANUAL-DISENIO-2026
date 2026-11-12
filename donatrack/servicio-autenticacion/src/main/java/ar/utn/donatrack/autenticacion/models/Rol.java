package ar.utn.donatrack.autenticacion.models;

public enum Rol {
    DONANTE, ENTIDAD_BENEFICIARIA, ADMINISTRADOR, CHOFER;

    public boolean esDonante() {
        return this == DONANTE || this == ENTIDAD_BENEFICIARIA;
    }
}
