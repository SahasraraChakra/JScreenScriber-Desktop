package com.jscreenscriber;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/** Colour palettes (light = "white grid" mock, dark = Atom One Dark mock) and FlatLaf installation. */
public final class Theme {
    private Theme() {}

    public static final class Palette {
        public Color win, titlebar, menubar, toolbar, sidebar, sideHeader, viewport, page, pageBorder, text, muted,
                accent, accentDark, accentBg, accentBorder, border, btnBorder, btnBg, btnHover, input, inputBorder,
                track, thumb, thumbBorder, cursor, tag, tagBg, listActive, listHover, listItem, closeBg, closeBorder, closeFg,
                status, statusSep, ok, sceneC, actionC, charC, parenC, dialogC, transC, pageNum, noteC, selection, dim;
    }

    public static final Palette LIGHT = new Palette(), DARK = new Palette();

    public static Palette P = LIGHT;
    public static boolean dark;
    private static final List<Runnable> listeners = new ArrayList<>();
    public static void onChange(Runnable r) { listeners.add(r); }

    // ───────── fonts ─────────
    private static final Set<String> FAMILIES = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    private static String uiFamily, monoFamily;

    /** Loads any TTF/OTF in /fonts on the classpath (drop Inter / Courier Prime there for pixel-exact output). */
    public static void loadBundledFonts() {
        for (String f : new String[] { "Inter-Regular.otf", "Inter-Italic.otf", "Inter-Bold.otf", "Inter-BoldItalic.otf", "Inter-Regular.ttf", "Inter-Bold.ttf",
                "CascadiaMono.ttf", "CascadiaMono-Regular.ttf", "CascadiaMono-Bold.ttf", "CascadiaMono-Italic.ttf", "CascadiaMono-BoldItalic.ttf" }) {
            try (InputStream in = Theme.class.getResourceAsStream("/fonts/" + f)) {
                if (in == null) continue;
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                FAMILIES.add(font.getFamily());
            } catch (Exception ignored) {}
        }
        uiFamily = pick("Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Dialog");
    }
    private static String pick(String... names) { for (String n : names) if (FAMILIES.contains(n)) return n; return names[names.length - 1]; }

    /** UI font: size is given on the design's 10px scale and multiplied up to the user's base size (default 12px). */
    public static Font ui(int style, float size) { if (uiFamily == null) loadBundledFonts(); return new Font(uiFamily, style, 1).deriveFont(size * Settings.get().uiFontPx / 10f); }
    /** Script font (default Cascadia Mono); size is in real pixels. */
    public static Font mono(int style, float size) {
        if (uiFamily == null) loadBundledFonts();
        monoFamily = pick(Settings.get().scriptFont, "Cascadia Mono", "Cascadia Code", "Consolas", "Courier New", "Liberation Mono", "DejaVu Sans Mono", "Monospaced");
        return new Font(monoFamily, style, 1).deriveFont(size);
    }
    public static Font ui() { return ui(Font.PLAIN, 10); }

    // ───────── install ─────────
    private static boolean registered;

    /** Installs FlatLaf with the theme .properties files in com/jscreenscriber/themes, then reads the JSS.* colours into the palette. */
    public static void install(boolean useDark) {
        dark = useDark; P = useDark ? DARK : LIGHT;
        if (!registered) { FlatLaf.registerCustomDefaultsSource("com.jscreenscriber.themes"); registered = true; }
        if (useDark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        UIManager.put("defaultFont", ui());
        loadPalette(P);
        FlatLaf.updateUI();
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }

    private static void loadPalette(Palette p) {
        for (java.lang.reflect.Field f : Palette.class.getFields()) {
            Color c = UIManager.getColor("JSS." + f.getName());
            if (c == null && f.getType() == Color.class) {
                try { if (f.get(p) == null) { System.err.println("Theme key JSS." + f.getName() + " missing - is com/jscreenscriber/themes/*.properties on the classpath?"); c = Color.GRAY; } } catch (IllegalAccessException ignored) {}
            }
            if (c != null) try { f.set(p, c); } catch (IllegalAccessException ignored) {}
        }
    }
}
