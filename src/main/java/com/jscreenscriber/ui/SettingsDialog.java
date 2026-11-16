package com.jscreenscriber.ui;

import com.jscreenscriber.Settings;
import com.jscreenscriber.Theme;
import com.jscreenscriber.model.ElementType;
import com.jscreenscriber.model.Paper;
import com.jscreenscriber.model.Styles;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Preferences window with category list; every control maps to a persisted {@link Settings} / {@link Styles} value. */
public class SettingsDialog extends ChromeDialog {
    private final Settings s = Settings.get();
    private final Runnable onApply;
    private final JList<String> cats = new JList<>(new String[] { "General", "Appearance", "Editor", "Page & Print", "Page Margins", "Element Styles", "Autosave & Files" });
    private final CardLayout cards = new CardLayout();
    private final JPanel stack = new JPanel(cards);

    private final JTextField author = Form.field(s.author, 24);
    private final JComboBox<String> theme = Form.combo(new String[] { "Light", "Dark" }, s.dark ? "Dark" : "Light");
    private final JSpinner uiFont = Form.spinner(s.uiFontPx, 10, 20, 1), scriptPx = Form.spinner(s.scriptFontPx, 8, 28, 1), zoom = Form.spinner(s.zoom, 50, 250, 10);
    private final JComboBox<String> scriptFont = new JComboBox<>();
    private final JCheckBox ruler = Form.check("Show text ruler", s.showRuler), left = Form.check("Show Scene Headings panel", s.showLeft), right = Form.check("Show Characters panel", s.showRight);
    private final JCheckBox caps = Form.check("Auto-capitalise elements whose style has ALL CAPS", s.autoCaps),
            smart = Form.check("Smart Enter / Tab element flow", s.smartEnter), detect = Form.check("Turn INT./EXT. lines into scene headings on Enter", s.autoDetectScene),
            sceneNum = Form.check("Show scene numbers in the page margins", s.sceneNumbers), spell = Form.check("Check spelling while typing", s.spell);
    private final JTextField dict = Form.field(s.dictionary, 24);
    private final JComboBox<Paper> paper = Form.combo(Paper.values(), s.paper);
    private final JCheckBox pTitle = Form.check("Print / export title page", s.printTitlePage), pNums = Form.check("Print page numbers", s.printPageNumbers),
            pFirst = Form.check("Number the first page", s.numberFirstPage), pScene = Form.check("Print scene numbers", s.printSceneNumbers);
    private final JTextField header = Form.field(s.headerText, 24);
    private final JCheckBox auto = Form.check("Enable autosave", s.autosave), backup = Form.check("Keep a .bak copy when saving", s.backup);
    private final JSpinner secs = Form.spinner(s.autosaveSecs, 10, 3600, 10);

    // margins
    private final JSpinner mL = dbl(s.marginLeft), mR = dbl(s.marginRight), mT = dbl(s.marginTop), mB = dbl(s.marginBottom);
    private final JLabel marginInfo = W.label(" ", Font.PLAIN, 10);

    // element styles
    private final EnumMap<ElementType, Styles.S> work = new EnumMap<>(ElementType.class);
    private final JComboBox<ElementType> elem = new JComboBox<>(ElementType.values());
    private final JSpinner eIndent = Form.spinner(0, 0, 40, 1), eWidth = Form.spinner(0, 0, 80, 1), eBefore = Form.spinner(0, 0, 4, 1);
    private final JComboBox<String> eAlign = Form.combo(new String[] { "Left", "Centre", "Right" }, "Left");
    private final JCheckBox eBold = Form.check("Bold", false), eItalic = Form.check("Italic", false), eUnder = Form.check("Underline", false), eCaps = Form.check("ALL CAPS", false);
    private ElementType shown = ElementType.SCENE; private boolean loading;

    private static JSpinner dbl(double v) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(v, 0.25, 3.0, 0.05)); sp.setEditor(new JSpinner.NumberEditor(sp, "0.00")); sp.setFont(Theme.ui(Font.PLAIN, 10)); return sp;
    }

    public SettingsDialog(Frame owner, Runnable onApply) {
        super(owner, "SETTINGS", true);
        this.onApply = onApply;
        for (ElementType t : ElementType.values()) work.put(t, Styles.of(t).copy());

        scriptFont.setEditable(true); scriptFont.setFont(Theme.ui(Font.PLAIN, 10));
        LinkedHashSet<String> fams = new LinkedHashSet<>(List.of("Cascadia Mono", "Cascadia Code", "Consolas", "Courier New", "Courier Prime", "Liberation Mono", "DejaVu Sans Mono", "Monospaced"));
        fams.add(s.scriptFont); for (String f : fams) scriptFont.addItem(f); scriptFont.setSelectedItem(s.scriptFont);

        stack.add(page(f -> { f.header("Author"); f.row("Default author name", author, true); f.header("Recent files");
            JButton clear = new W.Btn("Clear recent list", null); clear.addActionListener(e -> s.recent.clear()); f.row(null, wrap(clear)); }), "General");
        stack.add(page(f -> { f.header("Theme"); f.row("Colour theme", theme); f.header("Fonts");
            f.row("Interface font size (px)", uiFont); f.row("Script font", scriptFont, true); f.row("Script font size (px)", scriptPx);
            f.header("Workspace"); f.row("Default zoom (%)", zoom); f.row(null, ruler); f.row(null, left); f.row(null, right); }), "Appearance");
        stack.add(page(f -> { f.header("Typing"); f.row(null, caps); f.row(null, smart); f.row(null, detect); f.header("Layout"); f.row(null, sceneNum);
            f.header("Spelling"); f.row(null, spell);
            JButton br = new W.Btn("Browse\u2026", null); br.addActionListener(e -> browse());
            JPanel dr = new JPanel(new BorderLayout(4, 0)); dr.setOpaque(false); dr.add(dict, BorderLayout.CENTER); dr.add(br, BorderLayout.EAST);
            f.row("Word list (.txt)", dr, true); }), "Editor");
        stack.add(page(f -> { f.header("Page"); f.row("Default page size", paper); f.header("Printing and PDF"); f.row(null, pTitle); f.row(null, pNums); f.row(null, pFirst);
            f.row(null, pScene); f.row("Header text", header, true); }), "Page & Print");
        stack.add(page(f -> { f.header("Page margins (inches)"); f.row("Left", mL); f.row("Right", mR); f.row("Top", mT); f.row("Bottom", mB);
            JButton std = new W.Btn("Standard screenplay margins", null); std.addActionListener(e -> { mL.setValue(1.5); mR.setValue(1.0); mT.setValue(1.0); mB.setValue(1.0); });
            f.row(null, wrap(std)); f.row(null, marginInfo);
            f.header("Notes"); f.row(null, W.label("<html>Margins apply to the page view, printing and PDF export.<br>Text area = page size minus margins (10 characters / 6 lines per inch).</html>", Font.PLAIN, 10)); }), "Page Margins");
        stack.add(page(f -> { f.header("Element"); f.row("Edit style of", elem); f.header("Layout");
            f.row("Indent from left margin (chars)", eIndent); f.row("Width (chars, 0 = full)", eWidth); f.row("Blank lines before", eBefore); f.row("Alignment", eAlign);
            f.header("Text"); f.row(null, W.row(8, eBold, eItalic, eUnder, eCaps));
            JButton r1 = new W.Btn("Reset this element", null), ra = new W.Btn("Reset all elements", null);
            r1.addActionListener(e -> { work.put(shown, Styles.defaults(shown)); loadFields(shown); }); ra.addActionListener(e -> { for (ElementType t : ElementType.values()) work.put(t, Styles.defaults(t)); loadFields(shown); });
            f.header("Defaults"); f.row(null, W.row(6, r1, ra)); }), "Element Styles");
        stack.add(page(f -> { f.header("Autosave"); f.row(null, auto); f.row("Interval (seconds)", secs); f.header("Files"); f.row(null, backup); }), "Autosave & Files");

        elem.setFont(Theme.ui(Font.PLAIN, 10)); elem.setFocusable(false);
        elem.setRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
            return super.getListCellRendererComponent(l, ((ElementType) v).label + (((ElementType) v).printed ? "" : "  (not printed)"), i, sel, foc); } });
        elem.addActionListener(e -> { if (loading) return; storeFields(shown); shown = (ElementType) elem.getSelectedItem(); loadFields(shown); });
        loadFields(shown);
        javax.swing.event.ChangeListener mc = e -> updateMarginInfo();
        for (JSpinner sp : new JSpinner[] { mL, mR, mT, mB }) sp.addChangeListener(mc);
        updateMarginInfo();

        cats.setSelectedIndex(0); cats.setFont(Theme.ui(Font.PLAIN, 10)); cats.setFixedCellHeight(28);
        cats.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        cats.addListSelectionListener(e -> cards.show(stack, cats.getSelectedValue()));
        JScrollPane lsp = new JScrollPane(cats); lsp.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.P.border)); lsp.setPreferredSize(new Dimension(170, 100));
        body.add(lsp, BorderLayout.WEST); body.add(stack, BorderLayout.CENTER);

        JButton reset = new W.Btn("Reset defaults", null), cancel = new W.Btn("Cancel", null), apply = new W.Btn("Apply", null), ok = new W.Btn("OK", null);
        ok.putClientProperty("FlatLaf.style", "buttonType:default"); getRootPane().setDefaultButton(ok);
        reset.addActionListener(e -> reset()); cancel.addActionListener(e -> dispose());
        apply.addActionListener(e -> commit()); ok.addActionListener(e -> { commit(); dispose(); });
        JPanel foot = new JPanel(new BorderLayout()); foot.add(W.row(6, reset), BorderLayout.WEST); foot.add(W.row(6, cancel, apply, ok), BorderLayout.EAST);
        foot.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        body.add(foot, BorderLayout.SOUTH);
        open(700, 500);
    }

    private JPanel wrap(JComponent c) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); p.setOpaque(false); p.add(c); return p; }
    private JComponent page(java.util.function.Consumer<Form> fill) { Form f = new Form(); fill.accept(f); f.glue(); JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.add(f); return p; }
    private void browse() { JFileChooser fc = new JFileChooser(); if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) dict.setText(fc.getSelectedFile().getAbsolutePath()); }

    private void loadFields(ElementType t) {
        loading = true; Styles.S st = work.get(t);
        elem.setSelectedItem(t); eIndent.setValue(st.indent); eWidth.setValue(st.width); eBefore.setValue(st.before); eAlign.setSelectedIndex(st.align);
        eBold.setSelected(st.bold); eItalic.setSelected(st.italic); eUnder.setSelected(st.underline); eCaps.setSelected(st.caps);
        loading = false;
    }
    private void storeFields(ElementType t) {
        Styles.S st = work.get(t);
        st.indent = (Integer) eIndent.getValue(); st.width = (Integer) eWidth.getValue(); st.before = (Integer) eBefore.getValue(); st.align = eAlign.getSelectedIndex();
        st.bold = eBold.isSelected(); st.italic = eItalic.isSelected(); st.underline = eUnder.isSelected(); st.caps = eCaps.isSelected();
    }
    private void updateMarginInfo() {
        double l = (Double) mL.getValue(), r = (Double) mR.getValue(), t = (Double) mT.getValue(), b = (Double) mB.getValue();
        StringBuilder sb = new StringBuilder("<html>");
        for (Paper p : Paper.values()) sb.append(p.label).append(": ").append(Math.max(0, (int) Math.floor((p.wIn - l - r) * 10 + 1e-6))).append(" columns \u00D7 ")
                .append(Math.max(0, (int) Math.floor((p.hIn - t - b) * 6 + 1e-6))).append(" lines<br>");
        marginInfo.setText(sb.append("</html>").toString());
    }

    private void commit() {
        storeFields(shown);
        s.author = author.getText().trim(); s.dark = theme.getSelectedItem().equals("Dark");
        s.uiFontPx = (Integer) uiFont.getValue(); s.scriptFontPx = (Integer) scriptPx.getValue(); s.zoom = (Integer) zoom.getValue();
        Object sf = scriptFont.getEditor().getItem(); s.scriptFont = sf == null || sf.toString().isBlank() ? "Cascadia Mono" : sf.toString().trim();
        s.showRuler = ruler.isSelected(); s.showLeft = left.isSelected(); s.showRight = right.isSelected();
        s.autoCaps = caps.isSelected(); s.smartEnter = smart.isSelected(); s.autoDetectScene = detect.isSelected();
        s.sceneNumbers = sceneNum.isSelected(); s.spell = spell.isSelected(); s.dictionary = dict.getText().trim();
        s.paper = (Paper) paper.getSelectedItem(); s.printTitlePage = pTitle.isSelected(); s.printPageNumbers = pNums.isSelected();
        s.numberFirstPage = pFirst.isSelected(); s.printSceneNumbers = pScene.isSelected(); s.headerText = header.getText();
        s.autosave = auto.isSelected(); s.autosaveSecs = (Integer) secs.getValue(); s.backup = backup.isSelected();
        s.marginLeft = (Double) mL.getValue(); s.marginRight = (Double) mR.getValue(); s.marginTop = (Double) mT.getValue(); s.marginBottom = (Double) mB.getValue();
        s.applyMargins();
        for (ElementType t : ElementType.values()) Styles.set(t, work.get(t));
        s.save(); onApply.run();
    }

    private void reset() {
        author.setText(""); theme.setSelectedItem("Light"); uiFont.setValue(12); scriptPx.setValue(14); scriptFont.setSelectedItem("Cascadia Mono"); zoom.setValue(100);
        ruler.setSelected(true); left.setSelected(true); right.setSelected(true);
        caps.setSelected(true); smart.setSelected(true); detect.setSelected(true); sceneNum.setSelected(false); spell.setSelected(false);
        dict.setText(""); paper.setSelectedItem(Paper.A4);
        pTitle.setSelected(true); pNums.setSelected(true); pFirst.setSelected(false); pScene.setSelected(false); header.setText("");
        auto.setSelected(true); secs.setValue(60); backup.setSelected(true);
        mL.setValue(1.5); mR.setValue(1.0); mT.setValue(1.0); mB.setValue(1.0);
        for (ElementType t : ElementType.values()) work.put(t, Styles.defaults(t)); loadFields(shown);
    }
}
