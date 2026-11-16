package com.jscreenscriber.ui;

import com.jscreenscriber.Theme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Dockable list pane (scene headings / characters): header, FIND filter, list, 12px scrollbar, footer. */
public class SidePanel extends JPanel implements W.Themed {
    public static final class Item {
        public String left = "", right = "", sub = "", subRight = "", tag = ""; public boolean active; public int key;
    }

    private final JLabel titleL = new JLabel();
    private final JButton collapse = headBtn("Collapse"), close = headBtn("Close");
    private final JLabel findL = new JLabel("FIND:");
    private final JTextField find = new JTextField();
    private final JLabel footer = new JLabel();
    private final JScrollPane scroll;
    private final ListPane list = new ListPane();
    private final JPanel header = new JPanel(new BorderLayout()), search = new JPanel(new BorderLayout(5, 0)), center = new JPanel(new BorderLayout());
    private List<Item> items = new ArrayList<>(), shown = new ArrayList<>();
    private IntConsumer onPick = k -> {};
    private boolean collapsed;
    private Runnable onClose = () -> {};

    public SidePanel(String title, String placeholder, boolean rightSide) {
        super(new BorderLayout());
        titleL.setText(title);
        setPreferredSize(new Dimension(200, 100));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 3)); btns.setOpaque(false);
        btns.add(collapse); btns.add(close);
        header.add(titleL, BorderLayout.WEST); header.add(btns, BorderLayout.EAST);
        header.setPreferredSize(new Dimension(100, 30)); header.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
        find.putClientProperty("JTextField.placeholderText", placeholder);
        find.putClientProperty("FlatLaf.style", "arc:2; margin:2,6,2,6");
        search.add(findL, BorderLayout.WEST); search.add(find, BorderLayout.CENTER);
        search.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JPanel top = new JPanel(new BorderLayout()); top.add(header, BorderLayout.NORTH); top.add(search, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBar(new GripScrollBar(JScrollBar.VERTICAL));
        scroll.setBorder(null);
        center.add(scroll, BorderLayout.CENTER);
        footer.setHorizontalAlignment(SwingConstants.LEFT); footer.setPreferredSize(new Dimension(100, 24));
        center.add(footer, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        find.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refilter(); } public void removeUpdate(DocumentEvent e) { refilter(); } public void changedUpdate(DocumentEvent e) { refilter(); }
        });
        collapse.addActionListener(e -> { collapsed = !collapsed; center.setVisible(!collapsed); search.setVisible(!collapsed); updateIcons(); revalidate(); });
        close.addActionListener(e -> onClose.run());
        applyTheme();
    }

    public void setOnPick(IntConsumer c) { onPick = c; }
    public void setOnClose(Runnable r) { onClose = r; }
    public void setFooter(String s) { footer.setText(s); }
    public void setItems(List<Item> its) { items = its; refilter(); }

    public void markActive(java.util.function.IntPredicate active, java.util.function.BiConsumer<Integer, Item> tweak) {
        for (int i = 0; i < items.size(); i++) { Item it = items.get(i); it.active = active.test(i); tweak.accept(i, it); }
        list.repaint();
    }

    private void refilter() {
        String q = find.getText().trim().toLowerCase();
        shown = new ArrayList<>();
        for (Item it : items) if (q.isEmpty() || it.left.toLowerCase().contains(q)) shown.add(it);
        list.revalidate(); list.repaint();
    }

    private JButton headBtn(String tip) {
        JButton b = new JButton(); b.setToolTipText(tip); b.setFocusable(false); b.setPreferredSize(new Dimension(24, 24)); b.setMargin(new Insets(2, 2, 2, 2));
        return b;
    }
    private void updateIcons() {
        Theme.Palette p = Theme.P;
        collapse.setIcon(Icons.get(collapsed ? "chevdown" : "chevup", p.text, 11));
        close.setIcon(Icons.get("close", p.closeFg, 11));
    }

    public void applyTheme() {
        Theme.Palette p = Theme.P;
        setBackground(p.sidebar);
        header.setBackground(p.sideHeader); header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, p.border), BorderFactory.createEmptyBorder(0, 8, 0, 4)));
        search.setBackground(p.sidebar); search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, p.border), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        titleL.setFont(Theme.ui(Font.BOLD, 10)); titleL.setForeground(p.text);
        findL.setFont(Theme.ui(Font.BOLD, 10)); findL.setForeground(p.muted);
        find.setFont(Theme.ui(Font.PLAIN, 10)); find.setPreferredSize(new Dimension(100, 26));
        footer.setFont(Theme.ui(Font.PLAIN, 9)); footer.setForeground(p.muted);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, p.border), BorderFactory.createEmptyBorder(0, 8, 0, 0)));
        footer.setOpaque(true); footer.setBackground(p.sideHeader);
        updateIcons();
        collapse.putClientProperty("FlatLaf.style", "arc:3; margin:2,2,2,2; minimumWidth:24; focusWidth:0");
        close.putClientProperty("FlatLaf.style", "arc:3; margin:2,2,2,2; minimumWidth:24; focusWidth:0; background:" + hex(p.closeBg)
                + "; borderColor:" + hex(p.closeBorder) + "; foreground:" + hex(p.closeFg) + "; hoverBackground:" + hex(p.closeFg));
        scroll.getViewport().setBackground(p.sidebar);
        setBorder(null);
        repaint();
    }
    private static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }

    /** Owner-drawn list: two-line items with optional tag chip, matching the mock's cards. */
    private final class ListPane extends JComponent implements Scrollable {
        int hover = -1;
        ListPane() {
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { int i = idx(e.getY()); if (i >= 0 && i < shown.size()) onPick.accept(shown.get(i).key); }
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
            });
            addMouseMotionListener(new MouseMotionAdapter() { @Override public void mouseMoved(MouseEvent e) { hover = idx(e.getY()); repaint(); } });
        }
        int rowH(Item it) { return it.sub.isEmpty() && it.subRight.isEmpty() ? 26 : 44; }
        int idx(int y) { int yy = 4; for (int i = 0; i < shown.size(); i++) { int h = rowH(shown.get(i)); if (y >= yy && y < yy + h) return i; yy += h + 2; } return -1; }
        @Override public Dimension getPreferredSize() { int h = 8; for (Item it : shown) h += rowH(it) + 2; return new Dimension(100, h); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); Theme.Palette p = Theme.P;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(p.sidebar); g.fillRect(0, 0, getWidth(), getHeight());
            int y = 4, w = getWidth() - 8;
            for (int i = 0; i < shown.size(); i++) {
                Item it = shown.get(i); int h = rowH(it);
                g.setColor(it.active ? p.listActive : i == hover ? p.listHover : p.listItem);
                g.fillRoundRect(4, y, w, h, 3, 3);
                Color fg = it.active ? p.accentDark : p.text;
                Font f = Theme.ui(it.active ? Font.BOLD : (it.sub.isEmpty() ? Font.PLAIN : Font.BOLD), 10);
                g.setFont(f); FontMetrics fm = g.getFontMetrics();
                int line1 = y + ((it.sub.isEmpty() ? h : 26) + fm.getAscent() - fm.getDescent()) / 2;
                int rx = 4 + w - 6;
                if (!it.tag.isEmpty()) {
                    Font tf = Theme.ui(Font.BOLD, 9); g.setFont(tf); FontMetrics tm = g.getFontMetrics();
                    int tw = tm.stringWidth(it.tag) + 10, th = tm.getHeight() + 2, ty = y + (26 - th) / 2;
                    g.setColor(it.active ? p.accentBg : p.tagBg); g.fillRoundRect(rx - tw, ty, tw, th, 3, 3);
                    g.setColor(it.active ? p.accentBorder : p.btnBorder); g.drawRoundRect(rx - tw, ty, tw, th, 3, 3);
                    g.setColor(it.active ? p.accentDark : p.tag); g.drawString(it.tag, rx - tw + 5, ty + 1 + tm.getAscent());
                    rx -= tw + 4;
                    g.setFont(f);
                }
                if (!it.right.isEmpty()) {
                    g.setFont(Theme.ui(Font.PLAIN, 10)); fm = g.getFontMetrics();
                    g.setColor(it.active ? p.accentDark : p.muted);
                    int rw = fm.stringWidth(it.right); g.drawString(it.right, rx - rw, line1); rx -= rw + 6; g.setFont(f);
                }
                g.setColor(fg); g.setClip(4, y, Math.max(0, rx - 4), h);
                g.setFont(f); g.drawString(it.left, 10, line1); g.setClip(null);
                if (!it.sub.isEmpty() || !it.subRight.isEmpty()) {
                    g.setFont(Theme.ui(it.active ? Font.BOLD : Font.PLAIN, 9)); fm = g.getFontMetrics();
                    g.setColor(it.active ? p.accent : p.muted);
                    g.drawString(it.sub, 10, y + h - 9);
                    int sw = fm.stringWidth(it.subRight); g.drawString(it.subRight, 4 + w - 6 - sw, y + h - 9);
                }
                y += h + 2;
            }
            g.dispose();
        }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 24; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return getParent() instanceof JViewport v && getPreferredSize().height < v.getHeight(); }
    }
}
