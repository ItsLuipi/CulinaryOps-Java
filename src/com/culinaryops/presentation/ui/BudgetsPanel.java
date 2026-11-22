package com.culinaryops.presentation.ui;

import com.culinaryops.application.BudgetService;
import com.culinaryops.application.RecipeService;
import com.culinaryops.domain.exception.CulinaryDomainException;
import com.culinaryops.domain.model.BudgetItem;
import com.culinaryops.domain.model.OperationalCosts;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.ServiceBudget;
import com.culinaryops.domain.model.dto.ClientQuoteDTO;
import com.culinaryops.domain.model.enums.BudgetStatus;
import com.culinaryops.domain.service.CurrencyFormatter;
import com.culinaryops.domain.service.InputRegexValidator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Módulo 5: Presupuestos & Cotizador comercial (con Ficha Cliente confidencial).
 */
public class BudgetsPanel extends JPanel {

    private final BudgetService budgetService;
    private final RecipeService recipeService;
    private final Runnable onDataChanged;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel blockRaw = CulinaryTheme.createKpiLabel("$0.00 USD");
    private final JLabel blockOps = CulinaryTheme.createKpiLabel("$0.00 USD");
    private final JLabel blockBase = CulinaryTheme.createKpiLabel("$0.00 USD");
    private final JLabel blockFinal = CulinaryTheme.createKpiLabel("$0.00 USD");
    private final JLabel blockBaseSub = new JLabel("Margen: 0.0%");
    private final JLabel blockFinalSub = new JLabel("Impuestos: 0.0%");
    private List<ServiceBudget> displayed = List.of();

    public BudgetsPanel(
            BudgetService budgetService,
            RecipeService recipeService,
            Runnable onDataChanged) {
        this.budgetService = budgetService;
        this.recipeService = recipeService;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 12));
        setBackground(CulinaryTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Cabecera
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Presupuestos");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JButton newBtn = CulinaryTheme.createPrimaryButton("+ Nuevo Presupuesto");
        newBtn.addActionListener(e -> openNewBudgetDialog());
        header.add(title, BorderLayout.WEST);
        header.add(newBtn, BorderLayout.EAST);

        // Desglose visual de 4 bloques del presupuesto seleccionado
        JPanel blocksRow = new JPanel(new GridLayout(1, 4, 12, 0));
        blocksRow.setOpaque(false);
        blocksRow.add(CulinaryTheme.buildKpiCard("1. Materia Prima (Recetas)", blockRaw, new JLabel("Costo directo"), false));
        blocksRow.add(CulinaryTheme.buildKpiCard("2. Costos Operativos", blockOps, new JLabel("Mano obra + gastos"), false));
        blocksRow.add(CulinaryTheme.buildKpiCard("3. Costo Base + Margen", blockBase, blockBaseSub, false));
        blocksRow.add(CulinaryTheme.buildKpiCard("4. Precio Final Cliente", blockFinal, blockFinalSub, true));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(blocksRow, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Tabla de presupuestos
        tableModel = new DefaultTableModel(new String[]{
                "Cliente", "Evento / Servicio", "Entrega", "Estado", "Base (USD)", "Margen", "Precio Final"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        CulinaryTheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(4).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedBlocks();
            }
        });
        add(CulinaryTheme.wrapTable(table), BorderLayout.CENTER);

        // Acciones
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton quoteBtn = CulinaryTheme.createPrimaryButton("Ver Ficha Cliente");
        quoteBtn.addActionListener(e -> openClientQuoteModal());
        JButton statusBtn = CulinaryTheme.createSecondaryButton("Cambiar Estado");
        statusBtn.addActionListener(e -> changeSelectedStatus());
        JButton deleteBtn = CulinaryTheme.createSecondaryButton("Eliminar");
        deleteBtn.setForeground(CulinaryTheme.ALERT_RED);
        deleteBtn.addActionListener(e -> deleteSelected());
        footer.add(quoteBtn);
        footer.add(statusBtn);
        footer.add(deleteBtn);
        add(footer, BorderLayout.SOUTH);

        refreshData();
    }

    public final void refreshData() {
        displayed = budgetService.getAllBudgets();
        tableModel.setRowCount(0);
        for (ServiceBudget b : displayed) {
            tableModel.addRow(new Object[]{
                    b.getClientName(),
                    b.getEventName(),
                    b.getEventDate() == null ? "Sin fecha" : b.getEventDate().toString(),
                    b.getStatus().name(),
                    CurrencyFormatter.formatUSD(b.getBaseCost()),
                    String.format(Locale.US, "%.1f%%", b.getProfitMarginPercentage()),
                    CurrencyFormatter.formatUSD(b.getFinalPrice())
            });
        }
        if (!displayed.isEmpty() && table.getSelectedRow() < 0) {
            table.setRowSelectionInterval(0, 0);
        }
        updateSelectedBlocks();
    }

    private void updateSelectedBlocks() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayed.size()) {
            blockRaw.setText("$0.00 USD");
            blockOps.setText("$0.00 USD");
            blockBase.setText("$0.00 USD");
            blockFinal.setText("$0.00 USD");
            blockBaseSub.setText("Margen: 0.0%");
            blockFinalSub.setText("Impuestos: 0.0%");
            return;
        }
        ServiceBudget b = displayed.get(row);
        blockRaw.setText(CurrencyFormatter.formatUSD(b.getRawMaterialCost()));
        blockOps.setText(CurrencyFormatter.formatUSD(b.getTotalOperationalCost()));
        blockBase.setText(CurrencyFormatter.formatUSD(b.getBaseCost()));
        blockBaseSub.setText(String.format(Locale.US, "Margen: %.1f%% (+%s)",
                b.getProfitMarginPercentage(), CurrencyFormatter.formatUSD(b.getProfitAmount())));
        blockFinal.setText(CurrencyFormatter.formatUSD(b.getFinalPrice()));
        blockFinalSub.setText(String.format(Locale.US, "Impuestos: %.1f%% (+%s)",
                b.getTaxPercentage(), CurrencyFormatter.formatUSD(b.getTaxAmount())));
    }

    private ServiceBudget getSelectedBudget() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayed.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona un presupuesto de la tabla.",
                    "Selección requerida", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return displayed.get(row);
    }

    private void changeSelectedStatus() {
        ServiceBudget b = getSelectedBudget();
        if (b == null) {
            return;
        }
        BudgetStatus next = (BudgetStatus) JOptionPane.showInputDialog(
                this,
                "Selecciona nuevo estado para '" + b.getClientName() + "':",
                "Estado Comercial",
                JOptionPane.QUESTION_MESSAGE,
                null,
                BudgetStatus.values(),
                b.getStatus());
        if (next != null) {
            budgetService.updateStatus(b.getId(), next);
            refreshData();
            onDataChanged.run();
        }
    }

    private void deleteSelected() {
        ServiceBudget b = getSelectedBudget();
        if (b == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmas eliminar el presupuesto de '" + b.getClientName() + "'?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            budgetService.deleteBudget(b.getId());
            refreshData();
            onDataChanged.run();
        }
    }

    private void openClientQuoteModal() {
        ServiceBudget b = getSelectedBudget();
        if (b == null) {
            return;
        }
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Ficha Comercial Cliente — Confidencialidad Activa", true);
        CulinaryTheme.installEscapeClose(dialog);
        dialog.getContentPane().setBackground(CulinaryTheme.BG_CARD);
        dialog.setLayout(new BorderLayout(0, 10));

        ClientQuoteDTO dto = b.generateClientQuote();
        String exportedText = budgetService.exportClientQuote(b.getId());

        JPanel topInfo = new JPanel(new BorderLayout(0, 4));
        topInfo.setBackground(CulinaryTheme.BG_CARD);
        topInfo.setBorder(BorderFactory.createEmptyBorder(14, 18, 4, 18));
        JLabel title = new JLabel("Cotización para " + dto.clientName() + " — " + dto.eventName());
        title.setFont(CulinaryTheme.FONT_SUBTITLE);
        title.setForeground(CulinaryTheme.ACCENT_APRICOT);
        JLabel note = new JLabel("Vista limpia para cliente: omite costos de materia prima, gastos internos y márgenes.");
        note.setFont(CulinaryTheme.FONT_SMALL);
        note.setForeground(CulinaryTheme.TEXT_SECONDARY);
        topInfo.add(title, BorderLayout.NORTH);
        topInfo.add(note, BorderLayout.SOUTH);

        JTextArea area = new JTextArea(exportedText, 18, 58);
        area.setEditable(false);
        area.setFont(CulinaryTheme.FONT_MONO);
        area.setBackground(CulinaryTheme.BG_MAIN);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        buttons.setBackground(CulinaryTheme.BG_MAIN);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CulinaryTheme.BORDER_FLOUR));

        JButton copyBtn = CulinaryTheme.createSecondaryButton("Copiar Texto");
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(exportedText), null);
            JOptionPane.showMessageDialog(dialog, "Cotización copiada al portapapeles.");
        });
        JButton exportBtn = CulinaryTheme.createPrimaryButton("Guardar Ficha (.txt)");
        exportBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("cotizacion_" + b.getId() + ".txt"));
            if (chooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.writeString(chooser.getSelectedFile().toPath(), exportedText, StandardCharsets.UTF_8);
                    JOptionPane.showMessageDialog(dialog, "Archivo guardado correctamente.");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "Error al guardar: " + ex.getMessage(),
                            "Error E/S", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JButton closeBtn = CulinaryTheme.createSecondaryButton("Cerrar");
        closeBtn.addActionListener(e -> dialog.dispose());

        buttons.add(copyBtn);
        buttons.add(exportBtn);
        buttons.add(closeBtn);

        dialog.add(topInfo, BorderLayout.NORTH);
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void openNewBudgetDialog() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Nuevo Presupuesto Comercial", true);
        CulinaryTheme.installEscapeClose(dialog);
        dialog.getContentPane().setBackground(CulinaryTheme.BG_CARD);
        dialog.setLayout(new BorderLayout(0, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CulinaryTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(12, 18, 6, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField clientField = new JTextField(22);
        JTextField eventField = new JTextField("Servicio de Pastelería", 22);
        JTextField dateField = new JTextField(LocalDate.now().plusDays(14).toString(), 12);
        JTextField notesField = new JTextField(22);

        JTextField hoursField = new JTextField("4.0", 6);
        JTextField rateField = new JTextField("14.0", 6);
        JTextField utilField = new JTextField("8.0", 6);
        JTextField equipField = new JTextField("5.0", 6);
        JTextField packField = new JTextField("12.0", 6);

        JSlider marginSlider = CulinaryTheme.createStyledSlider(0, 100, 30);
        JLabel marginValLabel = new JLabel("30%");
        JTextField taxField = new JTextField("0.0", 6);

        int r = 0;
        addRow(form, gbc, r++, "Cliente *:", clientField);
        addRow(form, gbc, r++, "Evento / Servicio:", eventField);
        addRow(form, gbc, r++, "Fecha Entrega (YYYY-MM-DD):", dateField);
        addRow(form, gbc, r++, "Notas:", notesField);
        addRow(form, gbc, r++, "Horas de Trabajo:", hoursField);
        addRow(form, gbc, r++, "Tarifa (USD/hora):", rateField);
        addRow(form, gbc, r++, "Luz, Gas, Agua (USD):", utilField);
        addRow(form, gbc, r++, "Desgaste Equipos (USD):", equipField);
        addRow(form, gbc, r++, "Cajas / Envío (USD):", packField);

        JPanel sliderBox = new JPanel(new BorderLayout(8, 0));
        sliderBox.setOpaque(false);
        sliderBox.add(marginSlider, BorderLayout.CENTER);
        sliderBox.add(marginValLabel, BorderLayout.EAST);
        addRow(form, gbc, r++, "Margen de Utilidad (%):", sliderBox);
        addRow(form, gbc, r, "Impuestos (%):", taxField);

        // Recetas del servicio (inician vacías "-- Seleccionar receta --")
        JPanel rowsContainer = new JPanel();
        rowsContainer.setLayout(new BoxLayout(rowsContainer, BoxLayout.Y_AXIS));
        rowsContainer.setBackground(CulinaryTheme.BG_CARD);
        List<RecipeRow> recipeRows = new ArrayList<>();
        List<Recipe> allRecipes = recipeService.findAll();

        JLabel livePanel = new JLabel("Costo Base: $0.00 USD | Ganancia: $0.00 USD | Precio Final: $0.00 USD");
        livePanel.setFont(CulinaryTheme.FONT_MONO_BOLD);
        livePanel.setForeground(CulinaryTheme.ACCENT_APRICOT);
        JButton saveBtn = CulinaryTheme.createPrimaryButton("Guardar Presupuesto");

        Runnable[] recalcRef = new Runnable[1];
        recalcRef[0] = () -> {
            marginValLabel.setText(marginSlider.getValue() + "%");
            double rawMat = 0.0;
            boolean itemsOk = !recipeRows.isEmpty();
            for (RecipeRow rowItem : recipeRows) {
                RecipeChoice ch = (RecipeChoice) rowItem.combo.getSelectedItem();
                Recipe rec = ch == null ? null : ch.recipe;
                String qTxt = rowItem.qtyField.getText().trim();
                if (rec == null || !InputRegexValidator.isValidPositiveDecimal(qTxt) || Double.parseDouble(qTxt) <= 0) {
                    itemsOk = false;
                } else {
                    rawMat += new BudgetItem(rec, Double.parseDouble(qTxt)).getSubtotalCost();
                }
            }

            OperationalCosts ops = new OperationalCosts(
                    parseOrZero(hoursField), parseOrZero(rateField), parseOrZero(utilField),
                    parseOrZero(equipField), parseOrZero(packField));
            double base = rawMat + ops.getTotalOperationalCost();
            double profit = base * (marginSlider.getValue() / 100.0);
            double suggested = base + profit;
            double taxPct = parseOrZero(taxField);
            double finalPrice = suggested + (suggested * (taxPct / 100.0));

            livePanel.setText(String.format(Locale.US,
                    "Costo Base: %s | Ganancia: %s | Precio Final: %s",
                    CurrencyFormatter.formatUSD(base),
                    CurrencyFormatter.formatUSD(profit),
                    CurrencyFormatter.formatUSD(finalPrice)));

            boolean clientOk = InputRegexValidator.isValidName(clientField.getText());
            String dTxt = dateField.getText().trim();
            boolean dateOk = dTxt.isEmpty() || InputRegexValidator.isValidIsoDate(dTxt);
            saveBtn.setEnabled(clientOk && dateOk && itemsOk);
        };

        Runnable addEmptyRow = () -> {
            RecipeRow rr = new RecipeRow(allRecipes, () -> recalcRef[0].run(), toRemove -> {
                if (recipeRows.size() > 1) {
                    recipeRows.remove(toRemove);
                    rowsContainer.remove(toRemove.panel);
                    rowsContainer.revalidate();
                    rowsContainer.repaint();
                    recalcRef[0].run();
                }
            });
            recipeRows.add(rr);
            rowsContainer.add(rr.panel);
            rowsContainer.revalidate();
            rowsContainer.repaint();
            recalcRef[0].run();
        };
        addEmptyRow.run();

        DocListener dl = new DocListener(recalcRef[0]);
        clientField.getDocument().addDocumentListener(dl);
        dateField.getDocument().addDocumentListener(dl);
        hoursField.getDocument().addDocumentListener(dl);
        rateField.getDocument().addDocumentListener(dl);
        utilField.getDocument().addDocumentListener(dl);
        equipField.getDocument().addDocumentListener(dl);
        packField.getDocument().addDocumentListener(dl);
        taxField.getDocument().addDocumentListener(dl);
        marginSlider.addChangeListener(e -> recalcRef[0].run());

        JPanel recipesSection = new JPanel(new BorderLayout(0, 6));
        recipesSection.setBackground(CulinaryTheme.BG_CARD);
        recipesSection.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 18));
        JPanel recHeader = new JPanel(new BorderLayout());
        recHeader.setOpaque(false);
        JLabel recTitle = new JLabel("Recetas del Servicio:");
        recTitle.setFont(CulinaryTheme.FONT_SUBTITLE);
        JButton addRecBtn = CulinaryTheme.createSecondaryButton("+ Añadir Receta");
        addRecBtn.addActionListener(e -> addEmptyRow.run());
        recHeader.add(recTitle, BorderLayout.WEST);
        recHeader.add(addRecBtn, BorderLayout.EAST);

        JScrollPane recScroll = new JScrollPane(rowsContainer);
        recScroll.setPreferredSize(new Dimension(560, 120));

        JPanel summaryWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summaryWrap.setOpaque(false);
        summaryWrap.add(Box.createHorizontalStrut(2));
        summaryWrap.add(livePanel);

        recipesSection.add(recHeader, BorderLayout.NORTH);
        recipesSection.add(recScroll, BorderLayout.CENTER);
        recipesSection.add(summaryWrap, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(CulinaryTheme.BG_CARD);
        center.add(form, BorderLayout.NORTH);
        center.add(recipesSection, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBackground(CulinaryTheme.BG_MAIN);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CulinaryTheme.BORDER_FLOUR));
        JButton cancelBtn = CulinaryTheme.createSecondaryButton("Cancelar");
        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                Map<String, Double> quantities = new LinkedHashMap<>();
                for (RecipeRow rowItem : recipeRows) {
                    RecipeChoice ch = (RecipeChoice) rowItem.combo.getSelectedItem();
                    double qty = Double.parseDouble(rowItem.qtyField.getText().trim());
                    quantities.merge(ch.recipe.getId(), qty, Double::sum);
                }
                OperationalCosts ops = new OperationalCosts(
                        parseOrZero(hoursField), parseOrZero(rateField), parseOrZero(utilField),
                        parseOrZero(equipField), parseOrZero(packField));
                String dTxt = dateField.getText().trim();
                LocalDate eventDate = dTxt.isEmpty() ? null : LocalDate.parse(dTxt);

                budgetService.createBudget(
                        clientField.getText(), eventField.getText(), eventDate,
                        quantities, ops, marginSlider.getValue(), parseOrZero(taxField), notesField.getText());
                dialog.dispose();
                refreshData();
                onDataChanged.run();
            } catch (CulinaryDomainException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error al guardar", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttons.add(cancelBtn);
        buttons.add(saveBtn);

        dialog.add(center, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        recalcRef[0].run();
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static double parseOrZero(JTextField field) {
        String txt = field.getText().trim();
        return InputRegexValidator.isValidPositiveDecimal(txt) ? Double.parseDouble(txt) : 0.0;
    }

    private static void addRow(JPanel form, GridBagConstraints gbc, int row, String label, java.awt.Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(CulinaryTheme.FONT_BODY_BOLD);
        form.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(comp, gbc);
    }

    private record RecipeChoice(Recipe recipe) {
        @Override
        public String toString() {
            if (recipe == null) {
                return "-- Seleccionar receta --";
            }
            return recipe.getName() + " (" + CurrencyFormatter.formatUSD(recipe.calculateDirectCost()) + "/tanda)";
        }
    }

    private static class RecipeRow {
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        final JComboBox<RecipeChoice> combo = new JComboBox<>();
        final JTextField qtyField = new JTextField("1", 5);

        RecipeRow(List<Recipe> recipes, Runnable onChange, java.util.function.Consumer<RecipeRow> onRemove) {
            panel.setBackground(CulinaryTheme.BG_CARD);
            combo.addItem(new RecipeChoice(null));
            for (Recipe r : recipes) {
                combo.addItem(new RecipeChoice(r));
            }
            qtyField.setBorder(CulinaryTheme.fieldBorder());
            combo.addActionListener(e -> onChange.run());
            qtyField.getDocument().addDocumentListener(new DocListener(onChange));
            JButton removeBtn = CulinaryTheme.createSecondaryButton("Quitar");
            removeBtn.addActionListener(e -> onRemove.accept(this));

            panel.add(combo);
            panel.add(new JLabel("Cant:"));
            panel.add(qtyField);
            panel.add(removeBtn);
        }
    }

    private static class DocListener implements DocumentListener {
        private final Runnable cb;
        DocListener(Runnable cb) { this.cb = cb; }
        @Override public void insertUpdate(DocumentEvent e) { cb.run(); }
        @Override public void removeUpdate(DocumentEvent e) { cb.run(); }
        @Override public void changedUpdate(DocumentEvent e) { cb.run(); }
    }
}
