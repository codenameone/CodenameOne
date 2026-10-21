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
package com.codenameone.examples.skirmish.battle;

import com.codename1.gaming.AnimatedSprite;
import com.codename1.gaming.GameInput;
import com.codename1.gaming.GameView;
import com.codename1.gaming.Scene;
import com.codename1.gaming.Sprite;
import com.codename1.gaming.SpriteSheet;
import com.codename1.ui.CN;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Event;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Unit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// The arena, drawn with `com.codename1.gaming`.
///
/// The simulation advances in fixed 50 ms ticks -- the rules' clock -- while
/// the screen redraws as fast as the device allows. Between two ticks every
/// unit is drawn part of the way from where it was to where it is
/// ([#alpha]), so a 20 Hz simulation still moves smoothly at 60 or 120 Hz.
///
/// Nothing here decides anything. Units, hits and deaths all come from the
/// [Match] the [Driver] steps; this class only turns them into sprites,
/// effects and sound. Placing a card is the one input: a card chosen in the
/// hand (a styled `Button` under the arena) and a press, drag and release on
/// the field. A drag that starts on a card and ends on the field works too,
/// because [GameView] reads the pointer at the form level.
// tag::skirmish-battle-view[]
public class BattleView extends GameView {
    /// The widgets around the arena: told about ticks and about the end.
    public interface Hud {
        void ticked(Match match);

        void placed(String card);

        void ended(Match match);
    }

    public static final double TICK = 1.0 / Match.TICKS_PER_SECOND;
    private static final int MAX_STEPS_PER_FRAME = 12;
    private static final int Z_GROUND = 100;
    private static final int Z_AIR = 6000;
    private static final int Z_EFFECT = 9000;

    private final Driver driver;
    private Hud hud;
    private Viewport viewport;
    private Sprite background;
    private Sprite zone;
    private Sprite ghost;
    private final Map<Integer, UnitSprites> units = new HashMap<Integer, UnitSprites>();
    private final List<AnimatedSprite> effects = new ArrayList<AnimatedSprite>();
    private double accumulator;
    private double alpha;
    private String selected;
    private int hudTick = -1;
    private boolean ended;

    public BattleView(Driver driver) {
        this.driver = driver;
        setClearColor(0xff1d3322);
        setFocusable(true);
    }

    public void setHud(Hud hud) {
        this.hud = hud;
    }

    public Driver driver() {
        return driver;
    }

    /// The card the next press on the field places, or null.
    public void select(String card) {
        selected = card;
    }

    public String selected() {
        return selected;
    }

    @Override
    protected void update(double dt) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (viewport == null || viewport.width() != w || viewport.height() != h
                || viewport.me() != driver.me()) {
            layoutArena(w, h);
        }
        // tag::skirmish-fixed-tick[]
        accumulator += dt * driver.speedPercent() / 100.0;
        int steps = 0;
        while (accumulator >= TICK && steps < MAX_STEPS_PER_FRAME) {
            if (!driver.canStep()) {
                // Online and waiting for the server: hold at the last tick.
                accumulator = Math.min(accumulator, TICK);
                break;
            }
            remember();
            driver.step();
            effects(driver.match().events());
            accumulator -= TICK;
            steps++;
        }
        alpha = Math.min(1, accumulator / TICK);
        // end::skirmish-fixed-tick[]
        draw();
        input();
        notifyHud();
    }

    /// Where every unit stood before this tick, to draw it between ticks.
    private void remember() {
        for (UnitSprites s : units.values()) {
            s.fromX = s.toX;
            s.fromY = s.toY;
        }
    }

    private void layoutArena(int w, int h) {
        Scene scene = getScene();
        scene.clear();
        units.clear();
        effects.clear();
        viewport = new Viewport(driver.me(), w, h);
        background = new Sprite(Art.arena(w, h, viewport));
        background.setPosition(w / 2.0, h / 2.0);
        background.setZOrder(0);
        scene.add(background);

        // The part of the field the player may not drop troops on, shaded
        // while a card is held.
        zone = new Sprite(Art.pixel());
        int riverTop = Math.min(viewport.sy(0, Arena.RIVER_TOP), viewport.sy(0, Arena.RIVER_BOTTOM));
        int fieldTop = Math.min(viewport.sy(0, 0), viewport.sy(0, Arena.HEIGHT));
        zone.setAnchor(0, 0);
        zone.setPosition(Math.min(viewport.sx(0, 0), viewport.sx(Arena.WIDTH, 0)), fieldTop);
        zone.setSize(viewport.px(Arena.WIDTH), riverTop - fieldTop + viewport.px(2 * Arena.TILE));
        zone.setColor(0xffd0302a);
        zone.setAlpha(70);
        zone.setVisible(false);
        zone.setZOrder(1);
        scene.add(zone);

        ghost = new Sprite(Art.pixel());
        ghost.setVisible(false);
        ghost.setAlpha(150);
        ghost.setZOrder(Z_EFFECT + 500);
        scene.add(ghost);
        accumulator = 0;
        hudTick = -1;
    }

    /// Brings the sprites in line with the match's units and places them.
    private void draw() {
        Match m = driver.match();
        Scene scene = getScene();
        List<Unit> live = m.units();
        for (int i = 0; i < live.size(); i++) {
            Unit u = live.get(i);
            UnitSprites s = units.get(Integer.valueOf(u.id()));
            if (s == null) {
                s = new UnitSprites(u, viewport, scene);
                units.put(Integer.valueOf(u.id()), s);
            }
            s.seen = m.tick();
            s.toX = viewport.sx(u.x(), u.y());
            s.toY = viewport.sy(u.x(), u.y());
            s.place(u, alpha);
        }
        for (Iterator<UnitSprites> it = units.values().iterator(); it.hasNext();) {
            UnitSprites s = it.next();
            if (s.seen != m.tick()) {
                s.remove(scene);
                it.remove();
            }
        }
        for (Iterator<AnimatedSprite> it = effects.iterator(); it.hasNext();) {
            AnimatedSprite e = it.next();
            if (!e.isPlaying()) {
                scene.remove(e);
                it.remove();
            }
        }
        scene.markSortDirty();
    }

    // tag::skirmish-touch[]
    private void input() {
        GameInput in = getInput();
        String card = selected;
        boolean holding = card != null && driver.interactive() && !driver.match().isOver();
        int px = in.getPointerX();
        int py = in.getPointerY();
        boolean inside = px >= 0 && py >= 0 && px < getWidth() && py < getHeight();
        if (holding && in.isPointerDown() && inside) {
            CardDef def = Cards.get(card);
            int size = Math.max(16, viewport.px(Math.max(def.radius() * 2, def.splash() * 2)));
            ghost.setImage(Art.cardIcon(card, size));
            ghost.setPosition(px, py);
            ghost.setVisible(true);
            zone.setVisible(def.kind != CardDef.SPELL);
        } else {
            ghost.setVisible(false);
            zone.setVisible(false);
        }
        if (holding && in.wasPointerReleased() && inside) {
            int x = viewport.ax(px, py);
            int y = viewport.ay(px, py);
            if (driver.match().canPlay(driver.me(), card, x, y)) {
                driver.play(card, x, y);
                Sfx.deploy();
                selected = null;
                CN.callSerially(() -> {
                    if (hud != null) {
                        hud.placed(card);
                    }
                });
            }
        }
    }
    // end::skirmish-touch[]

    private void effects(List<Event> events) {
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            switch (e.kind) {
                case Event.ATTACK:
                    burst(e.x, e.y, Math.max(10, viewport.px(600)), 0xfff2e6c0, 0.04);
                    Sfx.hit();
                    break;
                case Event.SPELL:
                    // A spell lands where it was cast, splash wide.
                    Unit spell = driver.match().unit(e.source);
                    CardDef sd = spell == null ? null : spell.card();
                    burst(e.x, e.y, Math.max(24, viewport.px(sd == null ? 2500 : sd.splash() * 2)),
                            0xff000000 | (sd == null ? 0xffaa33 : Art.colorOf(sd.id)), 0.06);
                    Sfx.boom();
                    break;
                case Event.DEATH:
                    burst(e.x, e.y, Math.max(14, viewport.px(1200)), 0xff9aa0a6, 0.06);
                    break;
                case Event.TOWER_DOWN:
                    burst(e.x, e.y, Math.max(40, viewport.px(4000)), 0xffff8c2a, 0.09);
                    Sfx.boom();
                    break;
                case Event.SPAWN:
                    Unit u = driver.match().unit(e.source);
                    burst(e.x, e.y, Math.max(16, viewport.px(1400)),
                            u != null && u.owner() == driver.me() ? 0xff8fb8ff : 0xffff9a8f, 0.05);
                    break;
                default:
                    break;
            }
        }
    }

    private void burst(int x, int y, int size, int argb, double frameSeconds) {
        int sheetSize = size <= 24 ? 24 : size <= 64 ? 64 : 128;
        SpriteSheet sheet = Art.burst(sheetSize);
        AnimatedSprite s = new AnimatedSprite(sheet, new int[] {0, 1, 2, 3, 4, 5}, frameSeconds);
        s.setLooping(false);
        s.setScale((float) size / sheetSize);
        s.setColor(argb);
        s.setPosition(viewport.sx(x, y), viewport.sy(x, y));
        s.setZOrder(Z_EFFECT);
        s.play();
        getScene().add(s);
        effects.add(s);
    }

    private void notifyHud() {
        final Match m = driver.match();
        if (hud == null) {
            return;
        }
        if (m.isOver() && !ended) {
            ended = true;
            CN.callSerially(() -> {
                driver.finished();
                hud.ended(m);
            });
            return;
        }
        // The widgets are refreshed four times a second, which is all an
        // elixir bar and a clock need.
        if (m.tick() / 5 != hudTick) {
            hudTick = m.tick() / 5;
            CN.callSerially(() -> hud.ticked(m));
        }
    }

    /// The sprites of one unit: its body and, once hurt, a health bar.
    private static final class UnitSprites {
        final Sprite body;
        final Sprite barBack;
        final Sprite bar;
        final int size;
        final boolean structure;
        int fromX = Integer.MIN_VALUE;
        int fromY;
        int toX;
        int toY;
        int seen;

        UnitSprites(Unit u, Viewport v, Scene scene) {
            CardDef card = u.card();
            structure = card.kind == CardDef.TOWER || card.kind == CardDef.BUILDING;
            int team = u.owner() == v.me() ? 0 : 1;
            size = Math.max(14, v.px(card.radius() * 2 + (structure ? 400 : 300)));
            if (card.kind == CardDef.SPELL) {
                body = new Sprite(Art.cardIcon(card.id, size));
            } else {
                SpriteSheet sheet = Art.unitSheet(card.id, team, size);
                AnimatedSprite a = new AnimatedSprite(sheet, new int[] {0, 1, 2, 3},
                        structure ? 0.4 : 0.14);
                a.setLooping(true);
                a.play();
                body = a;
            }
            barBack = new Sprite(Art.pixel());
            barBack.setColor(0xff1b1b1b);
            bar = new Sprite(Art.pixel());
            bar.setColor(team == 0 ? 0xff4fa3ff : 0xffff5a4f);
            barBack.setAnchor(0, 0.5);
            bar.setAnchor(0, 0.5);
            scene.add(body);
            scene.add(barBack);
            scene.add(bar);
        }

        void place(Unit u, double alpha) {
            if (fromX == Integer.MIN_VALUE) {
                fromX = toX;
                fromY = toY;
            }
            double x = fromX + (toX - fromX) * alpha;
            double y = fromY + (toY - fromY) * alpha;
            body.setPosition(x, y);
            boolean air = u.card().isFlying();
            int z = (air ? Z_AIR : Z_GROUND) + (int) y;
            body.setZOrder(z);
            body.setAlpha(u.isDeploying() ? 140 : 255);
            boolean hurt = u.hp() < u.maxHp() || structure;
            barBack.setVisible(hurt && u.card().kind != CardDef.SPELL);
            bar.setVisible(barBack.isVisible());
            if (hurt) {
                int bw = Math.max(10, size * 4 / 5);
                int bh = Math.max(3, size / 10);
                double top = y - size / 2.0 - bh;
                barBack.setSize(bw, bh);
                barBack.setPosition(x - bw / 2.0, top);
                bar.setSize(Math.max(1, bw * u.hp() / Math.max(1, u.maxHp())), bh);
                bar.setPosition(x - bw / 2.0, top);
                barBack.setZOrder(z + 1);
                bar.setZOrder(z + 2);
            }
        }

        void remove(Scene scene) {
            scene.remove(body);
            scene.remove(barBack);
            scene.remove(bar);
        }
    }
}
// end::skirmish-battle-view[]
