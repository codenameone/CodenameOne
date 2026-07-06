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

    private DemoRegistry() {
        // utility class
    }

    public static List<Demo> getDemos() {
        return DEMOS;
    }
}
