package com.culinaryops.domain.exception;

/**
 * Se lanza cuando una salida o ajuste de inventario dejaría el stock negativo.
 */
public class InsufficientStockException extends CulinaryDomainException {

    /**
     * Crea la excepción indicando el detalle del stock insuficiente.
     *
     * @param message descripción del error de stock
     */
    public InsufficientStockException(String message) {
        super(message);
    }

    /**
     * Crea la excepción con mensaje y causa original.
     *
     * @param message descripción del error de stock
     * @param cause excepción original que provocó el fallo
     */
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
