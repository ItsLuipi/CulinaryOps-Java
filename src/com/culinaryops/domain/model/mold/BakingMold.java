/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model.mold;

/**
 *
 * @author roco
 */
public abstract class BakingMold {

    protected static final double MIN_DIMENSION_CM = 0.1;

    protected final String name;
    protected final double heightCm;

    protected BakingMold(String name, double heightCm) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del molde no puede estar vacío.");
        }
        validateDimension(heightCm, "altura");
        this.name = name.trim();
        this.heightCm = heightCm;
    }

    protected static void validateDimension(double value, String dimensionName) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < MIN_DIMENSION_CM) {
            throw new IllegalArgumentException(
                    "La " + dimensionName + " del molde debe ser al menos " + MIN_DIMENSION_CM + " cm."
            );
        }
    }

    public abstract double calculateVolumeCm3();

    public abstract String getShapeLabel();

    public String getName() {
        return name;
    }

    public double getHeightCm() {
        return heightCm;
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %.1f cm alto)", name, getShapeLabel(), heightCm);
    }
}