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
import java.util.Objects;
import java.util.UUID;

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
    return mapper.toDTOList(registrarCarga(mapper.toCargaDonacion(dto), donante));
  }

  public List<Donacion> segmentar(CargaDonacion carga) {
    return registrarCarga(carga, donanteRepository.buscarPorId(carga.getIdDonante()).orElse(null));
  }

  private List<Donacion> registrarCarga(CargaDonacion carga, PersonaDonante donante) {
    List<Donacion> resultado = carga.segmentar();

    cargarDonaciones(resultado);
    if (donante != null) {
      // Donar es interactuar con la plataforma: reinicia el plazo de inactividad.
      donante.registrarInteraccion();
      donanteRepository.guardar(donante);
    }
    if (donante != null) {
      // El aviso a incentivos sale después de confirmar (AvisosOyente).
      eventos.publishEvent(new DonacionesRegistradas(donante.getId(), carga.getBienes().size(),
          categorias(carga.getBienes()), FechaHoraArgentina.ahora()));
    }

    return resultado;
  }

  public void cargarDonaciones(List<Donacion> donaciones) {
    donacionesRepository.cargarDonaciones(donaciones);
  }

  private static List<String> categorias(List<Bien> bienes) {
    return bienes.stream()
        .map(Bien::getSubcategoria)
        .filter(Objects::nonNull)
        .map(Subcategoria::getTipo)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }
}
