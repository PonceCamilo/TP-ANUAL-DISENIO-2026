package ar.utn.donatrack.autenticacion.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "cuenta", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "rol"})
})

public abstract class Cuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Version
    private long version;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private Rol rol;
    @Column(length = 60)
    private String passwordHash;

    protected Cuenta() {}

    protected Cuenta(String email, String nombre, Rol rol, String passwordHash) {
        this.email = email;
        this.nombre = nombre;
        this.rol = rol;
        this.passwordHash = passwordHash;
    }

    public UUID id() { return id; }
    public String email() { return email; }
    public String nombre() { return nombre; }
    public Rol rol() { return rol; }
    public String passwordHash() { return passwordHash; }

    /** Una cuenta importada o creada con Google no tiene contraseña hasta que la persona la elige. */
    public boolean tieneClave() { return passwordHash != null; }

    /** La cargó la administración y la persona todavía no eligió cómo entrar. */
    public boolean pendienteDeActivacion() { return !tieneClave(); }

    /** La persona eligió su contraseña con un enlace de activación (o la cambió). */
    public void definirClave(String passwordHash) { this.passwordHash = passwordHash; }
}
