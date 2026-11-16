package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;

import javax.swing.*;
import java.awt.*;

/** Tiny label/field grid builder for dialogs. */
public final class Form extends JPanel implements W.Themed {
    private int row;
    public Form() { super(new GridBagLayout()); setOpaque(false); setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12)); }

    public void header(String text) {
        JLabel l = W.label(text.toUpperCase(), Font.BOLD, 10); l.setName("hdr");
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row++; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(row == 1 ? 0 : 12, 0, 4, 0); c.fill = GridBagConstraints.HORIZONTAL; add(l, c);
        JSeparator s = new JSeparator(); c = new GridBagConstraints(); c.gridx = 0; c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 4, 0); add(s, c);
    }
    public void row(String label, JComponent field) { row(label, field, false); }
    public void row(String label, JComponent field, boolean grow) {
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.insets = new Insets(3, 0, 3, 8); c.anchor = GridBagConstraints.NORTHWEST;
        if (label != null) { c.gridx = 0; JLabel l = W.label(label, Font.PLAIN, 10); l.setName("lbl"); l.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0)); add(l, c); }
        c.gridx = 1; c.weightx = 1; c.fill = grow ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE; c.insets = new Insets(3, 0, 3, 0);
        if (label == null) { c.gridx = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; }
        add(field, c); row++;
    }
    public void glue() {
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row++; c.weighty = 1; c.gridx = 0; c.gridwidth = 2; add(Box.createGlue(), c);
    }
    public void applyTheme() {
        for (Component k : getComponents()) if (k instanceof JLabel l) {
            l.setFont(Theme.ui(l.getName() != null && l.getName().equals("hdr") ? Font.BOLD : Font.PLAIN, 10));
            l.setForeground("hdr".equals(l.getName()) ? Theme.P.muted : Theme.P.text);
        }
    }
    public static JCheckBox check(String t, boolean v) { JCheckBox c = new JCheckBox(t, v); c.setOpaque(false); c.setFont(Theme.ui(Font.PLAIN, 10)); c.setFocusable(false); return c; }
    public static JTextField field(String v, int cols) { JTextField f = new JTextField(v, cols); f.setFont(Theme.ui(Font.PLAIN, 10)); return f; }
    public static JSpinner spinner(int v, int lo, int hi, int step) { JSpinner s = new JSpinner(new SpinnerNumberModel(v, lo, hi, step)); s.setFont(Theme.ui(Font.PLAIN, 10)); return s; }
    public static <T> JComboBox<T> combo(T[] items, T sel) { JComboBox<T> c = new JComboBox<>(items); c.setSelectedItem(sel); c.setFont(Theme.ui(Font.PLAIN, 10)); c.setFocusable(false); return c; }
    public static JScrollPane area(JTextArea a, int rows) {
        a.setRows(rows); a.setLineWrap(true); a.setWrapStyleWord(true); a.setFont(Theme.ui(Font.PLAIN, 10)); a.setMargin(new Insets(3, 5, 3, 5));
        JScrollPane sp = new JScrollPane(a); sp.setVerticalScrollBar(new GripScrollBar(JScrollBar.VERTICAL)); return sp;
    }
}
