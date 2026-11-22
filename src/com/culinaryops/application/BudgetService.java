package com.culinaryops.application;

import com.culinaryops.domain.contract.QuoteExporter;
import com.culinaryops.domain.exception.EntityNotFoundException;
import com.culinaryops.domain.model.BudgetItem;
import com.culinaryops.domain.model.OperationalCosts;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.ServiceBudget;
import com.culinaryops.domain.model.dto.ClientQuoteDTO;
import com.culinaryops.domain.model.enums.BudgetStatus;
import com.culinaryops.domain.repository.IBudgetRepository;
import com.culinaryops.domain.repository.IRecipeRepository;
import com.culinaryops.domain.service.PlainTextQuoteExporter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicación encargado de orquestar la creación, modificación, cálculo financiero
 * y exportación de cotizaciones comerciales. Aplica Inversión de Dependencias (DIP).
 */
public class BudgetService {

    private final IBudgetRepository budgetRepository;
    private final IRecipeRepository recipeRepository;
    private final QuoteExporter quoteExporter;

    /**
     * Constructor principal con inyección de dependencias.
     *
     * @param budgetRepository Repositorio de presupuestos.
     * @param recipeRepository Repositorio de recetas (Parte 1).
     */
    public BudgetService(IBudgetRepository budgetRepository, IRecipeRepository recipeRepository) {
        this(budgetRepository, recipeRepository, new PlainTextQuoteExporter());
    }

    /**
     * Constructor sobrecargado que permite inyectar un exportador de cotizaciones personalizado.
     *
     * @param budgetRepository Repositorio de presupuestos.
     * @param recipeRepository Repositorio de recetas.
     * @param quoteExporter    Estrategia de exportación comercial.
     */
    public BudgetService(IBudgetRepository budgetRepository, IRecipeRepository recipeRepository,
                         QuoteExporter quoteExporter) {
        if (budgetRepository == null || recipeRepository == null || quoteExporter == null) {
            throw new IllegalArgumentException("Las dependencias de BudgetService no pueden ser nulas.");
        }
        this.budgetRepository = budgetRepository;
        this.recipeRepository = recipeRepository;
        this.quoteExporter = quoteExporter;
    }

    /**
     * Crea y persiste un nuevo presupuesto resolviendo las recetas desde el repositorio.
     *
     * @param clientName             Nombre del cliente.
     * @param eventName              Nombre del evento.
     * @param eventDate              Fecha del evento.
     * @param recipeQuantities       Mapa de {@code recipeId -> cantidad de tandas}.
     * @param operationalCosts       Costos operativos del servicio.
     * @param profitMarginPercentage Margen de utilidad (%).
     * @param taxPercentage          Impuestos (%).
     * @param notes                  Notas comerciales.
     * @return Presupuesto creado y guardado.
     * @throws EntityNotFoundException si algún {@code recipeId} no existe.
     */
    public ServiceBudget createBudget(String clientName, String eventName, LocalDate eventDate,
                                      Map<String, Double> recipeQuantities, OperationalCosts operationalCosts,
                                      double profitMarginPercentage, double taxPercentage, String notes) {
        List<BudgetItem> items = new ArrayList<>();
        if (recipeQuantities != null) {
            for (Map.Entry<String, Double> entry : recipeQuantities.entrySet()) {
                Recipe recipe = recipeRepository.findById(entry.getKey())
                        .orElseThrow(() -> new EntityNotFoundException("Receta no encontrada con ID: " + entry.getKey()));
                items.add(new BudgetItem(recipe, entry.getValue()));
            }
        }

        ServiceBudget budget = new ServiceBudget(
                clientName, eventName, eventDate, items, operationalCosts,
                profitMarginPercentage, taxPercentage, notes
        );
        return budgetRepository.save(budget);
    }

    /**
     * Agrega una nueva receta con su multiplicador de tandas a un presupuesto existente.
     *
     * @param budgetId ID del presupuesto.
     * @param recipeId ID de la receta a agregar.
     * @param quantity Cantidad de tandas (> 0).
     * @return Presupuesto actualizado.
     * @throws EntityNotFoundException si el presupuesto o la receta no existen.
     */
    public ServiceBudget addRecipeToBudget(String budgetId, String recipeId, double quantity) {
        ServiceBudget budget = getBudgetById(budgetId);
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new EntityNotFoundException("Receta no encontrada con ID: " + recipeId));

        budget.addItem(new BudgetItem(recipe, quantity));
        return budgetRepository.save(budget);
    }

    /**
     * Actualiza el estado comercial de un presupuesto (DRAFT, SENT, APPROVED, REJECTED).
     *
     * @param budgetId  ID del presupuesto.
     * @param newStatus Nuevo estado comercial.
     * @return Presupuesto actualizado.
     * @throws EntityNotFoundException si el presupuesto no existe.
     */
    public ServiceBudget updateStatus(String budgetId, BudgetStatus newStatus) {
        ServiceBudget budget = getBudgetById(budgetId);
        budget.setStatus(newStatus);
        return budgetRepository.save(budget);
    }

    /**
     * Genera la ficha comercial limpia para el cliente y la exporta con la estrategia configurada.
     *
     * @param budgetId ID del presupuesto.
     * @return Texto formateado de la cotización comercial sin costos internos.
     * @throws EntityNotFoundException si el presupuesto no existe.
     */
    public String exportClientQuote(String budgetId) {
        ServiceBudget budget = getBudgetById(budgetId);
        ClientQuoteDTO quote = budget.generateClientQuote();
        return quoteExporter.export(quote);
    }

    public ServiceBudget getBudgetById(String budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado con ID: " + budgetId));
    }

    public List<ServiceBudget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    /**
     * Calcula la suma de precios finales de todos los presupuestos en curso (KPI Dashboard).
     *
     * @return Total cotizado en USD.
     */
    public double getTotalQuotedAmount() {
        return budgetRepository.findAll().stream()
                .mapToDouble(ServiceBudget::getFinalPrice)
                .sum();
    }

    public boolean deleteBudget(String budgetId) {
        return budgetRepository.deleteById(budgetId);
    }
}