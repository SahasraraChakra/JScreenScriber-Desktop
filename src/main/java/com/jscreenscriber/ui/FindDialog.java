package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;

import javax.swing.*;
import java.awt.*;

/** Non-modal find & replace. */
public class FindDialog extends ChromeDialog {
    private final PageView view;
    private final JTextField find = Form.field("", 22), repl = Form.field("", 22);
    private final JCheckBox matchCase = Form.check("Match case", false);
    private final JLabel msg = W.label(" ", Font.PLAIN, 10);

    public FindDialog(Frame owner, PageView view) {
        super(owner, "FIND & REPLACE", false);
        this.view = view;
        Form f = new Form(); f.row("Find", find, true); f.row("Replace with", repl, true); f.row(null, matchCase); f.row(null, msg);
        JButton next = new W.Btn("Find next", null), prev = new W.Btn("Previous", null), rep = new W.Btn("Replace", null), all = new W.Btn("Replace all", null), close = new W.Btn("Close", null);
        next.addActionListener(e -> go(true)); prev.addActionListener(e -> go(false)); find.addActionListener(e -> go(true));
        rep.addActionListener(e -> { msg.setText(view.replaceCurrent(find.getText(), repl.getText(), matchCase.isSelected()) ? " " : "No more matches"); });
        all.addActionListener(e -> msg.setText(view.replaceAll(find.getText(), repl.getText(), matchCase.isSelected()) + " replaced"));
        close.addActionListener(e -> dispose());
        JPanel btn = W.row(6, next, prev, rep, all, close); btn.setBorder(BorderFactory.createEmptyBorder(0, 6, 8, 6));
        body.add(f, BorderLayout.CENTER); body.add(btn, BorderLayout.SOUTH);
        if (view.selectedText().length() > 0 && !view.selectedText().contains("\n")) find.setText(view.selectedText());
        open(420, 200);
    }
    private void go(boolean fwd) { msg.setText(view.find(find.getText(), matchCase.isSelected(), fwd) ? " " : "Not found"); }
}
