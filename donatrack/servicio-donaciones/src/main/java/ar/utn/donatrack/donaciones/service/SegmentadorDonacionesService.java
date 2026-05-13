package ar.utn.donatrack.donaciones.service;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;
import ar.utn.donatrack.donaciones.model.donacion.DonacionConEstado;
import ar.utn.donatrack.donaciones.model.donacion.DonacionPerecible;
import ar.utn.donatrack.donaciones.model.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.model.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.model.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.model.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.repository.SegmentadorDonacionesRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SegmentadorDonacionesService {

  private static SegmentadorDonacionesService _instance;
  private final SegmentadorDonacionesRepository segmentadorDonacionesRepository;

  public SegmentadorDonacionesService(SegmentadorDonacionesRepository segmentadorDonacionesRepository) {
    this.segmentadorDonacionesRepository = segmentadorDonacionesRepository;
  }

  public static SegmentadorDonacionesService instance() {
    if (_instance == null) {
      _instance = new SegmentadorDonacionesService(SegmentadorDonacionesRepository.instance());
    }
    return _instance;
  }

  public void segmentar(CargaDonacion carga) {
    List<Bien> listaBienes = carga.getBienes();
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