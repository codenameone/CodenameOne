package com.codename1.ui.spinner;

import com.codename1.components.InteractionDialog;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

/// Covers Picker.setDefaultDate / DateGetter wiring (RFE #4973).
public class PickerDefaultDateTest extends UITestBase {

    private static Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, day, 0, 0, 0);
        return c.getTime();
    }

    @Test
    public void fixedDefaultDateAppliesWhenNoSetDateCalled() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        Date fixed = date(2030, Calendar.JUNE, 15);
        p.setDefaultDate(fixed);
        Assertions.assertEquals(fixed, p.getDate(),
                "Default date should be returned when no explicit setDate has been made");
    }

    @Test
    public void dynamicDefaultDateReEvaluatesEachRead() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        final Date[] slot = new Date[] { date(2030, Calendar.JANUARY, 1) };
        p.setDefaultDate(new Picker.DateGetter() {
            @Override
            public Date get() {
                return slot[0];
            }
        });
        Assertions.assertEquals(slot[0], p.getDate(), "First read should reflect initial getter value");
        slot[0] = date(2031, Calendar.FEBRUARY, 2);
        Assertions.assertEquals(slot[0], p.getDate(),
                "Second read should reflect the getter's updated value (lazy evaluation)");
    }

    @Test
    public void explicitSetDateOverridesDefault() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        p.setDefaultDate(date(2030, Calendar.JUNE, 15));
        Date pinned = date(2028, Calendar.MARCH, 20);
        p.setDate(pinned);
        Assertions.assertEquals(pinned, p.getDate(),
                "Once a date is explicitly pinned, the default getter must not be consulted");
    }

    @Test
    public void setDateNullReEnablesDefault() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        Date defaultDate = date(2030, Calendar.JUNE, 15);
        p.setDefaultDate(defaultDate);
        p.setDate(date(2028, Calendar.MARCH, 20));
        p.setDate(null);
        Assertions.assertEquals(defaultDate, p.getDate(),
                "setDate(null) should clear the explicit pin and restore the default");
    }

    @Test
    public void clearingDefaultGetterFallsBackToValueField() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        p.setDefaultDate(date(2030, Calendar.JUNE, 15));
        p.setDefaultDate((Picker.DateGetter) null);
        Assertions.assertNull(p.getDefaultDate(),
                "getDefaultDate should return null after clearing");
        Assertions.assertNotNull(p.getDate(),
                "Clearing the default should leave the picker with a usable (non-null) date");
    }

    @Test
    public void getDefaultDateReturnsInstalledGetter() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        Picker.DateGetter getter = new Picker.DateGetter() {
            @Override
            public Date get() {
                return date(2030, Calendar.JUNE, 15);
            }
        };
        p.setDefaultDate(getter);
        Assertions.assertSame(getter, p.getDefaultDate(),
                "getDefaultDate should return the exact getter installed via setDefaultDate(DateGetter)");
    }

    @Test
    public void setTypeOffDateTypeAndBackRestoresDefault() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        Date defaultDate = date(2030, Calendar.JUNE, 15);
        p.setDefaultDate(defaultDate);
        p.setDate(date(2028, Calendar.MARCH, 20));
        p.setType(Display.PICKER_TYPE_TIME);
        p.setType(Display.PICKER_TYPE_DATE);
        Assertions.assertEquals(defaultDate, p.getDate(),
                "Switching off a date type drops the pinned date, so the default should apply again");
    }

    @Test
    public void defaultAppliesToDateAndTimeType() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        Date fixed = date(2030, Calendar.JUNE, 15);
        p.setDefaultDate(fixed);
        Assertions.assertEquals(fixed, p.getDate(),
                "Default should apply to PICKER_TYPE_DATE_AND_TIME as well");
    }

    @Test
    public void getterReturningNullFallsBackToNewDate() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        p.setDefaultDate(new Picker.DateGetter() {
            @Override
            public Date get() {
                return null;
            }
        });
        Assertions.assertNotNull(p.getDate(),
                "A getter that returns null must fall back to a fresh Date, never null");
    }

    /// Regression for #5014: with a default-date getter installed and no
    /// explicit `setDate(non-null)`, tapping the picker stages the resolved
    /// default into `value` so the popup can show it. Pressing Cancel must
    /// roll that staging back so the picker keeps showing its placeholder
    /// ("...") and a re-open re-resolves the getter, exactly as before the
    /// RFE #4973 change.
    @FormTest
    public void cancelOnFirstOpenKeepsPlaceholderAndDoesNotPinValue() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        if (impl != null) {
            impl.setTablet(false);
        }
        final Picker picker = new Picker();
        picker.setDefaultDate(new Picker.DateGetter() {
            @Override
            public Date get() {
                return date(2030, Calendar.JUNE, 15);
            }
        });
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        picker.setDate(null);
        Assertions.assertEquals("...", picker.getText(),
                "Before the first tap the picker must show its placeholder");

        Form f = new Form(new BoxLayout(BoxLayout.Y_AXIS));
        f.add(picker);
        f.show();

        picker.pressed();
        picker.released();
        runAnimations(f);

        InteractionDialog dlg = findInteractionDialog(f);
        Assertions.assertNotNull(dlg, "Lightweight popup should be open");
        Button cancel = findButtonWithText(dlg, "Cancel");
        Assertions.assertNotNull(cancel, "Cancel button should be present");
        cancel.pressed();
        cancel.released();
        DisplayTest.flushEdt();
        runAnimations(f);

        Assertions.assertEquals("...", picker.getText(),
                "Cancel on the first open must leave the placeholder intact (#5014)");
        // Bump the slot the getter would return so we can prove the next
        // getDate() call still resolves through the getter rather than
        // returning a leaked staged value from the cancelled open.
        final Date[] slot = new Date[] { date(2031, Calendar.FEBRUARY, 2) };
        picker.setDefaultDate(new Picker.DateGetter() {
            @Override
            public Date get() {
                return slot[0];
            }
        });
        Assertions.assertEquals(slot[0], picker.getDate(),
                "After Cancel the default getter must still drive getDate()");
    }

    @FormTest
    public void cancelAfterCustomButtonRollsBackExplicitFlag() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        if (impl != null) {
            impl.setTablet(false);
        }
        final Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        final Date defaultDate = date(2030, Calendar.JUNE, 15);
        picker.setDefaultDate(defaultDate);
        // Custom button simulates the "+7 days" case from the RFE - calls setDate
        // mid-edit, which would normally pin the date. A subsequent Cancel must
        // roll the picker back so the default getter is consulted again.
        picker.addLightweightPopupButton("Shift", new Runnable() {
            @Override
            public void run() {
                picker.setDate(date(2031, Calendar.FEBRUARY, 2));
            }
        });
        Form f = new Form(new BoxLayout(BoxLayout.Y_AXIS));
        f.add(picker);
        f.show();

        picker.pressed();
        picker.released();
        runAnimations(f);

        InteractionDialog dlg = findInteractionDialog(f);
        Assertions.assertNotNull(dlg, "Lightweight popup should be open");
        Button shift = findButtonWithText(dlg, "Shift");
        Assertions.assertNotNull(shift, "Custom button should be present");
        shift.pressed();
        shift.released();
        DisplayTest.flushEdt();
        runAnimations(f);

        Button cancel = findButtonWithText(dlg, "Cancel");
        Assertions.assertNotNull(cancel, "Cancel button should be present");
        cancel.pressed();
        cancel.released();
        DisplayTest.flushEdt();
        runAnimations(f);

        Assertions.assertEquals(defaultDate, picker.getDate(),
                "Cancel after a custom-button setDate must restore the default-getter behavior");
    }

    private void runAnimations(Form f) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 400) {
            f.animate();
            f.layoutContainer();
            DisplayTest.flushEdt();
            f.revalidate();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                // ignored
            }
        }
    }

    private InteractionDialog findInteractionDialog(Form f) {
        InteractionDialog dlg = findInteractionDialog(f.getLayeredPane());
        if (dlg != null) return dlg;
        return findInteractionDialog((Container) f);
    }

    private InteractionDialog findInteractionDialog(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponentAt(i);
            if (child instanceof InteractionDialog) {
                return (InteractionDialog) child;
            }
            if (child instanceof Container) {
                InteractionDialog found = findInteractionDialog((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Button findButtonWithText(Container container, String text) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponentAt(i);
            if (c instanceof Button && text.equals(((Button) c).getText())) {
                return (Button) c;
            }
            if (c instanceof Container) {
                Button b = findButtonWithText((Container) c, text);
                if (b != null) return b;
            }
        }
        return null;
    }
}
