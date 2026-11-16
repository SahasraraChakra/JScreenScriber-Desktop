package com.jscreenscriber.ui;

import com.formdev.flatlaf.ui.FlatScrollBarUI;
import com.jscreenscriber.Theme;

import javax.swing.*;
import java.awt.*;

/** 12px scrollbar with arrow steppers (FlatLaf) and the mock's three-dot grip on the thumb (custom). */
public class GripScrollBar extends JScrollBar {
    public GripScrollBar(int orientation) { super(orientation); setUnitIncrement(16); }

    @Override public void updateUI() {
        setUI(new FlatScrollBarUI() {
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                super.paintThumb(g, c, r);
                if (r.width < 8 || r.height < 8) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.dark ? Theme.P.text : Color.WHITE);
                boolean vert = getOrientation() == VERTICAL;
                int cx = r.x + r.width / 2, cy = r.y + r.height / 2;
                if (r.height >= 14 || !vert) for (int i = -1; i <= 1; i++) { if (vert) g2.fillRect(cx - 1, cy + i * 3 - 1, 2, 2); else g2.fillRect(cx + i * 3 - 1, cy - 1, 2, 2); }
                g2.dispose();
            }
        });
    }
}
