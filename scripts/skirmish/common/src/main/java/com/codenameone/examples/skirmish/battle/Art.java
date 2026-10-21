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

import com.codename1.gaming.SpriteSheet;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codenameone.examples.skirmish.rules.Arena;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Cards;
import java.util.HashMap;
import java.util.Map;

/// Every picture the battle draws, painted in code when it is first asked for.
///
/// The sample ships no art files: a unit is a disc in its card's colour with
/// its team's ring and a glyph, and a four-frame [SpriteSheet] of it squashing
/// and stretching is what makes it look like it walks. That keeps the
/// repository free of binary assets and makes the shape of the art pipeline
/// plain. To re-skin the game, replace [#unitSheet] with a sheet loaded from
/// your own atlas (`Image.createImage("/units.png")`) -- frame layout and the
/// rest of the renderer stay as they are. See the "Make it your own" chapter.
// tag::skirmish-art[]
public final class Art {
    /// The player's own colour and the opponent's.
    public static final int BLUE = 0x2f7bff;
    public static final int RED = 0xe0453a;
    public static final int FRAMES = 4;
    /// Ground texture pixels per arena tile.
    public static final int TILE_PX = 32;

    /// A colour per card, in the order of [Cards#at].
    private static final Map<String, Integer> CARD_COLORS = new HashMap<String, Integer>();

    static {
        CARD_COLORS.put("squire", 0xc9a227);
        CARD_COLORS.put("twin-archers", 0x6dbb4a);
        CARD_COLORS.put("ogre", 0x8a5a3c);
        CARD_COLORS.put("bat-swarm", 0x5b3f8c);
        CARD_COLORS.put("sharpshooter", 0x2e9c9a);
        CARD_COLORS.put("spearmen", 0xb07a3a);
        CARD_COLORS.put("drake", 0xd2512d);
        CARD_COLORS.put("imp-den", 0x7a2f6b);
        CARD_COLORS.put("imp", 0xa64aa0);
        CARD_COLORS.put("fire-orb", 0xff7a1a);
        CARD_COLORS.put("arrow-rain", 0x9aa3ad);
        CARD_COLORS.put(Cards.KING_TOWER, 0xd9b44a);
        CARD_COLORS.put(Cards.PRINCESS_TOWER, 0xb9bec7);
    }

    private static final Map<String, SpriteSheet> SHEETS = new HashMap<String, SpriteSheet>();
    private static final Map<String, Image> ICONS = new HashMap<String, Image>();
    private static Image pixel;

    private Art() {
    }

    public static int colorOf(String cardId) {
        Integer c = CARD_COLORS.get(cardId);
        return c == null ? 0x888888 : c.intValue();
    }

    /// A walk cycle for `cardId` in `team` colours: [#FRAMES] frames of
    /// `size` pixels side by side.
    public static synchronized SpriteSheet unitSheet(String cardId, int team, int size) {
        String key = cardId + "/" + team + "/" + size;
        SpriteSheet sheet = SHEETS.get(key);
        if (sheet == null) {
            Image img = Image.createImage(size * FRAMES, size, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            CardDef card = Cards.get(cardId);
            for (int f = 0; f < FRAMES; f++) {
                // Squash and stretch: the frames breathe 0, +6%, 0, -6%.
                int squash = f == 1 ? 6 : f == 3 ? -6 : 0;
                if (card.kind == CardDef.TOWER || card.kind == CardDef.BUILDING) {
                    drawStructure(g, f * size, 0, size, cardId, team, f);
                } else {
                    drawUnit(g, f * size, 0, size, cardId, team, squash, card.isFlying());
                }
            }
            sheet = new SpriteSheet(img, size, size);
            SHEETS.put(key, sheet);
        }
        return sheet;
    }

    /// The picture on a card in the hand and the deck builder.
    public static synchronized Image cardIcon(String cardId, int size) {
        String key = cardId + "/" + size;
        Image icon = ICONS.get(key);
        if (icon == null) {
            icon = Image.createImage(size, size, 0);
            Graphics g = icon.getGraphics();
            g.setAntiAliased(true);
            CardDef card = Cards.get(cardId);
            if (card.kind == CardDef.SPELL) {
                drawSpell(g, 0, 0, size, cardId);
            } else if (card.kind == CardDef.BUILDING) {
                drawStructure(g, 0, 0, size, cardId, 0, 0);
            } else {
                drawUnit(g, 0, 0, size, cardId, 0, 0, card.isFlying());
            }
            ICONS.put(key, icon);
        }
        return icon;
    }

    /// A white pixel, tinted and stretched for bars and overlays.
    public static synchronized Image pixel() {
        if (pixel == null) {
            pixel = Image.createImage(4, 4, 0xffffffff);
        }
        return pixel;
    }

    /// A soft round blob for effects; tinted per use.
    public static synchronized SpriteSheet burst(int size) {
        String key = "burst/" + size;
        SpriteSheet sheet = SHEETS.get(key);
        if (sheet == null) {
            int frames = 6;
            Image img = Image.createImage(size * frames, size, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            for (int f = 0; f < frames; f++) {
                int r = size * (f + 2) / (frames + 2) / 2;
                int cx = f * size + size / 2;
                int cy = size / 2;
                g.setAlpha(255 - f * 38);
                g.setColor(0xffffff);
                g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
                g.setAlpha(255);
            }
            sheet = new SpriteSheet(img, size, size);
            SHEETS.put(key, sheet);
        }
        return sheet;
    }

    /// The field's ground texture, `TILE_PX` pixels per tile, laid out in rules
    /// coordinates (row 0 is player 1's back line). Checkered turf, worn lanes,
    /// sandy banks and the river; the bridges are 3D models on top of it.
    public static Image field() {
        int t = TILE_PX;
        int w = 18 * t;
        int h = 32 * t;
        Image img = Image.createImage(w, h, 0xff5a9644);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        for (int ty = 0; ty < 32; ty++) {
            for (int tx = 0; tx < 18; tx++) {
                g.setColor((tx + ty) % 2 == 0 ? 0x5f9e47 : 0x578f41);
                g.fillRect(tx * t, ty * t, t, t);
            }
        }
        // Worn lanes, wider near the towers.
        g.setColor(0xc9b27a);
        g.setAlpha(150);
        for (int lane = 0; lane < 2; lane++) {
            int lx = Arena.laneX(lane) * t / Arena.TILE;
            g.fillRoundRect(lx - t * 3 / 4, t * 5, t * 3 / 2, h - t * 10, t, t);
        }
        // the cross path between the king towers and the lanes
        g.fillRoundRect(Arena.LEFT_LANE_X * t / Arena.TILE, t * 5 / 2, (Arena.RIGHT_LANE_X - Arena.LEFT_LANE_X)
                * t / Arena.TILE, t, t, t);
        g.fillRoundRect(Arena.LEFT_LANE_X * t / Arena.TILE, h - t * 7 / 2, (Arena.RIGHT_LANE_X - Arena.LEFT_LANE_X)
                * t / Arena.TILE, t, t, t);
        g.setAlpha(255);
        // Banks, then the water with a lighter centre.
        int r1 = Arena.RIVER_TOP * t / Arena.TILE;
        int r2 = Arena.RIVER_BOTTOM * t / Arena.TILE;
        g.setColor(0xd8c48e);
        g.fillRect(0, r1 - t / 4, w, r2 - r1 + t / 2);
        g.fillLinearGradient(0x2f7fc8, 0x46a6e8, 0, r1, w, (r2 - r1) / 2, false);
        g.fillLinearGradient(0x46a6e8, 0x2f7fc8, 0, r1 + (r2 - r1) / 2, w, (r2 - r1) / 2, false);
        g.setColor(0xffffff);
        g.setAlpha(60);
        for (int i = 0; i < 18; i++) {
            int wx = (i * 37) % w;
            g.fillRoundRect(wx, r1 + ((i * 13) % (r2 - r1 - 6)), t, 3, 3, 3);
        }
        g.setAlpha(255);
        // Plank bridges over the river.
        for (int lane = 0; lane < 2; lane++) {
            int bx = (Arena.laneX(lane) - Arena.BRIDGE_HALF_WIDTH) * t / Arena.TILE;
            int bw = 2 * Arena.BRIDGE_HALF_WIDTH * t / Arena.TILE;
            g.setColor(0x5a3a1c);
            g.fillRect(bx - 3, r1 - t / 3, bw + 6, r2 - r1 + t * 2 / 3);
            for (int py = r1 - t / 3; py < r2 + t / 3; py += 8) {
                g.setColor((py / 8) % 2 == 0 ? 0xa8743e : 0x9a6835);
                g.fillRect(bx, py, bw, 7);
            }
        }
        // A low stone border around the play area.
        g.setColor(0x8f8a7c);
        g.drawRect(0, 0, w - 1, h - 1);
        g.drawRect(1, 1, w - 3, h - 3);
        return img;
    }

    /// The ground beyond the field.
    public static Image meadow() {
        int s = 128;
        Image img = Image.createImage(s, s, 0xff4f8f3e);
        Graphics g = img.getGraphics();
        for (int i = 0; i < 260; i++) {
            int x = (i * 53) % s;
            int y = (i * 97 + i / 3) % s;
            g.setColor(i % 3 == 0 ? 0x5a9c47 : 0x467f36);
            g.fillRect(x, y, 2 + i % 3, 2);
        }
        return img;
    }

    /// A soft-edged ring for the circle under a unit's feet, white so a
    /// material color tints it.
    public static int[] ring(int size) {
        int[] argb = new int[size * size];
        float r = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - r) / r;
                float dy = (y + 0.5f - r) / r;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a;
                if (d > 1f) {
                    a = 0f;
                } else if (d > 0.78f) {
                    a = 1f - Math.abs(d - 0.89f) / 0.11f;
                } else {
                    a = 0.18f;
                }
                int c = (int) (Math.max(0f, a) * 255f);
                argb[y * size + x] = (c << 24) | 0xffffff;
            }
        }
        return argb;
    }

    /// The arena seen from `me`'s side, `w` x `h` pixels with the arena
    /// centred in it. Grass, the river and its two bridges, the lanes.
    public static Image arena(int w, int h, Viewport v) {
        Image img = Image.createImage(w, h, 0xff23402a);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        int left = v.sx(0, 0);
        int right = v.sx(Arena.WIDTH, 0);
        int top = v.sy(0, 0);
        int bottom = v.sy(0, Arena.HEIGHT);
        int ax = Math.min(left, right);
        int ay = Math.min(top, bottom);
        int aw = Math.abs(right - left);
        int ah = Math.abs(bottom - top);
        // Checkered turf, a tile at a time.
        int tile = Math.max(1, aw / 18);
        for (int ty = 0; ty < 32; ty++) {
            for (int tx = 0; tx < 18; tx++) {
                g.setColor((tx + ty) % 2 == 0 ? 0x5fa64a : 0x579c43);
                int px = v.sx(tx * Arena.TILE, 0);
                int py = v.sy(0, ty * Arena.TILE);
                int qx = v.sx((tx + 1) * Arena.TILE, 0);
                int qy = v.sy(0, (ty + 1) * Arena.TILE);
                g.fillRect(Math.min(px, qx), Math.min(py, qy), Math.abs(qx - px) + 1, Math.abs(qy - py) + 1);
            }
        }
        // The lanes, worn into the grass.
        g.setColor(0x9c8a5a);
        g.setAlpha(110);
        for (int lane = 0; lane < 2; lane++) {
            int lx = v.sx(Arena.laneX(lane), 0);
            g.fillRect(lx - tile / 2, ay, tile, ah);
        }
        g.setAlpha(255);
        // The river and its bridges.
        int r1 = v.sy(0, Arena.RIVER_TOP);
        int r2 = v.sy(0, Arena.RIVER_BOTTOM);
        g.fillLinearGradient(0x3d8fd6, 0x2a6fb0, ax, Math.min(r1, r2), aw, Math.abs(r2 - r1), false);
        for (int lane = 0; lane < 2; lane++) {
            int bx1 = v.sx(Arena.laneX(lane) - Arena.BRIDGE_HALF_WIDTH, 0);
            int bx2 = v.sx(Arena.laneX(lane) + Arena.BRIDGE_HALF_WIDTH, 0);
            g.setColor(0x8b5a2b);
            g.fillRect(Math.min(bx1, bx2), Math.min(r1, r2) - tile / 4, Math.abs(bx2 - bx1),
                    Math.abs(r2 - r1) + tile / 2);
            g.setColor(0x6e4520);
            for (int plank = Math.min(r1, r2); plank < Math.max(r1, r2); plank += Math.max(3, tile / 3)) {
                g.drawLine(Math.min(bx1, bx2), plank, Math.max(bx1, bx2), plank);
            }
        }
        // A frame around the field.
        g.setColor(0x2d1f12);
        g.drawRect(ax, ay, aw - 1, ah - 1);
        return img;
    }

    private static void drawUnit(Graphics g, int x, int y, int size, String cardId, int team,
            int squashPercent, boolean flying) {
        int teamColor = team == 0 ? BLUE : RED;
        int body = colorOf(cardId);
        int w = size * (82 + squashPercent) / 100;
        int h = size * (82 - squashPercent) / 100;
        int cx = x + size / 2;
        int cy = y + size / 2 + (flying ? -size / 10 : 0);
        // Shadow, further below a flyer.
        g.setColor(0x000000);
        g.setAlpha(70);
        int sh = size / 5;
        g.fillArc(cx - w / 2, y + size - sh - 1, w, sh, 0, 360);
        g.setAlpha(255);
        // Team ring, body, a highlight.
        g.setColor(teamColor);
        g.fillArc(cx - w / 2, cy - h / 2, w, h, 0, 360);
        int iw = w * 78 / 100;
        int ih = h * 78 / 100;
        g.fillRadialGradient(lighter(body), body, cx - iw / 2, cy - ih / 2, iw, ih);
        g.setColor(0xffffff);
        g.setAlpha(90);
        g.fillArc(cx - iw / 3, cy - ih / 3, iw / 3, ih / 4, 0, 360);
        g.setAlpha(255);
        if (flying) {
            // Wings.
            g.setColor(darker(body));
            g.fillTriangle(cx - w / 2, cy, cx - w / 2 - size / 8, cy - size / 6, cx - w / 4, cy - h / 3);
            g.fillTriangle(cx + w / 2, cy, cx + w / 2 + size / 8, cy - size / 6, cx + w / 4, cy - h / 3);
        }
        glyph(g, cx, cy, size, Cards.get(cardId).name);
    }

    private static void drawStructure(Graphics g, int x, int y, int size, String cardId, int team, int frame) {
        int teamColor = team == 0 ? BLUE : RED;
        int stone = colorOf(cardId);
        int m = size / 10;
        // Base and battlements.
        g.setColor(0x000000);
        g.setAlpha(60);
        g.fillArc(x + m, y + size - size / 4, size - 2 * m, size / 5, 0, 360);
        g.setAlpha(255);
        g.fillLinearGradient(lighter(stone), darker(stone), x + m, y + size / 4, size - 2 * m,
                size * 3 / 5, true);
        int merlon = (size - 2 * m) / 5;
        g.setColor(stone);
        for (int i = 0; i < 5; i += 2) {
            g.fillRect(x + m + i * merlon, y + size / 4 - merlon, merlon, merlon);
        }
        // A banner in the team colour, fluttering a pixel per frame.
        g.setColor(teamColor);
        int flutter = frame % 2;
        g.fillRect(x + size / 2 - size / 10, y + size / 3 + flutter, size / 5, size / 4);
        glyph(g, x + size / 2, y + size * 3 / 5, size, Cards.get(cardId).name);
    }

    private static void drawSpell(Graphics g, int x, int y, int size, String cardId) {
        int c = colorOf(cardId);
        g.fillRadialGradient(0xffffff, c, x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4);
        glyph(g, x + size / 2, y + size / 2, size, Cards.get(cardId).name);
    }

    private static void glyph(Graphics g, int cx, int cy, int size, String name) {
        String letter = name.substring(0, 1);
        Font f = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD,
                size >= 48 ? Font.SIZE_LARGE : size >= 28 ? Font.SIZE_MEDIUM : Font.SIZE_SMALL);
        g.setFont(f);
        int tw = f.stringWidth(letter);
        int th = f.getHeight();
        g.setColor(0x1b1b1b);
        g.setAlpha(120);
        g.drawString(letter, cx - tw / 2 + 1, cy - th / 2 + 1);
        g.setAlpha(255);
        g.setColor(0xffffff);
        g.drawString(letter, cx - tw / 2, cy - th / 2);
    }

    static int lighter(int rgb) {
        return mix(rgb, 0xffffff, 35);
    }

    static int darker(int rgb) {
        return mix(rgb, 0x000000, 30);
    }

    private static int mix(int a, int b, int percent) {
        int r = ((a >> 16) & 0xff) * (100 - percent) / 100 + ((b >> 16) & 0xff) * percent / 100;
        int gr = ((a >> 8) & 0xff) * (100 - percent) / 100 + ((b >> 8) & 0xff) * percent / 100;
        int bl = (a & 0xff) * (100 - percent) / 100 + (b & 0xff) * percent / 100;
        return (r << 16) | (gr << 8) | bl;
    }
}
// end::skirmish-art[]
