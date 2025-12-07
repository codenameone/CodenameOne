package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Button;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateFormatTest2772PortTest extends UITestBase {

    @FormTest
    void tappingShowDateFormatsUsingLocalizationManager() {
        Date fixedDate = new Date(1700000000000L);
        RecordingL10NManager localizationManager = new RecordingL10NManager();
        implementation.setLocalizationManager(localizationManager);

        Form form = new Form("Test Date Format", BoxLayout.y());
        Label result = new Label();
        Button showDate = new Button("Show Date");
        showDate.addActionListener(evt -> {
            result.setText(L10NManager.getInstance().formatDateLongStyle(fixedDate));
            form.revalidateWithAnimationSafety();
        });
        form.add(result);
        form.add(showDate);
        form.show();
        ensureSized(showDate, form);

        implementation.tapComponent(showDate);
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();

        assertEquals("Formatted: 1700000000000", result.getText());
        assertNotNull(localizationManager.getLastFormattedDate());
        assertEquals(fixedDate.getTime(), localizationManager.getLastFormattedDate().getTime());
    }

    private void ensureSized(Button button, Form form) {
        for (int i = 0; i < 5 && (button.getWidth() <= 0 || button.getHeight() <= 0); i++) {
            form.revalidate();
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();
        }
    }

    private static class RecordingL10NManager extends L10NManager {
        private Date lastFormattedDate;

        RecordingL10NManager() {
            super("en", "US");
        }

        Date getLastFormattedDate() {
            return lastFormattedDate;
        }

        @Override
        public String formatDateLongStyle(Date date) {
            lastFormattedDate = date;
            if (date == null) {
                return "";
            }
            return "Formatted: " + date.getTime();
        }
    }
}
