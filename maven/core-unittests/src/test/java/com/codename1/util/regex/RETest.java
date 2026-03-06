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

}
