package ar.utn.donatrack.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.DonacionPerecible;
import ar.utn.donatrack.donaciones.model.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.model.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.repositories.SegmentadorDonacionesRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class SegmentadorDonacionesServiceTest {

  private final SegmentadorDonacionesRepository repository = SegmentadorDonacionesRepository.instance();
  private final SegmentadorDonacionesService segmentador = SegmentadorDonacionesService.instance();
  private final Subcategoria subRopa = new Subcategoria("Ropa");
  private final Subcategoria subAlimento = new Subcategoria("Alimento");
  BienPerecible bienPerecible = new BienPerecible(subAlimento, "Leche larga vida", "path/to/leche.jpg", 10, "unidades", LocalDate.of(2025, 1, 1));
  BienPerecible bienPerecible2 = new BienPerecible(subAlimento, "Arroz", "path/to/arroz.jpg", 5, "kg", LocalDate.of(2025, 1, 1));
  BienPerecible bienPerecible3 = new BienPerecible(subAlimento, "Queso", "path/to/queso.jpg", 2, "unidades", LocalDate.of(2025, 2, 1));
  BienConEstado bienConEstado = new BienConEstado(subRopa, "Remera", "path/to/remera.jpg", 5, "unidades", true);
  BienConEstado bienConEstado2 = new BienConEstado(subRopa, "Pantalon", "path/to/pantalon.jpg", 3, "unidades", false);
  BienConEstado bienConEstado3 = new BienConEstado(subRopa, "Camisa", "path/to/camisa.jpg", 4, "unidades", true);

  @BeforeEach
  void beforeEach() {
    this.repository.getDonaciones().clear();
  }

  @AfterEach
  void afterEach() {
    this.repository.getDonaciones().clear();
  }

  @Test
  void listaVacia_retornaListaVacia() {
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of());

    this.segmentador.segmentar(carga);

    assertTrue(this.repository.getDonaciones().isEmpty());
  }

  @Test
  void dosPereciblesMismaFecha_retornaUnaDonacion() {
    CargaDonacion carga = new CargaDonacion();

    carga.setBienes(List.of(bienPerecible, bienPerecible2));

    this.segmentador.segmentar(carga);

    assertEquals(1, this.repository.getDonaciones().size());
    assertEquals(bienPerecible.getFechaVencimiento(), ((DonacionPerecible) this.repository.getDonaciones().getFirst()).getFechaVencimiento());
    assertEquals(2, (this.repository.getDonaciones().getFirst().getBienes().size()));
  }

  @Test
  void dosPereciblesFechasDistintas_retornaDosDoaciones() {
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of(bienPerecible, bienPerecible3));

    this.segmentador.segmentar(carga);

    assertEquals(2, this.repository.getDonaciones().size());
    assertEquals(1, (this.repository.getDonaciones().getFirst().getBienes().size()));
    assertEquals(1, (this.repository.getDonaciones().get(1).getBienes().size()));
  }

  @Test
  void bienesConEstado_retornaUnaDonacionPorEstado() {
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of(bienConEstado, bienConEstado2, bienConEstado3));

    this.segmentador.segmentar(carga);

    assertEquals(2, this.repository.getDonaciones().size());
    assertTrue(Boolean.parseBoolean(String.valueOf((this.repository.getDonaciones().getFirst().getBienes().stream().allMatch(b -> ((BienConEstado) b).isEsNuevo())))));
    assertEquals(2, (this.repository.getDonaciones().getFirst().getBienes().size()));
    assertEquals(1, (this.repository.getDonaciones().get(1).getBienes().size()));
  }

  @Test
  void subcategoriasDistintas_generaDonacionesSeparadas() {
    CargaDonacion carga = new CargaDonacion();
    carga.setBienes(List.of(bienPerecible, bienConEstado, bienPerecible2, bienConEstado2, bienPerecible3, bienConEstado3));

   this.segmentador.segmentar(carga);

    assertEquals(4, this.repository.getDonaciones().size());
    assertEquals(2, (this.repository.getDonaciones().getFirst().getBienes().size()));
    assertEquals(1, (this.repository.getDonaciones().get(1).getBienes().size()));
    assertEquals(2, (this.repository.getDonaciones().get(2).getBienes().size()));
    assertEquals(1, (this.repository.getDonaciones().get(3).getBienes().size()));
  }
}