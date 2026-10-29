package ar.utn.donatrack.donaciones.eventos;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionEntregada;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("NecesidadesOyente - lo entregado cuenta para las necesidades de la entidad")
class NecesidadesOyenteTest {

    private final RepositorioEntidadesBeneficiarias entidades = mock(RepositorioEntidadesBeneficiarias.class);
    private final NecesidadesOyente oyente = new NecesidadesOyente(entidades);

    @Test
    @DisplayName("Imputa la cantidad entregada a las necesidades compatibles, a la fecha de la entrega")
    void imputaALaFechaDeEntrega() {
        // La campaña terminó el 31 de marzo: con la fecha de entrega (15 de marzo)
        // la necesidad está abierta; con la fecha de hoy no lo estaría.
        Campania campania = new Campania();
        campania.setIdCampania(UUID.randomUUID());
        campania.setFechaInicio(LocalDate.of(2026, 3, 1));
        campania.setFechaFin(LocalDate.of(2026, 3, 31));
        NecesidadExtraordinaria arroz = new NecesidadExtraordinaria();
        arroz.setNombre("Arroz para el comedor");
        arroz.setCantidadObjetivo(100);
        campania.agregarNecesidad(arroz);
        EntidadBeneficiaria entidad = EntidadBeneficiaria.builder().id(UUID.randomUUID()).razonSocial("Comedor")
                .campanias(new ArrayList<>()).build();
        entidad.agregarCampania(campania);
        when(entidades.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));

        oyente.alEntregarse(new DonacionEntregada(UUID.randomUUID(), UUID.randomUUID(), entidad.getId(),
                new Subcategoria("arroz"), 30, LocalDateTime.of(2026, 3, 15, 14, 30)));

        assertThat(arroz.getCantidadRecibida()).isEqualTo(30);
        verify(entidades).guardar(entidad);
    }

    @Test
    @DisplayName("Sin entidad asignada no hay a quién imputar")
    void sinEntidad() {
        oyente.alEntregarse(new DonacionEntregada(UUID.randomUUID(), UUID.randomUUID(), null,
                new Subcategoria("arroz"), 30, LocalDateTime.of(2026, 3, 15, 14, 30)));

        verifyNoInteractions(entidades);
    }
}
