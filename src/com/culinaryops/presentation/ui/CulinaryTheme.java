package com.culinaryops.presentation.ui;

import com.culinaryops.domain.model.Ingredient;
import com.culinaryops.domain.model.enums.UnitType;
import com.culinaryops.domain.model.enums.WasteLevel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Constantes visuales y componentes base de diseño para CulinaryOps
 * siguiendo la paleta oficial Marfil Suave, Pizarra Carbón y Damasco Vivo.
 */
public final class CulinaryTheme {

    public static final Color BG_MAIN = new Color(0xFA, 0xF8, 0xF5);        // Marfil Suave #FAF8F5
    public static final Color BG_CARD = Color.WHITE;                         // Blanco #FFFFFF
    public static final Color BORDER_FLOUR = new Color(0xE8, 0xE4, 0xDD);   // Gris Harina #E8E4DD
    public static final Color SIDEBAR_CHARCOAL = new Color(0x1E, 0x23, 0x2A); // Pizarra Carbón #1E232A
    public static final Color SIDEBAR_HOVER = new Color(0x2B, 0x31, 0x3B);
    public static final Color TEXT_PRIMARY = new Color(0x1E, 0x23, 0x2A);   // #1E232A
    public static final Color TEXT_SECONDARY = new Color(0x6B, 0x64, 0x5C); // #6B645C
    public static final Color TEXT_ACCESSIBLE = new Color(0x4A, 0x3E, 0x3D);// #4A3E3D
    public static final Color ACCENT_APRICOT = new Color(0xE0, 0x6D, 0x3B); // Damasco Vivo #E06D3B
    public static final Color ACCENT_APRICOT_HOVER = new Color(0xC9, 0x5B, 0x2C);
    public static final Color ALERT_RED = new Color(0xB4, 0x23, 0x18);
    public static final Color ALERT_RED_BG = new Color(0xFE, 0xF3, 0xF2);
    public static final Color ALERT_AMBER = new Color(0xB5, 0x47, 0x08);
    public static final Color ALERT_AMBER_BG = new Color(0xFF, 0xFA, 0xEB);
    public static final Color OK_GREEN = new Color(0x06, 0x76, 0x47);
    public static final Color OK_GREEN_BG = new Color(0xEC, 0xFD, 0xF3);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 13);
    public static final Font FONT_MONO_BOLD = new Font("Monospaced", Font.BOLD, 14);
    public static final Font FONT_KPI = new Font("Monospaced", Font.BOLD, 20);

    private CulinaryTheme() {
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_FLOUR, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    public static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_FLOUR, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(cardBorder());
        return panel;
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(ACCENT_APRICOT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.addPropertyChangeListener("enabled", evt -> {
            if (btn.isEnabled()) {
                btn.setBackground(ACCENT_APRICOT);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(BORDER_FLOUR);
                btn.setForeground(TEXT_SECONDARY);
            }
        });
        return btn;
    }

    public static JSlider createStyledSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setBackground(BG_CARD);
        slider.setOpaque(false);
        slider.setFocusable(true);
        slider.setUI(new BasicSliderUI(slider) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int trackY = trackRect.y + (trackRect.height - 6) / 2;
                g2.setColor(BORDER_FLOUR);
                g2.fillRoundRect(trackRect.x, trackY, trackRect.width, 6, 6, 6);

                int fillWidth = thumbRect.x + thumbRect.width / 2 - trackRect.x;
                if (fillWidth > 0) {
                    g2.setColor(ACCENT_APRICOT);
                    g2.fillRoundRect(trackRect.x, trackY, Math.min(fillWidth, trackRect.width), 6, 6, 6);
                }
                g2.dispose();
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 16;
                int x = thumbRect.x + (thumbRect.width - size) / 2;
                int y = thumbRect.y + (thumbRect.height - size) / 2;
                g2.setColor(ACCENT_APRICOT);
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);
                g2.setColor(ACCENT_APRICOT_HOVER);
                g2.drawOval(x, y, size, size);
                g2.dispose();
            }

            @Override
            protected Dimension getThumbSize() {
                return new Dimension(18, 18);
            }
        });
        return slider;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setFont(FONT_BODY);
        btn.setBackground(BG_CARD);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_FLOUR, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        return btn;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(30);
        table.setGridColor(BORDER_FLOUR);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(0xFB, 0xEB, 0xE3));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setBackground(BG_CARD);
        table.getTableHeader().setFont(FONT_SMALL.deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(BG_MAIN);
        table.getTableHeader().setForeground(TEXT_ACCESSIBLE);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
    }

    public static JScrollPane wrapTable(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_FLOUR, 1));
        scroll.getViewport().setBackground(BG_CARD);
        return scroll;
    }

    public static DefaultTableCellRenderer rightMonoRenderer() {
        return alignedRenderer(SwingConstants.RIGHT, true);
    }

    public static DefaultTableCellRenderer centerRenderer() {
        return alignedRenderer(SwingConstants.CENTER, false);
    }

    public static DefaultTableCellRenderer centerMonoRenderer() {
        return alignedRenderer(SwingConstants.CENTER, true);
    }

    private static DefaultTableCellRenderer alignedRenderer(int alignment, boolean mono) {
        return new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(alignment);
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (mono) {
                    comp.setFont(FONT_MONO);
                }
                return comp;
            }
        };
    }

    public static DefaultListCellRenderer unitSymbolRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UnitType unit) {
                    setText(unit.getSymbol());
                }
                return this;
            }
        };
    }

    public static void installEscapeClose(JDialog dialog) {
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static String formatWasteBadge(Ingredient ing) {
        WasteLevel level = ing.getWasteLevel();
        String prefix = switch (level) {
            case LOW -> "✓ Baja";
            case MEDIUM -> "● Media";
            case HIGH -> "⚠ Alta";
        };
        return String.format(Locale.US, "%s (%.1f%%)", prefix, ing.getWastePercentage());
    }

    public static String formatExpirationText(Ingredient ing) {
        if (!ing.isPerishable()) {
            return "No perecedero";
        }
        if (ing.getExpirationDate() == null) {
            return "Sin fecha asignada";
        }
        Long days = ing.getDaysUntilExpiration();
        if (days < 0) {
            return "⚠ ¡Venció! (" + ing.getExpirationDate() + ")";
        }
        if (days == 0) {
            return "⚠ Vence HOY (" + ing.getExpirationDate() + ")";
        }
        if (days <= 7) {
            return "⚠ " + ing.getExpirationDate() + " (" + days + "d)";
        }
        return ing.getExpirationDate() + " (" + days + "d)";
    }

    public static JLabel createKpiLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_KPI);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    public static JPanel buildKpiCard(String title, JLabel valueLabel, JLabel subtitleLabel, boolean accent) {
        JPanel card = createCardPanel();
        card.setLayout(new java.awt.BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(200, 120));
        card.setMinimumSize(new Dimension(180, 116));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_SMALL.deriveFont(Font.BOLD));
        titleLbl.setForeground(accent ? ACCENT_APRICOT : TEXT_SECONDARY);

        valueLabel.setFont(FONT_KPI);
        valueLabel.setForeground(accent ? ACCENT_APRICOT : TEXT_PRIMARY);

        subtitleLabel.setFont(FONT_SMALL);
        subtitleLabel.setForeground(TEXT_ACCESSIBLE);

        card.add(titleLbl, java.awt.BorderLayout.NORTH);
        card.add(valueLabel, java.awt.BorderLayout.CENTER);
        card.add(subtitleLabel, java.awt.BorderLayout.SOUTH);
        return card;
    }
}
