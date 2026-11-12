package ar.utn.donatrack.autenticacion.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "cuenta_entidad_beneficiaria")
@PrimaryKeyJoinColumn(name = "cuenta_id")
public class CuentaEntidadBeneficiaria extends Cuenta {
    @Column(name = "entidad_beneficiaria_id", unique = true)
    private UUID entidadBeneficiariaId;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    protected CuentaEntidadBeneficiaria() {}

    public CuentaEntidadBeneficiaria(String email, String nombre, String passwordHash, String googleSub) {
        super(email, nombre, Rol.ENTIDAD_BENEFICIARIA, passwordHash);
        this.googleSub = googleSub;
    }

    public UUID entidadBeneficiariaId() { return entidadBeneficiariaId; }

    public void vincularEntidadBeneficiaria(UUID entidadBeneficiariaId) {
        this.entidadBeneficiariaId = entidadBeneficiariaId;
    }
}
