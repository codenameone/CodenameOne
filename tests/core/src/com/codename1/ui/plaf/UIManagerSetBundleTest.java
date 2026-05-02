package com.codename1.ui.plaf;

import com.codename1.testing.AbstractTest;

import java.util.HashMap;
import java.util.Map;

/**
 * Regression test for the StringIndexOutOfBoundsException seen at CSS-compile
 * time in initializr-generated projects (issue #4850).
 *
 * The simulator's AutoLocalizationBundle echoes any missing key back as its
 * own value. setBundle queries `@im` (the input-mode descriptor) on every
 * bundle install, and when the underlying bundle is empty the auto-bundle
 * answered "@im". setBundle then tokenized that, queried "@im-@im", got
 * "@im-@im" back, and parseTextFieldInputMode crashed on substring(0, -1).
 *
 * The defensive fix lives in UIManager.parseTextFieldInputMode (skip tokens
 * without '=' and skip non-numeric keys). This test simulates an offending
 * bundle and verifies setBundle no longer throws.
 */
public class UIManagerSetBundleTest extends AbstractTest {

    /**
     * Bundle that echoes any missing key back as its value -- matches the
     * pre-fix AutoLocalizationBundle behavior so we can exercise setBundle's
     * defensive path independently of the simulator.
     */
    private static final class EchoBundle extends HashMap<String, String> {
        @Override
        public String get(Object key) {
            String value = super.get(key);
            if (value != null) {
                return value;
            }
            if (key instanceof String) {
                return (String) key;
            }
            return null;
        }
    }

    @Override
    public boolean runTest() throws Exception {
        UIManager mgr = UIManager.getInstance();

        Map<String, String> echo = new EchoBundle();
        // No assertion needed -- before the fix this throws StringIndexOutOfBoundsException
        // inside parseTextFieldInputMode("@im-@im") because the token has no '='.
        mgr.setBundle(echo);

        // Same flow but with a partially-populated bundle that has "@im" pointing at a
        // malformed input-mode descriptor (no '='). Should also not crash.
        Map<String, String> malformed = new HashMap<String, String>();
        malformed.put("@im", "ABC");
        malformed.put("@im-ABC", "garbage_with_no_equals");
        mgr.setBundle(malformed);

        // And one with a non-numeric key in the input-mode descriptor -- previously this
        // would have thrown NumberFormatException out of parseTextFieldInputMode.
        Map<String, String> nonNumericKey = new HashMap<String, String>();
        nonNumericKey.put("@im", "ABC");
        nonNumericKey.put("@im-ABC", "notANumber=value");
        mgr.setBundle(nonNumericKey);

        // Restore default bundle so we don't bleed state into other tests.
        mgr.setBundle(null);

        return true;
    }
}
