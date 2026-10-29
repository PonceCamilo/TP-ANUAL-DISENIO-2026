package ar.utn.donatrack.donaciones.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import ar.utn.donatrack.donaciones.services.PersonaDonanteService;
import ar.utn.donatrack.donaciones.dtos.response.ImportacionResumenResponseDTO;
import ar.utn.donatrack.donaciones.dtos.request.EstadoDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteUpdateRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.services.CsvImportService;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/donantes")
@Validated
@Tag(name = "Personas donantes", description = "Alta, consulta y gestión de personas donantes (humanas y organizaciones), "
        + "y su importación masiva por CSV")
public class PersonaDonanteController {

    private final PersonaDonanteService personaDonanteService;
    private final CsvImportService csvImportService;

    @Operation(
            summary = "Listar personas donantes (paginado)",
            description = "Ordenadas por email, con filtro opcional por estado. Las páginas empiezan en 0; el tamaño por "
                    + "defecto es " + SolicitudPagina.TAMANIO_POR_DEFECTO + " y el máximo " + SolicitudPagina.TAMANIO_MAXIMO + ".",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de personas donantes con el total de resultados"),
                    @ApiResponse(responseCode = "400", description = "Número o tamaño de página fuera de rango")
            }
    )
    @GetMapping
    public ResponseEntity<PaginaResponseDTO<PersonaDonanteResponseDTO>> obtenerDonantes(
            @Parameter(description = "ACTIVO, INACTIVO o BLOQUEADO", example = "ACTIVO")
            @RequestParam(required = false) String estado,
            @Parameter(description = "Número de página, desde 0", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Elementos por página (1 a 100)", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(personaDonanteService.obtenerDonantes(estado, SolicitudPagina.de(page, size)));
    }

    @Operation(
            summary = "Obtener una persona donante por ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Persona donante encontrada"),
                    @ApiResponse(responseCode = "404", description = "Persona donante no encontrada")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<PersonaDonanteResponseDTO> obtenerDonante(
            @Parameter(description = "ID de la persona donante") @PathVariable UUID id
    ) {
        return ResponseEntity.ok(personaDonanteService.obtenerDonante(id));
    }

    @Operation(
            summary = "Registrar una persona donante",
            description = "Alta de una persona humana u organización (campo tipo: HUMANA o JURIDICA). El email es su "
                    + "identidad: se guarda normalizado y no puede repetirse. Arranca ACTIVA.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Persona donante registrada"),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                    @ApiResponse(responseCode = "409", description = "Ya existe una persona donante con ese email")
            }
    )
    @PostMapping
    public ResponseEntity<PersonaDonanteResponseDTO> registrarDonante(
            @RequestBody @Valid PersonaDonanteRequestDTO dto
    ) {
        PersonaDonanteResponseDTO donanteCreado = personaDonanteService.registrar(dto);
        URI location = URI.create("/donantes/" + donanteCreado.getId());
        return ResponseEntity.created(location).body(donanteCreado);
    }

    @Operation(
            summary = "Actualizar los datos de una persona donante",
            description = "Los campos que no vienen no se modifican, y solo se aplican los del tipo de la persona: "
                    + "nombre, apellido, fecha de nacimiento y género para una persona humana; razón social, rubro y "
                    + "tipo de organización para una jurídica. El email no se cambia: es la identidad de la persona.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Persona donante actualizada"),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                    @ApiResponse(responseCode = "404", description = "Persona donante no encontrada")
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<PersonaDonanteResponseDTO> actualizarDonante(
            @Parameter(description = "ID de la persona donante") @PathVariable UUID id,
            @RequestBody @Valid PersonaDonanteUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(personaDonanteService.actualizar(id, dto));
    }

    @Operation(
            summary = "Cambiar el estado de una persona donante",
            description = "ACTIVO, INACTIVO o BLOQUEADO. Bloquear exige una justificación; una persona BLOQUEADA no puede "
                    + "registrar donaciones.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Estado actualizado"),
                    @ApiResponse(responseCode = "400", description = "Falta la justificación para bloquear"),
                    @ApiResponse(responseCode = "404", description = "Persona donante no encontrada"),
                    @ApiResponse(responseCode = "409", description = "La persona ya está en ese estado"),
                    @ApiResponse(responseCode = "422", description = "Transición de estado no permitida")
            }
    )
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @Parameter(description = "ID de la persona donante") @PathVariable UUID id,
            @RequestBody @Valid EstadoDonanteRequestDTO dto
    ) {
        personaDonanteService.cambiarEstado(id, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Agregar o reemplazar un medio de contacto",
            description = "Un contacto nuevo reemplaza al del mismo tipo (EMAIL, TELEFONO o WHATSAPP), y si ese era el "
                    + "predeterminado para las notificaciones, el nuevo pasa a serlo.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contacto actualizado"),
                    @ApiResponse(responseCode = "400", description = "Contacto inválido"),
                    @ApiResponse(responseCode = "404", description = "Persona donante no encontrada")
            }
    )
    @PatchMapping("/{id}/contactos")
    public ResponseEntity<Void> modificarContacto(
            @Parameter(description = "ID de la persona donante") @PathVariable UUID id,
            @RequestBody @Valid MedioDeContactoRequestDTO dto
    ) {
        personaDonanteService.modificarContacto(id, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Agregar un representante a una organización",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Representante agregado"),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos, o la persona es humana y no tiene representantes"),
                    @ApiResponse(responseCode = "404", description = "Persona donante no encontrada")
            }
    )
    @PatchMapping("/{id}/representantes")
    public ResponseEntity<Void> modificarRepresentante(
            @Parameter(description = "ID de la persona jurídica") @PathVariable UUID id,
            @RequestBody @Valid RepresentanteRequestDTO dto
    ) {
        personaDonanteService.modificarRepresentante(id, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Eliminar una persona donante",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Persona donante eliminada"),
                    @ApiResponse(responseCode = "404", description = "Persona donante no encontrada"),
                    @ApiResponse(responseCode = "409", description = "La persona tiene donaciones registradas")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDonante(
            @Parameter(description = "ID de la persona donante") @PathVariable UUID id
    ) {
        personaDonanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Importar personas donantes desde un CSV",
            description = "Columnas, en orden y con encabezado: TipoPersona (HUMANA o JURIDICA), TipoDoc, Documento, Nombre, Email y Telefono (opcional). "
                    + "Las personas se identifican por email: si ya existe se actualiza, si no se crea. Responde 200 aunque "
                    + "haya filas con error (el archivo se procesó) con un resumen y el detalle de esas filas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Archivo procesado: resumen y errores por fila"),
                    @ApiResponse(responseCode = "400", description = "El archivo no se pudo leer"),
                    @ApiResponse(responseCode = "413", description = "El archivo supera el límite de subida")
            }
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacionResumenResponseDTO> importarDonantes(
            @Parameter(description = "Archivo CSV con encabezado") @RequestParam("file") MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(csvImportService.importarYResumir(file.getInputStream()));
    }
}
