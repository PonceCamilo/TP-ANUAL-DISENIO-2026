package ar.utn.donatrack.donaciones.exceptions;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import ar.utn.donatrack.donaciones.dtos.response.ErrorResponseDTO;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;

/**
 * Traduce cada error a su respuesta HTTP.
 *
 * Las excepciones del dominio no se enumeran acá: cada una extiende la categoría
 * que le corresponde (NoEncontrado, SolicitudInvalida, Conflicto, ReglaDeNegocio,
 * ServicioNoDisponible) y es la categoría la que define el código. Sumar una
 * excepción nueva no obliga a tocar esta clase.
 *
 * El cuerpo es siempre un ErrorResponseDTO: timestamp, status, error y message,
 * en ese orden. El texto de `error` sale del propio código HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Estado SQL estándar de clave duplicada (violación de UNIQUE o de PK); lo usan PostgreSQL y H2. */
    private static final String CLAVE_DUPLICADA = "23505";

    // ── Excepciones del dominio, por categoría ──────────────────────────────

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> noEncontrado(NoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> solicitudInvalida(SolicitudInvalidaException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponseDTO> conflicto(ConflictoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> reglaDeNegocio(ReglaDeNegocioException ex) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /** Un servicio externo (el broker de logística) no respondió: quien llama puede reintentar. */
    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorResponseDTO> servicioNoDisponible(ServicioNoDisponibleException ex) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    // ── Errores de Spring y de la base ──────────────────────────────────────

    /**
     * Una violación de UNIQUE (por ejemplo, dos altas simultáneas con el mismo
     * email, que pasan las dos la validación previa del servicio) es un conflicto
     * de datos: 409. Cualquier otra violación (un NOT NULL, una FK) es un error
     * de programación: 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> violacionDeIntegridad(DataIntegrityViolationException ex) {
        if (NestedExceptionUtils.getMostSpecificCause(ex) instanceof SQLException sql
                && CLAVE_DUPLICADA.equals(sql.getSQLState())) {
            return responder(HttpStatus.CONFLICT, "Ya existe un registro con ese valor único (por ejemplo, el email).");
        }
        return errorInesperado(ex);
    }

    /**
     * Otra operación modificó la misma entidad entre que se la leyó y se la quiso
     * guardar (bloqueo optimista, columna version): 409, y quien llama puede
     * volver a leerla y reintentar.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponseDTO> modificacionConcurrente(ObjectOptimisticLockingFailureException ex) {
        return responder(HttpStatus.CONFLICT,
                "Otra operación modificó el mismo registro al mismo tiempo. Volvé a consultarlo y reintentá.");
    }

    /**
     * Un archivo más grande que el límite de subida (por ejemplo, un CSV de
     * importación) es un error de quien lo manda: 413 y no 500.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> archivoDemasiadoGrande(MaxUploadSizeExceededException ex) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamaño máximo permitido para la subida.");
    }

    /** Un parámetro de la URL con un tipo que no corresponde (page=abc, un id que no es UUID): 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> jsonInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición es inválido o está mal formado (revise tipos y valores enumerados).");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos inválidos");
        return responder(HttpStatus.BAD_REQUEST, detalle);
    }

    /**
     * Lo que no está previsto es un error del servidor: se registra completo y no
     * se expone el detalle.
     *
     * Los errores del propio Spring MVC que no tienen un manejador acá traen su
     * código (ErrorResponse): una ruta que no existe es 404, un método no
     * soportado 405 (con el encabezado Allow), un tipo de contenido no soportado
     * 415, un parámetro o una parte obligatoria que falta 400. Antes caían todos
     * en este manejador y respondían 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> errorInesperado(Exception ex) {
        if (ex instanceof ErrorResponse deSpring && deSpring.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(deSpring.getStatusCode().value());
            String detalle = deSpring.getBody().getDetail();
            return ResponseEntity.status(status).headers(deSpring.getHeaders())
                    .body(cuerpo(status, detalle == null ? status.getReasonPhrase() : detalle));
        }
        log.error("Error inesperado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado. Contacte al administrador.");
    }

    // ── Armado de la respuesta ──────────────────────────────────────────────

    private static ResponseEntity<ErrorResponseDTO> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(cuerpo(status, mensaje));
    }

    private static ErrorResponseDTO cuerpo(HttpStatus status, String mensaje) {
        return ErrorResponseDTO.builder()
                .timestamp(FechaHoraArgentina.ahora())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(mensaje)
                .build();
    }
}
