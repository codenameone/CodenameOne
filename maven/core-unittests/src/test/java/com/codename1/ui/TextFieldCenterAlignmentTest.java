package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for
 * https://github.com/codenameone/CodenameOne/issues/1511
 * -- the 2015 reporter could not centre-align a TextField with a NUMERIC
 * constraint. TextField.setAlignment(CENTER) used to throw
 * IllegalArgumentException; this verifies the API now accepts CENTER and the
 * underlying Style is updated, matching TextArea behaviour.
 */
class TextFieldCenterAlignmentTest extends UITestBase {

    @FormTest
    void setAlignmentCenterIsAcceptedOnTextField() {
        TextField tf = new TextField();
        tf.setConstraint(TextField.NUMERIC);
        // Pre-fix this call threw "CENTER alignment is not supported in
        // TextField." See #1511.
        assertDoesNotThrow(() -> tf.setAlignment(Component.CENTER));

        // The proxy returned by getAllStyles() doesn't track its own
        // alignment, so read the real underlying styles.
        assertEquals(Component.CENTER, tf.getUnselectedStyle().getAlignment(),
                "Unselected style alignment must reflect the setAlignment(CENTER) call.");
        assertEquals(Component.CENTER, tf.getSelectedStyle().getAlignment(),
                "Selected style alignment must reflect the setAlignment(CENTER) call.");
        // Constraint should be unaffected by the alignment change.
        assertEquals(TextField.NUMERIC, tf.getConstraint());
    }

    @FormTest
    void setAlignmentLeftRightAndCenterAllWork() {
        TextField tf = new TextField();

        tf.setAlignment(Component.LEFT);
        assertEquals(Component.LEFT, tf.getUnselectedStyle().getAlignment());

        tf.setAlignment(Component.CENTER);
        assertEquals(Component.CENTER, tf.getUnselectedStyle().getAlignment());

        tf.setAlignment(Component.RIGHT);
        assertEquals(Component.RIGHT, tf.getUnselectedStyle().getAlignment());
    }
}
