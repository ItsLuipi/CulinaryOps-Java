/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.culinaryops.domain.model.enums;

/**
 *
 * @author Luipi
 */
public enum UnitType {
    G("g"),
    KG("kg"),
    ML("ml"),
    L("l"),
    U("u");
    
    private final String symbol;
    UnitType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
