package com.culinaryops.presentation.ui;

import com.culinaryops.application.CatalogService;
import com.culinaryops.application.RecipeService;
import com.culinaryops.domain.exception.CulinaryDomainException;
import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.RecipeIngredient;
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.service.CurrencyFormatter;
import com.culinaryops.domain.service.IngredientValidator;
import com.culinaryops.domain.service.InputRegexValidator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Módulo 4: Recetario & Escandallos (fórmulas estandarizadas y costeo directo).
 */
public class RecipesPanel extends JPanel {

    private final RecipeService recipeService;
    private final CatalogService catalogService;
    private final Runnable onDataChanged;

    private final JTextField searchField = new JTextField(20);
    private final DefaultTableModel recipesModel;
    private final JTable recipesTable;
    private final DefaultTableModel detailModel;
    private final JLabel detailHeaderLabel = new JLabel("Selecciona una receta para ver su escandallo");
    private final JTextArea instructionsArea = new JTextArea(4, 40);
    private List<Recipe> displayed = List.of();

    public RecipesPanel(
            RecipeService recipeService,
            CatalogService catalogService,
            Runnable onDataChanged) {
        this.recipeService = recipeService;
        this.catalogService = catalogService;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 12));
        setBackground(CulinaryTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Cabecera + búsqueda
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Recetas");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JButton newBtn = CulinaryTheme.createPrimaryButton("+ Nueva Receta");
        newBtn.addActionListener(e -> openRecipeDialog(null));
        header.add(title, BorderLayout.WEST);
        header.add(newBtn, BorderLayout.EAST);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        searchField.setBorder(CulinaryTheme.fieldBorder());
        searchField.getDocument().addDocumentListener(new DocListener(this::applyFilter));
        filterRow.add(new JLabel("Buscar receta:"));
        filterRow.add(searchField);
        top.add(header, BorderLayout.NORTH);
        top.add(filterRow, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Tabla superior de recetas
        recipesModel = new DefaultTableModel(new String[]{
                "Receta", "Rendimiento", "Prep (min)", "Ingredientes", "Costo Tanda", "Costo / Unidad"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        recipesTable = new JTable(recipesModel);
        CulinaryTheme.styleTable(recipesTable);
        recipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recipesTable.getColumnModel().getColumn(2).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        recipesTable.getColumnModel().getColumn(4).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        recipesTable.getColumnModel().getColumn(5).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        recipesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateEscandalloDetail();
            }
        });

        // Panel inferior: Escandallo detallado
        JPanel detailCard = CulinaryTheme.createCardPanel();
        detailCard.setLayout(new BorderLayout(0, 8));
        detailHeaderLabel.setFont(CulinaryTheme.FONT_SUBTITLE);
        detailHeaderLabel.setForeground(CulinaryTheme.ACCENT_APRICOT);
        detailCard.add(detailHeaderLabel, BorderLayout.NORTH);

        detailModel = new DefaultTableModel(new String[]{
                "Ingrediente", "Cantidad Neta", "Merma", "Bruto Requerido", "Precio Compra", "Subtotal"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable detailTable = new JTable(detailModel);
        CulinaryTheme.styleTable(detailTable);
        detailTable.getColumnModel().getColumn(1).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        detailTable.getColumnModel().getColumn(2).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        detailTable.getColumnModel().getColumn(3).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        detailTable.getColumnModel().getColumn(4).setCellRenderer(CulinaryTheme.rightMonoRenderer());
        detailTable.getColumnModel().getColumn(5).setCellRenderer(CulinaryTheme.rightMonoRenderer());

        instructionsArea.setEditable(false);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setFont(CulinaryTheme.FONT_BODY);
        instructionsArea.setBackground(CulinaryTheme.BG_MAIN);
        instructionsArea.setBorder(BorderFactory.createTitledBorder("Preparación"));

        JPanel detailCenter = new JPanel(new BorderLayout(0, 8));
        detailCenter.setOpaque(false);
        detailCenter.add(CulinaryTheme.wrapTable(detailTable), BorderLayout.CENTER);
        detailCenter.add(new JScrollPane(instructionsArea), BorderLayout.SOUTH);
        detailCard.add(detailCenter, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                CulinaryTheme.wrapTable(recipesTable), detailCard);
        split.setResizeWeight(0.42);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);

        // Footer de acciones
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton editBtn = CulinaryTheme.createSecondaryButton("Editar Receta");
        editBtn.addActionListener(e -> editSelected());
        JButton deleteBtn = CulinaryTheme.createSecondaryButton("Eliminar Receta");
        deleteBtn.setForeground(CulinaryTheme.ALERT_RED);
        deleteBtn.addActionListener(e -> deleteSelected());
        footer.add(editBtn);
        footer.add(deleteBtn);
        add(footer, BorderLayout.SOUTH);

        refreshData();
    }

    public final void refreshData() {
        applyFilter();
    }

    private void applyFilter() {
        displayed = recipeService.search(searchField.getText());
        recipesModel.setRowCount(0);
        for (Recipe r : displayed) {
            recipesModel.addRow(new Object[]{
                    r.getName(),
                    String.format(Locale.US, "%.0f %s", r.getYieldQuantity(), r.getYieldUnit()),
                    r.getPrepTimeMinutes(),
                    r.getIngredients().size(),
                    CurrencyFormatter.formatUSD(r.calculateDirectCost()),
                    CurrencyFormatter.formatUSD(r.calculateCostPerUnit())
            });
        }
        if (!displayed.isEmpty() && recipesTable.getSelectedRow() < 0) {
            recipesTable.setRowSelectionInterval(0, 0);
        }
        updateEscandalloDetail();
    }

    private void updateEscandalloDetail() {
        int row = recipesTable.getSelectedRow();
        detailModel.setRowCount(0);
        if (row < 0 || row >= displayed.size()) {
            detailHeaderLabel.setText("Selecciona una receta para ver su escandallo");
            instructionsArea.setText("");
            return;
        }
        Recipe r = displayed.get(row);
        detailHeaderLabel.setText(String.format(Locale.US,
                "Escandallo: %s | Total: %s | Costo/%s: %s | Masa neta: %.0f g",
                r.getName(),
                CurrencyFormatter.formatUSD(r.calculateDirectCost()),
                r.getYieldUnit(),
                CurrencyFormatter.formatUSD(r.calculateCostPerUnit()),
                r.calculateTotalNetGrams()));

        for (RecipeIngredient item : r.getIngredients()) {
            Ingredient ing = item.getIngredient();
            detailModel.addRow(new Object[]{
                    ing.getName(),
                    String.format(Locale.US, "%.2f %s", item.getNetQuantity(), item.getUnit().getSymbol()),
                    String.format(Locale.US, "%.1f%%", ing.getWastePercentage()),
                    String.format(Locale.US, "%.2f %s", item.getRawQuantityRequired(), item.getUnit().getSymbol()),
                    CurrencyFormatter.formatUSD(ing.getUnitPrice()) + "/" + ing.getUnit().getSymbol(),
                    CurrencyFormatter.formatUSD(item.getSubtotalCost())
            });
        }
        instructionsArea.setText(r.getInstructions().isEmpty() ? "(Sin preparación registrada)" : r.getInstructions());
    }

    private void editSelected() {
        int row = recipesTable.getSelectedRow();
        if (row < 0 || row >= displayed.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona una receta para editar.",
                    "Selección requerida", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openRecipeDialog(displayed.get(row));
    }

    private void deleteSelected() {
        int row = recipesTable.getSelectedRow();
        if (row < 0 || row >= displayed.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona una receta para eliminar.",
                    "Selección requerida", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Recipe r = displayed.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Confirmas eliminar la receta '" + r.getName() + "'?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            recipeService.deleteRecipe(r.getId());
            refreshData();
            onDataChanged.run();
        }
    }

    public void openNewDialog() {
        openRecipeDialog(null);
    }

    private void openRecipeDialog(Recipe editing) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, editing == null ? "Nueva Receta" : "Editar Receta", true);
        CulinaryTheme.installEscapeClose(dialog);
        dialog.getContentPane().setBackground(CulinaryTheme.BG_CARD);
        dialog.setLayout(new BorderLayout(0, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CulinaryTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(10, 18, 4, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 6, 2, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameField = new JTextField(editing == null ? "" : editing.getName(), 26);
        nameField.setBorder(CulinaryTheme.fieldBorder());
        JLabel nameFeedback = new JLabel(" ");
        nameFeedback.setFont(CulinaryTheme.FONT_SMALL);

        JTextField descField = new JTextField(editing == null ? "" : editing.getDescription(), 26);
        descField.setBorder(CulinaryTheme.fieldBorder());

        JTextField yieldQtyField = new JTextField(editing == null ? "" : String.valueOf(editing.getYieldQuantity()), 8);
        yieldQtyField.setBorder(CulinaryTheme.fieldBorder());
        JLabel yieldFeedback = new JLabel(" ");
        yieldFeedback.setFont(CulinaryTheme.FONT_SMALL);

        JTextField yieldUnitField = new JTextField(editing == null ? "porciones" : editing.getYieldUnit(), 12);
        yieldUnitField.setBorder(CulinaryTheme.fieldBorder());
        JLabel yieldUnitFeedback = new JLabel(" ");
        yieldUnitFeedback.setFont(CulinaryTheme.FONT_SMALL);

        JTextField prepMinField = new JTextField(editing == null ? "45" : String.valueOf(editing.getPrepTimeMinutes()), 6);
        prepMinField.setBorder(CulinaryTheme.fieldBorder());
        JLabel prepFeedback = new JLabel(" ");
        prepFeedback.setFont(CulinaryTheme.FONT_SMALL);

        JTextArea instrArea = new JTextArea(editing == null ? "" : editing.getInstructions(), 2, 26);
        instrArea.setFont(CulinaryTheme.FONT_BODY);
        instrArea.setLineWrap(true);
        instrArea.setWrapStyleWord(true);
        instrArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane instrScroll = new JScrollPane(instrArea);
        instrScroll.setBorder(BorderFactory.createLineBorder(CulinaryTheme.BORDER_FLOUR, 1));

        int row = 0;
        addRow(form, gbc, row++, "Nombre de la Receta *:", nameField);
        addFeedbackRow(form, gbc, row++, nameFeedback);
        addRow(form, gbc, row++, "Descripción:", descField);
        addRow(form, gbc, row++, "Rendimiento *:", yieldQtyField);
        addFeedbackRow(form, gbc, row++, yieldFeedback);
        addRow(form, gbc, row++, "Unidad Rendimiento:", yieldUnitField);
        addFeedbackRow(form, gbc, row++, yieldUnitFeedback);
        addRow(form, gbc, row++, "Prep (Minutos):", prepMinField);
        addFeedbackRow(form, gbc, row++, prepFeedback);
        addRow(form, gbc, row++, "Preparación:", instrScroll);

        // Filas dinámicas de ingredientes
        JPanel rowsContainer = new JPanel();
        rowsContainer.setLayout(new BoxLayout(rowsContainer, BoxLayout.Y_AXIS));
        rowsContainer.setBackground(CulinaryTheme.BG_CARD);
        List<IngredientRow> ingredientRows = new ArrayList<>();

        JLabel liveSummary = new JLabel("Costo Directo Calculado: $0.00 USD");
        liveSummary.setFont(CulinaryTheme.FONT_MONO_BOLD);
        liveSummary.setForeground(CulinaryTheme.ACCENT_APRICOT);
        JLabel validationLabel = new JLabel(" ");
        validationLabel.setFont(CulinaryTheme.FONT_SMALL);
        JButton saveBtn = CulinaryTheme.createPrimaryButton("Guardar Receta");
        dialog.getRootPane().setDefaultButton(saveBtn);

        List<Ingredient> catalog = catalogService.findAll().stream()
                .sorted(Comparator.comparing(Ingredient::getCategory).thenComparing(Ingredient::getName))
                .toList();

        Runnable[] recalcRef = new Runnable[1];
        recalcRef[0] = () -> {
            // 1. Validar Nombre de la Receta (formato + unicidad)
            String rawName = nameField.getText();
            boolean validNameFormat = InputRegexValidator.isValidName(rawName);
            String normalizedName = IngredientValidator.normalizeIngredientName(rawName);
            String duplicateRecipeName = null;
            if (validNameFormat && !normalizedName.isEmpty()) {
                for (Recipe existing : recipeService.findAll()) {
                    if (editing != null && existing.getId().equals(editing.getId())) {
                        continue;
                    }
                    if (IngredientValidator.normalizeIngredientName(existing.getName()).equals(normalizedName)) {
                        duplicateRecipeName = existing.getName();
                        break;
                    }
                }
            }
            boolean nameOk = false;
            if (rawName.trim().isEmpty()) {
                nameFeedback.setText("Campo obligatorio.");
                nameFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
            } else if (!validNameFormat) {
                nameFeedback.setText("⚠ Nombre inválido (mín. 2 caracteres válidos).");
                nameFeedback.setForeground(CulinaryTheme.ALERT_RED);
            } else if (duplicateRecipeName != null) {
                nameFeedback.setText("⚠ Ya existe una receta llamada '" + duplicateRecipeName + "'.");
                nameFeedback.setForeground(CulinaryTheme.ALERT_RED);
            } else {
                nameFeedback.setText("✓ Nombre disponible");
                nameFeedback.setForeground(CulinaryTheme.OK_GREEN);
                nameOk = true;
            }

            // 2. Validar Rendimiento
            String yieldTxt = yieldQtyField.getText().trim();
            boolean validYield = false;
            double yQty = 1.0;
            if (yieldTxt.isEmpty()) {
                yieldFeedback.setText("Campo obligatorio (ej. 8 o 12.5).");
                yieldFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
            } else if (!InputRegexValidator.isValidPositiveDecimal(yieldTxt) || Double.parseDouble(yieldTxt) <= 0) {
                yieldFeedback.setText("⚠ Rendimiento inválido. Usa decimal positivo mayor a 0 (ej. 8).");
                yieldFeedback.setForeground(CulinaryTheme.ALERT_RED);
            } else {
                yQty = Double.parseDouble(yieldTxt);
                yieldFeedback.setText("✓ Rendimiento válido");
                yieldFeedback.setForeground(CulinaryTheme.OK_GREEN);
                validYield = true;
            }

            // 3. Feedback Unidad de Rendimiento
            String yUnitTxt = yieldUnitField.getText().trim();
            boolean validYieldUnit = true;
            if (yUnitTxt.isEmpty()) {
                yieldUnitFeedback.setText("Opcional (si se deja vacío se usará 'porciones').");
                yieldUnitFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
            } else if (!InputRegexValidator.isValidName(yUnitTxt)) {
                yieldUnitFeedback.setText("⚠ Unidad inválida (mín. 2 caracteres válidos, ej. porciones).");
                yieldUnitFeedback.setForeground(CulinaryTheme.ALERT_RED);
                validYieldUnit = false;
            } else {
                yieldUnitFeedback.setText("✓ Unidad: " + yUnitTxt);
                yieldUnitFeedback.setForeground(CulinaryTheme.OK_GREEN);
            }

            // 4. Validar Prep (Minutos)
            String prepTxt = prepMinField.getText().trim();
            boolean validPrep = false;
            if (prepTxt.isEmpty()) {
                prepFeedback.setText("Opcional (si se deja vacío se guardará 0 min).");
                prepFeedback.setForeground(CulinaryTheme.TEXT_SECONDARY);
                validPrep = true;
            } else if (!prepTxt.matches("^\\d+$")) {
                prepFeedback.setText("⚠ Tiempo inválido. Ingresa solo números enteros >= 0 (ej. 45).");
                prepFeedback.setForeground(CulinaryTheme.ALERT_RED);
            } else {
                prepFeedback.setText("✓ Tiempo válido (" + prepTxt + " min)");
                prepFeedback.setForeground(CulinaryTheme.OK_GREEN);
                validPrep = true;
            }

            // 5. Validar filas de ingredientes y costo directo
            Set<String> seenIds = new HashSet<>();
            boolean hasDuplicate = false;
            boolean hasInvalidRowQty = false;
            boolean hasMissingRow = ingredientRows.isEmpty();
            double totalCost = 0.0;

            for (IngredientRow rowItem : ingredientRows) {
                IngredientItem sel = (IngredientItem) rowItem.combo.getSelectedItem();
                Ingredient ing = sel == null ? null : sel.ingredient;
                String qtyTxt = rowItem.qtyField.getText().trim();
                UnitType unit = (UnitType) rowItem.unitCombo.getSelectedItem();

                if (ing == null) {
                    hasMissingRow = true;
                    rowItem.setRowStatus("Selecciona un ingrediente del catálogo.", CulinaryTheme.TEXT_SECONDARY, false);
                    continue;
                }
                if (qtyTxt.isEmpty()) {
                    hasMissingRow = true;
                    rowItem.setRowStatus("Ingresa cantidad > 0 (ej. 150).", CulinaryTheme.TEXT_SECONDARY, false);
                    continue;
                }
                if (!InputRegexValidator.isValidPositiveDecimal(qtyTxt) || Double.parseDouble(qtyTxt) <= 0) {
                    hasInvalidRowQty = true;
                    rowItem.setRowStatus("⚠ Cantidad inválida. Usa decimal > 0 (ej. 150).", CulinaryTheme.ALERT_RED, true);
                    continue;
                }
                double q = Double.parseDouble(qtyTxt);
                if (!seenIds.add(ing.getId())) {
                    hasDuplicate = true;
                    rowItem.setRowStatus("⚠ Ingrediente repetido en esta receta.", CulinaryTheme.ALERT_RED, false);
                    continue;
                }
                double sub = new RecipeIngredient(ing, unit, q).getSubtotalCost();
                totalCost += sub;
                rowItem.setRowStatus("✓ Subtotal: " + CurrencyFormatter.formatUSD(sub), CulinaryTheme.OK_GREEN, false);
            }

            String unitLabel = yUnitTxt.isEmpty() ? "Unidad" : yUnitTxt;
            liveSummary.setText(String.format(Locale.US, "Costo Directo: %s | Costo/%s: %s",
                    CurrencyFormatter.formatUSD(totalCost),
                    unitLabel,
                    CurrencyFormatter.formatUSD(validYield ? totalCost / yQty : 0.0)));

            boolean allRowsOk = !hasMissingRow && !hasInvalidRowQty && !hasDuplicate;
            boolean formOk = nameOk && validYield && validYieldUnit && validPrep;

            if (hasDuplicate) {
                validationLabel.setText("⚠ Hay ingredientes repetidos en la receta (cada insumo una sola vez).");
                validationLabel.setForeground(CulinaryTheme.ALERT_RED);
            } else if (hasInvalidRowQty) {
                validationLabel.setText("⚠ Hay cantidades inválidas en los ingredientes.");
                validationLabel.setForeground(CulinaryTheme.ALERT_RED);
            } else if (!formOk && (!rawName.trim().isEmpty() || !yieldTxt.isEmpty() || !prepTxt.matches("^\\d*$"))) {
                validationLabel.setText("⚠ Corrige los campos marcados en rojo antes de guardar.");
                validationLabel.setForeground(CulinaryTheme.ALERT_RED);
            } else if (!formOk || hasMissingRow) {
                validationLabel.setText("Completa los campos obligatorios (*) y al menos 1 ingrediente con cantidad > 0.");
                validationLabel.setForeground(CulinaryTheme.TEXT_SECONDARY);
            } else {
                validationLabel.setText("✓ Receta lista para guardar");
                validationLabel.setForeground(CulinaryTheme.OK_GREEN);
            }
            saveBtn.setEnabled(formOk && allRowsOk);
        };

        Runnable addEmptyRow = () -> {
            IngredientRow r = new IngredientRow(catalog, null, UnitType.Gr, "", () -> {
                recalcRef[0].run();
            }, rowToRemove -> {
                if (ingredientRows.size() > 1) {
                    ingredientRows.remove(rowToRemove);
                    rowsContainer.remove(rowToRemove.panel);
                    rowsContainer.revalidate();
                    rowsContainer.repaint();
                    recalcRef[0].run();
                }
            });
            ingredientRows.add(r);
            rowsContainer.add(r.panel);
            rowsContainer.revalidate();
            rowsContainer.repaint();
            recalcRef[0].run();
        };

        if (editing == null) {
            addEmptyRow.run(); // Inicia con 1 fila vacía por regla UX
        } else {
            for (RecipeIngredient item : editing.getIngredients()) {
                IngredientRow r = new IngredientRow(catalog, item.getIngredient(), item.getUnit(),
                        String.valueOf(item.getNetQuantity()), () -> recalcRef[0].run(), rowToRemove -> {
                    if (ingredientRows.size() > 1) {
                        ingredientRows.remove(rowToRemove);
                        rowsContainer.remove(rowToRemove.panel);
                        rowsContainer.revalidate();
                        rowsContainer.repaint();
                        recalcRef[0].run();
                    }
                });
                ingredientRows.add(r);
                rowsContainer.add(r.panel);
            }
        }

        nameField.getDocument().addDocumentListener(new DocListener(recalcRef[0]));
        yieldQtyField.getDocument().addDocumentListener(new DocListener(recalcRef[0]));
        yieldUnitField.getDocument().addDocumentListener(new DocListener(recalcRef[0]));
        prepMinField.getDocument().addDocumentListener(new DocListener(recalcRef[0]));

        JPanel ingSection = new JPanel(new BorderLayout(0, 6));
        ingSection.setBackground(CulinaryTheme.BG_CARD);
        ingSection.setBorder(BorderFactory.createEmptyBorder(0, 18, 8, 18));
        JPanel ingHeader = new JPanel(new BorderLayout());
        ingHeader.setOpaque(false);
        JLabel ingTitle = new JLabel("Ingredientes del Escandallo (sin repetidos):");
        ingTitle.setFont(CulinaryTheme.FONT_SUBTITLE);
        JButton addRowBtn = CulinaryTheme.createSecondaryButton("+ Añadir Ingrediente");
        addRowBtn.addActionListener(e -> addEmptyRow.run());
        ingHeader.add(ingTitle, BorderLayout.WEST);
        ingHeader.add(addRowBtn, BorderLayout.EAST);

        JScrollPane rowsScroll = new JScrollPane(rowsContainer);
        rowsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rowsScroll.setBorder(BorderFactory.createLineBorder(CulinaryTheme.BORDER_FLOUR, 1));
        rowsScroll.setPreferredSize(new Dimension(660, 150));

        JPanel summaryBox = new JPanel(new GridLayout(2, 1, 0, 2));
        summaryBox.setOpaque(false);
        summaryBox.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        summaryBox.add(liveSummary);
        summaryBox.add(validationLabel);

        ingSection.add(ingHeader, BorderLayout.NORTH);
        ingSection.add(rowsScroll, BorderLayout.CENTER);
        ingSection.add(summaryBox, BorderLayout.SOUTH);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(CulinaryTheme.BG_CARD);
        centerWrap.add(form, BorderLayout.NORTH);
        centerWrap.add(ingSection, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttons.setBackground(CulinaryTheme.BG_MAIN);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CulinaryTheme.BORDER_FLOUR));
        JButton cancelBtn = CulinaryTheme.createSecondaryButton("Cancelar");
        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            try {
                List<RecipeIngredient> built = new ArrayList<>();
                for (IngredientRow rowItem : ingredientRows) {
                    IngredientItem sel = (IngredientItem) rowItem.combo.getSelectedItem();
                    Ingredient ing = sel == null ? null : sel.ingredient;
                    UnitType u = (UnitType) rowItem.unitCombo.getSelectedItem();
                    double qty = Double.parseDouble(rowItem.qtyField.getText().trim());
                    built.add(new RecipeIngredient(ing, u, qty));
                }
                String prepTxt = prepMinField.getText().trim();
                int prep = prepTxt.isEmpty() ? 0 : Integer.parseInt(prepTxt);
                double yQty = Double.parseDouble(yieldQtyField.getText().trim());

                if (editing == null) {
                    recipeService.createRecipe(nameField.getText(), descField.getText(),
                            yQty, yieldUnitField.getText(), prep, instrArea.getText(), built);
                } else {
                    recipeService.updateRecipe(editing.getId(), nameField.getText(), descField.getText(),
                            yQty, yieldUnitField.getText(), prep, instrArea.getText(), built);
                }
                dialog.dispose();
                refreshData();
                onDataChanged.run();
            } catch (CulinaryDomainException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "No se pudo guardar", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttons.add(cancelBtn);
        buttons.add(saveBtn);

        dialog.add(centerWrap, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        recalcRef[0].run();
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

    private record IngredientItem(Ingredient ingredient) {
        @Override
        public String toString() {
            if (ingredient == null) {
                return "-- Seleccionar ingrediente --";
            }
            return ingredient.getName()
                    + " (" + CurrencyFormatter.formatUSD(ingredient.getEffectiveUnitPrice())
                    + "/" + ingredient.getUnit().getSymbol() + ")";
        }
    }

    private static class IngredientRow {
        final JPanel panel = new JPanel(new BorderLayout(0, 2));
        final JComboBox<IngredientItem> combo = new JComboBox<>();
        final JTextField qtyField = new JTextField(6);
        final JComboBox<UnitType> unitCombo = new JComboBox<>(
                new UnitType[]{UnitType.Gr, UnitType.KG, UnitType.ML, UnitType.Lt, UnitType.Und});
        final JLabel statusLabel = new JLabel(" ");

        IngredientRow(
                List<Ingredient> catalog,
                Ingredient selected,
                UnitType selectedUnit,
                String qty,
                Runnable onChange,
                java.util.function.Consumer<IngredientRow> onRemove) {
            panel.setBackground(CulinaryTheme.BG_CARD);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, CulinaryTheme.BORDER_FLOUR),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)));

            combo.setPreferredSize(new Dimension(280, 30));
            combo.addItem(new IngredientItem(null));
            IngredientItem match = null;
            for (Ingredient ing : catalog) {
                IngredientItem item = new IngredientItem(ing);
                combo.addItem(item);
                if (selected != null && ing.getId().equals(selected.getId())) {
                    match = item;
                }
            }
            if (match != null) {
                combo.setSelectedItem(match);
            }
            qtyField.setText(qty);
            qtyField.setBorder(CulinaryTheme.fieldBorder());
            qtyField.setPreferredSize(new Dimension(74, 30));
            unitCombo.setSelectedItem(selectedUnit);
            unitCombo.setPreferredSize(new Dimension(60, 30));

            combo.addActionListener(e -> {
                IngredientItem item = (IngredientItem) combo.getSelectedItem();
                if (item != null && item.ingredient != null) {
                    unitCombo.setSelectedItem(item.ingredient.getUnit());
                    combo.setToolTipText("[" + item.ingredient.getCategory() + "] " + item.toString());
                } else {
                    combo.setToolTipText(null);
                }
                onChange.run();
            });
            unitCombo.addActionListener(e -> onChange.run());
            qtyField.getDocument().addDocumentListener(new DocListener(onChange));

            JButton removeBtn = CulinaryTheme.createSecondaryButton("Quitar");
            removeBtn.addActionListener(e -> onRemove.accept(this));

            JPanel topControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            topControls.setOpaque(false);
            topControls.add(combo);
            topControls.add(new JLabel("Cant:"));
            topControls.add(qtyField);
            topControls.add(unitCombo);
            topControls.add(removeBtn);

            statusLabel.setFont(CulinaryTheme.FONT_SMALL);
            statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

            panel.add(topControls, BorderLayout.NORTH);
            panel.add(statusLabel, BorderLayout.SOUTH);
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 6));
        }

        void setRowStatus(String text, Color color, boolean invalidQtyBorder) {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
            if (invalidQtyBorder) {
                qtyField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CulinaryTheme.ALERT_RED, 1),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            } else {
                qtyField.setBorder(CulinaryTheme.fieldBorder());
            }
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
