package com.culinaryops.domain.model;

import com.culinaryops.domain.model.dto.ClientQuoteDTO;
import com.culinaryops.domain.model.enums.BudgetStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agregado raíz que modela un presupuesto comercial de pastelería y calcula toda la cadena
 * financiera desde el costo de materia prima hasta el precio final con margen e impuestos.
 */
public class ServiceBudget extends BaseEntity implements Costable {

    private static final DateTimeFormatter QUOTE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String clientName;
    private String eventName;
    private LocalDate eventDate;
    private BudgetStatus status;
    private final List<BudgetItem> items;
    private OperationalCosts operationalCosts;
    private double profitMarginPercentage;
    private double taxPercentage;
    private String notes;

    /**
     * Constructor sobrecargado para crear un nuevo presupuesto desde la aplicación.
     *
     * @param clientName             Nombre del cliente (obligatorio).
     * @param eventName              Nombre del evento (si es vacío, usa "Servicio de Pastelería").
     * @param eventDate              Fecha del evento (nullable).
     * @param items                  Lista inicial de ítems de receta.
     * @param operationalCosts       Costos operativos asociados (si es nulo, inicia en 0.0).
     * @param profitMarginPercentage Porcentaje de margen de utilidad (>= 0).
     * @param taxPercentage          Porcentaje de impuestos (>= 0).
     * @param notes                  Notas o condiciones comerciales.
     * @throws IllegalArgumentException si {@code clientName} es nulo/vacío o porcentajes negativos.
     */
    public ServiceBudget(String clientName, String eventName, LocalDate eventDate,
                         List<BudgetItem> items, OperationalCosts operationalCosts,
                         double profitMarginPercentage, double taxPercentage, String notes) {
        this(UUID.randomUUID().toString(), clientName, eventName, eventDate, BudgetStatus.DRAFT,
                items, operationalCosts, profitMarginPercentage, taxPercentage, notes,
                LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Constructor completo para reconstrucción desde persistencia o actualización controlada.
     *
     * @param id                     Identificador único del presupuesto.
     * @param clientName             Nombre del cliente (obligatorio).
     * @param eventName              Nombre del evento.
     * @param eventDate              Fecha del evento.
     * @param status                 Estado comercial del presupuesto.
     * @param items                  Ítems de presupuesto.
     * @param operationalCosts       Costos operativos.
     * @param profitMarginPercentage Margen de ganancia (%).
     * @param taxPercentage          Impuestos (%).
     * @param notes                  Notas comerciales.
     * @param createdAt              Marca de tiempo de creación.
     * @param updatedAt              Marca de tiempo de última actualización.
     * @throws IllegalArgumentException si los invariantes de negocio son inválidos.
     */
    public ServiceBudget(String id, String clientName, String eventName, LocalDate eventDate,
                         BudgetStatus status, List<BudgetItem> items, OperationalCosts operationalCosts,
                         double profitMarginPercentage, double taxPercentage, String notes,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        setClientName(clientName);
        setEventName(eventName);
        this.eventDate = eventDate;
        this.status = status != null ? status : BudgetStatus.DRAFT;
        this.items = new ArrayList<>();
        if (items != null) {
            for (BudgetItem item : items) {
                if (item != null) {
                    this.items.add(item);
                }
            }
        }
        this.operationalCosts = operationalCosts != null ? operationalCosts : new OperationalCosts();
        setProfitMarginPercentage(profitMarginPercentage);
        setTaxPercentage(taxPercentage);
        this.notes = notes != null ? notes.trim() : "";
    }

    // =========================================================================
    // CADENA EXACTA DE CÁLCULO FINANCIERO
    // =========================================================================

    /**
     * 1. Suma el costo directo de materia prima de todos los ítems del presupuesto.
     *
     * @return Costo total de materia prima en USD.
     */
    public double getRawMaterialCost() {
        return items.stream()
                .mapToDouble(BudgetItem::getSubtotalCost)
                .sum();
    }

    /**
     * 2. Obtiene el total de costos operativos e indirectos.
     *
     * @return Costos operativos totales en USD.
     */
    public double getTotalOperationalCost() {
        return operationalCosts.getTotalOperationalCost();
    }

    /**
     * 3. Calcula el costo base total de producción (Materia Prima + Costos Operativos).
     *
     * @return Costo base en USD.
     */
    public double getBaseCost() {
        return getRawMaterialCost() + getTotalOperationalCost();
    }

    /**
     * 4. Calcula la ganancia neta esperada según el margen de utilidad configurado.
     *
     * @return Ganancia en USD.
     */
    public double getProfitAmount() {
        return getBaseCost() * (profitMarginPercentage / 100.0);
    }

    /**
     * 5. Calcula el precio sugerido antes de impuestos.
     *
     * @return Precio sugerido sin impuestos en USD.
     */
    public double getSuggestedPrice() {
        return getBaseCost() + getProfitAmount();
    }

    /**
     * 6. Calcula el monto correspondiente a impuestos sobre el precio sugerido.
     *
     * @return Impuestos en USD.
     */
    public double getTaxAmount() {
        return getSuggestedPrice() * (taxPercentage / 100.0);
    }

    /**
     * 7. Calcula el precio final total a cobrar al cliente.
     *
     * @return Precio final en USD.
     */
    public double getFinalPrice() {
        return getSuggestedPrice() + getTaxAmount();
    }

    @Override
    public double calculateCost() {
        return getFinalPrice();
    }

    /**
     * Genera una ficha comercial limpia para entregar al cliente, omitiendo toda información
     * confidencial de costos internos, mermas y márgenes.
     *
     * @return Instancia inmutable de {@link ClientQuoteDTO}.
     */
    public ClientQuoteDTO generateClientQuote() {
        List<ClientQuoteDTO.QuoteItemDTO> quoteItems = items.stream()
                .map(item -> new ClientQuoteDTO.QuoteItemDTO(
                        item.getRecipe().getName(),
                        item.getRecipe().getDescription(),
                        item.getQuantity(),
                        item.getRecipe().getYieldQuantity(),
                        item.getRecipe().getYieldUnit(),
                        item.getTotalUnits()
                ))
                .toList();

        return new ClientQuoteDTO(
                getId(),
                clientName,
                eventName,
                eventDate,
                quoteItems,
                getFinalPrice(),
                notes,
                LocalDateTime.now().format(QUOTE_DATE_FORMATTER)
        );
    }

    // =========================================================================
    // MUTACIONES VALIDADAS Y ENCAPSULAMIENTO DE COLECCIONES
    // =========================================================================

    public void addItem(BudgetItem item) {
        if (item == null) {
            throw new IllegalArgumentException("No se puede agregar un ítem nulo al presupuesto.");
        }
        this.items.add(item);
        markUpdated();
    }

    public boolean removeItemById(String itemId) {
        boolean removed = this.items.removeIf(i -> i.getId().equals(itemId));
        if (removed) {
            markUpdated();
        }
        return removed;
    }

    public void setClientName(String clientName) {
        if (clientName == null || clientName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio y no puede estar vacío.");
        }
        this.clientName = clientName.trim();
        markUpdated();
    }

    public void setEventName(String eventName) {
        this.eventName = (eventName == null || eventName.trim().isEmpty())
                ? "Servicio de Pastelería"
                : eventName.trim();
        markUpdated();
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
        markUpdated();
    }

    public void setStatus(BudgetStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("El estado del presupuesto no puede ser nulo.");
        }
        this.status = status;
        markUpdated();
    }

    public void setOperationalCosts(OperationalCosts operationalCosts) {
        this.operationalCosts = operationalCosts != null ? operationalCosts : new OperationalCosts();
        markUpdated();
    }

    public void setProfitMarginPercentage(double profitMarginPercentage) {
        if (Double.isNaN(profitMarginPercentage) || profitMarginPercentage < 0.0) {
            throw new IllegalArgumentException("El margen de utilidad no puede ser negativo.");
        }
        this.profitMarginPercentage = profitMarginPercentage;
        markUpdated();
    }

    public void setTaxPercentage(double taxPercentage) {
        if (Double.isNaN(taxPercentage) || taxPercentage < 0.0) {
            throw new IllegalArgumentException("El porcentaje de impuestos no puede ser negativo.");
        }
        this.taxPercentage = taxPercentage;
        markUpdated();
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
        markUpdated();
    }

    /**
     * Retorna una vista inmutable de los ítems para proteger la encapsulación interna.
     *
     * @return Lista no modificable de {@link BudgetItem}.
     */
    public List<BudgetItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getClientName() { return clientName; }
    public String getEventName() { return eventName; }
    public LocalDate getEventDate() { return eventDate; }
    public BudgetStatus getStatus() { return status; }
    public OperationalCosts getOperationalCosts() { return operationalCosts; }
    public double getProfitMarginPercentage() { return profitMarginPercentage; }
    public double getTaxPercentage() { return taxPercentage; }
    public String getNotes() { return notes; }
}