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
// The cell is light gray with a small centred clip rect (1/5 of the cell so
// there is plenty of "outside" area to make bleed obvious). Inside the
// transform we paint a recognisable 5-patch pattern: a central red patch
// (sized so that, under the 2x magnification, it exactly fills the clip rect
// in screen space) plus four surrounding patches in distinct colours
// (green / blue / orange / magenta). The surrounding patches sit far enough
// from the magnifier centre that, when the clip is honoured, they are
// entirely outside the clip rect and invisible -- so the cell shows only a
// red square inside the black clip-rect outline drawn last on top.
//
// If the clip is dropped or partially dropped on iOS Metal, one or more of
// the coloured side patches becomes visible OUTSIDE the black outline. The
// colour of the leaking patch identifies which post-transform direction
// leaked. A full clip drop paints most of the cell red plus visible side
// patches; a sub-pixel bleed shows a thin coloured ring just outside the
// black outline.
public class ClipUnderScaleTranslate extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        // Light-gray cell so any leaked patch colour is unmistakable.
        g.setColor(0xeeeeee);
        g.fillRect(x, y, w, h);

        // Small centred clip rect -- 1/5 of the cell in each dimension. Plenty
        // of gray buffer around it so even a thin bleed is visible.
        int clipW = Math.max(16, w / 5);
        int clipH = Math.max(16, h / 5);
        int clipX = x + (w - clipW) / 2;
        int clipY = y + (h - clipH) / 2;

        g.pushClip();
        g.clipRect(clipX, clipY, clipW, clipH);

        // Mirror the magnifier code in issue #4907:
        //     gc.scale(scale, scale);
        //     gc.translate(-ax, -ay);
        //     <draw entire large surface>
        //     gc.translate(ax, ay);
        //     gc.scale(1/scale, 1/scale);
        float scale = 2.0f;
        int ax = clipX + clipW / 2;
        int ay = clipY + clipH / 2;
        g.scale(scale, scale);
        g.translate(-ax, -ay);

        // 5-patch test pattern, sized in source (pre-scale) coordinates.
        // Central patch is clipW/scale by clipH/scale in source space, so it
        // exactly fills the clip rect after the 2x scale. The four side
        // patches are placed clipW/scale and clipH/scale away from the centre
        // -- with the 2x scale that is one full clipW / clipH off centre, so
        // under a correct clip the side patches end up entirely outside the
        // clip rect and contribute no pixels. If the clip is dropped they
        // become visible at known cardinal positions outside the outline,
        // identifying the leak direction by colour.
        int patchW = clipW / 2;     // 1/scale * clipW
        int patchH = clipH / 2;     // 1/scale * clipH
        int halfPW = patchW / 2;
        int halfPH = patchH / 2;
        int offX = patchW;
        int offY = patchH;

        // Centre (red) -- the only patch that should be visible.
        g.setColor(0xff0000);
        g.fillRect(ax - halfPW, ay - halfPH, patchW, patchH);

        // North (green) -- one patch-height above centre.
        g.setColor(0x00aa00);
        g.fillRect(ax - halfPW, ay - halfPH - offY, patchW, patchH);

        // South (blue).
        g.setColor(0x0000ff);
        g.fillRect(ax - halfPW, ay - halfPH + offY, patchW, patchH);

        // West (orange).
        g.setColor(0xff8800);
        g.fillRect(ax - halfPW - offX, ay - halfPH, patchW, patchH);

        // East (magenta).
        g.setColor(0xff00ff);
        g.fillRect(ax - halfPW + offX, ay - halfPH, patchW, patchH);

        g.translate(ax, ay);
        g.scale(1f / scale, 1f / scale);

        g.popClip();

        // Black outline of the expected clip region drawn after popClip on
        // top of the rendered fill, so its position is unaffected by any leak
        // and the diff has a stable visual anchor.
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
