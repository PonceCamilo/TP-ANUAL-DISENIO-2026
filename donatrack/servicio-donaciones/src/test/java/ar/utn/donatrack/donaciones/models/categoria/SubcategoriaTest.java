package ar.utn.donatrack.donaciones.models.categoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de Subcategoria, la unidad mínima de asignación del sistema.
 *
 * Su igualdad se basa en el tipo NORMALIZADO (sin espacios extra y en minúsculas)
 * porque la segmentación agrupa los bienes usando la subcategoría como clave de
 * un Map: si "Arroz" y "arroz" no fueran iguales, una misma carga generaría
 * donaciones duplicadas por simples diferencias de tipeo del usuario.
 */
@DisplayName("Subcategoria - normalización e igualdad")
class SubcategoriaTest {

    @Test
    @DisplayName("Normaliza también acentos y espacios internos (como el matching de necesidades)")
    void normalizaAcentosYEspacios() {
        assertThat(new Subcategoria("Ropa   de Abrígo")).isEqualTo(new Subcategoria("ropa de abrigo"));
    }

    @Test
    @DisplayName("coincideCon() compara normalizado y una vacía no coincide con nada")
    void coincideCon() {
        assertThat(new Subcategoria("Arroz").coincideCon(new Subcategoria(" arroz "))).isTrue();
        assertThat(new Subcategoria("Arroz").coincideCon(new Subcategoria("fideos"))).isFalse();
        assertThat(new Subcategoria("  ").coincideCon(new Subcategoria("  "))).isFalse();
        assertThat(new Subcategoria("Arroz").coincideCon(null)).isFalse();
    }

    @Test
    @DisplayName("estaMencionadaEn() busca la subcategoría dentro de un texto")
    void estaMencionadaEn() {
        assertThat(new Subcategoria("arroz").estaMencionadaEn("Bolsas de ARROZ de 1 kg")).isTrue();
        assertThat(new Subcategoria("arroz").estaMencionadaEn(null)).isFalse();
        assertThat(new Subcategoria("").estaMencionadaEn("cualquier cosa")).isFalse();
    }

    @Test
    @DisplayName("Conserva el texto original tal como lo escribió el usuario")
    void conservaElTextoOriginal() {
        // La normalización es solo para comparar: lo que se muestra sigue siendo
        // lo que la persona cargó.
        assertThat(new Subcategoria("Arroz Largo Fino").getTipo()).isEqualTo("Arroz Largo Fino");
    }

    @Test
    @DisplayName("Guarda el texto sin espacios al principio ni al final")
    void recortaLosExtremos() {
        assertThat(new Subcategoria("  Arroz largo  ").getTipo()).isEqualTo("Arroz largo");
    }

    @Test
    @DisplayName("tiposDistintos() no repite una subcategoría escrita distinto y conserva la primera forma")
    void tiposDistintos() {
        assertThat(Subcategoria.tiposDistintos(Arrays.asList(new Subcategoria("Arroz"), new Subcategoria(" arroz "),
                null, new Subcategoria(" "), new Subcategoria("ARROZ"), new Subcategoria("Fideos"))))
                .containsExactly("Arroz", "Fideos");
    }

    @Test
    @DisplayName("Normaliza el tipo a minúsculas y sin espacios en los extremos")
    void normalizaElTipo() {
        assertThat(new Subcategoria("  ARROZ  ").getTipoNormalizado()).isEqualTo("arroz");
    }

    @Test
    @DisplayName("Dos subcategorías que solo difieren en mayúsculas son iguales")
    void igualdadIgnoraCase() {
        assertThat(new Subcategoria("Arroz")).isEqualTo(new Subcategoria("arroz"));
    }

    @Test
    @DisplayName("Dos subcategorías que solo difieren en espacios son iguales")
    void igualdadIgnoraEspacios() {
        assertThat(new Subcategoria("  arroz ")).isEqualTo(new Subcategoria("arroz"));
    }

    @Test
    @DisplayName("Subcategorías con tipos distintos NO son iguales")
    void tiposDistintosNoSonIguales() {
        assertThat(new Subcategoria("arroz")).isNotEqualTo(new Subcategoria("fideos"));
    }

    @Test
    @DisplayName("Subcategorías iguales comparten hashCode, por lo que funcionan como clave de un Map")
    void funcionaComoClaveDeMap() {
        // Ésta es la propiedad de la que depende la segmentación: si el hashCode
        // no coincidiera, el groupingBy crearía dos grupos separados.
        Map<Subcategoria, String> mapa = new HashMap<>();
        mapa.put(new Subcategoria("Arroz"), "grupo A");
        mapa.put(new Subcategoria("  arroz  "), "grupo A actualizado");

        assertThat(mapa).hasSize(1);
        assertThat(mapa.get(new Subcategoria("ARROZ"))).isEqualTo("grupo A actualizado");
    }

    @Test
    @DisplayName("Un tipo nulo se normaliza a cadena vacía y no rompe la igualdad")
    void tipoNuloNoRompe() {
        // Caso borde defensivo: sin esta guarda, comparar una subcategoría sin tipo
        // lanzaría NullPointerException durante la segmentación.
        Subcategoria sinTipo = new Subcategoria(null);

        assertThat(sinTipo.getTipoNormalizado()).isEmpty();
        assertThat(sinTipo).isEqualTo(new Subcategoria(null));
    }

    @Test
    @DisplayName("toString() devuelve el tipo original, útil en logs y mensajes de error")
    void toStringDevuelveElTipo() {
        assertThat(new Subcategoria("Arroz").toString()).isEqualTo("Arroz");
    }
}
