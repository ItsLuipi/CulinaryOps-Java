/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model;

/**
 *
 * @author Luipi
 */
import com.culinaryops.domain.model.enums.UnitType;
import java.time.LocalDateTime;
import java.util.List;

public class Recipe extends BaseEntity implements Costable {

    private String name;
    private String description;
    private double yieldQuantity;
    private String yieldUnit;
    private int prepTimeMinutes;
    private String instructions;
    private List<RecipeIngredient> ingredients;
    
    public Recipe(
            String id,
            String name,
            String description,
            double yieldQuantity,
            String yieldUnit,
            int prepTimeMinutes,
            String instructions,
            List<RecipeIngredient> ingredients) {
        this(id, name, description, yieldQuantity, yieldUnit, prepTimeMinutes, instructions, ingredients, LocalDateTime.now(), LocalDateTime.now());
    }
    
    public Recipe(
            String id,
            String name,
            String description,
            double yieldQuantity,
            String yieldUnit,
            int prepTimeMinutes,
            String instructions,
            List<RecipeIngredient> ingredients,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.name = validateName(name);
        this.description = defaultIfBlank(description, "");
        this.yieldQuantity = validatePositive(yieldQuantity, "El rendimiento debe ser mayor a 0.");
        this.yieldUnit = defaultIfBlank(yieldUnit, "porciones");
        this.prepTimeMinutes = Math.max(0, prepTimeMinutes);
        this.instructions = defaultIfBlank(instructions, "");
        this.ingredients = copyIngredients(ingredients);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la receta es obligatorio.");
        }
        return name.trim();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private static double validatePositive(double value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static List<RecipeIngredient> copyIngredients(List<RecipeIngredient> ingredients) {
        if (ingredients == null) {
            throw new IllegalArgumentException("La lista de ingredientes no puede ser nula.");
        }
        return List.copyOf(ingredients); // Protege la lista contra nulls y cambios externos
    }

    public void updateDetails(
            String name,
            String description,
            double yieldQuantity,
            String yieldUnit,
            int prepTimeMinutes,
            String instructions,
            List<RecipeIngredient> ingredients) {
        this.name = validateName(name);
        this.description = defaultIfBlank(description, "");
        this.yieldQuantity = validatePositive(yieldQuantity, "El rendimiento debe ser mayor a 0.");
        this.yieldUnit = defaultIfBlank(yieldUnit, "porciones");
        this.prepTimeMinutes = Math.max(0, prepTimeMinutes);
        this.instructions = defaultIfBlank(instructions, "");
        this.ingredients = copyIngredients(ingredients);
        markUpdated();
    }
    
    
    public double calculateDirectCost() {
        double total = 0.0;
        for (RecipeIngredient item : ingredients) {
            total += item.getSubtotalCost();
        }
        return total;
    }

    @Override
    public double calculateCost() {
        return calculateDirectCost();
    }

    public double calculateCostPerUnit() {
        if (yieldQuantity <= 0) {
            return 0.0;
        }
        return calculateDirectCost() / yieldQuantity;
    }

    public double calculateTotalNetGrams() {
        double totalGrams = 0.0;
        for (RecipeIngredient item : ingredients) {
            double qty = item.getNetQuantity();
            totalGrams += switch (item.getUnit()) {
                case Gr, ML ->
                    qty;
                case KG, Lt ->
                    qty * 1000.0;
                case Und ->
                    qty * 55.0; // Peso aproximado de 1 huevo mediano = 55 g
            };
        }
        return totalGrams;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getYieldQuantity() {
        return yieldQuantity;
    }

    public String getYieldUnit() {
        return yieldUnit;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public String getInstructions() {
        return instructions;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }
}

