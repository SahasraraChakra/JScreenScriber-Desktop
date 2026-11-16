package com.jscreenscriber;

import com.jscreenscriber.model.FountainIO;
import com.jscreenscriber.model.ScriptDocument;
import com.jscreenscriber.ui.MainFrame;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> report(e));
        try {
            SwingUtilities.invokeLater(() -> {
                try { start(args); } catch (Throwable e) { report(e); }
            });
        } catch (Throwable e) { report(e); }
    }

    private static void start(String[] args) throws Exception {
        Settings st = Settings.get();
        Theme.loadBundledFonts();
        Theme.install(st.dark);
        ScriptDocument doc = null;
        try {
            if (args.length > 0 && new File(args[0]).isFile()) { doc = FountainIO.read(new File(args[0]).toPath()); st.addRecent(new File(args[0])); }
            else try (InputStream in = Main.class.getResourceAsStream("/sample.fountain")) {
                if (in != null) doc = FountainIO.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
        if (doc == null) doc = new ScriptDocument();
        doc.dirty = false;
        MainFrame f = new MainFrame(doc);
        f.setVisible(true);
        f.fitWidthIfNeeded();
    }

    /** Writes ~/.jscreenscriber/error.log and shows the error (a double-clicked jar has no console). */
    static void report(Throwable e) {
        StringWriter sw = new StringWriter(); e.printStackTrace(new PrintWriter(sw));
        String text = "JScreenScriber " + System.getProperty("java.version") + " / " + System.getProperty("os.name") + "\n\n" + sw;
        System.err.println(text);
        try {
            Path p = Paths.get(System.getProperty("user.home"), ".jscreenscriber", "error.log");
            Files.createDirectories(p.getParent()); Files.writeString(p, text);
        } catch (IOException ignored) {}
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JTextArea a = new JTextArea(text.length() > 3000 ? text.substring(0, 3000) : text, 16, 70); a.setEditable(false);
            JOptionPane.showMessageDialog(null, new JScrollPane(a), "JScreenScriber failed to start (saved to ~/.jscreenscriber/error.log)", JOptionPane.ERROR_MESSAGE);
        }
    }
}
