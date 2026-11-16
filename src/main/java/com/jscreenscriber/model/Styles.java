package com.jscreenscriber.model;

import java.util.EnumMap;
import java.util.Properties;

/** User-editable per-element formatting (indent, width, spacing, alignment, emphasis, caps). */
public final class Styles {
    private Styles() {}

    public static final class S {
        public int indent, width, before, align;       // width 0 = full text width; align 0 left, 1 centre, 2 right
        public boolean bold, italic, underline, caps;
        public S copy() { S s = new S(); s.indent = indent; s.width = width; s.before = before; s.align = align; s.bold = bold; s.italic = italic; s.underline = underline; s.caps = caps; return s; }
    }

    private static final EnumMap<ElementType, S> MAP = new EnumMap<>(ElementType.class);
    static { resetAll(); }

    public static S of(ElementType t) { return MAP.get(t); }

    public static S defaults(ElementType t) {
        S s = new S(); s.before = 1;
        switch (t) {
            case SCENE -> { s.bold = true; s.caps = true; }
            case CHARACTER -> { s.indent = 22; s.width = 30; s.bold = true; s.caps = true; }
            case PAREN -> { s.indent = 16; s.width = 26; s.italic = true; }
            case DIALOGUE -> { s.indent = 10; s.width = 35; }
            case TRANSITION -> { s.align = 2; s.bold = true; s.caps = true; }
            case SHOT -> { s.bold = true; s.caps = true; }
            case CENTERED, PAGE_BREAK -> s.align = 1;
            case SECTION, SYNOPSIS, NOTE, BONEYARD -> s.italic = true;
            default -> {}
        }
        return s;
    }

    public static void reset(ElementType t) { MAP.put(t, defaults(t)); }
    public static void resetAll() { for (ElementType t : ElementType.values()) reset(t); }
    public static void set(ElementType t, S s) { MAP.put(t, s.copy()); }

    public static void load(Properties p) {
        for (ElementType t : ElementType.values()) {
            S s = defaults(t); String k = "style." + t.name() + ".";
            s.indent = i(p, k + "indent", s.indent); s.width = i(p, k + "width", s.width); s.before = i(p, k + "before", s.before);
            s.align = i(p, k + "align", s.align);
            s.bold = b(p, k + "bold", s.bold); s.italic = b(p, k + "italic", s.italic); s.underline = b(p, k + "underline", s.underline); s.caps = b(p, k + "caps", s.caps);
            s.indent = Math.max(0, Math.min(50, s.indent)); s.width = Math.max(0, Math.min(80, s.width)); s.before = Math.max(0, Math.min(4, s.before)); s.align = Math.max(0, Math.min(2, s.align));
            MAP.put(t, s);
        }
    }
    public static void save(Properties p) {
        for (ElementType t : ElementType.values()) {
            S s = MAP.get(t); String k = "style." + t.name() + ".";
            p.setProperty(k + "indent", "" + s.indent); p.setProperty(k + "width", "" + s.width); p.setProperty(k + "before", "" + s.before); p.setProperty(k + "align", "" + s.align);
            p.setProperty(k + "bold", "" + s.bold); p.setProperty(k + "italic", "" + s.italic); p.setProperty(k + "underline", "" + s.underline); p.setProperty(k + "caps", "" + s.caps);
        }
    }
    private static int i(Properties p, String k, int d) { try { return Integer.parseInt(p.getProperty(k, "" + d)); } catch (Exception e) { return d; } }
    private static boolean b(Properties p, String k, boolean d) { return Boolean.parseBoolean(p.getProperty(k, "" + d)); }
}
