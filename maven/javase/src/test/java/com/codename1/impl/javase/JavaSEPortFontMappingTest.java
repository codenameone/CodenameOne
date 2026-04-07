package com.codename1.impl.javase;

import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JavaSEPortFontMappingTest {
    
    private Boolean originalIsIOS;

    @AfterEach
    public void tearDown() throws Exception {
        JavaSEPort.clearAvailableFontNamesLowercaseForTest();
        if (originalIsIOS != null) {
            setIsIOS(originalIsIOS.booleanValue());
        }
    }

    private void setIsIOS(boolean value) throws Exception {
        Field f = JavaSEPort.class.getDeclaredField("isIOS");
        f.setAccessible(true);
        if (originalIsIOS == null) {
            originalIsIOS = Boolean.valueOf(f.getBoolean(null));
        }
        f.setBoolean(null, value);
    }

    @Test
    public void testFindFirstInstalledFontCandidateUsesCandidateOrder() {
        Set<String> installed = new HashSet<String>();
        installed.add("sf pro display");
        installed.add("helvetica neue");

        String out = JavaSEPort.findFirstInstalledFontCandidate(
                new String[] {"SF Pro Text", "SF Pro Display", "Helvetica Neue"},
                installed
        );

        assertEquals("SF Pro Display", out);
    }

    @Test
    public void testNativeFontNameForIOSReturnsNullWhenNoCandidatesInstalled() {
        Set<String> installed = new HashSet<String>();
        installed.add("roboto");

        String out = JavaSEPort.nativeFontNameForIOS("native:MainRegular", installed);
        assertNull(out);
    }

    @Test
    public void testNativeFontNameForIOSReturnsFirstMatchingFamily() {
        Set<String> installed = new HashSet<String>();
        installed.add("sf pro text");
        installed.add("helvetica neue");

        String out = JavaSEPort.nativeFontNameForIOS("native:ItalicRegular", installed);
        assertEquals("SF Pro Text", out);
    }
    
    @Test
    public void testLoadTrueTypeFontUsesInstalledIOSCandidateWhenPresent() throws Exception {
        Set<String> installed = new HashSet<String>();
        installed.add("helvetica neue");
        JavaSEPort.setAvailableFontNamesLowercaseForTest(installed);
        setIsIOS(true);

        JavaSEPort port = new JavaSEPort();
        Object out = port.loadTrueTypeFont("native:MainRegular", "native:MainRegular");

        assertNotNull(out);
        assertEquals("Helvetica Neue", ((java.awt.Font) out).getName());
    }

    @Test
    public void testLoadTrueTypeFontFallsBackWhenNoIOSFamilyInstalled() throws Exception {
        JavaSEPort.setAvailableFontNamesLowercaseForTest(new HashSet<String>());
        setIsIOS(true);

        JavaSEPort port = new JavaSEPort();
        Object out = port.loadTrueTypeFont("native:MainRegular", "native:MainRegular");

        assertNotNull(out);
        assertEquals(java.awt.Font.class, out.getClass());
    }
}
