package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Repro for issue #4907 (glitchy metal drawing): a magnifier-style render path
// sets a small clip rect, then applies g.scale() + g.translate() to draw a
// scaled and panned copy of a much larger surface into that clip. Under the
// pre-metal iOS pipeline (and on JavaSE/Android) the large fillRect emitted
// after the transform is correctly clipped to the inner rect; under metal the
// clip is reportedly not respected and the red fill leaks outside the inner
// rect.
//
// Each cell is gray with a black frame and a small centred inner rect that is
// the only region the red fill should reach. If the clip is honoured the cell
// shows a gray cell with a centred red square; if the clip is dropped the
// entire cell turns red (or shows red bleed outside the centred square).
public class ClipUnderScaleTranslate extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        // Cell background + frame so a fully-red cell (clip dropped) is
        // visually unambiguous and a centred red square (clip honoured) has a
        // reference frame to compare against.
        g.setColor(0xcccccc);
        g.fillRect(x, y, w, h);
        g.setColor(0x000000);
        g.drawRect(x, y, w - 1, h - 1);

        // Centred inner rect roughly 1/3 of the cell in each dimension --
        // small enough that a leak outside it is obvious, large enough to
        // dominate the cell when correctly filled.
        int clipW = Math.max(8, w / 3);
        int clipH = Math.max(8, h / 3);
        int clipX = x + (w - clipW) / 2;
        int clipY = y + (h - clipH) / 2;

        // Reference outline of the expected clip region. Drawn before the
        // clip + transform so it survives regardless of how the clipped fill
        // behaves, giving the diff a stable anchor.
        g.setColor(0x000080);
        g.drawRect(clipX, clipY, clipW - 1, clipH - 1);

        g.pushClip();
        g.clipRect(clipX, clipY, clipW, clipH);

        // Mirror the magnifier code in issue #4907:
        //     gc.scale(scale, scale);
        //     gc.translate(-ax, -ay);
        //     <draw entire large surface>
        //     gc.translate(ax, ay);
        //     gc.scale(1/scale, 1/scale);
        // The clip was set in pre-transform coordinates; after scale+translate
        // a fillRect over the full untransformed cell should still land only
        // inside the original clipRect.
        float scale = 2.0f;
        int ax = clipX + clipW / 2;
        int ay = clipY + clipH / 2;
        g.scale(scale, scale);
        g.translate(-ax, -ay);

        // Massive fill: cover the whole cell and then some, in the
        // post-transform coordinate space. If the clip was honoured this
        // paints only the inner clip rect red; if it was dropped this paints
        // the entire cell red.
        g.setColor(0xff0000);
        g.fillRect(x - w, y - h, w * 4, h * 4);

        g.translate(ax, ay);
        g.scale(1f / scale, 1f / scale);

        g.popClip();

        // Sentinel outside the clip rect drawn after popClip with the
        // identity transform. Should always render -- if it doesn't, the
        // pop/transform-reset path is also broken.
        g.setColor(0x008000);
        g.fillRect(x + 2, y + 2, 6, 6);
    }

    @Override
    protected String screenshotName() {
        return "graphics-clip-under-scale-translate";
    }
}
