package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.AsyncResource;
import com.codename1.ui.CommonProgressAnimations.CircleProgress;

import static org.junit.jupiter.api.Assertions.*;

class CircleProgressSampleTest extends UITestBase {

    private static final int DISPLAY_WIDTH = 1080;
    private static final int DISPLAY_HEIGHT = 1920;

    @FormTest
    void circleProgressIndicatorIsReplacedWhenDataIsReady() {
        implementation.setDisplaySize(DISPLAY_WIDTH, DISPLAY_HEIGHT);

        final AsyncResource<MyData> dataResource = new AsyncResource<MyData>();
        final Form[] detailsHolder = new Form[1];
        final Label[] nameHolder = new Label[1];
        final CircleProgress[] progressHolder = new CircleProgress[1];

        Form launcher = new Form("Hi World", BoxLayout.y());
        Button showDetails = new Button("Show Details");
        showDetails.addActionListener(evt -> showCircleProgressForm(dataResource, detailsHolder, nameHolder, progressHolder));
        launcher.add(showDetails);
        launcher.show();
        launcher.revalidate();
        flushSerialCalls();

        ensureSized(showDetails, launcher);
        tapComponent(showDetails);
        flushSerialCalls();
        DisplayTest.flushEdt();

        assertNotNull(detailsHolder[0]);
        assertSame(detailsHolder[0], Display.getInstance().getCurrent());

        assertNotNull(progressHolder[0]);
        assertSame(progressHolder[0], detailsHolder[0].getContentPane().getComponentAt(0));

        dataResource.complete(new MyData());
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();

        assertEquals("Steve", nameHolder[0].getText());
        assertSame(nameHolder[0], detailsHolder[0].getContentPane().getComponentAt(0));
    }

    private void showCircleProgressForm(AsyncResource<MyData> resource, Form[] detailsHolder, Label[] nameHolder, CircleProgress[] progressHolder) {
        Form previous = Display.getInstance().getCurrent();
        Form details = new Form("Hello", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
        Toolbar toolbar = new Toolbar();
        details.setToolbar(toolbar);
        toolbar.addCommandToLeftBar("Back", null, evt -> previous.showBack());

        Label nameLabel = new Label("placeholder");
        nameHolder[0] = nameLabel;
        details.add(BorderLayout.CENTER, nameLabel);

        CircleProgress progress = CircleProgress.markComponentLoading(nameLabel);
        progress.getStyle().setFgColor(0xff0000);
        progressHolder[0] = progress;

        resource.ready(data -> {
            nameLabel.setText(data.getName());
            CircleProgress.markComponentReady(nameLabel, null);
        });

        detailsHolder[0] = details;
        details.show();
    }

    private void ensureSized(Component component, Form form) {
        for (int i = 0; i < 5 && (component.getWidth() <= 0 || component.getHeight() <= 0); i++) {
            form.revalidate();
            flushSerialCalls();
        }
    }

    private static class MyData {
        String getName() {
            return "Steve";
        }
    }
}
