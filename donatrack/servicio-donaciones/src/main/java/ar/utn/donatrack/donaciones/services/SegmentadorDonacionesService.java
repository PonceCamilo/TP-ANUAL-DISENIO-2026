package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.SegmentadorDonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.SegmentadorDonacionesServiceInterface;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orquesta la segmentación automática de una CargaDonacion en múltiples Donaciones
 * independientes, agrupadas por subcategoría y, dentro de ella, por el criterio
 * que corresponda al tipo de bien:
 *   - BienPerecible  → una Donacion por fecha de vencimiento distinta
 *   - BienConEstado  → una Donacion por estado (nuevo / usado)
 *   - Bien genérico  → una única Donacion por subcategoría
 */

@Service
@RequiredArgsConstructor
public class SegmentadorDonacionesService implements SegmentadorDonacionesServiceInterface {

  private final SegmentadorDonacionesRepositoryInterface segmentadorDonacionesRepository;

  @Override
  public void segmentar(CargaDonacion carga) {
    List<Donacion> resultado = new ArrayList<>();

    // Agrupar todos los bienes de la carga por subcategoría
    Map<Subcategoria, List<Bien>> porSubcategoria = carga.getBienes().stream()
        .collect(Collectors.groupingBy(
            Bien::getSubcategoria,
            LinkedHashMap::new,
            Collectors.toList()
        ));

    for (Map.Entry<Subcategoria, List<Bien>> entry : porSubcategoria.entrySet()) {
      Subcategoria sub = entry.getKey();
      List<Bien> bienesDeSubcat = entry.getValue();

      // BienPerecible → agrupar por fechaVencimiento
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
              donacion.setEstado(EstadoDonacion.EN_DEPOSITO);
              donacion.setIdCargaOrigen(carga.getIdCargaDonacion());
              donacion.getBienes().addAll(grupo);
              donacion.setSubcategoria(sub);
            resultado.add(donacion);
          });

      // BienConEstado → agrupar por estado nuevo/usado
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
              donacion.setSubcategoria(sub);
              donacion.setEstado(EstadoDonacion.EN_DEPOSITO);
              donacion.setIdCargaOrigen(carga.getIdCargaDonacion());
              donacion.getBienes().addAll(grupo);
            resultado.add(donacion);
          });

      // Bienes genéricos (ni perecederos ni con estado) → una sola donación por subcategoría
      List<Bien> genericos = bienesDeSubcat.stream()
          .filter(b -> !(b instanceof BienPerecible) && !(b instanceof BienConEstado))
          .toList();

      if (!genericos.isEmpty()) {
        Donacion donacion = new Donacion();
        donacion.setSubcategoria(sub);
        donacion.setEstado(EstadoDonacion.EN_DEPOSITO);
        donacion.setIdCargaOrigen(carga.getIdCargaDonacion());
        donacion.getBienes().addAll(genericos);
        resultado.add(donacion);
      }
    }

    cargarDonaciones(resultado);
  }

  public void cargarDonaciones(List<Donacion> donaciones) {
    segmentadorDonacionesRepository.cargarDonaciones(donaciones);
  }
}
