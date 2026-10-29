package ar.utn.donatrack.donaciones.eventos;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionCambioDeEstado;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionDadaDeBaja;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("BandejaDeCandidatosOyente - el ranking sale de la bandeja cuando ya no sirve")
class BandejaDeCandidatosOyenteTest {

    private final RepositorioCandidatosAsignacion candidatos = mock(RepositorioCandidatosAsignacion.class);
    private final BandejaDeCandidatosOyente oyente = new BandejaDeCandidatosOyente(candidatos);
    private final UUID idDonacion = UUID.randomUUID();

    @Test
    @DisplayName("Si la donación dejó el depósito (asignada, vencida), su ranking sale de la bandeja")
    void salioDelDeposito() {
        oyente.alCambiarDeEstado(new DonacionCambioDeEstado(idDonacion, "EN_DEPOSITO", "VENCIDA"));

        verify(candidatos).eliminar(idDonacion);
    }

    @Test
    @DisplayName("Un cambio entre estados fuera del depósito no toca la bandeja")
    void fueraDelDeposito() {
        oyente.alCambiarDeEstado(new DonacionCambioDeEstado(idDonacion, "ASIGNACION_REALIZADA", "LISTA_PARA_ENTREGAR"));

        verifyNoInteractions(candidatos);
    }

    @Test
    @DisplayName("Una donación dada de baja no deja su ranking huérfano")
    void baja() {
        oyente.alDarseDeBaja(new DonacionDadaDeBaja(idDonacion));

        verify(candidatos).eliminar(idDonacion);
    }
}
