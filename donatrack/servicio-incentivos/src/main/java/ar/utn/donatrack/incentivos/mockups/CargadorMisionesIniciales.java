package ar.utn.donatrack.incentivos.mockups;

import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Sostenedor;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Transformador;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.misiones.Completitud;
import ar.utn.donatrack.incentivos.models.misiones.DonacionesExitosas;
import ar.utn.donatrack.incentivos.models.misiones.HabilDonador;
import ar.utn.donatrack.incentivos.models.misiones.Racha;
import ar.utn.donatrack.incentivos.repositories.IncentivosRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Carga las misiones base del sistema al arrancar el servicio.
 */
@Component
public class CargadorMisionesIniciales {

    private static final Logger log = LoggerFactory.getLogger(CargadorMisionesIniciales.class);
    private final IncentivosRepository repositorio;

    public CargadorMisionesIniciales(IncentivosRepository repositorio) {
        this.repositorio = repositorio;
    }

    @PostConstruct
    public void inicializarMisiones() {
        repositorio.guardarMision(new DonacionesExitosas(
                "Primera donacion exitosa",
                "Completar una donacion entregada a destino.",
                new Colaborador(),
                1,
                Insignia.builder()
                        .nombre("Semilla de Solidaridad")
                        .imagen("semilla.png")
                        .build()
        ));
        repositorio.guardarMision(new Racha(
                "Racha solidaria",
                "Donar al menos una vez por mes durante tres meses seguidos.",
                new Colaborador(),
                3,
                Insignia.builder()
                        .nombre("Constancia Solidaria")
                        .imagen("racha.png")
                        .build()
        ));
        repositorio.guardarMision(new HabilDonador(
                "Gran Corazon",
                "Donar diez bienes en una unica donacion.",
                new Sostenedor(),
                10,
                Insignia.builder()
                        .nombre("Corazon de Plata")
                        .imagen("plata.png")
                        .build()
        ));
        repositorio.guardarMision(new Completitud(
                "Ayuda Integral",
                "Donar bienes de tres categorias distintas.",
                new Transformador(),
                3,
                Insignia.builder()
                        .nombre("Estrella Dorada")
                        .imagen("oro.png")
                        .build()
        ));

        log.info("Misiones mockup del sistema cargadas correctamente.");
    }
}
