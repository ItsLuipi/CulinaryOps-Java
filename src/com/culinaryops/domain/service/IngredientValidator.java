/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.service;

import com.culinaryops.domain.model.Ingredient;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validador de dominio para categorías estándar y detección de ingredientes duplicados.
 *
 * @author Luipi
 */
public final class IngredientValidator {

    public static final List<String> DEFAULT_INGREDIENT_CATEGORIES = List.of(
        "Lácteos y Huevos",
        "Secos y Endulzantes",
        "Harinas y Almidones",
        "Lácteos y Grasas",
        "Chocolates y Coberturas",
        "Frutas y Frescos",
        "Aromas y Esencias",
        "Frutos Secos y Semillas"
    );

    private static final Pattern DIACRITICS_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern MULTIPLE_SPACES_PATTERN =
            Pattern.compile("\\s+");

    private IngredientValidator() {
    }

    public static String normalizeIngredientName(String name) {
        if (name == null) {
            return "";
        }
        String trimmedLower = name.trim().toLowerCase();
        String decomposed = Normalizer.normalize(trimmedLower, Normalizer.Form.NFD);
        String withoutAccents = DIACRITICS_PATTERN.matcher(decomposed).replaceAll("");
        return MULTIPLE_SPACES_PATTERN.matcher(withoutAccents).replaceAll(" ");
    }

    public static Optional<Ingredient> findDuplicateIngredient(
            String name,
            List<Ingredient> existingIngredients,
            String excludeId) {
        String normalizedTarget = normalizeIngredientName(name);
        if (normalizedTarget.isEmpty() || existingIngredients == null) {
            return Optional.empty();
        }
        for (Ingredient ingredient : existingIngredients) {
            if (excludeId != null && ingredient.getId().equals(excludeId)) {
                continue;
            }
            if (normalizeIngredientName(ingredient.getName()).equals(normalizedTarget)) {
                return Optional.of(ingredient);
            }
        }
        return Optional.empty();
    }
}
