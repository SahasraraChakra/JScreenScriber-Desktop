package com.jscreenscriber.model;

import java.util.List;

/** One screenplay paragraph. Text is raw Fountain-inline markup; '\n' is a hard line break. */
public final class Block {
    public ElementType type;
    public String text;
    public boolean dual;          // dual dialogue marker (^) on a CHARACTER
    public String sceneNum;       // explicit scene number (#12#) on a SCENE
    public int level = 1;         // SECTION depth

    // wrap cache (managed by Layout)
    transient List<Object> cache; public transient int cacheW = -1; transient String cacheText; transient ElementType cacheType;

    public Block(ElementType t, String text) { this.type = t; this.text = text; }

    public Block copy() {
        Block b = new Block(type, text);
        b.dual = dual; b.sceneNum = sceneNum; b.level = level;
        return b;
    }
    public boolean isEmpty() { return text.isEmpty(); }
}
