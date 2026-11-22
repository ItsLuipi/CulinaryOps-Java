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
import com.culinaryops.domain.service.InventoryQueryAssistant;
import com.culinaryops.domain.service.InventoryQueryHandler;

import java.util.List;
import java.util.Locale;

public class IngredientNameSearchHandler implements InventoryQueryHandler {

    @Override
    public boolean supports(String normalizedQuery) {
        return normalizedQuery != null && normalizedQuery.length() >= 3;
    }

    @Override
    public ChatbotAnswer handle(String normalizedQuery, List<Ingredient> ingredients) {
        for (Ingredient ing : ingredients) {
            String normalizedName = InventoryQueryAssistant.normalize(ing.getName());
            String firstWord = normalizedName.split(" ")[0];
            if (normalizedQuery.contains(normalizedName)
                    || (firstWord.length() >= 4 && normalizedQuery.contains(firstWord))) {
                String msg = String.format(Locale.US,
                        "Ficha rápida — %s (%s):\n" +
                        "- Stock físico: %.2f %s\n" +
                        "- Precio compra: %s/%s | Merma: %.1f%%\n" +
                        "- Costo real útil: %s/%s\n" +
                        "- Valor en almacén: %s",
                        ing.getName(),
                        ing.getCategory(),
                        ing.getStockQuantity(),
                        ing.getUnit().getSymbol(),
                        CurrencyFormatter.formatUSD(ing.getUnitPrice()),
                        ing.getUnit().getSymbol(),
                        ing.getWastePercentage(),
                        CurrencyFormatter.formatUSD(ing.getEffectiveUnitPrice()),
                        ing.getUnit().getSymbol(),
                        CurrencyFormatter.formatUSD(ing.getTotalStockValue())
                );
                return new ChatbotAnswer("INGREDIENT_LOOKUP", msg, List.of());
            }
        }
        return null;
    }
}