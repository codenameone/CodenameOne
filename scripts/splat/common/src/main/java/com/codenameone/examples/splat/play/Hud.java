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
package com.codenameone.examples.splat.play;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codenameone.examples.splat.rules.Arena;
import com.codenameone.examples.splat.rules.Box;
import com.codenameone.examples.splat.rules.Event;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Mission;
import com.codenameone.examples.splat.rules.Mode;
import com.codenameone.examples.splat.rules.Player;
import com.codenameone.examples.splat.rules.Trig;
import java.util.ArrayList;
import java.util.List;

/// Everything drawn over the field, painted with `Graphics` straight onto a
/// transparent component layered above the [ArenaView], laid out the way every
/// serious military shooter lays it out: the minimap top left, the score bar and
/// the clock top centre, the kill feed top right, the magazine bottom right,
/// hit markers and damage arcs around the crosshair, name tags over your squad,
/// and -- when it is you -- the big red PAINTED stamp.
///
/// It keeps to the form's safe area, so a notch never covers the score.
// tag::splat-hud[]
public final class Hud extends Component implements ArenaView.Hud {
    private static final long STAMP_MILLIS = 1800;
    private static final int FEED_LINES = 5;
    private static final long FEED_MILLIS = 6000;
    private static final long POP_MILLIS = 1400;
    private static final long ARC_MILLIS = 1600;
    /// What a painted player says. The ref has heard all of them.
    static final String[] EXCUSES = {
        "(disputes this)", "(says it was a bounce)", "(is checking for a break)",
        "(\"that was my marker\")", "(wants a replay)", "(demands to see the ref's license)",
        "(insists it was a teammate)", "(is calling their lawyer)",
    };

    private final Driver driver;
    private ArenaView view;
    private Match match;
    private int me;
    private final List<FeedLine> feed = new ArrayList<FeedLine>();
    private long stampAt = -STAMP_MILLIS;
    private String stampText = "PAINTED";
    private String stampSub = "";
    private long hitAt = -1000;
    private boolean hitKill;
    private long popAt = -POP_MILLIS;
    private String pop = "";
    private int streak;
    private long arcAt = -ARC_MILLIS;
    private int arcX;
    private int arcZ;
    private long splatAt = -10000;
    private int splatSeed;
    private String banner = "";
    private long bannerAt = -10000;
    private final float[] clip = new float[4];
    private Font huge;
    private Font big;
    private Font medium;
    private Font small;
    private Font tiny;
    private Font narrow;
    private Font stampFont;
    /// A phone: less on screen, every element smaller. A desktop or a tablet
    /// has room for the whole picture.
    private boolean compact;
    private boolean feedHidden;
    private boolean ended;
    private final List<String> chat = new ArrayList<String>();
    private final List<Long> chatAt = new ArrayList<Long>();

    /// One line of the kill feed: who, with what, whom.
    static final class FeedLine {
        final String left;
        final int leftColor;
        final String verb;
        final String right;
        final int rightColor;
        final long at;

        FeedLine(String left, int leftColor, String verb, String right, int rightColor, long at) {
            this.left = left;
            this.leftColor = leftColor;
            this.verb = verb;
            this.right = right;
            this.rightColor = rightColor;
            this.at = at;
        }
    }

    public Hud(Driver driver) {
        this.driver = driver;
        setFocusable(false);
        getAllStyles().setBgTransparency(0);
        setName("hud");
    }

    /// The view whose camera the name tags are projected through.
    public void attach(ArenaView v) {
        view = v;
    }

    @Override
    public void frame(Match m, int me, float lookYaw, float lookPitch) {
        this.match = m;
        this.me = me;
        repaint();
    }

    /// Friendly blue or hostile red, as seen by whoever this HUD belongs to.
    private int side(int player) {
        if (match == null || player < 0 || player >= match.playerCount()) {
            return Type.WHITE;
        }
        return match.player(player).team() == match.player(me).team() ? Type.FRIENDLY : Type.HOSTILE;
    }

    @Override
    public void event(Event e) {
        long now = System.currentTimeMillis();
        Match m = driver.match();
        if (e.kind == Event.PAINTED) {
            feed(driver.name(e.a), side(e.a), "PAINTED", driver.name(e.b) + " " + EXCUSES[(e.tick + e.b) % EXCUSES.length],
                    side(e.b), now);
            if (e.b == me && driver.interactive()) {
                stampAt = now;
                stampText = "PAINTED";
                stampSub = "BY " + Type.caps(driver.name(e.a)) + ". WALK IT OFF.";
                splatAt = now;
                splatSeed = e.tick;
                streak = 0;
                if (m != null && e.a >= 0 && e.a < m.playerCount()) {
                    arcAt = now;
                    arcX = m.player(e.a).x();
                    arcZ = m.player(e.a).z();
                }
            } else if (e.a == me && driver.interactive()) {
                hitAt = now;
                hitKill = true;
                streak++;
                popAt = now;
                pop = streak >= 3 ? "+150 PAINT STREAK x" + streak : "+100 PAINTED";
            }
        } else if (e.kind == Event.SPLAT && e.a == me && driver.interactive() && m != null && near(e)) {
            hitAt = now;
            hitKill = false;
        } else if (e.kind == Event.WIPE_OK) {
            feed(driver.name(e.a), side(e.a), "WIPED IT OFF.", "Nobody saw. (Everybody saw.)", Type.STEEL, now);
            if (e.a == me && driver.interactive()) {
                announce("WIPED. ACT NATURAL.", now);
                splatAt = -10000;
            }
        } else if (e.kind == Event.WIPE_CAUGHT) {
            feed("REF", Type.AMBER, "CAUGHT", driver.name(e.a) + " wiping. +3 seconds.", side(e.a), now);
            if (e.a == me && driver.interactive()) {
                stampAt = now;
                stampText = "CAUGHT";
                stampSub = "THE REF IS WRITING YOUR NAME DOWN.";
            }
        } else if (e.kind == Event.FRIENDLY) {
            feed(driver.name(e.a), side(e.a), "TEAMKILLED", "and is not over it", Type.STEEL, now);
        } else if (e.kind == Event.CAPTURE) {
            feed(driver.name(e.a), side(e.a), "CAPTURED", "the bedsheet", Type.AMBER, now);
            if (e.a == me && driver.interactive()) {
                popAt = now;
                pop = "+300 FLAG CAPTURE";
            }
        } else if (e.kind == Event.FLAG_TAKEN) {
            feed(driver.name(e.a), side(e.a), "TOOK", "the enemy flag", Type.AMBER, now);
        } else if (e.kind == Event.RELOAD && e.a == me && driver.interactive()) {
            announce("RELOADING -- SHOUT \"COVER ME!\" FOR EFFECT", now);
        } else if (e.kind == Event.END && m != null) {
            stampAt = now;
            boolean mine = driver.interactive() && e.b == m.player(me).team();
            stampText = e.b < 0 ? "DRAW" : mine ? "VICTORY" : driver.interactive() ? "DEFEAT" : "OVER";
            stampSub = e.b < 0 ? "EVERYONE CLAIMS THEY WON." : Type.caps(ArenaView.TEAM_NAMES[e.b]) + " WINS. SNACKS ON THEM.";
        }
    }

    /// A ball of yours burst within a metre of somebody on the other team.
    private boolean near(Event e) {
        int team = match == null ? -1 : match.player(me).team();
        for (int i = 0; match != null && i < match.playerCount(); i++) {
            Player p = match.player(i);
            if (p.team() == team || !p.alive()) {
                continue;
            }
            long dx = e.x - p.x();
            long dz = e.z - p.z();
            if (dx * dx + dz * dz < 1000000L && e.y < p.height() + 300) {
                return true;
            }
        }
        return false;
    }

    private void feed(String left, int lc, String verb, String right, int rc, long now) {
        // two identical lines in a row (a grenade painting two teammates)
        // read as a glitch; one is enough
        for (int i = feed.size() - 1; i >= 0 && now - feed.get(i).at < 1500; i--) {
            FeedLine f = feed.get(i);
            if (f.left.equals(left) && f.verb.equals(verb) && f.right.equals(right)) {
                return;
            }
        }
        feed.add(new FeedLine(left, lc, verb, right, rc, now));
        while (feed.size() > FEED_LINES) {
            feed.remove(0);
        }
    }

    private void announce(String text, long now) {
        banner = text;
        bannerAt = now;
    }

    private void fonts() {
        if (big != null) {
            return;
        }
        huge = Type.bold(9f);
        big = Type.bold(5.2f);
        medium = Type.bold(3.4f);
        small = Type.bold(2.6f);
        tiny = Type.medium(2.2f);
        narrow = Type.bold(2.1f);
        stampFont = Type.bold(15f);
    }

    @Override
    public void paint(Graphics g) {
        if (match == null || ended) {
            return;
        }
        fonts();
        long now = System.currentTimeMillis();
        int x0 = getX();
        int y0 = getY();
        int w = getWidth();
        int h = getHeight();
        int top = y0 + safeTop() + CN.convertToPixels(2);
        int pad = CN.convertToPixels(2.5f);
        g.setAntiAliased(true);
        g.setAntiAliasedText(true);
        Player p = match.player(me);
        compact = !CN.isDesktop() && Math.min(w, h) < CN.convertToPixels(110);
        int count = driver.countdownTicks();
        if (count > 0) {
            // the briefing is a screen of its own; the HUD comes after it
            briefing(g, x0, y0, w, h, count);
            return;
        }

        splatter(g, now, x0, y0, w, h);
        if (driver.interactive()) {
            nameTags(g, x0, y0, w, h);
        }
        int mapSize = compact ? Math.min(CN.convertToPixels(17), w / 4)
                : Math.min(CN.convertToPixels(26), Math.min(w, h) / 3);
        minimap(g, x0 + pad, top, mapSize);
        chatLines(g, now, x0 + pad, top + mapSize + CN.convertToPixels(compact ? 1.5f : 5), w / 2);
        scoreBar(g, x0 + w / 2, top);
        if (compact) {
            // Under the score bar, clear of the minimap and the pause button.
            killFeed(g, x0 + w - pad, top + medium.getHeight() + CN.convertToPixels(9));
        } else {
            // Under the pause button, and under the score bar where both
            // would not fit side by side.
            boolean roomy = w > CN.convertToPixels(150);
            killFeed(g, x0 + w - pad, top + (roomy ? CN.convertToPixels(10)
                    : big.getHeight() + tiny.getHeight() + CN.convertToPixels(3)));
        }

        if (driver.interactive()) {
            float ads = view == null ? 0f : view.aimAmount();
            crosshair(g, now, x0 + w / 2, y0 + h / 2, p.alive() && ads < 0.6f);
            hitMarker(g, now, x0 + w / 2, y0 + h / 2);
            damageArc(g, now, x0 + w / 2, y0 + h / 2, Math.min(w, h) / 4);
            ammo(g, p, x0 + w - pad, y0 + h - pad - safeBottom()
                    - (CN.isDesktop() ? 0 : CN.convertToPixels(34)));
            // the stamp has the middle of the screen to itself while it lands
            boolean stamping = now - stampAt < STAMP_MILLIS;
            if (!stamping) {
                xpPop(g, now, x0 + w / 2 + CN.convertToPixels(8), y0 + h / 2 - CN.convertToPixels(6));
            }
            if (!p.alive() && !stamping) {
                int left = Math.max(0, p.respawnAt() - match.tick()) / Match.TICK_HZ + 1;
                String wipe = compact ? "TAP WIPE -- IF THE REF ISN'T LOOKING"
                        : "HOLD [X] TO WIPE IT OFF -- IF THE REF ISN'T LOOKING";
                String back = p.canWipe(match.tick()) ? wipe : "REDEPLOYING IN " + left;
                // where the crosshair was: nothing else is drawn there while
                // you are out
                plate(g, back, x0 + w / 2, y0 + h / 2 - small.getHeight() / 2, compact ? small : medium, Type.AMBER);
            }
        } else {
            plate(g, "SPECTATING  " + Type.caps(driver.name(me)), x0 + w / 2,
                    y0 + h - CN.convertToPixels(10) - safeBottom(), medium, Type.WHITE);
        }
        if (now - bannerAt < 2200 && now - stampAt >= STAMP_MILLIS) {
            plate(g, banner, x0 + w / 2, y0 + h * 58 / 100, small, Type.WHITE);
        }
        bubbles(g, x0, y0, w, h);
        replayBars(g, x0, y0, w, h);
        stamp(g, now, x0, y0, w, h);
    }

    /// While the view plays the shot back: cinema bars, and a plate naming
    /// who did it, so a replay is never mistaken for the live match.
    private void replayBars(Graphics g, int x0, int y0, int w, int h) {
        int shooter = view == null ? -1 : view.replayingShooter();
        if (shooter < 0 || shooter >= match.playerCount()) {
            if (view != null && view.isOverhead() && driver.interactive()) {
                plate(g, "OVERHEAD", x0 + w / 2, y0 + h - safeBottom() - CN.convertToPixels(compact ? 40 : 8),
                        tiny, Type.WHITE);
            }
            return;
        }
        int bar = h / 9;
        g.setColor(0);
        g.setAlpha(230);
        g.fillRect(x0, y0, w, bar);
        g.fillRect(x0, y0 + h - bar, w, bar);
        g.setAlpha(255);
        int angle = view.replayAngle();
        String title = angle == ArenaView.ANGLE_SIDE ? "ANGLE 2/3 -- SUPER SLOW-MO"
                : angle == ArenaView.ANGLE_VICTIM ? "ANGLE 3/3 -- THE PERFORMANCE" : "INSTANT REPLAY -- ANGLE 1/3";
        plate(g, title, x0 + w / 2, y0 + bar + CN.convertToPixels(1.5f), medium, Type.AMBER);
        // a white flash on every cut, the way a broadcast marks a new angle
        long cut = view.sinceCut();
        if (cut >= 0 && cut < 220) {
            g.setColor(0xffffff);
            g.setAlpha((int) (200 * (220 - cut) / 220));
            g.fillRect(x0, y0 + bar, w, h - bar * 2);
            g.setAlpha(255);
        }
        plate(g, "PAINTED BY " + Type.caps(driver.name(shooter)), x0 + w / 2,
                y0 + h - bar - small.getHeight() - CN.convertToPixels(2), small, Type.WHITE);
    }

    /// Top left: the yard from above, north up, cover in steel, your squad in
    /// blue and anyone on the other side who just fired in red.
    private void minimap(Graphics g, int x, int y, int size) {
        Arena a = match.arena();
        g.setColor(Type.INK);
        g.setAlpha(170);
        g.fillRect(x, y, size, size);
        g.setAlpha(255);
        g.setColor(Type.STEEL);
        g.drawRect(x, y, size - 1, size - 1);
        int span = Math.max(a.width, a.depth);
        int ox = x + (size - (int) ((long) a.width * size / span)) / 2;
        int oy = y + (size - (int) ((long) a.depth * size / span)) / 2;
        List<Box> boxes = a.boxes();
        g.setColor(0x56606a);
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            // Mirrored like the field: rules +x is screen left.
            int bx0 = ox + mx(a.width - b.x1, size, span);
            int bx1 = ox + mx(a.width - b.x0, size, span);
            int bz0 = oy + mx(a.depth - b.z1, size, span);
            int bz1 = oy + mx(a.depth - b.z0, size, span);
            g.fillRect(bx0, bz0, Math.max(2, bx1 - bx0), Math.max(2, bz1 - bz0));
        }
        if (match.mode() == Mode.SNACK_BAR) {
            int r = Math.max(3, mx(a.hillRadius, size, span));
            g.setColor(Type.AMBER);
            g.drawArc(ox + mx(a.width - a.hillX, size, span) - r, oy + mx(a.depth - a.hillZ, size, span) - r,
                    r * 2, r * 2, 0, 360);
        }
        int dot = Math.max(3, CN.convertToPixels(0.9f));
        int myTeam = match.player(me).team();
        for (int i = 0; i < match.playerCount(); i++) {
            Player q = match.player(i);
            boolean friend = q.team() == myTeam;
            if (!q.alive() || (!friend && !q.input().has(com.codenameone.examples.splat.rules.Input.FIRE))) {
                continue;
            }
            int px = ox + mx(a.width - q.x(), size, span);
            int pz = oy + mx(a.depth - q.z(), size, span);
            if (i == me) {
                arrow(g, px, pz, q.yaw(), dot * 2, Type.AMBER);
            } else {
                g.setColor(friend ? Type.FRIENDLY : Type.HOSTILE);
                g.fillArc(px - dot, pz - dot, dot * 2, dot * 2, 0, 360);
            }
        }
        if (!compact) {
            g.setFont(tiny);
            g.setColor(Type.STEEL);
            g.drawString(Type.caps(a.name), x, y + size + 2);
        }
    }

    private static int mx(int mm, int size, int span) {
        return (int) ((long) mm * size / Math.max(1, span));
    }

    /// Your marker on the map: a chevron pointing where you look. The map is
    /// drawn from above with both axes flipped, so +z is up.
    private static void arrow(Graphics g, int cx, int cy, int yaw, int r, int color) {
        double a = yaw * 2 * Math.PI / Trig.FULL;
        float fx = (float) Math.sin(a);
        float fy = (float) -Math.cos(a);
        int[] xs = {cx + (int) (fx * r), cx + (int) ((-fx * 0.6f - fy * 0.7f) * r), cx + (int) ((-fx * 0.6f + fy * 0.7f) * r)};
        int[] ys = {cy + (int) (fy * r), cy + (int) ((-fy * 0.6f + fx * 0.7f) * r), cy + (int) ((-fy * 0.6f - fx * 0.7f) * r)};
        g.setColor(color);
        g.fillPolygon(xs, ys, 3);
    }

    /// Top centre: your team's score in blue on the left, theirs in red on the
    /// right, each with a bar toward the target, and the clock between them.
    private void scoreBar(Graphics g, int cx, int y) {
        int mine = match.player(me).team();
        int secs = match.ticksLeft() / Match.TICK_HZ;
        String clock = (secs / 60) + ":" + (secs % 60 < 10 ? "0" : "") + (secs % 60);
        Font num = compact ? medium : big;
        Font clockFont = compact ? small : medium;
        int cell = CN.convertToPixels(compact ? 9 : 13);
        int ch = num.getHeight();
        int clockW = clockFont.stringWidth("00:00") + CN.convertToPixels(compact ? 2 : 4);
        int target = Math.max(1, match.mode().target);
        for (int k = 0; k < 2; k++) {
            int team = k == 0 ? mine : 1 - mine;
            int bx = k == 0 ? cx - clockW / 2 - cell : cx + clockW / 2;
            int color = k == 0 ? Type.FRIENDLY : Type.HOSTILE;
            g.setColor(Type.INK);
            g.setAlpha(185);
            g.fillRect(bx, y, cell, ch);
            g.setAlpha(255);
            g.setColor(color);
            g.fillRect(bx, y, cell, 3);
            int fill = (int) ((long) Math.min(target, match.score(team)) * cell / target);
            g.fillRect(k == 0 ? bx + cell - fill : bx, y + ch - 4, fill, 4);
            g.setFont(num);
            String s = String.valueOf(match.score(team));
            g.setColor(Type.WHITE);
            g.drawString(s, bx + (cell - num.stringWidth(s)) / 2, y + 1);
        }
        g.setColor(Type.GUNMETAL);
        g.setAlpha(215);
        g.fillRect(cx - clockW / 2, y, clockW, ch);
        g.setAlpha(255);
        g.setFont(clockFont);
        g.setColor(secs <= 10 ? Type.HOSTILE : Type.WHITE);
        g.drawString(clock, cx - clockFont.stringWidth(clock) / 2, y + (ch - clockFont.getHeight()) / 2);
        if (compact) {
            return;
        }
        g.setFont(tiny);
        String goal = Type.caps(match.mode().title) + "  //  " + target;
        g.setColor(Type.AMBER);
        g.drawString(goal, cx - tiny.stringWidth(goal) / 2, y + ch + 3);
    }

    /// Top right, newest at the bottom: who, what, whom, each name in the
    /// colour of their side.
    private void killFeed(Graphics g, int right, int y) {
        if (feedHidden) {
            return;
        }
        long now = System.currentTimeMillis();
        Font name = compact ? tiny : small;
        g.setFont(name);
        int lh = name.getHeight() + (compact ? 4 : 6);
        // a phone shows the newest two, without the excuses
        int lines = compact ? 2 : FEED_LINES;
        int first = 0;
        int live = 0;
        for (int i = feed.size() - 1; i >= 0; i--) {
            if (now - feed.get(i).at <= FEED_MILLIS && ++live == lines) {
                first = i;
                break;
            }
        }
        for (int i = first; i < feed.size(); i++) {
            FeedLine f = feed.get(i);
            long age = now - f.at;
            if (age > FEED_MILLIS) {
                continue;
            }
            int alpha = age > FEED_MILLIS - 800 ? (int) (255 * (FEED_MILLIS - age) / 800) : 255;
            String verb = "  " + f.verb + "  ";
            String left = compact ? brief(f.left) : f.left;
            String right2 = compact ? brief(f.right) : f.right;
            int lw = name.stringWidth(left);
            int vw = tiny.stringWidth(verb);
            int rw = name.stringWidth(right2);
            int x = right - lw - vw - rw - 12;
            g.setColor(Type.INK);
            g.setAlpha(alpha * 150 / 255);
            g.fillRect(x - 6, y, lw + vw + rw + 18, lh - 2);
            g.setAlpha(alpha);
            g.setColor(f.leftColor);
            g.drawString(left, x, y + 2);
            g.setFont(tiny);
            g.setColor(Type.AMBER);
            g.drawString(verb, x + lw, y + 2 + (name.getHeight() - tiny.getHeight()) / 2);
            g.setFont(name);
            g.setColor(f.rightColor);
            g.drawString(right2, x + lw + vw, y + 2);
            y += lh;
        }
        g.setAlpha(255);
    }

    /// Hides the killfeed while something else, the comms list, is drawn
    /// where it goes.
    ///
    /// #### Parameters
    ///
    /// - `hidden`: true to hide it
    public void setFeedHidden(boolean hidden) {
        feedHidden = hidden;
    }

    /// Shows a line someone said, under the minimap, for a few seconds.
    ///
    /// #### Parameters
    ///
    /// - `from`: who said it
    ///
    /// - `text`: what they said
    public void chat(String from, String text) {
        chat.add(from + ": " + text);
        chatAt.add(Long.valueOf(System.currentTimeMillis()));
        while (chat.size() > 2) {
            chat.remove(0);
            chatAt.remove(0);
        }
    }

    private void chatLines(Graphics g, long now, int x, int y, int max) {
        g.setFont(tiny);
        for (int i = 0; i < chat.size(); i++) {
            long age = now - chatAt.get(i).longValue();
            if (age > FEED_MILLIS) {
                continue;
            }
            String line = chat.get(i);
            while (line.length() > 4 && tiny.stringWidth(line) > max) {
                line = line.substring(0, line.length() - 4) + "...";
            }
            int alpha = age > FEED_MILLIS - 800 ? (int) (255 * (FEED_MILLIS - age) / 800) : 255;
            g.setColor(Type.INK);
            g.setAlpha(alpha * 150 / 255);
            g.fillRect(x, y, tiny.stringWidth(line) + 8, tiny.getHeight() + 2);
            g.setAlpha(alpha);
            g.setColor(Type.WHITE);
            g.drawString(line, x + 4, y + 1);
            y += tiny.getHeight() + 4;
        }
        g.setAlpha(255);
    }

    /// Stops drawing once the result card is up, so nothing shows through it.
    ///
    /// #### Parameters
    ///
    /// - `ended`: true once the match is over and its card is shown
    public void setEnded(boolean ended) {
        this.ended = ended;
        repaint();
    }

    /// A feed name without its parenthesised aside or trailing sentence, for
    /// a phone: "Uncle Ray (disputes this)" is "Uncle Ray".
    private static String brief(String s) {
        int cut = s.indexOf(" (");
        if (cut < 0) {
            cut = s.indexOf(" wiping");
        }
        return cut > 0 ? s.substring(0, cut) : s;
    }

    /// Bottom right: the hopper count big, what is left in the pod after it,
    /// a row of pips and the marker's name.
    private void ammo(Graphics g, Player p, int right, int bottom) {
        boolean reloading = p.reloading(match.tick());
        int mag = p.marker.hopper;
        String count = reloading ? "--" : String.valueOf(p.ammo());
        String reserve = " / " + mag;
        if (compact) {
            // a small dark tab above the fire button, so the count reads over
            // a bright sky and over the marker in your hands alike
            int cw = big.stringWidth(count);
            int rw = small.stringWidth(reserve);
            int pad = CN.convertToPixels(1.2f);
            int th = big.getHeight() + (p.hasGrenade() ? tiny.getHeight() : 0);
            int x = right - cw - rw - pad;
            int y = bottom - th - pad;
            g.setColor(Type.INK);
            g.setAlpha(165);
            g.fillRect(x - pad, y - pad / 2, cw + rw + pad * 3, th + pad + pad / 2);
            g.setAlpha(255);
            g.setColor(Type.AMBER);
            g.fillRect(x - pad, y - pad / 2, CN.convertToPixels(0.5f), th + pad + pad / 2);
            g.setFont(big);
            g.setColor(p.ammo() == 0 || reloading ? Type.HOSTILE : Type.WHITE);
            g.drawString(count, x, y);
            g.setFont(small);
            g.setColor(Type.STEEL);
            g.drawString(reserve, x + cw, y + big.getHeight() - small.getHeight() - 2);
            if (p.hasGrenade()) {
                g.setFont(tiny);
                g.setColor(Type.AMBER);
                g.drawString("NADE x1", x, y + big.getHeight());
            }
            return;
        }
        g.setFont(huge);
        int cw = huge.stringWidth(count);
        int rw = medium.stringWidth(reserve);
        int y = bottom - huge.getHeight() - small.getHeight() - CN.convertToPixels(2);
        int x = right - cw - rw;
        g.setColor(0x000000);
        g.setAlpha(120);
        g.drawString(count, x + 2, y + 2);
        g.setAlpha(255);
        g.setColor(p.ammo() == 0 || reloading ? Type.HOSTILE : Type.WHITE);
        g.drawString(count, x, y);
        g.setFont(medium);
        g.setColor(Type.STEEL);
        g.drawString(reserve, x + cw, y + huge.getHeight() - medium.getHeight() - 4);
        // Pips, one per ball, right aligned under the count.
        int pip = Math.max(2, CN.convertToPixels(0.5f));
        int shown = Math.min(mag, 40);
        int py = y + huge.getHeight() + 2;
        for (int i = 0; i < shown; i++) {
            boolean full = !reloading && i < p.ammo() * shown / Math.max(1, mag);
            g.setColor(full ? Type.WHITE : 0x3a4048);
            g.fillRect(right - (i + 1) * (pip + 2), py, pip, pip * 3);
        }
        g.setFont(small);
        String name = Type.caps(p.marker.name) + (p.hasGrenade() ? "   [G] NADE x1" : "");
        g.setColor(Type.AMBER);
        g.drawString(name, right - small.stringWidth(name), py + pip * 3 + 4);
    }

    private void crosshair(Graphics g, long now, int cx, int cy, boolean show) {
        if (!show) {
            g.setColor(Type.WHITE);
            g.setAlpha(200);
            g.fillRect(cx - 1, cy - 1, 3, 3);
            g.setAlpha(255);
            return;
        }
        int r = CN.convertToPixels(1.4f);
        int gap = CN.convertToPixels(0.9f);
        g.setColor(Type.WHITE);
        g.setAlpha(220);
        g.fillRect(cx - r - gap, cy, r, 2);
        g.fillRect(cx + gap, cy, r, 2);
        g.fillRect(cx, cy - r - gap, 2, r);
        g.fillRect(cx, cy + gap, 2, r);
        g.setAlpha(255);
    }

    /// An alpha from a fade. The event can be stamped a moment after the
    /// frame read its clock, so the age may be negative; Graphics rejects
    /// anything outside 0..255.
    private static int fade(long a) {
        return (int) Math.max(0, Math.min(255, a));
    }

    /// The X that says a ball landed: white for a hit, red for a kill.
    private void hitMarker(Graphics g, long now, int cx, int cy) {
        long age = now - hitAt;
        if (age > 260) {
            return;
        }
        int r0 = CN.convertToPixels(1.2f);
        int r1 = CN.convertToPixels(hitKill ? 3.2f : 2.4f);
        g.setColor(hitKill ? Type.HOSTILE : Type.WHITE);
        g.setAlpha(fade(255 * (260 - age) / 260));
        for (int k = 0; k < 3; k++) {
            int o = k - 1;
            g.drawLine(cx - r1 + o, cy - r1, cx - r0 + o, cy - r0);
            g.drawLine(cx + r1 + o, cy - r1, cx + r0 + o, cy - r0);
            g.drawLine(cx - r1 + o, cy + r1, cx - r0 + o, cy + r0);
            g.drawLine(cx + r1 + o, cy + r1, cx + r0 + o, cy + r0);
        }
        g.setAlpha(255);
    }

    /// A red arc on the ring around the crosshair, on the side the shot came
    /// from, turning as you turn.
    private void damageArc(Graphics g, long now, int cx, int cy, int r) {
        long age = now - arcAt;
        if (age > ARC_MILLIS) {
            return;
        }
        Player p = match.player(me);
        double toward = com.codename1.util.MathUtil.atan2(-(arcX - p.x()), arcZ - p.z());
        double facing = -p.yaw() * 2 * Math.PI / Trig.FULL;
        // 0 is straight ahead (screen up), clockwise positive.
        double rel = toward - facing;
        int deg = 90 - (int) Math.toDegrees(rel);
        g.setColor(Type.HOSTILE);
        g.setAlpha(fade(220 * (ARC_MILLIS - age) / ARC_MILLIS));
        for (int k = 0; k < 5; k++) {
            g.drawArc(cx - r - k, cy - r - k, (r + k) * 2, (r + k) * 2, deg - 20, 40);
        }
        g.setAlpha(255);
    }

    /// "+100 PAINTED", rising and fading beside the crosshair.
    private void xpPop(Graphics g, long now, int x, int y) {
        long age = now - popAt;
        if (age > POP_MILLIS) {
            return;
        }
        g.setFont(medium);
        g.setColor(Type.AMBER);
        g.setAlpha(age > POP_MILLIS - 400 ? fade(255 * (POP_MILLIS - age) / 400) : 255);
        g.drawString(pop, x, y - (int) (age * CN.convertToPixels(4) / POP_MILLIS));
        g.setAlpha(255);
    }

    /// Your squad's names over their heads, projected through the camera.
    private void nameTags(Graphics g, int x0, int y0, int w, int h) {
        if (view == null) {
            return;
        }
        com.codename1.gaming.GameCamera c = view.getCamera();
        float[] look = com.codename1.gpu.Matrix4.lookAt(c.getEyeX(), c.getEyeY(), c.getEyeZ(),
                c.getTargetX(), c.getTargetY(), c.getTargetZ(), 0f, 1f, 0f);
        float[] proj = com.codename1.gpu.Matrix4.perspective((float) Math.toRadians(c.getFov()),
                w / (float) Math.max(1, h), 0.03f, 400f);
        float[] vp = new float[16];
        com.codename1.gpu.Matrix4.multiply(proj, look, vp);
        int myTeam = match.player(me).team();
        g.setFont(tiny);
        for (int i = 0; i < match.playerCount(); i++) {
            Player q = match.player(i);
            if (i == me || q.team() != myTeam || !q.alive()) {
                continue;
            }
            float wx = -q.x() * 0.001f;
            float wy = q.y() * 0.001f + 2.1f;
            float wz = q.z() * 0.001f;
            float cw = vp[3] * wx + vp[7] * wy + vp[11] * wz + vp[15];
            if (cw <= 0.1f) {
                continue;
            }
            float nx = (vp[0] * wx + vp[4] * wy + vp[8] * wz + vp[12]) / cw;
            float ny = (vp[1] * wx + vp[5] * wy + vp[9] * wz + vp[13]) / cw;
            if (nx < -1f || nx > 1f || ny < -1f || ny > 1f) {
                continue;
            }
            int sx = x0 + (int) ((nx + 1f) * 0.5f * w);
            int sy = y0 + (int) ((1f - ny) * 0.5f * h);
            String n = Type.caps(driver.name(i));
            g.setColor(0x000000);
            g.setAlpha(140);
            g.drawString(n, sx - tiny.stringWidth(n) / 2 + 1, sy + 1);
            g.setAlpha(255);
            g.setColor(Type.FRIENDLY);
            g.drawString(n, sx - tiny.stringWidth(n) / 2, sy);
        }
    }

    /// Speech over whoever was just painted and is making a scene about it,
    /// for three and a half seconds, live and in the replay alike.
    private void bubbles(Graphics g, int x0, int y0, int w, int h) {
        if (view == null) {
            return;
        }
        Match m = view.drawnMatch();
        float[] vp = viewProjection(w, h);
        for (int i = 0; i < m.playerCount(); i++) {
            Player q = m.player(i);
            if (q.alive() || q.input().has(com.codenameone.examples.splat.rules.Input.WIPE)) {
                continue;
            }
            int since = m.tick() - q.paintedAt();
            if (since < Match.TICK_HZ / 2 || since > Match.TICK_HZ * 4) {
                continue;
            }
            String line = ArenaView.complaint(i, q.paintedAt());
            if (line == null) {
                continue;
            }
            int[] at = project(vp, -q.x() * 0.001f, q.y() * 0.001f + 2.0f, q.z() * 0.001f, x0, y0, w, h);
            if (at == null) {
                continue;
            }
            Font f = compact ? tiny : small;
            g.setFont(f);
            int tw = f.stringWidth(line);
            int pad = CN.convertToPixels(1.2f);
            int bx = Math.max(x0 + pad, Math.min(x0 + w - tw - pad * 3, at[0] - tw / 2 - pad));
            int by = at[1] - f.getHeight() - pad * 3;
            g.setColor(0xffffff);
            g.fillRoundRect(bx, by, tw + pad * 2, f.getHeight() + pad, pad * 2, pad * 2);
            int[] tx = {at[0] - pad, at[0] + pad, at[0]};
            int[] ty = {by + f.getHeight() + pad - 1, by + f.getHeight() + pad - 1, by + f.getHeight() + pad * 3};
            g.fillPolygon(tx, ty, 3);
            g.setColor(0x111111);
            g.drawString(line, bx + pad, by + pad / 2);
        }
    }

    private float[] viewProjection(int w, int h) {
        com.codename1.gaming.GameCamera c = view.getCamera();
        float[] look = com.codename1.gpu.Matrix4.lookAt(c.getEyeX(), c.getEyeY(), c.getEyeZ(),
                c.getTargetX(), c.getTargetY(), c.getTargetZ(), 0f, 1f, 0f);
        float[] proj = com.codename1.gpu.Matrix4.perspective((float) Math.toRadians(c.getFov()),
                w / (float) Math.max(1, h), 0.03f, 400f);
        float[] vp = new float[16];
        com.codename1.gpu.Matrix4.multiply(proj, look, vp);
        return vp;
    }

    /// A world point on the screen, or null when it is behind or off it.
    private static int[] project(float[] vp, float wx, float wy, float wz, int x0, int y0, int w, int h) {
        float cw = vp[3] * wx + vp[7] * wy + vp[11] * wz + vp[15];
        if (cw <= 0.1f) {
            return null;
        }
        float nx = (vp[0] * wx + vp[4] * wy + vp[8] * wz + vp[12]) / cw;
        float ny = (vp[1] * wx + vp[5] * wy + vp[9] * wz + vp[13]) / cw;
        if (nx < -1f || nx > 1f || ny < -1f || ny > 1f) {
            return null;
        }
        return new int[] {x0 + (int) ((nx + 1f) * 0.5f * w), y0 + (int) ((1f - ny) * 0.5f * h)};
    }

    /// Text on a dark plate, centred on `cx`.
    private void plate(Graphics g, String s, int cx, int y, Font f, int color) {
        g.setFont(f);
        int tw = f.stringWidth(s);
        int pad = CN.convertToPixels(1.5f);
        if (tw > getWidth() - pad * 6 && f != narrow) {
            // too wide for a phone held upright: fall back to the narrowest size
            plate(g, s, cx, y, narrow, color);
            return;
        }
        g.setColor(Type.INK);
        g.setAlpha(170);
        g.fillRect(cx - tw / 2 - pad, y - pad / 2, tw + pad * 2, f.getHeight() + pad);
        g.setAlpha(255);
        g.setColor(color);
        g.drawString(s, cx - tw / 2, y);
    }

    /// The mission briefing: letterboxed, a classified header, the location
    /// stamped like a satellite feed, and the orders typed out a line at a
    /// time, read in the gravest voice available.
    private void briefing(Graphics g, int x0, int y0, int w, int h, int ticks) {
        g.setColor(0x07090b);
        g.setAlpha(238);
        g.fillRect(x0, y0, w, h);
        g.setAlpha(255);
        int bar = h / 9;
        g.fillRect(x0, y0, w, bar);
        g.fillRect(x0, y0 + h - bar, w, bar);
        Mode mode = match.mode();
        int elapsed = Math.max(0, 3 * Match.TICK_HZ - ticks);
        int left = x0 + Math.max(CN.convertToPixels(compact ? 4 : 6), w / 12);
        int room = x0 + w - left - Math.max(CN.convertToPixels(4), w / 12);
        int y = y0 + bar + CN.convertToPixels(4);
        g.setFont(tiny);
        g.setColor(Type.HOSTILE);
        g.drawString("// CLASSIFIED -- EYES ONLY -- DO NOT SHOW MUM", left, y);
        y += tiny.getHeight() + CN.convertToPixels(2);
        // a player-built mission brings its own name and orders
        Mission custom = Mission.find(match.arena().id);
        String[] title = wrap(new String[] {Type.caps(custom != null ? custom.name : mode.operation)}, room, big);
        g.setFont(big);
        g.setColor(Type.AMBER);
        for (int i = 0; i < title.length; i++) {
            g.drawString(title[i], left, y);
            y += big.getHeight();
        }
        g.setFont(small);
        g.setColor(Type.STEEL);
        Arena a = match.arena();
        String grid = Type.caps(a.name) + "   GRID " + (a.width / 100) + "-" + (a.depth / 100) + "   0" + (match.seed() & 7)
                + "00 HRS LOCAL";
        String[] gl = wrap(new String[] {grid}, room, small);
        for (int i = 0; i < gl.length; i++) {
            g.drawString(gl[i], left, y);
            y += small.getHeight();
        }
        y += CN.convertToPixels(3);
        String[] lines = {
            "OBJECTIVE: " + Type.caps(mode.title) + ". FIRST TO " + mode.target + ".",
            custom != null && custom.briefing.trim().length() > 0 ? Type.caps(custom.briefing.trim())
                    : "INTEL SUGGESTS THE ENEMY HAS BROUGHT [REDACTED] AND A COOLER.",
            "RULES OF ENGAGEMENT: NO BLIND FIRING. NO WIPING. NO CRYING.",
            "YOU ARE " + (driver.interactive() ? Type.caps(ArenaView.TEAM_NAMES[match.player(me).team()])
                    : "ON THE SIDELINES") + ". GODSPEED.",
        };
        g.setFont(medium);
        lines = wrap(lines, room, medium);
        int chars = elapsed * 3;
        for (int i = 0; i < lines.length && chars > 0; i++) {
            String line = lines[i].substring(0, Math.min(lines[i].length(), chars));
            chars -= lines[i].length();
            int at = line.indexOf("[REDACTED]");
            g.setColor(Type.WHITE);
            if (at >= 0) {
                g.drawString(line.substring(0, at), left, y);
                int rx = left + medium.stringWidth(line.substring(0, at));
                g.fillRect(rx, y + 3, medium.stringWidth("[REDACTED]"), medium.getHeight() - 6);
                if (line.length() > at + 10) {
                    g.drawString(line.substring(at + 10), rx + medium.stringWidth("[REDACTED]"), y);
                }
            } else {
                g.drawString(line, left, y);
            }
            y += medium.getHeight() + 4;
        }
        g.setFont(huge);
        String n = String.valueOf(ticks / Match.TICK_HZ + 1);
        g.setColor(Type.AMBER);
        g.drawString(n, x0 + w - left + x0 - huge.stringWidth(n), y0 + h - bar - huge.getHeight() - CN.convertToPixels(2));
    }

    /// Breaks each line at spaces so none is wider than `max`; a phone in
    /// portrait would otherwise clip the briefing at the right edge.
    private static String[] wrap(String[] lines, int max, Font f) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (String line : lines) {
            String rest = line;
            while (f.stringWidth(rest) > max) {
                int cut = rest.lastIndexOf(' ');
                while (cut > 0 && f.stringWidth(rest.substring(0, cut)) > max) {
                    cut = rest.lastIndexOf(' ', cut - 1);
                }
                if (cut <= 0) {
                    break;
                }
                out.add(rest.substring(0, cut));
                rest = rest.substring(cut + 1);
            }
            out.add(rest);
        }
        return out.toArray(new String[out.size()]);
    }

    /// Paint on the goggles: big blobs in the shooter's paint that drip and
    /// fade.
    private void splatter(Graphics g, long now, int x0, int y0, int w, int h) {
        long age = now - splatAt;
        if (age > 3500 || match == null) {
            return;
        }
        int alpha = fade(190 * (3500 - age) / 3500);
        int color = 0xff7a1f;
        Player p = match.player(me);
        if (p.paintedBy() >= 0 && p.paintedBy() < match.playerCount()) {
            color = ArenaView.TEAM_COLORS[match.player(p.paintedBy()).team()] & 0xffffff;
        }
        int s = splatSeed * 2654435 + 7;
        int unit = Math.min(w, h);
        // A few real splats hugging the edges of the visor, running down as
        // they age: the middle stays clear so you can see who did it.
        for (int i = 0; i < 4; i++) {
            s = s * 1103515245 + 12345;
            int along = ((s >>> 8) & 0x7fff) % Math.max(1, unit);
            int edge = (s >>> 3) & 3;
            int bx = edge == 0 ? x0 + unit / 14 : edge == 1 ? x0 + w - unit / 14 : x0 + along * w / Math.max(1, unit);
            int by = edge >= 2 ? (edge == 2 ? y0 + h / 7 : y0 + h - h / 7) : y0 + along * h / Math.max(1, unit);
            int r = unit / 6 + (i % 3) * unit / 20;
            int drip = (int) Math.min(r / 2, age * r / 2600);
            g.setAlpha(alpha);
            g.drawImage(visorSplat(color, (splatSeed + i) & 3), bx - r, by - r + drip / 2, r * 2, r * 2 + drip);
        }
        g.setAlpha(255);
    }

    private final java.util.Map<Integer, Image> visorSplats = new java.util.HashMap<Integer, Image>();

    /// A dripping splat in `rgb`, one of four shapes, built once and kept.
    private Image visorSplat(int rgb, int variant) {
        Integer key = Integer.valueOf((rgb << 2) | variant);
        Image img = visorSplats.get(key);
        if (img == null) {
            int[] px = PaintDecals.splat(9001L + variant * 7919L, true);
            for (int i = 0; i < px.length; i++) {
                px[i] = (px[i] & 0xff000000) | rgb;
            }
            img = Image.createImage(px, PaintDecals.SIZE, PaintDecals.SIZE);
            visorSplats.put(key, img);
        }
        return img;
    }

    /// The stamp: slammed on at twice the size and settling, a little crooked,
    /// like a rubber stamp in a hurry.
    private void stamp(Graphics g, long now, int x0, int y0, int w, int h) {
        long age = now - stampAt;
        if (age > STAMP_MILLIS) {
            return;
        }
        float k = age < 160 ? 2f - age / 160f : 1f;
        int alpha = age > STAMP_MILLIS - 400 ? fade(255 * (STAMP_MILLIS - age) / 400) : 255;
        int tw = stampFont.stringWidth(stampText);
        int th = stampFont.getHeight();
        int cx = x0 + w / 2;
        // high, so the line under it clears the buttons on a phone
        int cy = y0 + h * 36 / 100 - th / 2;
        boolean rotate = g.isAffineSupported();
        if (rotate) {
            g.rotateRadians(-0.18f, cx, cy);
            g.scale(k, k);
        }
        float inv = rotate ? 1f / k : 1f;
        int sx = (int) (cx * inv) - tw / 2;
        int sy = (int) (cy * inv) - th / 2;
        g.setAlpha(alpha);
        g.setColor(Type.STAMP);
        int pad = CN.convertToPixels(2);
        for (int b = 0; b < 4; b++) {
            g.drawRect(sx - pad - b * 2, sy - pad / 2 - b * 2, tw + pad * 2 + b * 4, th + pad + b * 4);
        }
        g.setFont(stampFont);
        g.drawString(stampText, sx, sy);
        if (rotate) {
            g.resetAffine();
        }
        plate(g, stampSub, x0 + w / 2, cy + th + CN.convertToPixels(2), medium, Type.WHITE);
        g.setAlpha(255);
    }

    private int safeTop() {
        if (getComponentForm() == null) {
            return 0;
        }
        com.codename1.ui.geom.Rectangle r = getComponentForm().getSafeArea();
        return r == null ? 0 : Math.max(0, r.getY() - getAbsoluteY());
    }

    private int safeBottom() {
        if (getComponentForm() == null) {
            return 0;
        }
        com.codename1.ui.geom.Rectangle r = getComponentForm().getSafeArea();
        if (r == null || r.getHeight() <= 0) {
            return 0;
        }
        return Math.max(0, getAbsoluteY() + getHeight() - (r.getY() + r.getHeight()));
    }
}
// end::splat-hud[]
