package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.salida.Destinos;
import ar.utn.donatrack.donaciones.salida.BandejaDeSalida;
import lombok.RequiredArgsConstructor;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Avisos al servicio-incentivos (puerto 8083).
 *
 *  - Cuando el donante registra una donación → notifica a incentivos para que
 *    actualice métricas y verifique misiones completadas.
 *  - Cuando una donación es entregada exitosamente → notifica a incentivos para
 *    que actualice el contador de donaciones exitosas / organizaciones ayudadas.
 *
 * Una sola firma por evento y la fecha siempre la indica quien llama: la del
 * registro o la de la entrega. Antes, la donación exitosa se informaba con la
 * fecha en que se había registrado la donación y no con la de la entrega, y el
 * registro tomaba la hora de la zona del servidor en lugar de la de Argentina.
 *
 * Desde la Entrega 5 los avisos no se envían en el momento: quedan en la bandeja
 * de salida, en la transacción de quien llama, y los manda EnviadorIncentivos a
 * través de RelayDeMensajes, con reintentos si incentivos no está levantado.
 */
@Component
@RequiredArgsConstructor
public class IncentivosClient {

    private final BandejaDeSalida salida;

    public void notificarDonacionRegistrada(UUID donanteId, String destinatario, String medio,
                                            int cantidadBienes, List<String> categoriasDonadas, LocalDateTime fecha) {
        salida.encolar(Destinos.INCENTIVOS_DONACION_REGISTRADA, new SolicitudIncentivo(
                donanteId, destinatario, medio, cantidadBienes, categoriasDonadas, fecha, null));
    }

    public void notificarDonacionExitosa(Donacion donacion, String destinatario, String medio, LocalDateTime fechaEntrega) {
        salida.encolar(Destinos.INCENTIVOS_DONACION_EXITOSA, new SolicitudIncentivo(
                donacion.getIdDonante(), destinatario, medio,
                cantidadBienes(donacion), categoriasDonadas(donacion), fechaEntrega, entidadBeneficiaria(donacion)));
    }

    private int cantidadBienes(Donacion donacion) {
        return donacion.getBienes().stream()
                .mapToInt(bien -> Math.max(1, bien.getCantidad()))
                .sum();
    }

    private List<String> categoriasDonadas(Donacion donacion) {
        return Subcategoria.tiposDistintos(donacion.getBienes().stream().map(Bien::getSubcategoria).toList());
    }

    private String entidadBeneficiaria(Donacion donacion) {
        return donacion.getIdEntidadBeneficiaria() != null ? donacion.getIdEntidadBeneficiaria().toString() : null;
    }

    /** Cuerpo de los dos endpoints de incentivos: tienen los mismos campos. */
    record SolicitudIncentivo(
            UUID donanteId, String destinatario, String medio,
            int cantidadBienes, List<String> categoriasDonadas,
            LocalDateTime fecha, String entidadBeneficiaria) {}
}
