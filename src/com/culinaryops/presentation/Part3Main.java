/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.culinaryops.presentation;

/**
 *
 * @author roco
 */
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.dto.ChatbotAnswer;
import com.culinaryops.domain.model.dto.MoldDistributionResult;
import com.culinaryops.domain.model.dto.MoldScaleResult;
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.model.mold.BakingMold;
import com.culinaryops.domain.model.mold.RectangularMold;
import com.culinaryops.domain.model.mold.RoundMold;
import com.culinaryops.domain.model.mold.SquareMold;
import com.culinaryops.domain.service.InventoryQueryAssistant;
import com.culinaryops.domain.service.MoldCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Part3Main {

    private final MoldCalculator moldCalculator;
    private final InventoryQueryAssistant assistant;
    private final List<Ingredient> seedIngredients;

    public Part3Main(MoldCalculator moldCalculator,
                     InventoryQueryAssistant assistant,
                     List<Ingredient> seedIngredients) {
        this.moldCalculator = moldCalculator;
        this.assistant = assistant;
        this.seedIngredients = List.copyOf(seedIngredients);
    }

    public static void main(String[] args) {
        Part3Main app = new Part3Main(
                new MoldCalculator(),
                new InventoryQueryAssistant(),
                createSeedIngredients()
        );
        app.runInteractiveMenu();
    }

    public void runInteractiveMenu() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                System.out.println("\n=== CULINARYOPS - PARTE 3: HERRAMIENTAS DEL OBRADOR ===");
                System.out.println("1. Escalar receta entre dos moldes");
                System.out.println("2. Distribuir masa en varios moldes por peso (g)");
                System.out.println("3. Consultar al Asistente Pastelero");
                System.out.println("0. Salir");
                System.out.print("Elige una opción: ");

                String option = scanner.nextLine().trim();
                switch (option) {
                    case "1" -> demoScaleMolds();
                    case "2" -> demoDistributeMixture();
                    case "3" -> {
                        System.out.print("Escribe tu consulta (ej. 'mayor merma', 'por vencer', 'cuanto vale', 'fresas'): ");
                        String query = scanner.nextLine();
                        ChatbotAnswer answer = assistant.ask(query, seedIngredients);
                        System.out.println("\n" + answer.getMessage());
                        if (!answer.getSuggestions().isEmpty()) {
                            System.out.println("Sugerencias: " + answer.getSuggestions());
                        }
                    }
                    case "0" -> running = false;
                    default -> System.out.println("Opción no válida.");
                }
            }
        }
    }

    private void demoScaleMolds() {
        BakingMold origin = new RoundMold("Molde Redondo Base 20cm", 20.0, 5.0);
        BakingMold target = new RoundMold("Molde Redondo Evento 26cm", 26.0, 6.0);
        MoldScaleResult result = moldCalculator.scaleRecipeBetweenMolds(origin, target);

        System.out.printf(Locale.US, "\nOrigen: %s -> %.2f cm³%n", origin, result.getOriginVolumeCm3());
        System.out.printf(Locale.US, "Destino: %s -> %.2f cm³%n", target, result.getTargetVolumeCm3());
        System.out.println(result.getExplanation());
    }

    private void demoDistributeMixture() {
        List<BakingMold> molds = List.of(
                new RoundMold("Piso Superior 15cm", 15.0, 5.0),
                new SquareMold("Base Cuadrada 20cm", 20.0, 5.0),
                new RectangularMold("Budinera 25x12cm", 25.0, 12.0, 6.0)
        );
        MoldDistributionResult dist = moldCalculator.distributeMixture(1500.0, molds);
        System.out.printf(Locale.US, "\nReparto de %.0f g en volumen total %.2f cm³:%n",
                dist.getTotalWeightGrams(), dist.getTotalVolumeCm3());
        for (MoldDistributionResult.MoldAllocation alloc : dist.getAllocations()) {
            System.out.printf(Locale.US, "- %s (%s): %.1f%% -> %d g%n",
                    alloc.getMoldName(), alloc.getShapeLabel(),
                    alloc.getProportionPercentage(), alloc.getAssignedGrams());
        }
    }

    public static List<Ingredient> createSeedIngredients() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new Ingredient("ing-1", "Huevos de campo", "Lácteos y Huevos", UnitType.Gr, 0.0032, 12500, 15.0, true, today.plusDays(7), now, now),
                new Ingredient("ing-2", "Azúcar común refinada", "Secos y Endulzantes", UnitType.Gr, 0.0014, 35000, 1.0, false, null, now, now),
                new Ingredient("ing-3", "Harina 0000 de repostería", "Harinas y Almidones", UnitType.Gr, 0.0011, 40000, 2.0, false, null, now, now),
                new Ingredient("ing-4", "Mantequilla sin sal 82% MG", "Lácteos y Grasas", UnitType.Gr, 0.0085, 6000, 3.0, true, today.plusDays(20), now, now),
                new Ingredient("ing-5", "Chocolate Cobertura 70% Cacao", "Chocolates y Coberturas", UnitType.Gr, 0.014, 8500, 5.0, false, null, now, now),
                new Ingredient("ing-6", "Fresas / Frutillas seleccionadas", "Frutas y Frescos", UnitType.Gr, 0.0048, 3000, 18.0, true, today.plusDays(3), now, now),
                new Ingredient("ing-7", "Crema de leche / Nata 35% MG", "Lácteos y Grasas", UnitType.Lt, 4.50, 5.0, 4.0, true, today.plusDays(5), now, now),
                new Ingredient("ing-8", "Extracto de vainilla natural", "Aromas y Esencias", UnitType.Gr, 0.12, 250, 2.0, false, null, now, now)
        );
    }
}