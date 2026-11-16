package com.jscreenscriber.ui;

import com.jscreenscriber.Settings;
import com.jscreenscriber.Theme;
import com.jscreenscriber.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

/** Paginated page-view screenplay editor (custom component: Swing has no paged text view). */
public class PageView extends JComponent implements Scrollable, W.Themed {
    static final int GAP = 16, PAD = 10;

    private ScriptDocument doc;
    private Layout layout;
    private int zoom = 100;
    private int cb, co, ab, ao, prefCol = -1;
    private boolean overwrite, blinkOn = true, focusMode, spellOn;
    private final javax.swing.Timer blink;
    private Runnable caretListener = () -> {};
    private Set<String> dict = Collections.emptySet();
    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z']+");

    public PageView(ScriptDocument d) {
        setFocusable(true); setOpaque(true); setFocusTraversalKeysEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        blink = new javax.swing.Timer(500, e -> { blinkOn = !blinkOn; repaint(caretRect()); });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { blink.start(); blinkOn = true; repaint(); }
            @Override public void focusLost(FocusEvent e) { blink.stop(); repaint(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKey(e); }
            @Override public void keyTyped(KeyEvent e) { onTyped(e); }
        });
        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onPress(e); }
            @Override public void mouseDragged(MouseEvent e) { onDrag(e); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) { setZoom(zoom + (e.getWheelRotation() < 0 ? 10 : -10)); fireZoom(); }
                else getParent().dispatchEvent(SwingUtilities.convertMouseEvent(PageView.this, e, getParent()));
            }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) popup(e); }
        };
        addMouseListener(m); addMouseMotionListener(m); addMouseWheelListener(m);
        setDocument(d);
    }

    // ───────────── configuration ─────────────
    public void setDocument(ScriptDocument d) {
        this.doc = d; cb = co = ab = ao = 0; d.addListener(this::docChanged);
        relayout(); scrollCaret();
    }
    public ScriptDocument document() { return doc; }
    public Layout pages() { return layout; }
    public void setCaretListener(Runnable r) { caretListener = r; }
    public int zoom() { return zoom; }
    private Runnable zoomListener = () -> {};
    public void setZoomListener(Runnable r) { zoomListener = r; }
    private void fireZoom() { zoomListener.run(); }
    public void setZoom(int z) {
        zoom = Math.max(50, Math.min(250, z));
        revalidate(); repaint(); caretListener.run();
    }
    /** Zoom percentage at which the page exactly fills the given width (px). */
    public int fitWidthZoom(int availablePx) {
        double cell100 = Theme.mono(Font.PLAIN, Settings.get().scriptFontPx).getStringBounds("M", new java.awt.font.FontRenderContext(null, true, true)).getWidth();
        double page100 = doc.paper.wIn * cell100 * 10;
        return (int) Math.max(50, Math.min(250, Math.floor((availablePx - 28) / page100 * 100 / 5) * 5));
    }
    public void setFocusMode(boolean f) { focusMode = f; repaint(); }
    public boolean overwrite() { return overwrite; }
    public void toggleOverwrite() { overwrite = !overwrite; caretListener.run(); }
    public void setSpell(boolean on) { spellOn = on; if (on) loadDictionary(); repaint(); }
    public boolean dictionaryLoaded() { return !dict.isEmpty(); }
    public void applyTheme() { revalidate(); repaint(); }

    private void loadDictionary() {
        if (!dict.isEmpty()) return;
        Set<String> s = new HashSet<>();
        try {
            String cfg = Settings.get().dictionary;
            java.io.InputStream in = null;
            if (cfg != null && !cfg.isBlank() && Files.exists(Paths.get(cfg))) in = Files.newInputStream(Paths.get(cfg));
            else in = PageView.class.getResourceAsStream("/dict/en.txt");
            if (in == null && Files.exists(Paths.get("/usr/share/dict/words"))) in = Files.newInputStream(Paths.get("/usr/share/dict/words"));
            if (in != null) try (var r = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                String l; while ((l = r.readLine()) != null) s.add(l.trim().toLowerCase());
            }
        } catch (IOException ignored) {}
        dict = s;
    }

    // ───────────── geometry ─────────────
    private double cachedZoomKey = -1, cachedCell; private String cachedFamily = "";
    /** Script font size in px at the current zoom (base size from Settings, default 14px). */
    public double fontPx() { return Settings.get().scriptFontPx * zoom / 100.0; }
    /** Advance of one character cell: 10 cells == 1 inch, so the page scales with the real font. */
    private double cellW() {
        double key = fontPx(); Font f = Theme.mono(Font.PLAIN, (float) key);
        if (key != cachedZoomKey || !f.getFamily().equals(cachedFamily)) {
            cachedCell = f.getStringBounds("M", new java.awt.font.FontRenderContext(null, true, true)).getWidth();
            cachedZoomKey = key; cachedFamily = f.getFamily();
        }
        return cachedCell;
    }
    private double pxIn() { return cellW() * 10.0; }
    private double pitch() { return pxIn() / 6.0; }
    private double pageW() { return doc.paper.wIn * pxIn(); }
    private double pageH() { return doc.paper.hIn * pxIn(); }
    private double pageX() { return Math.max(12, (getWidth() - pageW()) / 2.0); }
    private double pageY(int i) { return PAD + i * (pageH() + GAP); }
    private double textX(int pg) { return pageX() + Paper.LEFT * pxIn(); }
    private double textY(int pg) { return pageY(pg) + Paper.TOP * pxIn(); }
    /** x of the text area's left edge relative to the component (for the ruler). */
    public double textLeft() { return pageX() + Paper.LEFT * pxIn(); }
    public double cellWidth() { return cellW(); }

    @Override public Dimension getPreferredSize() {
        int n = layout == null ? 1 : layout.pageCount();
        return new Dimension((int) (pageW() + 24), (int) (2 * PAD + n * (pageH() + GAP)));
    }

    private Font font(int bits, boolean italicDefault, boolean boldDefault) {
        int st = Font.PLAIN;
        if ((bits & Layout.BOLD) != 0 || boldDefault) st |= Font.BOLD;
        if ((bits & Layout.ITALIC) != 0 || italicDefault) st |= Font.ITALIC;
        return Theme.mono(st, (float) fontPx());
    }

    // ───────────── layout ─────────────
    private void docChanged() { relayout(); }
    /** Re-run layout after margin / element-style / font settings change. */
    public void relayoutNow() { cachedZoomKey = -1; for (Block b : doc.blocks) b.cacheW = -2; relayout(); }
    private void relayout() {
        layout = Layout.build(doc, true);
        clamp(); revalidate(); repaint(); caretListener.run();
    }
    private void clamp() {
        cb = Math.max(0, Math.min(doc.blocks.size() - 1, cb)); co = Math.max(0, Math.min(doc.blocks.get(cb).text.length(), co));
        ab = Math.max(0, Math.min(doc.blocks.size() - 1, ab)); ao = Math.max(0, Math.min(doc.blocks.get(ab).text.length(), ao));
    }

    // ───────────── caret helpers ─────────────
    public int caretBlock() { return cb; }
    public int caretOffset() { return co; }
    public Block currentBlock() { return doc.blocks.get(cb); }
    public Layout.Line caretLine() { int i = layout.lineIndex(cb, co); return i < 0 ? null : layout.all.get(i); }
    public int caretLineNumber() { return layout.lineIndex(cb, co) + 1; }
    public int caretColumn() { Layout.Line l = caretLine(); return l == null ? 1 : l.col + (co - l.start) + 1; }
    public int caretPage() { Layout.Line l = caretLine(); return l == null ? 1 : l.page + 1; }
    private boolean hasSel() { return ab != cb || ao != co; }

    public void moveTo(int b, int o, boolean extend) {
        cb = b; co = o; clamp();
        if (!extend) { ab = cb; ao = co; }
        blinkOn = true; repaint(); scrollCaret(); caretListener.run();
    }
    public void jumpToBlock(int b) { moveTo(b, 0, false); requestFocusInWindow(); }

    private Rectangle caretRect() {
        if (layout == null) return new Rectangle(0, 0, 1, 1);
        Layout.Line l = caretLine(); if (l == null) return new Rectangle(0, 0, 1, 1);
        double x = textX(l.page) + (l.col + co - l.start) * cellW(), y = textY(l.page) + l.row * pitch();
        return new Rectangle((int) x - 2, (int) y - 2, (int) cellW() + 4, (int) pitch() + 4);
    }
    private void scrollCaret() {
        Rectangle r = caretRect();
        r.grow(0, (int) (pitch() * 3)); scrollRectToVisible(r);
    }

    // ───────────── painting ─────────────
    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.Palette p = Theme.P;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(p.viewport); g.fillRect(0, 0, getWidth(), getHeight());
        Rectangle clip = g.getClipBounds();
        int n = layout.pageCount();
        double pw = pageW(), ph = pageH(), px = pageX();
        int first = Math.max(0, (int) ((clip.y - PAD) / (ph + GAP))), last = Math.min(n - 1, (int) ((clip.y + clip.height - PAD) / (ph + GAP)));
        Settings st = Settings.get();
        Layout.Line cl = caretLine();
        int selS = 0, selE = 0;
        for (int pi = first; pi <= last; pi++) {
            double py = pageY(pi);
            g.setColor(new Color(0, 0, 0, Theme.dark ? 60 : 22)); g.fill(new Rectangle2D.Double(px + 1, py + 3, pw, ph));
            g.setColor(p.page); g.fill(new Rectangle2D.Double(px, py, pw, ph));
            g.setColor(p.pageBorder); g.draw(new Rectangle2D.Double(px, py, pw, ph));
            if (st.printPageNumbers && (pi > 0 || st.numberFirstPage)) {
                g.setFont(font(0, false, true)); g.setColor(p.pageNum);
                String s = (pi + 1) + ".";
                g.drawString(s, (float) (px + pw - Paper.RIGHT * pxIn() - s.length() * cellW()), (float) (py + Math.min(0.5, Paper.TOP / 2) * pxIn() + g.getFontMetrics().getAscent() * 0.4));
            }
            double tx = textX(pi), ty = textY(pi);
            for (Layout.Line l : layout.pages.get(pi).lines) {
                double y = ty + l.row * pitch(), x = tx + l.col * cellW();
                if (y + pitch() < clip.y || y > clip.y + clip.height) continue;
                boolean dimmed = focusMode && l.block != cb;
                Composite oc = g.getComposite();
                if (dimmed) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                // selection
                if (hasSel() && inSelection(l)) {
                    int[] r = selRange(l);
                    g.setColor(p.selection);
                    g.fill(new Rectangle2D.Double(x + (r[0] - l.start) * cellW(), y, Math.max(cellW() * 0.5, (r[1] - r[0]) * cellW()), pitch()));
                }
                drawLine(g, l, x, y, p);
                if (st.sceneNumbers && l.first && l.type == ElementType.SCENE) drawSceneNumber(g, l, tx, y, p);
                g.setComposite(oc);
            }
            // caret
            if (cl != null && cl.page == pi && hasFocus() && blinkOn) {
                double x = tx + (cl.col + co - cl.start) * cellW(), y = ty + cl.row * pitch();
                g.setColor(p.cursor);
                g.fill(new Rectangle2D.Double(x, y + pitch() * 0.08, Math.max(2, cellW()), pitch() * 0.92));
                if (co < cl.end && co - cl.start < cl.text.length()) {
                    g.setFont(font(cl.style[co - cl.start], Styles.of(cl.type).italic, bold(cl.type))); g.setColor(p.page);
                    drawCell(g, cl.text.charAt(co - cl.start), x, y);
                }
            }
        }
        g.dispose();
    }

    private boolean bold(ElementType t) { return Styles.of(t).bold; }

    private void drawCell(Graphics2D g, char ch, double x, double y) {
        FontMetrics fm = g.getFontMetrics();
        float by = (float) (y + (pitch() - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent());
        g.drawString(String.valueOf(ch), (float) x, by);
    }

    private void drawLine(Graphics2D g, Layout.Line l, double x, double y, Theme.Palette p) {
        Color base = switch (l.type) {
            case SCENE -> p.sceneC; case CHARACTER -> p.charC; case PAREN -> p.parenC; case DIALOGUE -> p.dialogC;
            case TRANSITION, SHOT -> p.transC; case ACTION, CENTERED -> p.actionC;
            case PAGE_BREAK -> p.muted; default -> p.noteC;
        };
        boolean italic = Styles.of(l.type).italic || !l.type.printed;
        boolean bold = bold(l.type);
        if (l.type == ElementType.SHOT) base = p.sceneC.equals(p.text) ? p.text : p.sceneC;
        int len = l.text.length();
        Font lastFont = null;
        for (int i = 0; i < len; i++) {
            char ch = l.text.charAt(i);
            double cx = x + i * cellW();
            if (ch == ' ') continue;
            byte s = l.style[i];
            Font f = font(s, italic, bold);
            if (f != lastFont) { g.setFont(f); lastFont = f; }
            g.setColor((s & Layout.MARK) != 0 ? p.muted : base);
            drawCell(g, ch, cx, y);
        }
        // underline runs
        boolean styleUnder = Styles.of(l.type).underline;
        for (int i = 0; i < len; i++) if (((l.style[i] & Layout.UNDER) != 0 || styleUnder) && (l.style[i] & Layout.MARK) == 0 && l.text.charAt(i) != ' ') {
            g.setColor(base); g.fill(new Rectangle2D.Double(x + i * cellW(), y + pitch() * 0.92, cellW(), 1));
        }
        if (spellOn && !dict.isEmpty() && (l.type == ElementType.ACTION || l.type == ElementType.DIALOGUE || l.type == ElementType.PAREN)) {
            Matcher m = WORD.matcher(l.text);
            while (m.find()) {
                String w = m.group().toLowerCase().replaceAll("^'+|'+$", "");
                if (w.length() < 3 || dict.contains(w) || (w.endsWith("'s") && dict.contains(w.substring(0, w.length() - 2)))) continue;
                if (m.group().equals(m.group().toUpperCase())) continue;
                g.setColor(new Color(0xE0, 0x3E, 0x3E));
                double sx = x + m.start() * cellW(), ex = x + m.end() * cellW(), yy = y + pitch() * 0.97;
                for (double xx = sx; xx < ex; xx += 2) g.draw(new java.awt.geom.Line2D.Double(xx, yy - ((int) (xx / 2) % 2 == 0 ? 0 : 1), xx + 2, yy - ((int) (xx / 2) % 2 == 0 ? 1 : 0)));
            }
        }
    }

    private void drawSceneNumber(Graphics2D g, Layout.Line l, double tx, double y, Theme.Palette p) {
        Block b = doc.blocks.get(l.block);
        String num = b.sceneNum != null ? b.sceneNum : String.valueOf(layout.sceneIndexOfBlock(l.block) + 1);
        g.setFont(font(0, false, false)); g.setColor(p.muted);
        double w = (num.length() + 1) * cellW();
        drawStr(g, num + ".", tx - w - cellW(), y);
        double rx = tx + doc.paper.cols() * cellW() + cellW();
        drawStr(g, num + ".", rx, y);
    }
    private void drawStr(Graphics2D g, String s, double x, double y) { for (int i = 0; i < s.length(); i++) drawCell(g, s.charAt(i), x + i * cellW(), y); }

    // selection helpers
    private int[] ordered() {
        if (ab < cb || (ab == cb && ao <= co)) return new int[] { ab, ao, cb, co };
        return new int[] { cb, co, ab, ao };
    }
    private boolean inSelection(Layout.Line l) { int[] s = ordered(); return l.block >= s[0] && l.block <= s[2]; }
    private int[] selRange(Layout.Line l) {
        int[] s = ordered(); int a = l.start, b = l.text.length() + l.start;
        if (l.block == s[0]) a = Math.max(a, s[1]);
        if (l.block == s[2]) b = Math.min(b, s[3]);
        if (l.block > s[0] && l.block < s[2]) b = l.start + l.text.length() + (l.softNext ? 0 : 0);
        else if (l.block != s[2]) b = Math.max(b, l.start + l.text.length());
        return new int[] { Math.min(a, b), Math.max(a, b) };
    }

    // ───────────── hit testing ─────────────
    private int[] hit(Point pt) {
        double ph = pageH();
        int pg = (int) Math.max(0, Math.min(layout.pageCount() - 1, Math.floor((pt.y - PAD + GAP / 2.0) / (ph + GAP))));
        double row = (pt.y - textY(pg)) / pitch();
        double col = (pt.x - textX(pg)) / cellW();
        Layout.Line best = null; double bd = 1e9;
        for (Layout.Line l : layout.pages.get(pg).lines) {
            double d = Math.abs(l.row + 0.5 - (row)); if (d < bd) { bd = d; best = l; }
        }
        if (best == null) return new int[] { cb, co };
        int max = best.softNext ? Math.max(0, best.text.length() - 1) : best.text.length();
        int off = best.start + (int) Math.max(0, Math.min(max, Math.round(col - best.col - 0.0 + (col - best.col >= 0 ? -0.0 : 0))));
        off = best.start + (int) Math.max(0, Math.min(max, Math.floor(col - best.col + 0.5)));
        return new int[] { best.block, off };
    }

    private void onPress(MouseEvent e) {
        requestFocusInWindow();
        if (e.isPopupTrigger()) { popup(e); return; }
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        int[] h = hit(e.getPoint());
        if (e.getClickCount() == 3) { ab = h[0]; ao = 0; cb = h[0]; co = doc.blocks.get(h[0]).text.length(); repaint(); caretListener.run(); return; }
        if (e.getClickCount() == 2) {
            String t = doc.blocks.get(h[0]).text; int s = h[1], en = h[1];
            while (s > 0 && Character.isLetterOrDigit(t.charAt(s - 1))) s--;
            while (en < t.length() && Character.isLetterOrDigit(t.charAt(en))) en++;
            ab = h[0]; ao = s; cb = h[0]; co = en; repaint(); caretListener.run(); return;
        }
        moveTo(h[0], h[1], e.isShiftDown()); prefCol = -1;
    }
    private void onDrag(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        int[] h = hit(e.getPoint()); cb = h[0]; co = h[1]; blinkOn = true; repaint();
        scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1)); caretListener.run();
    }

    // ───────────── keyboard ─────────────
    private void onTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (e.isControlDown() || e.isAltDown() && !e.isAltGraphDown() || e.isMetaDown() || c < 32 || c == 127) return;
        insertText(String.valueOf(c), true);
        e.consume();
    }

    private void onKey(KeyEvent e) {
        int k = e.getKeyCode(); boolean ctrl = e.isControlDown() || e.isMetaDown(), sh = e.isShiftDown();
        if (ctrl) {
            switch (k) {
                case KeyEvent.VK_A -> { ab = 0; ao = 0; cb = doc.blocks.size() - 1; co = doc.blocks.get(cb).text.length(); repaint(); caretListener.run(); e.consume(); return; }
                case KeyEvent.VK_C -> { copy(); e.consume(); return; }
                case KeyEvent.VK_X -> { cut(); e.consume(); return; }
                case KeyEvent.VK_V -> { paste(); e.consume(); return; }
                case KeyEvent.VK_Z -> { if (sh) redo(); else undo(); e.consume(); return; }
                case KeyEvent.VK_Y -> { redo(); e.consume(); return; }
                case KeyEvent.VK_B -> { toggleMarkup("**"); e.consume(); return; }
                case KeyEvent.VK_I -> { toggleMarkup("*"); e.consume(); return; }
                case KeyEvent.VK_U -> { toggleMarkup("_"); e.consume(); return; }
                case KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7 -> {
                    setType(ElementType.TOOLBAR[k - KeyEvent.VK_1]); e.consume(); return; }
                case KeyEvent.VK_HOME -> { moveTo(0, 0, sh); e.consume(); return; }
                case KeyEvent.VK_END -> { int b = doc.blocks.size() - 1; moveTo(b, doc.blocks.get(b).text.length(), sh); e.consume(); return; }
                case KeyEvent.VK_LEFT -> { moveTo(cb, wordLeft(), sh); e.consume(); return; }
                case KeyEvent.VK_RIGHT -> { moveTo(cb, wordRight(), sh); e.consume(); return; }
                default -> { return; }
            }
        }
        switch (k) {
            case KeyEvent.VK_LEFT -> { prefCol = -1; if (hasSel() && !sh) { int[] s = ordered(); moveTo(s[0], s[1], false); } else if (co > 0) moveTo(cb, co - 1, sh); else if (cb > 0) moveTo(cb - 1, doc.blocks.get(cb - 1).text.length(), sh); }
            case KeyEvent.VK_RIGHT -> { prefCol = -1; if (hasSel() && !sh) { int[] s = ordered(); moveTo(s[2], s[3], false); } else if (co < currentBlock().text.length()) moveTo(cb, co + 1, sh); else if (cb < doc.blocks.size() - 1) moveTo(cb + 1, 0, sh); }
            case KeyEvent.VK_UP -> moveVertical(-1, sh);
            case KeyEvent.VK_DOWN -> moveVertical(1, sh);
            case KeyEvent.VK_PAGE_UP -> moveVertical(-(doc.paper.lines() - 4), sh);
            case KeyEvent.VK_PAGE_DOWN -> moveVertical(doc.paper.lines() - 4, sh);
            case KeyEvent.VK_HOME -> { prefCol = -1; Layout.Line l = caretLine(); moveTo(cb, l == null ? 0 : l.start, sh); }
            case KeyEvent.VK_END -> { prefCol = -1; Layout.Line l = caretLine(); moveTo(cb, l == null ? currentBlock().text.length() : l.start + (l.softNext ? Math.max(0, l.text.length() - 1) : l.text.length()), sh); }
            case KeyEvent.VK_ENTER -> enter(sh);
            case KeyEvent.VK_BACK_SPACE -> backspace();
            case KeyEvent.VK_DELETE -> delete();
            case KeyEvent.VK_TAB -> tab(sh);
            case KeyEvent.VK_INSERT -> { overwrite = !overwrite; caretListener.run(); }
            case KeyEvent.VK_ESCAPE -> { ab = cb; ao = co; repaint(); return; }
            default -> { return; }
        }
        e.consume();
    }

    private int wordLeft() {
        String t = currentBlock().text; int i = co;
        while (i > 0 && !Character.isLetterOrDigit(t.charAt(i - 1))) i--;
        while (i > 0 && Character.isLetterOrDigit(t.charAt(i - 1))) i--;
        return i;
    }
    private int wordRight() {
        String t = currentBlock().text; int i = co;
        while (i < t.length() && !Character.isLetterOrDigit(t.charAt(i))) i++;
        while (i < t.length() && Character.isLetterOrDigit(t.charAt(i))) i++;
        return i;
    }

    private void moveVertical(int dy, boolean extend) {
        int li = layout.lineIndex(cb, co); if (li < 0) return;
        Layout.Line cur = layout.all.get(li);
        if (prefCol < 0) prefCol = cur.col + (co - cur.start);
        int ni = Math.max(0, Math.min(layout.all.size() - 1, li + dy));
        Layout.Line n = layout.all.get(ni);
        int max = n.softNext ? Math.max(0, n.text.length() - 1) : n.text.length();
        int off = n.start + Math.max(0, Math.min(max, prefCol - n.col));
        int keep = prefCol; moveTo(n.block, off, extend); prefCol = keep;
    }

    // ───────────── editing ─────────────
    private void deleteSelectionNoEdit() {
        int[] s = ordered();
        if (s[0] == s[2]) { Block b = doc.blocks.get(s[0]); b.text = b.text.substring(0, s[1]) + b.text.substring(s[3]); }
        else {
            Block f = doc.blocks.get(s[0]), l = doc.blocks.get(s[2]);
            f.text = f.text.substring(0, s[1]) + l.text.substring(s[3]);
            for (int i = s[2]; i > s[0]; i--) doc.blocks.remove(i);
        }
        cb = ab = s[0]; co = ao = s[1];
    }
    public void deleteSelection() {
        if (!hasSel()) return;
        doc.beginEdit(cb, co, null); deleteSelectionNoEdit(); doc.edited(); scrollCaret();
    }

    public void insertText(String s, boolean typing) {
        Settings st = Settings.get();
        doc.beginEdit(cb, co, typing && !hasSel() ? "type" + cb : null);
        if (hasSel()) deleteSelectionNoEdit();
        Block b = currentBlock();
        if (st.autoCaps && b.type.upper()) s = s.toUpperCase();
        if (overwrite && typing && co < b.text.length()) b.text = b.text.substring(0, co) + s + b.text.substring(co + 1);
        else b.text = b.text.substring(0, co) + s + b.text.substring(co);
        co += s.length(); ab = cb; ao = co; prefCol = -1; blinkOn = true;
        doc.edited(); scrollCaret();
    }

    public void enter(boolean soft) {
        Settings st = Settings.get();
        if (soft) { insertText("\n", false); return; }
        doc.beginEdit(cb, co, null);
        if (hasSel()) deleteSelectionNoEdit();
        Block b = currentBlock();
        if (st.smartEnter && b.text.isEmpty() && b.type != ElementType.ACTION && b.type != ElementType.PAGE_BREAK) { b.type = ElementType.ACTION; doc.edited(); return; }
        if (st.autoDetectScene && b.type == ElementType.ACTION && co == b.text.length() && !b.text.contains("\n")
                && b.text.matches("(?i)^(INT|EXT|EST|INT\\.?/EXT\\.?|EXT\\.?/INT\\.?|I/E)[. ].*")) { b.type = ElementType.SCENE; b.text = b.text.toUpperCase(); }
        String left = b.text.substring(0, co), right = b.text.substring(co);
        Block nb;
        if (right.isEmpty()) {
            ElementType nt = b.type.printed ? b.type.next() : ElementType.ACTION;
            if (b.type == ElementType.DIALOGUE && st.smartEnter && !b.text.isEmpty()) nt = ElementType.ACTION;
            nb = new Block(nt, "");
        } else { b.text = left; nb = new Block(b.type, right); }
        doc.blocks.add(cb + 1, nb);
        cb++; co = 0; ab = cb; ao = 0; prefCol = -1;
        doc.edited(); scrollCaret();
    }

    public void backspace() {
        if (hasSel()) { deleteSelection(); return; }
        if (co > 0) {
            doc.beginEdit(cb, co, "bs" + cb);
            Block b = currentBlock(); b.text = b.text.substring(0, co - 1) + b.text.substring(co); co--; ao = co; ab = cb;
        } else if (cb > 0) {
            doc.beginEdit(cb, co, null);
            Block b = currentBlock(), pv = doc.blocks.get(cb - 1);
            if (b.isEmpty() || pv.type == ElementType.PAGE_BREAK) { doc.blocks.remove(cb); cb--; co = doc.blocks.get(cb).text.length(); }
            else { co = pv.text.length(); pv.text += b.text; doc.blocks.remove(cb); cb--; }
            ab = cb; ao = co;
        } else return;
        prefCol = -1; doc.edited(); scrollCaret();
    }
    public void delete() {
        if (hasSel()) { deleteSelection(); return; }
        Block b = currentBlock();
        if (co < b.text.length()) { doc.beginEdit(cb, co, "del" + cb); b.text = b.text.substring(0, co) + b.text.substring(co + 1); }
        else if (cb < doc.blocks.size() - 1) { doc.beginEdit(cb, co, null); b.text += doc.blocks.get(cb + 1).text; doc.blocks.remove(cb + 1); }
        else return;
        doc.edited();
    }
    private void tab(boolean back) {
        Block b = currentBlock();
        if (b.isEmpty()) { setType(back ? ElementType.ACTION : b.type.tabNext()); }
        else if (b.type == ElementType.ACTION && co == b.text.length()) { setType(ElementType.TRANSITION); }
    }

    public void setType(ElementType t) {
        doc.beginEdit(cb, co, null);
        int[] s = ordered();
        for (int i = s[0]; i <= s[2]; i++) {
            Block b = doc.blocks.get(i); b.type = t;
            if (t.upper() && Settings.get().autoCaps) b.text = b.text.toUpperCase();
        }
        doc.edited(); repaint();
    }
    public ElementType currentType() { return currentBlock().type; }

    public void toggleMarkup(String m) {
        doc.beginEdit(cb, co, null);
        int[] s = ordered();
        if (s[0] == s[2] && s[1] == s[3]) {
            Block b = currentBlock(); b.text = b.text.substring(0, co) + m + m + b.text.substring(co); co += m.length(); ao = ab = co;
        } else for (int i = s[0]; i <= s[2]; i++) {
            Block b = doc.blocks.get(i);
            int a = i == s[0] ? s[1] : 0, e = i == s[2] ? s[3] : b.text.length();
            if (a >= e) continue;
            String pre = b.text.substring(0, a), mid = b.text.substring(a, e), post = b.text.substring(e);
            if (pre.endsWith(m) && post.startsWith(m) && !(m.equals("*") && (pre.endsWith("**") || post.startsWith("**")))) {
                b.text = pre.substring(0, pre.length() - m.length()) + mid + post.substring(m.length());
                if (i == s[0]) { ao = Math.min(ao, a - m.length()); }
                if (i == cb) { co = Math.max(0, co - m.length()); }
            } else {
                b.text = pre + m + mid + m + post;
                if (i == cb) co = Math.max(co, e + 2 * m.length());
                if (i == ab) ao += m.length();
                if (i == cb && cb == ab && ao > co) { int t = ao; ao = co; co = t; }
            }
        }
        doc.edited();
    }

    // ───────────── clipboard / undo ─────────────
    public String selectedText() {
        if (!hasSel()) return "";
        int[] s = ordered();
        if (s[0] == s[2]) return doc.blocks.get(s[0]).text.substring(s[1], s[3]);
        List<Block> part = new ArrayList<>();
        for (int i = s[0]; i <= s[2]; i++) {
            Block b = doc.blocks.get(i).copy();
            if (i == s[2]) b.text = b.text.substring(0, s[3]);
            if (i == s[0]) b.text = b.text.substring(s[1]);
            part.add(b);
        }
        return FountainIO.bodyToFountain(part);
    }
    public void copy() { if (hasSel()) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selectedText()), null); }
    public void cut() { if (hasSel()) { copy(); deleteSelection(); } }
    public void paste() {
        try {
            String t = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (t == null || t.isEmpty()) return;
            t = t.replace("\r\n", "\n").replace('\r', '\n');
            if (!t.contains("\n")) { insertText(t, false); return; }
            List<Block> parsed = FountainIO.parseBody(Arrays.asList(t.split("\n", -1)));
            if (parsed.isEmpty()) return;
            if (parsed.size() == 1 && parsed.get(0).type == ElementType.ACTION) { insertText(parsed.get(0).text, false); return; }
            doc.beginEdit(cb, co, null);
            if (hasSel()) deleteSelectionNoEdit();
            Block b = currentBlock();
            String left = b.text.substring(0, co), right = b.text.substring(co);
            int at = cb + 1;
            if (left.isEmpty() && right.isEmpty()) { doc.blocks.remove(cb); at = cb; }
            else { b.text = left; if (!right.isEmpty()) doc.blocks.add(cb + 1, new Block(b.type, right)); }
            doc.blocks.addAll(at, parsed);
            cb = at + parsed.size() - 1; co = doc.blocks.get(cb).text.length(); ab = cb; ao = co;
            if (doc.blocks.isEmpty()) doc.blocks.add(new Block(ElementType.ACTION, ""));
            doc.edited(); scrollCaret();
        } catch (Exception ignored) {}
    }
    public void undo() { var s = doc.undo(cb, co); if (s != null) { cb = s.caretBlock(); co = s.caretOffset(); ab = cb; ao = co; clamp(); relayout(); scrollCaret(); } }
    public void redo() { var s = doc.redo(cb, co); if (s != null) { cb = s.caretBlock(); co = s.caretOffset(); ab = cb; ao = co; clamp(); relayout(); scrollCaret(); } }
    public void selectAll() { ab = 0; ao = 0; cb = doc.blocks.size() - 1; co = doc.blocks.get(cb).text.length(); repaint(); caretListener.run(); }

    /** Insert a new block after the current one. */
    public void insertBlock(ElementType t, String text) {
        doc.beginEdit(cb, co, null);
        Block cur = currentBlock();
        int at = cur.isEmpty() ? cb : cb + 1;
        if (cur.isEmpty()) doc.blocks.remove(cb);
        doc.blocks.add(at, new Block(t, text)); cb = ab = at; co = ao = text.length();
        doc.edited(); scrollCaret();
    }

    // ───────────── find / replace ─────────────
    public boolean find(String q, boolean matchCase, boolean forward) {
        if (q.isEmpty()) return false;
        int n = doc.blocks.size();
        int[] s = ordered();
        int bi = forward ? s[2] : s[0], off = forward ? s[3] : s[1];
        for (int step = 0; step <= n; step++) {
            int idx = ((bi + (forward ? step : -step)) % n + n) % n;
            String t = doc.blocks.get(idx).text; String hay = matchCase ? t : t.toLowerCase(), nd = matchCase ? q : q.toLowerCase();
            int from = step == 0 ? off : (forward ? 0 : t.length());
            int pos = forward ? hay.indexOf(nd, from) : (from - 1 < 0 ? -1 : hay.lastIndexOf(nd, from - nd.length()));
            if (step == 0 && !forward && off - nd.length() < 0) pos = -1;
            if (pos >= 0) { ab = idx; ao = pos; cb = idx; co = pos + q.length(); repaint(); scrollCaret(); caretListener.run(); return true; }
        }
        return false;
    }
    public boolean replaceCurrent(String q, String r, boolean matchCase) {
        if (hasSel()) { String sel = selectedText(); if (matchCase ? sel.equals(q) : sel.equalsIgnoreCase(q)) { doc.beginEdit(cb, co, null); deleteSelectionNoEdit(); Block b = currentBlock(); b.text = b.text.substring(0, co) + r + b.text.substring(co); co += r.length(); ao = co; ab = cb; doc.edited(); } }
        return find(q, matchCase, true);
    }
    public int replaceAll(String q, String r, boolean matchCase) {
        if (q.isEmpty()) return 0;
        doc.beginEdit(cb, co, null); int count = 0;
        Pattern p = Pattern.compile(Pattern.quote(q), matchCase ? 0 : Pattern.CASE_INSENSITIVE);
        for (Block b : doc.blocks) { Matcher m = p.matcher(b.text); int c = 0; while (m.find()) c++; if (c > 0) { b.text = m.replaceAll(Matcher.quoteReplacement(r)); count += c; } }
        if (count > 0) doc.edited();
        return count;
    }

    // ───────────── popup ─────────────
    private void popup(MouseEvent e) {
        JPopupMenu m = new JPopupMenu();
        addItem(m, "Cut", this::cut); addItem(m, "Copy", this::copy); addItem(m, "Paste", this::paste); m.addSeparator();
        for (ElementType t : ElementType.TOOLBAR) addItem(m, t.label, () -> setType(t));
        m.show(this, e.getX(), e.getY());
    }
    private void addItem(JPopupMenu m, String t, Runnable r) { JMenuItem i = new JMenuItem(t); i.addActionListener(a -> r.run()); m.add(i); }

    // ───────────── Scrollable ─────────────
    @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(600, 500); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return o == SwingConstants.VERTICAL ? (int) (pitch() * 3) : 16; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return o == SwingConstants.VERTICAL ? r.height - 40 : r.width - 40; }
    @Override public boolean getScrollableTracksViewportWidth() { return getParent() != null && getParent().getWidth() >= getPreferredSize().width; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }

    /** Scroll so a given page index is at the top. */
    public void scrollToPage(int pg) { scrollRectToVisible(new Rectangle(0, (int) pageY(pg), 10, (int) (pageH() + GAP))); }
}
