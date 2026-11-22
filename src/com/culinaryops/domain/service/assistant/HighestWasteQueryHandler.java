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
import java.util.Locale;

public class HighestWasteQueryHandler implements InventoryQueryHandler {

    @Override
    public boolean supports(String normalizedQuery) {
        return normalizedQuery.contains("mayor merma")
                || normalizedQuery.contains("mas merma")
                || normalizedQuery.contains("mermas altas")
                || normalizedQuery.contains("merma");
    }

    @Override
    public ChatbotAnswer handle(String normalizedQuery, List<Ingredient> ingredients) {
        List<Ingredient> top = ingredients.stream()
                .sorted(Comparator.comparingDouble(Ingredient::getWastePercentage).reversed())
                .limit(4)
                .toList();

        if (top.isEmpty()) {
            return new ChatbotAnswer("HIGHEST_WASTE", "No hay ingredientes registrados en el catálogo.", List.of());
        }

        StringBuilder sb = new StringBuilder("Top insumos con mayor porcentaje de merma:\n");
        for (Ingredient ing : top) {
            sb.append(String.format(Locale.US,
                    "- %s: %.1f%% merma | Compra: %s/%s -> Real útil: %s/%s\n",
                    ing.getName(),
                    ing.getWastePercentage(),
                    CurrencyFormatter.formatUSD(ing.getUnitPrice()),
                    ing.getUnit().getSymbol(),
                    CurrencyFormatter.formatUSD(ing.getEffectiveUnitPrice()),
                    ing.getUnit().getSymbol()));
        }
        return new ChatbotAnswer("HIGHEST_WASTE", sb.toString().trim(), List.of());
    }
}