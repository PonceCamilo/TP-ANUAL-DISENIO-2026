package ar.utn.donatrack.incentivos.persistencia;

import ar.utn.donatrack.incentivos.models.DonacionRegistrada;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Sostenedor;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.DonacionesExitosas;
import ar.utn.donatrack.incentivos.models.misiones.HabilDonador;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.models.misiones.Racha;
import ar.utn.donatrack.incentivos.repositories.IncentivosRepository;
import ar.utn.donatrack.incentivos.repositories.jpa.CategoriaDonanteJpaRepository;
import ar.utn.donatrack.incentivos.repositories.jpa.DonanteJpaRepository;
import ar.utn.donatrack.incentivos.repositories.jpa.MisionJpaRepository;
import ar.utn.donatrack.incentivos.repositories.jpa.RankingMensualJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


// Tests del mapeo objeto-relacional de servicio-incentivos 

@DataJpaTest
@Import(IncentivosRepository.class)
@DisplayName("Mapeo objeto-relacional de incentivos")
class MapeoIncentivosTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private IncentivosRepository repositorio;

    @Autowired
    private MisionJpaRepository misiones;

    @Autowired
    private DonanteJpaRepository donantes;

    @Autowired
    private CategoriaDonanteJpaRepository categorias;

    @Autowired
    private RankingMensualJpaRepository rankings;

    // Escribe lo pendiente y vacía la caché, para que la próxima lectura vaya a la base.
    private void vaciarCache() {
        em.flush();
        em.clear();
    }

    private Insignia insignia(String nombre) {
        return Insignia.builder().nombre(nombre).imagen(nombre.toLowerCase() + ".png").build();
    }

    // Una donación registrada, con el id asignado a mano
    private DonacionRegistrada donacion(LocalDateTime fecha, boolean exitosa, int bienes, String... cats) {
        DonacionRegistrada donacion = DonacionRegistrada.builder()
                .fecha(fecha)
                .exitosa(exitosa)
                .cantidadBienes(bienes)
                .categorias(Set.of(cats))
                .entidadBeneficiaria("Comedor Los Andes")
                .build();
        return donacion;
    }

    // ── Herencia de categorías ────────────────────────────────────────────────

    @Nested
    @DisplayName("CategoriaDonante: herencia SINGLE_TABLE")
    class HerenciaDeCategorias {

        @Test
        @DisplayName("Las tres categorías conviven en una sola tabla, distinguidas por el discriminador")
        void tresCategoriasUnaSolaTabla() {
            // SINGLE_TABLE con @DiscriminatorColumn("tipo_categoria"): las tres
            // subclases comparten tabla y se distinguen por una columna de texto.
            categorias.save(new Colaborador());
            categorias.save(new Sostenedor());
            vaciarCache();

            Long filas = (Long) em.getEntityManager()
                    .createQuery("select count(c) from CategoriaDonante c")
                    .getSingleResult();
            assertThat(filas).isEqualTo(2);
        }

        @Test
        @DisplayName("Al recuperar una categoría vuelve con su clase concreta, no con la abstracta")
        void vuelveConSuClaseConcreta() {
            // Es lo que hace que siguienteCategoria() siga funcionando después
            // de leer de la base: si Hibernate devolviera CategoriaDonante
            // "pelada", el polimorfismo se perdería y subir de categoría fallaría
            categorias.save(new Sostenedor());
            vaciarCache();

            CategoriaDonante recuperada = categorias.findById(2).orElseThrow();

            assertThat(recuperada).isInstanceOf(Sostenedor.class);
            assertThat(recuperada.getNombre()).isEqualTo("Sostenedor");
            assertThat(recuperada.siguienteCategoria().getNombre()).isEqualTo("Transformador");
        }

        @Test
        @DisplayName("La clave primaria es el orden, así que cada categoría es única por diseño")
        void elOrdenEsLaClave() {
            categorias.save(new Colaborador());
            vaciarCache();

            assertThat(categorias.findById(1)).isPresent();
            assertThat(categorias.findById(1).orElseThrow().getOrden()).isEqualTo(1);
        }
    }

    // ── Herencia de misiones ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Mision: herencia JOINED")
    class HerenciaDeMisiones {

        @Test
        @DisplayName("Cada tipo de misión vuelve con su clase concreta y su regla de progreso")
        void cadaSubclaseVuelveConSuTipo() {
            CategoriaDonante colaborador = categorias.save(new Colaborador());
            misiones.save(new HabilDonador("Hábil", "20 bienes de una vez", colaborador, 20, insignia("Habil")));
            misiones.save(new DonacionesExitosas("Constante", "5 entregas", colaborador, 5, insignia("Constante")));
            vaciarCache();

            List<Mision> recuperadas = misiones.findAll();

            assertThat(recuperadas).hasSize(2);
            assertThat(recuperadas).hasAtLeastOneElementOfType(HabilDonador.class);
            assertThat(recuperadas).hasAtLeastOneElementOfType(DonacionesExitosas.class);
        }

        @Test
        @DisplayName("Los campos propios de la subclase sobreviven a la ida y vuelta")
        void losCamposDeLaSubclaseSobreviven() {
            CategoriaDonante colaborador = categorias.save(new Colaborador());
            Racha guardada = (Racha) misiones.save(
                    new Racha("Racha de 3", "Tres meses seguidos", colaborador, 3, insignia("Racha")));
            Long id = guardada.getId();
            vaciarCache();

            Racha recuperada = (Racha) misiones.findById(id).orElseThrow();

            assertThat(recuperada.getMesesRequeridos()).isEqualTo(3);
            assertThat(recuperada.getObjetivo()).isEqualTo(3);
        }

        @Test
        @DisplayName("Una Racha recuperada de la base puede calcular su progreso")
        void rachaRecuperadaCalculaProgreso() {
            CategoriaDonante colaborador = categorias.save(new Colaborador());
            Racha guardada = (Racha) misiones.save(
                    new Racha("Racha de 3", "Tres meses seguidos", colaborador, 3, insignia("Racha")));
            Long id = guardada.getId();

            UUID idDonante = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(idDonante);
            donante.setCategoria(colaborador);
            repositorio.guardarPerfil(donante);
            vaciarCache();

            Racha recuperada = (Racha) misiones.findById(id).orElseThrow();
            Donante donanteRecuperado = donantes.findById(idDonante).orElseThrow();

            assertThat(recuperada.getProgresoRacha())
                    .as("El @Embeddable sin campos volvió null")
                    .isNotNull();
            assertThat(recuperada.progresoActual(donanteRecuperado)).isZero();
            assertThat(recuperada.estaCompletada(donanteRecuperado)).isFalse();
        }

        @Test
        @DisplayName("La insignia de la misión se guarda en cascada")
        void laInsigniaViajaEnCascada() {
            CategoriaDonante colaborador = categorias.save(new Colaborador());
            Mision guardada = misiones.save(
                    new HabilDonador("Hábil", "20 bienes", colaborador, 20, insignia("Medalla de bienes")));
            Long id = guardada.getId();
            vaciarCache();

            Mision recuperada = misiones.findById(id).orElseThrow();

            assertThat(recuperada.getInsignia()).isNotNull();
            assertThat(recuperada.getInsignia().getNombre()).isEqualTo("Medalla de bienes");
            assertThat(recuperada.getInsignia().getId()).as("el @GeneratedValue se aplicó").isNotNull();
        }

        @Test
        @DisplayName("La misión queda ligada a la categoría que la requiere, en ambos sentidos")
        void relacionBidireccionalConLaCategoria() {
            CategoriaDonante colaborador = categorias.save(new Colaborador());
            misiones.save(new HabilDonador("Primera", "20 bienes", colaborador, 20, insignia("A")));
            vaciarCache();

            List<Mision> deLaCategoria = misiones.findByCategoriaRequeridaOrdenOrderByOrdenAsc(1);

            assertThat(deLaCategoria).hasSize(1);
            assertThat(deLaCategoria.get(0).getCategoriaRequerida().getOrden()).isEqualTo(1);
        }
    }

    // ── Perfil del donante ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Donante: colecciones y embeddables")
    class PerfilDelDonante {

        @Test
        @DisplayName("El repositorio escribe en la base y NO en el fallback en memoria")
        void escribeEnLaBaseYNoEnMemoria() {
            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(new Colaborador());
            repositorio.guardarPerfil(donante);
            vaciarCache();

            Long filas = (Long) em.getEntityManager()
                    .createQuery("select count(d) from Donante d")
                    .getSingleResult();

            assertThat(filas)
                    .as("El perfil no llegó a la tabla: el repositorio está en modo fallback en memoria")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("El perfil se guarda con su id asignado a mano (no hay @GeneratedValue)")
        void elIdLoAsignaElLlamador() {
            // El id del donante es el MISMO que usa servicio-donaciones: no se genera acá, llega en el request.
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(new Colaborador());

            repositorio.guardarPerfil(donante);
            vaciarCache();

            assertThat(donantes.findById(id)).isPresent();
        }

        @Test
        @DisplayName("Las insignias obtenidas se guardan en cascada y se recuperan con el perfil")
        void insigniasEnCascada() {
            // la insignia obtenida viaja con EL donante.
            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(categorias.save(new Colaborador()));
            donante.agregarInsignia(new InsigniaObtenida(em.persistFlushFind(insignia("Primera donación")), true));

            repositorio.guardarPerfil(donante);
            vaciarCache();

            Donante recuperado = donantes.findById(id).orElseThrow();

            assertThat(recuperado.getInsigniasObtenidas()).hasSize(1);
            assertThat(recuperado.getInsigniasObtenidas().get(0).getInsignia().getNombre())
                    .isEqualTo("Primera donación");
            assertThat(recuperado.getInsigniasObtenidas().get(0).isVisibilidad()).isTrue();
        }

        @Test
        @DisplayName("Subir de categoría no rompe al guardar, aunque la categoría nueva sea un objeto nuevo")
        void subirCategoriaYGuardar() {
            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(categorias.save(new Colaborador()));
            repositorio.guardarPerfil(donante);
            vaciarCache();

            Donante recuperado = donantes.findById(id).orElseThrow();
            assertThat(recuperado.subirCategoria()).isTrue();
            repositorio.guardarPerfil(recuperado);
            vaciarCache();

            Donante despues = donantes.findById(id).orElseThrow();
            assertThat(despues.getCategoria()).isInstanceOf(Sostenedor.class);
            assertThat(despues.getCategoria().getOrden()).isEqualTo(2);
        }

        @Test
        @DisplayName("Un donante sin misión actual se puede leer y operar sin NullPointerException")
        void progresoMisionNuncaVuelveNull() {
            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(categorias.save(new Colaborador()));
            repositorio.guardarPerfil(donante);
            vaciarCache();

            Donante recuperado = donantes.findById(id).orElseThrow();

            assertThat(recuperado.getProgresoMision())
                    .as("Hibernate anuló el @Embedded: falta el @PostLoad que lo rehidrata")
                    .isNotNull();
            assertThat(recuperado.getProgresoMision().getMisionActual()).isNull();
        }

        @Test
        @DisplayName("El ProgresoMision embebido sobrevive y conserva la misión actual")
        void progresoMisionEmbebido() {
            CategoriaDonante colaborador = categorias.save(new Colaborador());
            Mision mision = misiones.save(
                    new HabilDonador("Hábil", "20 bienes", colaborador, 20, insignia("A")));

            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(colaborador);
            donante.cambiarMisionActual(mision);
            repositorio.guardarPerfil(donante);
            vaciarCache();

            Donante recuperado = donantes.findById(id).orElseThrow();

            assertThat(recuperado.getProgresoMision()).isNotNull();
            assertThat(recuperado.getProgresoMision().getMisionActual()).isNotNull();
            assertThat(recuperado.getProgresoMision().getMisionActual().getNombre()).isEqualTo("Hábil");
        }
    }

    // ── Donaciones registradas ────────────────────────────────────────────────

    @Nested
    @DisplayName("DonacionRegistrada: colección de categorías")
    class DonacionesRegistradas {

        @Test
        @DisplayName("Las categorías donadas se guardan en su tabla y vuelven completas")
        void categoriasEnElementCollection() {
            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(categorias.save(new Colaborador()));
            donante.registrarDonacion(donacion(LocalDateTime.now(), false, 20, "Alimentos", "Higiene"));

            repositorio.guardarPerfil(donante);
            vaciarCache();

            Donante recuperado = donantes.findById(id).orElseThrow();

            assertThat(recuperado.getDonaciones()).hasSize(1);
            assertThat(recuperado.getDonaciones().get(0).getCategorias())
                    .containsExactlyInAnyOrder("Alimentos", "Higiene");
        }

        @Test
        @DisplayName("Los dos tipos de evento (registro y entrega) conviven como filas distintas")
        void registroYEntregaSonFilasDistintas() {
            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(categorias.save(new Colaborador()));
            donante.registrarDonacion(donacion(LocalDateTime.now(), false, 20, "Alimentos"));
            donante.registrarDonacion(donacion(LocalDateTime.now(), true, 20, "Alimentos"));

            repositorio.guardarPerfil(donante);
            vaciarCache();

            Donante recuperado = donantes.findById(id).orElseThrow();

            assertThat(recuperado.getDonaciones()).hasSize(2);
            assertThat(recuperado.getDonaciones()).extracting(DonacionRegistrada::isExitosa)
                    .containsExactlyInAnyOrder(true, false);
        }

        @Test
        @DisplayName("Una donación sin categorías devuelve un set vacío, no null")
        void sinCategoriasDevuelveVacio() {
            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(categorias.save(new Colaborador()));
            donante.registrarDonacion(DonacionRegistrada.builder()
                    .fecha(LocalDateTime.now())
                    .exitosa(false)
                    .cantidadBienes(3)
                    .build());

            repositorio.guardarPerfil(donante);
            vaciarCache();

            Donante recuperado = donantes.findById(id).orElseThrow();

            assertThat(recuperado.getDonaciones().get(0).getCategorias()).isEmpty();
        }
    }

    // ── Rankings ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RankingMensual: posiciones en cascada")
    class Rankings {

        @Test
        @DisplayName("El ranking se guarda con sus posiciones y las recupera ordenadas")
        void rankingConPosiciones() {
            CategoriaDonante colaborador = categorias.save(new Colaborador());
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();

            Donante a = new Donante();
            a.setId(idA);
            a.setCategoria(colaborador);
            a.registrarDonacion(donacion(LocalDateTime.now(), false, 30, "Alimentos"));
            repositorio.guardarPerfil(a);

            Donante b = new Donante();
            b.setId(idB);
            b.setCategoria(colaborador);
            repositorio.guardarPerfil(b);
            vaciarCache();

            RankingMensual ranking = RankingMensual.calcular(
                    List.of(donantes.findById(idA).orElseThrow(), donantes.findById(idB).orElseThrow()),
                    LocalDateTime.now());
            Long id = rankings.save(ranking).getId();
            vaciarCache();

            RankingMensual recuperado = rankings.findById(id).orElseThrow();

            assertThat(recuperado.getPosiciones()).hasSize(2);
            assertThat(recuperado.topTres()).extracting(p -> p.getPuesto()).containsExactly(1, 2);
        }

        @Test
        @DisplayName("Cada posición conserva a qué donante corresponde")
        void laPosicionRecuerdaAlDonante() {
            CategoriaDonante colaborador = categorias.save(new Colaborador());
            UUID idDonante = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(idDonante);
            donante.setCategoria(colaborador);
            repositorio.guardarPerfil(donante);
            vaciarCache();

            RankingMensual ranking = RankingMensual.calcular(
                    List.of(donantes.findById(idDonante).orElseThrow()), LocalDateTime.now());
            Long id = rankings.save(ranking).getId();
            vaciarCache();

            RankingMensual recuperado = rankings.findById(id).orElseThrow();

            assertThat(recuperado.posicionDe(idDonante)).isEqualTo(1);
        }

        @Test
        @DisplayName("Se puede buscar el ranking de un período, que es cómo lo consulta el job mensual")
        void busquedaPorPeriodo() {
            LocalDateTime fecha = LocalDateTime.of(2026, 3, 15, 10, 0);
            rankings.save(RankingMensual.calcular(List.of(), fecha));
            vaciarCache();

            LocalDateTime inicioDeMarzo = LocalDateTime.of(2026, 3, 1, 0, 0);

            assertThat(rankings.findByPeriodoInicio(inicioDeMarzo)).isPresent();
        }
    }
}
