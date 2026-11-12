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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "donacion_registrada")
public class DonacionRegistrada {
    @Id
    private UUID id = UUID.randomUUID();

    private LocalDateTime fecha;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "donacion_categoria", joinColumns = @JoinColumn(name = "donacion_id"))
    private Set<String> categorias = new HashSet<>();

    private int cantidadBienes;
    private boolean exitosa;
    private String entidadBeneficiaria;

    @Builder
    public DonacionRegistrada(UUID id, LocalDateTime fecha, Set<String> categorias, int cantidadBienes,
                              boolean exitosa, String entidadBeneficiaria) {
        if (id != null) {
            this.id = id;
        }
        this.fecha = fecha;
        if (categorias != null) {
            this.categorias.addAll(categorias);
        }
        this.cantidadBienes = cantidadBienes;
        this.exitosa = exitosa;
        this.entidadBeneficiaria = entidadBeneficiaria;
    }
}
