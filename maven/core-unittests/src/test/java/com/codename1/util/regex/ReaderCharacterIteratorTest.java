package com.codename1.util.regex;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import java.io.StringReader;
import org.junit.jupiter.api.Assertions;

public class ReaderCharacterIteratorTest extends UITestBase {

    @FormTest
    public void testReaderCharacterIterator() {
        String testString = "Hello World";
        StringReader reader = new StringReader(testString);
        ReaderCharacterIterator iterator = new ReaderCharacterIterator(reader);

        // Test charAt
        Assertions.assertEquals('H', iterator.charAt(0));
        Assertions.assertEquals('e', iterator.charAt(1));
        Assertions.assertEquals('d', iterator.charAt(10));

        // Test substring
        Assertions.assertEquals("Hello", iterator.substring(0, 5));
        Assertions.assertEquals("World", iterator.substring(6));

        // Test isEnd
        Assertions.assertFalse(iterator.isEnd(0));
        Assertions.assertFalse(iterator.isEnd(10));
        Assertions.assertTrue(iterator.isEnd(11));

        // Test out of bounds
        Assertions.assertThrows(StringIndexOutOfBoundsException.class, () -> iterator.charAt(12));
    }
}
