package com.codename1.maven;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The regression cases here are taken from a real failing customer build: a project whose
 * codenameone_settings.properties pinned three Gradle dependencies by hand, plus the
 * CN1JailbreakDetect cn1lib, whose codenameone_library_appended.properties contributes
 * "implementation 'com.scottyab:rootbeer-lib:0.0.8';" -- a trailing separator, which is the
 * wrong end for a value that is always the right hand side of the join.
 */
public class LibraryHintMergerTest {

    private static final String GRADLE_DEP = "codename1.arg.android.gradleDep";

    /** Exactly what CN1JailbreakDetect ships. */
    private static final String JAILBREAK_LIB_GRADLE_DEP =
            "implementation 'com.scottyab:rootbeer-lib:0.0.8';";

    @Test
    public void gradleDependenciesAreJoinedWithASemicolon() {
        String merged = LibraryHintMerger.append(GRADLE_DEP,
                "implementation 'co.infinum:goldeneye:1.1.2';"
                        + "implementation 'com.google.firebase:firebase-messaging:23.2.1'",
                JAILBREAK_LIB_GRADLE_DEP);

        assertEquals("implementation 'co.infinum:goldeneye:1.1.2';"
                + "implementation 'com.google.firebase:firebase-messaging:23.2.1';"
                + "implementation 'com.scottyab:rootbeer-lib:0.0.8'", merged);
        assertNull(LibraryHintMerger.findUnseparatedStatement(GRADLE_DEP, merged));
    }

    @Test
    public void aLibraryValueWithNoSeparatorAtAllStillMergesCleanly() {
        // The pre-2024 CN1JailbreakDetect release, which carried no separator either side.
        String merged = LibraryHintMerger.append(GRADLE_DEP,
                "implementation 'co.infinum:goldeneye:1.1.2'",
                "implementation 'com.scottyab:rootbeer-lib:0.0.8'");

        assertEquals("implementation 'co.infinum:goldeneye:1.1.2';"
                + "implementation 'com.scottyab:rootbeer-lib:0.0.8'", merged);
    }

    @Test
    public void aSeparatorIsNeverDoubled() {
        assertEquals("implementation 'a:b:1';implementation 'c:d:2'",
                LibraryHintMerger.append(GRADLE_DEP,
                        "implementation 'a:b:1';", ";implementation 'c:d:2'"));
    }

    @Test
    public void anEmptyProjectValueDoesNotLeadWithASeparator() {
        // ios.pods used to merge to ",DTTJailbreakDetection", whose first comma separated
        // entry is empty.
        assertEquals("DTTJailbreakDetection",
                LibraryHintMerger.append("codename1.arg.ios.pods", "", ",DTTJailbreakDetection"));
    }

    @Test
    public void iosPodsAreJoinedWithAComma() {
        assertEquals("QBImagePickerController ~> 3.4,DTTJailbreakDetection",
                LibraryHintMerger.append("codename1.arg.ios.pods",
                        "QBImagePickerController ~> 3.4", ",DTTJailbreakDetection"));
    }

    @Test
    public void xmlFragmentHintsStillAbutDirectly() {
        // The case the mechanism was designed for: two manifest fragments concatenate, and
        // inserting anything between them would corrupt the XML.
        String permissions = LibraryHintMerger.append("codename1.arg.android.xpermissions",
                "<uses-permission android:name=\"android.permission.CAMERA\"/>",
                "<uses-permission android:name=\"android.permission.VIBRATE\"/>");

        assertEquals("<uses-permission android:name=\"android.permission.CAMERA\"/>"
                + "<uses-permission android:name=\"android.permission.VIBRATE\"/>", permissions);
        assertEquals("", LibraryHintMerger.separatorFor("codename1.arg.android.xpermissions"));
    }

    @Test
    public void applicationAttributesAreSeparatedByASpace() {
        assertEquals("android:allowBackup=\"false\" android:hardwareAccelerated=\"true\"",
                LibraryHintMerger.append("codename1.arg.android.xapplication_attr",
                        "android:allowBackup=\"false\"",
                        "android:hardwareAccelerated=\"true\""));
    }

    @Test
    public void aDependencyAlreadyPinnedByTheProjectIsNotAddedAgain() {
        // Why the customer's build worked for years: their settings carried the library's
        // dependency verbatim, so the library's copy was suppressed. It has to stay suppressed
        // whichever separator convention the library used.
        String existing = "implementation 'co.infinum:goldeneye:1.1.2';"
                + "implementation 'com.scottyab:rootbeer-lib:0.0.8';"
                + "implementation 'com.google.firebase:firebase-messaging:23.2.1'";

        assertTrue(LibraryHintMerger.alreadyContains(GRADLE_DEP, existing, JAILBREAK_LIB_GRADLE_DEP));
        assertTrue(LibraryHintMerger.alreadyContains(GRADLE_DEP, existing,
                ";implementation 'com.scottyab:rootbeer-lib:0.0.8'"));
        assertFalse(LibraryHintMerger.alreadyContains(GRADLE_DEP, existing,
                "implementation 'ca.weblite:fridablocker:1.0.10'"));
    }

    @Test
    public void theUnseparatedStatementFromTheFailingBuildIsReported() {
        String broken = "implementation 'co.infinum:goldeneye:1.1.2';"
                + "implementation 'com.google.firebase:firebase-messaging:23.2.1'"
                + "implementation 'com.scottyab:rootbeer-lib:0.0.8'";

        String problem = LibraryHintMerger.findUnseparatedStatement(GRADLE_DEP, broken);

        assertNotNull(problem);
        assertTrue(problem.contains(GRADLE_DEP));
        assertTrue(problem.contains("com.scottyab:rootbeer-lib:0.0.8"));
    }

    @Test
    public void wellFormedDependencyListsAreNotReported() {
        assertNull(LibraryHintMerger.findUnseparatedStatement(GRADLE_DEP,
                "implementation 'a:b:1';implementation 'c:d:2'"));
        assertNull(LibraryHintMerger.findUnseparatedStatement(GRADLE_DEP,
                "implementation 'a:b:1'\nimplementation 'c:d:2'"));
        assertNull(LibraryHintMerger.findUnseparatedStatement(GRADLE_DEP,
                "implementation(name:'ZBarScannerLibrary', ext:'aar')"));
        // An artifact whose name merely contains a configuration keyword.
        assertNull(LibraryHintMerger.findUnseparatedStatement(GRADLE_DEP,
                "implementation 'com.example:implementation-helper:1.0'"));
        assertNull(LibraryHintMerger.findUnseparatedStatement(GRADLE_DEP, null));
    }

    @Test
    public void hintNamesResolveWithOrWithoutTheArgPrefix() {
        assertEquals(";", LibraryHintMerger.separatorFor("android.gradleDep"));
        assertEquals(";", LibraryHintMerger.separatorFor(GRADLE_DEP));
        assertEquals(",", LibraryHintMerger.separatorFor("ios.pods"));
        assertEquals("\n", LibraryHintMerger.separatorFor("codename1.arg.gradleDependencies"));
        assertEquals("", LibraryHintMerger.separatorFor("codename1.arg.some.unknown.hint"));
        assertEquals("", LibraryHintMerger.separatorFor(null));
    }
}
