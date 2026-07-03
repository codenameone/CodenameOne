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

        // Wait for the toast animation to complete before taking the screenshot
        UITimer.timer(2000, false, parent, new Runnable() {
            public void run() {
                // Diagnostic dump of the layered-pane tree at capture time: on
                // some CI environments the toast has not appeared 2s after
                // showMessage, and this is the only evidence of its state.
                // ToastBar sits in a class-specific sub-layer, so dump the
                // wrapper (the parent of the default layer) two levels deep.
                Container base = parent.getLayeredPane();
                Container wrapper = base.getParent() != null ? base.getParent() : base;
                StringBuilder sb = new StringBuilder("[ToastBarTop] capture at ")
                        .append(System.currentTimeMillis());
                dump(wrapper, sb, 0);
                System.out.println(sb.toString());
                run.run();
            }
        });
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
