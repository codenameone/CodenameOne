package com.codename1.util.regex;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class RETest extends UITestBase {

    @FormTest
    void testMatchAndGroups() throws Exception {
        RE expression = new RE("(item)-(\\d+)");

        assertTrue(expression.match("item-42"));
        assertEquals("item-42", expression.getParen(0));
        assertEquals("item", expression.getParen(1));
        assertEquals("42", expression.getParen(2));
        assertEquals(3, expression.getParenCount());
        assertEquals(0, expression.getParenStart(0));
        assertEquals(7, expression.getParenEnd(0));
    }

    @FormTest
    void testSubstitutionWithBackReferences() throws Exception {
        RE expression = new RE("(\\w+)-(\\d+)");
        String input = "item-12 and item-34";

        String replaced = expression.subst(input, "$2:$1", RE.REPLACE_ALL | RE.REPLACE_BACKREFERENCES);

        assertEquals("12:item and 34:item", replaced);

        String[] split = expression.split(input);
        assertArrayEquals(new String[]{"", " and "}, split);
    }

    @FormTest
    void testNestedPosixAlphaCharacterClassSupport() throws Exception {
        RE expression = new RE("^list [[:alpha:]]*$");

        assertTrue(expression.match("list abcXYZ"));
        assertTrue(expression.match("list "));
        assertFalse(expression.match("list 123"));
        assertFalse(expression.match("listing abc"));
    }

    @FormTest
    void testLegacyPosixAlphaCharacterClassSupport() throws Exception {
        RE expression = new RE("^list [:alpha:]*$");

        assertTrue(expression.match("list alpha"));
        assertFalse(expression.match("list alpha1"));
    }

    @FormTest
    void testPosixClassesAndEscapes() throws Exception {
        RE alnum = new RE("^[[:alnum:]]+$");
        assertTrue(alnum.match("abc123"));
        assertFalse(alnum.match("abc-123"));

        RE digit = new RE("^[[:digit:]]+$");
        assertTrue(digit.match("007"));
        assertFalse(digit.match("7a"));

        RE xdigit = new RE("^[[:xdigit:]]+$");
        assertTrue(xdigit.match("a0B9F"));
        assertFalse(xdigit.match("g0"));

        RE wordThenDigits = new RE("^\\w+\\s+\\d+$");
        assertTrue(wordThenDigits.match("item\t42"));
        assertFalse(wordThenDigits.match("item-42"));
    }

    // Non-Latin coverage. Source files must remain ASCII-only (CI javac uses
    // the platform default encoding), so non-ASCII test data is written with
    // Java's backslash-u escape syntax inside string literals.
    //
    //   U+00E7 c-cedilla (lower)        U+00C7 C-cedilla (upper)
    //   U+03B1 Greek alpha (lower)      U+03A3 Greek Sigma (upper)
    //   U+044F Cyrillic ya (lower)      U+042F Cyrillic YA (upper)
    //   U+65E5 CJK 'day' ideograph      (OTHER_LETTER, no case)
    //   U+00BD vulgar fraction one-half (OTHER_NUMBER, not a decimal digit)
    //   U+20AC euro sign                (CURRENCY_SYMBOL)

    @FormTest
    void testPosixAlphaMatchesNonLatinLetters() throws Exception {
        RE alpha = new RE("^[[:alpha:]]+$");
        assertTrue(alpha.match("\u00E7\u00C7"), "Latin with cedilla");
        assertTrue(alpha.match("\u03B1\u03A3"), "Greek letters");
        assertTrue(alpha.match("\u042F\u044F"), "Cyrillic letters");
        assertTrue(alpha.match("\u65E5"), "CJK ideograph (other letter)");
        assertTrue(alpha.match("abc\u00E7\u03B1\u042F"), "mixed scripts");

        assertFalse(alpha.match("\u00E71"), "letter followed by ASCII digit");
        assertFalse(alpha.match("\u00BD"), "vulgar fraction is not alpha");
        assertFalse(alpha.match("\u20AC"), "currency symbol is not alpha");
    }

    @FormTest
    void testPosixAlnumMatchesNonLatinLettersAndDigits() throws Exception {
        RE alnum = new RE("^[[:alnum:]]+$");
        assertTrue(alnum.match("\u00E7123"), "c-cedilla followed by digits");
        assertTrue(alnum.match("\u03B1\u03B2\u03B3"), "Greek run");
        assertTrue(alnum.match("abc\u042F9"), "ASCII + Cyrillic + digit");

        assertFalse(alnum.match("\u00E7-123"), "hyphen breaks alnum");
        assertFalse(alnum.match("\u00BD"), "fraction is not alnum (not a decimal digit)");
        assertFalse(alnum.match("\u20AC"), "currency symbol is not alnum");
    }

    @FormTest
    void testPosixLowerUpperOnNonLatinLetters() throws Exception {
        RE lower = new RE("^[[:lower:]]+$");
        assertTrue(lower.match("\u00E7"), "c-cedilla is lower");
        assertTrue(lower.match("\u03B1"), "Greek alpha is lower");
        assertTrue(lower.match("\u044F"), "Cyrillic ya is lower");
        assertFalse(lower.match("\u00C7"), "C-cedilla is not lower");
        assertFalse(lower.match("\u042F"), "Cyrillic YA is not lower");
        // CJK ideographs are OTHER_LETTER, neither lower nor upper.
        assertFalse(lower.match("\u65E5"), "CJK ideograph is not lower");

        RE upper = new RE("^[[:upper:]]+$");
        assertTrue(upper.match("\u00C7"), "C-cedilla is upper");
        assertTrue(upper.match("\u03A3"), "Greek Sigma is upper");
        assertTrue(upper.match("\u042F"), "Cyrillic YA is upper");
        assertFalse(upper.match("\u00E7"), "c-cedilla is not upper");
        assertFalse(upper.match("\u65E5"), "CJK ideograph is not upper");
    }

    @FormTest
    void testReportedAlphaAlnumCaptureBug() throws Exception {
        // Regression: "test:\\s*([[:alpha:]][[:alnum:]]*)" used to silently fail
        // to match identifiers that begin with a non-ASCII letter, because
        // RECharacter.getType() returned UNASSIGNED for any char >= 128.
        RE expression = new RE("test:\\s*([[:alpha:]][[:alnum:]]*)");

        assertTrue(expression.match("test: \u00E7123"),
                "alpha+alnum should match identifier starting with c-cedilla");
        assertEquals("\u00E7123", expression.getParen(1));

        assertTrue(expression.match("test: \u03B1\u03B2\u03B30"),
                "alpha+alnum should match a Greek identifier");
        assertEquals("\u03B1\u03B2\u03B30", expression.getParen(1));

        assertTrue(expression.match("test: \u042F\u044F1"),
                "alpha+alnum should match a Cyrillic identifier");
        assertEquals("\u042F\u044F1", expression.getParen(1));

        // A leading ASCII digit is still rejected (must start with [[:alpha:]]).
        assertFalse(expression.match("test: 9abc"));
    }

    @FormTest
    void testPosixDigitIsAsciiOnlyForOtherNumbers() throws Exception {
        // [[:digit:]] is decimal digits; vulgar fractions / superscripts
        // (OTHER_NUMBER) and currency / symbols must not match.
        RE digit = new RE("^[[:digit:]]+$");
        assertFalse(digit.match("\u00BD"), "one-half is not a decimal digit");
        assertFalse(digit.match("\u20AC"), "euro sign is not a digit");
        assertFalse(digit.match("\u00E7"), "letter is not a digit");
    }

}
