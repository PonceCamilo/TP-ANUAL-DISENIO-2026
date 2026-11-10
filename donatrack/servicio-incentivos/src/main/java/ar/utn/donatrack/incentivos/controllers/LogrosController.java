package ar.utn.donatrack.incentivos.controllers;

import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.dtos.response.InsigniaResponse;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.services.IncentivosService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incentivos")
public class LogrosController {
    private final IncentivosService incentivos;
    private final RankingMensualRepositoryInterface rankings;

    public LogrosController(IncentivosService incentivos, RankingMensualRepositoryInterface rankings) {
        this.incentivos = incentivos;
        this.rankings = rankings;
    }

    @PatchMapping("/donantes/{donanteId}/insignias/{insigniaId}/visibilidad")
    public ResponseEntity<Void> cambiarVisibilidad(@PathVariable UUID donanteId, @PathVariable Long insigniaId,
                                                   @Valid @RequestBody Visibilidad solicitud) {
        incentivos.cambiarVisibilidadInsignia(donanteId, insigniaId, solicitud.visible());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rankings/podio")
    public Podio podio() {
        YearMonth periodo = YearMonth.now();
        var cierre = rankings.buscarPorPeriodo(periodo.getMonthValue(), periodo.getYear());
        RankingMensual ranking = cierre.orElseGet(incentivos::obtenerRankingMensualActual);
        List<Puesto> puestos = ranking.topTres().stream()
                .map(posicion -> new Puesto(posicion.getPuesto(), posicion.getDonanteId(),
                        posicion.getMisionesCompletadas()))
                .toList();
        return new Podio(ranking.getMes(), ranking.getAnio(), cierre.isEmpty(), puestos);
    }

    @GetMapping("/donantes/{donanteId}/perfil-publico")
    public PerfilPublico perfilPublico(@PathVariable UUID donanteId) {
        return incentivos.buscarPerfilParaMostrar(donanteId)
                .map(perfil -> new PerfilPublico(
                        perfil.getCategoria() == null ? "Colaborador" : perfil.getCategoria().getNombre(),
                        perfil.getInsigniasObtenidas().stream()
                                .filter(InsigniaObtenida::isVisibilidad)
                                .map(InsigniaResponse::desde)
                                .toList()))
                .orElseGet(() -> new PerfilPublico("Colaborador", List.of()));
    }

    public record Visibilidad(@NotNull Boolean visible) {}
    public record Podio(int mes, int anio, boolean provisional, List<Puesto> puestos) {}
    public record Puesto(int puesto, UUID donanteId, int misionesCompletadas) {}
    public record PerfilPublico(String categoria, List<InsigniaResponse> insignias) {}
}
