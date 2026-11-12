package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.publico.DonacionEntregadaPublicaDTO;
import ar.utn.donatrack.donaciones.dtos.response.publico.EntidadPublicaDTO;
import ar.utn.donatrack.donaciones.exceptions.comunes.RangoDeFechasInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.mappers.PublicoMapper;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lo que se muestra sin login (Entrega 6): las donaciones entregadas para la
 * landing y el mapa (requisitos 1 y 2), las entidades para que las personas
 * donantes las conozcan (requisito 6) y el catálogo de subcategorías para los
 * formularios y filtros.
 *
 * Lo que se publica lo elige PublicoMapper: nunca datos de la persona donante,
 * ni contactos o representantes de las entidades.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultasPublicasService {

    /** Sin rango, la landing muestra el último mes. */
    static final int MESES_POR_DEFECTO = 1;

    private final RepositorioDonaciones donaciones;
    private final RepositorioEntidadesBeneficiarias entidades;
    private final PublicoMapper mapper;

    /**
     * Entregadas entre esas fechas, de la más reciente a la más vieja, con la
     * entidad que las recibió. Sin desde, un mes antes de hasta; sin hasta, hoy.
     */
    public PaginaResponseDTO<DonacionEntregadaPublicaDTO> entregadas(LocalDate desde, LocalDate hasta,
                                                                    SolicitudPagina pagina) {
        LocalDate fin = hasta == null ? FechaHoraArgentina.hoy() : hasta;
        LocalDate inicio = desde == null ? fin.minusMonths(MESES_POR_DEFECTO) : desde;
        if (inicio.isAfter(fin)) {
            throw new RangoDeFechasInvalidoException(
                    "La fecha 'desde' (" + inicio + ") no puede ser posterior a 'hasta' (" + fin + ")");
        }
        Pagina<Donacion> encontradas = donaciones.buscarEntregadas(inicio, fin, pagina);
        // Las entidades de la página en una sola consulta, no una por donación.
        Map<UUID, EntidadBeneficiaria> receptoras = entidades.obtenerPorIds(encontradas.contenido().stream()
                        .map(Donacion::getIdEntidadBeneficiaria)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(EntidadBeneficiaria::getId, Function.identity()));
        return PaginaResponseDTO.desde(encontradas.map(
                donacion -> mapper.entregada(donacion, receptoras.get(donacion.getIdEntidadBeneficiaria()))));
    }

    /** Las entidades por razón social, con lo que necesitan hoy. */
    public PaginaResponseDTO<EntidadPublicaDTO> entidades(SolicitudPagina pagina) {
        LocalDate hoy = FechaHoraArgentina.hoy();
        return PaginaResponseDTO.desde(entidades.buscar(pagina).map(entidad -> mapper.entidad(entidad, hoy)));
    }

    public EntidadPublicaDTO entidad(UUID id) {
        EntidadBeneficiaria entidad = entidades.buscarPorId(id)
                .orElseThrow(() -> new EntidadBeneficiariaNoEncontradaException(id));
        return mapper.entidad(entidad, FechaHoraArgentina.hoy());
    }

    /**
     * Las subcategorías de las donaciones y de las necesidades, una vez cada una
     * (las que solo difieren en mayúsculas o acentos cuentan como la misma), en
     * orden alfabético en español. Si una aparece en las dos, queda la forma en
     * que se escribió en las donaciones.
     */
    public List<String> subcategorias() {
        Map<String, String> porNormalizada = new LinkedHashMap<>();
        Stream.concat(donaciones.subcategoriasUsadas().stream(), entidades.subcategoriasDeNecesidades().stream())
                .filter(tipo -> tipo != null && !tipo.isBlank())
                .forEach(tipo -> porNormalizada.putIfAbsent(new Subcategoria(tipo).getTipoNormalizado(), tipo.strip()));
        Collator espaniol = Collator.getInstance(Locale.forLanguageTag("es-AR"));
        espaniol.setStrength(Collator.PRIMARY);
        return porNormalizada.values().stream().sorted(espaniol).toList();
    }
}
