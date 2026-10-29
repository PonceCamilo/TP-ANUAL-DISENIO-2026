package ar.utn.donatrack.donaciones.eventos;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionCambioDeEstado;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionDadaDeBaja;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Saca de la bandeja de la persona administradora el ranking de una donación que
 * ya no está en depósito (se asignó, venció, se dio de baja). Antes esta limpieza
 * estaba repetida en tres lugares del service de donaciones.
 *
 * Corre en la transacción del cambio: si el cambio se deshace, la bandeja también.
 */
@Component
@RequiredArgsConstructor
public class BandejaDeCandidatosOyente {

    private final RepositorioCandidatosAsignacion candidatos;

    @EventListener
    public void alCambiarDeEstado(DonacionCambioDeEstado evento) {
        if (evento.salioDelDeposito()) {
            candidatos.eliminar(evento.idDonacion());
        }
    }

    @EventListener
    public void alDarseDeBaja(DonacionDadaDeBaja evento) {
        candidatos.eliminar(evento.idDonacion());
    }
}
