package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DivisionInvalidaException;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Entrega 6: dividir una donación para repartirla entre varias entidades. */
@DisplayName("Dividir una donación")
class DivisionDeDonacionTest {

    private final UUID donante = UUID.randomUUID();

    private Donacion donacionConArrozYFideos() {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(donante);
        donacion.setSubcategoria(new Subcategoria("Alimentos"));
        donacion.setDescripcion("Alimentos secos");
        donacion.agregarBien(BienPerecible.builder().subcategoria(new Subcategoria("Alimentos"))
                .descripcion("Arroz").cantidad(10).unidad("kg").fechaVencimiento(LocalDate.now().plusMonths(6)).build());
        donacion.agregarBien(BienGenerico.builder().subcategoria(new Subcategoria("Alimentos"))
                .descripcion("Fideos").cantidad(4).unidad("paquetes").build());
        return donacion;
    }

    @Test
    @DisplayName("Pasa la cantidad pedida a una donación nueva de la misma persona, en depósito")
    void separaLaParte() {
        Donacion original = donacionConArrozYFideos();
        UUID arroz = original.getBienes().get(0).getId();
        UUID fideos = original.getBienes().get(1).getId();

        Donacion parte = original.dividir(Map.of(arroz, 4, fideos, 4));

        assertThat(parte.getIdDonante()).isEqualTo(donante);
        assertThat(parte.getEstadoNombre()).isEqualTo(EstadosDonacion.EN_DEPOSITO);
        assertThat(parte.getBienes()).extracting(b -> b.getDescripcion() + ":" + b.getCantidad())
                .containsExactly("Arroz:4", "Fideos:4");
        assertThat(parte.getBienes().get(0)).isInstanceOf(BienPerecible.class);
        assertThat(parte.getBienes().get(0).getId()).isNotEqualTo(arroz);
        // Los fideos pasaron enteros: salen de la original.
        assertThat(original.getBienes()).extracting(b -> b.getDescripcion() + ":" + b.getCantidad())
                .containsExactly("Arroz:6");
        assertThat(parte.resumen()).isEqualTo("4 kg de Arroz, 4 paquetes de Fideos");
    }

    @Test
    @DisplayName("No deja una de las dos partes vacía ni pasa más de lo que hay")
    void cantidadesInvalidas() {
        Donacion original = donacionConArrozYFideos();
        UUID arroz = original.getBienes().get(0).getId();
        UUID fideos = original.getBienes().get(1).getId();

        assertThatThrownBy(() -> original.dividir(Map.of(arroz, 10, fideos, 4)))
                .isInstanceOf(DivisionInvalidaException.class).hasMessageContaining("Tiene que quedar algo");
        assertThatThrownBy(() -> donacionConArrozYFideos().dividir(Map.of(UUID.randomUUID(), 1)))
                .isInstanceOf(DivisionInvalidaException.class).hasMessageContaining("no es de esta donación");
        Donacion otra = donacionConArrozYFideos();
        assertThatThrownBy(() -> otra.dividir(Map.of(otra.getBienes().get(0).getId(), 11)))
                .isInstanceOf(DivisionInvalidaException.class).hasMessageContaining("entre 1 y 10");
    }

    @Test
    @DisplayName("Solo se divide mientras está en depósito")
    void soloEnDeposito() {
        Donacion original = donacionConArrozYFideos();
        UUID arroz = original.getBienes().get(0).getId();
        original.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);

        assertThatThrownBy(() -> original.dividir(Map.of(arroz, 1)))
                .isInstanceOf(DivisionInvalidaException.class).hasMessageContaining("en depósito");
    }
}
