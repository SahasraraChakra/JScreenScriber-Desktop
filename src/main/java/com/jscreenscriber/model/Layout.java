package com.jscreenscriber.model;

import java.util.*;

/**
 * Monospace character-cell layout + pagination. Screen layout keeps raw markup visible (so raw offsets == display
 * offsets and caret mapping is trivial); print layout strips markup and drops non-printing elements.
 */
public final class Layout {
    public static final byte BOLD = 1, ITALIC = 2, UNDER = 4, MARK = 8;

    public static final class Line {
        public int block, start, end, col, row, page;
        public String text = "";
        public byte[] style;
        public ElementType type;
        public boolean first, softNext;
    }
    public static final class Page { public final List<Line> lines = new ArrayList<>(); public int number; }
    public record SceneInfo(int block, int page, int eighths, int lines) {
        public String lengthLabel() {
            int pg = eighths / 8, e = eighths % 8;
            return pg == 0 ? e + "/8 p" : pg + " " + e + "/8";
        }
    }

    public final List<Page> pages = new ArrayList<>();
    public final List<Line> all = new ArrayList<>();
    public final List<SceneInfo> scenes = new ArrayList<>();
    public int[] blockFirstLine = new int[0];
    public final Paper paper;
    public final boolean screen;

    private record Seg(int start, int end, boolean softNext) {}

    private Layout(Paper p, boolean screen) { this.paper = p; this.screen = screen; }

    // ───────────── inline markup ─────────────
    public static byte[] styles(String s) {
        byte[] st = new byte[s.length()];
        boolean bold = false, ital = false, und = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) { st[i] = (byte) (MARK | cur(bold, ital, und)); i++; st[i] = cur(bold, ital, und); i++; continue; }
            if (c == '*') {
                int run = 1; while (i + run < s.length() && s.charAt(i + run) == '*' && run < 3) run++;
                String tok = "*".repeat(run);
                boolean on = run == 3 ? (bold && ital) : run == 2 ? bold : ital;
                if (on || s.indexOf(tok, i + run) >= 0) {
                    if (run == 3) { bold = !on; ital = !on; } else if (run == 2) bold = !on; else ital = !on;
                    for (int k = 0; k < run; k++) st[i + k] = (byte) (MARK | cur(bold || run >= 2 && !on, ital, und));
                    i += run; continue;
                }
                st[i] = cur(bold, ital, und); i++; continue;
            }
            if (c == '_') {
                if (und || s.indexOf('_', i + 1) >= 0) { und = !und; st[i] = (byte) (MARK | cur(bold, ital, true)); i++; continue; }
            }
            st[i] = cur(bold, ital, und); i++;
        }
        return st;
    }
    private static byte cur(boolean b, boolean i, boolean u) { return (byte) ((b ? BOLD : 0) | (i ? ITALIC : 0) | (u ? UNDER : 0)); }

    /** Text with markup characters removed, plus per-char styles. */
    public static Object[] stripMarkup(String s) {
        byte[] st = styles(s);
        StringBuilder sb = new StringBuilder(); byte[] out = new byte[s.length()]; int n = 0;
        for (int i = 0; i < s.length(); i++) if ((st[i] & MARK) == 0) { sb.append(s.charAt(i)); out[n++] = st[i]; }
        return new Object[] { sb.toString(), Arrays.copyOf(out, n) };
    }

    // ───────────── wrapping ─────────────
    static List<int[]> wrap(String s, int w) {
        List<int[]> r = new ArrayList<>();
        int len = s.length();
        if (len == 0) { r.add(new int[] { 0, 0 }); return r; }
        int i = 0;
        while (i < len) {
            int lim = i + w;
            if (len <= lim) { r.add(new int[] { i, len }); break; }
            int brk = -1;
            for (int k = lim; k > i; k--) if (s.charAt(k) == ' ') { brk = k; break; }
            int end = brk < 0 ? lim : brk + 1;
            r.add(new int[] { i, end }); i = end;
        }
        return r;
    }

    private List<Seg> segments(String text, int w) {
        List<Seg> out = new ArrayList<>();
        int base = 0;
        for (String part : text.split("\n", -1)) {
            List<int[]> rs = wrap(part, w);
            for (int k = 0; k < rs.size(); k++) out.add(new Seg(base + rs.get(k)[0], base + rs.get(k)[1], k < rs.size() - 1));
            base += part.length() + 1;
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Seg> segsFor(Block b, String text) {
        int w = b.type.widthFor(paper);
        if (screen && b.cache != null && b.cacheW == w && b.cacheType == b.type && text.equals(b.cacheText)) return (List<Seg>) (List<?>) b.cache;
        List<Seg> s = segments(text, w);
        if (screen) { b.cache = new ArrayList<>(s); b.cacheW = w; b.cacheType = b.type; b.cacheText = text; }
        return s;
    }

    static int spaceBefore(ElementType prev, ElementType cur, int ignored) {
        if (prev == null) return 0;
        if ((cur == ElementType.PAREN || cur == ElementType.DIALOGUE)
                && (prev == ElementType.CHARACTER || prev == ElementType.PAREN || prev == ElementType.DIALOGUE)) return 0;
        return Styles.of(cur).before;
    }

    // ───────────── build ─────────────
    public static Layout build(ScriptDocument d, boolean screen) {
        Layout L = new Layout(d.paper, screen);
        int lpp = d.paper.lines();
        L.blockFirstLine = new int[d.blocks.size()];
        Arrays.fill(L.blockFirstLine, -1);
        Page page = L.newPage();
        int y = 0; ElementType prev = null; boolean forceNew = false;
        int sceneStartLine = -1, sceneBlock = -1, sceneStartPage = 0;
        int usedLines = 0;

        for (int i = 0; i < d.blocks.size(); i++) {
            Block b = d.blocks.get(i);
            if (!screen && !b.type.printed) continue;
            if (b.type == ElementType.PAGE_BREAK && !screen) { forceNew = true; prev = null; continue; }

            String text = b.type == ElementType.PAGE_BREAK ? "- - - - - - - page break - - - - - - -" : b.text;
            byte[] st; 
            if (screen) st = styles(text);
            else { Object[] sp = stripMarkup(text); text = (String) sp[0]; st = (byte[]) sp[1]; }
            List<Seg> segs = L.segsFor(b, text);
            int cnt = segs.size();

            if (b.type == ElementType.SCENE) {
                if (sceneBlock >= 0) L.closeScene(sceneBlock, sceneStartPage, usedLines, lpp);
                sceneBlock = i; usedLines = 0; sceneStartPage = L.pages.size() - 1;
            }

            if (forceNew) { page = L.newPage(); y = 0; forceNew = false; }
            int sb = y == 0 ? 0 : spaceBefore(prev, b.type, d.sceneSpacing);
            int look = 0;
            if (b.type == ElementType.SCENE || b.type == ElementType.CHARACTER || b.type == ElementType.PAREN) {
                for (int j = i + 1; j < d.blocks.size(); j++) {
                    Block nb = d.blocks.get(j);
                    if (!screen && !nb.type.printed) continue;
                    look = spaceBefore(b.type, nb.type, d.sceneSpacing) + Math.min(2, L.segsFor(nb, nb.text).size());
                    break;
                }
            }
            int firstOnPage = 0;
            if (y > 0 && y + sb + cnt + look > lpp) {
                int room = lpp - y - sb;
                int k = Math.min(room, cnt - 2);
                if ((b.type == ElementType.ACTION || b.type == ElementType.DIALOGUE) && cnt >= 4 && k >= 2) {
                    // split block across pages
                    y += sb; usedLines += sb;
                    for (int s = 0; s < k; s++) { L.place(page, b, i, segs, s, text, st, y++); usedLines++; }
                    page = L.newPage(); y = 0;
                    for (int s = k; s < cnt; s++) {
                        if (y >= lpp) { page = L.newPage(); y = 0; }
                        L.place(page, b, i, segs, s, text, st, y++); usedLines++;
                    }
                    prev = b.type; continue;
                }
                page = L.newPage(); y = 0; sb = 0;
            }
            if (sb > 0) { y += sb; usedLines += sb; }
            for (int s = 0; s < cnt; s++) {
                if (y >= lpp) { page = L.newPage(); y = 0; }
                L.place(page, b, i, segs, s, text, st, y++); usedLines++;
            }
            if (b.type == ElementType.PAGE_BREAK) forceNew = true;
            prev = b.type;
        }
        if (sceneBlock >= 0) L.closeScene(sceneBlock, sceneStartPage, usedLines, lpp);
        // drop trailing empty page
        while (L.pages.size() > 1 && L.pages.get(L.pages.size() - 1).lines.isEmpty()) L.pages.remove(L.pages.size() - 1);
        return L;
    }

    private void closeScene(int block, int pg, int lines, int lpp) {
        scenes.add(new SceneInfo(block, pg, (int) Math.round(lines * 8.0 / lpp), lines));
    }

    private Page newPage() { Page p = new Page(); p.number = pages.size() + 1; pages.add(p); return p; }

    private void place(Page pg, Block b, int bi, List<Seg> segs, int s, String text, byte[] st, int row) {
        Seg sg = segs.get(s);
        Line ln = new Line();
        ln.block = bi; ln.start = sg.start(); ln.end = sg.end(); ln.type = b.type; ln.row = row; ln.page = pg.number - 1;
        ln.softNext = sg.softNext(); ln.first = s == 0;
        ln.text = text.substring(Math.min(sg.start(), text.length()), Math.min(sg.end(), text.length()));
        ln.style = Arrays.copyOfRange(st, Math.min(sg.start(), st.length), Math.min(sg.end(), st.length));
        ln.col = b.type.indent();
        int w = b.type.widthFor(paper);
        String trimmed = ln.text.stripTrailing();
        int al = Styles.of(b.type).align;
        if (al == 2) ln.col = b.type.indent() + Math.max(0, w - trimmed.length());
        else if (al == 1) ln.col = b.type.indent() + Math.max(0, (w - trimmed.length()) / 2);
        if (ln.first && blockFirstLine.length > bi && blockFirstLine[bi] < 0) blockFirstLine[bi] = all.size();
        pg.lines.add(ln); all.add(ln);
    }

    // ───────────── queries ─────────────
    /** Index in {@link #all} of the line holding (block, offset), or -1 (print layout only). */
    public int lineIndex(int block, int offset) {
        if (block < 0 || block >= blockFirstLine.length || blockFirstLine[block] < 0) return -1;
        int i = blockFirstLine[block];
        while (i < all.size() && all.get(i).block == block) {
            Line l = all.get(i);
            if (offset < l.end || (offset == l.end && !l.softNext)) return i;
            i++;
        }
        return Math.max(blockFirstLine[block], i - 1);
    }
    public int pageCount() { return pages.size(); }
    public int sceneIndexOfBlock(int block) {
        int idx = -1;
        for (int i = 0; i < scenes.size(); i++) if (scenes.get(i).block() <= block) idx = i; else break;
        return idx;
    }
}
