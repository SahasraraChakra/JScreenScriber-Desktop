package com.jscreenscriber.ui;

import com.jscreenscriber.Settings;
import com.jscreenscriber.Theme;
import com.jscreenscriber.model.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/** The application window: custom title bar, menus, toolbars, three-pane workspace and status bar. */
public class MainFrame extends JFrame {
    private final Settings st = Settings.get();
    private ScriptDocument doc;
    private PageView view;
    private final TitleBar titleBar = new TitleBar(true);
    private final JMenuBar menuBar = new JMenuBar();
    private final JLabel modeLabel = new JLabel();
    private final JPanel menuRow = new JPanel(new BorderLayout()), toolbar = new JPanel(new BorderLayout()), elementBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 6));
    private final SidePanel scenes = new SidePanel("SCENE HEADINGS", "Filter scenes...", false), chars = new SidePanel("CHARACTERS", "Find character...", true);
    private JPanel wrapper;
    private final StatusBar status = new StatusBar();
    private final CardsView cards = new CardsView();
    private final CardLayout centerCards = new CardLayout();
    private final JPanel center = new JPanel(centerCards), editorColumn = new JPanel(new BorderLayout());
    private JScrollPane scroll;
    private RulerPanel ruler;
    private W.Tgl panelsBtn, cardsBtn, focusBtn;
    private final List<W.Tgl> pills = new ArrayList<>();
    private final List<JMenu> menus = new ArrayList<>();
    private JMenu recentMenu;
    private JCheckBoxMenuItem miDark, miPanels, miLeft, miRight, miRuler, miSceneNum, miCards, miFocus, miSpell, miTitlePrint;
    private JRadioButtonMenuItem miA4, miLetter;
    private boolean cardsOn, focusOn;
    private int sinceSave;
    private boolean updatingUi;

    public MainFrame(ScriptDocument initial) {
        super("JScreenScriber");
        setUndecorated(true);
        setIconImage(new ImageIcon(MainFrame.class.getResource("/icons/app-logo.png")).getImage());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(760, 480));
        doc = initial;
        view = new PageView(doc);
        build();
        bind();
        setSize(1200, 740); setLocationRelativeTo(null);
        WindowChrome.install(this, titleBar, this::toggleMax);
        addWindowStateListener(e -> WindowChrome.setEdgesVisible(wrapper, (e.getNewState() & MAXIMIZED_BOTH) == 0));
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { exit(); } });
        Theme.onChange(this::themeChanged);
        applySettings(false);
        javax.swing.Timer t = new javax.swing.Timer(1000, e -> autosaveTick()); t.start();
    }

    // ───────────────────────── construction ─────────────────────────
    private void build() {
        JPanel root = new JPanel(new BorderLayout());
        wrapper = WindowChrome.wrapResizable(this, root, getMinimumSize()); setContentPane(wrapper);

        // title bar buttons
        titleBar.min.addActionListener(e -> setExtendedState(ICONIFIED));
        titleBar.max.addActionListener(e -> toggleMax());
        titleBar.close.addActionListener(e -> exit());
        titleBar.theme.addActionListener(e -> setDark(!Theme.dark));

        // menu row
        buildMenus();
        modeLabel.setText("<html><b style='color:" + hex(Theme.P.accent) + "'>CUA-91</b> MODE</html>");
        modeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        menuRow.add(menuBar, BorderLayout.CENTER); menuRow.add(modeLabel, BorderLayout.EAST);
        menuRow.setPreferredSize(new Dimension(100, 28));
        menuBar.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        buildToolbar();

        JPanel north = new JPanel(); north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(titleBar); north.add(menuRow); north.add(toolbar);
        root.add(north, BorderLayout.NORTH);

        // editor column
        buildElementBar();
        scroll = new JScrollPane(view, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setVerticalScrollBar(new GripScrollBar(JScrollBar.VERTICAL));
        scroll.setHorizontalScrollBar(new GripScrollBar(JScrollBar.HORIZONTAL));
        scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new JPanel());
        scroll.setBorder(null);
        ruler = new RulerPanel(view, scroll.getViewport());
        JPanel top = new JPanel(new BorderLayout()); top.add(elementBar, BorderLayout.NORTH); top.add(ruler, BorderLayout.SOUTH);
        editorColumn.add(top, BorderLayout.NORTH); editorColumn.add(scroll, BorderLayout.CENTER);
        JScrollPane cardScroll = new JScrollPane(cards); cardScroll.setVerticalScrollBar(new GripScrollBar(JScrollBar.VERTICAL)); cardScroll.setBorder(null);
        center.add(editorColumn, "page"); center.add(cardScroll, "cards");

        JPanel work = new JPanel(new BorderLayout());
        work.add(scenes, BorderLayout.WEST); work.add(center, BorderLayout.CENTER); work.add(chars, BorderLayout.EAST);
        root.add(work, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);

        scenes.setOnPick(b -> { view.jumpToBlock(b); showCards(false); });
        chars.setOnPick(this::pickCharacter);
        scenes.setOnClose(() -> { st.showLeft = false; layoutPanels(); });
        chars.setOnClose(() -> { st.showRight = false; layoutPanels(); });
        cards.setOnPick(b -> { showCards(false); view.jumpToBlock(b); });
        cards.setOnMove(this::moveScene);

        status.click("Page Format :", this::pagePopup);
        status.clickZoom(() -> zoomBy(-10), () -> zoomBy(10), () -> setZoom(100));
        status.click("ins", () -> { view.toggleOverwrite(); updateStatus(); });
        view.setZoomListener(this::updateStatus);

        // global keys
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        am.put("esc", new AbstractAction() { public void actionPerformed(ActionEvent e) { if (focusOn) setFocusMode(false); else if (cardsOn) showCards(false); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "focus");
        am.put("focus", new AbstractAction() { public void actionPerformed(ActionEvent e) { setFocusMode(!focusOn); } });
        getRootPane().setBorder(null);
    }

    private static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }

    private void bind() {
        doc.addListener(this::docChanged);
        view.setCaretListener(this::caretChanged);
        view.setZoom(st.zoom);
        docChanged(); caretChanged();
    }

    // ───────────────────────── menus ─────────────────────────
    private JMenu menu(String text, char hot) {
        JMenu m = new JMenu(text); m.setMnemonic(hot); m.putClientProperty("hot", hot); m.putClientProperty("plain", text);
        menuBar.add(m); menus.add(m); return m;
    }
    private JMenuItem item(JMenu m, String text, String accel, Runnable r) {
        JMenuItem i = new JMenuItem(text);
        if (accel != null) i.setAccelerator(KeyStroke.getKeyStroke(accel));
        i.addActionListener(e -> r.run()); m.add(i); return i;
    }
    private JCheckBoxMenuItem check(JMenu m, String text, String accel, Runnable r) {
        JCheckBoxMenuItem i = new JCheckBoxMenuItem(text);
        if (accel != null) i.setAccelerator(KeyStroke.getKeyStroke(accel));
        i.addActionListener(e -> { if (!updatingUi) r.run(); }); m.add(i); return i;
    }

    private void buildMenus() {
        JMenu f = menu("File", 'F');
        item(f, "New", "ctrl N", this::newDoc); item(f, "Open\u2026", "ctrl O", this::open);
        recentMenu = new JMenu("Open Recent"); f.add(recentMenu);
        f.addSeparator(); item(f, "Save", "ctrl S", this::save); item(f, "Save As\u2026", "ctrl shift S", this::saveAs);
        f.addSeparator(); item(f, "Print\u2026", "ctrl P", this::print); item(f, "Export PDF\u2026", "ctrl E", this::exportPdf); f.addSeparator(); item(f, "Exit", "ctrl Q", this::exit);
        f.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuSelected(javax.swing.event.MenuEvent e) { fillRecent(); }
            public void menuDeselected(javax.swing.event.MenuEvent e) {} public void menuCanceled(javax.swing.event.MenuEvent e) {} });

        JMenu e = menu("Edit", 'E');
        item(e, "Undo", "ctrl Z", () -> view.undo()); item(e, "Redo", "ctrl Y", () -> view.redo()); e.addSeparator();
        item(e, "Cut", "ctrl X", () -> view.cut()); item(e, "Copy", "ctrl C", () -> view.copy()); item(e, "Paste", "ctrl V", () -> view.paste());
        item(e, "Select All", "ctrl A", () -> view.selectAll()); e.addSeparator();
        item(e, "Find & Replace\u2026", "ctrl F", this::find);

        JMenu v = menu("View", 'V');
        JMenu ps = new JMenu("Page Size"); v.add(ps);
        ButtonGroup bg = new ButtonGroup();
        miA4 = new JRadioButtonMenuItem("A4"); miLetter = new JRadioButtonMenuItem("Letter");
        miA4.addActionListener(x -> setPaper(Paper.A4)); miLetter.addActionListener(x -> setPaper(Paper.LETTER));
        bg.add(miA4); bg.add(miLetter); ps.add(miA4); ps.add(miLetter);
        item(v, "Fit Page Width", "ctrl shift 0", this::fitWidth); item(v, "Zoom In", "ctrl EQUALS", () -> zoomBy(10)); item(v, "Zoom Out", "ctrl MINUS", () -> zoomBy(-10)); item(v, "Reset Zoom", "ctrl 0", () -> setZoom(100));
        v.addSeparator();
        miPanels = check(v, "Panels", null, () -> { boolean on = !(st.showLeft || st.showRight); st.showLeft = on; st.showRight = on; layoutPanels(); });
        miLeft = check(v, "Scene Headings Panel", null, () -> { st.showLeft = !st.showLeft; layoutPanels(); });
        miRight = check(v, "Characters Panel", null, () -> { st.showRight = !st.showRight; layoutPanels(); });
        miRuler = check(v, "Text Ruler", null, () -> { st.showRuler = !st.showRuler; layoutPanels(); });
        miSceneNum = check(v, "Scene Numbers", null, () -> { st.sceneNumbers = !st.sceneNumbers; view.repaint(); });
        v.addSeparator();
        miCards = check(v, "Cards View", "ctrl K", () -> showCards(!cardsOn)); miFocus = check(v, "Focus Mode", "F11", () -> setFocusMode(!focusOn));
        miDark = check(v, "Dark Mode", "ctrl shift D", () -> setDark(!Theme.dark));

        JMenu fm = menu("Format", 'o');
        String[] keys = { "ctrl 1", "ctrl 2", "ctrl 3", "ctrl 4", "ctrl 5", "ctrl 6", "ctrl 7" };
        for (int i = 0; i < 7; i++) { ElementType t = ElementType.TOOLBAR[i]; item(fm, t.label, keys[i], () -> view.setType(t)); }
        fm.addSeparator();
        item(fm, "Bold", "ctrl B", () -> view.toggleMarkup("**")); item(fm, "Italic", "ctrl I", () -> view.toggleMarkup("*")); item(fm, "Underline", "ctrl U", () -> view.toggleMarkup("_"));

        JMenu in = menu("Insert", 'I');
        item(in, "Page Break", null, () -> view.insertBlock(ElementType.PAGE_BREAK, "")); item(in, "Centered Text", null, () -> view.insertBlock(ElementType.CENTERED, ""));
        item(in, "Note", null, () -> view.insertBlock(ElementType.NOTE, "")); item(in, "Section", null, () -> view.insertBlock(ElementType.SECTION, "# "));
        item(in, "Synopsis", null, () -> view.insertBlock(ElementType.SYNOPSIS, ""));

        JMenu pr = menu("Production", 'P');
        item(pr, "Title Page Designer\u2026", "ctrl shift T", this::titlePage);
        miTitlePrint = check(pr, "Print Title Page", null, () -> st.printTitlePage = !st.printTitlePage);
        pr.addSeparator();
        item(pr, "Next Scene", "alt DOWN", () -> stepScene(1)); item(pr, "Previous Scene", "alt UP", () -> stepScene(-1)); pr.addSeparator();
        item(pr, "Script Statistics\u2026", null, this::statistics);

        JMenu to = menu("Tools", 'T');
        miSpell = check(to, "Spell Check", "F7", () -> toggleSpell());
        item(to, "Settings\u2026", "ctrl COMMA", this::settings);

        JMenu wi = menu("Window", 'W');
        item(wi, "Minimize", null, () -> setExtendedState(ICONIFIED)); item(wi, "Maximize / Restore", null, this::toggleMax);

        JMenu he = menu("Help", 'H');
        item(he, "Keyboard Shortcuts", "F1", this::shortcuts); item(he, "About JScreenScriber", null, this::about);
        styleMenus();
    }

    private void styleMenus() {
        Theme.Palette p = Theme.P;
        for (JMenu m : menus) {
            char hot = (Character) m.getClientProperty("hot"); String t = (String) m.getClientProperty("plain");
            int i = Character.toLowerCase(t.charAt(0)) == Character.toLowerCase(hot) ? 0 : t.toLowerCase().indexOf(Character.toLowerCase(hot));
            m.setText("<html>" + t.substring(0, i) + "<u style='color:" + hex(p.accent) + "'>" + t.charAt(i) + "</u>" + t.substring(i + 1) + "</html>");
            m.setFont(Theme.ui(Font.PLAIN, 10));
        }
    }

    private void fillRecent() {
        recentMenu.removeAll();
        for (String s : st.recent) { JMenuItem i = new JMenuItem(new File(s).getName()); i.setToolTipText(s); i.addActionListener(e -> openFile(new File(s))); recentMenu.add(i); }
        recentMenu.setEnabled(!st.recent.isEmpty());
    }

    // ───────────────────────── toolbars ─────────────────────────
    private W.Btn tb(String text, String icon, Runnable r) { W.Btn b = new W.Btn(text, icon); b.addActionListener(e -> r.run()); return b; }

    private void buildToolbar() {
        JPanel l = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 6)); l.setOpaque(false);
        l.add(tb("New", "new", this::newDoc)); l.add(tb("Open", "open", this::open)); l.add(tb("Save", "save", this::save)); l.add(tb("Print", "print", this::print)); l.add(tb("PDF", "pdf", this::exportPdf));
        l.add(W.vsep());
        W.Btn undo = tb(null, "undo", () -> view.undo()), redo = tb(null, "redo", () -> view.redo());
        undo.setToolTipText("Undo"); redo.setToolTipText("Redo"); l.add(undo); l.add(redo); l.add(W.vsep());
        l.add(tb("Cut", "cut", () -> view.cut())); l.add(tb("Copy", "copy", () -> view.copy())); l.add(tb("Paste", "paste", () -> view.paste()));
        l.add(W.vsep());
        l.add(tb("Spell", "spell", this::toggleSpell)); l.add(W.vsep());
        for (String[] s : new String[][] { { "B", "**", "Bold" }, { "I", "*", "Italic" }, { "U", "_", "Underline" } }) {
            W.Btn b = tb(s[0], null, () -> view.toggleMarkup(s[1])); b.setToolTipText(s[2]);
            int style = s[0].equals("B") ? Font.BOLD : s[0].equals("I") ? Font.ITALIC : Font.PLAIN;
            b.putClientProperty("fontStyle", style);
            b.setFont(Theme.ui(style, 10)); b.setMargin(new Insets(0, 0, 0, 0));
            b.putClientProperty("FlatLaf.style", "margin:2,2,2,2; minimumWidth:24; arc:3");
            if (s[0].equals("U")) b.setText("<html><u>U</u></html>");
            l.add(b);
        }
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 6)); r.setOpaque(false);
        panelsBtn = new W.Tgl("PANELS:ON", null); cardsBtn = new W.Tgl("CARDS", null); focusBtn = new W.Tgl("FOCUS", null);
        panelsBtn.addActionListener(e -> { boolean on = panelsBtn.isSelected(); st.showLeft = on; st.showRight = on; layoutPanels(); });
        cardsBtn.addActionListener(e -> showCards(cardsBtn.isSelected()));
        focusBtn.addActionListener(e -> setFocusMode(focusBtn.isSelected()));
        r.add(panelsBtn); r.add(cardsBtn); r.add(focusBtn);
        toolbar.add(l, BorderLayout.WEST); toolbar.add(r, BorderLayout.EAST);
        toolbar.setPreferredSize(new Dimension(100, 38));
    }

    private void buildElementBar() {
        String[] icons = { "title", "run", "person", "quote", "chat", "trn", "cam" };
        for (int i = 0; i < 7; i++) {
            ElementType t = ElementType.TOOLBAR[i];
            W.Tgl b = new W.Tgl(t.abbr, icons[i]); b.setToolTipText("Shortcut: Ctrl+" + (i + 1));
            b.addActionListener(e -> { view.setType(t); view.requestFocusInWindow(); });
            pills.add(b); elementBar.add(b);
        }
        elementBar.setPreferredSize(new Dimension(100, 36)); elementBar.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
    }

    // ───────────────────────── theme / settings ─────────────────────────
    private void themeChanged() {
        Theme.Palette p = Theme.P;
        styleChrome();
        W.refresh(getContentPane());
        styleMenus();
        modeLabel.setText("<html><b style='color:" + hex(p.accent) + "'>CUA-91</b> MODE</html>");
        SwingUtilities.updateComponentTreeUI(menuBar);
        repaint();
    }
    private void styleChrome() {
        Theme.Palette p = Theme.P;
        wrapper.setBackground(p.win); wrapper.repaint();
        center.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, p.border));
        menuRow.setBackground(p.menubar); menuRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, p.border));
        menuBar.setBackground(p.menubar); menuBar.setOpaque(true);
        modeLabel.setFont(Theme.ui(Font.PLAIN, 10)); modeLabel.setForeground(p.muted);
        toolbar.setBackground(p.toolbar); toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, p.border));
        elementBar.setBackground(p.toolbar); elementBar.setBorder(BorderFactory.createMatteBorder(0, 3, 1, 0, p.border));
        for (Component c : ((Container) ((JPanel) elementBar.getParent()).getParent()).getComponents()) c.repaint();
        editorColumn.setBackground(p.viewport);
        scroll.getViewport().setBackground(p.viewport); scroll.setBackground(p.viewport);
        Component corner = scroll.getCorner(JScrollPane.LOWER_RIGHT_CORNER); if (corner != null) corner.setBackground(p.track);
        status.applyTheme();
        titleBar.setState(st.autosave, !doc.dirty);
        for (Component c : menuBar.getComponents()) if (c instanceof JComponent jc) { jc.setForeground(p.text); }
    }

    public void setDark(boolean dark) { st.dark = dark; st.save(); Theme.install(dark); syncMenuChecks(); }

    /** Applies persisted settings to the live window. */
    public void applySettings(boolean fromDialog) {
        if (fromDialog && st.dark != Theme.dark) Theme.install(st.dark);
        else if (!fromDialog) { styleChrome(); }
        UIManager.put("defaultFont", Theme.ui());
        if (fromDialog) { Theme.install(st.dark); }
        view.setSpell(st.spell);
        if (fromDialog) { view.setZoom(st.zoom); view.relayoutNow(); }
        layoutPanels();
        titleBar.setState(st.autosave, !doc.dirty);
        syncMenuChecks(); updateStatus();
        W.refresh(getContentPane());
        revalidate(); repaint();
    }

    private void syncMenuChecks() {
        updatingUi = true;
        miDark.setSelected(Theme.dark); miPanels.setSelected(st.showLeft || st.showRight); miLeft.setSelected(st.showLeft); miRight.setSelected(st.showRight);
        miRuler.setSelected(st.showRuler); miSceneNum.setSelected(st.sceneNumbers); miCards.setSelected(cardsOn); miFocus.setSelected(focusOn);
        miSpell.setSelected(st.spell); miTitlePrint.setSelected(st.printTitlePage);
        miA4.setSelected(doc.paper == Paper.A4); miLetter.setSelected(doc.paper == Paper.LETTER);
        updatingUi = false;
    }

    private void layoutPanels() {
        boolean l = st.showLeft && !focusOn, r = st.showRight && !focusOn;
        scenes.setVisible(l); chars.setVisible(r); ruler.setVisible(st.showRuler && !focusOn && !cardsOn);
        elementBar.setVisible(!focusOn); toolbar.setVisible(!focusOn); menuRow.setVisible(!focusOn);
        panelsBtn.setSelected(st.showLeft || st.showRight);
        panelsBtn.setText(st.showLeft || st.showRight ? "PANELS:ON" : "PANELS:OFF");
        syncMenuChecks(); revalidate(); repaint();
    }

    // ───────────────────────── modes ─────────────────────────
    private void showCards(boolean on) {
        cardsOn = on; cardsBtn.setSelected(on);
        if (on) cards.show(doc, view.pages());
        centerCards.show(center, on ? "cards" : "page");
        layoutPanels(); if (!on) view.requestFocusInWindow();
    }
    private void setFocusMode(boolean on) {
        focusOn = on; focusBtn.setSelected(on); view.setFocusMode(on); layoutPanels(); view.requestFocusInWindow();
    }
    private void toggleSpell() {
        st.spell = !st.spell; view.setSpell(st.spell); syncMenuChecks();
        if (st.spell && !view.dictionaryLoaded()) JOptionPane.showMessageDialog(this, "No word list found. Set one in Settings \u25B8 Editor \u25B8 Word list (.txt, one word per line).", "Spell Check", JOptionPane.INFORMATION_MESSAGE);
    }

    private void toggleMax() {
        if ((getExtendedState() & MAXIMIZED_BOTH) != 0) setExtendedState(NORMAL);
        else { setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()); setExtendedState(MAXIMIZED_BOTH); }
    }

    // ───────────────────────── zoom / paper ─────────────────────────
    /** Zoom so the page fills the centre pane. */
    public void fitWidth() {
        int avail = scroll.getViewport().getWidth(); if (avail < 100) avail = center.getWidth();
        if (avail > 100) setZoom(view.fitWidthZoom(avail));
    }
    /** Called once after the window is shown: shrink the page if it would need a horizontal scrollbar. */
    public void fitWidthIfNeeded() {
        SwingUtilities.invokeLater(() -> { int avail = scroll.getViewport().getWidth(); if (avail > 100 && view.fitWidthZoom(avail) < view.zoom()) setZoom(view.fitWidthZoom(avail)); });
    }
    private void zoomBy(int d) { setZoom(view.zoom() + d); }
    private void setZoom(int z) { view.setZoom(z); st.zoom = view.zoom(); updateStatus(); ruler.repaint(); }
    private void setPaper(Paper p) {
        if (doc.paper == p) return;
        doc.beginEdit(view.caretBlock(), view.caretOffset(), null); doc.paper = p; doc.edited(); syncMenuChecks(); revalidate();
    }
    private void pagePopup() {
        JPopupMenu m = new JPopupMenu();
        for (Paper p : Paper.values()) { JMenuItem i = new JMenuItem(p.label + (p == doc.paper ? "  \u2713" : "")); i.addActionListener(e -> setPaper(p)); m.add(i); }
        Component c = status; m.show(c, 380, -m.getPreferredSize().height);
    }

    // ───────────────────────── document events ─────────────────────────
    private void docChanged() {
        updateTitle(); updateSidebars(); updateStatus(); if (cardsOn) cards.show(doc, view.pages());
        titleBar.setState(st.autosave, !doc.dirty);
    }
    private void caretChanged() { updateStatus(); updateElementBar(); updateActiveItems(); ruler.repaint(); }

    private void updateTitle() { String t = "JScreenScriber \u2014 [" + doc.displayName() + "]" + (doc.dirty ? " *" : ""); titleBar.setTitleText(t); }

    private void updateElementBar() {
        ElementType cur = view.currentType();
        for (int i = 0; i < pills.size(); i++) {
            W.Tgl b = pills.get(i); boolean on = ElementType.TOOLBAR[i] == cur;
            b.setText(on ? "*" + ElementType.TOOLBAR[i].abbr + "*" : ElementType.TOOLBAR[i].abbr); b.setSelected(on);
        }
    }

    private void updateStatus() {
        Layout L = view.pages(); if (L == null) return;
        int sc = L.sceneIndexOfBlock(view.caretBlock()) + 1;
        status.set("Curr. scene:", sc == 0 ? "\u2014" : String.valueOf(sc), false);
        status.set("Line:", String.valueOf(view.caretLineNumber()), false);
        status.set("Col:", String.valueOf(view.caretColumn()), false);
        status.set("Page:", view.caretPage() + " of " + L.pageCount(), false);
        status.set("Page Format :", doc.paper.label, false);
        ElementType t = view.currentType();
        status.set("CURR:", t.label, true); status.set("NEXT:", t.printed ? t.next().label : "Action", false);
        status.set("Words:", String.format("%,d", doc.wordCount()), false);
        status.set("Zoom:", view.zoom() + "%", true);
        status.set("ins", view.overwrite() ? "INS:ON" : "INS:OFF", false);
    }

    private static final Pattern EXT = Pattern.compile("\\s*\\((?:V\\.O\\.|O\\.S\\.|O\\.C\\.|CONT'D|CONT\\u2019D|CONT)\\)\\s*$");
    private static String charKey(String cue) {
        String s = cue.trim(); String prev;
        do { prev = s; s = EXT.matcher(s).replaceAll("").trim(); } while (!s.equals(prev));
        return s.replaceAll("\\s*\\^$", "").replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
    }

    private void updateSidebars() {
        Layout L = view.pages(); if (L == null) return;
        List<SidePanel.Item> si = new ArrayList<>();
        for (int i = 0; i < L.scenes.size(); i++) {
            var s = L.scenes.get(i); Block b = doc.blocks.get(s.block());
            SidePanel.Item it = new SidePanel.Item(); it.key = s.block();
            it.left = (i + 1) + ". " + (b.text.isEmpty() ? "(untitled)" : b.text); it.right = "[" + s.lengthLabel() + "]"; si.add(it);
        }
        scenes.setItems(si);
        scenes.setFooter("TOTAL: " + L.scenes.size() + " SCENES (" + L.pageCount() + " PGS)");

        LinkedHashMap<String, int[]> dl = new LinkedHashMap<>(); Map<String, Set<Integer>> sc = new HashMap<>(); Map<String, Integer> firstBlock = new HashMap<>();
        String cur = null; int total = 0, scene = -1;
        for (int i = 0; i < doc.blocks.size(); i++) {
            Block b = doc.blocks.get(i);
            if (b.type == ElementType.SCENE) scene++;
            if (b.type == ElementType.CHARACTER && !b.text.isBlank()) { cur = charKey(b.text); firstBlock.putIfAbsent(cur, i); dl.computeIfAbsent(cur, k -> new int[1]); sc.computeIfAbsent(cur, k -> new HashSet<>()).add(scene); }
            else if (b.type == ElementType.DIALOGUE && cur != null && !b.isEmpty()) { dl.get(cur)[0]++; total++; }
            else if (b.type != ElementType.PAREN && b.type != ElementType.DIALOGUE) cur = null;
        }
        List<Map.Entry<String, int[]>> es = new ArrayList<>(dl.entrySet()); es.sort((a, b) -> b.getValue()[0] - a.getValue()[0]);
        List<SidePanel.Item> ci = new ArrayList<>(); charNames.clear();
        for (var e : es) {
            int pct = total == 0 ? 0 : (int) Math.round(e.getValue()[0] * 100.0 / total);
            SidePanel.Item it = new SidePanel.Item(); it.key = charNames.size(); charNames.add(e.getKey());
            it.left = e.getKey(); it.tag = e.getValue()[0] + " DLG";
            it.sub = (pct >= 40 ? "Lead" : pct >= 25 ? "Major" : pct >= 10 ? "Supporting" : "Minor") + " (" + pct + "%)";
            it.subRight = sc.get(e.getKey()).size() + " scenes"; ci.add(it);
        }
        chars.setItems(ci); chars.setFooter("CHARACTERS: " + ci.size() + " ACTIVE");
        updateActiveItems();
    }
    private final List<String> charNames = new ArrayList<>();
    private final List<SidePanel.Item> lastScene = new ArrayList<>();

    private void updateActiveItems() {
        // active scene / character highlighting is index based; rebuild flags cheaply
        Layout L = view.pages(); if (L == null) return;
        int sc = L.sceneIndexOfBlock(view.caretBlock());
        scenes.markActive(i -> i == sc, (i, it) -> { if (i == sc && !it.left.startsWith("\u25BA")) it.left = "\u25BA " + it.left; else if (i != sc && it.left.startsWith("\u25BA ")) it.left = it.left.substring(2); });
        String name = null;
        for (int i = view.caretBlock(); i >= 0; i--) {
            ElementType t = doc.blocks.get(i).type;
            if (t == ElementType.CHARACTER) { name = charKey(doc.blocks.get(i).text); break; }
            if (t != ElementType.DIALOGUE && t != ElementType.PAREN) break;
        }
        final String nm = name;
        chars.markActive(i -> nm != null && i < charNames.size() && charNames.get(i).equals(nm), (i, it) -> {});
    }

    private void pickCharacter(int idx) {
        if (idx < 0 || idx >= charNames.size()) return;
        String name = charNames.get(idx); int n = doc.blocks.size();
        for (int s = 1; s <= n; s++) {
            int i = (view.caretBlock() + s) % n; Block b = doc.blocks.get(i);
            if (b.type == ElementType.CHARACTER && charKey(b.text).equals(name)) { view.jumpToBlock(i); return; }
        }
    }

    private void stepScene(int d) {
        Layout L = view.pages(); int cur = L.sceneIndexOfBlock(view.caretBlock()), n = cur + d;
        if (n >= 0 && n < L.scenes.size()) view.jumpToBlock(L.scenes.get(n).block());
    }

    /** Move scene i to position j (adjacent), carrying all its blocks. */
    private void moveScene(int i, int j) {
        Layout L = view.pages(); if (j < 0 || j >= L.scenes.size()) return;
        int lo = Math.min(i, j), hi = Math.max(i, j);
        int a = L.scenes.get(lo).block(), b = L.scenes.get(hi).block(), end = hi + 1 < L.scenes.size() ? L.scenes.get(hi + 1).block() : doc.blocks.size();
        doc.beginEdit(view.caretBlock(), view.caretOffset(), null);
        List<Block> first = new ArrayList<>(doc.blocks.subList(a, b)), second = new ArrayList<>(doc.blocks.subList(b, end));
        for (int k = end - 1; k >= a; k--) doc.blocks.remove(k);
        doc.blocks.addAll(a, second); doc.blocks.addAll(a + second.size(), first);
        doc.edited();
    }

    // ───────────────────────── file operations ─────────────────────────
    private boolean confirmDiscard() {
        if (!doc.dirty) return true;
        int r = JOptionPane.showConfirmDialog(this, "Save changes to " + doc.displayName() + "?", "JScreenScriber", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.CANCEL_OPTION || r == JOptionPane.CLOSED_OPTION) return false;
        if (r == JOptionPane.YES_OPTION) return save();
        return true;
    }

    private void setDocument(ScriptDocument d) {
        doc = d; view.setDocument(d); d.addListener(this::docChanged); d.dirty = false; d.resetHistory();
        view.setZoom(st.zoom); docChanged(); caretChanged(); syncMenuChecks(); view.requestFocusInWindow();
    }

    private void newDoc() {
        if (!confirmDiscard()) return;
        ScriptDocument d = new ScriptDocument(); d.paper = st.paper;
        if (!st.author.isBlank()) d.title.set("Author", st.author);
        setDocument(d);
    }
    private void open() {
        if (!confirmDiscard()) return;
        JFileChooser fc = chooser(); if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) openFile(fc.getSelectedFile());
    }
    public void openFile(File f) {
        if (doc.dirty && !confirmDiscard()) return;
        try {
            ScriptDocument d = FountainIO.read(f.toPath());
            st.addRecent(f); st.lastDir = f.getParent(); st.save();
            setDocument(d);
        } catch (IOException ex) { error("Could not open file:\n" + ex.getMessage()); }
    }
    private JFileChooser chooser() {
        JFileChooser fc = new JFileChooser(st.lastDir);
        fc.setFileFilter(new FileNameExtensionFilter("Fountain screenplay (*.fountain, *.spmd, *.txt)", "fountain", "spmd", "txt"));
        return fc;
    }
    private boolean save() { return doc.file == null ? saveAs() : write(doc.file); }
    private boolean saveAs() {
        JFileChooser fc = chooser(); fc.setSelectedFile(new File(st.lastDir, doc.displayName()));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
        File f = fc.getSelectedFile();
        if (!f.getName().contains(".")) f = new File(f.getParentFile(), f.getName() + ".fountain");
        if (f.exists() && !f.equals(doc.file) && JOptionPane.showConfirmDialog(this, f.getName() + " exists. Overwrite?", "Save As", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return false;
        return write(f);
    }
    private boolean write(File f) {
        try {
            Path p = f.toPath();
            if (st.backup && Files.exists(p)) Files.copy(p, p.resolveSibling(f.getName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            FountainIO.write(doc, p);
            doc.file = f; doc.dirty = false; st.addRecent(f); st.lastDir = f.getParent(); st.save(); sinceSave = 0;
            docChanged(); return true;
        } catch (IOException ex) { error("Could not save file:\n" + ex.getMessage()); return false; }
    }
    private void autosaveTick() {
        if (!st.autosave || !doc.dirty) { sinceSave = 0; return; }
        if (++sinceSave < st.autosaveSecs) return;
        sinceSave = 0;
        try {
            if (doc.file != null) { FountainIO.write(doc, doc.file.toPath()); doc.dirty = false; docChanged(); }
            else {
                Path dir = Paths.get(System.getProperty("user.home"), ".jscreenscriber", "autosave"); Files.createDirectories(dir);
                FountainIO.write(doc, dir.resolve("untitled.fountain"));
            }
        } catch (IOException ignored) {}
    }

    private void exportPdf() {
        JFileChooser fc = new JFileChooser(st.lastDir);
        fc.setFileFilter(new FileNameExtensionFilter("PDF document (*.pdf)", "pdf"));
        String base = doc.displayName(); int dot = base.lastIndexOf('.'); if (dot > 0) base = base.substring(0, dot);
        fc.setSelectedFile(new File(st.lastDir, base + ".pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".pdf")) f = new File(f.getParentFile(), f.getName() + ".pdf");
        if (f.exists() && JOptionPane.showConfirmDialog(this, f.getName() + " exists. Overwrite?", "Export PDF", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try { PdfExporter.export(doc, f.toPath()); st.lastDir = f.getParent(); JOptionPane.showMessageDialog(this, "Exported " + f.getName(), "Export PDF", JOptionPane.INFORMATION_MESSAGE); }
        catch (Exception ex) { error("PDF export failed:\n" + ex.getMessage()); }
    }

    private void print() {
        try { Printing.print(doc); } catch (Exception ex) { error("Printing failed:\n" + ex.getMessage()); }
    }
    private void error(String m) { JOptionPane.showMessageDialog(this, m, "JScreenScriber", JOptionPane.ERROR_MESSAGE); }

    private void exit() {
        if (!confirmDiscard()) return;
        st.zoom = view.zoom(); st.save(); dispose(); System.exit(0);
    }

    // ───────────────────────── dialogs ─────────────────────────
    private FindDialog findDlg;
    private void find() { if (findDlg == null || !findDlg.isDisplayable()) findDlg = new FindDialog(this, view); else findDlg.toFront(); }
    private void settings() { new SettingsDialog(this, () -> applySettings(true)); }
    private void titlePage() {
        new TitlePageDesigner(this, doc, tp -> { doc.beginEdit(view.caretBlock(), view.caretOffset(), null); doc.title = tp; doc.edited(); });
    }
    private void statistics() {
        Layout L = view.pages(); int dlg = 0, act = 0;
        for (Block b : doc.blocks) { if (b.type == ElementType.DIALOGUE) dlg++; if (b.type == ElementType.ACTION) act++; }
        JOptionPane.showMessageDialog(this, "Pages: " + L.pageCount() + " (" + doc.paper.label + ")\nScenes: " + L.scenes.size() + "\nWords: " + String.format("%,d", doc.wordCount())
                + "\nDialogue blocks: " + dlg + "\nAction blocks: " + act + "\nCharacters: " + charNames.size() + "\nEstimated running time: ~" + L.pageCount() + " min",
                "Script Statistics", JOptionPane.INFORMATION_MESSAGE);
    }
    private void shortcuts() {
        JOptionPane.showMessageDialog(this, """
                Ctrl+1 \u2026 7   Heading, Action, Character, Parenthetical, Dialogue, Transition, Shot
                Enter            New element (smart next type)     Shift+Enter  Line break
                Tab              Cycle element type on an empty line
                Ctrl+B / I / U   Bold / Italic / Underline (Fountain markup)
                Ctrl+F           Find & replace      Ctrl+Z / Y   Undo / Redo
                Ctrl+P           Print               Ctrl+S       Save
                Ctrl+= / \u2212 / 0  Zoom             Alt+\u2191 / \u2193  Previous / next scene
                F11              Focus mode          Ctrl+K       Cards view
                Insert           Toggle overwrite    Ctrl+Shift+D Dark mode""", "Keyboard Shortcuts", JOptionPane.PLAIN_MESSAGE);
    }
    private void about() {
        JOptionPane.showMessageDialog(this, "JScreenScriber 1.0\nScreenplay writing in Fountain format.\nJava Swing + FlatLaf.", "About", JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon(new ImageIcon(MainFrame.class.getResource("/icons/app-logo.png")).getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH)));
    }
}
