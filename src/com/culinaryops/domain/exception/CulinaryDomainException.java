package com.culinaryops.domain.exception;

/**
 * Excepción base para violaciones de reglas de negocio en CulinaryOps.
 */
public class CulinaryDomainException extends RuntimeException {

    /**
     * Crea una excepción de dominio con un mensaje descriptivo.
     *
     * @param message descripción del error de negocio
     */
    public CulinaryDomainException(String message) {
        super(message);
    }

    /**
     * Crea una excepción de dominio con mensaje y causa original.
     *
     * @param message descripción del error de negocio
     * @param cause excepción original que provocó el fallo
     */
    public CulinaryDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
