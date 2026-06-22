package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

/**
 * Screenshot coverage for the {@code com.codename1.appreview} fallback rating
 * widget -- the bottom {@link Sheet} that {@code AppReview} shows when the
 * platform has no native review prompt.
 *
 * <p>The native store review widgets ({@code SKStoreReviewController} on iOS,
 * the Play In-App Review API on Android) are OS-drawn, throttled overlays that
 * live outside Codename One's {@code Display.screenshot()} pipeline, so they
 * can't be captured here. This test therefore exercises the Codename One drawn
 * fallback sheet, which is what renders on the simulator, desktop and the web
 * target. It builds the same sheet content as the real {@code RatingDialog}
 * (single-row {@link GridLayout} star strip so the stars never wrap) and shows
 * a genuine {@code Sheet.show()} -- following the {@link SheetScreenshotTest}
 * pattern -- so the capture includes the actual sheet chrome.</p>
 */
public class AppReviewDialogScreenshotTest extends BaseTest {
    private static final int MAX_STARS = 5;

    private Sheet sheet;

    @Override
    public boolean runTest() {
        Form form = createForm("App Review", new BorderLayout(), "AppReviewDialog");
        form.add(BorderLayout.CENTER, new Label("Rating sheet"));
        sheet = buildRatingSheet();
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        sheet.show();
        UITimer.timer(1500, false, parent, run);
    }

    private Sheet buildRatingSheet() {
        Sheet ratingSheet = new Sheet(null, "Enjoying HelloCodenameOne?");
        Container content = ratingSheet.getContentPane();
        content.setLayout(BoxLayout.y());

        SpanLabel prompt = new SpanLabel("Tap a star to rate your experience.");
        prompt.setUIID("DialogBody");
        content.add(prompt);

        Container stars = new Container(BoxLayout.x());
        for (int i = 0; i < MAX_STARS; i++) {
            Button star = new Button();
            star.setUIID("Label");
            FontImage.setMaterialIcon(star, FontImage.MATERIAL_STAR_BORDER, 5);
            stars.add(star);
        }
        content.add(stars);

        Button never = new Button("Don't ask again");
        never.setUIID("DialogCommandText");
        content.add(never);
        return ratingSheet;
    }
}
