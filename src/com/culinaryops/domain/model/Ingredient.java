/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model;

import com.culinaryops.domain.exception.InsufficientStockException;
import com.culinaryops.domain.model.enums.StockMovementType;
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.model.enums.WasteLevel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


public class Ingredient extends BaseEntity {

    private String name;
    private String category;
    private UnitType unit;
    private double unitPrice;
    private double stockQuantity;
    private double wastePercentage;
    private boolean isPerishable;
    private LocalDate expirationDate;

    public Ingredient(
            String id,
            String name,
            String category,
            UnitType unit,
            double unitPrice,
            double wastePercentage,
            boolean isPerishable) {
        this(id, name, category, unit, unitPrice, 0.0, wastePercentage,
             isPerishable, null, LocalDateTime.now(), LocalDateTime.now());
    }

    public Ingredient(
            String id,
            String name,
            String category,
            UnitType unit,
            double unitPrice,
            double stockQuantity,
            double wastePercentage,
            boolean isPerishable,
            LocalDate expirationDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.name = validateName(name);
        this.category = normalizeCategory(category);
        this.unit = validateUnit(unit);
        this.unitPrice = validateNonNegative(unitPrice, "El precio no puede ser menor a 0.");
        this.stockQuantity = validateNonNegative(stockQuantity, "La cantidad en el inventario no puede ser menor a 0.");
        this.wastePercentage = validateWastePercentage(wastePercentage);
        this.isPerishable = isPerishable;
        this.expirationDate = isPerishable ? expirationDate : null;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("No puede estar vacío el nombre.");
        }
        return name.trim();
    }

    private static String normalizeCategory(String category) {
        return (category == null || category.isBlank()) ? "General" : category.trim();
    }

    private static UnitType validateUnit(UnitType unit) {
        if (unit == null) {
            throw new IllegalArgumentException("La unidad es obligatoria.");
        }
        return unit;
    }

    private static double validateNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static double validateWastePercentage(double wastePercentage) {
        if (wastePercentage < 0 || wastePercentage >= 100) {
            throw new IllegalArgumentException("El porcentaje no puede ser menor a 0 o mayor/igual a 100.");
        }
        return wastePercentage;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public UnitType getUnit() {
        return unit;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getStockQuantity() {
        return stockQuantity;
    }

    public double getWastePercentage() {
        return wastePercentage;
    }

    public boolean isPerishable() {
        return isPerishable;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void updateTechnicalSheet(
            String name,
            String category,
            UnitType unit,
            double unitPrice,
            double wastePercentage,
            boolean isPerishable) {
        this.name = validateName(name);
        this.category = normalizeCategory(category);
        this.unit = validateUnit(unit);
        this.unitPrice = validateNonNegative(unitPrice, "El precio no puede ser menor a 0.");
        this.wastePercentage = validateWastePercentage(wastePercentage);
        this.isPerishable = isPerishable;
        if (!isPerishable) {
            this.expirationDate = null;
        }
        markUpdated();
    }


    public void applyStockMovement(StockMovementType mode, double quantity) {
        applyStockMovement(mode, quantity, null, null);
    }

    public void applyStockMovement(
            StockMovementType mode,
            double quantity,
            Double updatedUnitPrice,
            LocalDate newExpirationDate) {
        if (mode == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio.");
        }
        validateNonNegative(quantity, "La cantidad del movimiento no puede ser negativa.");
        if (updatedUnitPrice != null) {
            validateNonNegative(updatedUnitPrice, "El precio no puede ser menor a 0.");
        }

        double nextStock = switch (mode) {
            case ADD -> this.stockQuantity + quantity;
            case SUBTRACT -> this.stockQuantity - quantity;
            case SET -> quantity;
        };

        if (nextStock < 0) {
            throw new InsufficientStockException(
                    "Stock insuficiente para '" + this.name + "'. Disponible: "
                            + this.stockQuantity + " " + this.unit.getSymbol());
        }

        this.stockQuantity = nextStock;
        if (updatedUnitPrice != null) {
            this.unitPrice = updatedUnitPrice;
        }
        if (this.isPerishable && newExpirationDate != null) {
            this.expirationDate = newExpirationDate;
        }
        markUpdated();
    }

    public double getEffectiveUnitPrice() {
        double yieldFactor = 1.0 - (wastePercentage / 100.0);
        if (yieldFactor <= 0) {
            return unitPrice;
        }
        return unitPrice / yieldFactor;
    }


    public double getTotalStockValue() {
        return stockQuantity * unitPrice;
    }

    public double getTotalEffectiveStockValue() {
        double usefulQuantity = stockQuantity * (1.0 - (wastePercentage / 100.0));
        return usefulQuantity * getEffectiveUnitPrice();
    }

    public WasteLevel getWasteLevel() {
        if (wastePercentage < 3.0) {
            return WasteLevel.LOW;
        }
        if (wastePercentage <= 10.0) {
            return WasteLevel.MEDIUM;
        }
        return WasteLevel.HIGH;
    }

    public boolean isLowStock() {
        return isLowStock(2.0);
    }

    public boolean isLowStock(double threshold) {
        return this.stockQuantity <= threshold;
    }

    public Long getDaysUntilExpiration() {
        if (expirationDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    public boolean isExpiringSoon() {
        return isExpiringSoon(7);
    }

    public boolean isExpiringSoon(int daysThreshold) {
        Long days = getDaysUntilExpiration();
        return days != null && days <= daysThreshold;
    }
}
