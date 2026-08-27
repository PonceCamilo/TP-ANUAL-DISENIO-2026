package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.client.N8nWebhookClient;
import ar.utn.donatrack.incentivos.client.NotificacionClient;
import ar.utn.donatrack.incentivos.models.DonacionRegistrada;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.models.misiones.Racha;
import ar.utn.donatrack.incentivos.interfaces.services.IncentivosServiceInterface;
import ar.utn.donatrack.incentivos.repositories.IncentivosRepositorioEnMemoria;
import ar.utn.donatrack.incentivos.validations.IncentivosValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class IncentivosService implements IncentivosServiceInterface {

    private final IncentivosRepositorioEnMemoria repositorio;
    private final NotificacionClient notificacionClient;
    private final N8nWebhookClient n8nWebhookClient;
    private final IncentivosValidator validator;

    public IncentivosService(IncentivosRepositorioEnMemoria repositorio, NotificacionClient notificacionClient, N8nWebhookClient n8nWebhookClient, IncentivosValidator validator) {
        this.repositorio = repositorio;
        this.notificacionClient = notificacionClient;
        this.n8nWebhookClient = n8nWebhookClient;
        this.validator = validator;
    }

    public Donante obtenerPerfil(UUID donanteId) {
        Donante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        if (perfil.getCategoria() == null) {
            asignarCategoria(perfil, new Colaborador());
        }
        sincronizarMisionesDeCategoria(perfil.getCategoria());
        if (perfil.getProgresoMision().getMisionActual() == null) {
            asignarPrimeraMisionDeCategoria(perfil);
        }
        revisarPerdidaMision(perfil);
        repositorio.guardarPerfil(perfil);
        return perfil;
    }

    public List<Mision> obtenerMisiones(UUID donanteId) {
        Donante perfil = obtenerPerfil(donanteId);
        return repositorio.listarMisionesPorCategoria(perfil.getCategoria());
    }

    public List<InsigniaObtenida> obtenerInsignias(UUID donanteId) {
        return obtenerPerfil(donanteId).getInsigniasObtenidas();
    }

    public RankingMensual obtenerRankingMensualActual() {
        return RankingMensual.calcular(repositorio.listarPerfiles(), LocalDateTime.now());
    }

    public int obtenerPosicionRankingActual(UUID donanteId) {
        obtenerPerfil(donanteId);
        return obtenerRankingMensualActual().posicionDe(donanteId);
    }

    public void revisarRachas() {
        repositorio.listarPerfiles().forEach(perfil -> {
            sincronizarMisionesDeCategoria(perfil.getCategoria());
            revisarPerdidaMision(perfil);
            repositorio.guardarPerfil(perfil);
        });
    }

    public void procesarDonacion(UUID donanteId, DonacionRegistrada donacion, String destinatario, String medio) {
        validator.validarCategoriasDonadas(donacion.getCategorias().stream().toList());
        registrarDonacionYVerificarMision(donanteId, donacion, destinatario, medio);
    }

    public void procesarDonacionExitosa(UUID donanteId, DonacionRegistrada donacion, String destinatario, String medio) {
        registrarDonacionYVerificarMision(donanteId, donacion, destinatario, medio);
    }

    private void registrarDonacionYVerificarMision(UUID donanteId, DonacionRegistrada donacion, String destinatario, String medio) {
        Donante perfil = obtenerPerfil(donanteId);
        perfil.registrarDonacion(donacion);
        repositorio.guardarPerfil(perfil);
        verificarMisionActiva(perfil, destinatario, medio);
    }

    private void verificarMisionActiva(Donante perfil, String destinatario, String medio) {
        Mision activa = perfil.getProgresoMision().getMisionActual();
        if (activa == null || !perfil.getProgresoMision().completadaPor(perfil)) {
            return;
        }

        InsigniaObtenida nueva = activa.otorgarInsignia();
        perfil.agregarInsignia(nueva);
        if (destinatario != null && medio != null) {
            notificacionClient.enviarNotificacion(destinatario, "Mision cumplida: " + activa.getNombre(), medio, "MISION_CUMPLIDA");
            n8nWebhookClient.notificarInsigniaObtenida(perfil.getId(), nueva, destinatario);
        }

        avanzarMision(perfil, activa, destinatario, medio);
        repositorio.guardarPerfil(perfil);
    }

    private void avanzarMision(Donante perfil, Mision activa, String destinatario, String medio) {
        Mision siguienteMision = perfil.getCategoria().siguienteMision(activa);
        if (siguienteMision != null) {
            perfil.cambiarMisionActual(siguienteMision);
            return;
        }

        if (perfil.subirCategoria()) {
            sincronizarMisionesDeCategoria(perfil.getCategoria());
            asignarPrimeraMisionDeCategoria(perfil);
            if (destinatario != null && medio != null) {
                notificacionClient.enviarNotificacion(destinatario, "Subiste a " + perfil.getCategoria().getClass().getSimpleName() + ".", medio, "CAMBIO_CATEGORIA");
            }
            return;
        }

        perfil.cambiarMisionActual(null);
    }

    private void asignarPrimeraMisionDeCategoria(Donante perfil) {
        sincronizarMisionesDeCategoria(perfil.getCategoria());
        Mision primeraMision = perfil.getCategoria().primeraMision();
        validator.validarMisionesDisponibles(perfil.getCategoria().getMisiones(), perfil.getCategoria());
        perfil.cambiarMisionActual(primeraMision);
    }

    private void revisarPerdidaMision(Donante perfil) {
        if (perfil.getProgresoMision().getMisionActual() instanceof Racha racha
                && racha.perdioProgreso(perfil, LocalDate.now())) {
            asignarPrimeraMisionDeCategoria(perfil);
        }
    }

    private void asignarCategoria(Donante perfil, CategoriaDonante categoria) {
        perfil.setCategoria(categoria);
        sincronizarMisionesDeCategoria(categoria);
    }

    private void sincronizarMisionesDeCategoria(CategoriaDonante categoria) {
        categoria.setMisiones(repositorio.listarMisionesPorCategoria(categoria));
    }
}
