package ar.utn.donatrack.incentivos.services.impl;

import ar.utn.donatrack.incentivos.client.NotificacionClient;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.*;
import ar.utn.donatrack.incentivos.repositories.impl.IncentivosRepositorioEnMemoria;
import ar.utn.donatrack.incentivos.services.IncentivosService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IncentivosServiceImpl implements IncentivosService {

    private static final Logger log = LoggerFactory.getLogger(IncentivosServiceImpl.class);
    private final IncentivosRepositorioEnMemoria repositorio;
    private final NotificacionClient notificacionClient;

    public IncentivosServiceImpl(IncentivosRepositorioEnMemoria repositorio, NotificacionClient notificacionClient) {
        this.repositorio = repositorio;
        this.notificacionClient = notificacionClient;
    }

    @PostConstruct
    public void inicializarMisiones() {
        Mision m1 = new DonacionesExitosas();
        m1.setNombre("Primera donación");
        m1.setCategoriaRequerida(new Colaborador());
        m1.setObjetivo(1);
        repositorio.guardarMision(m1);
        
        log.info("Misiones base del sistema cargadas correctamente.");
    }

    @Override
    public Donante obtenerMetricas(UUID donanteId) {
        return repositorio.obtenerOCrearPerfil(donanteId);
    }

    @Override
    public List<Mision> obtenerMisiones(UUID donanteId) {
        Donante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        if (perfil.getCategoria() == null) {
            perfil.setCategoria(new Colaborador());
        }
        return repositorio.listarMisionesPorCategoria(perfil.getCategoria());
    }

    @Override
    public List<InsigniaObtenida> obtenerInsignias(UUID donanteId) {
        return repositorio.obtenerOCrearPerfil(donanteId).getInsigniasObtenidas();
    }

    @Override
    public void procesarDonacion(UUID donanteId, String destinatario, String medio, int cantidadBienes, List<String> categoriasDonadas) {
        Donante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        perfil.registrarDonacion(cantidadBienes);
        repositorio.guardarPerfil(perfil);
        
        verificarMisionActiva(perfil, destinatario, medio);
    }

    @Override
    public void procesarDonacionExitosa(UUID donanteId, String destinatario, String medio) {
        Donante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        perfil.registrarOrganizacionAyudada();
        repositorio.guardarPerfil(perfil);

        verificarMisionActiva(perfil, destinatario, medio);
    }

    private void verificarMisionActiva(Donante perfil, String destinatario, String medio) {
        Mision activa = perfil.getMisionActual();
        if (activa != null && activa.estaCompletada(perfil)) {
            InsigniaObtenida nueva = new InsigniaObtenida(activa.getInsignia(), true);
            perfil.addInsignia(nueva);
            
            notificacionClient.enviarNotificacion(destinatario, 
                "¡Misión cumplida!: " + activa.getNombre(), medio);
                
            if (perfil.subirCategoria()) {
                notificacionClient.enviarNotificacion(destinatario, 
                    "¡Subiste de categoría! Ahora sos " + perfil.getCategoria().getClass().getSimpleName(), medio);
            }
            repositorio.guardarPerfil(perfil);
        }
    }
}