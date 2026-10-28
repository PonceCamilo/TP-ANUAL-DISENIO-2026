package ar.utn.donatrack.donaciones.services.logistica;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La persona donante y la entidad de una donación, que son a quienes se les
 * avisa de cada paso de la entrega. Para una ruta entera, las de todas sus
 * donaciones se leen en dos consultas y no dos por donación.
 */
@Component
@RequiredArgsConstructor
public class ParticipantesDeDonaciones {

    private final RepositorioPersonasDonantes donantes;
    private final RepositorioEntidadesBeneficiarias entidades;

    public PersonaDonante donanteDe(Donacion donacion) {
        UUID id = donacion.getIdDonante();
        return id == null ? null : donantes.buscarPorId(id).orElse(null);
    }

    public EntidadBeneficiaria entidadDe(Donacion donacion) {
        UUID id = donacion.getIdEntidadBeneficiaria();
        return id == null ? null : entidades.buscarPorId(id).orElse(null);
    }

    public Lote de(List<Donacion> donaciones) {
        Set<UUID> idsDonantes = ids(donaciones, Donacion::getIdDonante);
        Set<UUID> idsEntidades = ids(donaciones, Donacion::getIdEntidadBeneficiaria);
        return new Lote(
                idsDonantes.isEmpty() ? Map.of() : porId(donantes.obtenerPorIds(idsDonantes), PersonaDonante::getId),
                idsEntidades.isEmpty() ? Map.of()
                        : porId(entidades.obtenerPorIds(idsEntidades), EntidadBeneficiaria::getId));
    }

    /** Los participantes de varias donaciones, ya leídos. */
    public record Lote(Map<UUID, PersonaDonante> donantes, Map<UUID, EntidadBeneficiaria> entidades) {

        public PersonaDonante donanteDe(Donacion donacion) {
            return donacion.getIdDonante() == null ? null : donantes.get(donacion.getIdDonante());
        }

        public EntidadBeneficiaria entidadDe(Donacion donacion) {
            return donacion.getIdEntidadBeneficiaria() == null ? null : entidades.get(donacion.getIdEntidadBeneficiaria());
        }
    }

    private static Set<UUID> ids(Collection<Donacion> donaciones, Function<Donacion, UUID> id) {
        return donaciones.stream().map(id).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static <T> Map<UUID, T> porId(List<T> elementos, Function<T, UUID> id) {
        return elementos.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a));
    }
}
