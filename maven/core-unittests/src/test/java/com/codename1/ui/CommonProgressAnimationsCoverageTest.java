package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CommonProgressAnimations.CircleProgress;
import com.codename1.ui.CommonProgressAnimations.EmptyAnimation;
import com.codename1.ui.CommonProgressAnimations.LoadingTextAnimation;
import com.codename1.ui.CommonProgressAnimations.ProgressAnimation;
import com.codename1.ui.layouts.BorderLayout;
import static com.codename1.testing.TestUtils.*;

public class CommonProgressAnimationsCoverageTest extends UITestBase {

    @FormTest
    public void testCircleProgress() {
        CircleProgress cp = new CircleProgress();
        Form f = new Form("Test", new BorderLayout());
        f.add(BorderLayout.CENTER, cp);
        f.show();

        cp.animate();
        // Graphics g = f.getComponentGraphics(); // Not available publically
        // We can create a fake graphics and call paint
        Image img = Image.createImage(100, 100);
        Graphics g = img.getGraphics();
        cp.paint(g);

        Label l = new Label("Loading...");
        f.add(BorderLayout.SOUTH, l);

        ProgressAnimation pa = CircleProgress.markComponentLoading(l);
        ProgressAnimation.markComponentReady(l);

        // Coverage for outer class constructor
        new CommonProgressAnimations();
    }

    @FormTest
    public void testEmptyAnimation() {
        EmptyAnimation ea = new EmptyAnimation();
        Label l = new Label("Loading...");
        Form f = new Form("Test", new BorderLayout());
        f.add(BorderLayout.CENTER, l);
        f.show();

        EmptyAnimation.markComponentLoading(l);
        EmptyAnimation.markComponentReady(l);
    }

    @FormTest
    public void testLoadingTextAnimation() {
        LoadingTextAnimation lta = new LoadingTextAnimation();
        lta.rows(5).cols(20);
        assertTrue(lta.rows() == 5);
        assertTrue(lta.cols() == 20);

        lta.animate();

        Label l = new Label("Text...");
        Form f = new Form("Test", new BorderLayout());
        f.add(BorderLayout.CENTER, l);
        f.show();

        LoadingTextAnimation.markComponentLoading(l);
        LoadingTextAnimation.markComponentReady(l);
    }
}
