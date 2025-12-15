package com.codename1.util.regex;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;

public class CharacterArrayCharacterIteratorTest extends UITestBase {

    @FormTest
    public void testIterator() {
        char[] chars = "Hello World".toCharArray();
        CharacterArrayCharacterIterator it = new CharacterArrayCharacterIterator(chars, 0, chars.length);

        Assertions.assertEquals('H', it.charAt(0));
        Assertions.assertEquals('e', it.charAt(1));

        Assertions.assertEquals("Hello", it.substring(0, 5));
        Assertions.assertEquals("World", it.substring(6));

        Assertions.assertFalse(it.isEnd(0));
        Assertions.assertTrue(it.isEnd(chars.length));
    }

    @FormTest
    public void testExceptions() {
        char[] chars = "Test".toCharArray();
        CharacterArrayCharacterIterator it = new CharacterArrayCharacterIterator(chars, 0, chars.length);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> it.substring(0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> it.substring(-1, 2));
    }
}
