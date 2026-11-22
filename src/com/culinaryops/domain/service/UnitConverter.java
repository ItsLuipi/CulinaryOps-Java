/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.service;

/**
 *
 * @author Luipi
 */
import com.culinaryops.domain.model.enums.UnitType;
public class UnitConverter {
    private UnitConverter(){}
    
    public static double convert(double amount, UnitType from, UnitType to){
        if(from==null || to==null){
            throw new IllegalArgumentException("Las unidades son obligatorias");
        }
        
        if(from==to || from==UnitType.Und || to==UnitType.Und){
            return amount;
        }
        
        return amount*toBaseFactor (from)/toBaseFactor (to);
    }
    
    private static double toBaseFactor (UnitType unit){
        return switch(unit){
            case UnitType.Gr -> 1.0;
            case UnitType.ML -> 1.0;
            case UnitType.KG -> 1000.0;
            case UnitType.Lt -> 1000.0;
            default -> throw new IllegalArgumentException("Unidad desconocida: "+unit);
        };
    }
}
