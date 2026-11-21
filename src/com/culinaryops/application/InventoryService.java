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

/**
 * Servicio de aplicación para gestionar existencias físicas, movimientos de stock,
 * alertas de reposición/caducidad y valoración del inventario.
 */
public class InventoryService {

    private final IIngredientRepository ingredientRepository;

    /**
     * Crea el servicio inyectando su repositorio de ingredientes.
     *
     * @param ingredientRepository repositorio de persistencia de ingredientes
     * @throws IllegalArgumentException si {@code ingredientRepository} es nulo
     */
    public InventoryService(IIngredientRepository ingredientRepository) {
        if (ingredientRepository == null) {
            throw new IllegalArgumentException("El repositorio de ingredientes es obligatorio.");
        }
        this.ingredientRepository = ingredientRepository;
    }

    /**
     * @return lista completa de ingredientes con sus existencias actuales
     */
    public List<Ingredient> findAll() {
        return ingredientRepository.findAll();
    }

    /**
     * Aplica un movimiento simple de inventario ({@code ADD}, {@code SUBTRACT} o {@code SET}).
     *
     * @param ingredientId identificador del ingrediente
     * @param mode tipo de movimiento
     * @param quantity cantidad del movimiento
     * @return ingrediente actualizado tras persistir el cambio
     * @throws IllegalArgumentException si algún parámetro es inválido
     * @throws EntityNotFoundException si el ingrediente no existe
     * @throws InsufficientStockException si el stock resultante sería negativo
     */
    public Ingredient applyMovement(String ingredientId, StockMovementType mode, double quantity) {
        return applyMovement(ingredientId, mode, quantity, null, null);
    }

    /**
     * Aplica un movimiento de inventario permitiendo actualizar opcionalmente el precio de compra
     * y la fecha de caducidad del lote.
     *
     * @param ingredientId identificador del ingrediente
     * @param mode tipo de movimiento ({@code ADD}, {@code SUBTRACT} o {@code SET})
     * @param quantity cantidad del movimiento
     * @param updatedUnitPrice nuevo precio unitario opcional en USD (null para conservar el actual)
     * @param expirationDate nueva fecha de caducidad opcional (solo aplica si es perecedero)
     * @return ingrediente actualizado tras persistir el cambio
     * @throws IllegalArgumentException si algún parámetro es inválido
     * @throws EntityNotFoundException si el ingrediente no existe
     * @throws InsufficientStockException si el stock resultante sería negativo
     */
    public Ingredient applyMovement(
            String ingredientId,
            StockMovementType mode,
            double quantity,
            Double updatedUnitPrice,
            LocalDate expirationDate) {
        if (ingredientId == null || ingredientId.isBlank()) {
            throw new IllegalArgumentException("El ID del ingrediente es obligatorio.");
        }
        String cleanId = ingredientId.trim();
        Ingredient ingredient = ingredientRepository.findById(cleanId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el ingrediente con ID: " + cleanId));

        ingredient.applyStockMovement(mode, quantity, updatedUnitPrice, expirationDate);
        return ingredientRepository.save(ingredient);
    }

    /**
     * Busca ingredientes en inventario por texto libre.
     *
     * @param textQuery texto a buscar en nombre o categoría
     * @return lista inmutable de ingredientes coincidentes
     */
    public List<Ingredient> search(String textQuery) {
        return search(textQuery, null, false, false);
    }

    /**
     * Busca ingredientes en inventario filtrando por texto y categoría.
     *
     * @param textQuery texto a buscar en nombre o categoría
     * @param category categoría a filtrar (null/vacío/"Todas..." para incluir todas)
     * @return lista inmutable de ingredientes coincidentes
     */
    public List<Ingredient> search(String textQuery, String category) {
        return search(textQuery, category, false, false);
    }

    /**
     * Busca ingredientes en inventario combinando texto, categoría y filtros operativos.
     *
     * @param textQuery texto a buscar en nombre o categoría
     * @param category categoría a filtrar (null/vacío/"Todas..." para incluir todas)
     * @param perishableOnly si es {@code true}, incluye solo insumos perecederos
     * @param lowStockOnly si es {@code true}, incluye solo insumos con stock bajo
     * @return lista inmutable de ingredientes coincidentes
     */
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

    /**
     * Lista todos los ingredientes con stock bajo usando el umbral por defecto.
     *
     * @return lista inmutable de insumos en stock bajo
     */
    public List<Ingredient> findLowStock() {
        return search(null, null, false, true);
    }

    /**
     * Lista todos los ingredientes cuyo stock físico es menor o igual al umbral indicado.
     *
     * @param threshold límite mínimo de stock
     * @return lista inmutable de insumos por debajo o igual al umbral
     */
    public List<Ingredient> findLowStock(double threshold) {
        List<Ingredient> result = new ArrayList<>();
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            if (ingredient.isLowStock(threshold)) {
                result.add(ingredient);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Lista los insumos perecederos próximos a vencer (7 días o menos por defecto).
     *
     * @return lista inmutable de insumos próximos a vencer o ya vencidos
     */
    public List<Ingredient> findExpiringSoon() {
        return findExpiringSoon(7);
    }

    /**
     * Lista los insumos perecederos que vencen dentro del umbral de días indicado.
     *
     * @param daysThreshold días máximos hasta el vencimiento
     * @return lista inmutable de insumos próximos a vencer o ya vencidos
     */
    public List<Ingredient> findExpiringSoon(int daysThreshold) {
        List<Ingredient> result = new ArrayList<>();
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            if (ingredient.isExpiringSoon(daysThreshold)) {
                result.add(ingredient);
            }
        }
        return List.copyOf(result);
    }

    /**
     * @return suma del valor monetario bruto en almacén de todos los insumos (USD)
     */
    public double getTotalInventoryValue() {
        double total = 0.0;
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            total += ingredient.getTotalStockValue();
        }
        return total;
    }

    /**
     * @return suma del valor monetario neto aprovechable considerando mermas (USD)
     */
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
