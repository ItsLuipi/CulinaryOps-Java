package com.culinaryops.presentation.ui;

import com.culinaryops.application.BudgetService;
import com.culinaryops.application.CatalogService;
import com.culinaryops.application.InventoryService;
import com.culinaryops.application.RecipeService;
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.ServiceBudget;
import com.culinaryops.domain.service.CurrencyFormatter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Módulo 1: Obrador & Resumen (Dashboard principal con KPIs, comparativa de merma y resúmenes).
 */
public class DashboardPanel extends JPanel {

    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final RecipeService recipeService;
    private final BudgetService budgetService;

    private final JLabel kpiStockValue = CulinaryTheme.createKpiLabel("$0.00 USD");
    private final JLabel kpiStockSub = new JLabel("Útil con mermas: $0.00 USD");
    private final JLabel kpiWasteValue = CulinaryTheme.createKpiLabel("0.0%");
    private final JLabel kpiWasteSub = new JLabel("-");
    private final JLabel kpiExpiringValue = CulinaryTheme.createKpiLabel("0");
    private final JLabel kpiExpiringSub = new JLabel("Sin alertas próximas");
    private final JLabel kpiBudgetsValue = CulinaryTheme.createKpiLabel("$0.00 USD");
    private final JLabel kpiBudgetsSub = new JLabel("0 presupuestos");

    private final DefaultTableModel recipesModel;
    private final DefaultTableModel budgetsModel;

    public DashboardPanel(
            CatalogService catalogService,
            InventoryService inventoryService,
            RecipeService recipeService,
            BudgetService budgetService,
            Runnable onNewIngredient,
            Runnable onNewRecipe) {
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.recipeService = recipeService;
        this.budgetService = budgetService;

        setLayout(new BorderLayout(0, 16));
        setBackground(CulinaryTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Cabecera con accesos rápidos
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Obrador & Resumen");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Control operativo de mermas, existencias, escandallos y cotizaciones en USD");
        subtitle.setFont(CulinaryTheme.FONT_BODY);
        subtitle.setForeground(CulinaryTheme.TEXT_SECONDARY);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        quickActions.setOpaque(false);
        JButton addRecipeBtn = CulinaryTheme.createSecondaryButton("+ Receta");
        addRecipeBtn.addActionListener(e -> onNewRecipe.run());
        JButton addIngBtn = CulinaryTheme.createPrimaryButton("+ Ingrediente");
        addIngBtn.addActionListener(e -> onNewIngredient.run());
        quickActions.add(addRecipeBtn);
        quickActions.add(addIngBtn);

        header.add(titleBox, BorderLayout.WEST);
        header.add(quickActions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Contenido central
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.add(CulinaryTheme.buildKpiCard("Valor Total Inventario", kpiStockValue, kpiStockSub, false));
        kpiRow.add(CulinaryTheme.buildKpiCard("Mayor Factor de Merma", kpiWasteValue, kpiWasteSub, true));
        kpiRow.add(CulinaryTheme.buildKpiCard("Alertas de Vencimiento (<=7d)", kpiExpiringValue, kpiExpiringSub, true));
        kpiRow.add(CulinaryTheme.buildKpiCard("Cotizaciones en Curso", kpiBudgetsValue, kpiBudgetsSub, false));
        content.add(kpiRow);
        content.add(Box.createVerticalStrut(14));

        // Listados rápidos: Top 3 recetas y Top 3 presupuestos
        JPanel listsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        listsRow.setOpaque(false);

        recipesModel = new DefaultTableModel(new String[]{"Receta", "Rendimiento", "Costo/Unidad"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable recipesTable = new JTable(recipesModel);
        CulinaryTheme.styleTable(recipesTable);
        recipesTable.getColumnModel().getColumn(2).setCellRenderer(CulinaryTheme.rightMonoRenderer());

        JPanel recipesCard = CulinaryTheme.createCardPanel();
        recipesCard.setLayout(new BorderLayout(0, 8));
        JLabel recHdr = new JLabel("Recetas Recientes (Top 3)");
        recHdr.setFont(CulinaryTheme.FONT_SUBTITLE);
        recipesCard.add(recHdr, BorderLayout.NORTH);
        recipesCard.add(CulinaryTheme.wrapTable(recipesTable), BorderLayout.CENTER);

        budgetsModel = new DefaultTableModel(new String[]{"Cliente / Evento", "Margen", "Precio Final"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable budgetsTable = new JTable(budgetsModel);
        CulinaryTheme.styleTable(budgetsTable);
        budgetsTable.getColumnModel().getColumn(1).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        budgetsTable.getColumnModel().getColumn(2).setCellRenderer(CulinaryTheme.rightMonoRenderer());

        JPanel budgetsCard = CulinaryTheme.createCardPanel();
        budgetsCard.setLayout(new BorderLayout(0, 8));
        JLabel budHdr = new JLabel("Presupuestos Recientes (Top 3)");
        budHdr.setFont(CulinaryTheme.FONT_SUBTITLE);
        budgetsCard.add(budHdr, BorderLayout.NORTH);
        budgetsCard.add(CulinaryTheme.wrapTable(budgetsTable), BorderLayout.CENTER);

        listsRow.add(recipesCard);
        listsRow.add(budgetsCard);
        content.add(listsRow);

        add(content, BorderLayout.CENTER);
        refreshData();
    }

    public final void refreshData() {
        double rawStock = inventoryService.getTotalInventoryValue();
        double effStock = inventoryService.getTotalEffectiveInventoryValue();
        kpiStockValue.setText(CurrencyFormatter.formatUSD(rawStock));
        kpiStockSub.setText("Útil con mermas: " + CurrencyFormatter.formatUSD(effStock));

        List<Ingredient> allIngredients = catalogService.findAll();
        Ingredient highestWaste = allIngredients.stream()
                .max(Comparator.comparingDouble(Ingredient::getWastePercentage))
                .orElse(null);
        if (highestWaste != null) {
            kpiWasteValue.setText(String.format(Locale.US, "%.1f%%", highestWaste.getWastePercentage()));
            kpiWasteSub.setText("<html>" + highestWaste.getName() + " ("
                    + CurrencyFormatter.formatUSD(highestWaste.getEffectiveUnitPrice()) + ")</html>");
        } else {
            kpiWasteValue.setText("0.0%");
            kpiWasteSub.setText("Sin insumos registrados");
        }

        List<Ingredient> expiring = inventoryService.findExpiringSoon(7).stream()
                .sorted(Comparator.comparing(Ingredient::getDaysUntilExpiration, Comparator.nullsLast(Long::compareTo)))
                .toList();
        kpiExpiringValue.setText("⚠ " + expiring.size());
        if (expiring.isEmpty()) {
            kpiExpiringSub.setText("✓ Sin vencimientos próximos");
        } else {
            Ingredient first = expiring.get(0);
            kpiExpiringSub.setText(first.getName() + " (" + first.getDaysUntilExpiration() + "d)");
        }

        List<ServiceBudget> budgets = budgetService.getAllBudgets();
        kpiBudgetsValue.setText(CurrencyFormatter.formatUSD(budgetService.getTotalQuotedAmount()));
        kpiBudgetsSub.setText(budgets.size() + " presupuesto(s) activos");

        recipesModel.setRowCount(0);
        recipeService.findAll().stream().limit(3).forEach(r -> recipesModel.addRow(new Object[]{
                r.getName(),
                String.format(Locale.US, "%.0f %s", r.getYieldQuantity(), r.getYieldUnit()),
                CurrencyFormatter.formatUSD(r.calculateCostPerUnit())
        }));

        budgetsModel.setRowCount(0);
        budgets.stream().limit(3).forEach(b -> budgetsModel.addRow(new Object[]{
                b.getClientName() + " — " + b.getEventName(),
                String.format(Locale.US, "%.1f%%", b.getProfitMarginPercentage()),
                CurrencyFormatter.formatUSD(b.getFinalPrice())
        }));
    }
}
