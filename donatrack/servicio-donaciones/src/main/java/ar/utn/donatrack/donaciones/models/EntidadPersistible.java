package ar.utn.donatrack.donaciones.models;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.AfterDomainEventPublication;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.domain.Persistable;


/**
 * Base de las raíces de agregado: Donacion, PersonaDonante y EntidadBeneficiaria.
 *
 * Reúne lo que antes estaba copiado en cada una (y asignado en lugares
 * distintos: un inicializador, el service, el repositorio):
 *
 *  - IDENTIDAD: el UUID se asigna al construir el objeto, siempre acá.
 *
 *  - ALTA SIN MERGE (Entrega 5): como el id ya viene asignado, Spring Data no
 *    puede deducir si la entidad es nueva y haría un merge, que es un SELECT por
 *    cada INSERT. La marca `nueva` se lo dice (Persistable.isNew) y se apaga al
 *    guardarla o al leerla de la base.
 *
 *  - CONCURRENCIA: `version` es un bloqueo optimista. Si dos operaciones leen la
 *    misma entidad y las dos la modifican, la segunda falla en lugar de pisar en
 *    silencio a la primera (GlobalExceptionHandler responde 409).
 *
 *  - EVENTOS DE DOMINIO: la entidad junta los eventos de lo que le pasó y el
 *    repositorio de Spring Data los publica al guardarla o borrarla (@DomainEvents,
 *    como AbstractAggregateRoot). Los oyentes del paquete `eventos` hacen los
 *    efectos que antes estaban mezclados en los services.
 */
@MappedSuperclass
@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class EntidadPersistible implements Persistable<UUID> {

    @Id
    @Column(name = "id")
    @Builder.Default
    protected UUID id = UUID.randomUUID();

    @Version
    @Column(name = "version", nullable = false)
    @ColumnDefault("0")
    protected long version;

    @Transient
    @Builder.Default
    @Getter(AccessLevel.NONE)
    protected boolean nueva = true;

    @Transient
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private List<Object> eventos = new ArrayList<>();

    @Override
    public boolean isNew() {
        return nueva;
    }

    protected void registrarEvento(Object evento) {
        eventos.add(evento);
    }

    /** Los eventos que todavía no se publicaron, en el orden en que ocurrieron. */
    @DomainEvents
    public List<Object> eventosPendientes() {
        return List.copyOf(eventos);
    }

    @AfterDomainEventPublication
    protected void limpiarEventos() {
        eventos.clear();
    }

    @PostLoad
    @PostPersist
    void marcarComoPersistida() {
        this.nueva = false;
    }
}
