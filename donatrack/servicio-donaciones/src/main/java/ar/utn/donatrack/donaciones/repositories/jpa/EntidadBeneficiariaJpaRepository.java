package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Acceso JPA a la tabla entidad_beneficiaria y, por cascada, a sus campañas y necesidades. */
public interface EntidadBeneficiariaJpaRepository extends JpaRepository<EntidadBeneficiaria, UUID> {

    /**
     * Entidades que tienen ese email como contacto o como email de un
     * representante (Entrega 6). El parámetro llega en minúsculas; del lado de
     * la base se compara con lower(...) porque esos valores se guardan como los
     * escribió la persona, a diferencia del email de las personas donantes.
     *
     * Son subconsultas y no joins para que cada entidad aparezca una sola vez
     * aunque el email figure en un contacto y en un representante a la vez.
     */
    @Query(value = """
            select e from EntidadBeneficiaria e
            where e.id in (select e1.id from EntidadBeneficiaria e1 join e1.contactos c where lower(c.valor) = :email)
               or e.id in (select e2.id from EntidadBeneficiaria e2 join e2.representantes r where lower(r.email) = :email)
            """,
            countQuery = """
            select count(e) from EntidadBeneficiaria e
            where e.id in (select e1.id from EntidadBeneficiaria e1 join e1.contactos c where lower(c.valor) = :email)
               or e.id in (select e2.id from EntidadBeneficiaria e2 join e2.representantes r where lower(r.email) = :email)
            """)
    Page<EntidadBeneficiaria> buscarPorEmail(@Param("email") String email, Pageable pagina);

    /** Una forma escrita por cada subcategoría normalizada de las necesidades (Entrega 6: catálogo). */
    @Query("select min(n.subcategoria.tipo) from Necesidad n "
            + "where n.subcategoria.tipoNormalizado is not null and n.subcategoria.tipoNormalizado <> '' "
            + "group by n.subcategoria.tipoNormalizado")
    List<String> subcategoriasDeNecesidades();
}
