/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.culinaryops;

import com.culinaryops.application.BudgetService;
import com.culinaryops.application.CatalogService;
import com.culinaryops.application.InventoryService;
import com.culinaryops.application.RecipeService;
import com.culinaryops.domain.repository.IBudgetRepository;
import com.culinaryops.domain.repository.IIngredientRepository;
import com.culinaryops.domain.repository.IRecipeRepository;
import com.culinaryops.domain.service.InventoryQueryAssistant;
import com.culinaryops.domain.service.MoldCalculator;
import com.culinaryops.infrastructure.persistence.memory.InMemoryBudgetRepository;
import com.culinaryops.infrastructure.persistence.memory.InMemoryIngredientRepository;
import com.culinaryops.infrastructure.persistence.memory.InMemoryRecipeRepository;
import com.culinaryops.presentation.ui.CulinaryOpsFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada principal de la aplicación de escritorio ejecutable CulinaryOps.
 * Ensambla por inyección de dependencias (DIP) las Partes 1, 2 y 3 con la interfaz gráfica Swing.
 *
 * @author Luipi
 */
public class CulinaryOps {

    /**
     * Inicia la aplicación gráfica de CulinaryOps.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        IIngredientRepository ingredientRepo = new InMemoryIngredientRepository();
        IRecipeRepository recipeRepo = new InMemoryRecipeRepository(ingredientRepo);
        IBudgetRepository budgetRepo = new InMemoryBudgetRepository(recipeRepo);

        CatalogService catalogService = new CatalogService(ingredientRepo);
        InventoryService inventoryService = new InventoryService(ingredientRepo);
        RecipeService recipeService = new RecipeService(recipeRepo);
        BudgetService budgetService = new BudgetService(budgetRepo, recipeRepo);
        InventoryQueryAssistant assistant = new InventoryQueryAssistant();
        MoldCalculator moldCalculator = new MoldCalculator();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Si el sistema no permite el Look&Feel nativo, conserva el predeterminado
            }
            CulinaryOpsFrame frame = new CulinaryOpsFrame(
                    catalogService,
                    inventoryService,
                    recipeService,
                    budgetService,
                    assistant,
                    moldCalculator);
            frame.setVisible(true);
        });
    }
}
