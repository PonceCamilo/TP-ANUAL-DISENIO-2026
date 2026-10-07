package ar.utn.donatrack.incentivos.models.insignias;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "insignia_obtenida")
public class InsigniaObtenida {
    @Id
    private UUID id;

    @ManyToOne
    private Insignia insignia;

    private boolean visibilidad;
    private LocalDate fechaObtencion; // no lo pide pero me parece que es importante tenerlo

    public InsigniaObtenida(Insignia insignia, boolean visibilidad) {
        this.id = UUID.randomUUID();
        this.insignia = insignia;
        this.visibilidad = visibilidad;
        this.fechaObtencion = LocalDate.now();
    }
    
}
