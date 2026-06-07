package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Donacion {

    protected List<Bien> bienes = new ArrayList<>();
    protected Subcategoria subcategoria;
    protected EstadoDonacion estado = EstadoDonacion.EN_DEPOSITO;
    protected List<CambioEstado> historialEstados = new ArrayList<>(
        List.of(CambioEstado.builder().estado(EstadoDonacion.EN_DEPOSITO).build()));
    protected int idCargaOrigen;

    public boolean esPerecible() {
        return !bienes.isEmpty() && bienes.get(0) instanceof BienPerecible;
    }

    public boolean requiereEstado() {
        return !bienes.isEmpty() && bienes.get(0) instanceof BienConEstado;
    }

    public void cambiarEstado(EstadoDonacion nuevo, String justificacion) {
        validarTransicion(nuevo);
        if (nuevo == EstadoDonacion.ENTREGA_FALLIDA && (justificacion == null || justificacion.isBlank())) {
            throw new IllegalArgumentException("Se requiere justificación para Entrega fallida");
        }
        this.historialEstados.add(CambioEstado.builder()
                .estado(nuevo)
                .justificacion(justificacion)
                .build());
        this.estado = nuevo;
    }

    private void validarTransicion(EstadoDonacion nuevo) {
        boolean valida = switch (this.estado) {
            case EN_DEPOSITO -> nuevo == EstadoDonacion.ASIGNACION_REALIZADA || nuevo == EstadoDonacion.VENCIDA;
            case ASIGNACION_REALIZADA -> nuevo == EstadoDonacion.LISTA_PARA_ENTREGAR;
            case LISTA_PARA_ENTREGAR -> nuevo == EstadoDonacion.EN_TRASLADO;
            case EN_TRASLADO -> nuevo == EstadoDonacion.ENTREGADA || nuevo == EstadoDonacion.ENTREGA_FALLIDA;
            case ENTREGA_FALLIDA -> nuevo == EstadoDonacion.EN_DEPOSITO;
            default -> false;
        };
        if (!valida) {
            throw new IllegalStateException("Transición inválida: " + this.estado + " → " + nuevo);
        }
    }
}
