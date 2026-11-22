/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model;

import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.service.UnitConverter;


public class RecipeIngredient implements Costable {

    private final String id;
    private final String ingredientId;
    private final Ingredient ingredient;
    private final UnitType unit;
    private final double netQuantity;

    public RecipeIngredient(Ingredient ingredient, UnitType unit, double netQuantity) {
        this(null, ingredient, unit, netQuantity);
    }

    public RecipeIngredient(String id, Ingredient ingredient, UnitType unit, double netQuantity) {
        if (ingredient == null) {
            throw new IllegalArgumentException("El ingrediente es obligatorio.");
        }
        if (unit == null) {
            throw new IllegalArgumentException("La unidad es obligatoria.");
        }
        if (netQuantity <= 0) {
            throw new IllegalArgumentException("La cantidad neta debe ser mayor a 0.");
        }
        this.id = (id == null || id.isBlank()) ? null : id.trim();
        this.ingredient = ingredient;
        this.ingredientId = ingredient.getId();
        this.unit = unit;
        this.netQuantity = netQuantity;
    }

    public String getId() {
        return id;
    }

    public String getIngredientId() {
        return ingredientId;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public UnitType getUnit() {
        return unit;
    }

    public double getNetQuantity() {
        return netQuantity;
    }

    public double getRawQuantityRequired() {
        double yieldFactor = 1.0 - (ingredient.getWastePercentage() / 100.0);
        if (yieldFactor <= 0) {
            return netQuantity;
        }
        return netQuantity / yieldFactor;
    }

    public double getRawQuantityInBaseUnit() {
        return UnitConverter.convert(getRawQuantityRequired(), this.unit, this.ingredient.getUnit());
    }

    public double getSubtotalCost() {
        return getRawQuantityInBaseUnit() * ingredient.getUnitPrice();
    }

    @Override
    public double calculateCost() {
        return getSubtotalCost();
    }
}
