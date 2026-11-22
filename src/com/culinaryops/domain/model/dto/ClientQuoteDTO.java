package com.culinaryops.domain.model.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Objeto de Transferencia de Datos (DTO) que representa la cotización comercial limpia para el cliente.
 * Por regla estricta de confidencialidad, omite costos de materia prima, costos operativos y márgenes.
 *
 * @param id            Identificador del presupuesto.
 * @param clientName    Nombre del cliente.
 * @param eventName     Nombre del evento o servicio.
 * @param eventDate     Fecha programada de entrega (puede ser nula).
 * @param items         Lista inmutable de líneas cotizadas al cliente.
 * @param totalPriceUSD Precio final total en USD (impuestos incluidos).
 * @param notes         Notas comerciales y condiciones de entrega.
 * @param generatedAt   Fecha y hora legible de emisión del documento.
 */
public record ClientQuoteDTO(
        String id,
        String clientName,
        String eventName,
        LocalDate eventDate,
        List<QuoteItemDTO> items,
        double totalPriceUSD,
        String notes,
        String generatedAt
) {
    public ClientQuoteDTO {
        items = items == null ? List.of() : List.copyOf(items);
        notes = notes == null ? "" : notes;
    }

    /**
     * Detalle comercial de cada producto incluido en la ficha del cliente.
     *
     * @param recipeName    Nombre comercial de la preparación.
     * @param description   Descripción del producto.
     * @param quantity      Cantidad de tandas solicitadas.
     * @param yieldQuantity Rendimiento por tanda.
     * @param yieldUnit     Unidad de rendimiento (ej. "porciones").
     * @param totalUnits    Total de unidades/porciones incluidas.
     */
    public record QuoteItemDTO(
            String recipeName,
            String description,
            double quantity,
            double yieldQuantity,
            String yieldUnit,
            double totalUnits
    ) {}
}