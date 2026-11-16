package com.jscreenscriber.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fountain 1.1 reader / writer. */
public final class FountainIO {
    private FountainIO() {}

    static final Pattern SCENE = Pattern.compile("^(?:INT|EXT|EST|INT\\.?/EXT\\.?|EXT\\.?/INT\\.?|I/E)[. ].*", Pattern.CASE_INSENSITIVE);
    static final Pattern SCENE_NUM = Pattern.compile("^(.*?)\\s*#([\\w.\\-]+)#\\s*$");
    static final Pattern KEY = Pattern.compile("^([A-Za-z][A-Za-z0-9 \\-]{0,28}):\\s*(.*)$");
    static final Pattern SHOT = Pattern.compile("^(?:CLOSE ON|CLOSE UP|CLOSEUP|ANGLE ON|WIDE ON|WIDE SHOT|TWO SHOT|INSERT|POV|OTS|OVER THE SHOULDER|EXTREME CLOSE|MEDIUM ON|TRACKING|AERIAL)\\b.*");

    public static ScriptDocument read(Path p) throws IOException {
        ScriptDocument d = parse(Files.readString(p, StandardCharsets.UTF_8));
        d.file = p.toFile();
        return d;
    }

    public static ScriptDocument parse(String src) {
        if (src.startsWith("\uFEFF")) src = src.substring(1);
        src = src.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>(Arrays.asList(src.split("\n", -1)));
        ScriptDocument d = new ScriptDocument();
        d.blocks.clear();
        int i = 0;
        if (!lines.isEmpty() && looksLikeTitleKey(lines.get(0))) {
            String key = null; StringBuilder val = new StringBuilder();
            while (i < lines.size() && !lines.get(i).isBlank()) {
                String l = lines.get(i);
                Matcher m = KEY.matcher(l);
                boolean indented = l.startsWith("   ") || l.startsWith("\t");
                if (!indented && m.matches()) {
                    if (key != null) d.title.fields.put(key, val.toString().trim());
                    key = m.group(1).trim(); val.setLength(0); val.append(m.group(2));
                } else if (key != null) val.append('\n').append(l.trim());
                i++;
            }
            if (key != null) d.title.fields.put(key, val.toString().trim());
            String paper = d.title.get("X-Paper");
            if (!paper.isEmpty()) { d.paper = Paper.parse(paper); d.title.fields.keySet().removeIf(k -> k.equalsIgnoreCase("X-Paper")); }
            d.title.readDesigner();
        }
        d.blocks.addAll(parseBody(lines.subList(i, lines.size())));
        if (d.blocks.isEmpty()) d.blocks.add(new Block(ElementType.SCENE, ""));
        return d;
    }

    private static boolean looksLikeTitleKey(String l) {
        Matcher m = KEY.matcher(l);
        if (!m.matches() || l.startsWith(" ")) return false;
        return !SCENE.matcher(l).matches();
    }

    public static List<Block> parseBody(List<String> lines) {
        List<Block> out = new ArrayList<>();
        int n = lines.size(), i = 0;
        while (i < n) {
            String line = lines.get(i);
            if (line.isBlank()) { i++; continue; }
            if (line.trim().startsWith("/*")) {
                StringBuilder sb = new StringBuilder();
                while (i < n) { sb.append(lines.get(i)); if (lines.get(i).contains("*/")) { i++; break; } sb.append('\n'); i++; }
                String t = sb.toString().trim();
                if (t.startsWith("/*")) t = t.substring(2);
                if (t.endsWith("*/")) t = t.substring(0, t.length() - 2);
                out.add(new Block(ElementType.BONEYARD, t.trim()));
                continue;
            }
            int j = i;
            while (j < n && !lines.get(j).isEmpty()) j++;
            classify(new ArrayList<>(lines.subList(i, j)), out);
            i = j;
        }
        return out;
    }

    private static void classify(List<String> para, List<Block> out) {
        String first = para.get(0), t = first.trim();
        String all = String.join("\n", para);
        if (t.matches("^={3,}$")) { out.add(new Block(ElementType.PAGE_BREAK, "")); return; }
        if (t.startsWith("#")) {
            Block b = new Block(ElementType.SECTION, t);
            int lv = 0; while (lv < t.length() && t.charAt(lv) == '#') lv++;
            b.level = lv; out.add(b); return;
        }
        if (t.startsWith("=")) { out.add(new Block(ElementType.SYNOPSIS, t.substring(1).trim())); return; }
        if (t.startsWith("[[") && all.trim().endsWith("]]")) {
            String in = all.trim(); out.add(new Block(ElementType.NOTE, in.substring(2, in.length() - 2).trim())); return;
        }
        boolean forcedScene = t.startsWith(".") && !t.startsWith("..");
        if (forcedScene || SCENE.matcher(t).matches()) {
            String h = forcedScene ? t.substring(1).trim() : t;
            Block b = new Block(ElementType.SCENE, h);
            Matcher m = SCENE_NUM.matcher(h);
            if (m.matches()) { b.text = m.group(1); b.sceneNum = m.group(2); }
            out.add(b);
            if (para.size() > 1) out.add(new Block(ElementType.ACTION, String.join("\n", para.subList(1, para.size()))));
            return;
        }
        if (t.startsWith("!")) {
            String txt = all.replaceFirst("^\\s*!", "");
            out.add(new Block(!txt.contains("\n") && SHOT.matcher(txt.trim()).matches() ? ElementType.SHOT : ElementType.ACTION, txt));
            return;
        }
        if (t.startsWith(">")) {
            if (t.endsWith("<") && para.size() == 1) { out.add(new Block(ElementType.CENTERED, t.substring(1, t.length() - 1).trim())); return; }
            if (para.size() == 1) { out.add(new Block(ElementType.TRANSITION, t.substring(1).trim())); return; }
        }
        if (para.size() == 1 && isUpper(t) && t.endsWith("TO:")) { out.add(new Block(ElementType.TRANSITION, t)); return; }
        boolean forcedChar = t.startsWith("@");
        if (forcedChar || (para.size() >= 2 && isUpper(t))) {
            String name = forcedChar ? t.substring(1).trim() : t;
            boolean dual = false;
            if (name.endsWith("^")) { dual = true; name = name.substring(0, name.length() - 1).trim(); }
            Block c = new Block(ElementType.CHARACTER, name); c.dual = dual; out.add(c);
            StringBuilder dlg = new StringBuilder(); boolean has = false;
            for (int k = 1; k < para.size(); k++) {
                String l = para.get(k), lt = l.trim();
                if (lt.startsWith("(") && lt.endsWith(")") && !has) {
                    out.add(new Block(ElementType.PAREN, lt));
                } else if (lt.startsWith("(") && lt.endsWith(")") && has) {
                    out.add(new Block(ElementType.DIALOGUE, dlg.toString())); dlg.setLength(0); has = false;
                    out.add(new Block(ElementType.PAREN, lt));
                } else {
                    if (has) dlg.append('\n');
                    dlg.append(l.equals("  ") ? "" : l.stripTrailing()); has = true;
                }
            }
            if (has) out.add(new Block(ElementType.DIALOGUE, dlg.toString()));
            return;
        }
        String txt = all.stripTrailing();
        out.add(new Block(!txt.contains("\n") && isUpper(txt.trim()) && SHOT.matcher(txt.trim()).matches() ? ElementType.SHOT : ElementType.ACTION, txt));
    }

    static boolean isUpper(String s) {
        String core = s.replaceAll("\\(.*?\\)", "").replace("^", "").trim();
        if (core.startsWith("@")) return false;
        boolean letter = false;
        for (char c : core.toCharArray()) {
            if (Character.isLetter(c)) { letter = true; if (!Character.isUpperCase(c)) return false; }
        }
        return letter;
    }

    // ───────────────────────────── writer ─────────────────────────────

    public static void write(ScriptDocument d, Path p) throws IOException {
        Files.writeString(p, toFountain(d), StandardCharsets.UTF_8);
    }

    public static String toFountain(ScriptDocument d) {
        StringBuilder sb = new StringBuilder();
        var tp = d.title.forWrite();
        if (d.paper != Paper.A4 || !tp.isEmpty()) {
            if (d.paper != Paper.A4) tp.put("X-Paper", d.paper.label);
        }
        if (!tp.isEmpty()) {
            for (var e : tp.entrySet()) {
                String[] vl = e.getValue().split("\n");
                sb.append(e.getKey()).append(':');
                if (vl.length == 1) sb.append(' ').append(vl[0]).append('\n');
                else { sb.append('\n'); for (String v : vl) sb.append("   ").append(v).append('\n'); }
            }
            sb.append('\n');
        }
        sb.append(bodyToFountain(d.blocks));
        return sb.toString();
    }

    public static String bodyToFountain(List<Block> blocks) {
        StringBuilder sb = new StringBuilder();
        ElementType prev = null;
        for (Block b : blocks) {
            if (b.isEmpty() && b.type != ElementType.PAGE_BREAK) continue;
            boolean chain = prev != null && (b.type == ElementType.PAREN || b.type == ElementType.DIALOGUE)
                    && (prev == ElementType.CHARACTER || prev == ElementType.PAREN || prev == ElementType.DIALOGUE);
            if (prev != null && !chain) sb.append('\n');
            sb.append(blockText(b)).append('\n');
            prev = b.type;
        }
        return sb.toString();
    }

    static String blockText(Block b) {
        String t = b.text;
        switch (b.type) {
            case SCENE: {
                String s = SCENE.matcher(t).matches() ? t : "." + t;
                return b.sceneNum != null && !b.sceneNum.isEmpty() ? s + " #" + b.sceneNum + "#" : s;
            }
            case CHARACTER: {
                String s = isUpper(t) ? t : "@" + t;
                return b.dual ? s + " ^" : s;
            }
            case PAREN: return t.startsWith("(") ? t : "(" + t + ")";
            case DIALOGUE: return t.replace("\n\n", "\n  \n");
            case TRANSITION: return isUpper(t) && t.endsWith("TO:") ? t : ">" + t;
            case SHOT: return "!" + t;
            case CENTERED: return "> " + t + " <";
            case PAGE_BREAK: return "===";
            case SECTION: return t.startsWith("#") ? t : "# " + t;
            case SYNOPSIS: return "= " + t;
            case NOTE: return "[[" + t + "]]";
            case BONEYARD: return "/*\n" + t + "\n*/";
            default: {
                String first = t.split("\n", 2)[0].trim();
                boolean force = SCENE.matcher(first).matches() || first.matches("^[.@!>#=\\[~].*") && !first.startsWith("~")
                        || (t.contains("\n") && isUpper(first)) || (isUpper(first) && first.endsWith("TO:"))
                        || (isUpper(first) && SHOT.matcher(first).matches()) || first.startsWith("/*");
                return force ? "!" + t : t;
            }
        }
    }
}
