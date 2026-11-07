package ar.utn.donatrack.incentivos.models;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "donacion_registrada")
public class DonacionRegistrada {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    private LocalDateTime fecha;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "donacion_categoria", joinColumns = @JoinColumn(name = "donacion_id"))
    private Set<String> categorias;

    private int cantidadBienes;
    private boolean exitosa;
    private String entidadBeneficiaria;

    public Set<String> getCategorias() {
        return categorias == null ? Collections.emptySet() : categorias;
    }
}
