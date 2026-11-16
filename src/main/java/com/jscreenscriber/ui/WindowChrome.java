package com.jscreenscriber.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Edge-resize + drag + maximize support for undecorated windows (custom title bar).
 * Custom code is unavoidable here: FlatLaf's own decorations cannot host the mock's chip/badge title bar content.
 */
public final class WindowChrome {
    private WindowChrome() {}
    private static final int EDGE = 5;

    public static void install(Window w, JComponent dragArea, Runnable toggleMax) {
        final Point[] start = { null };
        final Point[] winStart = { null };
        dragArea.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { start[0] = e.getLocationOnScreen(); winStart[0] = w.getLocation(); }
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && toggleMax != null) toggleMax.run(); }
        });
        dragArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (start[0] == null || (w instanceof Frame f && (f.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0)) return;
                Point p = e.getLocationOnScreen();
                w.setLocation(winStart[0].x + p.x - start[0].x, winStart[0].y + p.y - start[0].y);
            }
        });
    }

    /**
     * Wraps the content in a frame of real edge components (4px) that carry resize cursors and drag logic.
     * Real components get their own mouse events reliably on every side (a global listener did not on left/bottom).
     */
    public static JPanel wrapResizable(Window win, JComponent content, Dimension min) {
        final int T = 4, C = 12;
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(com.jscreenscriber.Theme.P.border); g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        outer.setBackground(com.jscreenscriber.Theme.P.win);
        java.util.List<JComponent> edges = new java.util.ArrayList<>();
        JPanel north = new JPanel(new BorderLayout()), south = new JPanel(new BorderLayout());
        north.setOpaque(false); south.setOpaque(false);
        north.add(edge(win, min, 1 | 4, C, T, edges), BorderLayout.WEST); north.add(edge(win, min, 4, 1, T, edges), BorderLayout.CENTER); north.add(edge(win, min, 2 | 4, C, T, edges), BorderLayout.EAST);
        south.add(edge(win, min, 1 | 8, C, T, edges), BorderLayout.WEST); south.add(edge(win, min, 8, 1, T, edges), BorderLayout.CENTER); south.add(edge(win, min, 2 | 8, C, T, edges), BorderLayout.EAST);
        outer.add(north, BorderLayout.NORTH); outer.add(south, BorderLayout.SOUTH);
        outer.add(edge(win, min, 1, T, 1, edges), BorderLayout.WEST); outer.add(edge(win, min, 2, T, 1, edges), BorderLayout.EAST);
        outer.add(content, BorderLayout.CENTER);
        outer.putClientProperty("edges", edges); outer.putClientProperty("edgePanels", new JComponent[] { north, south });
        return outer;
    }

    /** Show/hide the resize edges (hidden while maximised). */
    @SuppressWarnings("unchecked")
    public static void setEdgesVisible(JPanel outer, boolean on) {
        for (JComponent c : (java.util.List<JComponent>) outer.getClientProperty("edges")) c.setVisible(on);
        for (JComponent c : (JComponent[]) outer.getClientProperty("edgePanels")) c.setVisible(on);
        outer.revalidate();
    }

    private static JComponent edge(Window win, Dimension min, int mode, int w, int h, java.util.List<JComponent> list) {
        JComponent c = new JComponent() {};
        c.setPreferredSize(new Dimension(w, h)); c.setOpaque(false);
        c.setCursor(Cursor.getPredefinedCursor(cursorFor(mode)));
        final Point[] press = { null }; final Rectangle[] orig = { null };
        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { press[0] = e.getLocationOnScreen(); orig[0] = win.getBounds(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (press[0] == null) return;
                Point s = e.getLocationOnScreen(); int dx = s.x - press[0].x, dy = s.y - press[0].y;
                Rectangle r = new Rectangle(orig[0]);
                if ((mode & 1) != 0) { r.x += dx; r.width -= dx; }
                if ((mode & 2) != 0) r.width += dx;
                if ((mode & 4) != 0) { r.y += dy; r.height -= dy; }
                if ((mode & 8) != 0) r.height += dy;
                if (r.width < min.width) { if ((mode & 1) != 0) r.x -= min.width - r.width; r.width = min.width; }
                if (r.height < min.height) { if ((mode & 4) != 0) r.y -= min.height - r.height; r.height = min.height; }
                win.setBounds(r);
            }
        };
        c.addMouseListener(m); c.addMouseMotionListener(m);
        list.add(c); return c;
    }

    private static int cursorFor(int m) {
        return switch (m) {
            case 1 -> Cursor.W_RESIZE_CURSOR; case 2 -> Cursor.E_RESIZE_CURSOR; case 4 -> Cursor.N_RESIZE_CURSOR; case 8 -> Cursor.S_RESIZE_CURSOR;
            case 5 -> Cursor.NW_RESIZE_CURSOR; case 6 -> Cursor.NE_RESIZE_CURSOR; case 9 -> Cursor.SW_RESIZE_CURSOR; case 10 -> Cursor.SE_RESIZE_CURSOR;
            default -> Cursor.DEFAULT_CURSOR;
        };
    }
}
