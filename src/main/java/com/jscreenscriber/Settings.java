package com.jscreenscriber;

import com.jscreenscriber.model.Paper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Persistent user settings (~/.jscreenscriber/settings.properties). */
public final class Settings {
    private static Settings inst;
    public static Settings get() { if (inst == null) { inst = new Settings(); inst.load(); } return inst; }

    // appearance
    public boolean dark = false;
    public int uiFontPx = 12;
    public String scriptFont = "Cascadia Mono";
    public int scriptFontPx = 14;
    public double marginLeft = 1.5, marginRight = 1.0, marginTop = 1.0, marginBottom = 1.0;
    public boolean showLeft = true, showRight = true, showRuler = true;
    public int zoom = 100;
    // editor
    public boolean autoCaps = true, smartEnter = true, autoDetectScene = true, spell = false, typewriterFocus = false;
    public boolean sceneNumbers = false;
    public int sceneSpacing = 1;
    public String dictionary = "";
    // page & print
    public Paper paper = Paper.A4;
    public boolean printTitlePage = true, printPageNumbers = true, numberFirstPage = false, printSceneNumbers = false;
    public String headerText = "";
    // file
    public boolean autosave = true;
    public int autosaveSecs = 60;
    public boolean backup = true;
    public String author = "";
    public final LinkedList<String> recent = new LinkedList<>();
    public String lastDir = System.getProperty("user.home");

    private static Path path() { return Paths.get(System.getProperty("user.home"), ".jscreenscriber", "settings.properties"); }

    private void load() {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path())) { p.load(in); } catch (IOException e) { return; }
        dark = b(p, "dark", dark); uiFontPx = i(p, "uiFontPx", uiFontPx, 10, 20);
        scriptFont = p.getProperty("scriptFont", scriptFont); scriptFontPx = i(p, "scriptFontPx", scriptFontPx, 8, 28);
        marginLeft = d(p, "marginLeft", marginLeft); marginRight = d(p, "marginRight", marginRight); marginTop = d(p, "marginTop", marginTop); marginBottom = d(p, "marginBottom", marginBottom);
        com.jscreenscriber.model.Styles.load(p); applyMargins();
        showLeft = b(p, "showLeft", showLeft); showRight = b(p, "showRight", showRight); showRuler = b(p, "showRuler", showRuler);
        zoom = i(p, "zoom", zoom, 50, 250);
        autoCaps = b(p, "autoCaps", autoCaps); smartEnter = b(p, "smartEnter", smartEnter); autoDetectScene = b(p, "autoDetectScene", autoDetectScene);
        spell = b(p, "spell", spell); sceneNumbers = b(p, "sceneNumbers", sceneNumbers); sceneSpacing = i(p, "sceneSpacing", sceneSpacing, 1, 2);
        dictionary = p.getProperty("dictionary", dictionary);
        paper = Paper.parse(p.getProperty("paper", paper.label));
        printTitlePage = b(p, "printTitlePage", printTitlePage); printPageNumbers = b(p, "printPageNumbers", printPageNumbers);
        numberFirstPage = b(p, "numberFirstPage", numberFirstPage); printSceneNumbers = b(p, "printSceneNumbers", printSceneNumbers);
        headerText = p.getProperty("headerText", headerText);
        autosave = b(p, "autosave", autosave); autosaveSecs = i(p, "autosaveSecs", autosaveSecs, 10, 3600); backup = b(p, "backup", backup);
        author = p.getProperty("author", author); lastDir = p.getProperty("lastDir", lastDir);
        String r = p.getProperty("recent", "");
        for (String s : r.split("\u0001")) if (!s.isBlank()) recent.add(s);
    }
    private static double d(Properties p, String k, double def) { try { return Double.parseDouble(p.getProperty(k, String.valueOf(def))); } catch (Exception e) { return def; } }
    public void applyMargins() { Paper.setMargins(marginLeft, marginRight, marginTop, marginBottom); marginLeft = Paper.LEFT; marginRight = Paper.RIGHT; marginTop = Paper.TOP; marginBottom = Paper.BOTTOM; }
    private static boolean b(Properties p, String k, boolean d) { return Boolean.parseBoolean(p.getProperty(k, String.valueOf(d))); }
    private static int i(Properties p, String k, int d, int lo, int hi) {
        try { return Math.max(lo, Math.min(hi, Integer.parseInt(p.getProperty(k, String.valueOf(d))))); } catch (Exception e) { return d; }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("dark", s(dark)); p.setProperty("uiFontPx", s(uiFontPx)); p.setProperty("scriptFont", scriptFont); p.setProperty("scriptFontPx", s(scriptFontPx));
        p.setProperty("marginLeft", s(marginLeft)); p.setProperty("marginRight", s(marginRight)); p.setProperty("marginTop", s(marginTop)); p.setProperty("marginBottom", s(marginBottom));
        com.jscreenscriber.model.Styles.save(p); p.setProperty("showLeft", s(showLeft));
        p.setProperty("showRight", s(showRight)); p.setProperty("showRuler", s(showRuler)); p.setProperty("zoom", s(zoom));
        p.setProperty("autoCaps", s(autoCaps)); p.setProperty("smartEnter", s(smartEnter)); p.setProperty("autoDetectScene", s(autoDetectScene));
        p.setProperty("spell", s(spell)); p.setProperty("sceneNumbers", s(sceneNumbers)); p.setProperty("sceneSpacing", s(sceneSpacing));
        p.setProperty("dictionary", dictionary); p.setProperty("paper", paper.label);
        p.setProperty("printTitlePage", s(printTitlePage)); p.setProperty("printPageNumbers", s(printPageNumbers));
        p.setProperty("numberFirstPage", s(numberFirstPage)); p.setProperty("printSceneNumbers", s(printSceneNumbers));
        p.setProperty("headerText", headerText); p.setProperty("autosave", s(autosave)); p.setProperty("autosaveSecs", s(autosaveSecs));
        p.setProperty("backup", s(backup)); p.setProperty("author", author); p.setProperty("lastDir", lastDir);
        p.setProperty("recent", String.join("\u0001", recent));
        try {
            Files.createDirectories(path().getParent());
            try (OutputStream out = Files.newOutputStream(path())) { p.store(out, "JScreenScriber"); }
        } catch (IOException ignored) {}
    }
    private static String s(Object o) { return String.valueOf(o); }

    public void addRecent(File f) {
        String a = f.getAbsolutePath(); recent.remove(a); recent.addFirst(a);
        while (recent.size() > 8) recent.removeLast();
    }
}
