package ar.utn.donatrack.autenticacion.models;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuenta_administrador")
@PrimaryKeyJoinColumn(name = "cuenta_id")
public class CuentaAdministrador extends Cuenta {
    protected CuentaAdministrador() {}

    public CuentaAdministrador(String email, String nombre, String passwordHash) {
        super(email, nombre, Rol.ADMINISTRADOR, passwordHash);
    }
}
