package com.culinaryops.presentation.ui;

import com.culinaryops.application.CatalogService;
import com.culinaryops.domain.exception.CulinaryDomainException;
import com.culinaryops.domain.model.Ingredient;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
    private final Runnable onDataChanged;
    private final Runnable onGoToInventory;

    private final JLabel bannerStats = new JLabel();
    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Ingredient> displayedIngredients = List.of();

    public IngredientsPanel(
            CatalogService catalogService,
            Runnable onDataChanged,
            Runnable onGoToInventory) {
        this.catalogService = catalogService;
        this.onDataChanged = onDataChanged;
        this.onGoToInventory = onGoToInventory;

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

        // Banner explicativo + enlace a Inventario
        JPanel banner = CulinaryTheme.createCardPanel();
        banner.setLayout(new BorderLayout(12, 0));
        JPanel bannerText = new JPanel();
        bannerText.setLayout(new BoxLayout(bannerText, BoxLayout.Y_AXIS));
        bannerText.setOpaque(false);
        JLabel ruleNote = new JLabel(
                "Regla de separación: aquí defines la ficha técnica y merma individual. "
                + "Para sumar existencias o fechas de lote, registra movimientos en Inventario.");
        ruleNote.setFont(CulinaryTheme.FONT_BODY);
        ruleNote.setForeground(CulinaryTheme.TEXT_ACCESSIBLE);
        bannerStats.setFont(CulinaryTheme.FONT_SMALL.deriveFont(java.awt.Font.BOLD));
        bannerStats.setForeground(CulinaryTheme.TEXT_SECONDARY);
        bannerText.add(ruleNote);
        bannerText.add(bannerStats);
        JButton goStockBtn = CulinaryTheme.createSecondaryButton("Ir a Stock ->");
        goStockBtn.addActionListener(e -> onGoToInventory.run());
        banner.add(bannerText, BorderLayout.CENTER);
        banner.add(goStockBtn, BorderLayout.EAST);

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
        top.add(banner, BorderLayout.CENTER);
        top.add(filters, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Tabla de ficha técnica
        tableModel = new DefaultTableModel(new String[]{
                "Ingrediente", "Categoría", "Unidad Base", "Precio Compra",
                "Merma Individual", "Costo Real Útil", "Conservación"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        CulinaryTheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(3).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(CulinaryTheme.rightMonoRenderer());
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
        bannerStats.setText("Ingredientes registrados: " + catalogService.findAll().size()
                + "   |   Categorías activas: " + cats.size());
        applyFilter();
    }

    private void applyFilter() {
        String cat = (String) categoryFilter.getSelectedItem();
        displayedIngredients = catalogService.search(searchField.getText(), cat);
        tableModel.setRowCount(0);
        for (Ingredient ing : displayedIngredients) {
            tableModel.addRow(new Object[]{
                    ing.getName(),
                    ing.getCategory(),
                    ing.getUnit().getSymbol(),
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

        JComboBox<UnitType> unitBox = new JComboBox<>(new UnitType[]{UnitType.G, UnitType.L, UnitType.U});
        if (editing != null) {
            unitBox.setSelectedItem(editing.getUnit());
        }

        JTextField priceField = new JTextField(
                editing == null ? "" : String.valueOf(editing.getUnitPrice()), 12);
        priceField.setBorder(CulinaryTheme.fieldBorder());

        // Slider 0.0% a 50.0% con paso 0.5% (representado 0..100 en enteros)
        int initialSteps = editing == null ? 0 : (int) Math.round(Math.min(50.0, editing.getWastePercentage()) * 2);
        JSlider wasteSlider = new JSlider(0, 100, initialSteps);
        JLabel wasteValueLabel = new JLabel();
        JLabel effectivePreview = new JLabel();
        effectivePreview.setFont(CulinaryTheme.FONT_MONO_BOLD);
        effectivePreview.setForeground(CulinaryTheme.ACCENT_APRICOT);

        JCheckBox perishableCheck = new JCheckBox("Es un ingrediente perecedero",
                editing != null && editing.isPerishable());
        perishableCheck.setBackground(CulinaryTheme.BG_CARD);

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

            String priceTxt = priceField.getText().trim();
            boolean validPrice = InputRegexValidator.isValidPositiveDecimal(priceTxt);
            if (validPrice) {
                double p = Double.parseDouble(priceTxt);
                double eff = p / (1.0 - wastePct / 100.0);
                UnitType u = (UnitType) unitBox.getSelectedItem();
                effectivePreview.setText("Costo Real Útil: " + CurrencyFormatter.formatUSD(eff)
                        + "/" + (u != null ? u.getSymbol() : "g"));
            } else {
                effectivePreview.setText("Costo Real Útil: ingresa precio válido (ej. 0.0032)");
            }

            if (rawName.trim().isEmpty()) {
                nameFeedback.setText("Campo obligatorio.");
                nameFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
                saveBtn.setEnabled(false);
            } else if (!validName) {
                nameFeedback.setText("⚠ Nombre inválido (mín. 2 caracteres válidos).");
                nameFeedback.setForeground(CulinaryTheme.ALERT_RED);
                saveBtn.setEnabled(false);
            } else if (dup.isPresent()) {
                nameFeedback.setText("⚠ Ya existe '" + dup.get().getName() + "'. Suma stock en Inventario.");
                nameFeedback.setForeground(CulinaryTheme.ALERT_RED);
                saveBtn.setEnabled(false);
            } else {
                nameFeedback.setText("✓ Nombre disponible");
                nameFeedback.setForeground(CulinaryTheme.OK_GREEN);
                saveBtn.setEnabled(validPrice);
            }
        };

        nameField.getDocument().addDocumentListener(new SimpleDocListener(updateLiveState));
        priceField.getDocument().addDocumentListener(new SimpleDocListener(updateLiveState));
        wasteSlider.addChangeListener(e -> updateLiveState.run());
        unitBox.addActionListener(e -> updateLiveState.run());

        int row = 0;
        addRow(form, gbc, row++, "Nombre del Ingrediente *:", nameField);
        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(nameFeedback, gbc);
        addRow(form, gbc, row++, "Categoría *:", categoryBox);
        addRow(form, gbc, row++, "Unidad Base:", unitBox);
        addRow(form, gbc, row++, "Precio Compra (USD) *:", priceField);

        JPanel sliderBox = new JPanel(new BorderLayout(8, 0));
        sliderBox.setOpaque(false);
        sliderBox.add(wasteSlider, BorderLayout.CENTER);
        sliderBox.add(wasteValueLabel, BorderLayout.EAST);
        addRow(form, gbc, row++, "Porcentaje de Merma:", sliderBox);

        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(effectivePreview, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(perishableCheck, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBackground(CulinaryTheme.BG_MAIN);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CulinaryTheme.BORDER_FLOUR));
        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                String name = nameField.getText();
                String cat = (String) categoryBox.getSelectedItem();
                UnitType unit = (UnitType) unitBox.getSelectedItem();
                double price = Double.parseDouble(priceField.getText().trim());
                double waste = wasteSlider.getValue() / 2.0;
                boolean perishable = perishableCheck.isSelected();

                if (editing == null) {
                    catalogService.createIngredient(name, cat, unit, price, waste, perishable);
                } else {
                    catalogService.updateIngredient(editing.getId(), name, cat, unit, price, waste, perishable);
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
