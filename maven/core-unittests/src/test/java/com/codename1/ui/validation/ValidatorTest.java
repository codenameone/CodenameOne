package com.codename1.ui.validation;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.TextField;

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
}
