package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;

/**
 * Screenshot coverage for the {@code com.codename1.appreview} fallback rating
 * widget (the sheet {@code AppReview} shows when no native review prompt is
 * available). As with {@link DialogThemeScreenshotTest}, the widget body is
 * rendered inline as a styled container -- using the same star icons and the
 * same single-row {@link GridLayout} as the real {@code RatingDialog} -- rather
 * than via a {@code Sheet.show()}, so the screenshot captures the layout
 * deterministically without waiting for the sheet animation to settle. The grid
 * is what keeps the five stars on one row on narrow screens (a flow layout would
 * wrap them).
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

        Container stars = new Container(new GridLayout(1, MAX_STARS));
        for (int i = 0; i < MAX_STARS; i++) {
            Button star = new Button();
            star.setUIID("Label");
            FontImage.setMaterialIcon(star, FontImage.MATERIAL_STAR_BORDER, 5);
            stars.add(star);
        }
        body.add(stars);

        Button never = new Button("Don't ask again");
        never.setUIID("DialogCommandText");
        body.add(never);

        dialog.add(BorderLayout.CENTER, body);
        form.add(BorderLayout.CENTER, dialog);
        form.show();
        return true;
    }
}
