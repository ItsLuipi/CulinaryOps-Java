package com.culinaryops.domain.model;

import java.util.Objects;

/**
 * Objeto de valor que encapsula los costos operativos e indirectos asociados a un presupuesto.
 * Sanea automáticamente cualquier valor negativo a 0.0 para garantizar invariantes financieros.
 */
public final class OperationalCosts implements Costable {

    private final double laborHours;
    private final double hourlyRate;
    private final double utilitiesCost;
    private final double equipmentCost;
    private final double packagingDeliveryCost;

    /**
     * Constructor vacío que inicializa todos los costos operativos en 0.0.
     */
    public OperationalCosts() {
        this(0.0, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Constructor completo con saneamiento defensivo de valores negativos.
     *
     * @param laborHours            Horas de trabajo de pastelería.
     * @param hourlyRate            Tarifa en USD por hora trabajada.
     * @param utilitiesCost         Costo de servicios (luz, gas, agua) en USD.
     * @param equipmentCost         Depreciación y uso de equipos en USD.
     * @param packagingDeliveryCost Costo de cajas, blondas y entrega refrigerada en USD.
     */
    public OperationalCosts(double laborHours, double hourlyRate, double utilitiesCost,
                            double equipmentCost, double packagingDeliveryCost) {
        this.laborHours = sanitize(laborHours);
        this.hourlyRate = sanitize(hourlyRate);
        this.utilitiesCost = sanitize(utilitiesCost);
        this.equipmentCost = sanitize(equipmentCost);
        this.packagingDeliveryCost = sanitize(packagingDeliveryCost);
    }

    private static double sanitize(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, value);
    }

    /**
     * Calcula el costo total de mano de obra.
     *
     * @return {@code laborHours * hourlyRate} en USD.
     */
    public double getLaborCost() {
        return laborHours * hourlyRate;
    }

    /**
     * Calcula la suma total de todos los costos operativos e indirectos.
     *
     * @return Total de costos operativos en USD.
     */
    public double getTotalOperationalCost() {
        return getLaborCost() + utilitiesCost + equipmentCost + packagingDeliveryCost;
    }

    @Override
    public double calculateCost() {
        return getTotalOperationalCost();
    }

    public double getLaborHours() { return laborHours; }
    public double getHourlyRate() { return hourlyRate; }
    public double getUtilitiesCost() { return utilitiesCost; }
    public double getEquipmentCost() { return equipmentCost; }
    public double getPackagingDeliveryCost() { return packagingDeliveryCost; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperationalCosts that)) return false;
        return Double.compare(that.laborHours, laborHours) == 0
                && Double.compare(that.hourlyRate, hourlyRate) == 0
                && Double.compare(that.utilitiesCost, utilitiesCost) == 0
                && Double.compare(that.equipmentCost, equipmentCost) == 0
                && Double.compare(that.packagingDeliveryCost, packagingDeliveryCost) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(laborHours, hourlyRate, utilitiesCost, equipmentCost, packagingDeliveryCost);
    }

    @Override
    public String toString() {
        return String.format("OperationalCosts{labor=%.2f, utilities=%.2f, equipment=%.2f, packaging=%.2f, total=%.2f}",
                getLaborCost(), utilitiesCost, equipmentCost, packagingDeliveryCost, getTotalOperationalCost());
    }
}