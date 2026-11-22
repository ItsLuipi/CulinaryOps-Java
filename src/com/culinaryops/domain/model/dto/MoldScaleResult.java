/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model.dto;

/**
 *
 * @author roco
 */
public class MoldScaleResult {

    private final double originVolumeCm3;
    private final double targetVolumeCm3;
    private final double scaleFactor;
    private final double percentageChange;
    private final String explanation;

    public MoldScaleResult(double originVolumeCm3,
                           double targetVolumeCm3,
                           double scaleFactor,
                           double percentageChange,
                           String explanation) {
        this.originVolumeCm3 = originVolumeCm3;
        this.targetVolumeCm3 = targetVolumeCm3;
        this.scaleFactor = scaleFactor;
        this.percentageChange = percentageChange;
        this.explanation = explanation;
    }

    public double getOriginVolumeCm3() {
        return originVolumeCm3;
    }

    public double getTargetVolumeCm3() {
        return targetVolumeCm3;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public double getPercentageChange() {
        return percentageChange;
    }

    public String getExplanation() {
        return explanation;
    }
}