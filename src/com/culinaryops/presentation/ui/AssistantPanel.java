package com.culinaryops.presentation.ui;

import com.culinaryops.application.InventoryService;
import com.culinaryops.application.RecipeService;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.dto.ChatbotAnswer;
import com.culinaryops.domain.model.dto.MoldDistributionResult;
import com.culinaryops.domain.model.dto.MoldScaleResult;
import com.culinaryops.domain.model.enums.MoldShape;
import com.culinaryops.domain.model.mold.BakingMold;
import com.culinaryops.domain.model.mold.RectangularMold;
import com.culinaryops.domain.model.mold.RoundMold;
import com.culinaryops.domain.model.mold.SquareMold;
import com.culinaryops.domain.service.InputRegexValidator;
import com.culinaryops.domain.service.InventoryQueryAssistant;
import com.culinaryops.domain.service.MoldCalculator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Módulo 6: Asistente Pastelero (Dudas de Inventario) & Calculadora de Moldes.
 */
public class AssistantPanel extends JPanel {

    private final InventoryService inventoryService;
    private final RecipeService recipeService;
    private final InventoryQueryAssistant assistant;
    private final MoldCalculator moldCalculator;

    private final JTextArea chatHistory = new JTextArea();
    private final JTextField chatInput = new JTextField(28);
    private final JComboBox<RecipeWeightChoice> recipeWeightCombo = new JComboBox<>();

    public AssistantPanel(
            InventoryService inventoryService,
            RecipeService recipeService,
            InventoryQueryAssistant assistant,
            MoldCalculator moldCalculator) {
        this.inventoryService = inventoryService;
        this.recipeService = recipeService;
        this.assistant = assistant;
        this.moldCalculator = moldCalculator;

        setLayout(new BorderLayout(0, 12));
        setBackground(CulinaryTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Asistente Pastelero");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(CulinaryTheme.FONT_BODY_BOLD);
        tabs.addTab("1. Dudas de Inventario", buildChatbotTab());
        tabs.addTab("2. Calculadora de Moldes", buildMoldTab());
        add(tabs, BorderLayout.CENTER);

        refreshData();
    }

    public final void refreshData() {
        recipeWeightCombo.removeAllItems();
        recipeWeightCombo.addItem(new RecipeWeightChoice(null));
        for (Recipe r : recipeService.findAll()) {
            recipeWeightCombo.addItem(new RecipeWeightChoice(r));
        }
    }

    private JPanel buildChatbotTab() {
        JPanel panel = CulinaryTheme.createCardPanel();
        panel.setLayout(new BorderLayout(0, 10));

        // Botones de consulta rápida
        JPanel quickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        quickRow.setOpaque(false);
        String[] quickQueries = {
                "¿Qué insumos tienen mayor merma?",
                "¿Qué ingredientes están por vencer?",
                "¿Cuánto vale mi inventario total?",
                "Stock bajo",
                "¿Cuáles son los más caros?"
        };
        for (String q : quickQueries) {
            JButton btn = CulinaryTheme.createSecondaryButton(q);
            btn.addActionListener(e -> submitChatQuery(q));
            quickRow.add(btn);
        }

        chatHistory.setEditable(false);
        chatHistory.setLineWrap(true);
        chatHistory.setWrapStyleWord(true);
        chatHistory.setFont(CulinaryTheme.FONT_BODY);
        chatHistory.setBackground(CulinaryTheme.BG_MAIN);
        chatHistory.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        chatHistory.setText("Asistente Pastelero listo. Usa un botón rápido o escribe el nombre de un ingrediente.\n\n");

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        chatInput.setBorder(CulinaryTheme.fieldBorder());
        JButton sendBtn = CulinaryTheme.createPrimaryButton("Consultar");
        sendBtn.addActionListener(e -> {
            String q = chatInput.getText().trim();
            if (!q.isEmpty()) {
                chatInput.setText("");
                submitChatQuery(q);
            }
        });
        chatInput.addActionListener(e -> sendBtn.doClick());
        inputRow.add(chatInput, BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);

        panel.add(quickRow, BorderLayout.NORTH);
        panel.add(new JScrollPane(chatHistory), BorderLayout.CENTER);
        panel.add(inputRow, BorderLayout.SOUTH);
        return panel;
    }

    private void submitChatQuery(String query) {
        ChatbotAnswer answer = assistant.ask(query, inventoryService.findAll());
        chatHistory.append("Tú: " + query + "\n");
        chatHistory.append("Asistente [" + answer.getIntentHandled() + "]:\n" + answer.getMessage() + "\n\n");
        chatHistory.setCaretPosition(chatHistory.getDocument().getLength());
    }

    private JPanel buildMoldTab() {
        JTabbedPane modes = new JTabbedPane();
        modes.setFont(CulinaryTheme.FONT_BODY);
        modes.addTab("Modo Escala (Origen -> Destino)", buildScalePanel());
        modes.addTab("Modo Distribución (Repartir Masa)", buildDistributePanel());

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CulinaryTheme.BG_MAIN);
        wrap.add(modes, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildScalePanel() {
        JPanel card = CulinaryTheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 12));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<MoldShape> shapeA = new JComboBox<>(MoldShape.values());
        JTextField dimA1 = new JTextField("20", 6);
        JTextField dimA2 = new JTextField("20", 6);
        JTextField hA = new JTextField("5", 6);

        JComboBox<MoldShape> shapeB = new JComboBox<>(MoldShape.values());
        JTextField dimB1 = new JTextField("26", 6);
        JTextField dimB2 = new JTextField("26", 6);
        JTextField hB = new JTextField("6", 6);

        JLabel resultLbl = new JLabel("Presiona Calcular Escala para ver el factor multiplicador.");
        resultLbl.setFont(CulinaryTheme.FONT_MONO_BOLD);
        resultLbl.setForeground(CulinaryTheme.ACCENT_APRICOT);

        int r = 0;
        gbc.gridy = r++; gbc.gridx = 0; grid.add(new JLabel("Molde Origen — Forma:"), gbc);
        gbc.gridx = 1; grid.add(shapeA, gbc);
        gbc.gridx = 2; grid.add(new JLabel("Diám/Lado/Largo (cm):"), gbc);
        gbc.gridx = 3; grid.add(dimA1, gbc);
        gbc.gridx = 4; grid.add(new JLabel("Ancho (si rect.):"), gbc);
        gbc.gridx = 5; grid.add(dimA2, gbc);
        gbc.gridx = 6; grid.add(new JLabel("Alto (cm):"), gbc);
        gbc.gridx = 7; grid.add(hA, gbc);

        gbc.gridy = r++; gbc.gridx = 0; grid.add(new JLabel("Molde Destino — Forma:"), gbc);
        gbc.gridx = 1; grid.add(shapeB, gbc);
        gbc.gridx = 2; grid.add(new JLabel("Diám/Lado/Largo (cm):"), gbc);
        gbc.gridx = 3; grid.add(dimB1, gbc);
        gbc.gridx = 4; grid.add(new JLabel("Ancho (si rect.):"), gbc);
        gbc.gridx = 5; grid.add(dimB2, gbc);
        gbc.gridx = 6; grid.add(new JLabel("Alto (cm):"), gbc);
        gbc.gridx = 7; grid.add(hB, gbc);

        JButton calcBtn = CulinaryTheme.createPrimaryButton("Calcular Escala");
        calcBtn.addActionListener(e -> {
            try {
                BakingMold origin = buildMold("Origen", (MoldShape) shapeA.getSelectedItem(), dimA1, dimA2, hA);
                BakingMold target = buildMold("Destino", (MoldShape) shapeB.getSelectedItem(), dimB1, dimB2, hB);
                MoldScaleResult res = moldCalculator.scaleRecipeBetweenMolds(origin, target);
                resultLbl.setText(String.format(Locale.US,
                        "<html>Vol. Origen: %.1f cm³ | Vol. Destino: %.1f cm³ | Factor: <b>%.3fx</b> (%.1f%%)<br/>%s</html>",
                        res.getOriginVolumeCm3(), res.getTargetVolumeCm3(),
                        res.getScaleFactor(), res.getPercentageChange(), res.getExplanation()));
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(card, ex.getMessage(), "Datos inválidos", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRow.setOpaque(false);
        btnRow.add(calcBtn);

        card.add(grid, BorderLayout.NORTH);
        card.add(resultLbl, BorderLayout.CENTER);
        card.add(btnRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildDistributePanel() {
        JPanel card = CulinaryTheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        JTextField totalGramsField = new JTextField("1500", 8);
        totalGramsField.setBorder(CulinaryTheme.fieldBorder());
        recipeWeightCombo.addActionListener(e -> {
            RecipeWeightChoice ch = (RecipeWeightChoice) recipeWeightCombo.getSelectedItem();
            if (ch != null && ch.recipe != null) {
                totalGramsField.setText(String.format(Locale.US, "%.0f", ch.recipe.calculateTotalNetGrams()));
            }
        });

        DefaultListModel<BakingMold> moldListModel = new DefaultListModel<>();
        moldListModel.addElement(new RoundMold("Molde 15 cm", 15.0, 5.0));
        moldListModel.addElement(new RoundMold("Molde 20 cm", 20.0, 5.0));
        JList<BakingMold> moldList = new JList<>(moldListModel);

        JComboBox<MoldShape> newShape = new JComboBox<>(MoldShape.values());
        JTextField d1 = new JTextField("24", 5);
        JTextField d2 = new JTextField("12", 5);
        JTextField h = new JTextField("5", 5);
        JButton addMoldBtn = CulinaryTheme.createSecondaryButton("+ Agregar Molde");
        addMoldBtn.addActionListener(e -> {
            try {
                BakingMold m = buildMold("Molde #" + (moldListModel.size() + 1),
                        (MoldShape) newShape.getSelectedItem(), d1, d2, h);
                moldListModel.addElement(m);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(card, ex.getMessage(), "Molde inválido", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton removeMoldBtn = CulinaryTheme.createSecondaryButton("Quitar Molde");
        removeMoldBtn.addActionListener(e -> {
            int idx = moldList.getSelectedIndex();
            if (idx >= 0) {
                moldListModel.remove(idx);
            }
        });

        DefaultTableModel distModel = new DefaultTableModel(
                new String[]{"Molde", "Forma", "Volumen (cm³)", "Proporción (%)", "Gramos a Verter"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable distTable = new JTable(distModel);
        CulinaryTheme.styleTable(distTable);
        distTable.getColumnModel().getColumn(2).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        distTable.getColumnModel().getColumn(3).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        distTable.getColumnModel().getColumn(4).setCellRenderer(CulinaryTheme.rightMonoRenderer());

        JButton calcDistBtn = CulinaryTheme.createPrimaryButton("Distribuir Masa");
        calcDistBtn.addActionListener(e -> {
            try {
                String gTxt = totalGramsField.getText().trim();
                if (!InputRegexValidator.isValidPositiveDecimal(gTxt)) {
                    throw new IllegalArgumentException("Ingresa un peso total válido en gramos.");
                }
                List<BakingMold> molds = new ArrayList<>();
                for (int i = 0; i < moldListModel.size(); i++) {
                    molds.add(moldListModel.getElementAt(i));
                }
                MoldDistributionResult res = moldCalculator.distributeMixture(Double.parseDouble(gTxt), molds);
                distModel.setRowCount(0);
                for (MoldDistributionResult.MoldAllocation a : res.getAllocations()) {
                    distModel.addRow(new Object[]{
                            a.getMoldName(),
                            a.getShapeLabel(),
                            String.format(Locale.US, "%.1f", a.getVolumeCm3()),
                            String.format(Locale.US, "%.1f%%", a.getProportionPercentage()),
                            a.getAssignedGrams() + " g"
                    });
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(card, ex.getMessage(), "Error de cálculo", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topRow.setOpaque(false);
        topRow.add(new JLabel("Tomar peso de receta:"));
        topRow.add(recipeWeightCombo);
        topRow.add(new JLabel("Peso Total Masa (g):"));
        topRow.add(totalGramsField);
        topRow.add(calcDistBtn);

        JPanel addMoldRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        addMoldRow.setOpaque(false);
        addMoldRow.add(new JLabel("Forma:"));
        addMoldRow.add(newShape);
        addMoldRow.add(new JLabel("D/Lado/Largo:"));
        addMoldRow.add(d1);
        addMoldRow.add(new JLabel("Ancho:"));
        addMoldRow.add(d2);
        addMoldRow.add(new JLabel("Alto:"));
        addMoldRow.add(h);
        addMoldRow.add(addMoldBtn);
        addMoldRow.add(removeMoldBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(topRow, BorderLayout.NORTH);
        north.add(addMoldRow, BorderLayout.CENTER);
        north.add(new JScrollPane(moldList), BorderLayout.SOUTH);

        card.add(north, BorderLayout.NORTH);
        card.add(CulinaryTheme.wrapTable(distTable), BorderLayout.CENTER);
        return card;
    }

    private static BakingMold buildMold(String name, MoldShape shape, JTextField f1, JTextField f2, JTextField fh) {
        double d1 = parseDim(f1.getText(), "Dimensión principal");
        double h = parseDim(fh.getText(), "Altura");
        return switch (shape) {
            case ROUND -> new RoundMold(name, d1, h);
            case SQUARE -> new SquareMold(name, d1, h);
            case RECTANGULAR -> new RectangularMold(name, d1, parseDim(f2.getText(), "Ancho"), h);
        };
    }

    private static double parseDim(String raw, String label) {
        String txt = raw == null ? "" : raw.trim();
        if (!InputRegexValidator.isValidPositiveDecimal(txt)) {
            throw new IllegalArgumentException(label + " debe ser un número positivo válido.");
        }
        return Double.parseDouble(txt);
    }

    private record RecipeWeightChoice(Recipe recipe) {
        @Override
        public String toString() {
            if (recipe == null) {
                return "-- Peso manual --";
            }
            return String.format(Locale.US, "%s (%.0f g)", recipe.getName(), recipe.calculateTotalNetGrams());
        }
    }
}
