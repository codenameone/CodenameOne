package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Disposes mutable images while they are still the open drawing target, then draws
/// elsewhere and lets the finalizers run.
///
/// On iOS the port kept an unretained pointer to the image being drawn into. Disposing it
/// released the native peer, and the next draw anywhere closed the drawing session and
/// stored that pointer back in the disposed image, so its finalizer released the peer a
/// second time. That is a `-[GLUIImage release]` sent to a deallocated instance from the
/// main queue, which crashed inside the signal handler and hung the app. The chart
/// animation tests dispose their scratch image exactly this way, and the suite hung a few
/// tests after them. This test does it on purpose and many times, so the crash lands here.
public class MutableImageDisposeWhileDrawingTest extends BaseTest {
    private static final int ROUNDS = 200;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        for (int i = 0; i < ROUNDS; i++) {
            Image a = Image.createImage(32, 32, 0xffffffff);
            Graphics ga = a.getGraphics();
            ga.setColor(0xff0000);
            ga.fillRect(0, 0, 16, 16);
            // still the drawing target: nothing has been drawn anywhere else since
            a.dispose();
            Image b = Image.createImage(32, 32, 0xffffffff);
            Graphics gb = b.getGraphics();
            gb.setColor(0x00ff00);
            gb.fillRect(8, 8, 16, 16);
            b.dispose();
        }
        // the finalizers of the disposed images run on the GC, and their native releases
        // run later on the main queue; give both time to happen inside this test
        System.gc();
        System.gc();
        // showing a form also paints the screen, which is what closed the session before
        final Form f = new Form("Dispose while drawing", new BorderLayout());
        f.show();
        UITimer.timer(1500, false, f, new Runnable() {
            @Override
            public void run() {
                System.gc();
                Image c = Image.createImage(32, 32, 0xffffffff);
                c.getGraphics().fillRect(0, 0, 32, 32);
                int[] rgb = c.getRGB();
                c.dispose();
                if (rgb.length != 32 * 32) {
                    fail("a mutable image created after the disposals reads back " + rgb.length + " pixels");
                    return;
                }
                UITimer.timer(1500, false, f, new Runnable() {
                    @Override
                    public void run() {
                        done();
                    }
                });
            }
        });
        return true;
    }
}
