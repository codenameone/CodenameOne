package com.codename1.l10n;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import static com.codename1.testing.TestUtils.*;

public class DateFormatPatternsTest extends UITestBase {
    @FormTest
    public void testDateFormatPatterns() {
        DateFormatPatterns dfp = new DateFormatPatterns();
        assertNotNull(dfp);
        assertNotNull(DateFormatPatterns.ISO8601);
    }
}
