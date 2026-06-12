package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.services.EntidadesBeneficiariasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/entidades")
public class EntidadBeneficiariaController {

    private final EntidadesBeneficiariasService entidadesBeneficiariasService;

    @PostMapping
    public ResponseEntity<Void> crearEntidad(@RequestBody EntidadBeneficiaria entidad) {
        // la entidad ya viene con su UUID seteado desde el frontend
        entidadesBeneficiariasService.guardar(entidad);
            //código 201 (Created) con una "caja vacía" (.build())
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<EntidadBeneficiaria>> obtenerTodas() {
        List<EntidadBeneficiaria> entidades = entidadesBeneficiariasService.obtenerTodas();
        // 200 OK con la lista en el cuerpo de la respuesta
        return ResponseEntity.ok(entidades);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiaria> obtenerPorId(
        @PathVariable UUID id
    ) {
        EntidadBeneficiaria entidad = entidadesBeneficiariasService.obtenerPorId(id);

        //!!!!! Si no encuentra el id en la BD: EXCEPCION HTTP 404 Not Found

        // 200 OK con los datos de la entidad
        return ResponseEntity.ok(entidad);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizarEntidad(
        @PathVariable UUID id,
        @RequestBody EntidadBeneficiaria entidad
    ) {

        //validar Existencia

        entidadesBeneficiariasService.actualizar(id, entidad);
        // Convención REST: Cuando un PUT sale bien pero no devuelve datos,
        // se responde con 204 No Content (Sin Contenido)
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entidadId}/{campaniaId}")
    public ResponseEntity<Void> agregarNecesidadACampania(
        @PathVariable UUID entidadId,
        @PathVariable UUID campaniaId,
        @RequestBody Necesidad nuevaNecesidad) {


        //validarExistenciaEntidad(entidadId);

        entidadesBeneficiariasService.agregarNecesidadACampania(entidadId, campaniaId, nuevaNecesidad);

                        // 201 Created (Caja vacía)
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
