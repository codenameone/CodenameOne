package com.codename1.share;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;

import static org.junit.jupiter.api.Assertions.*;

class ShareServiceTest extends UITestBase {

    @FormTest
    void actionPerformedChoosesCorrectShareMethod() {
        RecordingShare share = new RecordingShare();
        RecordingForm form = new RecordingForm();
        share.setOriginalForm(form);
        share.setMessage("hello world");

        share.actionPerformed(new ActionEvent(this));
        assertEquals("hello world", share.lastText);
        assertNull(share.lastImagePath);

        share.setImage("file://image.png", "image/png");
        share.actionPerformed(new ActionEvent(this));
        assertEquals("file://image.png", share.lastImagePath);
        assertEquals("image/png", share.lastMime);
    }

    @FormTest
    void finishShowsOriginalForm() {
        RecordingShare share = new RecordingShare();
        RecordingForm form = new RecordingForm();
        share.setOriginalForm(form);

        share.finish();
        assertTrue(form.showBackInvoked);
    }

    private static class RecordingShare extends ShareService {
        private String lastText;
        private String lastImagePath;
        private String lastMime;

        RecordingShare() {
            super("Test", (Image) null);
        }

        public void share(String text) {
            lastText = text;
            lastImagePath = null;
            lastMime = null;
        }

        public void share(String text, String image, String mimeType) {
            lastText = text;
            lastImagePath = image;
            lastMime = mimeType;
        }

        public boolean canShareImage() {
            return true;
        }
    }

    private static class RecordingForm extends Form {
        private boolean showBackInvoked;

        public void showBack() {
            showBackInvoked = true;
            super.showBack();
        }
    }
}
