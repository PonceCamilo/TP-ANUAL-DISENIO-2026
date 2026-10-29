package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionesRegistradas;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.DonanteBloqueadoException;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.request.DonacionRequestDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class SegmentadorDonacionesService {

    private final RepositorioDonaciones donacionesRepository;
    private final RepositorioPersonasDonantes donanteRepository;
    private final ApplicationEventPublisher eventos;
    private final PersonasValidator personasValidator;
    private final DonacionMapper mapper;

    /**
     * Registra una carga de donación: valida a la persona donante, segmenta los
     * bienes en donaciones, las guarda y avisa a incentivos. Antes este flujo
     * estaba repartido en el controller.
     *
     * Una persona donante BLOQUEADA no puede registrar donaciones (422).
     */
    public List<DonacionResponseDTO> registrar(DonacionRequestDTO dto) {
        PersonaDonante donante = personasValidator.validarYObtenerPersona(dto.getIdDonante());
        if (donante.estaBloqueada()) {
            throw new DonanteBloqueadoException(donante.getId());
        }
        CargaDonacion carga = mapper.toCargaDonacion(dto);
        List<Donacion> donaciones = carga.segmentar();
        donacionesRepository.cargarDonaciones(donaciones);

        // Donar es interactuar con la plataforma: reinicia el plazo de inactividad.
        donante.registrarInteraccion();
        donanteRepository.guardar(donante);

        // El aviso a incentivos sale después de confirmar (AvisosOyente).
        eventos.publishEvent(new DonacionesRegistradas(donante.getId(), carga.getBienes().size(),
                categorias(carga.getBienes()), FechaHoraArgentina.ahora()));

        return mapper.toDTOList(donaciones);
    }

    /** Las subcategorías de la carga sin repetir: Incentivos cuenta categorías distintas. */
    private static List<String> categorias(List<Bien> bienes) {
        return Subcategoria.tiposDistintos(bienes.stream().map(Bien::getSubcategoria).toList());
    }
}
