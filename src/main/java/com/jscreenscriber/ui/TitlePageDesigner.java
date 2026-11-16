package com.jscreenscriber.ui;

import com.jscreenscriber.Settings;
import com.jscreenscriber.Theme;
import com.jscreenscriber.model.*;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

/** Title page designer: fields + layout options on the left, live paper preview on the right. */
public class TitlePageDesigner extends ChromeDialog {
    private final ScriptDocument doc;
    private final TitlePage tp;
    private final Consumer<TitlePage> onApply;
    private final Preview preview = new Preview();

    private final JTextArea title = new JTextArea(), contact = new JTextArea(), notes = new JTextArea();
    private final JTextField credit = Form.field("", 18), author = Form.field("", 18), source = Form.field("", 18), date = Form.field("", 18), copyright = Form.field("", 18);
    private final JComboBox<String> preset = Form.combo(new String[] { "Classic", "Centered", "Minimal" }, "Classic");
    private final JComboBox<String> size = Form.combo(new String[] { "Small", "Medium", "Large" }, "Medium");
    private final JComboBox<String> contactSide = Form.combo(new String[] { "Left", "Right" }, "Left");
    private final JComboBox<String> dateSide = Form.combo(new String[] { "Right", "Left", "Hidden" }, "Right");
    private final JCheckBox bold = Form.check("Bold", true), underline = Form.check("Underline", false), caps = Form.check("ALL CAPS", true), include = Form.check("Include title page when printing", true);

    public TitlePageDesigner(Frame owner, ScriptDocument doc, Consumer<TitlePage> onApply) {
        super(owner, "TITLE PAGE DESIGNER", true);
        this.doc = doc; this.tp = doc.title.copy(); this.onApply = onApply;
        load();

        Form f = new Form();
        f.header("Content");
        f.row("Title", Form.area(title, 2), true); f.row("Credit", credit, true); f.row("Author", author, true); f.row("Source", source, true);
        f.row("Draft date", date, true); f.row("Contact", Form.area(contact, 3), true); f.row("Copyright", copyright, true); f.row("Notes", Form.area(notes, 2), true);
        f.header("Design");
        f.row("Layout", preset); f.row("Title size", size); f.row(null, W.row(8, bold, underline, caps));
        f.row("Contact block", contactSide); f.row("Date block", dateSide); f.row(null, include);
        f.glue();
        JScrollPane left = new JScrollPane(f, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        left.setVerticalScrollBar(new GripScrollBar(JScrollBar.VERTICAL)); left.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.P.border));
        left.setPreferredSize(new Dimension(330, 100)); left.getViewport().setOpaque(false); left.setOpaque(false);
        body.add(left, BorderLayout.WEST); body.add(preview, BorderLayout.CENTER);

        JButton fill = new W.Btn("Use default author", null), cancel = new W.Btn("Cancel", null), apply = new W.Btn("Apply", null);
        apply.putClientProperty("FlatLaf.style", "buttonType:default"); getRootPane().setDefaultButton(apply);
        fill.addActionListener(e -> author.setText(Settings.get().author));
        cancel.addActionListener(e -> dispose());
        apply.addActionListener(e -> { collect(); onApply.accept(tp); dispose(); });
        JPanel foot = new JPanel(new BorderLayout()); foot.add(W.row(6, fill), BorderLayout.WEST); foot.add(W.row(6, cancel, apply), BorderLayout.EAST);
        foot.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8)); body.add(foot, BorderLayout.SOUTH);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { upd(); } public void removeUpdate(javax.swing.event.DocumentEvent e) { upd(); } public void changedUpdate(javax.swing.event.DocumentEvent e) { upd(); }
        };
        for (javax.swing.text.JTextComponent c : new javax.swing.text.JTextComponent[] { title, contact, notes, credit, author, source, date, copyright }) c.getDocument().addDocumentListener(dl);
        for (JComboBox<?> c : new JComboBox<?>[] { preset, size, contactSide, dateSide }) c.addActionListener(e -> upd());
        for (JCheckBox c : new JCheckBox[] { bold, underline, caps, include }) c.addActionListener(e -> upd());
        open(840, 560);
    }

    private void load() {
        title.setText(tp.get("Title")); credit.setText(tp.get("Credit")); author.setText(tp.get("Author")); source.setText(tp.get("Source"));
        date.setText(tp.get("Draft date")); contact.setText(tp.get("Contact")); copyright.setText(tp.get("Copyright")); notes.setText(tp.get("Notes"));
        preset.setSelectedItem(tp.preset); size.setSelectedIndex(tp.titleSize - 1);
        contactSide.setSelectedItem(cap(tp.contactSide)); dateSide.setSelectedItem(tp.dateSide.equals("none") ? "Hidden" : cap(tp.dateSide));
        bold.setSelected(tp.titleBold); underline.setSelected(tp.titleUnderline); caps.setSelected(tp.titleCaps); include.setSelected(tp.enabled);
    }
    private static String cap(String s) { return s.substring(0, 1).toUpperCase() + s.substring(1); }

    private void collect() {
        tp.set("Title", title.getText().trim()); tp.set("Credit", credit.getText().trim()); tp.set("Author", author.getText().trim());
        tp.set("Source", source.getText().trim()); tp.set("Draft date", date.getText().trim()); tp.set("Contact", contact.getText().trim());
        tp.set("Copyright", copyright.getText().trim()); tp.set("Notes", notes.getText().trim());
        tp.preset = (String) preset.getSelectedItem(); tp.titleSize = size.getSelectedIndex() + 1;
        tp.contactSide = ((String) contactSide.getSelectedItem()).toLowerCase();
        String ds = (String) dateSide.getSelectedItem(); tp.dateSide = ds.equals("Hidden") ? "none" : ds.toLowerCase();
        tp.titleBold = bold.isSelected(); tp.titleUnderline = underline.isSelected(); tp.titleCaps = caps.isSelected(); tp.enabled = include.isSelected();
    }
    private void upd() { collect(); preview.repaint(); }

    private final class Preview extends JComponent {
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); Theme.Palette p = Theme.P;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(p.viewport); g.fillRect(0, 0, getWidth(), getHeight());
            Paper pp = doc.paper;
            double ppi = Math.min((getWidth() - 40) / pp.wIn, (getHeight() - 40) / pp.hIn);
            double w = pp.wIn * ppi, h = pp.hIn * ppi, x = (getWidth() - w) / 2, y = (getHeight() - h) / 2;
            g.setColor(new Color(0, 0, 0, Theme.dark ? 70 : 28)); g.fill(new Rectangle2D.Double(x + 2, y + 4, w, h));
            g.setColor(p.page); g.fill(new Rectangle2D.Double(x, y, w, h)); g.setColor(p.pageBorder); g.draw(new Rectangle2D.Double(x, y, w, h));
            g.translate(x, y);
            TitlePageRenderer.paint(g, tp, pp, ppi, p.text);
            if (!tp.enabled) { g.setFont(Theme.ui(Font.BOLD, 10)); g.setColor(p.muted); g.drawString("Title page excluded from print", 8, 14); }
            g.dispose();
        }
    }
}
