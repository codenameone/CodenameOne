package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.ToastBar;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

/**
 * Screenshot test for ToastBar positioned at {@link Component#TOP}.
 *
 * <p>This verifies the fix for the issue where {@code ToastBar} with
 * {@code setPosition(Component.TOP)} rendered spurious empty space above
 * the message text because the safe-area inset was double-counted when
 * the layered-pane parent was already below the safe-area boundary.</p>
 */
public class ToastBarTopPositionScreenshotTest extends BaseTest {
    private Form form;
    private int originalPosition;

    @Override
    public boolean runTest() {
        originalPosition = ToastBar.getInstance().getPosition();

        form = createForm("ToastBar Top", new BorderLayout(), "ToastBarTopPosition");

        Container content = new Container(BoxLayout.y());
        content.add(new Label("ToastBar at TOP position"));
        content.add(new Label("No empty space should appear above the toast"));
        form.add(BorderLayout.CENTER, content);

        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(final Form parent, final Runnable run) {
        ToastBar tb = ToastBar.getInstance();
        tb.setPosition(Component.TOP);

        // Use a long timeout so the toast stays visible for the screenshot
        System.out.println("[ToastBarTop] showMessage at " + System.currentTimeMillis());
        ToastBar.showMessage("Info message at top", FontImage.MATERIAL_INFO, 30000);

        // A fixed 2s sleep raced the toast's slide-in on slow CI targets (the
        // watch simulator lost it every run, tvOS intermittently). Poll for
        // the toast component being visible AND then hold for a settle window
        // that clears its slide animation: show() runs slideUpAndWait(2) +
        // slideDownAndWait(800), so the ToastBarComponent reports visible with
        // full bounds ~400ms in while the animation is still compositing it
        // into view -- capturing then snapshots a half-slid (or absent) toast
        // (seen on tvOS). Require SETTLE_MS of continuous visibility past the
        // ~802ms animation before capturing. Cap the total wait so a genuinely
        // broken toast still yields a (failing) screenshot with the dump below.
        final int SETTLE_MS = 1400;
        final UITimer[] timerRef = new UITimer[1];
        timerRef[0] = UITimer.timer(200, true, parent, new Runnable() {
            private int waited;
            private int shownFor;
            private boolean fired;
            public void run() {
                if (fired) {
                    return;
                }
                waited += 200;
                boolean shown = toastVisible(parent);
                shownFor = shown ? shownFor + 200 : 0;
                if ((shown && shownFor >= SETTLE_MS) || waited >= 15000) {
                    fired = true;
                    if (timerRef[0] != null) {
                        timerRef[0].cancel();
                    }
                    Container base = parent.getLayeredPane();
                    Container wrapper = base.getParent() != null ? base.getParent() : base;
                    StringBuilder sb = new StringBuilder("[ToastBarTop] capture at ")
                            .append(System.currentTimeMillis())
                            .append(" waited=").append(waited);
                    dump(wrapper, sb, 0);
                    System.out.println(sb.toString());
                    run.run();
                }
            }
        });
    }

    /** True when a visible, non-empty component sits in a ToastBar layer. */
    private static boolean toastVisible(Form parent) {
        Container base = parent.getLayeredPane();
        Container wrapper = base.getParent() != null ? base.getParent() : base;
        return findVisibleToast(wrapper, 0);
    }

    private static boolean findVisibleToast(Component c, int depth) {
        if (depth <= 2 && c.getClass().getName().contains("ToastBar")
                && c.isVisible() && c.getHeight() > 0) {
            return true;
        }
        if (depth < 2 && c instanceof Container) {
            Container ct = (Container) c;
            for (int i = 0; i < ct.getComponentCount(); i++) {
                if (findVisibleToast(ct.getComponentAt(i), depth + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void dump(Component c, StringBuilder sb, int depth) {
        if (depth > 2) {
            return;
        }
        sb.append(" d").append(depth).append(':')
          .append(c.getClass().getName())
          .append(" visible=").append(c.isVisible())
          .append(" bounds=").append(c.getX()).append(',').append(c.getY())
          .append(',').append(c.getWidth()).append('x').append(c.getHeight());
        if (c instanceof Container) {
            Container ct = (Container) c;
            for (int i = 0; i < ct.getComponentCount(); i++) {
                dump(ct.getComponentAt(i), sb, depth + 1);
            }
        }
    }
}
