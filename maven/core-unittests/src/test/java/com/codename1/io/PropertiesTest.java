package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesTest {
    private TestCodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = new TestCodenameOneImplementation(true);
        Util.setImplementation(implementation);
    }

    @Test
    void defaultConstructor() {
        Properties props = new Properties();
        assertNotNull(props);
    }

    @Test
    void constructWithDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty("key1", "default1");

        Properties props = new Properties(defaults);

        assertEquals("default1", props.getProperty("key1"));
    }

    @Test
    void setAndGetProperty() {
        Properties props = new Properties();
        props.setProperty("name", "value");

        assertEquals("value", props.getProperty("name"));
    }

    @Test
    void getPropertyWithDefaultValue() {
        Properties props = new Properties();

        assertEquals("defaultValue", props.getProperty("missing", "defaultValue"));
    }

    @Test
    void getPropertyFallsBackToDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty("key1", "default1");

        Properties props = new Properties(defaults);

        assertEquals("default1", props.getProperty("key1"));
    }

    @Test
    void getPropertyPrefersCurrentOverDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty("key1", "default1");

        Properties props = new Properties(defaults);
        props.setProperty("key1", "override1");

        assertEquals("override1", props.getProperty("key1"));
    }

    @Test
    void getNonExistentProperty() {
        Properties props = new Properties();

        assertNull(props.getProperty("nonexistent"));
    }

    @Test
    void loadFromInputStream() throws IOException {
        String content = "key1=value1\nkey2=value2\n";
        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes("UTF-8"));
        Properties props = new Properties();

        props.load(bais);

        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void loadFromReader() throws IOException {
        String content = "key1=value1\nkey2=value2\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void loadHandlesComments() throws IOException {
        String content = "# Comment\nkey1=value1\n! Another comment\nkey2=value2\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void loadHandlesColonSeparator() throws IOException {
        String content = "key1:value1\nkey2:value2\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void loadHandlesWhitespaceSeparator() throws IOException {
        String content = "key1 value1\nkey2   value2\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void loadHandlesEscapeSequences() throws IOException {
        String content = "key1=value\\nwith\\nnewlines\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertTrue(props.getProperty("key1").contains("\n"));
    }

    @Test
    void loadHandlesLineContinuation() throws IOException {
        String content = "key1=value\\\ncontinued\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertEquals("valuecontinued", props.getProperty("key1"));
    }

    @Test
    void loadHandlesUnicodeEscapes() throws IOException {
        String content = "key1=\\u0048\\u0065\\u006C\\u006C\\u006F\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertEquals("Hello", props.getProperty("key1"));
    }

    @Test
    void loadThrowsOnInvalidUnicode() {
        String content = "key1=\\u00G\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        assertThrows(IllegalArgumentException.class, () -> props.load(reader));
    }

    @Test
    void loadHandlesEmptyLines() throws IOException {
        String content = "key1=value1\n\nkey2=value2\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void storeToOutputStream() throws IOException {
        Properties props = new Properties();
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, "Test comment");

        String result = new String(baos.toByteArray(), "UTF-8");
        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("key2=value2"));
        assertTrue(result.contains("Test comment"));
    }

    @Test
    void storeToWriter() throws IOException {
        Properties props = new Properties();
        props.setProperty("key1", "value1");

        StringWriter writer = new StringWriter();
        props.store(writer, "Comment");

        String result = writer.toString();
        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("Comment"));
    }

    @Test
    void storeWithoutComment() throws IOException {
        Properties props = new Properties();
        props.setProperty("key1", "value1");

        StringWriter writer = new StringWriter();
        props.store(writer, null);

        String result = writer.toString();
        assertTrue(result.contains("key1=value1"));
    }

    @Test
    void storeEscapesSpecialCharacters() throws IOException {
        Properties props = new Properties();
        props.setProperty("key", "value with spaces");

        StringWriter writer = new StringWriter();
        props.store(writer, null);

        String result = writer.toString();
        assertTrue(result.contains("value\\ with\\ spaces"));
    }

    @Test
    void propertyNames() {
        Properties props = new Properties();
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");

        Enumeration<?> names = props.propertyNames();
        int count = 0;
        while (names.hasMoreElements()) {
            names.nextElement();
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    void stringPropertyNames() {
        Properties props = new Properties();
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");

        Set<String> names = props.stringPropertyNames();

        assertEquals(2, names.size());
        assertTrue(names.contains("key1"));
        assertTrue(names.contains("key2"));
    }

    @Test
    void stringPropertyNamesIncludesDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty("default1", "value1");

        Properties props = new Properties(defaults);
        props.setProperty("key1", "value1");

        Set<String> names = props.stringPropertyNames();

        assertTrue(names.contains("default1"));
        assertTrue(names.contains("key1"));
    }

    @Test
    void saveMethod() throws IOException {
        Properties props = new Properties();
        props.setProperty("key1", "value1");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.save(baos, "Comment");

        String result = new String(baos.toByteArray(), "UTF-8");
        assertTrue(result.contains("key1=value1"));
    }

    @Test
    void loadNullThrowsException() {
        Properties props = new Properties();

        assertThrows(NullPointerException.class, () -> props.load((java.io.InputStream) null));
        assertThrows(NullPointerException.class, () -> props.load((java.io.Reader) null));
    }

    @Test
    void loadHandlesBackslashEscapes() throws IOException {
        String content = "key1=value\\\\withbackslash\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertTrue(props.getProperty("key1").contains("\\"));
    }

    @Test
    void loadHandlesTabEscape() throws IOException {
        String content = "key1=value\\twith\\ttabs\n";
        StringReader reader = new StringReader(content);
        Properties props = new Properties();

        props.load(reader);

        assertTrue(props.getProperty("key1").contains("\t"));
    }

    @Test
    void storeProducesSortedOutput() throws IOException {
        Properties props = new Properties();
        props.setProperty("zebra", "value");
        props.setProperty("apple", "value");
        props.setProperty("banana", "value");

        StringWriter writer = new StringWriter();
        props.store(writer, null);

        String result = writer.toString();
        int applePos = result.indexOf("apple");
        int bananaPos = result.indexOf("banana");
        int zebraPos = result.indexOf("zebra");

        assertTrue(applePos < bananaPos);
        assertTrue(bananaPos < zebraPos);
    }
}
