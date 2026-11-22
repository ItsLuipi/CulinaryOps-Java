/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.application;

import com.culinaryops.domain.exception.CulinaryDomainException;
import com.culinaryops.domain.exception.DuplicateIngredientException;
import com.culinaryops.domain.exception.EntityNotFoundException;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.RecipeIngredient;
import com.culinaryops.domain.repository.IRecipeRepository;
import com.culinaryops.domain.service.IngredientValidator;
import com.culinaryops.domain.service.InputRegexValidator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class RecipeService implements IQueryService<Recipe> {

    private static final Pattern NUMERIC_SUFFIX_PATTERN = Pattern.compile("\\d+");

    private final IRecipeRepository recipeRepository;

    public RecipeService(IRecipeRepository recipeRepository) {
        if (recipeRepository == null) {
            throw new IllegalArgumentException("El repositorio de recetas es obligatorio.");
        }
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    @Override
    public Recipe findById(String id) {
        String cleanId = requireNonBlankId(id);
        return recipeRepository.findById(cleanId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la receta con ID: " + cleanId));
    }

    @Override
    public List<Recipe> search(String textQuery) {
        String normalizedQuery = IngredientValidator.normalizeIngredientName(textQuery);
        if (normalizedQuery.isEmpty()) {
            return recipeRepository.findAll();
        }
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : recipeRepository.findAll()) {
            boolean matchesName = IngredientValidator.normalizeIngredientName(recipe.getName())
                    .contains(normalizedQuery);
            boolean matchesDescription = IngredientValidator.normalizeIngredientName(recipe.getDescription())
                    .contains(normalizedQuery);
            if (matchesName || matchesDescription) {
                result.add(recipe);
            }
        }
        return List.copyOf(result);
    }

    public Recipe createRecipe(
            String name,
            String description,
            double yieldQuantity,
            String yieldUnit,
            int prepTimeMinutes,
            String instructions,
            List<RecipeIngredient> ingredients) {
        validateNameAndUnique(name, null);
        validateIngredients(ingredients);
        String newId = generateNextId();
        Recipe recipe = new Recipe(
                newId, name, description, yieldQuantity, yieldUnit,
                prepTimeMinutes, instructions, ingredients);
        return recipeRepository.save(recipe);
    }

    public Recipe updateRecipe(
            String id,
            String name,
            String description,
            double yieldQuantity,
            String yieldUnit,
            int prepTimeMinutes,
            String instructions,
            List<RecipeIngredient> ingredients) {
        Recipe existing = findById(id);
        validateNameAndUnique(name, existing.getId());
        validateIngredients(ingredients);
        existing.updateDetails(
                name, description, yieldQuantity, yieldUnit,
                prepTimeMinutes, instructions, ingredients);
        return recipeRepository.save(existing);
    }

    public void deleteRecipe(String id) {
        String cleanId = requireNonBlankId(id);
        boolean deleted = recipeRepository.deleteById(cleanId);
        if (!deleted) {
            throw new EntityNotFoundException("No se encontró la receta con ID: " + cleanId);
        }
    }

    private void validateNameAndUnique(String name, String excludeId) {
        if (!InputRegexValidator.isValidName(name)) {
            throw new IllegalArgumentException(
                    "El nombre de la receta debe tener entre 2 y 150 caracteres válidos.");
        }
        String normalizedTarget = IngredientValidator.normalizeIngredientName(name);
        for (Recipe existing : recipeRepository.findAll()) {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            if (IngredientValidator.normalizeIngredientName(existing.getName()).equals(normalizedTarget)) {
                throw new CulinaryDomainException(
                        "Ya existe una receta con el nombre: '" + existing.getName() + "'.");
            }
        }
    }

    private static void validateIngredients(List<RecipeIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("La receta debe tener al menos un ingrediente.");
        }
        Set<String> seenIngredientIds = new HashSet<>();
        for (RecipeIngredient item : ingredients) {
            if (item == null) {
                throw new IllegalArgumentException("La lista de ingredientes no puede contener elementos nulos.");
            }
            if (!seenIngredientIds.add(item.getIngredientId())) {
                throw new DuplicateIngredientException(
                        "No se puede repetir el ingrediente '" + item.getIngredient().getName()
                                + "' dentro de la misma receta.");
            }
        }
    }

    private String generateNextId() {
        int maxId = 0;
        for (Recipe recipe : recipeRepository.findAll()) {
            String id = recipe.getId();
            if (id.startsWith("rec-")) {
                String suffix = id.substring(4);
                if (NUMERIC_SUFFIX_PATTERN.matcher(suffix).matches()) {
                    maxId = Math.max(maxId, Integer.parseInt(suffix));
                }
            }
        }
        return "rec-" + (maxId + 1);
    }

    private static String requireNonBlankId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de la receta es obligatorio.");
        }
        return id.trim();
    }
}
