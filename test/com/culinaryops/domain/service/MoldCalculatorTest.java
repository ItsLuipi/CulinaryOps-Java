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
import com.culinaryops.domain.model.dto.MoldScaleResult;
import com.culinaryops.domain.model.mold.BakingMold;
import com.culinaryops.domain.model.mold.RectangularMold;
import com.culinaryops.domain.model.mold.RoundMold;
import com.culinaryops.domain.model.mold.SquareMold;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoldCalculatorTest {

    private MoldCalculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new MoldCalculator();
    }

    @Test
    @DisplayName("Debe calcular el factor de escala exacto entre molde redondo 20x5 cm y 26x6 cm")
    public void shouldScaleBetweenRoundMolds20x5And26x6() {
        BakingMold origin = new RoundMold("Molde Base 20cm", 20.0, 5.0);
        BakingMold target = new RoundMold("Molde Evento 26cm", 26.0, 6.0);

        MoldScaleResult result = calculator.scaleRecipeBetweenMolds(origin, target);

        assertEquals(2.028, result.getScaleFactor(), 0.001);
        assertEquals(102.8, result.getPercentageChange(), 0.1);
        assertTrue(result.getExplanation().contains("2.028x"));
    }

    @Test
    @DisplayName("Debe calcular volúmenes polimórficos para moldes redondo, rectangular y cuadrado")
    public void shouldCalculateVolumesPolymorphically() {
        BakingMold round = new RoundMold("Redondo", 20.0, 5.0);
        BakingMold rectangular = new RectangularMold("Budinera", 30.0, 10.0, 8.0);
        BakingMold square = new SquareMold("Brownie", 20.0, 4.0);

        assertEquals(Math.PI * 100.0 * 5.0, calculator.calculateVolume(round), 0.01);
        assertEquals(2400.0, calculator.calculateVolume(rectangular), 0.01);
        assertEquals(1600.0, calculator.calculateVolume(square), 0.01);
    }

    @Test
    @DisplayName("Debe distribuir masa en gramos proporcionalmente al volumen de cada molde")
    public void shouldDistributeMixtureProportionally() {
        List<BakingMold> molds = List.of(
                new RoundMold("Piso 1 - 15cm", 15.0, 5.0),
                new RoundMold("Piso 2 - 20cm", 20.0, 5.0),
                new RoundMold("Piso 3 - 24cm", 24.0, 5.0)
        );

        MoldDistributionResult distribution = calculator.distributeMixture(1200.0, molds);

        long totalAssigned = distribution.getAllocations().stream()
                .mapToLong(MoldDistributionResult.MoldAllocation::getAssignedGrams)
                .sum();

        assertEquals(3, distribution.getAllocations().size());
        assertEquals(1200L, totalAssigned, 1L);
    }

    @Test
    @DisplayName("Debe lanzar IllegalArgumentException ante dimensiones menores a 0.1 cm o lista vacía")
    public void shouldThrowExceptionForInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> new RoundMold("Inválido", 0.05, 5.0));
        assertThrows(IllegalArgumentException.class, () -> new SquareMold("Inválido", 15.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> calculator.distributeMixture(500.0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> calculator.distributeMixture(-100.0, List.of(new SquareMold("S", 10, 5))));
    }
}