/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.domain.service;

/**
 *
 * @author roco
 */
import java.util.Locale;

public final class CurrencyFormatter {

    private CurrencyFormatter() {
    }

    public static String formatUSD(double amount) {
        int decimals = Math.abs(amount) < 0.05 ? 4 : 2;
        return formatUSD(amount, decimals, true);
    }

    public static String formatUSD(double amount, int decimals, boolean showCode) {
        String pattern = "$%." + Math.max(0, decimals) + "f" + (showCode ? " USD" : "");
        return String.format(Locale.US, pattern, amount);
    }
}
