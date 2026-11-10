package ar.utn.donatrack.autenticacion.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "cuenta_chofer")
@PrimaryKeyJoinColumn(name = "cuenta_id")
public class CuentaChofer extends Cuenta {
    @Column(name = "camion_id")
    private UUID camionId;

    protected CuentaChofer() {}

    public CuentaChofer(String email, String nombre, String passwordHash) {
        super(email, nombre, Rol.CHOFER, passwordHash);
    }

    public UUID camionId() { return camionId; }

    public void asignarCamion(UUID camionId) { this.camionId = camionId; }
}
