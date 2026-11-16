package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;

import javax.swing.*;
import java.awt.*;

/** The "----+----10---+----20" text ruler, aligned to the page's text columns and scroll offset. */
public class RulerPanel extends JComponent implements W.Themed {
    private final PageView view; private final JViewport vp;
    public RulerPanel(PageView v, JViewport vp) {
        this.view = v; this.vp = vp; setPreferredSize(new Dimension(10, 21));
        vp.addChangeListener(e -> repaint());
    }
    public void applyTheme() { repaint(); }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.Palette p = Theme.P;
        g.setColor(p.menubar); g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(p.border); g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double cw = view.cellWidth(), x0 = view.textLeft() - vp.getViewPosition().x;
        int cols = 70;
        char[] r = new char[cols]; java.util.Arrays.fill(r, '-');
        for (int c = 5; c <= cols; c += 10) r[c - 1] = '+';
        for (int c = 10; c <= cols; c += 10) { String s = String.valueOf(c); r[c - 1] = s.charAt(0); if (c < cols) r[c] = s.charAt(1); }
        int caretCol = view.caretColumn() - 1;
        Font f = Theme.mono(Font.PLAIN, (float) view.fontPx());
        g.setFont(f); FontMetrics fm = g.getFontMetrics();
        int base = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        for (int i = 0; i < cols; i++) {
            double x = x0 + i * cw;
            if (i == caretCol) { g.setColor(p.accentBg); g.fillRect((int) x, 3, (int) Math.ceil(cw), getHeight() - 7); }
            g.setColor(i == caretCol ? p.accentDark : p.muted);
            g.drawString(String.valueOf(r[i]), (float) x, base);
        }
        g.dispose();
    }
}
