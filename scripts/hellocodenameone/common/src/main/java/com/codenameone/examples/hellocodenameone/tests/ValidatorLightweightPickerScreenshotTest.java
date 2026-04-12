package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.PickerComponent;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.validation.LengthConstraint;
import com.codename1.ui.validation.Validator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ValidatorLightweightPickerScreenshotTest extends BaseTest {
    private Picker picker;
    private final List<TextField> fields = new ArrayList<>();
    private boolean originalValidateOnEveryKey;

    @Override
    public boolean runTest() {
        Form form = createForm("Validator + Picker", BoxLayout.y(), "ValidatorLightweightPicker");
        originalValidateOnEveryKey = Validator.isValidateOnEveryKey();
        Validator.setValidateOnEveryKey(true);

        Date fixedBirthDate = createLegacyDate(2026, 4, 11);
        PickerComponent birthDate = PickerComponent.createDate(fixedBirthDate).label("Birthdate");
        picker = birthDate.getPicker();
        picker.setUseLightweightPopup(true);
        form.add(birthDate);

        Validator validator = new Validator();
        validator.setValidationFailureHighlightMode(Validator.HighlightMode.EMBLEM);
        validator.setValidationEmblemPositionX(1f);

        for (int i = 1; i <= 12; i++) {
            TextField tf = new TextField();
            tf.setHint("Field " + i);
            form.add(tf);
            fields.add(tf);
            validator.addConstraint(tf, new LengthConstraint(5, "Must be at least 5 chars"));
        }

        form.show();
        return true;
    }

    @SuppressWarnings("deprecation")
    private static Date createLegacyDate(int year, int month, int day) {
        // Use java.util.Date(year, month, day) so conversion/rendering stays in the same legacy date pipeline
        // that Picker uses internally across ports.
        return new Date(year - 1900, month - 1, day);
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        for (TextField field : fields) {
            field.setText("x");
        }
        picker.startEditingAsync();
        UITimer.timer(1000, false, parent, run);
    }

    @Override
    public void cleanup() {
        Validator.setValidateOnEveryKey(originalValidateOnEveryKey);
        fields.clear();
        super.cleanup();
    }
}
