package com.codename1.ui.validation;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for
 * https://github.com/codenameone/CodenameOne/issues/1459
 *
 * The 2015 reporter said that when a user typed invalid content into a
 * validator-bound TextField and then moved focus by tapping into another
 * field (rather than the VKB 'next' / Enter button), the previous field was
 * not highlighted as invalid until the user returned to it. The
 * action-listener path the Validator wires up only fires on VKB Enter /
 * action; tapping a different field never went through that path.
 *
 * The fix registers an unconditional focus listener that re-validates on
 * focus loss, so the highlight gets applied as soon as focus moves anywhere
 * else.
 */
class ValidatorFocusLossHighlightTest extends UITestBase {

    /// A constraint that simply requires non-empty text.
    private static class NonEmptyConstraint implements Constraint {
        @Override
        public boolean isValid(Object value) {
            return value != null && value.toString().length() > 0;
        }

        @Override
        public String getDefaultFailMessage() {
            return "must not be empty";
        }
    }

    @FormTest
    void focusLossOnAnotherFieldFlagsTheLeftFieldAsInvalid() {
        Form f = Display.getInstance().getCurrent();
        f.setLayout(BoxLayout.y());
        TextField first = new TextField("good", "first");
        TextField second = new TextField("", "second");
        f.add(first).add(second);
        f.revalidate();

        Validator v = new Validator();
        v.addConstraint(first, new NonEmptyConstraint());

        // Initially the field is valid (text is "good").
        assertTrue(v.isValid(first));

        // Simulate the failure case: clear the field to an invalid value.
        first.setText("");
        first.requestFocus();
        assertTrue(first.hasFocus());

        // User taps into the second field. Before the fix the validator's
        // focusLost handler was empty and the action-listener path only
        // fired on VKB Enter, so validate(first) was never called and
        // first stayed marked valid until it regained focus.
        second.requestFocus();
        assertFalse(first.hasFocus());

        assertFalse(v.isValid(first),
                "First field should now be marked invalid because focus left "
                        + "it without correcting the value. See #1459.");
    }

    @FormTest
    void focusLossDoesNotFalselyInvalidateAValidField() {
        Form f = Display.getInstance().getCurrent();
        f.setLayout(BoxLayout.y());
        TextField first = new TextField("", "first");
        TextField second = new TextField("", "second");
        f.add(first).add(second);
        f.revalidate();

        Validator v = new Validator();
        v.addConstraint(first, new NonEmptyConstraint());

        first.setText("ok");
        first.requestFocus();
        second.requestFocus();

        assertTrue(v.isValid(first),
                "A field with valid content must stay valid after focus loss.");
    }
}
