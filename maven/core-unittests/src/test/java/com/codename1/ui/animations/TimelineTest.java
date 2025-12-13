package com.codename1.ui.animations;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import org.junit.jupiter.api.Assertions;

public class TimelineTest extends UITestBase {

    @FormTest
    public void testTimeline() {
        Image img = Image.createImage(20, 20, 0xFFFF0000);
        AnimationObject anim = AnimationObject.createAnimationImage(img, 10, 10);
        anim.defineMotionX(AnimationObject.MOTION_TYPE_LINEAR, 0, 1000, 0, 100);

        Timeline timeline = Timeline.createTimeline(1000, new AnimationObject[]{anim}, new Dimension(200, 200));

        Assertions.assertEquals(1000, timeline.getDuration());
        Assertions.assertEquals(1, timeline.getAnimationCount());
        Assertions.assertEquals(anim, timeline.getAnimation(0));

        timeline.setTime(500);
        Assertions.assertEquals(500, timeline.getTime());
        // Since setTime updates animation objects
        anim.setTime(500); // Wait, timeline.setTime should have done this but maybe only during paint or animate?
        // Checking source: setTime sets time.
        // animate() calls setTime based on system time.
        // paint calls paintScaled which calls animations[iter].setTime(time).

        // So checking anim.getX() might be stale if we didn't paint?
        // But let's check basic properties.

        Assertions.assertNotNull(timeline.getSize());

        timeline.setLoop(false);
        Assertions.assertFalse(timeline.isLoop());

        // Add another animation
        AnimationObject anim2 = AnimationObject.createAnimationImage(img, 50, 50);
        timeline.addAnimation(anim2);
        Assertions.assertEquals(2, timeline.getAnimationCount());
    }
}
