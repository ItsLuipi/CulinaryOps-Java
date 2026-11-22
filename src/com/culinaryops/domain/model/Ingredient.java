/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model;

/**
 *
 * @author roco
 */
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.model.enums.WasteLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Ingredient {

    private final String id;
    private String name;
    private String category;
    private UnitType unit;
    private double unitPrice;
    private double stockQuantity;
    private double wastePercentage;
    private boolean isPerishable;
    private LocalDate expirationDate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Ingredient(String id,
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
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del ingrediente es obligatorio.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del ingrediente es obligatorio.");
        }
        if (unit == null) {
            throw new IllegalArgumentException("La unidad de medida es obligatoria.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo.");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
        if (wastePercentage < 0 || wastePercentage >= 100) {
            throw new IllegalArgumentException("La merma debe estar entre 0% y menos de 100%.");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.category = (category == null || category.trim().isEmpty()) ? "General" : category.trim();
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.wastePercentage = wastePercentage;
        this.isPerishable = isPerishable;
        this.expirationDate = isPerishable ? expirationDate : null;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public Ingredient(String id,
                      String name,
                      String category,
                      UnitType unit,
                      double unitPrice,
                      double wastePercentage,
                      boolean isPerishable) {
        this(id, name, category, unit, unitPrice, 0.0, wastePercentage, isPerishable, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public double getEffectiveUnitPrice() {
        double yieldFactor = 1.0 - (wastePercentage / 100.0);
        return yieldFactor <= 0 ? unitPrice : unitPrice / yieldFactor;
    }

    public double getTotalStockValue() {
        return stockQuantity * unitPrice;
    }

    public double getTotalEffectiveStockValue() {
        double usefulQuantity = stockQuantity * (1.0 - (wastePercentage / 100.0));
        return usefulQuantity * getEffectiveUnitPrice();
    }

    public boolean isLowStock() {
        return isLowStock(2.0);
    }

    public boolean isLowStock(double threshold) {
        return stockQuantity <= threshold;
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

    public WasteLevel getWasteLevel() {
        if (wastePercentage < 3.0) {
            return WasteLevel.LOW;
        }
        if (wastePercentage <= 10.0) {
            return WasteLevel.MEDIUM;
        }
        return WasteLevel.HIGH;
    }

    public String getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}