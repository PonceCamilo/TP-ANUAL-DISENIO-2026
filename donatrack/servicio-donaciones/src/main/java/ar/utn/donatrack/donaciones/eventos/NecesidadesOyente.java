package ar.utn.donatrack.donaciones.eventos;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionEntregada;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cierra el ciclo de cantidades: lo entregado cuenta para las necesidades de la
 * entidad (ver EntidadBeneficiaria.recibirDonacion), venga la entrega del aviso
 * de logística o de un cambio manual a ENTREGADA.
 *
 * Corre en la transacción de la entrega: el estado de la donación y lo recibido
 * por la entidad se confirman juntos.
 */
@Component
@RequiredArgsConstructor
public class NecesidadesOyente {

    private final RepositorioEntidadesBeneficiarias entidades;

    @EventListener
    public void alEntregarse(DonacionEntregada evento) {
        if (evento.idEntidad() == null) {
            return;
        }
        entidades.buscarPorId(evento.idEntidad()).ifPresent(entidad -> {
            entidad.recibirDonacion(evento.subcategoria(), evento.cantidad(), evento.fechaEntrega().toLocalDate());
            entidades.guardar(entidad);
        });
    }
}
