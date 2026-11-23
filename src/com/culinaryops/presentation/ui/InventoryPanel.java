package com.culinaryops.presentation.ui;

import com.culinaryops.application.CatalogService;
import com.culinaryops.application.InventoryService;
import com.culinaryops.domain.exception.CulinaryDomainException;
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.enums.StockMovementType;
import com.culinaryops.domain.service.CurrencyFormatter;
import com.culinaryops.domain.service.InputRegexValidator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Módulo 3: Control de Inventario & Stock (existencias físicas, movimientos y caducidades).
 */
public class InventoryPanel extends JPanel {

    private final InventoryService inventoryService;
    private final CatalogService catalogService;
    private final Runnable onDataChanged;

    private final JLabel kpiRawValue = CulinaryTheme.createKpiLabel("$0.00 USD");
    private final JLabel kpiRawSub = new JLabel("Costo de compra en almacén");
    private final JLabel kpiEffValue = CulinaryTheme.createKpiLabel("$0.00 USD");
    private final JLabel kpiEffSub = new JLabel("Valor aprovechable neto");
    private final JLabel kpiLowValue = CulinaryTheme.createKpiLabel("0");
    private final JLabel kpiLowSub = new JLabel("Umbral <= 2.0 unidades");
    private final JLabel kpiExpValue = CulinaryTheme.createKpiLabel("0");
    private final JLabel kpiExpSub = new JLabel("Vencen en <= 7 días");

    private final JTextField searchField = new JTextField(16);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JCheckBox perishableOnlyCheck = new JCheckBox("Solo Perecederos");
    private final JCheckBox lowStockOnlyCheck = new JCheckBox("Solo Stock Bajo");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Ingredient> displayed = List.of();

    public InventoryPanel(
            InventoryService inventoryService,
            CatalogService catalogService,
            Runnable onDataChanged) {
        this.inventoryService = inventoryService;
        this.catalogService = catalogService;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 12));
        setBackground(CulinaryTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Cabecera + KPIs + Filtros
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Control de Inventario");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JButton adjustBtn = CulinaryTheme.createPrimaryButton("Ajustar Stock");
        adjustBtn.addActionListener(e -> openSelectedOrPicker());
        header.add(title, BorderLayout.WEST);
        header.add(adjustBtn, BorderLayout.EAST);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.add(CulinaryTheme.buildKpiCard("Valor Total Compra", kpiRawValue, kpiRawSub, false));
        kpiRow.add(CulinaryTheme.buildKpiCard("Valor Útil con Mermas", kpiEffValue, kpiEffSub, false));
        kpiRow.add(CulinaryTheme.buildKpiCard("Stock Bajo", kpiLowValue, kpiLowSub, true));
        kpiRow.add(CulinaryTheme.buildKpiCard("Próximos a Vencer", kpiExpValue, kpiExpSub, true));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setOpaque(false);
        searchField.setBorder(CulinaryTheme.fieldBorder());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        categoryFilter.addActionListener(e -> applyFilter());
        perishableOnlyCheck.setOpaque(false);
        perishableOnlyCheck.addActionListener(e -> applyFilter());
        lowStockOnlyCheck.setOpaque(false);
        lowStockOnlyCheck.addActionListener(e -> applyFilter());

        filters.add(new JLabel("Buscar:"));
        filters.add(searchField);
        filters.add(new JLabel("Categoría:"));
        filters.add(categoryFilter);
        filters.add(perishableOnlyCheck);
        filters.add(lowStockOnlyCheck);

        top.add(header, BorderLayout.NORTH);
        top.add(kpiRow, BorderLayout.CENTER);
        top.add(filters, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Tabla de existencias
        tableModel = new DefaultTableModel(new String[]{
                "Ingrediente", "Categoría", "Stock Físico", "Valor Compra", "Valor Útil", "Caducidad"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        CulinaryTheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(2).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        add(CulinaryTheme.wrapTable(table), BorderLayout.CENTER);

        refreshData();
    }

    public final void refreshData() {
        kpiRawValue.setText(CurrencyFormatter.formatUSD(inventoryService.getTotalInventoryValue()));
        kpiEffValue.setText(CurrencyFormatter.formatUSD(inventoryService.getTotalEffectiveInventoryValue()));
        kpiLowValue.setText(String.valueOf(inventoryService.findLowStock().size()));
        kpiExpValue.setText(String.valueOf(inventoryService.findExpiringSoon(7).size()));

        String prevCat = (String) categoryFilter.getSelectedItem();
        categoryFilter.removeAllItems();
        categoryFilter.addItem("Todas las Categorías");
        for (String cat : catalogService.getAvailableCategories()) {
            categoryFilter.addItem(cat);
        }
        if (prevCat != null) {
            categoryFilter.setSelectedItem(prevCat);
        }
        applyFilter();
    }

    private void applyFilter() {
        displayed = inventoryService.search(
                searchField.getText(),
                (String) categoryFilter.getSelectedItem(),
                perishableOnlyCheck.isSelected(),
                lowStockOnlyCheck.isSelected());

        tableModel.setRowCount(0);
        for (Ingredient ing : displayed) {
            String stockStr = String.format(Locale.US, "%.2f %s",
                    ing.getStockQuantity(), ing.getUnit().getSymbol());
            if (ing.isLowStock()) {
                stockStr = "⚠ Stock bajo  " + stockStr;
            }
            tableModel.addRow(new Object[]{
                    ing.getName(),
                    ing.getCategory(),
                    stockStr,
                    CurrencyFormatter.formatUSD(ing.getTotalStockValue()),
                    CurrencyFormatter.formatUSD(ing.getTotalEffectiveStockValue()),
                    CulinaryTheme.formatExpirationText(ing)
            });
        }
    }

    private void openSelectedOrPicker() {
        int row = table.getSelectedRow();
        Ingredient preselected = (row >= 0 && row < displayed.size()) ? displayed.get(row) : null;
        openAdjustDialog(preselected);
    }

    private void openAdjustDialog(Ingredient preselected) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Ajustar Cantidad de Inventario", true);
        CulinaryTheme.installEscapeClose(dialog);
        dialog.getContentPane().setBackground(CulinaryTheme.BG_CARD);
        dialog.setLayout(new BorderLayout(0, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CulinaryTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JComboBox<IngredientChoice> ingCombo = new JComboBox<>();
        ingCombo.addItem(new IngredientChoice(null));
        List<Ingredient> sorted = catalogService.findAll().stream()
                .sorted(Comparator.comparing(Ingredient::getCategory).thenComparing(Ingredient::getName))
                .toList();
        IngredientChoice toSelect = null;
        for (Ingredient ing : sorted) {
            IngredientChoice choice = new IngredientChoice(ing);
            ingCombo.addItem(choice);
            if (preselected != null && ing.getId().equals(preselected.getId())) {
                toSelect = choice;
            }
        }
        if (toSelect != null) {
            ingCombo.setSelectedItem(toSelect);
        }
        JLabel ingFeedback = createFeedbackLabel();

        JTextField qtyField = new JTextField(14);
        qtyField.setBorder(CulinaryTheme.fieldBorder());
        JLabel qtyFeedback = createFeedbackLabel();

        JLabel dateLabel = new JLabel("Caducidad YYYY-MM-DD (opcional):");
        dateLabel.setFont(CulinaryTheme.FONT_BODY_BOLD);
        dateLabel.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JTextField dateField = new JTextField(14);
        dateField.setBorder(CulinaryTheme.fieldBorder());
        dateField.setToolTipText("Opcional: YYYY-MM-DD");
        JLabel dateFeedback = createFeedbackLabel();

        JLabel previewLabel = new JLabel(" ");
        previewLabel.setFont(CulinaryTheme.FONT_MONO_BOLD);

        JButton saveBtn = CulinaryTheme.createPrimaryButton("Guardar Cantidad");
        JButton cancelBtn = CulinaryTheme.createSecondaryButton("Cancelar");
        dialog.getRootPane().setDefaultButton(saveBtn);

        Runnable syncFieldsFromSelection = () -> {
            IngredientChoice ch = (IngredientChoice) ingCombo.getSelectedItem();
            if (ch != null && ch.ingredient != null) {
                qtyField.setText(formatPlainNumber(ch.ingredient.getStockQuantity()));
                dateField.setText(ch.ingredient.getExpirationDate() != null
                        ? ch.ingredient.getExpirationDate().toString() : "");
            } else {
                qtyField.setText("");
                dateField.setText("");
            }
        };

        Runnable updatePreview = () -> {
            IngredientChoice selected = (IngredientChoice) ingCombo.getSelectedItem();
            Ingredient ing = selected == null ? null : selected.ingredient;
            if (ing == null) {
                ingFeedback.setText("Selecciona un ingrediente del catálogo para ajustar su stock.");
                ingFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
                qtyField.setEnabled(false);
                qtyField.setBorder(CulinaryTheme.fieldBorder());
                qtyFeedback.setText(" ");
                dateLabel.setVisible(false);
                dateField.setVisible(false);
                dateFeedback.setVisible(false);
                previewLabel.setText("Vista previa: selecciona un ingrediente");
                previewLabel.setForeground(CulinaryTheme.TEXT_SECONDARY);
                saveBtn.setEnabled(false);
                return;
            }

            ingFeedback.setText(String.format(Locale.US, "Stock actual: %.2f %s | Precio catálogo: %s/%s (solo lectura)",
                    ing.getStockQuantity(), ing.getUnit().getSymbol(),
                    CurrencyFormatter.formatUSD(ing.getUnitPrice()), ing.getUnit().getSymbol()));
            ingFeedback.setForeground(CulinaryTheme.OK_GREEN);

            qtyField.setEnabled(true);
            String qtyTxt = qtyField.getText().trim();
            boolean qtyOk = false;
            double nextQty = 0.0;
            if (qtyTxt.isEmpty()) {
                setFieldBorderState(qtyField, qtyFeedback,
                        "Campo obligatorio (usa número >= 0, ej. 5000).", CulinaryTheme.TEXT_SECONDARY, false);
            } else if (!InputRegexValidator.isValidPositiveDecimal(qtyTxt)) {
                setFieldBorderState(qtyField, qtyFeedback,
                        "⚠ Cantidad inválida. Usa número >= 0 (ej. 5000).", CulinaryTheme.ALERT_RED, true);
            } else {
                nextQty = Double.parseDouble(qtyTxt);
                setFieldBorderState(qtyField, qtyFeedback,
                        "✓ Cantidad válida (" + qtyTxt + " " + ing.getUnit().getSymbol() + ")",
                        CulinaryTheme.OK_GREEN, false);
                qtyOk = true;
            }

            boolean perishable = ing.isPerishable();
            dateLabel.setVisible(perishable);
            dateField.setVisible(perishable);
            dateFeedback.setVisible(perishable);
            boolean dateOk = true;
            if (perishable) {
                String dateTxt = dateField.getText().trim();
                if (dateTxt.isEmpty()) {
                    setFieldBorderState(dateField, dateFeedback,
                            "Opcional (formato YYYY-MM-DD).", CulinaryTheme.TEXT_SECONDARY, false);
                } else if (!isValidCalendarIsoDate(dateTxt)) {
                    setFieldBorderState(dateField, dateFeedback,
                            "⚠ Fecha inválida. Usa YYYY-MM-DD (ej. 2026-12-15).", CulinaryTheme.ALERT_RED, true);
                    dateOk = false;
                } else {
                    setFieldBorderState(dateField, dateFeedback,
                            "✓ Fecha válida", CulinaryTheme.OK_GREEN, false);
                }
            }

            if (qtyOk && dateOk) {
                double newRawValue = nextQty * ing.getUnitPrice();
                previewLabel.setText(String.format(Locale.US,
                        "Nuevo Stock: %.2f -> %.2f %s | Valor: %s",
                        ing.getStockQuantity(), nextQty, ing.getUnit().getSymbol(),
                        CurrencyFormatter.formatUSD(newRawValue)));
                previewLabel.setForeground(CulinaryTheme.ACCENT_APRICOT);
                saveBtn.setEnabled(true);
            } else {
                previewLabel.setText("⚠ Corrige los campos pendientes antes de guardar.");
                previewLabel.setForeground(CulinaryTheme.ALERT_RED);
                saveBtn.setEnabled(false);
            }
        };

        ingCombo.addActionListener(e -> {
            syncFieldsFromSelection.run();
            updatePreview.run();
            dialog.pack();
        });
        qtyField.getDocument().addDocumentListener(new SimpleDoc(updatePreview));
        dateField.getDocument().addDocumentListener(new SimpleDoc(updatePreview));

        int r = 0;
        addFormRow(form, gbc, r++, "Ingrediente del Catálogo *:", ingCombo);
        addFeedbackRow(form, gbc, r++, ingFeedback);
        addFormRow(form, gbc, r++, "Nueva Cantidad en Stock *:", qtyField);
        addFeedbackRow(form, gbc, r++, qtyFeedback);

        gbc.gridx = 0;
        gbc.gridy = r;
        gbc.weightx = 0.0;
        form.add(dateLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(dateField, gbc);
        r++;
        addFeedbackRow(form, gbc, r++, dateFeedback);

        addFeedbackRow(form, gbc, r, previewLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBackground(CulinaryTheme.BG_MAIN);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CulinaryTheme.BORDER_FLOUR));
        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                IngredientChoice ch = (IngredientChoice) ingCombo.getSelectedItem();
                double qty = Double.parseDouble(qtyField.getText().trim());
                String dTxt = dateField.getText().trim();
                LocalDate expDate = (ch.ingredient.isPerishable() && !dTxt.isEmpty())
                        ? LocalDate.parse(dTxt) : null;

                inventoryService.applyMovement(
                        ch.ingredient.getId(), StockMovementType.SET, qty, null, expDate);
                dialog.dispose();
                refreshData();
                onDataChanged.run();
            } catch (CulinaryDomainException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Operación bloqueada", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttons.add(cancelBtn);
        buttons.add(saveBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        syncFieldsFromSelection.run();
        updatePreview.run();
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JLabel createFeedbackLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(CulinaryTheme.FONT_SMALL);
        return lbl;
    }

    private static void setFieldBorderState(
            JTextField field, JLabel feedback, String text, Color color, boolean error) {
        feedback.setText(text);
        feedback.setForeground(color);
        field.setBorder(error
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CulinaryTheme.ALERT_RED, 1),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8))
                : CulinaryTheme.fieldBorder());
    }

    private static boolean isValidCalendarIsoDate(String text) {
        if (!InputRegexValidator.isValidIsoDate(text)) {
            return false;
        }
        try {
            LocalDate.parse(text);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static String formatPlainNumber(double value) {
        if (value == 0.0) {
            return "0";
        }
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, java.awt.Component comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(CulinaryTheme.FONT_BODY_BOLD);
        lbl.setForeground(CulinaryTheme.TEXT_PRIMARY);
        form.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(comp, gbc);
    }

    private static void addFeedbackRow(JPanel form, GridBagConstraints gbc, int row, JLabel feedback) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        form.add(feedback, gbc);
    }

    private record IngredientChoice(Ingredient ingredient) {
        @Override
        public String toString() {
            if (ingredient == null) {
                return "-- Seleccionar ingrediente --";
            }
            return String.format(Locale.US, "[%s] %s (Stock: %.2f %s)",
                    ingredient.getCategory(),
                    ingredient.getName(),
                    ingredient.getStockQuantity(),
                    ingredient.getUnit().getSymbol());
        }
    }

    private static class SimpleDoc implements DocumentListener {
        private final Runnable cb;
        SimpleDoc(Runnable cb) { this.cb = cb; }
        @Override public void insertUpdate(DocumentEvent e) { cb.run(); }
        @Override public void removeUpdate(DocumentEvent e) { cb.run(); }
        @Override public void changedUpdate(DocumentEvent e) { cb.run(); }
    }
}
