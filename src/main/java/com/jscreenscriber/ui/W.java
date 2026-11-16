package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/** Small widget helpers shared by the shell. */
public final class W {
    private W() {}

    public interface Themed { void applyTheme(); }

    /** Re-applies palette-dependent properties to every Themed component below c. */
    public static void refresh(Component c) {
        if (c instanceof Themed t) t.applyTheme();
        if (c instanceof Container ct) for (Component k : ct.getComponents()) refresh(k);
        if (c instanceof JMenu m) for (Component k : m.getMenuComponents()) refresh(k);
        if (c instanceof JComponent jc) jc.repaint();
    }

    /** Flat 20px-high push button (toolbar action button). */
    public static class Btn extends JButton implements Themed {
        private final String iconName;
        public Btn(String text, String icon) {
            super(text); this.iconName = icon;
            setFocusable(false); setFont(Theme.ui(Font.PLAIN, 10)); setIconTextGap(4); setMargin(new Insets(2, 6, 2, 6));
            putClientProperty("FlatLaf.style", "arc:3; margin:2,6,2,6; minimumWidth:24; focusWidth:0");
            applyTheme();
        }
        public void applyTheme() {
            setFont(Theme.ui(Font.PLAIN, 10));
            if (iconName != null) setIcon(Icons.get(iconName, Theme.P.text));
            setPreferredSize(null); Dimension d = getPreferredSize(); setPreferredSize(new Dimension(Math.max(24, d.width), Math.max(24, d.height)));
        }
    }

    public static class Tgl extends JToggleButton implements Themed {
        private final String iconName;
        public Tgl(String text, String icon) {
            super(text); this.iconName = icon;
            setFocusable(false); setFont(Theme.ui(Font.PLAIN, 10)); setIconTextGap(3); setMargin(new Insets(2, 6, 2, 6));
            putClientProperty("FlatLaf.style", "arc:3; margin:2,6,2,6; minimumWidth:24; focusWidth:0");
            addChangeListener(e -> applyTheme());
            applyTheme();
        }
        public void applyTheme() {
            setFont(Theme.ui(isSelected() ? Font.BOLD : Font.PLAIN, 10));
            if (iconName != null) setIcon(Icons.get(iconName, isSelected() ? Theme.P.accentDark : Theme.P.text));
            setPreferredSize(null); Dimension d = getPreferredSize(); setPreferredSize(new Dimension(Math.max(24, d.width), Math.max(24, d.height)));
        }
    }

    /** Small status/title "chip" with an optional dot. */
    public static class Chip extends JComponent implements Themed {
        private String text = "", valueText = ""; private Color dot; private boolean accent, valueColored;
        private Color valueColor;
        public Chip(String text) { this.text = text; setOpaque(false); setFont(Theme.ui(Font.PLAIN, 10)); }
        public void set(String label, String value, Color dot, boolean accent, Color valueColor) {
            this.text = label; this.valueText = value == null ? "" : value; this.dot = dot; this.accent = accent; this.valueColor = valueColor;
            revalidate(); repaint();
        }
        public void applyTheme() { setFont(Theme.ui(Font.PLAIN, 10)); revalidate(); repaint(); }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Theme.ui(Font.BOLD, 10));
            int w = 12 + fm.stringWidth(text) + (valueText.isEmpty() ? 0 : 3 + fm.stringWidth(valueText)) + (dot != null ? 9 : 0);
            return new Dimension(w, 22);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Theme.Palette p = Theme.P;
            int w = getWidth() - 1, h = getHeight() - 1;
            g.setColor(accent ? p.accentBg : p.tagBg); g.fillRoundRect(0, 0, w, h, 3, 3);
            g.setColor(accent ? p.accentBorder : p.btnBorder); g.drawRoundRect(0, 0, w, h, 3, 3);
            int x = 5; FontMetrics fm;
            if (dot != null) { g.setColor(dot); g.fillOval(x, h / 2 - 2, 5, 5); x += 9; }
            g.setFont(Theme.ui(Font.BOLD, 10)); fm = g.getFontMetrics();
            int by = (h + fm.getAscent() - fm.getDescent()) / 2 + 1;
            g.setColor(accent ? p.accentDark : p.text); g.drawString(text, x, by); x += fm.stringWidth(text) + 3;
            if (!valueText.isEmpty()) { g.setColor(valueColor != null ? valueColor : p.text); g.drawString(valueText, x, by); }
            g.dispose();
        }
    }

    public static JLabel label(String t, int style, float size) { JLabel l = new JLabel(t); l.setFont(Theme.ui(style, size)); return l; }

    public static JPanel row(int hgap, Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, 0)); p.setOpaque(false);
        for (Component c : cs) p.add(c); return p;
    }

    public static JSeparator vsep() { JSeparator s = new JSeparator(JSeparator.VERTICAL); s.setPreferredSize(new Dimension(5, 20)); return s; }

    public static void onClick(AbstractButton b, Runnable r) { b.addActionListener(e -> r.run()); }
    public static <T> void on(JComboBox<T> c, Consumer<T> r) { c.addActionListener(e -> { @SuppressWarnings("unchecked") T v = (T) c.getSelectedItem(); r.accept(v); }); }
}
