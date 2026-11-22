package com.culinaryops.infrastructure.persistence.memory;

import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.repository.IIngredientRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio en memoria para ingredientes, precargado con los 8 insumos semilla
 * de referencia definidos en la especificación de CulinaryOps.
 */
public class InMemoryIngredientRepository implements IIngredientRepository {

    private final Map<String, Ingredient> storage = new LinkedHashMap<>();

    /**
     * Inicializa el repositorio cargando automáticamente los 8 insumos semilla.
     */
    public InMemoryIngredientRepository() {
        this(true);
    }

    /**
     * Inicializa el repositorio indicando si debe precargar los datos semilla.
     *
     * @param loadSeedData {@code true} para precargar los 8 insumos iniciales
     */
    public InMemoryIngredientRepository(boolean loadSeedData) {
        if (loadSeedData) {
            seed();
        }
    }

    @Override
    public List<Ingredient> findAll() {
        return List.copyOf(storage.values());
    }

    @Override
    public Optional<Ingredient> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id.trim()));
    }

    @Override
    public Ingredient save(Ingredient ingredient) {
        if (ingredient == null) {
            throw new IllegalArgumentException("El ingrediente a guardar no puede ser nulo.");
        }
        storage.put(ingredient.getId(), ingredient);
        return ingredient;
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return storage.remove(id.trim()) != null;
    }

    private void seed() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        save(new Ingredient("ing-1", "Huevos de campo", "Lácteos y Huevos",
                UnitType.G, 0.0032, 12500.0, 15.0, true, today.plusDays(7), now, now));
        save(new Ingredient("ing-2", "Azúcar común refinada", "Secos y Endulzantes",
                UnitType.G, 0.0014, 35000.0, 1.0, false, null, now, now));
        save(new Ingredient("ing-3", "Harina 0000 de repostería", "Harinas y Almidones",
                UnitType.G, 0.0011, 40000.0, 2.0, false, null, now, now));
        save(new Ingredient("ing-4", "Mantequilla sin sal 82% MG", "Lácteos y Grasas",
                UnitType.G, 0.0085, 6000.0, 3.0, true, today.plusDays(20), now, now));
        save(new Ingredient("ing-5", "Chocolate Cobertura 70% Cacao", "Chocolates y Coberturas",
                UnitType.G, 0.014, 8500.0, 5.0, false, null, now, now));
        save(new Ingredient("ing-6", "Fresas / Frutillas seleccionadas", "Frutas y Frescos",
                UnitType.G, 0.0048, 3000.0, 18.0, true, today.plusDays(3), now, now));
        save(new Ingredient("ing-7", "Crema de leche / Nata 35% MG", "Lácteos y Grasas",
                UnitType.L, 4.50, 5.0, 4.0, true, today.plusDays(5), now, now));
        save(new Ingredient("ing-8", "Extracto de vainilla natural", "Aromas y Esencias",
                UnitType.G, 0.12, 250.0, 2.0, false, null, now, now));
    }
}
