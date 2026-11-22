/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.service;

/**
 *
 * @author roco
 */
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.dto.ChatbotAnswer;
import com.culinaryops.domain.service.assistant.ExpiringSoonQueryHandler;
import com.culinaryops.domain.service.assistant.HighestWasteQueryHandler;
import com.culinaryops.domain.service.assistant.IngredientNameSearchHandler;
import com.culinaryops.domain.service.assistant.LowStockQueryHandler;
import com.culinaryops.domain.service.assistant.MostExpensiveQueryHandler;
import com.culinaryops.domain.service.assistant.TotalInventoryValueQueryHandler;

import java.text.Normalizer;
import java.util.List;

public class InventoryQueryAssistant {

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "¿Qué insumos tienen mayor merma?",
            "¿Qué ingredientes están por vencer?",
            "¿Cuánto vale mi inventario total?",
            "¿Qué insumos tienen stock bajo?",
            "¿Cuáles son los ingredientes más caros?"
    );

    private final List<InventoryQueryHandler> handlers;

    public InventoryQueryAssistant() {
        this(List.of(
                new HighestWasteQueryHandler(),
                new ExpiringSoonQueryHandler(),
                new TotalInventoryValueQueryHandler(),
                new LowStockQueryHandler(),
                new MostExpensiveQueryHandler(),
                new IngredientNameSearchHandler()
        ));
    }

    public InventoryQueryAssistant(List<InventoryQueryHandler> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            throw new IllegalArgumentException("Debe registrar al menos un manejador de consultas.");
        }
        this.handlers = List.copyOf(handlers);
    }

    public ChatbotAnswer ask(String query, List<Ingredient> ingredients) {
        List<Ingredient> safeIngredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        String normalized = normalize(query);

        if (!normalized.isEmpty()) {
            for (InventoryQueryHandler handler : handlers) {
                if (handler.supports(normalized)) {
                    ChatbotAnswer answer = handler.handle(normalized, safeIngredients);
                    if (answer != null) {
                        return answer;
                    }
                }
            }
        }

        return new ChatbotAnswer(
                "FALLBACK",
                "¡Hola! Soy tu Asistente Pastelero. Puedo ayudarte a revisar mermas, caducidades, valor del almacén, stock bajo o fichas de insumos.",
                DEFAULT_SUGGESTIONS
        );
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input.trim().toLowerCase(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}