package com.culinaryops.presentation.ui;

import com.culinaryops.application.CatalogService;
import com.culinaryops.application.InventoryService;
import com.culinaryops.domain.exception.CulinaryDomainException;
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.enums.StockMovementType;
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.service.CurrencyFormatter;
import com.culinaryops.domain.service.IngredientValidator;
import com.culinaryops.domain.service.InputRegexValidator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Módulo 2: Catálogo de Ingredientes & Mermas (separado del stock físico).
 */
public class IngredientsPanel extends JPanel {

    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final Runnable onDataChanged;

    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Ingredient> displayedIngredients = List.of();

    public IngredientsPanel(
            CatalogService catalogService,
            InventoryService inventoryService,
            Runnable onDataChanged) {
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 12));
        setBackground(CulinaryTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Cabecera + Botón primario arriba a la derecha
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Catálogo de Ingredientes & Mermas");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JButton newBtn = CulinaryTheme.createPrimaryButton("+ Nuevo Ingrediente");
        newBtn.addActionListener(e -> openIngredientDialog(null));
        header.add(title, BorderLayout.WEST);
        header.add(newBtn, BorderLayout.EAST);

        // Filtros
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setOpaque(false);
        searchField.setBorder(CulinaryTheme.fieldBorder());
        searchField.setToolTipText("Buscar por nombre o categoría");
        searchField.getDocument().addDocumentListener(new SimpleDocListener(this::applyFilter));
        categoryFilter.addActionListener(e -> applyFilter());

        filters.add(new JLabel("Buscar:"));
        filters.add(searchField);
        filters.add(new JLabel("Categoría:"));
        filters.add(categoryFilter);

        top.add(header, BorderLayout.NORTH);
        top.add(filters, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Tabla de ficha técnica
        tableModel = new DefaultTableModel(new String[]{
                "Ingrediente", "Categoría", "Unidad Base", "Compra (Precio / Cant.)",
                "Precio Unitario", "Merma Individual", "Costo Real Útil", "Conservación"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        CulinaryTheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        var cols = table.getColumnModel();
        cols.getColumn(0).setPreferredWidth(160);
        cols.getColumn(1).setPreferredWidth(140);
        cols.getColumn(2).setPreferredWidth(92);
        cols.getColumn(2).setMinWidth(88);
        cols.getColumn(2).setCellRenderer(CulinaryTheme.centerRenderer());
        cols.getColumn(3).setPreferredWidth(180);
        cols.getColumn(3).setMinWidth(170);
        cols.getColumn(4).setPreferredWidth(112);
        cols.getColumn(4).setCellRenderer(CulinaryTheme.centerMonoRenderer());
        cols.getColumn(5).setPreferredWidth(124);
        cols.getColumn(5).setCellRenderer(CulinaryTheme.centerRenderer());
        cols.getColumn(6).setPreferredWidth(112);
        cols.getColumn(6).setCellRenderer(CulinaryTheme.centerMonoRenderer());
        cols.getColumn(7).setPreferredWidth(110);
        cols.getColumn(7).setCellRenderer(CulinaryTheme.centerRenderer());
        add(CulinaryTheme.wrapTable(table), BorderLayout.CENTER);

        // Barra inferior de acciones sobre fila seleccionada
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton editBtn = CulinaryTheme.createSecondaryButton("Editar Seleccionado");
        editBtn.addActionListener(e -> editSelected());
        JButton deleteBtn = CulinaryTheme.createSecondaryButton("Eliminar Seleccionado");
        deleteBtn.setForeground(CulinaryTheme.ALERT_RED);
        deleteBtn.addActionListener(e -> deleteSelected());
        footer.add(editBtn);
        footer.add(deleteBtn);
        add(footer, BorderLayout.SOUTH);

        refreshData();
    }

    public final void refreshData() {
        String previousCat = (String) categoryFilter.getSelectedItem();
        categoryFilter.removeAllItems();
        categoryFilter.addItem("Todas las Categorías");
        List<String> cats = catalogService.getAvailableCategories();
        for (String cat : cats) {
            categoryFilter.addItem(cat);
        }
        if (previousCat != null) {
            categoryFilter.setSelectedItem(previousCat);
        }
        applyFilter();
    }

    private void applyFilter() {
        String cat = (String) categoryFilter.getSelectedItem();
        displayedIngredients = catalogService.search(searchField.getText(), cat);
        tableModel.setRowCount(0);
        for (Ingredient ing : displayedIngredients) {
            String purchaseSummary = CurrencyFormatter.formatUSD(ing.getPurchasePrice())
                    + " / " + formatPlainNumber(ing.getPurchaseQuantity()) + " " + ing.getUnit().getSymbol();
            tableModel.addRow(new Object[]{
                    ing.getName(),
                    ing.getCategory(),
                    ing.getUnit().getSymbol(),
                    purchaseSummary,
                    CurrencyFormatter.formatUSD(ing.getUnitPrice()),
                    CulinaryTheme.formatWasteBadge(ing),
                    CurrencyFormatter.formatUSD(ing.getEffectiveUnitPrice()),
                    ing.isPerishable() ? "Perecedero" : "Estable / Seco"
            });
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayedIngredients.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona un ingrediente de la tabla para editar.",
                    "Selección requerida", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openIngredientDialog(displayedIngredients.get(row));
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayedIngredients.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona un ingrediente de la tabla para eliminar.",
                    "Selección requerida", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Ingredient target = displayedIngredients.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmas eliminar '" + target.getName() + "' del catálogo?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                catalogService.deleteIngredient(target.getId());
                refreshData();
                onDataChanged.run();
            } catch (CulinaryDomainException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void openNewDialog() {
        openIngredientDialog(null);
    }

    private void openIngredientDialog(Ingredient editing) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, editing == null ? "Nuevo Ingrediente" : "Editar Ingrediente", true);
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

        JTextField nameField = new JTextField(editing == null ? "" : editing.getName(), 24);
        nameField.setBorder(CulinaryTheme.fieldBorder());
        JLabel nameFeedback = new JLabel(" ");
        nameFeedback.setFont(CulinaryTheme.FONT_SMALL);

        JComboBox<String> categoryBox = new JComboBox<>(
                catalogService.getAvailableCategories().toArray(new String[0]));
        if (editing != null) {
            categoryBox.setSelectedItem(editing.getCategory());
        }

        JComboBox<UnitType> unitBox = new JComboBox<>(new UnitType[]{UnitType.Gr, UnitType.Lt, UnitType.Und});
        if (editing != null) {
            unitBox.setSelectedItem(editing.getUnit());
        }

        JTextField priceField = new JTextField(
                editing == null ? "" : formatPlainNumber(editing.getPurchasePrice()), 12);
        priceField.setBorder(CulinaryTheme.fieldBorder());
        JLabel priceFeedback = new JLabel(" ");
        priceFeedback.setFont(CulinaryTheme.FONT_SMALL);

        JTextField purchaseQtyField = new JTextField(
                editing == null ? "" : formatPlainNumber(editing.getPurchaseQuantity()), 12);
        purchaseQtyField.setBorder(CulinaryTheme.fieldBorder());
        JLabel purchaseQtyFeedback = new JLabel(" ");
        purchaseQtyFeedback.setFont(CulinaryTheme.FONT_SMALL);

        // Slider 0.0% a 50.0% con paso 0.5% (representado 0..100 en enteros)
        int initialSteps = editing == null ? 0 : (int) Math.round(Math.min(50.0, editing.getWastePercentage()) * 2);
        JSlider wasteSlider = CulinaryTheme.createStyledSlider(0, 100, initialSteps);
        JLabel wasteValueLabel = new JLabel();
        wasteValueLabel.setFont(CulinaryTheme.FONT_MONO_BOLD);
        JLabel unitPricePreview = new JLabel();
        unitPricePreview.setFont(CulinaryTheme.FONT_MONO_BOLD);
        unitPricePreview.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JLabel effectivePreview = new JLabel();
        effectivePreview.setFont(CulinaryTheme.FONT_MONO_BOLD);
        effectivePreview.setForeground(CulinaryTheme.ACCENT_APRICOT);

        JCheckBox perishableCheck = new JCheckBox("Es un ingrediente perecedero",
                editing != null && editing.isPerishable());
        perishableCheck.setBackground(CulinaryTheme.BG_CARD);

        JLabel expDateRowLabel = new JLabel("Fecha Vencimiento (YYYY-MM-DD):");
        expDateRowLabel.setFont(CulinaryTheme.FONT_BODY_BOLD);
        expDateRowLabel.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JTextField expDateField = new JTextField(
                (editing != null && editing.getExpirationDate() != null)
                        ? editing.getExpirationDate().toString() : "", 14);
        expDateField.setBorder(CulinaryTheme.fieldBorder());
        expDateField.setToolTipText("Opcional: formato YYYY-MM-DD (ej. 2026-12-15)");
        JLabel expDateFeedback = new JLabel(" ");
        expDateFeedback.setFont(CulinaryTheme.FONT_SMALL);

        JButton saveBtn = CulinaryTheme.createPrimaryButton("Guardar");
        JButton cancelBtn = CulinaryTheme.createSecondaryButton("Cancelar");
        dialog.getRootPane().setDefaultButton(saveBtn);

        Runnable updateLiveState = () -> {
            String rawName = nameField.getText();
            boolean validName = InputRegexValidator.isValidName(rawName);
            Optional<Ingredient> dup = IngredientValidator.findDuplicateIngredient(
                    rawName, catalogService.findAll(), editing == null ? null : editing.getId());

            double wastePct = wasteSlider.getValue() / 2.0;
            wasteValueLabel.setText(String.format(Locale.US, "%.1f%%", wastePct));

            UnitType selectedUnit = (UnitType) unitBox.getSelectedItem();
            String unitSym = selectedUnit != null ? selectedUnit.getSymbol() : "g";

            String priceTxt = priceField.getText().trim();
            boolean validPrice = InputRegexValidator.isValidPositiveDecimal(priceTxt);
            if (priceTxt.isEmpty()) {
                priceFeedback.setText("Campo obligatorio (ej. 3.20 por paquete/lote).");
                priceFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
            } else if (!validPrice) {
                priceFeedback.setText("⚠ Precio inválido. Usa decimal positivo hasta 4 decimales (ej. 3.20).");
                priceFeedback.setForeground(CulinaryTheme.ALERT_RED);
            } else {
                priceFeedback.setText("✓ Precio de compra válido");
                priceFeedback.setForeground(CulinaryTheme.OK_GREEN);
            }

            String purchaseQtyTxt = purchaseQtyField.getText().trim();
            boolean validPurchaseQty = InputRegexValidator.isValidPositiveDecimal(purchaseQtyTxt)
                    && Double.parseDouble(purchaseQtyTxt) > 0;
            if (purchaseQtyTxt.isEmpty()) {
                purchaseQtyFeedback.setText("Campo obligatorio (ej. 1000 para 1 kg, 500, 1).");
                purchaseQtyFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
            } else if (!validPurchaseQty) {
                purchaseQtyFeedback.setText("⚠ Cantidad inválida. Usa número positivo > 0 (ej. 1000).");
                purchaseQtyFeedback.setForeground(CulinaryTheme.ALERT_RED);
            } else {
                purchaseQtyFeedback.setText("✓ Cantidad válida (" + purchaseQtyTxt + " " + unitSym + ")");
                purchaseQtyFeedback.setForeground(CulinaryTheme.OK_GREEN);
            }

            if (validPrice && validPurchaseQty) {
                double p = Double.parseDouble(priceTxt);
                double q = Double.parseDouble(purchaseQtyTxt);
                double unitPrice = p / q;
                double eff = unitPrice / (1.0 - wastePct / 100.0);
                unitPricePreview.setText("Precio Unitario Base: " + CurrencyFormatter.formatUSD(unitPrice)
                        + "/" + unitSym + " (compra / cantidad)");
                effectivePreview.setText("Costo Real Útil (con merma): " + CurrencyFormatter.formatUSD(eff)
                        + "/" + unitSym);
            } else {
                unitPricePreview.setText("Precio Unitario Base: ingresa precio y cantidad > 0");
                effectivePreview.setText("Costo Real Útil (con merma): pendiente de datos válidos");
            }

            boolean perishable = perishableCheck.isSelected();
            expDateRowLabel.setVisible(perishable);
            expDateField.setVisible(perishable);
            expDateFeedback.setVisible(perishable);

            boolean validDate = true;
            if (perishable) {
                String dateTxt = expDateField.getText().trim();
                if (dateTxt.isEmpty()) {
                    expDateFeedback.setText("Opcional (formato YYYY-MM-DD, o asígnala luego en Inventario).");
                    expDateFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
                } else if (!isValidCalendarIsoDate(dateTxt)) {
                    expDateFeedback.setText("⚠ Fecha inválida. Usa formato YYYY-MM-DD (ej. 2026-12-01).");
                    expDateFeedback.setForeground(CulinaryTheme.ALERT_RED);
                    validDate = false;
                } else {
                    expDateFeedback.setText("✓ Fecha válida");
                    expDateFeedback.setForeground(CulinaryTheme.OK_GREEN);
                }
            }

            boolean nameOk = false;
            if (rawName.trim().isEmpty()) {
                nameFeedback.setText("Campo obligatorio.");
                nameFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
            } else if (!validName) {
                nameFeedback.setText("⚠ Nombre inválido (mín. 2 caracteres válidos).");
                nameFeedback.setForeground(CulinaryTheme.ALERT_RED);
            } else if (dup.isPresent()) {
                nameFeedback.setText("⚠ Ya existe '" + dup.get().getName() + "'. Suma stock en Inventario.");
                nameFeedback.setForeground(CulinaryTheme.ALERT_RED);
            } else {
                nameFeedback.setText("✓ Nombre disponible");
                nameFeedback.setForeground(CulinaryTheme.OK_GREEN);
                nameOk = true;
            }
            saveBtn.setEnabled(nameOk && validPrice && validPurchaseQty && validDate);
        };

        nameField.getDocument().addDocumentListener(new SimpleDocListener(updateLiveState));
        priceField.getDocument().addDocumentListener(new SimpleDocListener(updateLiveState));
        purchaseQtyField.getDocument().addDocumentListener(new SimpleDocListener(updateLiveState));
        expDateField.getDocument().addDocumentListener(new SimpleDocListener(updateLiveState));
        wasteSlider.addChangeListener(e -> updateLiveState.run());
        unitBox.addActionListener(e -> updateLiveState.run());
        perishableCheck.addActionListener(e -> {
            updateLiveState.run();
            dialog.pack();
        });

        int row = 0;
        addRow(form, gbc, row++, "Nombre del Ingrediente *:", nameField);
        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(nameFeedback, gbc);
        addRow(form, gbc, row++, "Categoría *:", categoryBox);
        addRow(form, gbc, row++, "Unidad Base:", unitBox);
        addRow(form, gbc, row++, "Precio de Compra (USD) *:", priceField);
        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(priceFeedback, gbc);
        addRow(form, gbc, row++, "Cantidad por Compra *:", purchaseQtyField);
        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(purchaseQtyFeedback, gbc);

        JPanel sliderBox = new JPanel(new BorderLayout(8, 0));
        sliderBox.setOpaque(false);
        sliderBox.add(wasteSlider, BorderLayout.CENTER);
        sliderBox.add(wasteValueLabel, BorderLayout.EAST);
        addRow(form, gbc, row++, "Porcentaje de Merma:", sliderBox);

        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(unitPricePreview, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(effectivePreview, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(perishableCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        form.add(expDateRowLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(expDateField, gbc);
        row++;

        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(expDateFeedback, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBackground(CulinaryTheme.BG_MAIN);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CulinaryTheme.BORDER_FLOUR));
        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                String name = nameField.getText();
                String cat = (String) categoryBox.getSelectedItem();
                UnitType unit = (UnitType) unitBox.getSelectedItem();
                double purchasePrice = Double.parseDouble(priceField.getText().trim());
                double purchaseQty = Double.parseDouble(purchaseQtyField.getText().trim());
                double waste = wasteSlider.getValue() / 2.0;
                boolean perishable = perishableCheck.isSelected();
                String dateTxt = expDateField.getText().trim();
                LocalDate expDate = (perishable && !dateTxt.isEmpty()) ? LocalDate.parse(dateTxt) : null;

                Ingredient saved;
                if (editing == null) {
                    saved = catalogService.createIngredient(name, cat, unit, purchasePrice, purchaseQty, waste, perishable);
                } else {
                    saved = catalogService.updateIngredient(
                            editing.getId(), name, cat, unit, purchasePrice, purchaseQty, waste, perishable);
                }
                if (expDate != null) {
                    inventoryService.applyMovement(saved.getId(), StockMovementType.ADD, 0.0, null, expDate);
                }
                dialog.dispose();
                refreshData();
                onDataChanged.run();
            } catch (CulinaryDomainException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error de validación", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttons.add(cancelBtn);
        buttons.add(saveBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        updateLiveState.run();
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static String formatPlainNumber(double value) {
        if (value == 0.0) {
            return "0";
        }
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
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

    private static void addRow(JPanel form, GridBagConstraints gbc, int row, String label, java.awt.Component comp) {
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

    private static class SimpleDocListener implements DocumentListener {
        private final Runnable callback;

        SimpleDocListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void insertUpdate(DocumentEvent e) { callback.run(); }

        @Override
        public void removeUpdate(DocumentEvent e) { callback.run(); }

        @Override
        public void changedUpdate(DocumentEvent e) { callback.run(); }
    }
}
