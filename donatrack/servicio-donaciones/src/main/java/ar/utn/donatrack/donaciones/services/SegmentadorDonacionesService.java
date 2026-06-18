package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.SegmentadorDonacionesServiceInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquesta la segmentación automática de bienes en múltiples Donaciones independientes,
 * agrupadas por subcategoría y, dentro de ella, por el criterio del tipo de bien:
 *   - BienPerecible  → una Donacion por fecha de vencimiento distinta
 *   - BienConEstado  → una Donacion por estado (nuevo / usado)
 *   - BienGenerico   → una única Donacion por subcategoría
 */
@Service
@RequiredArgsConstructor
public class SegmentadorDonacionesService implements SegmentadorDonacionesServiceInterface {

  private final DonacionesRepositoryInterface donacionesRepository;
  private final DonacionMapper mapper;

  public List<UUID> segmentar(List<BienRequestDTO> bienesDTO, UUID idDonante, String descripcion) {
    List<Bien> bienes = bienesDTO.stream().map(mapper::toBien).toList();
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
            donacion.setEstado(EstadoDonacion.EN_DEPOSITO);
            donacion.getHistorialEstados().add(CambioEstado.builder()
                .estado(EstadoDonacion.EN_DEPOSITO)
                .justificacion("Segmentación automática: perecibles con vencimiento " + fecha)
                .build());
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
            donacion.setEstado(EstadoDonacion.EN_DEPOSITO);
            donacion.getHistorialEstados().add(CambioEstado.builder()
                .estado(EstadoDonacion.EN_DEPOSITO)
                .justificacion("Segmentación automática: bienes con estado " + (esNuevo ? "nuevo" : "usado"))
                .build());
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
        donacion.setEstado(EstadoDonacion.EN_DEPOSITO);
        donacion.getHistorialEstados().add(CambioEstado.builder()
            .estado(EstadoDonacion.EN_DEPOSITO)
            .justificacion("Segmentación automática: bienes genéricos")
            .build());
        donacion.getBienes().addAll(genericos);
        resultado.add(donacion);
      }
    }

    cargarDonaciones(resultado);

    return resultado.stream().map(Donacion::getId).toList();
  }

  public void cargarDonaciones(List<Donacion> donaciones) {
    donacionesRepository.cargarDonaciones(donaciones);
  }
}
