package com.codename1.tools.skindesigner;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import java.util.List;

/**
 * Live device preview rendered onto a CN1 {@link Component}. Mirrors the
 * {@code DeviceSvg} React component from the design — rounded body, screen
 * tint, cutouts and home indicator. When a body image is supplied (image
 * source) it is drawn instead of the gradient body and clipped to the
 * rounded shape.
 */
public final class DevicePreview extends Component {
    static final int VB_W = 320;
    static final int VB_H = 620;

    private SkinModel skin;
    private Image bodyImage;
    private final boolean fixedSize;

    public DevicePreview() {
        this(false);
    }

    public DevicePreview(boolean fixedSize) {
        this.fixedSize = fixedSize;
        setUIID("DevicePreview");
        setFocusable(false);
    }

    public void setSkin(SkinModel skin) {
        this.skin = skin;
        repaint();
    }

    public void setBodyImage(Image bodyImage) {
        this.bodyImage = bodyImage;
        repaint();
    }

    @Override
    protected Dimension calcPreferredSize() {
        if (fixedSize) {
            return new Dimension(CN.convertToPixels(40), CN.convertToPixels(78));
        }
        return new Dimension(CN.convertToPixels(60), CN.convertToPixels(110));
    }

    @Override
    public void paint(Graphics g) {
        if (skin == null) {
            return;
        }
        int w = getInnerWidth();
        int h = getInnerHeight();
        int x = getX() + getStyle().getPaddingLeftNoRTL();
        int y = getY() + getStyle().getPaddingTop();
        if (w <= 4 || h <= 4) {
            return;
        }

        // Fit a 320x620 viewbox into the available area.
        float scale = Math.min(((float) w) / VB_W, ((float) h) / VB_H);
        int drawW = Math.max(1, Math.round(VB_W * scale));
        int drawH = Math.max(1, Math.round(VB_H * scale));
        int drawX = x + (w - drawW) / 2;
        int drawY = y + (h - drawH) / 2;

        int bezel = Math.round(skin.bezel * scale);
        int cornerR = Math.round(skin.cornerR * scale);
        int screenX = drawX + bezel;
        int screenY = drawY + bezel;
        int screenW = drawW - bezel * 2;
        int screenH = drawH - bezel * 2;
        int screenR = Math.max(2, cornerR - Math.round(8 * scale));

        int[] origClip = g.getClip();
        g.pushClip();
        g.clipRect(x, y, w, h);

        // Body
        if (bodyImage != null) {
            int oldClip[] = g.getClip();
            g.pushClip();
            clipRoundRect(g, drawX, drawY, drawW, drawH, cornerR);
            g.drawImage(bodyImage, drawX, drawY, drawW, drawH);
            g.popClip();
            g.setClip(oldClip);
        } else {
            // Faux gradient using two color fills + outline, to avoid relying on RadialGradient
            g.setColor(0x121822);
            fillRoundRect(g, drawX, drawY, drawW, drawH, cornerR);
            g.setColor(0x2a2f3a);
            int inset = Math.max(1, Math.round(4 * scale));
            int oldAlpha = g.getAlpha();
            g.setAlpha(180);
            fillRoundRect(g, drawX + inset, drawY + inset, drawW - inset * 2, drawH - inset * 2, Math.max(0, cornerR - inset));
            g.setAlpha(oldAlpha);
        }

        // Screen tint
        g.setColor(0x1E3A8A);
        int oldA = g.getAlpha();
        g.setAlpha(230);
        fillRoundRect(g, screenX, screenY, screenW, screenH, screenR);
        g.setColor(0x2A8A8A);
        g.setAlpha(80);
        fillRoundRect(g, screenX, screenY, screenW, screenH, screenR);
        g.setAlpha(oldA);

        // Cutouts overlay (relative to top-center of screen)
        int cx = drawX + drawW / 2;
        if (skin.cutouts != null) {
            List<SkinModel.Cutout> cutouts = skin.cutouts;
            for (SkinModel.Cutout c : cutouts) {
                drawCutout(g, c, scale, cx, screenY);
            }
        }

        // Screen dashed outline
        g.setColor(0x2F6BFF);
        int dashAlpha = (int) (255 * 0.7f);
        int prev = g.getAlpha();
        g.setAlpha(dashAlpha);
        drawDashedRoundRect(g, screenX, screenY, screenW, screenH, screenR, Math.max(2, Math.round(4 * scale)), Math.max(1, Math.round(3 * scale)));
        g.setAlpha(prev);

        // Home indicator
        if (skin.homeIndicator) {
            g.setColor(0xffffff);
            int hiW = Math.round(100 * scale);
            int hiH = Math.max(2, Math.round(4 * scale));
            int hx = cx - hiW / 2;
            int hy = drawY + drawH - bezel - Math.round(10 * scale);
            int hiA = g.getAlpha();
            g.setAlpha((int) (255 * 0.7f));
            fillRoundRect(g, hx, hy, hiW, hiH, hiH / 2);
            g.setAlpha(hiA);
        }

        g.popClip();
        g.setClip(origClip);
    }

    private void drawCutout(Graphics g, SkinModel.Cutout c, float scale, int cx, int screenTopY) {
        g.setColor(0x000000);
        int cw = Math.round(c.w * scale);
        int ch = Math.round(c.h * scale);
        int ox = cx + Math.round(c.x * scale);
        int oy = screenTopY + Math.round(c.y * scale);
        if (SkinModel.CUTOUT_NOTCH.equals(c.type)) {
            int x0 = ox - cw / 2;
            int rad = Math.max(2, Math.round(8 * scale));
            // Top straight portion
            g.fillRect(x0, screenTopY, cw, ch - rad);
            // Bottom rounded portion (use round rect, clipping the top half off via overdrawing top)
            g.fillRoundRect(x0, screenTopY + ch - rad * 2, cw, rad * 2, rad * 2, rad * 2);
            // Cover the top half of the round rect
            g.fillRect(x0, screenTopY, cw, ch - rad);
        } else if (SkinModel.CUTOUT_ISLAND.equals(c.type)) {
            int x0 = ox - cw / 2;
            fillRoundRect(g, x0, oy, cw, ch, ch);
        } else if (SkinModel.CUTOUT_HOLE.equals(c.type)) {
            int r = cw / 2;
            g.fillArc(ox - r, oy, cw, ch, 0, 360);
        }
    }

    /**
     * fillRoundRect with corner radius in pixels (CN1 takes diameter for arc width).
     */
    private static void fillRoundRect(Graphics g, int x, int y, int w, int h, int radius) {
        if (w <= 0 || h <= 0) return;
        int d = Math.max(0, Math.min(radius * 2, Math.min(w, h)));
        g.fillRoundRect(x, y, w, h, d, d);
    }

    private static void clipRoundRect(Graphics g, int x, int y, int w, int h, int radius) {
        // CN1 clipRect is rectangular; approximate by clipping to bounding box only.
        // The body image sits inside a rounded gradient; image source mostly fits the
        // bezel region so the rectangular clip is acceptable.
        g.clipRect(x, y, w, h);
    }

    private static void drawDashedRoundRect(Graphics g, int x, int y, int w, int h, int radius, int dash, int gap) {
        // CN1 doesn't expose dashed strokes — approximate with a solid round outline.
        int d = Math.max(0, Math.min(radius * 2, Math.min(w, h)));
        g.drawRoundRect(x, y, w - 1, h - 1, d, d);
    }
}
