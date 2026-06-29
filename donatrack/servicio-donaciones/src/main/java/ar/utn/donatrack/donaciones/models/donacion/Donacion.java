package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donacion.estado.EnDepositoState;
import ar.utn.donatrack.donaciones.models.donacion.estado.EstadoDonacionBase;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Donacion {

    @Setter protected List<Bien> bienes = new ArrayList<>();
    @Setter protected Subcategoria subcategoria;
    protected EstadoDonacionBase estado = new EnDepositoState();
    protected List<CambioEstado> historialEstados = new ArrayList<>();
    @Setter protected UUID idDonante;
    @Setter protected UUID idEntidadBeneficiaria;
    @Setter protected String descripcion;
    @Setter protected LocalDateTime fechaDonacion = LocalDateTime.now();
    @Setter protected UUID id = UUID.randomUUID();
    @Setter protected LocalDate fechaAsignacion;

    public void cambiarEstado(String estadoDestino, String nombreTransicion, String justificacion) {
        EstadoDonacionBase estadoPrevio = this.estado;
        this.estado = this.estado.transicionarA(estadoDestino, justificacion);
        historialEstados.add(CambioEstado.builder()
                .estadoPrevio(estadoPrevio)
                .estado(this.estado)
                .nombreTransicion(nombreTransicion)
                .justificacion(justificacion)
                .build());
    }

    public boolean esPerecible() {
        return !bienes.isEmpty() && bienes.getFirst() instanceof BienPerecible;
    }

    public boolean requiereEstado() {
        return !bienes.isEmpty() && bienes.getFirst() instanceof BienConEstado;
    }
}