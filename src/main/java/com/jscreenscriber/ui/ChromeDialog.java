package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/** Undecorated dialog that reuses the main window's title-bar look. */
public class ChromeDialog extends JDialog implements W.Themed {
    protected final TitleBar bar = new TitleBar(false);
    protected final JPanel body = new JPanel(new BorderLayout());

    public ChromeDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        setUndecorated(true);
        bar.setTitleText(title);
        bar.min.setVisible(false); bar.max.setVisible(false);
        bar.close.addActionListener(e -> dispose());
        JPanel root = new JPanel(new BorderLayout());
        root.add(bar, BorderLayout.NORTH); root.add(body, BorderLayout.CENTER);
        setContentPane(root);
        WindowChrome.install(this, bar, null);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        applyTheme();
    }

    public void applyTheme() {
        ((JComponent) getContentPane()).setBorder(BorderFactory.createLineBorder(Theme.P.border, 1));
        body.setBackground(Theme.P.win);
    }

    public void open(int w, int h) {
        setSize(w, h); setLocationRelativeTo(getOwner()); W.refresh(getContentPane()); setVisible(true);
    }
}
