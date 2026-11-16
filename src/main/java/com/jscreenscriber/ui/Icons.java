package com.jscreenscriber.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Small 11px line icons (stand-ins for the mock's Material Symbols). Custom drawing: FlatLaf ships no icon set. */
public final class Icons {
    private Icons() {}

    public static Icon get(String name, Color c) { return new Ic(name, c, 11); }
    public static Icon get(String name, Color c, int size) { return new Ic(name, c, size); }

    private record Ic(String n, Color c, int s) implements Icon {
        public int getIconWidth() { return s; }
        public int getIconHeight() { return s; }
        public void paintIcon(Component comp, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(x, y); g.scale(s / 11.0, s / 11.0);
            g.setColor(c); g.setStroke(new BasicStroke(1.15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g);
            g.dispose();
        }
        void line(Graphics2D g, double a, double b, double c2, double d) { g.draw(new Line2D.Double(a, b, c2, d)); }
        void rect(Graphics2D g, double a, double b, double w, double h) { g.draw(new RoundRectangle2D.Double(a, b, w, h, 1.2, 1.2)); }
        void poly(Graphics2D g, boolean close, double... p) {
            Path2D.Double q = new Path2D.Double(); q.moveTo(p[0], p[1]);
            for (int i = 2; i < p.length; i += 2) q.lineTo(p[i], p[i + 1]);
            if (close) q.closePath(); g.draw(q);
        }
        void dot(Graphics2D g, double x, double y, double r) { g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r)); }
        void draw(Graphics2D g) {
            switch (n) {
                case "new" -> { poly(g, false, 2.5, 5.5, 2.5, 1, 6.5, 1, 8.5, 3, 8.5, 10, 2.5, 10, 2.5, 5.5); poly(g, false, 6.5, 1, 6.5, 3, 8.5, 3); }
                case "open" -> { poly(g, false, 1, 9.5, 1, 2.5, 4, 2.5, 5, 3.8, 9, 3.8, 9, 5); poly(g, false, 1, 9.5, 2.8, 5, 10, 5, 8.3, 9.5, 1, 9.5); }
                case "save" -> { poly(g, false, 1.5, 1.5, 8, 1.5, 9.5, 3, 9.5, 9.5, 1.5, 9.5, 1.5, 1.5); rect(g, 3.2, 1.5, 4, 2.6); rect(g, 3, 6, 5, 3.5); }
                case "print" -> { poly(g, false, 3, 4, 3, 1.2, 8, 1.2, 8, 4); rect(g, 1.2, 4, 8.6, 4); rect(g, 3, 7, 5, 3); }
                case "undo" -> { poly(g, false, 3.5, 2, 1.5, 4, 3.5, 6); g.draw(new Arc2D.Double(1.5, 3.8, 8, 5.4, 90, -180, Arc2D.OPEN)); line(g, 1.5, 4, 5.5, 4); }
                case "redo" -> { poly(g, false, 7.5, 2, 9.5, 4, 7.5, 6); g.draw(new Arc2D.Double(1.5, 3.8, 8, 5.4, 90, 180, Arc2D.OPEN)); line(g, 5.5, 4, 9.5, 4); }
                case "cut" -> { line(g, 3, 1, 7, 7); line(g, 8, 1, 4, 7); g.draw(new Ellipse2D.Double(1.6, 7, 2.8, 2.8)); g.draw(new Ellipse2D.Double(6.6, 7, 2.8, 2.8)); }
                case "copy" -> { rect(g, 3.5, 3.5, 6, 6.5); poly(g, false, 2.2, 7.5, 1.5, 7.5, 1.5, 1, 6.5, 1, 6.5, 2); }
                case "paste" -> { rect(g, 1.8, 2, 7.4, 8); rect(g, 3.6, 0.8, 3.8, 2.2); line(g, 3.6, 6, 7.4, 6); line(g, 3.6, 8, 6.2, 8); }
                case "spell" -> { poly(g, false, 1, 8, 3.2, 1.5, 5.4, 8); line(g, 1.9, 5.6, 4.5, 5.6); poly(g, false, 6.2, 6.2, 7.8, 8.4, 10.3, 3.6); }
                case "title" -> { line(g, 2, 2, 9, 2); line(g, 5.5, 2, 5.5, 9.5); }
                case "run" -> { dot(g, 6.8, 1.9, 1.1); poly(g, false, 4, 4, 6, 3.4, 7.4, 5.2, 9.2, 5.6); poly(g, false, 6, 3.4, 5.2, 6.2, 6.8, 7.6, 6.2, 10); poly(g, false, 5.2, 6.2, 3.2, 8.2, 1.8, 7.8); }
                case "person" -> { g.draw(new Ellipse2D.Double(3.5, 1, 4, 4)); g.draw(new Arc2D.Double(1.5, 6.2, 8, 7, 0, 180, Arc2D.OPEN)); }
                case "quote" -> { dot(g, 3.5, 4, 1.4); dot(g, 7.8, 4, 1.4); line(g, 3.5, 4.5, 2.6, 8); line(g, 7.8, 4.5, 6.9, 8); }
                case "chat" -> { poly(g, true, 1.5, 1.5, 9.5, 1.5, 9.5, 7, 5, 7, 2.5, 9.5, 2.5, 7, 1.5, 7); }
                case "trn" -> { poly(g, false, 1.5, 2, 5, 5.5, 1.5, 9); poly(g, false, 6, 2, 9.5, 5.5, 6, 9); }
                case "cam" -> { rect(g, 1, 3, 6.2, 5); poly(g, true, 7.2, 5, 10, 3.4, 10, 7.6, 7.2, 6); }
                case "minus" -> { g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); line(g, 2, 5.5, 9, 5.5); }
                case "plus" -> { g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); line(g, 2, 5.5, 9, 5.5); line(g, 5.5, 2, 5.5, 9); }
                case "close" -> { g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); line(g, 2, 2, 9, 9); line(g, 9, 2, 2, 9); }
                case "chevup" -> { g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); poly(g, false, 2, 7, 5.5, 3.5, 9, 7); }
                case "chevdown" -> { g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); poly(g, false, 2, 4, 5.5, 7.5, 9, 4); }
                case "pdf" -> { poly(g, false, 2.5, 5.5, 2.5, 1, 6.5, 1, 8.5, 3, 8.5, 5.5); poly(g, false, 6.5, 1, 6.5, 3, 8.5, 3); line(g, 5.5, 5.5, 5.5, 10); poly(g, false, 3.6, 8.2, 5.5, 10, 7.4, 8.2); }
                case "moon" -> { Path2D.Double q = new Path2D.Double(); q.moveTo(8.5, 7.5); q.curveTo(4, 9, 1.5, 5, 4, 1.5); q.curveTo(4.5, 5.5, 7, 7, 8.5, 7.5); g.draw(q); }
                case "sun" -> { g.draw(new Ellipse2D.Double(3.2, 3.2, 4.6, 4.6)); for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; line(g, 5.5 + 3.4 * Math.cos(a), 5.5 + 3.4 * Math.sin(a), 5.5 + 4.8 * Math.cos(a), 5.5 + 4.8 * Math.sin(a)); } }
                case "bold" -> { g.setStroke(new BasicStroke(1.7f)); poly(g, false, 3, 1.5, 3, 9.5); poly(g, false, 3, 1.5, 6.3, 1.5, 7.2, 2.4, 7.2, 4, 6.3, 5, 3, 5); poly(g, false, 3, 5, 6.8, 5, 8, 6.2, 8, 8.3, 6.8, 9.5, 3, 9.5); }
                case "settings" -> { g.draw(new Ellipse2D.Double(3.5, 3.5, 4, 4)); for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; line(g, 5.5 + 3 * Math.cos(a), 5.5 + 3 * Math.sin(a), 5.5 + 4.6 * Math.cos(a), 5.5 + 4.6 * Math.sin(a)); } }
                default -> rect(g, 1.5, 1.5, 8, 8);
            }
        }
    }
}
