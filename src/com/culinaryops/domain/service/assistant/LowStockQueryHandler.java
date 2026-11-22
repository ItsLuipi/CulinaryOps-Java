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
import com.culinaryops.domain.service.InventoryQueryHandler;

import java.util.List;
import java.util.Locale;

public class LowStockQueryHandler implements InventoryQueryHandler {

    @Override
    public boolean supports(String normalizedQuery) {
        return normalizedQuery.contains("stock bajo")
                || normalizedQuery.contains("agotando")
                || normalizedQuery.contains("comprar");
    }

    @Override
    public ChatbotAnswer handle(String normalizedQuery, List<Ingredient> ingredients) {
        List<Ingredient> lowStockItems = ingredients.stream()
                .filter(Ingredient::isLowStock)
                .toList();

        if (lowStockItems.isEmpty()) {
            return new ChatbotAnswer("LOW_STOCK", "Todos los insumos tienen existencias por encima del umbral mínimo.", List.of());
        }

        StringBuilder sb = new StringBuilder("Insumos con stock bajo que requieren reposición:\n");
        for (Ingredient ing : lowStockItems) {
            sb.append(String.format(Locale.US, "- %s: %.2f %s disponibles\n",
                    ing.getName(), ing.getStockQuantity(), ing.getUnit().getSymbol()));
        }
        return new ChatbotAnswer("LOW_STOCK", sb.toString().trim(), List.of());
    }
}