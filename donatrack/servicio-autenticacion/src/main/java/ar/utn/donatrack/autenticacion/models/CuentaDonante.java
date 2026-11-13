package ar.utn.donatrack.autenticacion.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "cuenta_donante")
@PrimaryKeyJoinColumn(name = "cuenta_id")
public class CuentaDonante extends Cuenta {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_donante", nullable = false)
    private TipoDonante tipoDonante;

    @Column(name = "donante_id", unique = true)
    private UUID donanteId;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    protected CuentaDonante() {}

    public CuentaDonante(String email, String nombre, String passwordHash, TipoDonante tipoDonante, String googleSub) {
        super(email, nombre, Rol.DONANTE, passwordHash);
        this.tipoDonante = tipoDonante == null ? TipoDonante.HUMANA : tipoDonante;
        this.googleSub = googleSub;
    }

    /**
     * Cuenta de una persona que la administración cargó en Donaciones (CSV o alta
     * manual). Nace vinculada a su perfil y sin contraseña: la persona la elige con
     * el enlace de activación que pide por email.
     */
    public static CuentaDonante importada(String email, String nombre, TipoDonante tipoDonante, UUID donanteId) {
        CuentaDonante cuenta = new CuentaDonante(email, nombre, null, tipoDonante, null);
        cuenta.donanteId = donanteId;
        return cuenta;
    }

    @Override
    public boolean pendienteDeActivacion() { return super.pendienteDeActivacion() && googleSub == null; }

    public TipoDonante tipoDonante() { return tipoDonante; }

    public UUID donanteId() { return donanteId; }

    public void vincularDonante(UUID donanteId) { this.donanteId = donanteId; }
}
