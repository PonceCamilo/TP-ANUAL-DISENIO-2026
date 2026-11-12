package ar.utn.donatrack.incentivos.controllers;

import ar.utn.donatrack.incentivos.dtos.response.PuestoRankingResponse;
import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.interfaces.services.IncentivosServiceInterface;
import ar.utn.donatrack.incentivos.models.PosicionRanking;
import ar.utn.donatrack.incentivos.seguridad.AccesoRankingAdmin;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/incentivos/admin/rankings")
public class RankingAdminController {
    private final RankingMensualRepositoryInterface rankings;
    private final IncentivosServiceInterface incentivos;
    private final AccesoRankingAdmin acceso;

    public RankingAdminController(RankingMensualRepositoryInterface rankings, IncentivosServiceInterface incentivos,
                                  AccesoRankingAdmin acceso) {
        this.rankings = rankings;
        this.incentivos = incentivos;
        this.acceso = acceso;
    }

    @GetMapping("/meses")
    @Operation(summary = "Listar los períodos con rankings cerrados")
    public List<Periodo> historial(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera) {
        acceso.exigirAdministrador(cabecera);
        return rankings.listarPeriodos().stream()
                .map(periodo -> new Periodo(periodo.getYear(), periodo.getMonthValue()))
                .toList();
    }

    @GetMapping("/{anio}/{mes}")
    @Operation(summary = "Consultar los puestos del ranking de un mes")
    public Ranking ranking(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera,
                           @PathVariable @Min(1) @Max(9999) int anio,
                           @PathVariable @Min(1) @Max(12) int mes,
                           @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        acceso.exigirAdministrador(cabecera);
        YearMonth periodo = YearMonth.of(anio, mes);
        PageRequest pagina = PageRequest.of(page, size);
        var guardado = rankings.buscarPuestosGuardados(periodo, pagina);
        if (guardado.isPresent()) {
            return Ranking.desde(periodo, false, guardado.get());
        }
        if (!periodo.equals(YearMonth.now())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay un ranking guardado para ese mes.");
        }
        return Ranking.desde(periodo, true, rankingProvisional(pagina));
    }

    private Page<PuestoRankingResponse> rankingProvisional(PageRequest pagina) {
        List<PosicionRanking> puestos = incentivos.obtenerRankingMensualActual().getPosiciones();
        List<PuestoRankingResponse> contenido = puestos.stream()
                .skip(pagina.getOffset())
                .limit(pagina.getPageSize())
                .map(puesto -> new PuestoRankingResponse(
                        puesto.getPuesto(), puesto.getDonanteId(), puesto.getMisionesCompletadas()))
                .toList();
        return new PageImpl<>(contenido, pagina, puestos.size());
    }

    public record Periodo(int anio, int mes) {}

    public record Ranking(int anio, int mes, boolean provisional, List<PuestoRankingResponse> contenido,
                          int pagina, int totalPaginas, long totalElementos) {
        static Ranking desde(YearMonth periodo, boolean provisional, Page<PuestoRankingResponse> puestos) {
            return new Ranking(periodo.getYear(), periodo.getMonthValue(), provisional, puestos.getContent(),
                    puestos.getNumber(), puestos.getTotalPages(), puestos.getTotalElements());
        }
    }
}
