package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.CamionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.DonacionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.SolicitudRuteoLoteRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.PlanificacionProveedorResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaPlanificadaProveedorDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockProveedorRuteoController - reparto por entidad")
class MockProveedorRuteoControllerTest {

    private final MockProveedorRuteoController mock = new MockProveedorRuteoController();

    @Test
    @DisplayName("Agrupa por entidad, balancea grupos enteros y no deja donaciones afuera")
    void reparteGruposEnterosEntreCamiones() {
        UUID escuelaA = UUID.randomUUID();
        UUID comedorB = UUID.randomUUID();
        UUID escuelaC = UUID.randomUUID();
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        DonacionParaRutearRequestDTO a1 = donacion(escuelaA);
        DonacionParaRutearRequestDTO b1 = donacion(comedorB);
        DonacionParaRutearRequestDTO a2 = donacion(escuelaA);
        DonacionParaRutearRequestDTO c1 = donacion(escuelaC);

        SolicitudRuteoLoteRequestDTO pedido = new SolicitudRuteoLoteRequestDTO();
        pedido.setLoteId(UUID.randomUUID());
        pedido.setCamiones(List.of(camion(t1), camion(t2)));
        pedido.setDonaciones(List.of(a1, b1, a2, c1));

        PlanificacionProveedorResponseDTO respuesta = mock.planificar(pedido).getBody();

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getRutas()).extracting(RutaPlanificadaProveedorDTO::getCamionId).containsExactly(t1, t2);

        RutaPlanificadaProveedorDTO rutaT1 = respuesta.getRutas().getFirst();
        assertThat(rutaT1.getParadas()).hasSize(1);
        assertThat(rutaT1.getParadas().getFirst().getOrden()).isEqualTo(1);
        assertThat(rutaT1.getParadas().getFirst().getIdEntidadBeneficiaria()).isEqualTo(escuelaA);
        assertThat(rutaT1.getParadas().getFirst().getDonacionesIds())
                .containsExactly(a1.getIdDonacion(), a2.getIdDonacion());

        RutaPlanificadaProveedorDTO rutaT2 = respuesta.getRutas().get(1);
        assertThat(rutaT2.getParadas()).extracting(parada -> parada.getIdEntidadBeneficiaria())
                .containsExactly(comedorB, escuelaC);
        assertThat(rutaT2.getParadas()).extracting(parada -> parada.getOrden()).containsExactly(1, 2);
        assertThat(rutaT2.getParadas().getFirst().getDonacionesIds()).containsExactly(b1.getIdDonacion());
        assertThat(rutaT2.getParadas().get(1).getDonacionesIds()).containsExactly(c1.getIdDonacion());
    }

    @Test
    @DisplayName("Con un solo camión, todas las entidades van en ese camión y en orden de aparición")
    void unSoloCamionLlevaTodo() {
        UUID escuela = UUID.randomUUID();
        UUID comedor = UUID.randomUUID();
        UUID camionId = UUID.randomUUID();
        DonacionParaRutearRequestDTO primera = donacion(escuela);
        DonacionParaRutearRequestDTO segunda = donacion(comedor);

        SolicitudRuteoLoteRequestDTO pedido = new SolicitudRuteoLoteRequestDTO();
        pedido.setCamiones(List.of(camion(camionId)));
        pedido.setDonaciones(List.of(primera, segunda));

        PlanificacionProveedorResponseDTO respuesta = mock.planificar(pedido).getBody();

        assertThat(respuesta.getRutas()).hasSize(1);
        assertThat(respuesta.getRutas().getFirst().getCamionId()).isEqualTo(camionId);
        assertThat(respuesta.getRutas().getFirst().getParadas())
                .extracting(parada -> parada.getIdEntidadBeneficiaria())
                .containsExactly(escuela, comedor);
    }

    private DonacionParaRutearRequestDTO donacion(UUID idEntidad) {
        DireccionRequestDTO direccion = new DireccionRequestDTO();
        direccion.setCalle("Medrano");
        direccion.setNumero(951);
        direccion.setLocalidad("CABA");
        direccion.setProvincia("Buenos Aires");
        direccion.setCodigoPostal("C1179");

        DonacionParaRutearRequestDTO donacion = new DonacionParaRutearRequestDTO();
        donacion.setIdDonacion(UUID.randomUUID());
        donacion.setIdEntidadBeneficiaria(idEntidad);
        donacion.setDireccionEntrega(direccion);
        return donacion;
    }

    private CamionParaRutearRequestDTO camion(UUID id) {
        CamionParaRutearRequestDTO camion = new CamionParaRutearRequestDTO();
        camion.setId(id);
        camion.setPatente("AA000AA");
        camion.setCapacidadVolumenM3(12);
        camion.setAlturaM(2.5);
        camion.setCapacidadCargaKg(1500);
        return camion;
    }
}
