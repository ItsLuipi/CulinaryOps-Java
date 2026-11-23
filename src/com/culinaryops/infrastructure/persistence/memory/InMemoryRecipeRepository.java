package com.culinaryops.infrastructure.persistence.memory;

import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.RecipeIngredient;
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.repository.IIngredientRepository;
import com.culinaryops.domain.repository.IRecipeRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio en memoria para recetas y escandallos, precargado con las 2 recetas
 * semilla de referencia conectadas al repositorio de ingredientes.
 */
public class InMemoryRecipeRepository implements IRecipeRepository {

    private final Map<String, Recipe> storage = new LinkedHashMap<>();

    /**
     * Inicializa el repositorio cargando las 2 recetas semilla usando el catálogo dado.
     *
     * @param ingredientRepository repositorio de ingredientes para hidratar las líneas
     * @throws IllegalArgumentException si {@code ingredientRepository} es nulo
     */
    public InMemoryRecipeRepository(IIngredientRepository ingredientRepository) {
        this(ingredientRepository, true);
    }

    /**
     * Inicializa el repositorio indicando si debe precargar las recetas semilla.
     *
     * @param ingredientRepository repositorio de ingredientes para hidratar las líneas
     * @param loadSeedData {@code true} para precargar las 2 recetas semilla
     * @throws IllegalArgumentException si {@code loadSeedData} es {@code true} y
     *                                  {@code ingredientRepository} es nulo
     */
    public InMemoryRecipeRepository(IIngredientRepository ingredientRepository, boolean loadSeedData) {
        if (loadSeedData) {
            if (ingredientRepository == null) {
                throw new IllegalArgumentException(
                        "El repositorio de ingredientes es obligatorio para cargar recetas semilla.");
            }
            seed(ingredientRepository);
        }
    }

    @Override
    public List<Recipe> findAll() {
        return List.copyOf(storage.values());
    }

    @Override
    public Optional<Recipe> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id.trim()));
    }

    @Override
    public Recipe save(Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("La receta a guardar no puede ser nula.");
        }
        storage.put(recipe.getId(), recipe);
        return recipe;
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return storage.remove(id.trim()) != null;
    }

    private void seed(IIngredientRepository ingredientRepo) {
        LocalDateTime now = LocalDateTime.now();

        Recipe rec1 = new Recipe(
                "rec-1",
                "Bizcochuelo Genovés Clásico (Génoise)",
                "Bizcochuelo base liviano y esponjoso para tartas de alta pastelería (molde redondo 20cm).",
                8.0,
                "porciones",
                45,
                "1. Batir huevos y azúcar a punto cinta. 2. Incorporar harina tamizada en forma envolvente. "
                        + "3. Añadir mantequilla derretida tibia y vainilla. 4. Hornear a 175°C durante 30 minutos.",
                List.of(
                        requireItem(ingredientRepo, "ing-1", UnitType.Gr, 250.0),
                        requireItem(ingredientRepo, "ing-2", UnitType.Gr, 150.0),
                        requireItem(ingredientRepo, "ing-3", UnitType.Gr, 150.0),
                        requireItem(ingredientRepo, "ing-4", UnitType.Gr, 30.0),
                        requireItem(ingredientRepo, "ing-8", UnitType.Gr, 5.0)
                ),
                now,
                now
        );

        Recipe rec2 = new Recipe(
                "rec-2",
                "Tarta Ganache 70% & Frutos Rojos",
                "Tarta cremosa de chocolate cobertura al 70% con crema fresca y fresas seleccionadas.",
                10.0,
                "porciones",
                60,
                "1. Calentar la crema de leche hasta hervor suave. 2. Volcar sobre el chocolate picado y emulsionar. "
                        + "3. Incorporar la mantequilla en cubos. 4. Decorar con fresas frescas al enfriar.",
                List.of(
                        requireItem(ingredientRepo, "ing-5", UnitType.Gr, 350.0),
                        requireItem(ingredientRepo, "ing-7", UnitType.ML, 300.0),
                        requireItem(ingredientRepo, "ing-4", UnitType.Gr, 50.0),
                        requireItem(ingredientRepo, "ing-6", UnitType.Gr, 250.0)
                ),
                now,
                now
        );

        save(rec1);
        save(rec2);
    }

    private static RecipeIngredient requireItem(
            IIngredientRepository ingredientRepo,
            String ingredientId,
            UnitType unit,
            double netQuantity) {
        Ingredient ingredient = ingredientRepo.findById(ingredientId)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el insumo semilla requerido: " + ingredientId));
        return new RecipeIngredient(ingredient, unit, netQuantity);
    }
}
