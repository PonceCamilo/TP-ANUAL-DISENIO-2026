
package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Donacion {
    protected Bien bien;
    protected EstadoDonacion estado = EstadoDonacion.EN_DEPOSITO;
    protected List<CambioEstado> historialEstados = new ArrayList<>(
        List.of(CambioEstado.builder().estado(EstadoDonacion.EN_DEPOSITO).build())
    );
    protected int idCargaOrigen;
    protected int cantidad;
    protected List<Bien> bienes = new ArrayList<>();


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

    public void cambiarEstado(EstadoDonacion nuevo) {
        cambiarEstado(nuevo, null);
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
