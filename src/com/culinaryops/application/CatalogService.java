package com.culinaryops.application;

import com.culinaryops.domain.exception.DuplicateIngredientException;
import com.culinaryops.domain.exception.EntityNotFoundException;
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.repository.IIngredientRepository;
import com.culinaryops.domain.service.IngredientValidator;
import com.culinaryops.domain.service.InputRegexValidator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class CatalogService implements IQueryService<Ingredient> {

    private static final Pattern NUMERIC_SUFFIX_PATTERN = Pattern.compile("\\d+");

    private final IIngredientRepository ingredientRepository;


    public CatalogService(IIngredientRepository ingredientRepository) {
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
        String cleanId = requireNonBlankId(id);
        return ingredientRepository.findById(cleanId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el ingrediente con ID: " + cleanId));
    }

    public Ingredient createIngredient(
            String name,
            String category,
            UnitType unit,
            double unitPrice,
            double wastePercentage,
            boolean isPerishable) {
        return createIngredient(name, category, unit, unitPrice, 1.0, wastePercentage, isPerishable);
    }

    public Ingredient createIngredient(
            String name,
            String category,
            UnitType unit,
            double purchasePrice,
            double purchaseQuantity,
            double wastePercentage,
            boolean isPerishable) {
        validateNameAndDuplicate(name, null);
        String newId = generateNextId();
        Ingredient ingredient = new Ingredient(
                newId, name, category, unit, purchasePrice, purchaseQuantity, wastePercentage, isPerishable);
        return ingredientRepository.save(ingredient);
    }

    public Ingredient updateIngredient(
            String id,
            String name,
            String category,
            UnitType unit,
            double unitPrice,
            double wastePercentage,
            boolean isPerishable) {
        return updateIngredient(id, name, category, unit, unitPrice, 1.0, wastePercentage, isPerishable);
    }

    public Ingredient updateIngredient(
            String id,
            String name,
            String category,
            UnitType unit,
            double purchasePrice,
            double purchaseQuantity,
            double wastePercentage,
            boolean isPerishable) {
        Ingredient existing = findById(id);
        validateNameAndDuplicate(name, existing.getId());
        existing.updateTechnicalSheet(
                name, category, unit, purchasePrice, purchaseQuantity, wastePercentage, isPerishable);
        return ingredientRepository.save(existing);
    }

    public void deleteIngredient(String id) {
        String cleanId = requireNonBlankId(id);
        boolean deleted = ingredientRepository.deleteById(cleanId);
        if (!deleted) {
            throw new EntityNotFoundException("No se encontró el ingrediente con ID: " + cleanId);
        }
    }

    public List<String> getAvailableCategories() {
        Set<String> categories = new LinkedHashSet<>(IngredientValidator.DEFAULT_INGREDIENT_CATEGORIES);
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            categories.add(ingredient.getCategory());
        }
        return List.copyOf(categories);
    }

    @Override
    public List<Ingredient> search(String textQuery) {
        return search(textQuery, null);
    }

    public List<Ingredient> search(String textQuery, String category) {
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

            if (matchesText && matchesCategory) {
                result.add(ingredient);
            }
        }
        return List.copyOf(result);
    }

    private String generateNextId() {
        int maxId = 0;
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            String id = ingredient.getId();
            if (id.startsWith("ing-")) {
                String suffix = id.substring(4);
                if (NUMERIC_SUFFIX_PATTERN.matcher(suffix).matches()) {
                    maxId = Math.max(maxId, Integer.parseInt(suffix));
                }
            }
        }
        return "ing-" + (maxId + 1);
    }

    private void validateNameAndDuplicate(String name, String excludeId) {
        if (!InputRegexValidator.isValidName(name)) {
            throw new IllegalArgumentException(
                    "El nombre del ingrediente debe tener entre 2 y 150 caracteres válidos.");
        }
        Optional<Ingredient> duplicate = IngredientValidator.findDuplicateIngredient(
                name, ingredientRepository.findAll(), excludeId);
        if (duplicate.isPresent()) {
            throw new DuplicateIngredientException(
                    "Ya existe '" + duplicate.get().getName()
                            + "' en el catálogo. Para añadir más cantidad, registra una entrada en Inventario.");
        }
    }

    private static String requireNonBlankId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID del ingrediente es obligatorio.");
        }
        return id.trim();
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
