package ar.utn.donatrack.donaciones.eventos;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lo que sale hacia afuera cuando algo le pasa a una donación: los avisos a las
 * personas (servicio de notificaciones) y al servicio de incentivos.
 *
 * Corre DESPUÉS de que la transacción se confirma (AFTER_COMMIT): si el cambio se
 * deshace, nadie recibe un aviso de algo que no pasó, y un servicio externo lento
 * no tiene abierta la transacción. Cada aviso lee lo que necesita en una
 * transacción propia de solo lectura. Un aviso que falla no deshace el cambio:
 * se registra en el log.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvisosOyente {

    private final RepositorioDonaciones donaciones;
    private final RepositorioPersonasDonantes donantes;
    private final RepositorioEntidadesBeneficiarias entidades;
    private final Notificador notificador;
    private final IncentivosClient incentivos;

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alAsignarse(DonacionAsignada evento) {
        avisar("asignación de la donación " + evento.idDonacion(), () -> notificador.donacionAsignada(
                donantes.buscarPorId(evento.idDonante()).orElse(null),
                entidades.buscarPorId(evento.idEntidad()).orElse(null)));
    }

    /** Incentivos usa las entregas para "donaciones exitosas" y "organizaciones ayudadas". */
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alEntregarse(DonacionEntregada evento) {
        avisar("entrega de la donación " + evento.idDonacion(), () ->
                donaciones.buscarPorId(evento.idDonacion()).ifPresent(donacion ->
                        notificador.destinoDe(donante(evento.idDonante())).ifPresent(destino ->
                                incentivos.notificarDonacionExitosa(donacion, destino.valor(), destino.canal().name(),
                                        evento.fechaEntrega()))));
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alRegistrarse(DonacionesRegistradas evento) {
        avisar("registro de donaciones del donante " + evento.idDonante(), () ->
                notificador.destinoDe(donante(evento.idDonante())).ifPresent(destino ->
                        incentivos.notificarDonacionRegistrada(evento.idDonante(), destino.valor(),
                                destino.canal().name(), evento.cantidadBienes(), evento.categorias(), evento.cuando())));
    }

    private PersonaDonante donante(java.util.UUID idDonante) {
        return idDonante == null ? null : donantes.buscarPorId(idDonante).orElse(null);
    }

    private void avisar(String que, Runnable aviso) {
        try {
            aviso.run();
        } catch (RuntimeException e) {
            log.error("[AVISOS] No se pudo avisar la {}: {}", que, e.getMessage());
        }
    }
}
