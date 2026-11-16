package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** 24px status bar: info fields on the left; Zoom controls and INS status on the right. */
public class StatusBar extends JPanel implements W.Themed {
    private final Map<String, Field> fields = new LinkedHashMap<>();
    private final JPanel leftPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)), rightPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private JLabel[] zoomBtns = new JLabel[0];

    private static final class Field extends JPanel {
        final JLabel label = new JLabel(), value = new JLabel(); boolean highlight, right;
        Field(String l, boolean right) { super(new FlowLayout(FlowLayout.LEFT, 4, 0)); this.right = right; setOpaque(false); label.setText(l); add(label); add(value); }
    }

    public StatusBar() {
        super(new BorderLayout());
        setPreferredSize(new Dimension(100, 26));
        leftPane.setOpaque(false); rightPane.setOpaque(false);
        for (String f : new String[] { "Curr. scene:", "Line:", "Col:", "Page:", "Page Format :", "CURR:", "NEXT:", "Words:" }) add(f, false);
        add("Zoom:", true); add("", true);
        add(leftPane, BorderLayout.WEST); add(rightPane, BorderLayout.EAST);
        applyTheme();
    }

    private void add(String label, boolean right) {
        Field f = new Field(label, right); fields.put(label.isEmpty() ? "ins" : label, f);
        (right ? rightPane : leftPane).add(f);
    }

    public void set(String key, String value, boolean highlight) {
        Field f = fields.get(key); if (f == null) return;
        f.highlight = highlight; f.value.setText(value); style(f);
    }
    public void click(String key, Runnable r) {
        Field f = fields.get(key); f.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        f.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { r.run(); } });
    }
    public void clickZoom(Runnable minus, Runnable plus, Runnable reset) {
        Field f = fields.get("Zoom:");
        f.removeAll(); f.add(f.label);
        JLabel m = new JLabel(Icons.get("minus", Theme.P.muted)), pl = new JLabel(Icons.get("plus", Theme.P.muted));
        m.setToolTipText("Zoom out"); pl.setToolTipText("Zoom in"); f.value.setToolTipText("Reset zoom");
        for (JLabel l : new JLabel[] { m, pl, f.value }) l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        m.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3)); pl.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        m.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { minus.run(); } });
        pl.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { plus.run(); } });
        f.value.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { reset.run(); } });
        f.add(m); f.add(f.value); f.add(pl);
        zoomBtns = new JLabel[] { m, pl }; applyTheme();
    }

    private void style(Field f) {
        Theme.Palette p = Theme.P;
        f.label.setFont(Theme.ui(Font.PLAIN, 10)); f.label.setForeground(p.muted);
        f.value.setFont(Theme.ui(Font.BOLD, 10)); f.value.setForeground(f.highlight ? (Theme.dark ? p.tag : p.accent) : p.text);
        f.setBorder(BorderFactory.createCompoundBorder(
                f.right ? BorderFactory.createMatteBorder(0, 1, 0, 0, p.statusSep) : BorderFactory.createMatteBorder(0, 0, 0, 1, p.statusSep),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
    }

    public void applyTheme() {
        Theme.Palette p = Theme.P;
        setBackground(p.status); setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, p.statusSep));
        for (Field f : fields.values()) style(f);
        if (zoomBtns.length == 2) { zoomBtns[0].setIcon(Icons.get("minus", p.muted)); zoomBtns[1].setIcon(Icons.get("plus", p.muted)); }
        revalidate(); repaint();
    }
}
