package com.jscreenscriber.model;

import java.util.*;

/** Title page key/values plus designer options (persisted as X-* keys so other Fountain apps ignore them). */
public final class TitlePage {
    public static final String[] STD = { "Title", "Credit", "Author", "Source", "Draft date", "Contact", "Copyright", "Notes" };

    public final LinkedHashMap<String, String> fields = new LinkedHashMap<>();
    public boolean enabled = true;
    public String preset = "Classic";         // Classic | Centered | Minimal
    public int titleSize = 2;                 // 1 = small, 2 = medium, 3 = large (x scale)
    public boolean titleBold = true, titleUnderline = false, titleCaps = true;
    public String contactSide = "left";       // left | right
    public String dateSide = "right";         // right | left | none

    public String get(String k) {
        for (var e : fields.entrySet()) if (e.getKey().equalsIgnoreCase(k)) return e.getValue();
        return "";
    }
    public void set(String k, String v) {
        for (var e : fields.entrySet()) if (e.getKey().equalsIgnoreCase(k)) { if (v == null || v.isBlank()) fields.remove(e.getKey()); else e.setValue(v); return; }
        if (v != null && !v.isBlank()) fields.put(k, v);
    }
    public boolean isEmpty() { return fields.isEmpty(); }

    public TitlePage copy() {
        TitlePage t = new TitlePage();
        t.fields.putAll(fields);
        t.enabled = enabled; t.preset = preset; t.titleSize = titleSize; t.titleBold = titleBold;
        t.titleUnderline = titleUnderline; t.titleCaps = titleCaps; t.contactSide = contactSide; t.dateSide = dateSide;
        return t;
    }

    /** Extra designer keys. */
    void readDesigner() {
        String v;
        if (!(v = get("X-Title-Page")).isEmpty()) enabled = !v.equalsIgnoreCase("off");
        if (!(v = get("X-Layout")).isEmpty()) preset = v;
        if (!(v = get("X-Title-Size")).isEmpty()) try { titleSize = Math.max(1, Math.min(3, Integer.parseInt(v.trim()))); } catch (Exception ignore) {}
        if (!(v = get("X-Title-Style")).isEmpty()) { String s = v.toLowerCase(); titleBold = s.contains("bold"); titleUnderline = s.contains("underline"); titleCaps = s.contains("caps"); }
        if (!(v = get("X-Contact-Side")).isEmpty()) contactSide = v.trim();
        if (!(v = get("X-Date-Side")).isEmpty()) dateSide = v.trim();
        for (String k : new String[] { "X-Title-Page", "X-Layout", "X-Title-Size", "X-Title-Style", "X-Contact-Side", "X-Date-Side" }) remove(k);
    }
    private void remove(String k) { fields.keySet().removeIf(x -> x.equalsIgnoreCase(k)); }

    /** Key/value pairs for writing, designer keys last. */
    LinkedHashMap<String, String> forWrite() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        for (String s : STD) { String v = get(s); if (!v.isEmpty()) m.put(s, v); }
        for (var e : fields.entrySet()) if (!m.containsKey(e.getKey()) && Arrays.stream(STD).noneMatch(s -> s.equalsIgnoreCase(e.getKey()))) m.put(e.getKey(), e.getValue());
        boolean any = !m.isEmpty();
        if (any || !enabled) {
            if (!enabled) m.put("X-Title-Page", "off");
            if (!preset.equals("Classic")) m.put("X-Layout", preset);
            if (titleSize != 2) m.put("X-Title-Size", String.valueOf(titleSize));
            if (!(titleBold && !titleUnderline && titleCaps)) m.put("X-Title-Style", (titleBold ? "bold " : "") + (titleUnderline ? "underline " : "") + (titleCaps ? "caps" : "")); 
            if (!contactSide.equals("left")) m.put("X-Contact-Side", contactSide);
            if (!dateSide.equals("right")) m.put("X-Date-Side", dateSide);
        }
        return m;
    }
}
