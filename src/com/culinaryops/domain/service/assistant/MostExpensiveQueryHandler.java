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

import java.util.Comparator;
import java.util.List;

public class MostExpensiveQueryHandler implements InventoryQueryHandler {

    @Override
    public boolean supports(String normalizedQuery) {
        return normalizedQuery.contains("mas caros")
                || normalizedQuery.contains("mayor costo")
                || normalizedQuery.contains("caro");
    }

    @Override
    public ChatbotAnswer handle(String normalizedQuery, List<Ingredient> ingredients) {
        List<Ingredient> expensive = ingredients.stream()
                .sorted(Comparator.comparingDouble(Ingredient::getEffectiveUnitPrice).reversed())
                .limit(4)
                .toList();

        if (expensive.isEmpty()) {
            return new ChatbotAnswer("MOST_EXPENSIVE", "No hay ingredientes registrados en el catálogo.", List.of());
        }

        StringBuilder sb = new StringBuilder("Top insumos de mayor costo real útil por unidad:\n");
        for (Ingredient ing : expensive) {
            sb.append(String.format("- %s (%s): %s / %s\n",
                    ing.getName(),
                    ing.getCategory(),
                    CurrencyFormatter.formatUSD(ing.getEffectiveUnitPrice()),
                    ing.getUnit().getSymbol()));
        }
        return new ChatbotAnswer("MOST_EXPENSIVE", sb.toString().trim(), List.of());
    }
}