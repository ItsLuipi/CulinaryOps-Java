/*2
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.model.dto;

/**
 *
 * @author roco
 */
import java.util.List;

public class MoldDistributionResult {

    public static class MoldAllocation {
        private final String moldName;
        private final String shapeLabel;
        private final double volumeCm3;
        private final double proportionPercentage;
        private final long assignedGrams;

        public MoldAllocation(String moldName,
                              String shapeLabel,
                              double volumeCm3,
                              double proportionPercentage,
                              long assignedGrams) {
            this.moldName = moldName;
            this.shapeLabel = shapeLabel;
            this.volumeCm3 = volumeCm3;
            this.proportionPercentage = proportionPercentage;
            this.assignedGrams = assignedGrams;
        }

        public String getMoldName() {
            return moldName;
        }

        public String getShapeLabel() {
            return shapeLabel;
        }

        public double getVolumeCm3() {
            return volumeCm3;
        }

        public double getProportionPercentage() {
            return proportionPercentage;
        }

        public long getAssignedGrams() {
            return assignedGrams;
        }
    }

    private final double totalWeightGrams;
    private final double totalVolumeCm3;
    private final List<MoldAllocation> allocations;

    public MoldDistributionResult(double totalWeightGrams,
                                  double totalVolumeCm3,
                                  List<MoldAllocation> allocations) {
        this.totalWeightGrams = totalWeightGrams;
        this.totalVolumeCm3 = totalVolumeCm3;
        this.allocations = List.copyOf(allocations);
    }

    public double getTotalWeightGrams() {
        return totalWeightGrams;
    }

    public double getTotalVolumeCm3() {
        return totalVolumeCm3;
    }

    public List<MoldAllocation> getAllocations() {
        return allocations;
    }
}