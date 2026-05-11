package com.codename1.ui.util;

import com.codename1.testing.AbstractTest;

import java.util.Hashtable;

/**
 * Regression test for Resources.getL10N / listL10NLocales / l10NLocaleSet
 * returning null instead of throwing NullPointerException when a bundle id
 * is not present in the .res file.
 *
 * Reported when an initializr-generated barebones project shipped its bundles
 * under common/src/main/resources instead of common/src/main/l10n. The runtime
 * lookup blew up at MyAppName.init -> Resources.getL10N because the resource id
 * was missing from theme.res entirely.
 */
public class ResourcesL10NTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        Resources empty = new Resources();

        Hashtable<String, String> bundle = empty.getL10N("missingBundle", "en");
        assertTrue(bundle == null, "getL10N must return null for an unknown bundle id, not throw NPE");

        bundle = empty.getL10N("missingBundle", "");
        assertTrue(bundle == null, "getL10N must return null for an unknown bundle id with empty locale");

        assertTrue(empty.listL10NLocales("missingBundle") == null,
                "listL10NLocales must return null for an unknown bundle id, not throw NPE");

        assertTrue(empty.l10NLocaleSet("missingBundle") == null,
                "l10NLocaleSet must return null for an unknown bundle id, not throw NPE");

        return true;
    }
}
