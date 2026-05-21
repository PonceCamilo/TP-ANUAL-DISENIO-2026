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

  private final SegmentadorDonacionesRepository repository = new SegmentadorDonacionesRepository();
  private final SegmentadorDonacionesService segmentador = new SegmentadorDonacionesService(repository);
  private final Subcategoria subRopa = new Subcategoria("Ropa");
  private final Subcategoria subAlimento = new Subcategoria("Alimento");

  BienPerecible bienPerecible = BienPerecible.builder()
      .subcategoria(subAlimento)
      .descripcion("Leche larga vida")
      .foto("path/to/leche.jpg")
      .cantidad(10)
      .unidad("unidades")
      .fechaVencimiento(LocalDate.of(2025, 1, 1))
      .build();
  BienPerecible bienPerecible2 = BienPerecible.builder()
      .subcategoria(subAlimento)
      .descripcion("Arroz")
      .foto("path/to/arroz.jpg")
      .cantidad(5)
      .unidad("kg")
      .fechaVencimiento(LocalDate.of(2025, 1, 1))
      .build();
  BienPerecible bienPerecible3 = BienPerecible.builder()
      .subcategoria(subAlimento)
      .descripcion("Queso")
      .foto("path/to/queso.jpg")
      .cantidad(2)
      .unidad("unidades")
      .fechaVencimiento(LocalDate.of(2025, 2, 1))
      .build();
  BienConEstado bienConEstado = BienConEstado.builder()
      .subcategoria(subRopa)
      .descripcion("Remera")
      .foto("path/to/remera.jpg")
      .cantidad(5)
      .unidad("unidades")
      .esNuevo(true)
      .build();
  BienConEstado bienConEstado2 = BienConEstado.builder()
      .subcategoria(subRopa)
      .descripcion("Pantalon")
      .foto("path/to/pantalon.jpg")
      .cantidad(3)
      .unidad("unidades")
      .esNuevo(false)
      .build();
  BienConEstado bienConEstado3 = BienConEstado.builder()
      .subcategoria(subRopa)
      .descripcion("Camisa")
      .foto("path/to/camisa.jpg")
      .cantidad(4)
      .unidad("unidades")
      .esNuevo(true)
      .build();

  @BeforeEach
  void beforeEach() {
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