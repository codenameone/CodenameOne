package com.codename1.flutter;

import com.codename1.flutter.testsupport.AltBox;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanUpdateTest {

    @Test
    void sameTypeNoKeysCanUpdate() {
        assertTrue(Widget.canUpdate(new ProbeBox(1, 1), new ProbeBox(2, 2)));
    }

    @Test
    void differentTypeCannotUpdate() {
        assertFalse(Widget.canUpdate(new ProbeBox(1, 1), new AltBox(1, 1)));
    }

    @Test
    void sameTypeEqualValueKeysCanUpdate() {
        ProbeBox a = new ProbeBox(1, 1);
        a.key(new ValueKey<String>("k"));
        ProbeBox b = new ProbeBox(2, 2);
        b.key(new ValueKey<String>("k"));
        assertTrue(Widget.canUpdate(a, b));
    }

    @Test
    void sameTypeDifferentKeysCannotUpdate() {
        ProbeBox a = new ProbeBox(1, 1);
        a.key(new ValueKey<String>("k1"));
        ProbeBox b = new ProbeBox(1, 1);
        b.key(new ValueKey<String>("k2"));
        assertFalse(Widget.canUpdate(a, b));
    }

    @Test
    void keyOnOnlyOneSideCannotUpdate() {
        ProbeBox a = new ProbeBox(1, 1);
        a.key(new ValueKey<Long>(7L));
        assertFalse(Widget.canUpdate(a, new ProbeBox(1, 1)));
        assertFalse(Widget.canUpdate(new ProbeBox(1, 1), a));
    }

    @Test
    void nullsNeverUpdate() {
        assertFalse(Widget.canUpdate(null, new ProbeBox(1, 1)));
        assertFalse(Widget.canUpdate(new ProbeBox(1, 1), null));
        assertFalse(Widget.canUpdate(null, null));
    }
}
