package com.jscreenscriber.model;

public enum ElementType {
    SCENE("HDG", "Scene Heading", 0, -1, true),
    ACTION("ACT", "Action", 0, -1, true),
    CHARACTER("CHR", "Character", 22, 30, true),
    PAREN("PAR", "Parenthetical", 16, 26, true),
    DIALOGUE("DLG", "Dialogue", 10, 35, true),
    TRANSITION("TRN", "Transition", 0, -1, true),
    SHOT("SHT", "Shot", 0, -1, true),
    CENTERED("CTR", "Centered", 0, -1, true),
    PAGE_BREAK("BRK", "Page Break", 0, -1, true),
    SECTION("SEC", "Section", 0, -1, false),
    SYNOPSIS("SYN", "Synopsis", 0, -1, false),
    NOTE("NTE", "Note", 0, -1, false),
    BONEYARD("BON", "Boneyard", 0, -1, false);

    public final String abbr, label;
    public final int indent, width;      // width -1 = full text width
    public final boolean printed;

    ElementType(String abbr, String label, int indent, int width, boolean printed) {
        this.abbr = abbr; this.label = label; this.indent = indent; this.width = width; this.printed = printed;
    }

    public int indent() { return Math.min(Styles.of(this).indent, 40); }
    public int widthFor(Paper p) { int w = Styles.of(this).width, avail = Math.max(10, p.cols() - indent()); return w <= 0 ? avail : Math.min(w, avail); }
    public boolean upper() { return Styles.of(this).caps; }

    /** Type produced by pressing Enter at the end of a block of this type. */
    public ElementType next() {
        return switch (this) {
            case CHARACTER, PAREN -> DIALOGUE;
            case TRANSITION -> SCENE;
            default -> ACTION;
        };
    }
    /** Type produced by Tab on an empty block. */
    public ElementType tabNext() {
        return switch (this) {
            case ACTION -> CHARACTER;
            case CHARACTER -> TRANSITION;
            case TRANSITION -> SCENE;
            case SCENE -> ACTION;
            case DIALOGUE -> PAREN;
            case PAREN -> DIALOGUE;
            default -> ACTION;
        };
    }
    /** The element buttons in toolbar order (Ctrl+1 .. Ctrl+7). */
    public static final ElementType[] TOOLBAR = { SCENE, ACTION, CHARACTER, PAREN, DIALOGUE, TRANSITION, SHOT };
}
