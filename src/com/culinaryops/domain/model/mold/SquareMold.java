/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model.mold;

/**
 *
 * @author roco
 */
public class SquareMold extends BakingMold {
    private final double sideCm;

    public SquareMold(String name, double sideCm, double heightCm) {
        super(name, heightCm);
        validateDimension(sideCm, "lado");
        this.sideCm = sideCm;
    }

    public double getSideCm() {
        return sideCm;
    }

    @Override
    public double calculateVolumeCm3() {
        return sideCm * sideCm * heightCm;
    }

    @Override
    public String getShapeLabel() {
        return "Cuadrado";
    }
}