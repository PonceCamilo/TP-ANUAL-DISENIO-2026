package ar.utn.donatrack.incentivos.model;

/**
 * Categorías de donante en orden ascendente.
 * Para subir de categoría, el donante debe completar
 * todas las misiones de la categoría actual.
 */
public enum CategoriaDonante {
    COLABORADOR,    // categoría inicial
    SOSTENEDOR,
    TRANSFORMADOR   // categoría máxima
}
