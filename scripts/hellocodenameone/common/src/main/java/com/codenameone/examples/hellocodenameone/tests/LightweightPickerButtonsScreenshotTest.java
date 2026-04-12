package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class LightweightPickerButtonsScreenshotTest extends BaseTest {
    private Picker picker;
    private Date fixedDate;
    private Date fixedDatePlus7;

    @Override
    public boolean runTest() {
        Form form = createForm("Picker Quick Buttons", BoxLayout.y(), "LightweightPickerButtons");
        fixedDate = toDate(LocalDate.of(2026, 4, 11));
        fixedDatePlus7 = toDate(LocalDate.of(2026, 4, 18));
        picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        picker.setDate(fixedDate);
        picker.addLightweightPopupButton("Today", new Runnable() {
            @Override
            public void run() {
                picker.setDate(fixedDate);
            }
        });
        picker.addLightweightPopupButton("+7 Days", new Runnable() {
            @Override
            public void run() {
                picker.setDate(fixedDatePlus7);
            }
        }, Picker.LightweightPopupButtonPlacement.BELOW_SPINNER);
        form.add(picker);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        picker.setDate(fixedDate);
        picker.startEditingAsync();
        UITimer.timer(1000, false, parent, run);
    }

    private static Date toDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
