package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.repositories.jpa.MensajeSalienteJpaRepository;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Por acá se deja todo aviso que tiene que salir del servicio. Guarda el mensaje
 * en la transacción de quien llama: si esa transacción se deshace, el aviso
 * también. Lo envía después RelayDeMensajes.
 *
 * El contenido se guarda en JSON con el ObjectMapper de Spring, así llega al
 * destino igual que si se hubiera mandado en el momento (las fechas, en ISO-8601).
 */
@Component
@RequiredArgsConstructor
public class BandejaDeSalida {

    private final MensajeSalienteJpaRepository mensajes;
    private final ObjectMapper json;

    @Transactional
    public void encolar(String destino, Object contenido) {
        mensajes.save(MensajeSaliente.nuevo(destino, aJson(destino, contenido), FechaHoraArgentina.ahora()));
    }

    private String aJson(String destino, Object contenido) {
        try {
            return json.writeValueAsString(contenido);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo armar el mensaje para " + destino, e);
        }
    }
}
