package ar.utn.donatrack.donaciones.eventos;

import java.util.UUID;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionAsignada;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionEntregada;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionesRegistradas;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lo que sale hacia afuera cuando algo le pasa a una donación: los avisos a las
 * personas (servicio de notificaciones) y al servicio de incentivos.
 *
 * Corre al final de la transacción del cambio (BEFORE_COMMIT) y deja los avisos
 * en la bandeja de salida: se confirman junto con el cambio o no existen. Los
 * envía RelayDeMensajes, fuera de la operación y con reintentos, así un servicio
 * externo lento o caído no frena ni deshace nada.
 */
@Component
@RequiredArgsConstructor
public class AvisosOyente {

    private final RepositorioDonaciones donaciones;
    private final RepositorioPersonasDonantes donantes;
    private final RepositorioEntidadesBeneficiarias entidades;
    private final Notificador notificador;
    private final IncentivosClient incentivos;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void alAsignarse(DonacionAsignada evento) {
        notificador.donacionAsignada(donantes.buscarPorId(evento.idDonante()).orElse(null),
                entidades.buscarPorId(evento.idEntidad()).orElse(null));
    }

    /** Incentivos usa las entregas para "donaciones exitosas" y "organizaciones ayudadas". */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void alEntregarse(DonacionEntregada evento) {
        donaciones.buscarPorId(evento.idDonacion()).ifPresent(donacion ->
                notificador.destinoDe(donante(evento.idDonante())).ifPresent(destino ->
                        incentivos.notificarDonacionExitosa(donacion, destino.valor(), destino.canal().name(),
                                evento.fechaEntrega())));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void alRegistrarse(DonacionesRegistradas evento) {
        notificador.destinoDe(donante(evento.idDonante())).ifPresent(destino ->
                incentivos.notificarDonacionRegistrada(evento.idDonante(), destino.valor(),
                        destino.canal().name(), evento.cantidadBienes(), evento.categorias(), evento.cuando()));
    }

    private PersonaDonante donante(UUID idDonante) {
        return idDonante == null ? null : donantes.buscarPorId(idDonante).orElse(null);
    }
}
