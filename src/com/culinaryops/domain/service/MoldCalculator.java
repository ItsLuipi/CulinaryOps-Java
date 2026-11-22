/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.service;

/**
 *
 * @author roco
 */
import com.culinaryops.domain.model.dto.MoldDistributionResult;
import com.culinaryops.domain.model.dto.MoldDistributionResult.MoldAllocation;
import com.culinaryops.domain.model.dto.MoldScaleResult;
import com.culinaryops.domain.model.mold.BakingMold;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MoldCalculator {

    public double calculateVolume(BakingMold mold) {
        if (mold == null) {
            throw new IllegalArgumentException("El molde no puede ser nulo.");
        }
        return mold.calculateVolumeCm3();
    }

    public MoldScaleResult scaleRecipeBetweenMolds(BakingMold origin, BakingMold target) {
        if (origin == null || target == null) {
            throw new IllegalArgumentException("Tanto el molde origen como el destino son obligatorios.");
        }

        double originVol = origin.calculateVolumeCm3();
        double targetVol = target.calculateVolumeCm3();

        double rawFactor = originVol > 0 ? (targetVol / originVol) : 1.0;
        double scaleFactor = Math.round(rawFactor * 1000.0) / 1000.0;
        double percentageChange = Math.round(((scaleFactor - 1.0) * 100.0) * 10.0) / 10.0;

        String explanation;
        if (Math.abs(percentageChange) < 1.0) {
            explanation = "Ambos moldes tienen prácticamente la misma capacidad; mantén las cantidades originales.";
        } else if (percentageChange > 0) {
            explanation = String.format(Locale.US,
                    "El molde destino es un %.1f%% más grande. Multiplica todos los ingredientes por %.3fx.",
                    percentageChange, scaleFactor);
        } else {
            explanation = String.format(Locale.US,
                    "El molde destino es un %.1f%% más pequeño. Reduce la preparación multiplicando por %.3fx.",
                    Math.abs(percentageChange), scaleFactor);
        }

        return new MoldScaleResult(originVol, targetVol, scaleFactor, percentageChange, explanation);
    }

    public MoldDistributionResult distributeMixture(double totalWeightGrams, List<BakingMold> molds) {
        if (totalWeightGrams <= 0) {
            throw new IllegalArgumentException("El peso total de la masa debe ser mayor a 0 gramos.");
        }
        if (molds == null || molds.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un molde.");
        }

        double totalVolume = 0.0;
        for (BakingMold mold : molds) {
            if (mold == null) {
                throw new IllegalArgumentException("La lista de moldes no puede contener elementos nulos.");
            }
            totalVolume += mold.calculateVolumeCm3();
        }

        List<MoldAllocation> allocations = new ArrayList<>();
        for (BakingMold mold : molds) {
            double moldVol = mold.calculateVolumeCm3();
            double ratio = moldVol / totalVolume;
            double proportionPct = Math.round(ratio * 1000.0) / 10.0;
            long assignedGrams = Math.round(totalWeightGrams * ratio);

            allocations.add(new MoldAllocation(
                    mold.getName(),
                    mold.getShapeLabel(),
                    moldVol,
                    proportionPct,
                    assignedGrams
            ));
        }

        return new MoldDistributionResult(totalWeightGrams, totalVolume, allocations);
    }
}