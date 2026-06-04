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

@Service
@RequiredArgsConstructor
public class SegmentadorDonacionesService implements SegmentadorDonacionesServiceInterface {

  public void segmentar(List<Bien> bienes) {
    List<Donacion> resultado = new ArrayList<>();

    Map<Subcategoria, List<Bien>> porSubcategoria = listaBienes.stream()
        .collect(Collectors.groupingBy(Bien::getSubcategoria, LinkedHashMap::new, Collectors.toList()));

    for (Map.Entry<Subcategoria, List<Bien>> entry : porSubcategoria.entrySet()) {
      Subcategoria sub = entry.getKey();
      List<Bien> bienes = entry.getValue();

      // BienPerecible → agrupar por fechaVencimiento
      bienes.stream()
          .filter(b -> b instanceof BienPerecible)
          .map(b -> (BienPerecible) b)
          .collect(Collectors.groupingBy(BienPerecible::getFechaVencimiento, LinkedHashMap::new, Collectors.toList()))
          .forEach((fecha, grupo) -> {
            DonacionPerecible donacion = new DonacionPerecible();
            donacion.setSubcategoria(sub);
            donacion.setEstado(EstadoDonacion.ASIGNADA);
            donacion.setFechaVencimiento(fecha);
            donacion.setIdCargaOrigen(carga.getIdCargaDonacion());
            donacion.getBienes().addAll(grupo);
            resultado.add(donacion);
          });

      // BienConEstado → agrupar por estado (NUEVO / USADO)
      bienes.stream()
          .filter(b -> b instanceof BienConEstado)
          .map(b -> (BienConEstado) b)
          .collect(Collectors.groupingBy(BienConEstado::isEsNuevo, LinkedHashMap::new, Collectors.toList()))
          .forEach((esNuevo, grupo) -> {
            DonacionConEstado donacion = new DonacionConEstado();
            donacion.setSubcategoria(sub);
            donacion.setEstado(EstadoDonacion.ASIGNADA);
            donacion.setEsNuevo(esNuevo);
            donacion.setIdCargaOrigen(carga.getIdCargaDonacion());
            donacion.getBienes().addAll(grupo);
            resultado.add(donacion);
          });
    }

    this.cargarDonaciones(resultado);
  }

  public void cargarDonaciones(List<Donacion> cargaDonacion){
    this.segmentadorDonacionesRepository.cargarDonaciones(cargaDonacion);
  }
}