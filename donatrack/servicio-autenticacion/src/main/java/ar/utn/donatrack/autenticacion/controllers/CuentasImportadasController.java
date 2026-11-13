package ar.utn.donatrack.autenticacion.controllers;

import ar.utn.donatrack.autenticacion.dtos.request.CuentasImportadas;
import ar.utn.donatrack.autenticacion.dtos.response.ResultadoImportacion;
import ar.utn.donatrack.autenticacion.seguridad.ClavesCompartidas;
import ar.utn.donatrack.autenticacion.services.CuentasImportadasService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo llama el Servicio de Donaciones (su bandeja de salida) con las personas que
 * cargó la administración. Requiere la clave entre servicios en X-DonaTrack-Servicio.
 */
@RestController
@RequestMapping("/api/autenticacion/cuentas")
public class CuentasImportadasController {
    private final CuentasImportadasService importacion;
    private final ClavesCompartidas claves;

    CuentasImportadasController(CuentasImportadasService importacion, ClavesCompartidas claves) {
        this.importacion = importacion;
        this.claves = claves;
    }

    @PostMapping("/importadas")
    ResultadoImportacion importadas(@RequestHeader(value = ClavesCompartidas.CABECERA_SERVICIO, defaultValue = "") String clave,
                                    @Valid @RequestBody CuentasImportadas aviso) {
        claves.exigirServicio(clave);
        return importacion.registrar(aviso.donantes());
    }
}
