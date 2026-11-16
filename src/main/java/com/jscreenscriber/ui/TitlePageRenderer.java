package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;
import com.jscreenscriber.model.*;

import java.awt.*;
import java.awt.geom.Line2D;

/** Lays out a title page onto a {@link Sink}; used by the designer preview, printing and PDF export (ppi = units per inch). */
public final class TitlePageRenderer {
    private TitlePageRenderer() {}

    /** Drawing target. y is a baseline measured downward from the top of the page. */
    public interface Sink {
        double width(String s, double size, boolean bold);
        void text(String s, double x, double y, double size, boolean bold);
        void line(double x1, double y1, double x2, double y2);
    }

    public static void paint(Graphics2D g, TitlePage t, Paper paper, double ppi, Color fg) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(fg);
        layout(new Sink() {
            public double width(String s, double size, boolean bold) { return g.getFontMetrics(Theme.mono(bold ? Font.BOLD : Font.PLAIN, (float) size)).stringWidth(s); }
            public void text(String s, double x, double y, double size, boolean bold) { g.setFont(Theme.mono(bold ? Font.BOLD : Font.PLAIN, (float) size)); g.drawString(s, (float) x, (float) y); }
            public void line(double x1, double y1, double x2, double y2) { g.draw(new Line2D.Double(x1, y1, x2, y2)); }
        }, t, paper, ppi);
    }

    public static void layout(Sink k, TitlePage t, Paper paper, double ppi) {
        double W = paper.wIn * ppi, H = paper.hIn * ppi, lm = Paper.LEFT * ppi, rm = W - Paper.RIGHT * ppi, cx = (lm + rm) / 2;
        double lh = ppi / 6.0, size = ppi / 6.0;   // 12pt on a 72-ppi page
        boolean minimal = t.preset.equals("Minimal"), centered = t.preset.equals("Centered");
        double scale = switch (t.titleSize) { case 1 -> 1.0; case 3 -> 1.6; default -> 1.25; };

        double y = H * 0.30;
        String title = t.get("Title");
        if (!title.isEmpty()) {
            for (String ln : (t.titleCaps ? title.toUpperCase() : title).split("\n")) {
                double w = k.width(ln, size * scale, t.titleBold);
                double x = minimal ? lm : cx - w / 2;
                k.text(ln, x, y, size * scale, t.titleBold);
                if (t.titleUnderline) k.line(x, y + size * 0.15, x + w, y + size * 0.15);
                y += lh * scale;
            }
            y += lh * 2;
        }
        y = stack(k, t.get("Credit"), minimal ? lm : cx, y, minimal ? 0 : 1, lh, size) + lh;
        y = stack(k, t.get("Author"), minimal ? lm : cx, y, minimal ? 0 : 1, lh, size) + lh;
        stack(k, t.get("Source"), minimal ? lm : cx, y, minimal ? 0 : 1, lh, size);

        double bottom = H - Paper.BOTTOM * ppi;
        String contact = join(t.get("Contact"), t.get("Copyright")), date = join(t.get("Draft date"), t.get("Notes"));
        if (centered) {
            block(k, join(join(t.get("Contact"), t.get("Draft date")), join(t.get("Copyright"), t.get("Notes"))), cx, bottom, 1, lh, size);
        } else if (minimal) {
            block(k, join(contact, date), lm, bottom, 0, lh, size);
        } else {
            boolean contactLeft = !t.contactSide.equals("right");
            block(k, contact, contactLeft ? lm : rm, bottom, contactLeft ? 0 : 2, lh, size);
            if (!t.dateSide.equals("none")) {
                boolean dateRight = t.dateSide.equals("right");
                block(k, date, dateRight ? rm : lm, bottom, dateRight ? 2 : 0, lh, size);
            }
        }
    }

    private static String join(String a, String b) { return a.isEmpty() ? b : b.isEmpty() ? a : a + "\n" + b; }

    /** align 0=left 1=centre 2=right; returns y of the last line drawn. */
    private static double stack(Sink k, String text, double x, double y, int align, double lh, double size) {
        if (text.isEmpty()) return y - lh;
        for (String ln : text.split("\n")) {
            double w = k.width(ln, size, false);
            k.text(ln, align == 1 ? x - w / 2 : align == 2 ? x - w : x, y, size, false); y += lh;
        }
        return y - lh;
    }
    private static void block(Sink k, String text, double x, double bottom, int align, double lh, double size) {
        if (text.isEmpty()) return;
        stack(k, text, x, bottom - (text.split("\n").length - 1) * lh, align, lh, size);
    }
}
