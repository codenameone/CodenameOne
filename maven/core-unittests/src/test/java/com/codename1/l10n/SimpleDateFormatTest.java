package com.codename1.l10n;

import com.codename1.junit.UITestBase;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleDateFormatTest extends UITestBase {
    private L10NManager l10nManager;
    private boolean originalRestrict;

    @BeforeEach
    void setUpLocalization() {
        originalRestrict = SimpleDateFormat.isRestrictMonthNameLength();
        l10nManager = new StubL10NManager();
        implementation.setLocalizationManager(l10nManager);
    }

    @AfterEach
    void resetRestrictionFlag() {
        SimpleDateFormat.setRestrictMonthNameLength(originalRestrict);
    }

    @Test
    void applyPatternClearsCachedTokens() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        List<String> initialTokens = format.getPatternTokens();
        assertFalse(initialTokens.isEmpty());

        format.applyPattern("dd/MM");
        List<String> newTokens = format.getPatternTokens();

        assertNotSame(initialTokens, newTokens);
        assertEquals(3, newTokens.size());
    }

    @Test
    void formatProducesExpectedTimestamp() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone testZone = TimeZone.getTimeZone("GMT+02:00");
            TimeZone.setDefault(testZone);
            Calendar calendar = Calendar.getInstance(testZone);
            calendar.set(Calendar.YEAR, 2020);
            calendar.set(Calendar.MONTH, Calendar.MARCH);
            calendar.set(Calendar.DAY_OF_MONTH, 15);
            calendar.set(Calendar.HOUR_OF_DAY, 13);
            calendar.set(Calendar.MINUTE, 45);
            calendar.set(Calendar.SECOND, 30);
            calendar.set(Calendar.MILLISECOND, 500);
            Date date = calendar.getTime();

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String result = format.format(date);

            assertEquals("2020-03-15 13:45:30.500", result);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void formatTruncatesMonthNamesWhenRestrictionEnabled() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
            calendar.set(2021, Calendar.SEPTEMBER, 1, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date date = calendar.getTime();

            SimpleDateFormat.setRestrictMonthNameLength(true);
            SimpleDateFormat format = new SimpleDateFormat("MMMM");
            String result = format.format(date);

            assertEquals(4, result.length());
            assertEquals("Long", result);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void parseHandlesNumericTimeZoneOffsets() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");
        Date parsed = format.parse("2020-03-15 16:20 +0200");

        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("GMT+02:00"));
        expected.set(2020, Calendar.MARCH, 15, 16, 20, 0);
        expected.set(Calendar.MILLISECOND, 0);

        assertEquals(expected.getTime().getTime(), parsed.getTime());
    }

    @Test
    void parseRecognizesNamedTimeZones() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy MMM dd HH:mm z");
        DateFormatSymbols symbols = format.getDateFormatSymbols();
        symbols.addZoneMapping("America/Los_Angeles", "Pacific Standard Time", "Pacific Daylight Time", "PST", "PDT");
        format.setDateFormatSymbols(symbols);

        Date parsed = format.parse("2020 Feb 15 08:00 PST");

        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        expected.set(2020, Calendar.FEBRUARY, 15, 8, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);

        assertEquals(expected.getTime().getTime(), parsed.getTime());
    }

    @Test
    void parseThrowsExceptionForInvalidInput() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        assertThrows(com.codename1.l10n.ParseException.class, () -> format.parse("invalid"));
    }

    @Test
    void cloneCopiesPatternAndSymbols() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy");
        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setAmPmStrings(new String[]{"morning", "night"});
        format.setDateFormatSymbols(symbols);

        SimpleDateFormat clone = (SimpleDateFormat) format.clone();

        assertEquals("yyyy", clone.toPattern());
        assertSame(symbols, clone.getDateFormatSymbols());
    }

    @Test
    void testQuotedStringLiteral() {
        // "formats like yyyy.MM.dd G 'at' HH:mm:ss z yield illegal argument exception on the t char."
        assertDoesNotThrow(() -> {
            new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
        }, "Should not throw exception for quoted literal 'at'");
    }

    @Test
    void testEscapedSingleQuoteFormat() {
        // "Also formats like: hh 'o''clock' yield exception for the c char."
        SimpleDateFormat sdf = new SimpleDateFormat("hh 'o''clock'");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 5);
        String result = sdf.format(cal.getTime());
        // Expected: 05 o'clock
        assertTrue(result.contains("o'clock"), "Should contain single quote: " + result);
    }

    @Test
    void testXXXFormat() {
        // "yyyy-MM-dd'T'HH:mm:ss.SSSXXX (exception for X char, recent API level prerequisite)"
        assertDoesNotThrow(() -> {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        }, "Should not throw exception for XXX format");

        SimpleDateFormat sdf = new SimpleDateFormat("XXX");
        // We cannot reliably test exact timezone string without controlling default timezone,
        // but we can check it doesn't crash and produces something reasonable.
        String result = sdf.format(new Date());
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testYYYYAnduFormat() {
        // "YYYY-'W'ww-u (exception for Y char)"
        assertDoesNotThrow(() -> {
            new SimpleDateFormat("YYYY-'W'ww-u");
        }, "Should not throw exception for YYYY and u format");

        SimpleDateFormat sdf = new SimpleDateFormat("u");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String result = sdf.format(cal.getTime());
        assertEquals("1", result, "Monday should be 1");

        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        result = sdf.format(cal.getTime());
        assertEquals("7", result, "Sunday should be 7");
    }

    private static class StubL10NManager extends L10NManager {
        StubL10NManager() {
            super("en", "US");
        }

        @Override
        public String getLongMonthName(Date date) {
            return "LongMonthName";
        }

        @Override
        public String getShortMonthName(Date date) {
            return "Long";
        }
    }
}
