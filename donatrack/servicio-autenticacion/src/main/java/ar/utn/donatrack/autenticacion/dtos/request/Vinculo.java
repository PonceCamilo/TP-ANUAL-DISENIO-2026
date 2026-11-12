package ar.utn.donatrack.autenticacion.dtos.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record Vinculo(@NotNull UUID perfilId) {}
