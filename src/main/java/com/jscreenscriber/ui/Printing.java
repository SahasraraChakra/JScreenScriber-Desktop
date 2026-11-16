package com.jscreenscriber.ui;

import com.jscreenscriber.Settings;
import com.jscreenscriber.Theme;
import com.jscreenscriber.model.*;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

/** Prints the title page + all script pages at true A4/Letter geometry (72 units per inch). */
public final class Printing implements Printable {
    private final ScriptDocument doc; private final Layout lay; private final boolean title;
    private final Settings st = Settings.get();

    private Printing(ScriptDocument d) {
        doc = d; lay = Layout.build(d, false);
        title = st.printTitlePage && d.title.enabled && !d.title.isEmpty();
    }

    public int pageCount() { return lay.pageCount() + (title ? 1 : 0); }

    public static void print(ScriptDocument d) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        Printing p = new Printing(d);
        java.awt.print.Paper paper = new java.awt.print.Paper();
        paper.setSize(d.paper.wIn * 72, d.paper.hIn * 72);
        paper.setImageableArea(0, 0, d.paper.wIn * 72, d.paper.hIn * 72);
        PageFormat pf = job.defaultPage(); pf.setPaper(paper); pf.setOrientation(PageFormat.PORTRAIT);
        job.setPrintable(p, pf);
        job.setJobName(d.displayName());
        if (job.printDialog()) job.print();
    }

    @Override public int print(Graphics g0, PageFormat pf, int index) {
        if (index >= pageCount()) return NO_SUCH_PAGE;
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(pf.getImageableX(), pf.getImageableY());
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        if (title && index == 0) { TitlePageRenderer.paint(g, doc.title, doc.paper, 72, Color.BLACK); g.dispose(); return PAGE_EXISTS; }
        int pi = index - (title ? 1 : 0);
        Layout.Page page = lay.pages.get(pi);
        double cw = 7.2, lh = 12, x0 = Paper.LEFT * 72, y0 = Paper.TOP * 72;
        if (st.printPageNumbers && (pi > 0 || st.numberFirstPage)) {
            g.setFont(Theme.mono(Font.PLAIN, 12));
            String s = (pi + 1) + ".";
            g.drawString(s, (float) (doc.paper.wIn * 72 - Paper.RIGHT * 72 - s.length() * cw), (float) (0.5 * 72 + 8));
        }
        if (!st.headerText.isBlank()) { g.setFont(Theme.mono(Font.PLAIN, 10)); g.drawString(st.headerText, (float) x0, (float) (0.5 * 72 + 8)); }
        for (Layout.Line l : page.lines) {
            Styles.S sty = Styles.of(l.type); boolean boldT = sty.bold;
            double y = y0 + l.row * lh + 10;
            for (int i = 0; i < l.text.length(); i++) {
                char ch = l.text.charAt(i); if (ch == ' ') continue;
                byte s = l.style[i];
                int fs = ((s & Layout.BOLD) != 0 || boldT ? Font.BOLD : 0) | ((s & Layout.ITALIC) != 0 || sty.italic ? Font.ITALIC : 0);
                g.setFont(Theme.mono(fs, 12));
                double x = x0 + (l.col + i) * cw;
                g.drawString(String.valueOf(ch), (float) x, (float) y);
                if ((s & Layout.UNDER) != 0 || sty.underline) g.draw(new java.awt.geom.Line2D.Double(x, y + 1.5, x + cw, y + 1.5));
            }
            if (st.printSceneNumbers && l.first && l.type == ElementType.SCENE) {
                Block b = doc.blocks.get(l.block);
                String num = b.sceneNum != null ? b.sceneNum : String.valueOf(lay.sceneIndexOfBlock(l.block) + 1);
                g.setFont(Theme.mono(Font.PLAIN, 12));
                g.drawString(num + ".", (float) (x0 - (num.length() + 3) * cw), (float) y);
                g.drawString(num + ".", (float) (x0 + doc.paper.cols() * cw + cw), (float) y);
            }
        }
        g.dispose();
        return PAGE_EXISTS;
    }
}
