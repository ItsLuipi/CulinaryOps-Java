/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model.mold;

/**
 *
 * @author roco
 */
public class RectangularMold extends BakingMold {

    private final double lengthCm;
    private final double widthCm;

    public RectangularMold(String name, double lengthCm, double widthCm, double heightCm) {
        super(name, heightCm);
        validateDimension(lengthCm, "largo");
        validateDimension(widthCm, "ancho");
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
    }

    public double getLengthCm() {
        return lengthCm;
    }

    public double getWidthCm() {
        return widthCm;
    }

    @Override
    public double calculateVolumeCm3() {
        return lengthCm * widthCm * heightCm;
    }

    @Override
    public String getShapeLabel() {
        return "Rectangular";
    }
}