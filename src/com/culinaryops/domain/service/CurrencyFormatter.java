package com.culinaryops.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Utilidad de formateo monetario estandarizado en Dólares Estadounidenses (USD).
 * Implementa sobrecarga de métodos para precisión automática o explícita.
 */
public final class CurrencyFormatter {

    private CurrencyFormatter() {
        // Clase utilitaria no instanciable
    }

    /**
     * Formatea un monto en USD con precisión automática:
     * 4 decimales si el valor absoluto es menor a $0.05 (y distinto de 0), o 2 decimales en caso general.
     * Incluye siempre el código de moneda "USD".
     *
     * @param amount Monto en dólares estadounidenses.
     * @return Cadena formateada (ej. "$12.50 USD" o "$0.0032 USD").
     */
    public static String formatUSD(double amount) {
        double abs = Math.abs(amount);
        int decimals = (abs > 0.0 && abs < 0.05) ? 4 : 2;
        return formatUSD(amount, decimals, true);
    }

    /**
     * Sobrecarga explícita que permite controlar cantidad de decimales y visibilidad del código USD.
     *
     * @param amount   Monto numérico a formatear.
     * @param decimals Cantidad de decimales (>= 0).
     * @param showCode Si es {@code true}, agrega el sufijo " USD".
     * @return Cadena monetaria formateada con redondeo {@link RoundingMode#HALF_UP}.
     * @throws IllegalArgumentException si {@code decimals < 0} o {@code amount} no es finito.
     */
    public static String formatUSD(double amount, int decimals, boolean showCode) {
        if (decimals < 0) {
            throw new IllegalArgumentException("La cantidad de decimales no puede ser negativa.");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("El monto monetario debe ser un número finito.");
        }

        BigDecimal bd = BigDecimal.valueOf(amount).setScale(decimals, RoundingMode.HALF_UP);
        String pattern = "%." + decimals + "f";
        String numericPart = String.format(Locale.US, pattern, bd.doubleValue());
        return showCode ? "$" + numericPart + " USD" : "$" + numericPart;
    }
}