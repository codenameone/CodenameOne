package com.codename1.ui.validation;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.SpinnerNumberModel;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorIntegrationTest extends UITestBase {

    @FormTest
    void validatorAppliesGlobalAndPerFieldConstraintsOnSubmit() {
        TextField username = new TextField();
        username.setName("username");
        TextField password = new TextField();
        password.setName("password");
        Button submit = new Button("Submit");

        Validator validator = new Validator();
        validator.addConstraint(username, new RegexConstraint("[a-z]{3,}", "lower"));
        validator.addConstraint(password, new LengthConstraint(4));
        validator.addSubmitButtons(submit);
        validator.addGlobalConstraint(new LengthConstraint(2));

        Form form = new Form(BoxLayout.y());
        form.add(username);
        form.add(password);
        form.add(submit);
        form.show();

        username.setText("ab");
        password.setText("123");
        assertFalse(validator.isValid());

        username.setText("abcdef");
        password.setText("1234");
        assertTrue(validator.isValid());

        submit.fireActionEvent();
        assertTrue(validator.isValid());
    }

    @FormTest
    void validatorConstraintListenersReactToModelChanges() {
        TextField code = new TextField();
        code.setName("code");
        SpinnerNumberModel model = new SpinnerNumberModel(1, 0, 10, 1);
        Validator validator = new Validator();
        final AtomicInteger invoked = new AtomicInteger();
        validator.addConstraint(code, new LengthConstraint(1));
        validator.addConstraintListener(new ConstraintListener() {
            public void onConstraintChange(Constraint src, boolean valid) {
                invoked.incrementAndGet();
            }
        });

        Form form = new Form(BoxLayout.y());
        form.add(code);
        form.show();

        code.setText("");
        model.setValue(5);
        code.setText("7");
        assertTrue(invoked.get() >= 2);
    }

    @FormTest
    void validatorHighlightModesCanBeCustomized() {
        Validator.setDefaultValidationFailureHighlightMode(Validator.HighlightMode.EMBLEM);
        Validator.setDefaultValidationFailedEmblemPosition(0.2f, 0.8f);
        Validator.setDefaultValidateOnKeyRelease(true);

        Validator validator = new Validator();
        validator.setValidationFailureHighlightMode(Validator.HighlightMode.UIID);
        validator.setValidationFailedEmblemPosition(0.1f, 0.2f);
        validator.setErrorMessageUIID("ErrorLabel");
        TextField field = new TextField();
        validator.addConstraint(field, new RegexConstraint("[0-9]+", "numbers"));
        validator.setShowErrorMessageForFocusedComponent(true);

        field.setText("abc");
        assertFalse(validator.isValid());
        field.setText("123");
        assertTrue(validator.isValid());
    }
}
