package com.culinaryops.domain.exception;

/**
 * Se lanza al buscar o operar sobre una entidad cuyo identificador no existe.
 */
public class EntityNotFoundException extends CulinaryDomainException {

    /**
     * Crea la excepción indicando la entidad no encontrada.
     *
     * @param message descripción de la entidad inexistente
     */
    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * Crea la excepción con mensaje y causa original.
     *
     * @param message descripción de la entidad inexistente
     * @param cause excepción original que provocó el fallo
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
