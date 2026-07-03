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
        // Show the toast INSTANTLY (no slide). The slide animation's completion
        // is not guaranteed on every backend -- the Metal and tvOS offscreen
        // pipelines left the ToastBarComponent stuck at height 0, so time-based
        // capture snapshotted an absent toast and the screenshot flaked. The
        // instant path lays the toast out synchronously via revalidate, so it is
        // deterministically present the moment showMessage returns, on every
        // platform. This still validates what the test exists for: the TOP
        // position leaves no empty band above the toast.
        tb.setAnimated(false);

        // Long timeout so the toast stays up well past the capture.
        ToastBar.showMessage("Info message at top", FontImage.MATERIAL_INFO, 30000);

        // Poll briefly for the toast to be laid out + painted (a couple of frames),
        // then capture. With animation off this settles almost immediately; the
        // cap only guards a genuinely broken toast so a (failing) screenshot with
        // the diagnostic dump is still produced.
        final UITimer[] timerRef = new UITimer[1];
        timerRef[0] = UITimer.timer(100, true, parent, new Runnable() {
            private int waited;
            private int shownFor;
            private boolean fired;
            public void run() {
                if (fired) {
                    return;
                }
                waited += 100;
                boolean shown = toastVisible(parent);
                shownFor = shown ? shownFor + 100 : 0;
                if ((shown && shownFor >= 300) || waited >= 5000) {
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
