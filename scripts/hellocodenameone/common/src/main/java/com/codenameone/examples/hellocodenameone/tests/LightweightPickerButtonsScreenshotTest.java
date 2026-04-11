package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;

import java.util.Calendar;
import java.util.Date;

public class LightweightPickerButtonsScreenshotTest extends BaseTest {
    private Picker picker;

    @Override
    public boolean runTest() {
        Form form = createForm("Picker Quick Buttons", BoxLayout.y(), "LightweightPickerButtons");
        picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        picker.setDate(new Date());
        picker.addLightweightPopupButton("Today", new Runnable() {
            @Override
            public void run() {
                picker.setDate(new Date());
            }
        });
        picker.addLightweightPopupButton("+7 Days", new Runnable() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 7);
                picker.setDate(cal.getTime());
            }
        }, Picker.LightweightPopupButtonPlacement.BELOW_SPINNER);
        form.add(picker);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        picker.startEditingAsync();
        UITimer.timer(1000, false, parent, run);
    }
}
