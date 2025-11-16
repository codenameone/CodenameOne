package com.codename1.share;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ShareServiceIntegrationTest extends UITestBase {

    @FormTest
    void shareServiceActionUsesStoredMessageAndImage() {
        Image icon = Image.createImage(3, 3);
        RecordingShareService service = new RecordingShareService("Recorder", icon);
        service.setOriginalForm(new Form(new Label("Origin")));
        service.setMessage("hello world");
        service.setImage("file://icon.png", "image/png");

        service.actionPerformed(new ActionEvent(this));
        assertEquals("hello world", service.lastText.get());
        assertEquals("file://icon.png", service.lastImage.get());
        assertEquals("image/png", service.lastMime.get());
        assertSame(icon, service.getIcon());
    }

    @FormTest
    void finishReturnsToOriginalForm() {
        Form original = new Form(new Label("Original"));
        RecordingShareService service = new RecordingShareService("Recorder", null);
        service.setOriginalForm(original);
        Form next = new Form(new Label("Second"));
        next.show();
        service.finish();
        assertSame(original, com.codename1.ui.Display.getInstance().getCurrent());
    }

    private static class RecordingShareService extends ShareService {
        private final AtomicReference<String> lastText = new AtomicReference<String>();
        private final AtomicReference<String> lastImage = new AtomicReference<String>();
        private final AtomicReference<String> lastMime = new AtomicReference<String>();

        RecordingShareService(String name, Image icon) {
            super(name, icon);
        }

        public void share(String text) {
            lastText.set(text);
        }

        public void share(String text, String image, String imageMimeType) {
            lastText.set(text);
            lastImage.set(image);
            lastMime.set(imageMimeType);
        }

        public boolean canShareImage() {
            return true;
        }
    }
}
