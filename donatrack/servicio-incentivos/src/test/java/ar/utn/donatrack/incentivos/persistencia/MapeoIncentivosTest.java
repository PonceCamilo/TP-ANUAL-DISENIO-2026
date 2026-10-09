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

/**
 * Tests del mapeo objeto-relacional de servicio-incentivos (Entrega 4).
 *
 * POR QUÉ EXISTEN: hasta la Entrega 4 este servicio tenía 15 clases @Entity y
 * ninguna base de datos — ni driver ni datasource. El mapeo nunca se había
 * ejecutado contra un motor real, así que un @ManyToOne mal puesto o una
 * cascada faltante no se detectaba de ninguna forma.
 *
 * QUÉ SE PRUEBA ACÁ Y NO EN LOS TESTS DE DOMINIO: los tests existentes
 * (DonanteTest, MisionesTest, RankingMensualTest) trabajan con objetos en
 * memoria y verifican reglas de negocio. Lo que no pueden ver es si esos
 * objetos SOBREVIVEN a una ida y vuelta a la base. Por eso cada test de acá
 * guarda, LIMPIA EL CONTEXTO DE PERSISTENCIA y vuelve a leer.
 *
 * El flush() + clear() no es decorativo: sin él, find() devuelve la MISMA
 * instancia que quedó en la caché de primer nivel y el test pasa sin haber
 * leído una sola fila. Es la trampa clásica de los tests de JPA.
 */
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

    /** Escribe lo pendiente y vacía la caché, para que la próxima lectura vaya a la base. */
    private void vaciarCache() {
        em.flush();
        em.clear();
    }

    private Insignia insignia(String nombre) {
        return Insignia.builder().nombre(nombre).imagen(nombre.toLowerCase() + ".png").build();
    }

    /** Una donación registrada, con el id asignado a mano (ver el test del builder). */
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
            // Es la estrategia correcta acá porque las subclases no agregan
            // campos propios, solo comportamiento (siguienteCategoria()).
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
            // "pelada", el polimorfismo se perdería y subir de categoría
            // fallaría con un NPE o con la categoría equivocada.
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
            // @Id private int orden: new Colaborador() siempre tiene PK 1. Eso
            // vuelve imposible duplicar una categoría, pero obliga a reusar la
            // fila existente en lugar de insertar (ver guardarORecuperarCategoria).
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
            // JOINED y no SINGLE_TABLE: acá sí cada subclase agrega campos
            // propios (mesesRequeridos, cantidadBienesRequerida...), y con una
            // sola tabla todos tendrían que ser nullable.
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
            // Si la tabla de la subclase no se generara, este campo volvería en 0
            // y la misión sería imposible de completar (o trivial).
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
            // Racha tiene un @Embedded ProgresoRacha SIN NINGÚN CAMPO: solo
            // métodos. Un embeddable sin columnas es el caso donde más fácil
            // Hibernate devuelve null, y progresoActual() lo invoca directo.
            //
            // Si este test falla con NPE, Racha necesita el mismo @PostLoad que
            // se le puso a Donante.progresoMision.
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
            // Sin donaciones el progreso es 0, pero lo que importa es que no explote.
            assertThat(recuperada.progresoActual(donanteRecuperado)).isZero();
            assertThat(recuperada.estaCompletada(donanteRecuperado)).isFalse();
        }

        @Test
        @DisplayName("La insignia de la misión se guarda en cascada")
        void laInsigniaViajaEnCascada() {
            // @OneToOne(cascade = ALL): sin la cascada, guardar la misión
            // fallaría porque la insignia quedaría en estado transient.
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
            // CategoriaDonante.misiones está mappedBy "categoriaRequerida": el
            // dueño de la relación es la misión. Si el mappedBy estuviera mal,
            // Hibernate crearía una tabla intermedia y primeraMision() volvería
            // vacía.
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
            // ESTE TEST ENCONTRÓ UN BUG REAL. IncentivosRepository tiene dos
            // constructores: uno sin argumentos (modo en memoria, solo para
            // IncentivosServiceTest) y uno con los repositorios JPA. Cuando una
            // clase tiene varios constructores y ninguno lleva @Autowired,
            // Spring elige el SIN ARGUMENTOS. Resultado: la aplicación corría
            // entera en memoria, los endpoints devolvían 201 y la base quedaba
            // vacía. Se verificó contra el contenedor: POST /donantes/donacion
            // respondía OK y donante seguía en 0 filas.
            //
            // Si este test vuelve a fallar, lo primero que hay que mirar es si
            // el @Autowired sigue en el constructor de tres argumentos.
            UUID id = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(id);
            donante.setCategoria(new Colaborador());
            repositorio.guardarPerfil(donante);
            vaciarCache();

            // Contra la tabla directamente: si el repositorio guardó en el Map
            // interno, acá hay cero filas aunque guardarPerfil no haya fallado.
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
            // El id del donante es el MISMO que usa servicio-donaciones: no se
            // genera acá, llega en el request. Por eso @Id sin @GeneratedValue.
            UUID id = UUID.randomUUID();
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
            // @OneToMany(cascade = ALL, fetch = EAGER): la insignia obtenida no
            // tiene vida propia fuera del donante, así que viaja con él.
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
            // ESTE ES EL CASO DELICADO. subirCategoria() hace
            // `this.categoriaActual = new Sostenedor()`: un objeto TRANSIENT con
            // la PK 2. El @ManyToOne no tiene cascada, así que guardar así
            // fallaría con TransientObjectException... o duplicaría la PK.
            //
            // Lo que lo salva es guardarORecuperarCategoria() en el repositorio,
            // que busca la fila por `orden` y reusa la existente. Este test
            // existe para que esa pieza no se borre por parecer redundante.
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
            // SEGUNDO BUG QUE ENCONTRÓ ESTE TEST. ProgresoMision es un @Embedded
            // cuya única columna es la misión actual. Con esa columna en NULL
            // —un donante recién creado— Hibernate deja el embeddable ENTERO en
            // null, pisando el `= new ProgresoMision()` del campo, que solo
            // corre al construir el objeto en memoria.
            //
            // Consecuencia: todo donante leído de la base sin misión arrancada
            // tiraba NPE al subir de categoría. Lo arregla el @PostLoad
            // rehidratarProgresoMision() en Donante.
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
            // ProgresoMision es @Embeddable con un @ManyToOne adentro: sus
            // columnas van en la tabla del donante, pero la misión es una fila
            // aparte. Es el mapeo que permite preguntar "¿en qué misión está?"
            // sin una tabla extra.
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
            // @ElementCollection + @CollectionTable("donacion_categoria"): un
            // Set<String> no cabe en una columna, va a una tabla hija.
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
            // El modelo guarda DOS filas por donación: `exitosa=false` cuando se
            // registra y `exitosa=true` cuando logística confirma la entrega. Por
            // eso MetricasDonante cuenta `!exitosa` para el total histórico: ese
            // filtro NO es un error, cuenta eventos de registro.
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
            // getCategorias() tiene la guarda contra null porque la columna puede
            // volver vacía. Sin ella, categoriasDistintasDonadas() tiraría NPE
            // al recorrer un donante que donó sin especificar categoría.
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
            // @OneToMany(cascade = ALL, orphanRemoval = true, fetch = EAGER): las
            // posiciones no existen sin su ranking, así que viajan con él.
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
            // PosicionRanking tiene @ManyToOne Donante: si esa relación no se
            // persistiera, posicionDe(donanteId) devolvería siempre 0 y el
            // ranking sería inútil.
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
            // findByPeriodoInicio es lo que usa el @Scheduled para no recalcular
            // un período ya cerrado.
            LocalDateTime fecha = LocalDateTime.of(2026, 3, 15, 10, 0);
            rankings.save(RankingMensual.calcular(List.of(), fecha));
            vaciarCache();

            LocalDateTime inicioDeMarzo = LocalDateTime.of(2026, 3, 1, 0, 0);

            assertThat(rankings.findByPeriodoInicio(inicioDeMarzo)).isPresent();
        }
    }
}
