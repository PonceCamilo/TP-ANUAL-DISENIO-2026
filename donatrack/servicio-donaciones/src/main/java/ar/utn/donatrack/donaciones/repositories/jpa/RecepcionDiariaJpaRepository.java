package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.asignacion.RecepcionDiariaEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Acceso JPA al contador desnormalizado de donaciones recibidas por entidad y día. */
public interface RecepcionDiariaJpaRepository
        extends JpaRepository<RecepcionDiariaEntidad, RecepcionDiariaEntidad.Clave> {

    /** Total de una entidad desde una fecha, contada inclusive. */
    interface TotalPorEntidad {
        UUID getIdEntidad();

        Long getTotal();
    }

    @Query("SELECT r.clave.idEntidad AS idEntidad, SUM(r.cantidad) AS total "
            + "FROM RecepcionDiariaEntidad r WHERE r.clave.fecha >= :desde GROUP BY r.clave.idEntidad")
    List<TotalPorEntidad> totalesDesde(@Param("desde") LocalDate desde);

    @Query("SELECT COALESCE(SUM(r.cantidad), 0) FROM RecepcionDiariaEntidad r "
            + "WHERE r.clave.idEntidad = :idEntidad AND r.clave.fecha >= :desde")
    long totalDesde(@Param("idEntidad") UUID idEntidad, @Param("desde") LocalDate desde);

    /** Suma (o resta, con delta negativo) a una fila existente. Devuelve cuántas filas tocó: 0 o 1. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RecepcionDiariaEntidad r SET r.cantidad = r.cantidad + :delta "
            + "WHERE r.clave.idEntidad = :idEntidad AND r.clave.fecha = :fecha")
    int sumar(@Param("idEntidad") UUID idEntidad, @Param("fecha") LocalDate fecha, @Param("delta") long delta);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM entidad_recepcion_diaria", nativeQuery = true)
    void vaciar();

    /**
     * Recalcula el contador entero desde la tabla donacion, que es la fuente de
     * verdad. Es SQL nativo, así que la baja lógica se filtra a mano: las
     * donaciones dadas de baja no cuentan.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO entidad_recepcion_diaria (entidad_id, fecha, cantidad) "
            + "SELECT id_entidad_beneficiaria, fecha_asignacion, COUNT(*) FROM donacion "
            + "WHERE id_entidad_beneficiaria IS NOT NULL AND fecha_asignacion IS NOT NULL "
            + "AND fecha_baja IS NULL "
            + "GROUP BY id_entidad_beneficiaria, fecha_asignacion", nativeQuery = true)
    int recalcularDesdeDonaciones();
}
