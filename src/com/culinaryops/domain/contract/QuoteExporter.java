package com.culinaryops.domain.contract;

import com.culinaryops.domain.model.dto.ClientQuoteDTO;

/**
 * Contrato polimórfico para exportar la ficha comercial de un cliente en distintos formatos.
 */
public interface QuoteExporter {

    /**
     * Exporta una cotización comercial limpia al formato implementado.
     *
     * @param quote DTO de cotización comercial (no nulo).
     * @return Representación formateada lista para entregar o imprimir.
     */
    String export(ClientQuoteDTO quote);

    /**
     * Nombre descriptivo del formato de exportación.
     *
     * @return Etiqueta del formato.
     */
    default String getFormatName() {
        return "TEXT";
    }
}
