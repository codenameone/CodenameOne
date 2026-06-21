package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

/**
 * Screenshot coverage for the {@code com.codename1.appreview} fallback rating
 * widget (the dialog {@code AppReview} shows when no native review prompt is
 * available). As with {@link DialogThemeScreenshotTest}, the dialog is rendered
 * inline as a styled container -- using the same UIIDs and star icons as the
 * real {@code RatingDialog} -- rather than via a modal {@code show()}, so the
 * screenshot captures the dialog chrome deterministically without waiting for
 * modal animation to settle.
 */
public class AppReviewDialogScreenshotTest extends BaseTest {
    private static final int MAX_STARS = 5;

    @Override
    public boolean runTest() {
        Form form = createForm("App Review", new BorderLayout(), "AppReviewDialog");

        Container dialog = new Container(new BorderLayout());
        dialog.setUIID("Dialog");

        Container body = new Container(BoxLayout.y());
        body.setUIID("DialogBody");

        Label title = new Label("Enjoying HelloCodenameOne?");
        title.setUIID("DialogTitle");
        body.add(title);

        SpanLabel prompt = new SpanLabel("Tap a star to rate your experience.");
        body.add(prompt);

        Container stars = new Container(new FlowLayout(Component.CENTER));
        for (int i = 0; i < MAX_STARS; i++) {
            Button star = new Button();
            FontImage.setMaterialIcon(star, FontImage.MATERIAL_STAR_BORDER, 5);
            stars.add(star);
        }
        body.add(stars);

        Container commands = new Container(new FlowLayout(Component.CENTER));
        commands.setUIID("DialogCommandArea");
        commands.add(new Button("Not now")).add(new Button("Don't ask again"));

        dialog.add(BorderLayout.CENTER, body).add(BorderLayout.SOUTH, commands);
        form.add(BorderLayout.CENTER, dialog);
        form.show();
        return true;
    }
}
