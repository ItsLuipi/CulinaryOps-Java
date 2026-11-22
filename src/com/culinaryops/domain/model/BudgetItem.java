package com.culinaryops.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Línea de receta incluida dentro de un presupuesto comercial.
 * Vincula una {@link Recipe} con una cantidad de tandas a producir.
 */
public class BudgetItem implements Costable {

    private final String id;
    private final String recipeId;
    private final Recipe recipe;
    private final double quantity;

    /**
     * Constructor para crear un nuevo ítem de presupuesto generando un ID único.
     *
     * @param recipe   Receta asociada (no nula).
     * @param quantity Multiplicador de tandas (debe ser mayor a 0).
     * @throws IllegalArgumentException si {@code recipe} es nula o {@code quantity <= 0}.
     */
    public BudgetItem(Recipe recipe, double quantity) {
        this(UUID.randomUUID().toString(), recipe, quantity);
    }

    /**
     * Constructor completo para reconstrucción o asignación explícita de ID.
     *
     * @param id       Identificador del ítem.
     * @param recipe   Receta asociada (no nula).
     * @param quantity Multiplicador de tandas (debe ser mayor a 0).
     * @throws IllegalArgumentException si algún invariante no se cumple.
     */
    public BudgetItem(String id, Recipe recipe, double quantity) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del ítem de presupuesto no puede estar vacío.");
        }
        if (recipe == null) {
            throw new IllegalArgumentException("La receta del ítem de presupuesto no puede ser nula.");
        }
        if (Double.isNaN(quantity) || quantity <= 0.0) {
            throw new IllegalArgumentException("La cantidad de tandas debe ser mayor a 0.");
        }
        this.id = id.trim();
        this.recipe = recipe;
        this.recipeId = recipe.getId();
        this.quantity = quantity;
    }

    /**
     * Calcula el subtotal de materia prima para esta línea.
     *
     * @return {@code recipe.calculateDirectCost() * quantity} en USD.
     */
    public double getSubtotalCost() {
        return recipe.calculateDirectCost() * quantity;
    }

    /**
     * Calcula el rendimiento total de porciones o unidades resultantes.
     *
     * @return {@code recipe.getYieldQuantity() * quantity}.
     */
    public double getTotalUnits() {
        return recipe.getYieldQuantity() * quantity;
    }

    @Override
    public double calculateCost() {
        return getSubtotalCost();
    }

    public String getId() { return id; }
    public String getRecipeId() { return recipeId; }
    public Recipe getRecipe() { return recipe; }
    public double getQuantity() { return quantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BudgetItem that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}