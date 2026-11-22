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
import com.culinaryops.presentation.Part3Main;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryQueryAssistantTest {

    private InventoryQueryAssistant assistant;
    private List<Ingredient> ingredients;

    @BeforeEach
    public void setUp() {
        assistant = new InventoryQueryAssistant();
        ingredients = Part3Main.createSeedIngredients();
    }

    @Test
    @DisplayName("Debe identificar los insumos con mayor merma (Fresas 18% y Huevos 15%)")
    public void shouldAnswerHighestWasteQuery() {
        ChatbotAnswer answer = assistant.ask("¿Qué insumos tienen mayor merma?", ingredients);
        assertEquals("HIGHEST_WASTE", answer.getIntentHandled());
        assertTrue(answer.getMessage().contains("Fresas"));
        assertTrue(answer.getMessage().contains("18.0%"));
    }

    @Test
    @DisplayName("Debe listar ingredientes próximos a vencer ordenados por urgencia")
    public void shouldAnswerExpiringSoonQuery() {
        ChatbotAnswer answer = assistant.ask("¿Qué está por vencer?", ingredients);
        assertEquals("EXPIRING_SOON", answer.getIntentHandled());
        assertTrue(answer.getMessage().contains("Fresas"));
    }

    @Test
    @DisplayName("Debe calcular el valor total de compra y útil del inventario")
    public void shouldAnswerTotalInventoryValue() {
        ChatbotAnswer answer = assistant.ask("¿Cuánto vale mi inventario total?", ingredients);
        assertEquals("TOTAL_VALUE", answer.getIntentHandled());
        assertTrue(answer.getMessage().contains("8 insumos"));
    }

    @Test
    @DisplayName("Debe devolver ficha rápida al consultar por el nombre de un ingrediente")
    public void shouldFindIngredientByName() {
        ChatbotAnswer answer = assistant.ask("Dame datos del chocolate", ingredients);
        assertEquals("INGREDIENT_LOOKUP", answer.getIntentHandled());
        assertTrue(answer.getMessage().contains("Chocolate Cobertura"));
    }

    @Test
    @DisplayName("Debe devolver mensaje guía con sugerencias cuando no entiende la consulta")
    public void shouldReturnFallbackWithSuggestions() {
        ChatbotAnswer answer = assistant.ask("¿Cómo está el clima hoy?", ingredients);
        assertEquals("FALLBACK", answer.getIntentHandled());
        assertFalse(answer.getSuggestions().isEmpty());
    }
}
