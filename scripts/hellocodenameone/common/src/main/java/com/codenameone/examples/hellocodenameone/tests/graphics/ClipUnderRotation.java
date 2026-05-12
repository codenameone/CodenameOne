package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

// Targeted test for the rotated-clip rasterisation path raised in issue
// #3921 ("Clipping region not respected with non-90 degree rotations").
// Stresses the same code path the original report cared about, but with
// pushClip/popClip only -- no getClip/setClip(int[]) -- so it isolates the
// rasterisation path from the well-known fact that getClip()/setClip(int[])
// can't preserve a non-axis-aligned clip shape (which is what ddyer0's own
// follow-up comment on the issue identified as the cause of his repro).
//
// Sequence:
//   pushClip
//   clipRect(cell)                                  // outer axis-aligned clip
//   rotateRadians(30deg, pivot=inner-rect-centre)
//   clipRect(inner)                                 // intersect in rotated space
//                                                   //  -> screen clip is now
//                                                   //  the intersection of an
//                                                   //  axis-aligned rect and a
//                                                   //  rotated rect (a polygon
//                                                   //  on screen)
//   fillRect(big red)
//   rotateRadians(-30deg, pivot)
//   popClip
//
// Correct render: the red fill appears as a 30deg-tilted rect centred on the
// inner-rect outline -- the clip shape under rotation is honoured.
// Bug A (clip widened to axis-aligned bbox): the red fill is an axis-aligned
// rect larger than (and aligned with) the navy reference outline -- the bbox
// of the rotated rect, not the rotated rect itself.
// Bug B (polygon clip dropped entirely): the red fill covers the whole cell
// -- the rasteriser saw an unknown clip shape and disabled clipping. This is
// the suspected iOS Metal behaviour: ClipRect.m's polygon initialiser stores
// x=y=w=h=-1, and the Metal execute path then calls
// CN1MetalSetScissor(0, 0, -2, -2), whose `width<=0 || height<=0` branch
// sets the scissor to the full framebuffer.
//
// The navy reference outline is the pre-rotation inner rect; a correct
// render shows a tilted-rect red fill that overhangs the navy outline at
// the two diagonal corners and falls short of it at the two opposite
// corners, while a Bug A render exactly matches the outline, and a Bug B
// render swamps the outline entirely.
public class ClipUnderRotation extends AbstractGraphicsScreenshotTest {

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int x = bounds.getX();
        int y = bounds.getY();
        int w = bounds.getWidth();
        int h = bounds.getHeight();

        g.setColor(0xeeeeee);
        g.fillRect(x, y, w, h);

        if (!Transform.isSupported()) {
            g.setColor(0);
            g.drawString("Affine unsupported", x + 4, y + 4);
            return;
        }

        // Inner rect: 1/3 of the cell, centred. Big enough that the
        // 30deg-rotated version still fits comfortably inside the cell;
        // small enough that there is plenty of gray buffer around it so a
        // Bug B render (whole-cell red) is unmistakable.
        int innerW = Math.max(40, w / 3);
        int innerH = Math.max(40, h / 3);
        int innerX = x + (w - innerW) / 2;
        int innerY = y + (h - innerH) / 2;
        int pivotX = innerX + innerW / 2;
        int pivotY = innerY + innerH / 2;

        g.pushClip();
        // Outer clip = the cell itself. This guarantees we are intersecting
        // a known rect with the rotated inner rect inside the transform
        // block, which is what forces the framework into the polygon-clip
        // branch (clipRect-under-non-identity-transform path in
        // IOSImplementation.NativeGraphics.clipRect line 4670).
        g.clipRect(x, y, w, h);

        float angle = (float)(Math.PI / 6); // 30deg
        g.rotateRadians(angle, pivotX, pivotY);
        // Intersect with the inner rect. In screen pixels this produces a
        // rotated rectangular clip shape (a parallelogram-shaped polygon).
        g.clipRect(innerX, innerY, innerW, innerH);

        // Big red fill: a rect much larger than the cell, expressed in the
        // rotated coordinate system. The rasteriser is responsible for
        // honouring the polygon clip and rendering only the rotated-rect
        // intersection.
        g.setColor(0xff0000);
        g.fillRect(x - w, y - h, w * 4, h * 4);

        // Restore the transform before popClip so the popped state is the
        // identity transform we started with.
        g.rotateRadians(-angle, pivotX, pivotY);
        g.popClip();

        // Navy axis-aligned outline of the pre-rotation inner rect. Drawn
        // after popClip so its position is stable. The red fill should
        // appear as a 30deg-rotated rect overlapping this outline (not
        // matching it exactly).
        g.setColor(0x000080);
        g.drawRect(innerX, innerY, innerW - 1, innerH - 1);

        // Sentinel green dot in the corner; should always render after the
        // pop / un-rotate restored the identity transform.
        g.setColor(0x008000);
        g.fillRect(x + 2, y + 2, 6, 6);
    }

    @Override
    protected String screenshotName() {
        return "graphics-clip-under-rotation";
    }
}
