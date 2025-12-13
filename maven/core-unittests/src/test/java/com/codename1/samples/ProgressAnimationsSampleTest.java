package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.CommonProgressAnimations;
import com.codename1.ui.TextArea;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.util.UITimer;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static com.codename1.ui.CN.*;
import static org.junit.jupiter.api.Assertions.*;

public class ProgressAnimationsSampleTest extends UITestBase {

    @FormTest
    public void testProgressAnimations() {
        Form f = new Form("Hi World", BoxLayout.y());
        f.add(new Label("Hi World"));
        f.add(new CommonProgressAnimations.CircleProgress());
        f.add(new CommonProgressAnimations.LoadingTextAnimation());

        Label labelThatIsLoading = new Label("Loading...");

        f.add(labelThatIsLoading);
        CommonProgressAnimations.CircleProgress.markComponentLoading(labelThatIsLoading);

        // Simulate timer 1 completion immediately
        labelThatIsLoading.setText("Found 248 results");
        CommonProgressAnimations.CircleProgress.markComponentReady(labelThatIsLoading);

        Label anotherLabelThatIsLoading = new Label("Loading...");

        f.add(anotherLabelThatIsLoading);
        CommonProgressAnimations.CircleProgress.markComponentLoading(anotherLabelThatIsLoading);

        // Simulate timer 2 completion immediately
        labelThatIsLoading.setText("Found 512 results");
        CommonProgressAnimations.CircleProgress.markComponentReady(anotherLabelThatIsLoading, CommonTransitions.createFade(300));

        TextArea someText = new TextArea("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        someText.setGrowByContent(true);
        someText.setRows(4);
        someText.setColumns(40);
        someText.setPreferredW(Display.getInstance().getDisplayWidth());
        f.add(someText);
        CommonProgressAnimations.LoadingTextAnimation.markComponentLoading(someText).cols(40).rows(5);

        // Simulate timer 3 completion immediately
        CommonProgressAnimations.CircleProgress.markComponentReady(someText, CommonTransitions.createFade(300));

        f.show();
        DisplayTest.flushEdt();

        // Verifications
        assertEquals("Found 512 results", labelThatIsLoading.getText());

        assertTrue(f.contains(someText));
        assertTrue(f.contains(labelThatIsLoading));
        assertTrue(f.contains(anotherLabelThatIsLoading));
    }
}
