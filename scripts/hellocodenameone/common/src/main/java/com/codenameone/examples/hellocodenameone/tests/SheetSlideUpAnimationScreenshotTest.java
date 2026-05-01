package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.Sheet;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Animation grid for the sheet slide-up open animation. The host container
/// uses the same `BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE` parent and
/// `BorderLayout.SOUTH` placement that `Sheet.show()` uses internally, then
/// stamps the sheet to its hidden off-screen Y before kicking off
/// `createAnimateLayout` so each captured frame walks it back to the resting
/// position.
public class SheetSlideUpAnimationScreenshotTest extends AbstractContainerAnimationScreenshotTest {
    private Sheet sheet;

    @Override
    protected Container buildContainer(int frameWidth, int frameHeight) {
        Container parent = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
        Style ps = parent.getAllStyles();
        ps.setBgColor(0xf0f4f8);
        ps.setBgTransparency(255);
        // 30% black scrim mirrors Sheet.ShowPainter so the animation reads as
        // an actual sheet over a dimmed surface, not a free-floating panel.
        parent.getStyle().setBgPainter(new DimScrimPainter());

        sheet = new Sheet(null, "Sheet Animation");
        Label icon = new Label();
        FontImage.setMaterialIcon(icon, FontImage.MATERIAL_INFO_OUTLINE, 3f);
        sheet.setTitleComponent(BoxLayout.encloseYCenter(icon, new Label("Sheet Animation")));
        Container content = sheet.getContentPane();
        content.setLayout(BoxLayout.y());
        content.add(new Label("Slide-up open animation"));
        content.add(new Button("Primary action"));
        content.add(new Label("Secondary detail"));
        parent.add(BorderLayout.SOUTH, sheet);
        return parent;
    }

    @Override
    protected ComponentAnimation startAnimation(Container container, int duration) {
        // The host has just been laid out, so the sheet sits at its resting
        // Y at the bottom of the container. Reposition it just past the
        // bottom edge so createAnimateLayout produces the slide-up motion -
        // animateLayout records the current (hidden) Y, calls layoutContainer
        // to compute the resting Y, and animates from one to the other.
        sheet.setY(container.getHeight());
        return container.createAnimateLayout(duration);
    }

    @Override
    protected String getHostTitle() {
        return "Sheet Slide Up";
    }

    @Override
    protected int getAnimationDurationMillis() {
        return 300;
    }

    private static final class DimScrimPainter implements Painter {
        @Override
        public void paint(Graphics g, Rectangle rect) {
            int alpha = g.getAlpha();
            g.setColor(0xf0f4f8);
            g.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
            g.setAlpha((int) (alpha * 0.3f));
            g.setColor(0x000000);
            g.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
            g.setAlpha(alpha);
        }
    }
}
