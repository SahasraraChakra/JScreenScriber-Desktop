package com.jscreenscriber.model;

/** Page sizes with screenplay geometry (1.5" left, 1" right/top/bottom, 10 cpi, 6 lpi). */
public enum Paper {
    A4("A4", 8.27, 11.69), LETTER("Letter", 8.5, 11.0);

    public final String label;
    public final double wIn, hIn;
    public static double LEFT = 1.5, RIGHT = 1.0, TOP = 1.0, BOTTOM = 1.0;

    /** Sets page margins in inches (clamped so a page always keeps a usable text area). */
    public static void setMargins(double l, double r, double t, double b) {
        LEFT = clamp(l, 0.25, 3); RIGHT = clamp(r, 0.25, 3); TOP = clamp(t, 0.25, 3); BOTTOM = clamp(b, 0.25, 3);
    }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    Paper(String label, double w, double h) { this.label = label; this.wIn = w; this.hIn = h; }

    /** Character cells across the text area (A4: 57, Letter: 60). */
    public int cols() { return Math.max(20, (int) Math.floor((wIn - LEFT - RIGHT) * 10 + 1e-6)); }
    /** Text lines per page (A4: 58, Letter: 54). */
    public int lines() { return Math.max(12, (int) Math.floor((hIn - TOP - BOTTOM) * 6 + 1e-6)); }

    public static Paper parse(String s) { return "letter".equalsIgnoreCase(s) ? LETTER : A4; }
    @Override public String toString() { return label; }
}
