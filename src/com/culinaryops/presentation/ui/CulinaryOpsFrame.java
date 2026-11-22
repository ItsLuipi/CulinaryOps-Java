package com.culinaryops.presentation.ui;

import com.culinaryops.application.BudgetService;
import com.culinaryops.application.CatalogService;
import com.culinaryops.application.InventoryService;
import com.culinaryops.application.RecipeService;
import com.culinaryops.domain.service.InventoryQueryAssistant;
import com.culinaryops.domain.service.MoldCalculator;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Ventana principal de la aplicación de escritorio CulinaryOps.
 * Integra la navegación lateral fija y los 6 módulos funcionales bajo diseño UX/UI unificado.
 */
public class CulinaryOpsFrame extends JFrame {

    private static final String CARD_DASHBOARD = "dashboard";
    private static final String CARD_INGREDIENTS = "ingredients";
    private static final String CARD_INVENTORY = "inventory";
    private static final String CARD_RECIPES = "recipes";
    private static final String CARD_BUDGETS = "budgets";
    private static final String CARD_ASSISTANT = "assistant";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentArea = new JPanel(cardLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private final DashboardPanel dashboardPanel;
    private final IngredientsPanel ingredientsPanel;
    private final InventoryPanel inventoryPanel;
    private final RecipesPanel recipesPanel;
    private final BudgetsPanel budgetsPanel;
    private final AssistantPanel assistantPanel;

    public CulinaryOpsFrame(
            CatalogService catalogService,
            InventoryService inventoryService,
            RecipeService recipeService,
            BudgetService budgetService,
            InventoryQueryAssistant assistant,
            MoldCalculator moldCalculator) {
        super("CulinaryOps — Obrador, Escandallos & Costos (USD)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setSize(1240, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        Runnable refreshAll = this::refreshAllPanels;

        this.ingredientsPanel = new IngredientsPanel(
                catalogService,
                refreshAll,
                () -> showModule(CARD_INVENTORY));
        this.inventoryPanel = new InventoryPanel(
                inventoryService,
                catalogService,
                refreshAll);
        this.recipesPanel = new RecipesPanel(
                recipeService,
                catalogService,
                refreshAll);
        this.budgetsPanel = new BudgetsPanel(
                budgetService,
                recipeService,
                refreshAll);
        this.assistantPanel = new AssistantPanel(
                inventoryService,
                recipeService,
                assistant,
                moldCalculator);
        this.dashboardPanel = new DashboardPanel(
                catalogService,
                inventoryService,
                recipeService,
                budgetService,
                () -> {
                    showModule(CARD_INGREDIENTS);
                    ingredientsPanel.openNewDialog();
                },
                () -> {
                    showModule(CARD_RECIPES);
                    recipesPanel.openNewDialog();
                });

        contentArea.setBackground(CulinaryTheme.BG_MAIN);
        contentArea.add(dashboardPanel, CARD_DASHBOARD);
        contentArea.add(ingredientsPanel, CARD_INGREDIENTS);
        contentArea.add(inventoryPanel, CARD_INVENTORY);
        contentArea.add(recipesPanel, CARD_RECIPES);
        contentArea.add(budgetsPanel, CARD_BUDGETS);
        contentArea.add(assistantPanel, CARD_ASSISTANT);

        add(buildSidebar(), BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);
        showModule(CARD_DASHBOARD);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(CulinaryTheme.SIDEBAR_CHARCOAL);
        sidebar.setPreferredSize(new Dimension(236, 720));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 14, 20, 14));

        JLabel brand = new JLabel("CulinaryOps");
        brand.setFont(new Font("SansSerif", Font.BOLD, 21));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Pastelería · Escandallos · USD");
        tagline.setFont(CulinaryTheme.FONT_SMALL);
        tagline.setForeground(new Color(0xB8, 0xB2, 0xA8));
        tagline.setAlignmentX(LEFT_ALIGNMENT);

        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(tagline);
        sidebar.add(Box.createVerticalStrut(24));

        addNavButton(sidebar, CARD_DASHBOARD, "1. Obrador & Resumen");
        addNavButton(sidebar, CARD_INGREDIENTS, "2. Ingredientes & Mermas");
        addNavButton(sidebar, CARD_INVENTORY, "3. Inventario & Stock");
        addNavButton(sidebar, CARD_RECIPES, "4. Recetas & Escandallos");
        addNavButton(sidebar, CARD_BUDGETS, "5. Presupuestos & Cotizador");

        sidebar.add(Box.createVerticalGlue());

        JButton assistantBtn = createSidebarButton("6. Asistente & Moldes");
        assistantBtn.setBackground(CulinaryTheme.ACCENT_APRICOT);
        assistantBtn.setForeground(Color.WHITE);
        assistantBtn.addActionListener(e -> showModule(CARD_ASSISTANT));
        navButtons.put(CARD_ASSISTANT, assistantBtn);
        sidebar.add(assistantBtn);

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String key, String label) {
        JButton btn = createSidebarButton(label);
        btn.addActionListener(e -> showModule(key));
        navButtons.put(key, btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private static JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(CulinaryTheme.FONT_BODY_BOLD);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(208, 40));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showModule(String key) {
        refreshAllPanels();
        cardLayout.show(contentArea, key);
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (CARD_ASSISTANT.equals(entry.getKey())) {
                btn.setBackground(entry.getKey().equals(key)
                        ? CulinaryTheme.ACCENT_APRICOT_HOVER
                        : CulinaryTheme.ACCENT_APRICOT);
                btn.setForeground(Color.WHITE);
            } else if (entry.getKey().equals(key)) {
                btn.setBackground(CulinaryTheme.SIDEBAR_HOVER);
                btn.setForeground(CulinaryTheme.ACCENT_APRICOT);
            } else {
                btn.setBackground(CulinaryTheme.SIDEBAR_CHARCOAL);
                btn.setForeground(new Color(0xEC, 0xE8, 0xE1));
            }
        }
    }

    private void refreshAllPanels() {
        dashboardPanel.refreshData();
        ingredientsPanel.refreshData();
        inventoryPanel.refreshData();
        recipesPanel.refreshData();
        budgetsPanel.refreshData();
        assistantPanel.refreshData();
    }
}
