package com.culinaryops.presentation.ui;

import com.culinaryops.application.InventoryService;
import com.culinaryops.domain.model.dto.ChatbotAnswer;
import com.culinaryops.domain.service.InventoryQueryAssistant;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * Módulo 7: Preguntas Frecuentes (consultas guiadas sobre el inventario y catálogo).
 */
public class AssistantPanel extends JPanel {

    private static final String[] FAQ_QUESTIONS = {
            "¿Qué insumos tienen mayor merma?",
            "¿Qué ingredientes están por vencer?",
            "¿Cuánto vale mi inventario total?",
            "¿Qué insumos tienen stock bajo?",
            "¿Cuáles son los ingredientes más caros?"
    };

    private final InventoryService inventoryService;
    private final InventoryQueryAssistant assistant;

    private final Map<String, JButton> questionButtons = new LinkedHashMap<>();
    private final JLabel selectedQuestionLabel = new JLabel("Selecciona una pregunta frecuente");
    private final JLabel statusBadgeLabel = new JLabel("Elige cualquiera de las consultas para ver el reporte actualizado.");
    private final JTextArea answerArea = new JTextArea();
    private String activeQuestion;

    public AssistantPanel(InventoryService inventoryService, InventoryQueryAssistant assistant) {
        this.inventoryService = inventoryService;
        this.assistant = assistant;

        setLayout(new BorderLayout(0, 12));
        setBackground(CulinaryTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Preguntas Frecuentes");
        title.setFont(CulinaryTheme.FONT_TITLE);
        title.setForeground(CulinaryTheme.TEXT_PRIMARY);
        JLabel subtitle = new JLabel(
                "Consultas rápidas sobre mermas, caducidades, existencias y valorización del inventario");
        subtitle.setFont(CulinaryTheme.FONT_BODY);
        subtitle.setForeground(CulinaryTheme.TEXT_SECONDARY);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel questionsCard = CulinaryTheme.createCardPanel();
        questionsCard.setLayout(new BorderLayout(0, 8));
        JLabel qTitle = new JLabel("Elige una pregunta para consultar:");
        qTitle.setFont(CulinaryTheme.FONT_SUBTITLE);
        qTitle.setForeground(CulinaryTheme.TEXT_PRIMARY);

        JPanel buttonsGrid = new JPanel(new GridLayout(0, 2, 8, 8));
        buttonsGrid.setOpaque(false);
        for (String question : FAQ_QUESTIONS) {
            JButton btn = CulinaryTheme.createSecondaryButton(question);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.addActionListener(e -> selectQuestion(question));
            questionButtons.put(question, btn);
            buttonsGrid.add(btn);
        }
        questionsCard.add(qTitle, BorderLayout.NORTH);
        questionsCard.add(buttonsGrid, BorderLayout.CENTER);

        JPanel answerCard = CulinaryTheme.createCardPanel();
        answerCard.setLayout(new BorderLayout(0, 8));

        JPanel answerHeader = new JPanel(new GridLayout(2, 1, 0, 2));
        answerHeader.setOpaque(false);
        selectedQuestionLabel.setFont(CulinaryTheme.FONT_SUBTITLE);
        selectedQuestionLabel.setForeground(CulinaryTheme.ACCENT_APRICOT);
        statusBadgeLabel.setFont(CulinaryTheme.FONT_SMALL);
        statusBadgeLabel.setForeground(CulinaryTheme.TEXT_SECONDARY);
        answerHeader.add(selectedQuestionLabel);
        answerHeader.add(statusBadgeLabel);

        answerArea.setEditable(false);
        answerArea.setLineWrap(true);
        answerArea.setWrapStyleWord(true);
        answerArea.setFont(CulinaryTheme.FONT_BODY);
        answerArea.setForeground(CulinaryTheme.TEXT_PRIMARY);
        answerArea.setBackground(CulinaryTheme.BG_MAIN);
        answerArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        answerArea.setText("Haz clic en una de las preguntas frecuentes de arriba para ver los datos en tiempo real.");

        JScrollPane scroll = new JScrollPane(answerArea);
        scroll.setBorder(BorderFactory.createLineBorder(CulinaryTheme.BORDER_FLOUR, 1));

        answerCard.add(answerHeader, BorderLayout.NORTH);
        answerCard.add(scroll, BorderLayout.CENTER);

        JPanel centerWrap = new JPanel(new BorderLayout(0, 12));
        centerWrap.setOpaque(false);
        centerWrap.add(questionsCard, BorderLayout.NORTH);
        centerWrap.add(answerCard, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);
    }

    public final void refreshData() {
        if (activeQuestion != null) {
            selectQuestion(activeQuestion);
        }
    }

    private void selectQuestion(String question) {
        this.activeQuestion = question;
        ChatbotAnswer answer = assistant.ask(question, inventoryService.findAll());

        for (Map.Entry<String, JButton> entry : questionButtons.entrySet()) {
            JButton btn = entry.getValue();
            boolean selected = entry.getKey().equals(question);
            if (selected) {
                btn.setBackground(CulinaryTheme.ACCENT_APRICOT);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
            } else {
                btn.setBackground(CulinaryTheme.BG_CARD);
                btn.setForeground(CulinaryTheme.TEXT_PRIMARY);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CulinaryTheme.BORDER_FLOUR, 1),
                        BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            }
        }

        selectedQuestionLabel.setText(question);
        statusBadgeLabel.setText("✓ Consulta actualizada con el estado actual del inventario");
        statusBadgeLabel.setForeground(CulinaryTheme.OK_GREEN);
        answerArea.setText(answer.getMessage());
        answerArea.setCaretPosition(0);
    }
}
