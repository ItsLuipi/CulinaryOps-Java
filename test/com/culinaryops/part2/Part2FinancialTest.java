package com.culinaryops.part2;

import com.culinaryops.domain.model.BudgetItem;
import com.culinaryops.domain.model.OperationalCosts;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.ServiceBudget;
import com.culinaryops.domain.model.dto.ClientQuoteDTO;
import com.culinaryops.domain.service.CurrencyFormatter;
import com.culinaryops.domain.service.PlainTextQuoteExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Part2FinancialTest {

    @Test
    @DisplayName("OperationalCosts: sanea valores negativos y calcula mano de obra y total")
    void testOperationalCostsSanitizationAndCalculation() {
        OperationalCosts empty = new OperationalCosts();
        assertEquals(0.0, empty.getTotalOperationalCost(), 0.0001);

        OperationalCosts costs = new OperationalCosts(5.5, 14.0, -10.0, 6.0, 15.0);
        assertEquals(77.0, costs.getLaborCost(), 0.0001);
        assertEquals(0.0, costs.getUtilitiesCost(), 0.0001); // Saneado de -10.0 a 0.0
        assertEquals(98.0, costs.getTotalOperationalCost(), 0.0001);
    }

    @Test
    @DisplayName("ServiceBudget: verifica cadena financiera completa y confidencialidad de ClientQuoteDTO")
    void testServiceBudgetFinancialChainAndQuoteConfidentiality() {
        // Simulación de receta de Parte 1 (costo directo $10.00, rinde 8 porciones)
        Recipe mockRecipe = new Recipe("rec-test", "Tarta Prueba", "Descripción", 8.0, "porciones", 45, "", List.of());
        // Si la receta tiene costo directo 0 en el mock sin ingredientes, probamos la estructura con 2 tandas:
        BudgetItem item = new BudgetItem(mockRecipe, 2.0);
        assertEquals(16.0, item.getTotalUnits(), 0.0001);

        OperationalCosts opCosts = new OperationalCosts(5.0, 10.0, 10.0, 5.0, 15.0); // Total = 80.0
        ServiceBudget budget = new ServiceBudget(
                "Mariana Gómez",
                "Boda Civil",
                LocalDate.of(2026, 12, 1),
                List.of(item),
                opCosts,
                35.0, // 35% margen
                10.0, // 10% impuesto
                "Entrega puntual"
        );

        double expectedBase = budget.getRawMaterialCost() + 80.0;
        double expectedProfit = expectedBase * 0.35;
        double expectedSuggested = expectedBase + expectedProfit;
        double expectedTax = expectedSuggested * 0.10;
        double expectedFinal = expectedSuggested + expectedTax;

        assertEquals(expectedBase, budget.getBaseCost(), 0.0001);
        assertEquals(expectedProfit, budget.getProfitAmount(), 0.0001);
        assertEquals(expectedSuggested, budget.getSuggestedPrice(), 0.0001);
        assertEquals(expectedTax, budget.getTaxAmount(), 0.0001);
        assertEquals(expectedFinal, budget.getFinalPrice(), 0.0001);

        // Validar confidencialidad del DTO y texto exportado
        ClientQuoteDTO dto = budget.generateClientQuote();
        assertEquals("Mariana Gómez", dto.clientName());
        assertEquals(expectedFinal, dto.totalPriceUSD(), 0.0001);

        String exported = new PlainTextQuoteExporter().export(dto);
        assertTrue(exported.contains("Mariana Gómez"));
        assertFalse(exported.contains("35.0%")); // No expone el margen interno
    }

    @Test
    @DisplayName("ServiceBudget: lanza excepción con cliente vacío o colección externa modificada")
    void testServiceBudgetInvariants() {
        assertThrows(IllegalArgumentException.class, () ->
                new ServiceBudget("   ", "Evento", null, List.of(), null, 30.0, 0.0, ""));

        ServiceBudget valid = new ServiceBudget("Cliente A", "Evento", null, List.of(), null, 30.0, 0.0, "");
        assertThrows(UnsupportedOperationException.class, () -> valid.getItems().clear());
    }

    @Test
    @DisplayName("CurrencyFormatter: sobrecargas para precisión automática (<0.05 -> 4 dec) y explícita")
    void testCurrencyFormatterOverloads() {
        assertEquals("$0.0032 USD", CurrencyFormatter.formatUSD(0.0032));
        assertEquals("$12.50 USD", CurrencyFormatter.formatUSD(12.5));
        assertEquals("$12.500", CurrencyFormatter.formatUSD(12.5, 3, false));
    }
}
