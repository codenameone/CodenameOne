package com.codename1.router;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PopGuardTest extends UITestBase {

    @Test
    void noGuardAllowsPop() {
        Form f = new Form();
        assertTrue(f.checkPopGuard(PopReason.PROGRAMMATIC));
    }

    @Test
    void installedGuardCanDeny() {
        Form f = new Form();
        f.setPopGuard(new PopGuard() {
            public boolean canPop(Form form, PopReason reason) { return false; }
        });
        assertFalse(f.checkPopGuard(PopReason.BACK_COMMAND));
    }

    @Test
    void guardSeesReason() {
        final PopReason[] seen = new PopReason[1];
        Form f = new Form();
        f.setPopGuard(new PopGuard() {
            public boolean canPop(Form form, PopReason reason) {
                seen[0] = reason;
                return true;
            }
        });
        f.checkPopGuard(PopReason.HARDWARE_BACK);
        assertSame(PopReason.HARDWARE_BACK, seen[0]);
    }

    @Test
    void throwingGuardDefaultsToAllow() {
        Form f = new Form();
        f.setPopGuard(new PopGuard() {
            public boolean canPop(Form form, PopReason reason) {
                throw new RuntimeException("boom");
            }
        });
        // Throwing must not propagate; navigation should continue (true).
        assertTrue(f.checkPopGuard(PopReason.PROGRAMMATIC));
    }
}
