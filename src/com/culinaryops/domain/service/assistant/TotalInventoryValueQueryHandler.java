/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.service.assistant;

/**
 *
 * @author roco
 */
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.dto.ChatbotAnswer;
import com.culinaryops.domain.service.CurrencyFormatter;
import com.culinaryops.domain.service.InventoryQueryHandler;

import java.util.List;

public class TotalInventoryValueQueryHandler implements InventoryQueryHandler {

    @Override
    public boolean supports(String normalizedQuery) {
        return normalizedQuery.contains("cuanto vale")
                || normalizedQuery.contains("valor total")
                || normalizedQuery.contains("total inventario")
                || normalizedQuery.contains("dinero en stock");
    }

    @Override
    public ChatbotAnswer handle(String normalizedQuery, List<Ingredient> ingredients) {
        double purchaseTotal = ingredients.stream()
                .mapToDouble(Ingredient::getTotalStockValue)
                .sum();
        double effectiveTotal = ingredients.stream()
                .mapToDouble(Ingredient::getTotalEffectiveStockValue)
                .sum();

        String msg = String.format(
                "Resumen financiero del almacén (%d insumos registrados):\n" +
                "- Valor total a precio de compra: %s\n" +
                "- Valor útil neto descontando mermas: %s",
                ingredients.size(),
                CurrencyFormatter.formatUSD(purchaseTotal),
                CurrencyFormatter.formatUSD(effectiveTotal)
        );
        return new ChatbotAnswer("TOTAL_VALUE", msg, List.of());
    }
}