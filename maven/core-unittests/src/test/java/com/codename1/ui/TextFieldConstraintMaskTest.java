package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A modifier bit must not turn a typed constraint back into "anything goes".
 */
class TextFieldConstraintMaskTest extends UITestBase {

    @FormTest
    void aHintBesideNumericStillRestrictsToDigits() {
        // the documented way to mark a code field is NUMERIC | ONE_TIME_CODE, and the
        // lightweight editing path compared the whole constraint, so the hint beside the
        // base type turned a digits-only field into one that took letters
        TextField f = new TextField();
        f.setConstraint(TextArea.NUMERIC | TextArea.ONE_TIME_CODE);
        assertTrue(f.validChar("7"));
        assertFalse(f.validChar("a"));
    }

    @FormTest
    void everyOtherModifierBehavesTheSameWay() {
        TextField f = new TextField();
        f.setConstraint(TextArea.NUMERIC | TextArea.SENSITIVE);
        assertFalse(f.validChar("a"));
        f.setConstraint(TextArea.PHONENUMBER | TextArea.NON_PREDICTIVE);
        assertTrue(f.validChar("+"));
        assertFalse(f.validChar("a"));
    }

    @FormTest
    void aPlainConstraintIsUnaffected() {
        TextField f = new TextField();
        f.setConstraint(TextArea.ANY);
        assertTrue(f.validChar("a"));
        f.setConstraint(TextArea.NUMERIC);
        assertFalse(f.validChar("a"));
    }
}
