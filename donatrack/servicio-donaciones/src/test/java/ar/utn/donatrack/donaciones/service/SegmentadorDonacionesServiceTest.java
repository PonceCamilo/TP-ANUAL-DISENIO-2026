package ar.utn.donatrack.donaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;
import ar.utn.donatrack.donaciones.model.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.model.donacion.bien.BienPerecible;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class SegmentadorDonacionesServiceTest {

  private SegmentadorDonacionesService segmentador;
  private Subcategoria subRopa;
  private Subcategoria subRopa2;
  private Subcategoria subAlimento;
  private Subcategoria subAlimento2;

  @BeforeEach
  void setUp() {
    segmentador = new SegmentadorDonacionesService();
    subRopa = new Subcategoria("Ropa");
    subAlimento = new Subcategoria("Alimento");
  }

  private BienPerecible crearBienPerecible(Subcategoria subcategoria, LocalDate fechaVencimiento,
                                           String descripcion, int cantidad, String unidad) {
    BienPerecible bien = new BienPerecible(subcategoria, fechaVencimiento);
    bien.setDescripcion(descripcion);
    bien.setFoto("https://test.local/foto.png");
    bien.setCantidad(cantidad);
    bien.setUnidad(unidad);
    return bien;
  }

  private BienConEstado crearBienConEstado(Subcategoria subcategoria, boolean esNuevo,
                                           String descripcion, int cantidad, String unidad) {
    BienConEstado bien = new BienConEstado(subcategoria, esNuevo);
    bien.setDescripcion(descripcion);
    bien.setFoto("https://test.local/foto.png");
    bien.setCantidad(cantidad);
    bien.setUnidad(unidad);
    return bien;
  }

  @Test
  void listaVacia_retornaListaVacia() {
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of());

    assertTrue(segmentador.segmentar(carga).isEmpty());
  }

  @Test
  void dosPereciblesMismaFecha_retornaUnaDonacion() {
    LocalDate fecha = LocalDate.of(2025, 1, 1);
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of(
        crearBienPerecible(subAlimento, fecha, "Leche larga vida", 10, "unidades"),
        crearBienPerecible(subAlimento, fecha, "Arroz", 5, "kg")
    ));

    List<Donacion> resultado = segmentador.segmentar(carga);

    assertEquals(1, resultado.size());
    assertEquals(fecha, resultado.getFirst().getFechaVencimiento());
  }

  @Test
  void dosPereciblesFechasDistintas_retornaDosDoaciones() {
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of(
        crearBienPerecible(subAlimento, LocalDate.of(2025, 1, 1), "Yogur", 12, "unidades"),
        crearBienPerecible(subAlimento, LocalDate.of(2025, 6, 1), "Fideos", 8, "kg")
    ));

    assertEquals(2, segmentador.segmentar(carga).size());
  }

  @Test
  void bienesConEstado_retornaUnaDonacionPorEstado() {
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of(
        crearBienConEstado(subRopa, true, "Campera", 2, "unidades"),
        crearBienConEstado(subRopa, false, "Pantalon", 3, "unidades")
    ));

    List<Donacion> resultado = segmentador.segmentar(carga);

    assertEquals(2, resultado.size());
    assertTrue(resultado.stream().anyMatch(d -> d.isEsNuevo()));
    assertTrue(resultado.stream().anyMatch(d -> !d.isEsNuevo()));
  }

  @Test
  void subcategoriasDistintas_generaDonacionesSeparadas() {
    LocalDate fecha = LocalDate.of(2025, 1, 1);
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of(
        crearBienConEstado(subRopa, true, "Remera", 4, "unidades"),
        crearBienPerecible(subAlimento, fecha, "Galletitas", 6, "kg")
    ));

    List<Donacion> resultado = segmentador.segmentar(carga);

    assertEquals(2, resultado.size());
    assertTrue(resultado.stream().anyMatch(d -> d.getSubcategoria().equals(subRopa)));
    assertTrue(resultado.stream().anyMatch(d -> d.getSubcategoria().equals(subAlimento)));
  }
}