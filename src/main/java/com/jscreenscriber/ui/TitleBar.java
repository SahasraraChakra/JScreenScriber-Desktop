package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;

import javax.swing.*;
import java.awt.*;

/** 28px custom title bar: logo, title, theme toggle, status chips, minimise / maximise / close. */
public class TitleBar extends JPanel implements W.Themed {
    private final JLabel title = new JLabel();
    private final JLabel logo = new JLabel();
    public final JButton theme = new JButton();
    public final W.Chip format = new W.Chip("FOUNTAIN"), autosave = new W.Chip("AUTOSAVE:"), saved = new W.Chip("SAVED");
    private final JSeparator sep1 = new JSeparator(JSeparator.VERTICAL), sep2 = new JSeparator(JSeparator.VERTICAL);
    public final JButton min = winBtn("Minimize"), max = winBtn("Maximize"), close = winBtn("Close");
    private static final Image LOGO = new ImageIcon(TitleBar.class.getResource("/icons/app-logo.png")).getImage();

    public TitleBar(boolean withExtras) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(100, 32));
        sep1.setPreferredSize(new Dimension(3, 18)); sep2.setPreferredSize(new Dimension(3, 18));
        logo.setIcon(new ImageIcon(LOGO.getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
        logo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 0));
        left.add(logo); left.add(title);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4)); right.setOpaque(false);
        if (withExtras) {
            styleThemeButton();
            right.add(theme); right.add(format); right.add(sep1); right.add(autosave); right.add(sep2); right.add(saved);
        }
        JPanel wins = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); wins.setOpaque(false);
        wins.add(min); wins.add(max); wins.add(close);
        right.add(Box.createHorizontalStrut(4)); right.add(wins);
        add(right, BorderLayout.EAST);
        applyTheme();
    }

    private void styleThemeButton() {
        theme.setFocusable(false); theme.setToolTipText("Toggle Light/Dark Mode");
        theme.putClientProperty("FlatLaf.style", "arc:3; margin:0,5,0,5; minimumWidth:0; focusWidth:0");
    }

    private JButton winBtn(String tip) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getForeground()); int cx = getWidth() / 2, cy = getHeight() / 2;
                g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (tip) {
                    case "Minimize" -> g.drawLine(cx - 3, cy + 1, cx + 3, cy + 1);
                    case "Maximize" -> g.drawRoundRect(cx - 3, cy - 3, 6, 6, 1, 1);
                    default -> { g.drawLine(cx - 3, cy - 3, cx + 3, cy + 3); g.drawLine(cx + 3, cy - 3, cx - 3, cy + 3); }
                }
                g.dispose();
            }
        };
        b.setToolTipText(tip); b.setFocusable(false); b.setPreferredSize(new Dimension(26, 22));
        b.putClientProperty("FlatLaf.style", "arc:3; margin:0,0,0,0; minimumWidth:0; focusWidth:0");
        return b;
    }

    public void setTitleText(String t) { title.setText(t); }

    public void setState(boolean autosaveOn, boolean isSaved) {
        Theme.Palette p = Theme.P;
        autosave.set("AUTOSAVE:", autosaveOn ? "ON" : "OFF", autosaveOn ? p.ok : p.muted, false, autosaveOn ? (Theme.dark ? p.ok : new Color(0x15803d)) : p.muted);
        saved.set(isSaved ? "SAVED" : "MODIFIED", "", isSaved ? p.accent : new Color(0xEF7D19), isSaved, null);
        format.set("FOUNTAIN", "", null, false, null);
    }

    public void applyTheme() {
        Theme.Palette p = Theme.P;
        setBackground(p.titlebar); setForeground(p.text);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, p.border));
        title.setFont(Theme.ui(Font.BOLD, 11f)); title.setForeground(p.text);
        sep1.setForeground(p.border); sep2.setForeground(p.border);
        theme.setIcon(Icons.get(Theme.dark ? "sun" : "moon", p.text));
        theme.setText(Theme.dark ? "LIGHT" : "DARK"); theme.setText(Theme.dark ? "LIGHT" : null);
        theme.setMargin(new Insets(0, 4, 0, 4)); theme.setFont(Theme.ui(Font.PLAIN, 10)); theme.setPreferredSize(new Dimension(Theme.dark ? 66 : 28, 22));
        for (JButton b : new JButton[] { min, max, close }) {
            boolean c = b == close;
            b.setForeground(c ? p.closeFg : p.text);
            b.putClientProperty("FlatLaf.style", "arc:3; margin:0,0,0,0; minimumWidth:0; focusWidth:0; "
                    + "background:" + hex(c ? p.closeBg : p.btnHover) + "; borderColor:" + hex(c ? p.closeBorder : p.inputBorder)
                    + "; hoverBackground:" + hex(c ? p.closeFg : p.btnBorder) + "; pressedBackground:" + hex(c ? p.closeFg.darker() : p.btnHover));
        }
        close.addPropertyChangeListener("model", e -> {});
        format.applyTheme(); autosave.applyTheme(); saved.applyTheme();
        revalidate(); repaint();
    }
    private static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }
}
