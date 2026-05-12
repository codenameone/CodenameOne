package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Repro for issue #4907 (glitchy metal drawing): a magnifier-style render path
// sets a small clip rect, then applies g.scale() + g.translate() to draw a
// scaled and panned copy of a much larger surface into that clip. Under the
// pre-metal iOS pipeline (and on JavaSE/Android) drawing emitted after the
// transform is correctly clipped to the inner rect; under metal the clip is
// reportedly not respected and content leaks outside the inner rect.
//
// The transform math:
//
//   After  g.scale(s, s);  g.translate(-ax, -ay);
//   a source point p ends up at native screen coord  s * (p - (ax, ay)).
//   (Graphics adds xTranslate/yTranslate to coords before passing them to
//   the native impl on every active port -- isTranslationSupported() is
//   false everywhere -- and the native impl then applies the scale.)
//
//   To make the clip-rect centre a fixed point of the transform, pick
//   ax = clipCenterX / s and ay = clipCenterY / s. Then a source patch
//   centred at (clipCenterX, clipCenterY) of size (clipW/s, clipH/s)
//   maps exactly onto the clip rect on screen.
//
// The cell is light gray with a small centred clip rect (1/5 of the cell so
// there is plenty of "outside" area to make bleed obvious). Inside the
// transform we paint a 5-patch pattern in source space: a central red patch
// sized so it exactly fills the clip rect on screen, plus four surrounding
// patches in distinct colours (green N, blue S, orange W, magenta E). Each
// side patch is one full source-patch-size off-centre, so under the 2x
// scale they map to the cell rows/columns immediately adjacent to the clip
// rect on screen -- well outside the clip rect, entirely invisible under a
// correct clip.
//
// Correct render: red square inside a black clip-rect outline (drawn last
// on top), light gray everywhere else, small green sentinel dot in the
// corner. A bleed shows one or more coloured patches outside the outline;
// the colour identifies the leak direction. A full clip drop shows all
// four side patches.
public class ClipUnderScaleTranslate extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        g.setColor(0xeeeeee);
        g.fillRect(x, y, w, h);

        // Small centred clip rect -- 1/5 of the cell.
        int clipW = Math.max(20, w / 5);
        int clipH = Math.max(20, h / 5);
        int clipX = x + (w - clipW) / 2;
        int clipY = y + (h - clipH) / 2;
        int clipCenterX = clipX + clipW / 2;
        int clipCenterY = clipY + clipH / 2;

        g.pushClip();
        g.clipRect(clipX, clipY, clipW, clipH);

        // Mirror the magnifier code in issue #4907.
        float scale = 2.0f;
        int ax = (int) (clipCenterX / scale);
        int ay = (int) (clipCenterY / scale);
        g.scale(scale, scale);
        g.translate(-ax, -ay);

        // Source-space patch size: clipW/s by clipH/s. Under the 2x scale
        // each patch maps to a clipW x clipH rectangle on screen. The
        // central patch centred at (clipCenterX, clipCenterY) maps onto
        // the clip rect; each side patch maps onto the adjacent cell row /
        // column and should be entirely clipped out.
        int patchW = (int) (clipW / scale);
        int patchH = (int) (clipH / scale);
        int halfPW = patchW / 2;
        int halfPH = patchH / 2;

        // Centre (red) -- fills the clip rect under a correct clip.
        g.setColor(0xff0000);
        g.fillRect(clipCenterX - halfPW, clipCenterY - halfPH, patchW, patchH);

        // North (green).
        g.setColor(0x00aa00);
        g.fillRect(clipCenterX - halfPW, clipCenterY - halfPH - patchH, patchW, patchH);

        // South (blue).
        g.setColor(0x0000ff);
        g.fillRect(clipCenterX - halfPW, clipCenterY - halfPH + patchH, patchW, patchH);

        // West (orange).
        g.setColor(0xff8800);
        g.fillRect(clipCenterX - halfPW - patchW, clipCenterY - halfPH, patchW, patchH);

        // East (magenta).
        g.setColor(0xff00ff);
        g.fillRect(clipCenterX - halfPW + patchW, clipCenterY - halfPH, patchW, patchH);

        g.translate(ax, ay);
        g.scale(1f / scale, 1f / scale);

        g.popClip();

        // Black clip-rect outline drawn AFTER popClip on top of the fill so
        // its position is unaffected by any leak and the diff has a stable
        // visual anchor for spotting bleed.
        g.setColor(0x000000);
        g.drawRect(clipX, clipY, clipW - 1, clipH - 1);

        // Sentinel outside the clip rect drawn last with the identity
        // transform. Should always render -- if it doesn't, the pop /
        // transform-reset path is also broken.
        g.setColor(0x008000);
        g.fillRect(x + 2, y + 2, 6, 6);
    }

    @Override
    protected String screenshotName() {
        return "graphics-clip-under-scale-translate";
    }
}
