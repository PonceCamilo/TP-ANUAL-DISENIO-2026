package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.SegmentadorDonacionesServiceInterface;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SegmentadorDonacionesService implements SegmentadorDonacionesServiceInterface {

  private final DonacionesRepositoryInterface donacionesRepository;
  private final PersonaDonanteRepositoryInterface donanteRepository;
  private final IncentivosClient incentivosClient;

  public List<Donacion> segmentar(CargaDonacion carga) {
    List<Donacion> resultado = carga.segmentar();

    cargarDonaciones(resultado);
    notificarIncentivos(carga.getIdDonante(), carga.getBienes());

    return resultado;
  }

  public void cargarDonaciones(List<Donacion> donaciones) {
    donacionesRepository.cargarDonaciones(donaciones);
  }

  private void notificarIncentivos(UUID idDonante, List<Bien> bienes) {
    PersonaDonante donante = donanteRepository.obtenerPersona(idDonante);
    if (donante == null || donante.obtenerEmail() == null) {
      return;
    }
    List<String> categorias = bienes.stream()
        .map(Bien::getSubcategoria)
        .filter(Objects::nonNull)
        .map(Subcategoria::getTipo)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    incentivosClient.notificarDonacionRegistrada(idDonante, donante.obtenerEmail(), "EMAIL", bienes.size(), categorias);
  }
}
