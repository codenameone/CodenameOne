package com.codenameone.developerguide;

import com.codenameone.developerguide.animations.AnimationManagerDemo;
import com.codenameone.developerguide.animations.AnimationSynchronicityDemo;
import com.codenameone.developerguide.animations.BubbleTransitionDemo;
import com.codenameone.developerguide.animations.HiddenComponentDemo;
import com.codenameone.developerguide.animations.HierarchyAnimationDemo;
import com.codenameone.developerguide.animations.LayoutAnimationsDemo;
import com.codenameone.developerguide.animations.LowLevelAnimationDemo;
import com.codenameone.developerguide.animations.MorphTransitionDemo;
import com.codenameone.developerguide.animations.ReplaceTransitionDemo;
import com.codenameone.developerguide.animations.SlideTransitionsDemo;
import com.codenameone.developerguide.animations.SwipeBackSupportDemo;
import com.codenameone.developerguide.animations.UnlayoutAnimationsDemo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Registry of available demos so that the browser can enumerate them.
 */
public final class DemoRegistry {
    private static final List<Demo> DEMOS = Collections.unmodifiableList(
            Arrays.asList(
                    new LayoutAnimationsDemo(),
                    new UnlayoutAnimationsDemo(),
                    new HiddenComponentDemo(),
                    new AnimationSynchronicityDemo(),
                    new HierarchyAnimationDemo(),
                    new AnimationManagerDemo(),
                    new LowLevelAnimationDemo(),
                    new ReplaceTransitionDemo(),
                    new SlideTransitionsDemo(),
                    new BubbleTransitionDemo(),
                    new MorphTransitionDemo(),
                    new SwipeBackSupportDemo()
            )
    );
    private static final List<GuideScreenshot> SCREENSHOTS = Collections.unmodifiableList(
            Arrays.asList(
                    screenshot("layout-animations", "Layout Animations", "layout-animation-1.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-2.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-3.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-4.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-5.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-6.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-7.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-slide.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-slide-vertical.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-slide-fade.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-cover.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-uncover.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-fade.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-flip.png"),
                    screenshot("bubble-transition", "Bubble Transition", "transition-bubble.png"),
                    screenshot("morph-transition", "Morph Transition", "mighty-morphing-components-1.png")
            )
    );

    private DemoRegistry() {
        // utility class
    }

    public static List<Demo> getDemos() {
        return DEMOS;
    }

    public static List<GuideScreenshot> getScreenshots() {
        return SCREENSHOTS;
    }

    private static GuideScreenshot screenshot(String id, String demoTitle, String fileName) {
        for (Demo demo : DEMOS) {
            if (demo.getTitle().equals(demoTitle)) {
                return new GuideScreenshot(id, demo, fileName);
            }
        }
        throw new IllegalStateException("No guide demo registered with title: " + demoTitle);
    }
}
