package com.culinaryops.application;

import com.culinaryops.domain.exception.EntityNotFoundException;
import com.culinaryops.domain.exception.InsufficientStockException;
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.enums.StockMovementType;
import com.culinaryops.domain.repository.IIngredientRepository;
import com.culinaryops.domain.service.IngredientValidator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class InventoryService implements IQueryService<Ingredient> {

    private final IIngredientRepository ingredientRepository;

    public InventoryService(IIngredientRepository ingredientRepository) {
        if (ingredientRepository == null) {
            throw new IllegalArgumentException("El repositorio de ingredientes es obligatorio.");
        }
        this.ingredientRepository = ingredientRepository;
    }

    @Override
    public List<Ingredient> findAll() {
        return ingredientRepository.findAll();
    }

    @Override
    public Ingredient findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID del ingrediente es obligatorio.");
        }
        String cleanId = id.trim();
        return ingredientRepository.findById(cleanId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el ingrediente con ID: " + cleanId));
    }

    public Ingredient applyMovement(String ingredientId, StockMovementType mode, double quantity) {
        return applyMovement(ingredientId, mode, quantity, null, null);
    }

    public Ingredient applyMovement(
            String ingredientId,
            StockMovementType mode,
            double quantity,
            Double updatedUnitPrice,
            LocalDate expirationDate) {
        Ingredient ingredient = findById(ingredientId);

        ingredient.applyStockMovement(mode, quantity, updatedUnitPrice, expirationDate);
        return ingredientRepository.save(ingredient);
    }

    @Override
    public List<Ingredient> search(String textQuery) {
        return search(textQuery, null, false, false);
    }

    public List<Ingredient> search(String textQuery, String category) {
        return search(textQuery, category, false, false);
    }

    public List<Ingredient> search(
            String textQuery,
            String category,
            boolean perishableOnly,
            boolean lowStockOnly) {
        String normalizedQuery = IngredientValidator.normalizeIngredientName(textQuery);
        boolean filterByCategory = isCategoryFilterActive(category);
        String targetCategory = filterByCategory ? category.trim() : "";

        List<Ingredient> result = new ArrayList<>();
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            boolean matchesText = normalizedQuery.isEmpty()
                    || IngredientValidator.normalizeIngredientName(ingredient.getName()).contains(normalizedQuery)
                    || IngredientValidator.normalizeIngredientName(ingredient.getCategory()).contains(normalizedQuery);
            boolean matchesCategory = !filterByCategory
                    || ingredient.getCategory().equalsIgnoreCase(targetCategory);
            boolean matchesPerishable = !perishableOnly || ingredient.isPerishable();
            boolean matchesLowStock = !lowStockOnly || ingredient.isLowStock();

            if (matchesText && matchesCategory && matchesPerishable && matchesLowStock) {
                result.add(ingredient);
            }
        }
        return List.copyOf(result);
    }

    public List<Ingredient> findLowStock() {
        return search(null, null, false, true);
    }

    public List<Ingredient> findLowStock(double threshold) {
        List<Ingredient> result = new ArrayList<>();
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            if (ingredient.isLowStock(threshold)) {
                result.add(ingredient);
            }
        }
        return List.copyOf(result);
    }

    public List<Ingredient> findExpiringSoon() {
        return findExpiringSoon(7);
    }

    public List<Ingredient> findExpiringSoon(int daysThreshold) {
        List<Ingredient> result = new ArrayList<>();
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            if (ingredient.isExpiringSoon(daysThreshold)) {
                result.add(ingredient);
            }
        }
        return List.copyOf(result);
    }

    public double getTotalInventoryValue() {
        double total = 0.0;
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            total += ingredient.getTotalStockValue();
        }
        return total;
    }

    public double getTotalEffectiveInventoryValue() {
        double total = 0.0;
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            total += ingredient.getTotalEffectiveStockValue();
        }
        return total;
    }

    private static boolean isCategoryFilterActive(String category) {
        if (category == null || category.isBlank()) {
            return false;
        }
        String trimmed = category.trim();
        return !trimmed.equalsIgnoreCase("Todas")
                && !trimmed.equalsIgnoreCase("Todas las Categorías");
    }
}
