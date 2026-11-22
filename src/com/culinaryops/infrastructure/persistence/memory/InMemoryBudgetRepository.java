package com.culinaryops.infrastructure.persistence.memory;

import com.culinaryops.domain.model.BudgetItem;
import com.culinaryops.domain.model.OperationalCosts;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.ServiceBudget;
import com.culinaryops.domain.model.enums.BudgetStatus;
import com.culinaryops.domain.repository.IBudgetRepository;
import com.culinaryops.domain.repository.IRecipeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación en memoria de {@link IBudgetRepository}.
 * Precarga automáticamente el presupuesto semilla de "Mariana Gómez" si existen las recetas base.
 */
public class InMemoryBudgetRepository implements IBudgetRepository {

    private final Map<String, ServiceBudget> storage = new LinkedHashMap<>();

    /**
     * Constructor vacío sin datos semilla.
     */
    public InMemoryBudgetRepository() {
    }

    /**
     * Constructor que inicializa el repositorio precargando el presupuesto semilla de referencia
     * a partir de las recetas registradas en la Parte 1.
     *
     * @param recipeRepository Repositorio de recetas para hidratar los ítems semilla.
     */
    public InMemoryBudgetRepository(IRecipeRepository recipeRepository) {
        if (recipeRepository != null) {
            seedInitialBudget(recipeRepository);
        }
    }

    private void seedInitialBudget(IRecipeRepository recipeRepository) {
        Optional<Recipe> genoise = recipeRepository.findById("rec-1");
        Optional<Recipe> ganache = recipeRepository.findById("rec-2");

        if (genoise.isPresent() && ganache.isPresent()) {
            List<BudgetItem> seedItems = List.of(
                    new BudgetItem("bi-1", genoise.get(), 3.0),
                    new BudgetItem("bi-2", ganache.get(), 2.0)
            );
            OperationalCosts seedCosts = new OperationalCosts(5.5, 14.0, 9.5, 6.0, 15.0);
            ServiceBudget seedBudget = new ServiceBudget(
                    "bud-1",
                    "Mariana Gómez",
                    "Mesa Dulce para Boda Civil",
                    LocalDate.now().plusDays(14),
                    BudgetStatus.DRAFT,
                    seedItems,
                    seedCosts,
                    35.0,
                    0.0,
                    "Entrega a las 16:00 hs con refrigeración garantizada.",
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            storage.put(seedBudget.getId(), seedBudget);
        }
    }

    @Override
    public List<ServiceBudget> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Optional<ServiceBudget> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public ServiceBudget save(ServiceBudget budget) {
        if (budget == null) {
            throw new IllegalArgumentException("El presupuesto a guardar no puede ser nulo.");
        }
        storage.put(budget.getId(), budget);
        return budget;
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        return storage.remove(id) != null;
    }
}