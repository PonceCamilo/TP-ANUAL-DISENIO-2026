package ar.utn.donatrack.autenticacion.dtos.response;

import ar.utn.donatrack.autenticacion.models.Rol;
import ar.utn.donatrack.autenticacion.models.TipoDonante;

import java.util.UUID;

public record TokenResponse(String accessToken, long expiresIn, String email, String nombre, Rol rol, TipoDonante tipoDonante, UUID perfilId, UUID camionId) {}
