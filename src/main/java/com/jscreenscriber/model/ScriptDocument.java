package com.jscreenscriber.model;

import java.io.File;
import java.util.*;

/** The screenplay: title page + blocks, with snapshot undo/redo. */
public final class ScriptDocument {
    public final List<Block> blocks = new ArrayList<>();
    public TitlePage title = new TitlePage();
    public Paper paper = Paper.A4;
    public File file;
    public boolean dirty;
    public int sceneSpacing = 1;

    public record Snap(List<Block> blocks, TitlePage title, int caretBlock, int caretOffset) {}

    private final Deque<Snap> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private long lastEditNanos; private Object lastKey;

    public ScriptDocument() { blocks.add(new Block(ElementType.SCENE, "")); }

    public void addListener(Runnable r) { listeners.add(r); }
    public void fire() { for (Runnable r : new ArrayList<>(listeners)) r.run(); }

    public String displayName() { return file == null ? "Untitled.fountain" : file.getName(); }

    private List<Block> copyBlocks() { List<Block> c = new ArrayList<>(blocks.size()); for (Block b : blocks) c.add(b.copy()); return c; }

    /** Call before every mutation; consecutive edits with the same key within 1s share one undo step. */
    public void beginEdit(int caretBlock, int caretOffset, Object coalesceKey) {
        long now = System.nanoTime();
        boolean merge = coalesceKey != null && coalesceKey.equals(lastKey) && now - lastEditNanos < 1_000_000_000L && !undo.isEmpty();
        lastEditNanos = now; lastKey = coalesceKey;
        if (merge) return;
        undo.push(new Snap(copyBlocks(), title.copy(), caretBlock, caretOffset));
        if (undo.size() > 300) undo.removeLast();
        redo.clear();
    }
    public void edited() { dirty = true; fire(); }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    public Snap undo(int cb, int co) {
        if (undo.isEmpty()) return null;
        redo.push(new Snap(copyBlocks(), title.copy(), cb, co));
        return restore(undo.pop());
    }
    public Snap redo(int cb, int co) {
        if (redo.isEmpty()) return null;
        undo.push(new Snap(copyBlocks(), title.copy(), cb, co));
        return restore(redo.pop());
    }
    private Snap restore(Snap s) {
        blocks.clear(); blocks.addAll(s.blocks()); title = s.title(); lastKey = null; dirty = true; fire(); return s;
    }
    public void resetHistory() { undo.clear(); redo.clear(); lastKey = null; }

    public int wordCount() {
        int n = 0;
        for (Block b : blocks) {
            if (!b.type.printed) continue;
            String t = b.text.trim();
            if (!t.isEmpty()) n += t.split("\\s+").length;
        }
        return n;
    }
}
