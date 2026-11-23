package com.culinaryops.presentation.ui;

import com.culinaryops.application.RecipeService;
import com.culinaryops.domain.model.Recipe;
import com.culinaryops.domain.model.dto.MoldDistributionResult;
import com.culinaryops.domain.model.dto.MoldScaleResult;
import com.culinaryops.domain.model.enums.MoldShape;
import com.culinaryops.domain.model.mold.BakingMold;
import com.culinaryops.domain.model.mold.RectangularMold;
import com.culinaryops.domain.model.mold.RoundMold;
import com.culinaryops.domain.model.mold.SquareMold;
import com.culinaryops.domain.service.InputRegexValidator;
import com.culinaryops.domain.service.MoldCalculator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Módulo 6: Calculadora de Moldes (escalado entre moldes y distribución proporcional de masa).
 */
public class MoldCalculatorPanel extends JPanel {

    private static final double MIN_MOLD_CM = 0.1;

    private final RecipeService recipeService;
    private final MoldCalculator moldCalculator;
    private final JComboBox<RecipeWeightChoice> recipeWeightCombo = new JComboBox<>();

    public MoldCalculatorPanel(RecipeService recipeService, MoldCalculator moldCalculator) {
        this.recipeService = recipeService;
        this.moldCalculator = moldCalculator;

        setLayout(new BorderLayout(0, 12));
        setBackground(CulinaryTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Calculadora de Moldes");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JLabel subtitle = new JLabel(
                "Escala recetas proporcionalmente entre moldes o reparte una masa exacta según el volumen");
        subtitle.setFont(CulinaryTheme.FONT_BODY);
        subtitle.setForeground(CulinaryTheme.TEXT_SECONDARY);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(CulinaryTheme.FONT_BODY_BOLD);
        tabs.addTab("1. Escalar Receta (Origen -> Destino)", buildScaleTab());
        tabs.addTab("2. Repartir Masa entre Moldes", buildDistributeTab());
        add(tabs, BorderLayout.CENTER);

        refreshData();
    }

    public final void refreshData() {
        Object previous = recipeWeightCombo.getSelectedItem();
        recipeWeightCombo.removeAllItems();
        recipeWeightCombo.addItem(new RecipeWeightChoice(null));
        for (Recipe r : recipeService.findAll()) {
            recipeWeightCombo.addItem(new RecipeWeightChoice(r));
        }
        if (previous instanceof RecipeWeightChoice prev && prev.recipe != null) {
            for (int i = 0; i < recipeWeightCombo.getItemCount(); i++) {
                RecipeWeightChoice item = recipeWeightCombo.getItemAt(i);
                if (item.recipe != null && item.recipe.getId().equals(prev.recipe.getId())) {
                    recipeWeightCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private JPanel buildScaleTab() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(CulinaryTheme.BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JLabel kpiOrigVal = CulinaryTheme.createKpiLabel("0.0 cm³");
        JLabel kpiOrigSub = new JLabel("Capacidad del molde base");
        JLabel kpiDestVal = CulinaryTheme.createKpiLabel("0.0 cm³");
        JLabel kpiDestSub = new JLabel("Capacidad del molde objetivo");
        JLabel kpiFactorVal = CulinaryTheme.createKpiLabel("1.000x");
        JLabel kpiFactorSub = new JLabel("Variación: 0.0%");

        JPanel kpiRow = new JPanel(new GridLayout(1, 3, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.add(CulinaryTheme.buildKpiCard("Volumen Molde Origen", kpiOrigVal, kpiOrigSub, false));
        kpiRow.add(CulinaryTheme.buildKpiCard("Volumen Molde Destino", kpiDestVal, kpiDestSub, false));
        kpiRow.add(CulinaryTheme.buildKpiCard("Factor de Escala", kpiFactorVal, kpiFactorSub, true));
        root.add(kpiRow, BorderLayout.NORTH);

        MoldFormInputs originInputs = new MoldFormInputs("Molde Origen", MoldShape.ROUND, "20", "20", "5");
        MoldFormInputs targetInputs = new MoldFormInputs("Molde Destino", MoldShape.ROUND, "26", "26", "6");

        JPanel formsGrid = new JPanel(new GridLayout(1, 2, 12, 0));
        formsGrid.setOpaque(false);
        formsGrid.add(originInputs.card);
        formsGrid.add(targetInputs.card);
        root.add(formsGrid, BorderLayout.CENTER);

        JPanel resultCard = CulinaryTheme.createCardPanel();
        resultCard.setLayout(new BorderLayout(12, 6));
        JLabel scaleStatusLabel = new JLabel(" ");
        scaleStatusLabel.setFont(CulinaryTheme.FONT_SMALL);
        JLabel explanationLabel = new JLabel(" ");
        explanationLabel.setFont(CulinaryTheme.FONT_BODY_BOLD);
        explanationLabel.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JButton calcBtn = CulinaryTheme.createPrimaryButton("Calcular Escala");

        JPanel textCol = new JPanel(new GridLayout(2, 1, 0, 4));
        textCol.setOpaque(false);
        textCol.add(scaleStatusLabel);
        textCol.add(explanationLabel);
        resultCard.add(textCol, BorderLayout.CENTER);
        resultCard.add(calcBtn, BorderLayout.EAST);
        root.add(resultCard, BorderLayout.SOUTH);

        Runnable updateScale = () -> {
            Optional<BakingMold> originOpt = originInputs.validateAndBuild("Origen");
            Optional<BakingMold> targetOpt = targetInputs.validateAndBuild("Destino");

            originOpt.ifPresentOrElse(
                    m -> {
                        kpiOrigVal.setText(String.format(Locale.US, "%.1f cm³", m.calculateVolumeCm3()));
                        kpiOrigSub.setText(m.getShapeLabel() + " (" + m.getHeightCm() + " cm alto)");
                    },
                    () -> {
                        kpiOrigVal.setText("-- cm³");
                        kpiOrigSub.setText("Dimensiones pendientes");
                    });
            targetOpt.ifPresentOrElse(
                    m -> {
                        kpiDestVal.setText(String.format(Locale.US, "%.1f cm³", m.calculateVolumeCm3()));
                        kpiDestSub.setText(m.getShapeLabel() + " (" + m.getHeightCm() + " cm alto)");
                    },
                    () -> {
                        kpiDestVal.setText("-- cm³");
                        kpiDestSub.setText("Dimensiones pendientes");
                    });

            boolean bothOk = originOpt.isPresent() && targetOpt.isPresent();
            calcBtn.setEnabled(bothOk);
            if (bothOk) {
                MoldScaleResult res = moldCalculator.scaleRecipeBetweenMolds(originOpt.get(), targetOpt.get());
                kpiFactorVal.setText(String.format(Locale.US, "%.3fx", res.getScaleFactor()));
                String sign = res.getPercentageChange() > 0 ? "+" : "";
                kpiFactorSub.setText(String.format(Locale.US, "Variación: %s%.1f%%", sign, res.getPercentageChange()));
                scaleStatusLabel.setText("✓ Moldes válidos — factor calculado automáticamente");
                scaleStatusLabel.setForeground(CulinaryTheme.OK_GREEN);
                explanationLabel.setText(res.getExplanation());
                explanationLabel.setForeground(CulinaryTheme.ACCENT_APRICOT);
            } else {
                kpiFactorVal.setText("--");
                kpiFactorSub.setText("Corrige las medidas en rojo");
                scaleStatusLabel.setText("⚠ Corrige las dimensiones marcadas en rojo (mínimo 0.1 cm).");
                scaleStatusLabel.setForeground(CulinaryTheme.ALERT_RED);
                explanationLabel.setText("Completa medidas válidas en ambos moldes para calcular la escala.");
                explanationLabel.setForeground(CulinaryTheme.TEXT_SECONDARY);
            }
        };

        originInputs.onAnyChange(updateScale);
        targetInputs.onAnyChange(updateScale);
        calcBtn.addActionListener(e -> updateScale.run());
        updateScale.run();
        return root;
    }

    private JPanel buildDistributeTab() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(CulinaryTheme.BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JLabel kpiGramsVal = CulinaryTheme.createKpiLabel("1500 gr");
        JLabel kpiGramsSub = new JLabel("Peso total de preparación");
        JLabel kpiMoldsVal = CulinaryTheme.createKpiLabel("2");
        JLabel kpiMoldsSub = new JLabel("Moldes seleccionados");
        JLabel kpiVolVal = CulinaryTheme.createKpiLabel("0.0 cm³");
        JLabel kpiVolSub = new JLabel("Suma de capacidades");

        JPanel kpiRow = new JPanel(new GridLayout(1, 3, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.add(CulinaryTheme.buildKpiCard("Masa Total a Repartir", kpiGramsVal, kpiGramsSub, false));
        kpiRow.add(CulinaryTheme.buildKpiCard("Moldes Activos", kpiMoldsVal, kpiMoldsSub, false));
        kpiRow.add(CulinaryTheme.buildKpiCard("Volumen Combinado", kpiVolVal, kpiVolSub, true));
        root.add(kpiRow, BorderLayout.NORTH);

        // Panel superior de controles: Peso + Agregar Molde
        JPanel controlsCard = CulinaryTheme.createCardPanel();
        controlsCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField totalGramsField = new JTextField("1500", 8);
        totalGramsField.setBorder(CulinaryTheme.fieldBorder());
        JLabel gramsFeedback = createFeedbackLabel();

        DefaultListModel<BakingMold> moldListModel = new DefaultListModel<>();
        moldListModel.addElement(new RoundMold("Molde 15 cm", 15.0, 5.0));
        moldListModel.addElement(new RoundMold("Molde 20 cm", 20.0, 5.0));
        JList<BakingMold> moldList = new JList<>(moldListModel);
        moldList.setFont(CulinaryTheme.FONT_BODY);
        moldList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        MoldFormInputs newMoldInputs = new MoldFormInputs("Nuevo Molde", MoldShape.ROUND, "24", "12", "5");
        JButton addMoldBtn = CulinaryTheme.createSecondaryButton("+ Agregar Molde");
        JButton removeMoldBtn = CulinaryTheme.createSecondaryButton("Quitar Molde");
        removeMoldBtn.setEnabled(false);
        JButton calcDistBtn = CulinaryTheme.createPrimaryButton("Distribuir Masa");
        JLabel distStatusLabel = createFeedbackLabel();

        recipeWeightCombo.addActionListener(e -> {
            RecipeWeightChoice ch = (RecipeWeightChoice) recipeWeightCombo.getSelectedItem();
            if (ch != null && ch.recipe != null) {
                totalGramsField.setText(String.format(Locale.US, "%.0f", ch.recipe.calculateTotalNetGrams()));
            }
        });

        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.0;
        controlsCard.add(boldLabel("Tomar peso de receta:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4;
        controlsCard.add(recipeWeightCombo, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        controlsCard.add(boldLabel("Peso Total (gr) *:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.25;
        controlsCard.add(totalGramsField, gbc);
        gbc.gridx = 4; gbc.weightx = 0.0;
        controlsCard.add(calcDistBtn, gbc);

        gbc.gridy = 1; gbc.gridx = 3; gbc.gridwidth = 2;
        controlsCard.add(gramsFeedback, gbc);
        gbc.gridwidth = 1;

        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 5; gbc.weightx = 1.0;
        JPanel addBar = new JPanel(new BorderLayout(8, 0));
        addBar.setOpaque(false);
        addBar.add(newMoldInputs.buildCompactRow(), BorderLayout.CENTER);
        JPanel moldBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        moldBtns.setOpaque(false);
        moldBtns.add(addMoldBtn);
        moldBtns.add(removeMoldBtn);
        addBar.add(moldBtns, BorderLayout.EAST);
        controlsCard.add(addBar, gbc);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 5;
        controlsCard.add(distStatusLabel, gbc);
        gbc.gridwidth = 1;

        DefaultTableModel distModel = new DefaultTableModel(
                new String[]{"Molde", "Forma", "Volumen (cm³)", "Proporción (%)", "Gramos a Verter"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable distTable = new JTable(distModel);
        CulinaryTheme.styleTable(distTable);
        var cols = distTable.getColumnModel();
        cols.getColumn(1).setCellRenderer(CulinaryTheme.centerRenderer());
        cols.getColumn(2).setCellRenderer(CulinaryTheme.centerMonoRenderer());
        cols.getColumn(3).setCellRenderer(CulinaryTheme.centerMonoRenderer());
        cols.getColumn(4).setCellRenderer(CulinaryTheme.centerMonoRenderer());

        JPanel listPanel = CulinaryTheme.createCardPanel();
        listPanel.setLayout(new BorderLayout(0, 6));
        listPanel.setPreferredSize(new Dimension(280, 180));
        JLabel listTitle = new JLabel("Moldes en Reparto");
        listTitle.setFont(CulinaryTheme.FONT_SUBTITLE);
        listPanel.add(listTitle, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(moldList), BorderLayout.CENTER);

        JPanel centerSplit = new JPanel(new BorderLayout(12, 0));
        centerSplit.setOpaque(false);
        centerSplit.add(listPanel, BorderLayout.WEST);
        centerSplit.add(CulinaryTheme.wrapTable(distTable), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(controlsCard, BorderLayout.NORTH);
        body.add(centerSplit, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        Runnable[] recalcRef = new Runnable[1];
        recalcRef[0] = () -> {
            String gTxt = totalGramsField.getText().trim();
            boolean gramsOk = InputRegexValidator.isValidPositiveDecimal(gTxt) && Double.parseDouble(gTxt) > 0;
            if (gTxt.isEmpty()) {
                setFieldBorderState(totalGramsField, gramsFeedback,
                        "Campo obligatorio (ej. 1500 gr).", CulinaryTheme.TEXT_SECONDARY, false);
                kpiGramsVal.setText("-- gr");
            } else if (!gramsOk) {
                setFieldBorderState(totalGramsField, gramsFeedback,
                        "⚠ Ingresa peso > 0 gr (ej. 1500).", CulinaryTheme.ALERT_RED, true);
                kpiGramsVal.setText("-- gr");
            } else {
                setFieldBorderState(totalGramsField, gramsFeedback,
                        "✓ Peso válido (" + gTxt + " gr)", CulinaryTheme.OK_GREEN, false);
                kpiGramsVal.setText(gTxt + " gr");
            }

            Optional<BakingMold> newMoldOpt = newMoldInputs.validateAndBuild("Molde #" + (moldListModel.size() + 1));
            addMoldBtn.setEnabled(newMoldOpt.isPresent());
            removeMoldBtn.setEnabled(moldList.getSelectedIndex() >= 0 && moldListModel.size() > 0);

            List<BakingMold> molds = new ArrayList<>();
            double totalVol = 0.0;
            for (int i = 0; i < moldListModel.size(); i++) {
                BakingMold m = moldListModel.getElementAt(i);
                molds.add(m);
                totalVol += m.calculateVolumeCm3();
            }
            kpiMoldsVal.setText(String.valueOf(molds.size()));
            kpiMoldsSub.setText(molds.size() == 1 ? "1 molde activo" : molds.size() + " moldes activos");
            kpiVolVal.setText(String.format(Locale.US, "%.1f cm³", totalVol));

            boolean canDistribute = gramsOk && !molds.isEmpty();
            calcDistBtn.setEnabled(canDistribute);

            if (molds.isEmpty()) {
                distStatusLabel.setText("⚠ Agrega al menos 1 molde válido a la lista para repartir la masa.");
                distStatusLabel.setForeground(CulinaryTheme.ALERT_RED);
                distModel.setRowCount(0);
            } else if (!gramsOk) {
                distStatusLabel.setText("⚠ Corrige el peso total de masa (> 0 gr) para calcular el reparto.");
                distStatusLabel.setForeground(CulinaryTheme.ALERT_RED);
            } else {
                if (newMoldOpt.isPresent()) {
                    distStatusLabel.setText(String.format(Locale.US,
                            "✓ Nuevo molde listo (+%.1f cm³) | Reparto calculado para %.0f gr.",
                            newMoldOpt.get().calculateVolumeCm3(), Double.parseDouble(gTxt)));
                    distStatusLabel.setForeground(CulinaryTheme.OK_GREEN);
                } else {
                    distStatusLabel.setText("⚠ Las medidas para agregar un nuevo molde deben ser >= 0.1 cm.");
                    distStatusLabel.setForeground(CulinaryTheme.ALERT_AMBER);
                }
                MoldDistributionResult res = moldCalculator.distributeMixture(Double.parseDouble(gTxt), molds);
                distModel.setRowCount(0);
                for (MoldDistributionResult.MoldAllocation a : res.getAllocations()) {
                    distModel.addRow(new Object[]{
                            a.getMoldName(),
                            a.getShapeLabel(),
                            String.format(Locale.US, "%.1f", a.getVolumeCm3()),
                            String.format(Locale.US, "%.1f%%", a.getProportionPercentage()),
                            a.getAssignedGrams() + " gr"
                    });
                }
            }
        };

        totalGramsField.getDocument().addDocumentListener(new SimpleDoc(recalcRef[0]));
        newMoldInputs.onAnyChange(recalcRef[0]);
        moldList.addListSelectionListener(e -> recalcRef[0].run());

        addMoldBtn.addActionListener(e -> {
            Optional<BakingMold> m = newMoldInputs.validateAndBuild("Molde #" + (moldListModel.size() + 1));
            if (m.isPresent()) {
                moldListModel.addElement(m.get());
                recalcRef[0].run();
            }
        });
        removeMoldBtn.addActionListener(e -> {
            int idx = moldList.getSelectedIndex();
            if (idx >= 0) {
                moldListModel.remove(idx);
                recalcRef[0].run();
            }
        });
        calcDistBtn.addActionListener(e -> recalcRef[0].run());

        recalcRef[0].run();
        return root;
    }

    private static JLabel boldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(CulinaryTheme.FONT_BODY_BOLD);
        lbl.setForeground(CulinaryTheme.TEXT_PRIMARY);
        return lbl;
    }

    private static JLabel createFeedbackLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(CulinaryTheme.FONT_SMALL);
        return lbl;
    }

    private static void setFieldBorderState(
            JTextField field, JLabel feedback, String text, Color color, boolean error) {
        if (feedback != null) {
            feedback.setText(text);
            feedback.setForeground(color);
        }
        field.setBorder(error
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CulinaryTheme.ALERT_RED, 1),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8))
                : CulinaryTheme.fieldBorder());
    }

    private static String shapeDisplay(MoldShape shape) {
        return switch (shape) {
            case ROUND -> "Redondo";
            case RECTANGULAR -> "Rectangular";
            case SQUARE -> "Cuadrado";
        };
    }

    /**
     * Grupo reutilizable de entradas para un molde con validación en vivo y ajuste dinámico de dimensiones.
     */
    private static class MoldFormInputs {
        final JPanel card = CulinaryTheme.createCardPanel();
        final JComboBox<MoldShape> shapeCombo = new JComboBox<>(MoldShape.values());
        final JTextField dim1Field;
        final JTextField dim2Field;
        final JTextField heightField;
        final JLabel dim1Label = boldLabel("Diámetro (cm):");
        final JLabel dim2Label = boldLabel("Ancho (cm):");
        final JLabel dim1Feedback = createFeedbackLabel();
        final JLabel dim2Feedback = createFeedbackLabel();
        final JLabel heightFeedback = createFeedbackLabel();
        final JLabel summaryFeedback = createFeedbackLabel();

        MoldFormInputs(String title, MoldShape initialShape, String d1, String d2, String h) {
            dim1Field = new JTextField(d1, 8);
            dim2Field = new JTextField(d2, 8);
            heightField = new JTextField(h, 8);
            dim1Field.setBorder(CulinaryTheme.fieldBorder());
            dim2Field.setBorder(CulinaryTheme.fieldBorder());
            heightField.setBorder(CulinaryTheme.fieldBorder());

            shapeCombo.setSelectedItem(initialShape);
            shapeCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof MoldShape s) {
                        setText(shapeDisplay(s));
                    }
                    return this;
                }
            });

            card.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 6, 2, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            JLabel titleLbl = new JLabel(title);
            titleLbl.setFont(CulinaryTheme.FONT_SUBTITLE);
            titleLbl.setForeground(CulinaryTheme.ACCENT_APRICOT);
            gbc.gridy = 0; gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
            card.add(titleLbl, gbc);
            gbc.gridwidth = 1;

            int r = 1;
            addCardRow(gbc, r++, boldLabel("Forma:"), shapeCombo);
            addCardRow(gbc, r++, dim1Label, dim1Field);
            addCardFeedback(gbc, r++, dim1Feedback);
            addCardRow(gbc, r++, dim2Label, dim2Field);
            addCardFeedback(gbc, r++, dim2Feedback);
            addCardRow(gbc, r++, boldLabel("Alto (cm):"), heightField);
            addCardFeedback(gbc, r++, heightFeedback);

            gbc.gridy = r; gbc.gridx = 0; gbc.gridwidth = 2;
            card.add(summaryFeedback, gbc);
        }

        private void addCardRow(GridBagConstraints gbc, int row, JLabel label, Component comp) {
            gbc.gridy = row; gbc.gridx = 0; gbc.weightx = 0.0;
            card.add(label, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            card.add(comp, gbc);
        }

        private void addCardFeedback(GridBagConstraints gbc, int row, JLabel fb) {
            gbc.gridy = row; gbc.gridx = 1; gbc.weightx = 1.0;
            card.add(fb, gbc);
        }

        JPanel buildCompactRow() {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setOpaque(false);
            row.add(boldLabel("Nuevo Molde:"));
            row.add(shapeCombo);
            row.add(dim1Label);
            row.add(dim1Field);
            row.add(dim2Label);
            row.add(dim2Field);
            row.add(boldLabel("Alto (cm):"));
            row.add(heightField);
            return row;
        }

        void onAnyChange(Runnable callback) {
            SimpleDoc doc = new SimpleDoc(callback);
            dim1Field.getDocument().addDocumentListener(doc);
            dim2Field.getDocument().addDocumentListener(doc);
            heightField.getDocument().addDocumentListener(doc);
            shapeCombo.addActionListener(e -> callback.run());
        }

        Optional<BakingMold> validateAndBuild(String moldName) {
            MoldShape shape = (MoldShape) shapeCombo.getSelectedItem();
            if (shape == null) {
                shape = MoldShape.ROUND;
            }
            boolean isRect = shape == MoldShape.RECTANGULAR;
            dim1Label.setText(switch (shape) {
                case ROUND -> "Diámetro (cm):";
                case SQUARE -> "Lado (cm):";
                case RECTANGULAR -> "Largo (cm):";
            });
            dim2Label.setVisible(isRect);
            dim2Field.setVisible(isRect);
            dim2Feedback.setVisible(isRect);
            dim2Field.setEnabled(isRect);

            Double v1 = parseValidDim(dim1Field, dim1Feedback);
            Double v2 = isRect ? parseValidDim(dim2Field, dim2Feedback) : Double.valueOf(1.0);
            Double vh = parseValidDim(heightField, heightFeedback);

            if (v1 == null || v2 == null || vh == null) {
                summaryFeedback.setText("⚠ Ingresa medidas válidas (>= 0.1 cm).");
                summaryFeedback.setForeground(CulinaryTheme.ALERT_RED);
                return Optional.empty();
            }

            BakingMold mold = switch (shape) {
                case ROUND -> new RoundMold(moldName, v1, vh);
                case SQUARE -> new SquareMold(moldName, v1, vh);
                case RECTANGULAR -> new RectangularMold(moldName, v1, v2, vh);
            };
            summaryFeedback.setText(String.format(Locale.US, "✓ Volumen estimado: %.1f cm³", mold.calculateVolumeCm3()));
            summaryFeedback.setForeground(CulinaryTheme.OK_GREEN);
            return Optional.of(mold);
        }

        private Double parseValidDim(JTextField field, JLabel fb) {
            String txt = field.getText().trim();
            if (txt.isEmpty()) {
                setFieldBorderState(field, fb, "Obligatorio (>= 0.1 cm).", CulinaryTheme.TEXT_SECONDARY, true);
                return null;
            }
            if (!InputRegexValidator.isValidPositiveDecimal(txt)) {
                setFieldBorderState(field, fb, "⚠ Número inválido (ej. 20).", CulinaryTheme.ALERT_RED, true);
                return null;
            }
            double val = Double.parseDouble(txt);
            if (val < MIN_MOLD_CM) {
                setFieldBorderState(field, fb, "⚠ Mínimo 0.1 cm.", CulinaryTheme.ALERT_RED, true);
                return null;
            }
            setFieldBorderState(field, fb, "✓ " + txt + " cm", CulinaryTheme.OK_GREEN, false);
            return val;
        }
    }

    private record RecipeWeightChoice(Recipe recipe) {
        @Override
        public String toString() {
            if (recipe == null) {
                return "-- Peso manual --";
            }
            return String.format(Locale.US, "%s (%.0f gr)", recipe.getName(), recipe.calculateTotalNetGrams());
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
