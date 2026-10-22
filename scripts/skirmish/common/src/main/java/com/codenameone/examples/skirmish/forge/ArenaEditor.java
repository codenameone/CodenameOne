/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.skirmish.forge;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.skirmish.battle.Art;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Draws an arena from above and changes it with a tap: towers, rocks and
/// trees, the size of the field, its lanes and its look.
///
/// The field is always fair. A tower is placed on the player's half and its
/// mirror image appears on the other; so does an obstacle, unless mirroring
/// is switched off. The river always crosses the middle.
public final class ArenaEditor {
    private static final String[] TOOLS = {"King", "Tower", "Rock", "Tree", "Erase"};
    /// An icon over a short word: five words side by side do not fit a phone.
    private static final char[] TOOL_ICONS = {FontImage.MATERIAL_CASTLE, FontImage.MATERIAL_FORT,
        FontImage.MATERIAL_LANDSCAPE, FontImage.MATERIAL_PARK, FontImage.MATERIAL_BACKSPACE};
    private static final String[] THEMES = {"meadow", "frost", "desert"};
    private static final int HALF = Arena.TILE / 2;

    private final int index;
    private String name;
    private String theme;
    private int cols;
    private int rows;
    private int lanes;
    private int riverTiles;
    private final List<Arena.Tower> mine = new ArrayList<Arena.Tower>();
    private final List<Arena.Obstacle> obstacles = new ArrayList<Arena.Obstacle>();
    private final List<Arena> undo = new ArrayList<Arena>();
    private int tool = 1;
    private boolean mirror = true;
    private String towerCard = Cards.PRINCESS_TOWER;
    private Canvas canvas;

    private ArenaEditor(int index, Arena a) {
        this.index = index;
        load(a);
    }

    static void create() {
        Arena c = Arena.CLASSIC;
        String id = unique("arena");
        Arena a = new Arena(id, "New arena", "meadow", c.width(), c.height(), c.riverTop(), c.riverBottom(),
                new int[] {c.laneX(0), c.laneX(1)}, c.bridgeHalfWidth(), c.towers(), c.obstacles());
        PackEditor.pack.arenas.add(a);
        new ArenaEditor(PackEditor.pack.arenas.size() - 1, a).show();
    }

    static void edit(int index) {
        new ArenaEditor(index, PackEditor.pack.arenas.get(index)).show();
    }

    private static String unique(String base) {
        String id = base;
        for (int n = 2; PackEditor.pack.arena(id) != null; n++) {
            id = base + "-" + n;
        }
        return id;
    }

    private void load(Arena a) {
        name = a.name();
        theme = a.theme();
        cols = a.width() / Arena.TILE;
        rows = a.height() / Arena.TILE;
        lanes = a.laneCount();
        riverTiles = Math.max(1, (a.riverBottom() - a.riverTop()) / Arena.TILE);
        mine.clear();
        for (Arena.Tower t : a.towers()) {
            if (t.owner == 0) {
                mine.add(t);
            }
        }
        obstacles.clear();
        obstacles.addAll(a.obstacles());
    }

    /// The arena as the editor holds it now.
    private Arena build() {
        Arena old = PackEditor.pack.arenas.get(index);
        int w = cols * Arena.TILE;
        int h = rows * Arena.TILE;
        int[] x = new int[lanes];
        for (int i = 0; i < lanes; i++) {
            // Evenly spread, on a half tile, like the classic field's lanes.
            x[i] = (w * (2 * i + 1) / (2 * lanes)) / HALF * HALF;
        }
        List<Arena.Tower> towers = new ArrayList<Arena.Tower>();
        for (Arena.Tower t : mine) {
            Arena.mirrored(towers, h, t.card, Math.min(t.x, w - HALF), Math.min(t.y, h - HALF), t.king);
        }
        List<Arena.Obstacle> obs = new ArrayList<Arena.Obstacle>();
        for (Arena.Obstacle o : obstacles) {
            if (o.x < w && o.y < h) {
                obs.add(o);
            }
        }
        int river = riverTiles * Arena.TILE;
        return new Arena(old.id(), name, theme, w, h, (h - river) / 2, (h + river) / 2, x, old.bridgeHalfWidth(),
                Arena.sortByOwner(towers), obs);
    }

    private void changed(boolean remember) {
        if (remember) {
            undo.add(PackEditor.pack.arenas.get(index));
            if (undo.size() > 40) {
                undo.remove(0);
            }
        }
        PackEditor.pack.arenas.set(index, build());
        canvas.field = null;
        canvas.setShouldCalcPreferredSize(true);
        canvas.getParent().revalidate();
    }

    private void show() {
        Form f = Ui.form("Arena", "arenaEditor");
        Ui.back(f, PackEditor::show);
        canvas = new Canvas();
        canvas.setName("arenaCanvas");
        f.add(Fields.text("Name", "arenaName", name, 40, v -> {
            name = v;
            PackEditor.pack.arenas.set(index, build());
        }));
        Container tools = new Container(new GridLayout(1, TOOLS.length));
        final com.codename1.ui.Button[] buttons = new com.codename1.ui.Button[TOOLS.length];
        for (int i = 0; i < TOOLS.length; i++) {
            final int t = i;
            buttons[i] = Ui.button(TOOLS[i], i == tool ? "ForgeToolOn" : "ForgeTool", "tool-" + TOOLS[i], e -> {
                tool = t;
                for (int j = 0; j < buttons.length; j++) {
                    buttons[j].setUIID(j == t ? "ForgeToolOn" : "ForgeTool");
                    FontImage.setMaterialIcon(buttons[j], TOOL_ICONS[j], 4);
                }
                buttons[t].getParent().revalidate();
            });
            FontImage.setMaterialIcon(buttons[i], TOOL_ICONS[i], 4);
            buttons[i].setTextPosition(com.codename1.ui.Component.BOTTOM);
            tools.add(buttons[i]);
        }
        f.add(tools);
        f.add(canvas);
        f.add(Fields.help("Tap your half of the field (the bottom) to place; the enemy's half gets the same. "
                + "Troops walk around rocks and trees."));
        f.add(Ui.row(Ui.stone("Undo", "arenaUndo", e -> {
            if (!undo.isEmpty()) {
                Arena prev = undo.remove(undo.size() - 1);
                PackEditor.pack.arenas.set(index, prev);
                load(prev);
                canvas.field = null;
                canvas.repaint();
            }
        }), Ui.stone("Done", "arenaDone", e -> PackEditor.show())));
        f.add(Fields.check("Mirror rocks and trees", "arenaMirror", mirror, v -> mirror = v));
        List<CardDef> towerCards = new ArrayList<CardDef>();
        for (CardDef c : PackEditor.pack.cardSet().all()) {
            if (c.kind == CardDef.TOWER && !Cards.KING_TOWER.equals(c.id)) {
                towerCards.add(c);
            }
        }
        String[] towerNames = new String[towerCards.size()];
        for (int i = 0; i < towerNames.length; i++) {
            towerNames[i] = towerCards.get(i).name;
        }
        final List<CardDef> tc = towerCards;
        f.add(Fields.choice("Tower card", "arenaTowerCard", towerNames, 0, v -> towerCard = tc.get(v).id));
        f.add(Fields.choice("Look", "arenaTheme", THEMES, Fields.indexOf(THEMES, theme), v -> {
            theme = THEMES[v];
            changed(true);
        }));
        f.add(Fields.stepper("Width", "arenaWidth", cols, Arena.MIN_TILES_WIDE, Arena.MAX_TILES_WIDE, 1, " tiles",
                v -> {
                    cols = v;
                    changed(true);
                }));
        f.add(Fields.stepper("Length", "arenaHeight", rows, Arena.MIN_TILES_TALL, Arena.MAX_TILES_TALL, 2,
                " tiles", v -> {
                    rows = v;
                    changed(true);
                }));
        f.add(Fields.stepper("Lanes", "arenaLanes", lanes, 1, Arena.MAX_LANES, 1, "", v -> {
            lanes = v;
            changed(true);
        }));
        f.add(Fields.stepper("River", "arenaRiver", riverTiles, 1, 6, 1, " tiles", v -> {
            riverTiles = v;
            changed(true);
        }));
        f.add(Ui.stone("Delete arena", "arenaDelete", e -> {
            PackEditor.pack.arenas.remove(index);
            PackEditor.show();
        }));
        f.show();
    }

    /// A tap at (x, y) in rules coordinates.
    private void tap(int x, int y) {
        Arena a = PackEditor.pack.arenas.get(index);
        x = Math.max(HALF, Math.min(a.width() - HALF, (x + HALF / 2) / HALF * HALF));
        y = Math.max(HALF, Math.min(a.height() - HALF, (y + HALF / 2) / HALF * HALF));
        String t = TOOLS[tool];
        if ("Erase".equals(t)) {
            erase(a, x, y);
            return;
        }
        if ("King".equals(t) || "Tower".equals(t)) {
            if (!a.ownHalf(0, y)) {
                Ui.toast("Towers go on your half; the other side mirrors them");
                return;
            }
            boolean king = "King".equals(t);
            for (int i = mine.size() - 1; king && i >= 0; i--) {
                if (mine.get(i).king) {
                    mine.remove(i);
                }
            }
            if (!king && mine.size() >= Arena.MAX_TOWERS / 2) {
                Ui.toast("That is enough towers");
                return;
            }
            mine.add(new Arena.Tower(0, king ? Cards.KING_TOWER : towerCard, x, y, king));
        } else {
            if (obstacles.size() + (mirror ? 2 : 1) > Arena.MAX_OBSTACLES) {
                Ui.toast("That is enough obstacles");
                return;
            }
            String kind = "Tree".equals(t) ? "tree" : "rock";
            int r = "Tree".equals(t) ? 500 : 700;
            obstacles.add(new Arena.Obstacle(kind, x, y, r));
            int my = a.height() - y;
            if (mirror && Math.abs(my - y) > HALF) {
                obstacles.add(new Arena.Obstacle(kind, x, my, r));
            }
        }
        changed(true);
    }

    private void erase(Arena a, int x, int y) {
        int near = Arena.TILE * Arena.TILE;
        int ty = a.ownHalf(0, y) ? y : a.height() - y;
        for (int i = mine.size() - 1; i >= 0; i--) {
            Arena.Tower t = mine.get(i);
            if (sq(t.x - x) + sq(t.y - ty) <= near * 2) {
                mine.remove(i);
                changed(true);
                return;
            }
        }
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Arena.Obstacle o = obstacles.get(i);
            if (sq(o.x - x) + sq(o.y - y) <= near * 2) {
                obstacles.remove(i);
                changed(true);
                return;
            }
        }
    }

    private static int sq(int v) {
        return v * v;
    }

    /// The field, drawn as the battle draws it, with the pieces on top.
    private final class Canvas extends Component {
        Image field;

        Canvas() {
            setUIID("Container");
            setFocusable(true);
        }

        @Override
        protected Dimension calcPreferredSize() {
            Arena a = PackEditor.pack.arenas.get(index);
            int w = Math.min(CN.getDisplayWidth() - CN.convertToPixels(6), CN.convertToPixels(90));
            return new Dimension(w, w * a.height() / a.width());
        }

        private int scale(int v) {
            return (int) ((long) v * getWidth() / PackEditor.pack.arenas.get(index).width());
        }

        @Override
        public void paint(Graphics g) {
            Arena a = PackEditor.pack.arenas.get(index);
            if (field == null) {
                field = Art.field(a);
            }
            int w = getWidth();
            int h = w * a.height() / a.width();
            g.drawImage(field, getX(), getY(), w, h);
            g.setAntiAliased(true);
            for (Arena.Obstacle o : a.obstacles()) {
                int r = Math.max(3, scale(o.radius));
                g.setColor("tree".equals(o.kind) ? 0x2f6b2a : 0x8a8a8a);
                g.fillArc(getX() + scale(o.x) - r, getY() + scale(o.y) - r, r * 2, r * 2, 0, 360);
            }
            for (Arena.Tower t : a.towers()) {
                int r = scale(t.king ? Arena.TILE * 3 / 2 : Arena.TILE);
                g.setColor(t.owner == 0 ? 0x2f6fd0 : 0xd03a2f);
                g.fillRect(getX() + scale(t.x) - r, getY() + scale(t.y) - r, r * 2, r * 2);
                g.setColor(0xffffff);
                g.drawRect(getX() + scale(t.x) - r, getY() + scale(t.y) - r, r * 2, r * 2);
                if (t.king) {
                    g.drawString("K", getX() + scale(t.x) - g.getFont().stringWidth("K") / 2,
                            getY() + scale(t.y) - g.getFont().getHeight() / 2);
                }
            }
        }

        @Override
        public void pointerReleased(int x, int y) {
            super.pointerReleased(x, y);
            Arena a = PackEditor.pack.arenas.get(index);
            if (getWidth() <= 0) {
                return;
            }
            tap((int) ((long) (x - getAbsoluteX()) * a.width() / getWidth()),
                    (int) ((long) (y - getAbsoluteY()) * a.width() / getWidth()));
        }
    }
}
