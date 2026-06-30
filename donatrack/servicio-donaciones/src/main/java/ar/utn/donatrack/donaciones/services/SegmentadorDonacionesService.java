package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.SegmentadorDonacionesServiceInterface;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SegmentadorDonacionesService implements SegmentadorDonacionesServiceInterface {

  private final DonacionesRepositoryInterface donacionesRepository;
  private final PersonaDonanteRepositoryInterface donanteRepository;
  private final IncentivosClient incentivosClient;

  public List<Donacion> segmentar(CargaDonacion carga) {
    List<Bien> bienes = carga.getBienes();
    UUID idDonante = carga.getIdDonante();
    String descripcion = carga.getDescripcion();

    List<Donacion> resultado = new ArrayList<>();

    Map<Subcategoria, List<Bien>> porSubcategoria = bienes.stream()
        .collect(Collectors.groupingBy(
            Bien::getSubcategoria,
            LinkedHashMap::new,
            Collectors.toList()
        ));

    for (Map.Entry<Subcategoria, List<Bien>> entry : porSubcategoria.entrySet()) {
      Subcategoria sub = entry.getKey();
      List<Bien> bienesDeSubcat = entry.getValue();

      bienesDeSubcat.stream()
          .filter(BienPerecible.class::isInstance)
          .map(b -> (BienPerecible) b)
          .collect(Collectors.groupingBy(
              BienPerecible::getFechaVencimiento,
              LinkedHashMap::new,
              Collectors.toList()
          ))
          .forEach((fecha, grupo) -> {
            Donacion donacion = new Donacion();
            donacion.setIdDonante(idDonante);
            donacion.setDescripcion(descripcion);
            donacion.setSubcategoria(sub);
            donacion.getBienes().addAll(grupo);
            resultado.add(donacion);
          });

      bienesDeSubcat.stream()
          .filter(BienConEstado.class::isInstance)
          .map(b -> (BienConEstado) b)
          .collect(Collectors.groupingBy(
              BienConEstado::isEsNuevo,
              LinkedHashMap::new,
              Collectors.toList()
          ))
          .forEach((esNuevo, grupo) -> {
            Donacion donacion = new Donacion();
            donacion.setIdDonante(idDonante);
            donacion.setDescripcion(descripcion);
            donacion.setSubcategoria(sub);
            donacion.getBienes().addAll(grupo);
            resultado.add(donacion);
          });

      List<Bien> genericos = bienesDeSubcat.stream()
          .filter(b -> !(b instanceof BienPerecible) && !(b instanceof BienConEstado))
          .toList();

      if (!genericos.isEmpty()) {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(idDonante);
        donacion.setDescripcion(descripcion);
        donacion.setSubcategoria(sub);
        donacion.getBienes().addAll(genericos);
        resultado.add(donacion);
      }
    }

    cargarDonaciones(resultado);
    notificarIncentivos(idDonante, bienes);

    return resultado;
  }

  public void cargarDonaciones(List<Donacion> donaciones) {
    donacionesRepository.cargarDonaciones(donaciones);
  }

  private void notificarIncentivos(UUID idDonante, List<Bien> bienes) {
    PersonaDonante donante = donanteRepository.obtenerPersona(idDonante);
    if (donante == null || donante.getEmail() == null) {
      return;
    }
    List<String> categorias = bienes.stream()
        .map(Bien::getSubcategoria)
        .filter(Objects::nonNull)
        .map(Subcategoria::getTipo)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    incentivosClient.notificarDonacionRegistrada(idDonante, donante.getEmail(), "EMAIL", bienes.size(), categorias);
  }
}
