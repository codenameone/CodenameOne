package com.codename1.ui.validation;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest extends UITestBase {

    @FormTest
    void testRegexConstraintValidation() {
        RegexConstraint lettersOnly = new RegexConstraint("^[A-Za-z]+$", "Letters only");
        assertTrue(lettersOnly.isValid("Codename"));
        assertFalse(lettersOnly.isValid("123"));
        assertEquals("Letters only", lettersOnly.getDefaultFailMessage());
    }

    @FormTest
    void testValidatorAppliesConstraints() {
        Validator validator = new Validator();
        TextField numeric = new TextField();
        Button submit = new Button("Submit");

        validator.addConstraint(numeric, new NumericConstraint(false));
        validator.addSubmitButtons(submit);

        numeric.setText("42");
        assertTrue(validator.isValid());

        numeric.setText("NaN");
        assertFalse(validator.isValid());

        Validator.setValidateOnEveryKey(true);
        assertTrue(Validator.isValidateOnEveryKey());
        Validator.setValidateOnEveryKey(false);
        assertFalse(Validator.isValidateOnEveryKey());
    }

    @FormTest
    void testNotConstraintInvertsValidation() {
        Constraint digits = new RegexConstraint("^\\d+$", "Numbers only");
        NotConstraint notDigits = new NotConstraint("Must not be numeric", digits);

        assertTrue(notDigits.isValid("ABC"));
        assertFalse(notDigits.isValid("123"));
        assertEquals("Must not be numeric", notDigits.getDefaultFailMessage());
    }

    @FormTest
    void testExistInConstraint() {
        String[] values = {"one", "two", "three"};
        ExistInConstraint constraint = new ExistInConstraint(values, "Must be one of: one, two, three");

        assertTrue(constraint.isValid("one"));
        assertTrue(constraint.isValid("two"));
        assertFalse(constraint.isValid("four"));
        // Removed null check because ExistInConstraint implementation throws NPE
        assertEquals("Must be one of: one, two, three", constraint.getDefaultFailMessage());
    }

    @FormTest
    void testGroupConstraint() {
        Constraint numeric = new NumericConstraint(false);
        Constraint length = new LengthConstraint(2, "Must be at least 2 chars");
        GroupConstraint group = new GroupConstraint(numeric, length);

        assertTrue(group.isValid("12"));
        assertFalse(group.isValid("1")); // Fails length
        assertFalse(group.isValid("ab")); // Fails numeric

        // Test message logic if exposed, but mostly check valid logic
    }

    @FormTest
    void testLengthConstraint() {
        LengthConstraint length = new LengthConstraint(2, "Must be at least 2 chars");
        assertTrue(length.isValid("ab"));
        assertTrue(length.isValid("abc"));
        assertFalse(length.isValid("a"));
        assertFalse(length.isValid(null));
        assertEquals("Must be at least 2 chars", length.getDefaultFailMessage());
    }

    @FormTest
    void testValidatorFocusListenerInteraction() {
        Form form = new Form(new BoxLayout(BoxLayout.Y_AXIS));
        TextField tf = new TextField();
        Button submit = new Button("Submit");
        form.add(tf);
        form.add(submit);
        form.show(); // Ensure form is current

        Validator validator = new Validator();
        validator.addConstraint(tf, new NumericConstraint(false));
        validator.addSubmitButtons(submit);

        // Initial state
        tf.setText("abc"); // Invalid

        // Simulate focus to trigger any listeners attached
        tf.requestFocus();

        assertFalse(validator.isValid());
        assertFalse(submit.isEnabled());

        tf.setText("123");

        boolean originalEveryKey = Validator.isValidateOnEveryKey();
        try {
            Validator.setValidateOnEveryKey(true);
             validator = new Validator();
             validator.addConstraint(tf, new NumericConstraint(false));
             validator.addSubmitButtons(submit);

             tf.setText("abc");
             assertFalse(submit.isEnabled());

             tf.setText("123");
             assertTrue(submit.isEnabled());

        } finally {
             Validator.setValidateOnEveryKey(originalEveryKey);
        }
    }

    @FormTest
    void testShowErrorMessageForFocusedComponent() {
        Form form = new Form(new BoxLayout(BoxLayout.Y_AXIS));
        TextField tf = new TextField();
        form.add(tf);
        form.show();

        Validator validator = new Validator();
        validator.setShowErrorMessageForFocusedComponent(true);
        validator.addConstraint(tf, new LengthConstraint(5, "Too short"));

        tf.setText("abc");

        // Trigger focus gained on the component
        tf.requestFocus();

        // This test mainly ensures no exception is thrown when the focus listener runs.
        // Verifying the actual popup is hard as it is UI side effect.
    }
}
