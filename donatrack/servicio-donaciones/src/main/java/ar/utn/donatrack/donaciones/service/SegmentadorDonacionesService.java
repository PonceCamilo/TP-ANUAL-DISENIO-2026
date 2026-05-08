package ar.utn.donatrack.donaciones.service;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;
import ar.utn.donatrack.donaciones.model.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.model.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.model.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.repository.SegmentadorDonacionesRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SegmentadorDonacionesService {

  public void cargarDonaciones(List<Donacion> donaciones){

    SegmentadorDonacionesRepository.cargarDonaciones(donaciones);
  }

  public List<Donacion> segmentar(CargaDonacion carga) {
    List<Bien> listaBienes = carga.getBienes();
    List<Donacion> resultado = new ArrayList<>();

    Map<Subcategoria, List<Bien>> porSubcategoria = listaBienes.stream()
        .collect(Collectors.groupingBy(Bien::getSubcategoria));

    for (Map.Entry<Subcategoria, List<Bien>> entry : porSubcategoria.entrySet()) {
      Subcategoria sub = entry.getKey();
      List<Bien> bienes = entry.getValue();

      // BienPerecible → agrupar por fechaVencimiento
      bienes.stream()
          .filter(b -> b instanceof BienPerecible)
          .map(b -> (BienPerecible) b)
          .collect(Collectors.groupingBy(BienPerecible::getFechaVencimiento))
          .forEach((fecha, grupo) -> {
            Donacion donacion = new Donacion();
            donacion.setSubcategoria(sub);
            donacion.setFechaVencimiento(fecha);
            donacion.setCargaOrigen(carga);
            resultado.add(donacion);
          });

      // BienConEstado → agrupar por estado (NUEVO / USADO)
      bienes.stream()
          .filter(b -> b instanceof BienConEstado)
          .map(b -> (BienConEstado) b)
          .collect(Collectors.groupingBy(BienConEstado::isEsNuevo))
          .forEach((esNuevo, grupo) -> {
            Donacion donacion = new Donacion();
            donacion.setSubcategoria(sub);
            donacion.setEsNuevo(esNuevo);
            donacion.setCargaOrigen(carga);
            resultado.add(donacion);
          });
    }

    return resultado;
  }
}