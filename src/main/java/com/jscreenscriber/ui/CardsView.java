package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;
import com.jscreenscriber.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Index-card board of scenes (CARDS toggle). Click a card to jump to the scene; ◄ ► reorder scenes. */
public class CardsView extends JPanel implements W.Themed, Scrollable {
    private ScriptDocument doc; private Layout layout; private IntConsumer onPick = b -> {}; private java.util.function.BiConsumer<Integer, Integer> onMove = (a, b) -> {};
    private final List<Rectangle> rects = new ArrayList<>(), left = new ArrayList<>(), right = new ArrayList<>();
    private static final int CW = 176, CH = 112, G = 12;

    public CardsView() {
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                for (int i = 0; i < rects.size(); i++) {
                    if (left.get(i).contains(e.getPoint()) && i > 0) { onMove.accept(i, i - 1); return; }
                    if (right.get(i).contains(e.getPoint()) && i < rects.size() - 1) { onMove.accept(i, i + 1); return; }
                    if (rects.get(i).contains(e.getPoint())) { onPick.accept(layout.scenes.get(i).block()); return; }
                }
            }
        });
    }
    public void setOnPick(IntConsumer c) { onPick = c; }
    public void setOnMove(java.util.function.BiConsumer<Integer, Integer> c) { onMove = c; }
    public void show(ScriptDocument d, Layout l) { doc = d; layout = l; revalidate(); repaint(); }
    public void applyTheme() { repaint(); }

    private int perRow() { return Math.max(1, (getParent() == null ? 600 : getParent().getWidth() - G) / (CW + G)); }
    @Override public Dimension getPreferredSize() {
        int n = layout == null ? 0 : layout.scenes.size(); int rows = (n + perRow() - 1) / perRow();
        return new Dimension(100, G + rows * (CH + G));
    }
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create(); Theme.Palette p = Theme.P;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(p.viewport); g.fillRect(0, 0, getWidth(), getHeight());
        rects.clear(); left.clear(); right.clear();
        if (layout == null) return;
        int per = perRow(), x0 = (getWidth() - (per * (CW + G) - G)) / 2;
        for (int i = 0; i < layout.scenes.size(); i++) {
            var si = layout.scenes.get(i);
            int cx = x0 + (i % per) * (CW + G), cy = G + (i / per) * (CH + G);
            Rectangle r = new Rectangle(cx, cy, CW, CH); rects.add(r);
            g.setColor(new Color(0, 0, 0, Theme.dark ? 60 : 20)); g.fillRoundRect(cx + 1, cy + 2, CW, CH, 4, 4);
            g.setColor(p.page); g.fillRoundRect(cx, cy, CW, CH, 4, 4);
            g.setColor(p.pageBorder); g.drawRoundRect(cx, cy, CW, CH, 4, 4);
            g.setColor(p.sideHeader); g.fillRect(cx + 1, cy + 1, CW - 1, 20);
            g.setColor(p.border); g.drawLine(cx, cy + 21, cx + CW, cy + 21);
            g.setFont(Theme.ui(Font.BOLD, 10)); g.setColor(p.accentDark);
            g.drawString((i + 1) + ".", cx + 8, cy + 15);
            g.setFont(Theme.ui(Font.PLAIN, 9)); g.setColor(p.muted);
            String len = "p." + (si.page() + 1) + "  [" + si.lengthLabel() + "]";
            g.drawString(len, cx + CW - 8 - g.getFontMetrics().stringWidth(len), cy + 15);
            String head = doc.blocks.get(si.block()).text; if (head.isEmpty()) head = "(untitled scene)";
            g.setFont(Theme.mono(Font.BOLD, 9.5f)); g.setColor(p.sceneC);
            g.setClip(cx + 6, cy + 24, CW - 12, 14); g.drawString(head, cx + 8, cy + 35); g.setClip(null);
            // summary: first synopsis / action text
            String sum = ""; for (int b = si.block() + 1; b < doc.blocks.size() && doc.blocks.get(b).type != ElementType.SCENE; b++) {
                Block bl = doc.blocks.get(b); if (bl.type == ElementType.ACTION || bl.type == ElementType.SYNOPSIS || bl.type == ElementType.DIALOGUE) { sum = bl.text; break; } }
            g.setFont(Theme.ui(Font.PLAIN, 10)); g.setColor(p.text);
            int ty = cy + 52; FontMetrics fm = g.getFontMetrics(); String[] words = sum.replace('\n', ' ').split(" "); StringBuilder ln = new StringBuilder(); int lines = 0;
            for (String w : words) {
                if (fm.stringWidth(ln + " " + w) > CW - 16 && ln.length() > 0) { g.drawString(ln.toString(), cx + 8, ty); ty += 13; ln.setLength(0); if (++lines >= 3) { ln.append("\u2026"); break; } }
                if (ln.length() > 0 && !ln.toString().endsWith("\u2026")) ln.append(' '); ln.append(w);
            }
            if (ln.length() > 0 && lines < 4) g.drawString(ln.toString(), cx + 8, ty);
            // reorder arrows
            Rectangle lb = new Rectangle(cx + CW - 40, cy + CH - 18, 16, 14), rb = new Rectangle(cx + CW - 22, cy + CH - 18, 16, 14);
            left.add(lb); right.add(rb);
            g.setFont(Theme.ui(Font.BOLD, 9)); g.setColor(p.muted);
            g.drawString("\u25C4", lb.x + 3, lb.y + 11); g.drawString("\u25BA", rb.x + 3, rb.y + 11);
        }
        g.dispose();
    }
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 30; }
    public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 30; }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
}
