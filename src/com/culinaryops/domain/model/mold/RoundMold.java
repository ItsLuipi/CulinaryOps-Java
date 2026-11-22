/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model.mold;

/**
 *
 * @author roco
 */
public class RoundMold extends BakingMold {

    private final double diameterCm;

    public RoundMold(String name, double diameterCm, double heightCm) {
        super(name, heightCm);
        validateDimension(diameterCm, "diámetro");
        this.diameterCm = diameterCm;
    }

    public double getDiameterCm() {
        return diameterCm;
    }

    @Override
    public double calculateVolumeCm3() {
        double radius = diameterCm / 2.0;
        return Math.PI * radius * radius * heightCm;
    }

    @Override
    public String getShapeLabel() {
        return "Redondo";
    }
}