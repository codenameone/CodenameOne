package com.codename1.util.regex;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;

public class StreamCharacterIteratorTest extends UITestBase {

    @FormTest
    public void testStreamCharacterIterator() {
        String testString = "Hello Stream";
        ByteArrayInputStream is = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
        StreamCharacterIterator iterator = new StreamCharacterIterator(is);

        // Test charAt
        Assertions.assertEquals('H', iterator.charAt(0));
        Assertions.assertEquals('e', iterator.charAt(1));
        Assertions.assertEquals('m', iterator.charAt(11));

        // Test substring
        Assertions.assertEquals("Hello", iterator.substring(0, 5));
        Assertions.assertEquals("Stream", iterator.substring(6));

        // Test isEnd
        Assertions.assertFalse(iterator.isEnd(0));
        Assertions.assertFalse(iterator.isEnd(11));
        Assertions.assertTrue(iterator.isEnd(12));

        // Test out of bounds
        Assertions.assertThrows(StringIndexOutOfBoundsException.class, () -> iterator.charAt(13));
    }
}
