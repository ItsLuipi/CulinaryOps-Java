package com.culinaryops.domain.exception;

/**
 * Se lanza al intentar registrar un ingrediente duplicado en el catálogo
 * o repetir un ingrediente dentro de una misma receta.
 */
public class DuplicateIngredientException extends CulinaryDomainException {

    /**
     * Crea la excepción con el detalle del duplicado detectado.
     *
     * @param message descripción del error de duplicado
     */
    public DuplicateIngredientException(String message) {
        super(message);
    }

    /**
     * Crea la excepción con mensaje y causa original.
     *
     * @param message descripción del error de duplicado
     * @param cause excepción original que provocó el fallo
     */
    public DuplicateIngredientException(String message, Throwable cause) {
        super(message, cause);
    }
}
