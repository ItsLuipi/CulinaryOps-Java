package com.culinaryops.domain.service;

import com.culinaryops.domain.contract.QuoteExporter;
import com.culinaryops.domain.model.dto.ClientQuoteDTO;

/**
 * Implementación de {@link QuoteExporter} que genera una ficha comercial limpia en texto plano
 * lista para impresión o envío directo al cliente.
 */
public class PlainTextQuoteExporter implements QuoteExporter {

    @Override
    public String export(ClientQuoteDTO quote) {
        if (quote == null) {
            throw new IllegalArgumentException("La cotización a exportar no puede ser nula.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("              CULINARYOPS — COTIZACIÓN DE SERVICIO          \n");
        sb.append("============================================================\n");
        sb.append("Cotización N° : ").append(quote.id()).append("\n");
        sb.append("Cliente       : ").append(quote.clientName()).append("\n");
        sb.append("Evento        : ").append(quote.eventName()).append("\n");
        sb.append("Fecha Entrega : ").append(quote.eventDate() != null ? quote.eventDate() : "A confirmar").append("\n");
        sb.append("Emitido el    : ").append(quote.generatedAt()).append("\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("DETALLE DE PRODUCTOS:\n");

        for (ClientQuoteDTO.QuoteItemDTO item : quote.items()) {
            sb.append(String.format(" • %s (%.1f tandas | %.0f %s totales)\n",
                    item.recipeName(),
                    item.quantity(),
                    item.totalUnits(),
                    item.yieldUnit()));
            if (item.description() != null && !item.description().isBlank()) {
                sb.append("   ").append(item.description()).append("\n");
            }
        }

        sb.append("------------------------------------------------------------\n");
        sb.append("TOTAL FINAL   : ").append(CurrencyFormatter.formatUSD(quote.totalPriceUSD())).append("\n");
        if (!quote.notes().isBlank()) {
            sb.append("Notas         : ").append(quote.notes()).append("\n");
        }
        sb.append("============================================================\n");
        return sb.toString();
    }

    @Override
    public String getFormatName() {
        return "PLAIN_TEXT";
    }
}