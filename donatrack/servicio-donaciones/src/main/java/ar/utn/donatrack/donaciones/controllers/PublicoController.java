package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.publico.DonacionEntregadaPublicaDTO;
import ar.utn.donatrack.donaciones.dtos.response.publico.EntidadPublicaDTO;
import ar.utn.donatrack.donaciones.services.ConsultasPublicasService;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos que se muestran sin login (Entrega 6): no piden token. Solo lectura, y
 * sin datos personales: ni de las personas donantes ni de los contactos de las
 * entidades.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/publico")
@Tag(name = "Público", description = "Sin token: donaciones entregadas para la landing y el mapa, entidades "
        + "beneficiarias con lo que necesitan y el catálogo de subcategorías. No incluye datos personales.")
public class PublicoController {

    private final ConsultasPublicasService publico;

    @Operation(
            summary = "Donaciones entregadas (paginado)",
            description = "Las entregadas entre desde y hasta (los dos incluidos, por fecha de entrega), de la más "
                    + "reciente a la más vieja, con la entidad que las recibió y sus coordenadas para el mapa. Sin "
                    + "fechas, el último mes (requisitos 1 y 2). totalElementos es la cantidad de entregadas en el "
                    + "período.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de donaciones entregadas"),
                    @ApiResponse(responseCode = "400", description = "Rango de fechas o página inválidos")
            }
    )
    @GetMapping("/donaciones-entregadas")
    public ResponseEntity<PaginaResponseDTO<DonacionEntregadaPublicaDTO>> entregadas(
            @Parameter(description = "Entregadas ese día o después (AAAA-MM-DD). Por defecto, un mes antes de hasta")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Entregadas ese día o antes (AAAA-MM-DD). Por defecto, hoy")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Número de página, desde 1", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Elementos por página (1 a 100)", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(publico.entregadas(desde, hasta, SolicitudPagina.de(page, size)));
    }

    @Operation(
            summary = "Entidades beneficiarias (paginado)",
            description = "Ordenadas por razón social, con su localidad y las necesidades abiertas de sus campañas "
                    + "vigentes (requisito 6). Sin contactos, representantes ni calle.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de entidades"),
                    @ApiResponse(responseCode = "400", description = "Número o tamaño de página fuera de rango")
            }
    )
    @GetMapping("/entidades")
    public ResponseEntity<PaginaResponseDTO<EntidadPublicaDTO>> entidades(
            @Parameter(description = "Número de página, desde 1", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Elementos por página (1 a 100)", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(publico.entidades(SolicitudPagina.de(page, size)));
    }

    @Operation(
            summary = "Una entidad beneficiaria",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Entidad encontrada"),
                    @ApiResponse(responseCode = "404", description = "Entidad no encontrada")
            }
    )
    @GetMapping("/entidades/{id}")
    public ResponseEntity<EntidadPublicaDTO> entidad(
            @Parameter(description = "ID de la entidad beneficiaria") @PathVariable UUID id
    ) {
        return ResponseEntity.ok(publico.entidad(id));
    }

    @Operation(
            summary = "Catálogo de subcategorías",
            description = "Las subcategorías que ya tienen donaciones o necesidades, una vez cada una y en orden "
                    + "alfabético. Para los formularios y para filtrar las donaciones por varias subcategorías.",
            responses = @ApiResponse(responseCode = "200", description = "Subcategorías")
    )
    @GetMapping("/subcategorias")
    public ResponseEntity<List<String>> subcategorias() {
        return ResponseEntity.ok(publico.subcategorias());
    }
}
