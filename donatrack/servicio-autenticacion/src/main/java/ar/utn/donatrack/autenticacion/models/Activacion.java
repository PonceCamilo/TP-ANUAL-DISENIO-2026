package ar.utn.donatrack.autenticacion.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enlace de activación de una cuenta: con él la persona elige su contraseña.
 *
 * Sirve para las cuentas que cargó la administración (no tienen contraseña) y
 * para recuperar el acceso. Se guarda solo el hash del código: quien lea la base
 * no puede usar los enlaces. Vence, sirve una sola vez y pedir uno nuevo borra
 * los anteriores que no se usaron.
 */
@Entity
@Table(name = "activacion")
public class Activacion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "codigo_hash", nullable = false, unique = true, length = 64)
    private String codigoHash;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "vence_en", nullable = false)
    private LocalDateTime venceEn;

    @Column(name = "usada_en")
    private LocalDateTime usadaEn;

    protected Activacion() {}

    public Activacion(Cuenta cuenta, String codigoHash, LocalDateTime creadaEn, LocalDateTime venceEn) {
        this.cuenta = cuenta;
        this.codigoHash = codigoHash;
        this.creadaEn = creadaEn;
        this.venceEn = venceEn;
    }

    public Cuenta cuenta() { return cuenta; }

    public LocalDateTime creadaEn() { return creadaEn; }

    public boolean usada() { return usadaEn != null; }

    public boolean vencida(LocalDateTime ahora) { return !ahora.isBefore(venceEn); }

    public void marcarUsada(LocalDateTime ahora) { this.usadaEn = ahora; }
}
