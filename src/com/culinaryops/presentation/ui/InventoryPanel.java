package com.culinaryops.presentation.ui;

import com.culinaryops.application.CatalogService;
import com.culinaryops.application.InventoryService;
import com.culinaryops.domain.exception.CulinaryDomainException;
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.enums.StockMovementType;
import com.culinaryops.domain.service.CurrencyFormatter;
import com.culinaryops.domain.service.InputRegexValidator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
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
        JLabel title = new JLabel("Control de Inventario & Stock");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JButton globalMoveBtn = CulinaryTheme.createPrimaryButton("Registrar Movimiento");
        globalMoveBtn.addActionListener(e -> openMovementDialog(null, StockMovementType.ADD));
        header.add(title, BorderLayout.WEST);
        header.add(globalMoveBtn, BorderLayout.EAST);

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

        // Botones de movimiento rápido por fila
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton addBtn = CulinaryTheme.createSecondaryButton("+ Entrada");
        addBtn.addActionListener(e -> openForSelected(StockMovementType.ADD));
        JButton subBtn = CulinaryTheme.createSecondaryButton("- Salida");
        subBtn.addActionListener(e -> openForSelected(StockMovementType.SUBTRACT));
        JButton setBtn = CulinaryTheme.createSecondaryButton("= Ajustar Conteo");
        setBtn.addActionListener(e -> openForSelected(StockMovementType.SET));
        footer.add(addBtn);
        footer.add(subBtn);
        footer.add(setBtn);
        add(footer, BorderLayout.SOUTH);

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

    private void openForSelected(StockMovementType mode) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayed.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona un ingrediente de la tabla.",
                    "Selección requerida", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openMovementDialog(displayed.get(row), mode);
    }

    private void openMovementDialog(Ingredient preselected, StockMovementType initialMode) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Movimiento & Ajuste de Inventario", true);
        CulinaryTheme.installEscapeClose(dialog);
        dialog.getContentPane().setBackground(CulinaryTheme.BG_CARD);
        dialog.setLayout(new BorderLayout(0, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CulinaryTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Selector inicia vacío si es botón global
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

        JComboBox<MovementOption> modeCombo = new JComboBox<>(new MovementOption[]{
                new MovementOption(StockMovementType.ADD, "+ Sumar Entrada"),
                new MovementOption(StockMovementType.SUBTRACT, "- Registrar Salida"),
                new MovementOption(StockMovementType.SET, "= Conteo Exacto")
        });
        for (int i = 0; i < modeCombo.getItemCount(); i++) {
            if (modeCombo.getItemAt(i).type == initialMode) {
                modeCombo.setSelectedIndex(i);
                break;
            }
        }

        JTextField qtyField = new JTextField(12);
        qtyField.setBorder(CulinaryTheme.fieldBorder());
        JTextField priceField = new JTextField(12);
        priceField.setBorder(CulinaryTheme.fieldBorder());
        JTextField dateField = new JTextField(12);
        dateField.setBorder(CulinaryTheme.fieldBorder());
        dateField.setToolTipText("Opcional: YYYY-MM-DD");

        JLabel previewLabel = new JLabel("Selecciona un ingrediente y cantidad");
        previewLabel.setFont(CulinaryTheme.FONT_MONO_BOLD);

        JButton saveBtn = CulinaryTheme.createPrimaryButton("Aplicar Movimiento");
        JButton cancelBtn = CulinaryTheme.createSecondaryButton("Cancelar");
        dialog.getRootPane().setDefaultButton(saveBtn);

        Runnable updatePreview = () -> {
            IngredientChoice selected = (IngredientChoice) ingCombo.getSelectedItem();
            Ingredient ing = selected == null ? null : selected.ingredient;
            if (ing == null) {
                previewLabel.setText("⚠ Selecciona un ingrediente del catálogo.");
                previewLabel.setForeground(CulinaryTheme.TEXT_SECONDARY);
                dateField.setEnabled(false);
                saveBtn.setEnabled(false);
                return;
            }

            dateField.setEnabled(ing.isPerishable());
            String qtyTxt = qtyField.getText().trim();
            if (!InputRegexValidator.isValidPositiveDecimal(qtyTxt)) {
                previewLabel.setText(String.format(Locale.US, "Stock actual: %.2f %s -> Ingresa cantidad válida",
                        ing.getStockQuantity(), ing.getUnit().getSymbol()));
                previewLabel.setForeground(CulinaryTheme.TEXT_SECONDARY);
                saveBtn.setEnabled(false);
                return;
            }

            double qty = Double.parseDouble(qtyTxt);
            StockMovementType mode = ((MovementOption) modeCombo.getSelectedItem()).type;
            double next = switch (mode) {
                case ADD -> ing.getStockQuantity() + qty;
                case SUBTRACT -> ing.getStockQuantity() - qty;
                case SET -> qty;
            };

            String priceTxt = priceField.getText().trim();
            boolean priceOk = priceTxt.isEmpty() || InputRegexValidator.isValidPositiveDecimal(priceTxt);
            String dateTxt = dateField.getText().trim();
            boolean dateOk = !ing.isPerishable() || dateTxt.isEmpty() || InputRegexValidator.isValidIsoDate(dateTxt);

            if (next < 0) {
                previewLabel.setText(String.format(Locale.US,
                        "⚠ Error: %.2f -> %.2f %s (Stock negativo no permitido)",
                        ing.getStockQuantity(), next, ing.getUnit().getSymbol()));
                previewLabel.setForeground(CulinaryTheme.ALERT_RED);
                saveBtn.setEnabled(false);
            } else if (!priceOk) {
                previewLabel.setText("⚠ Precio opcional inválido (usa decimal positivo).");
                previewLabel.setForeground(CulinaryTheme.ALERT_RED);
                saveBtn.setEnabled(false);
            } else if (!dateOk) {
                previewLabel.setText("⚠ Fecha inválida (formato YYYY-MM-DD).");
                previewLabel.setForeground(CulinaryTheme.ALERT_RED);
                saveBtn.setEnabled(false);
            } else {
                previewLabel.setText(String.format(Locale.US, "Vista previa: %.2f -> %.2f %s",
                        ing.getStockQuantity(), next, ing.getUnit().getSymbol()));
                previewLabel.setForeground(CulinaryTheme.OK_GREEN);
                saveBtn.setEnabled(true);
            }
        };

        ingCombo.addActionListener(e -> {
            IngredientChoice ch = (IngredientChoice) ingCombo.getSelectedItem();
            if (ch != null && ch.ingredient != null && ch.ingredient.getExpirationDate() != null) {
                dateField.setText(ch.ingredient.getExpirationDate().toString());
            } else {
                dateField.setText("");
            }
            updatePreview.run();
        });
        modeCombo.addActionListener(e -> updatePreview.run());
        qtyField.getDocument().addDocumentListener(new SimpleDoc(updatePreview));
        priceField.getDocument().addDocumentListener(new SimpleDoc(updatePreview));
        dateField.getDocument().addDocumentListener(new SimpleDoc(updatePreview));

        int r = 0;
        addFormRow(form, gbc, r++, "Ingrediente del Catálogo *:", ingCombo);
        addFormRow(form, gbc, r++, "Tipo de Operación *:", modeCombo);
        addFormRow(form, gbc, r++, "Cantidad *:", qtyField);
        addFormRow(form, gbc, r++, "Nuevo Precio USD (opcional):", priceField);
        addFormRow(form, gbc, r++, "Caducidad YYYY-MM-DD (opcional):", dateField);
        gbc.gridx = 1;
        gbc.gridy = r;
        form.add(previewLabel, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBackground(CulinaryTheme.BG_MAIN);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CulinaryTheme.BORDER_FLOUR));
        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                IngredientChoice ch = (IngredientChoice) ingCombo.getSelectedItem();
                StockMovementType mode = ((MovementOption) modeCombo.getSelectedItem()).type;
                double qty = Double.parseDouble(qtyField.getText().trim());
                String pTxt = priceField.getText().trim();
                Double newPrice = pTxt.isEmpty() ? null : Double.parseDouble(pTxt);
                String dTxt = dateField.getText().trim();
                LocalDate expDate = (ch.ingredient.isPerishable() && !dTxt.isEmpty())
                        ? LocalDate.parse(dTxt) : null;

                inventoryService.applyMovement(ch.ingredient.getId(), mode, qty, newPrice, expDate);
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
        updatePreview.run();
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, java.awt.Component comp) {
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

    private record MovementOption(StockMovementType type, String label) {
        @Override
        public String toString() {
            return label;
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
