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

import java.util.Comparator;
import java.util.List;

public class ExpiringSoonQueryHandler implements InventoryQueryHandler {

    @Override
    public boolean supports(String normalizedQuery) {
        return normalizedQuery.contains("vencer")
                || normalizedQuery.contains("caducar")
                || normalizedQuery.contains("perecedero")
                || normalizedQuery.contains("fecha");
    }

    @Override
    public ChatbotAnswer handle(String normalizedQuery, List<Ingredient> ingredients) {
        List<Ingredient> perishables = ingredients.stream()
                .filter(ing -> ing.isPerishable() && ing.getExpirationDate() != null)
                .sorted(Comparator.comparingLong(Ingredient::getDaysUntilExpiration))
                .limit(5)
                .toList();

        if (perishables.isEmpty()) {
            return new ChatbotAnswer("EXPIRING_SOON", "No hay ingredientes perecederos con fecha de caducidad asignada.", List.of());
        }

        StringBuilder sb = new StringBuilder("Control de caducidad de ingredientes perecederos:\n");
        for (Ingredient ing : perishables) {
            long days = ing.getDaysUntilExpiration();
            String status;
            if (days < 0) {
                status = "¡Venció hace " + Math.abs(days) + " día(s)!";
            } else if (days == 0) {
                status = "¡Vence HOY!";
            } else if (days <= 3) {
                status = "Alerta crítica: vence en " + days + " día(s)";
            } else {
                status = "Vence en " + days + " día(s)";
            }
            sb.append(String.format("- %s (%s): %s\n", ing.getName(), ing.getExpirationDate(), status));
        }
        return new ChatbotAnswer("EXPIRING_SOON", sb.toString().trim(), List.of());
    }
}