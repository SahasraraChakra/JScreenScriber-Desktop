package com.jscreenscriber.ui;

import com.jscreenscriber.Settings;
import com.jscreenscriber.model.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;

/**
 * Direct PDF export (no external library). Text is set in the PDF base-14 Courier family at 12pt / 10 cpi / 6 lpi,
 * the screenplay industry standard, so output is identical on every machine and needs no embedded fonts.
 */
public final class PdfExporter {
    private PdfExporter() {}
    private static final Charset CP1252 = Charset.forName("windows-1252");

    private static final class Page {
        final StringBuilder c = new StringBuilder(); final double h;
        Page(double h) { this.h = h; }
        /** y measured downward from the top; font 0..3 = Courier, Bold, Oblique, BoldOblique. */
        void text(String s, double x, double yDown, int font, double size) {
            if (s.isEmpty()) return;
            c.append("BT /F").append(font + 1).append(' ').append(f(size)).append(" Tf ").append(f(x)).append(' ').append(f(h - yDown)).append(" Td (").append(esc(s)).append(") Tj ET\n");
        }
        void line(double x1, double y1, double x2, double y2) { c.append("0.6 w ").append(f(x1)).append(' ').append(f(h - y1)).append(" m ").append(f(x2)).append(' ').append(f(h - y2)).append(" l S\n"); }
    }

    private static String f(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace("\n", " ").replace("\r", ""); }

    public static void export(ScriptDocument d, Path out) throws IOException {
        Settings st = Settings.get();
        double W = d.paper.wIn * 72, H = d.paper.hIn * 72;
        List<Page> pages = new ArrayList<>();

        if (st.printTitlePage && d.title.enabled && !d.title.isEmpty()) {
            Page p = new Page(H);
            TitlePageRenderer.layout(new TitlePageRenderer.Sink() {
                public double width(String s, double size, boolean bold) { return s.length() * size * 0.6; }
                public void text(String s, double x, double y, double size, boolean bold) { p.text(s, x, y, bold ? 1 : 0, size); }
                public void line(double x1, double y1, double x2, double y2) { p.line(x1, y1, x2, y2); }
            }, d.title, d.paper, 72);
            pages.add(p);
        }

        Layout lay = Layout.build(d, false);
        double cw = 7.2, lh = 12, x0 = Paper.LEFT * 72, y0 = Paper.TOP * 72;
        for (int pi = 0; pi < lay.pageCount(); pi++) {
            Page p = new Page(H);
            if (st.printPageNumbers && (pi > 0 || st.numberFirstPage)) {
                String s = (pi + 1) + ".";
                p.text(s, W - Paper.RIGHT * 72 - s.length() * cw, Math.min(0.5, Paper.TOP / 2) * 72 + 8, 1, 12);
            }
            if (!st.headerText.isBlank()) p.text(st.headerText, x0, Math.min(0.5, Paper.TOP / 2) * 72 + 8, 0, 10);
            for (Layout.Line l : lay.pages.get(pi).lines) {
                Styles.S sty = Styles.of(l.type);
                double y = y0 + l.row * lh + 10;
                int i = 0, n = l.text.length();
                while (i < n) {                                   // runs of identical style
                    int font = fontOf(l.style[i], sty), j = i + 1;
                    while (j < n && fontOf(l.style[j], sty) == font && (isUnder(l.style[j], sty) == isUnder(l.style[i], sty))) j++;
                    String run = l.text.substring(i, j);
                    if (!run.isBlank()) p.text(run, x0 + (l.col + i) * cw, y, font, 12);
                    if (isUnder(l.style[i], sty) && !run.isBlank()) p.line(x0 + (l.col + i) * cw, y + 1.8, x0 + (l.col + j) * cw, y + 1.8);
                    i = j;
                }
                if (st.printSceneNumbers && l.first && l.type == ElementType.SCENE) {
                    Block b = d.blocks.get(l.block);
                    String num = (b.sceneNum != null ? b.sceneNum : String.valueOf(lay.sceneIndexOfBlock(l.block) + 1)) + ".";
                    p.text(num, x0 - (num.length() + 2) * cw, y, 0, 12);
                    p.text(num, x0 + d.paper.cols() * cw + cw, y, 0, 12);
                }
            }
            pages.add(p);
        }
        if (pages.isEmpty()) pages.add(new Page(H));
        write(pages, W, H, d, out);
    }

    private static int fontOf(byte s, Styles.S sty) { boolean b = (s & Layout.BOLD) != 0 || sty.bold, i = (s & Layout.ITALIC) != 0 || sty.italic; return (b ? 1 : 0) + (i ? 2 : 0); }
    private static boolean isUnder(byte s, Styles.S sty) { return (s & Layout.UNDER) != 0 || sty.underline; }

    // ───────── PDF file structure ─────────
    private static void write(List<Page> pages, double W, double H, ScriptDocument d, Path out) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        List<Integer> offs = new ArrayList<>();
        o.write("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        int nPages = pages.size();
        // object numbers: 1 catalog, 2 pages, 3-6 fonts, 7 info, then (page, content) pairs
        obj(o, offs, 1, "<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < nPages; i++) kids.append(8 + 2 * i).append(" 0 R ");
        obj(o, offs, 2, "<< /Type /Pages /Kids [" + kids + "] /Count " + nPages + " >>");
        String[] fonts = { "Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique" };
        for (int i = 0; i < 4; i++) obj(o, offs, 3 + i, "<< /Type /Font /Subtype /Type1 /BaseFont /" + fonts[i] + " /Encoding /WinAnsiEncoding >>");
        String title = d.title.get("Title"); if (title.isEmpty()) title = d.displayName();
        obj(o, offs, 7, "<< /Title (" + esc(title) + ") /Producer (JScreenScriber) >>");
        for (int i = 0; i < nPages; i++) {
            int po = 8 + 2 * i, co = po + 1;
            obj(o, offs, po, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + f(W) + " " + f(H) + "] /Contents " + co + " 0 R /Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R /F4 6 0 R >> >> >>");
            ByteArrayOutputStream z = new ByteArrayOutputStream();
            try (DeflaterOutputStream df = new DeflaterOutputStream(z)) { df.write(pages.get(i).c.toString().getBytes(CP1252)); }
            offs.add(o.size());
            o.write((co + " 0 obj\n<< /Length " + z.size() + " /Filter /FlateDecode >>\nstream\n").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
            o.write(z.toByteArray()); o.write("\nendstream\nendobj\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        }
        int xref = o.size(), total = 8 + 2 * nPages;
        StringBuilder x = new StringBuilder("xref\n0 " + total + "\n0000000000 65535 f \n");
        for (int off : offs) x.append(String.format("%010d 00000 n \n", off));
        x.append("trailer\n<< /Size ").append(total).append(" /Root 1 0 R /Info 7 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        o.write(x.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        Files.write(out, o.toByteArray());
    }

    private static void obj(ByteArrayOutputStream o, List<Integer> offs, int n, String body) throws IOException {
        offs.add(o.size());
        o.write((n + " 0 obj\n" + body + "\nendobj\n").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
    }
}
