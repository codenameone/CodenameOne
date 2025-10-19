package com.codename1.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CSVParserTest {
    @Test
    void parsesSimpleRowsAndColumns() throws IOException {
        String csv = "name,age,city\nAlice,30,Paris\nBob,25,London";
        CSVParser parser = new CSVParser();

        String[][] rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, rows.length);
        assertArrayEquals(new String[]{"name", "age", "city"}, rows[0]);
        assertArrayEquals(new String[]{"Alice", "30", "Paris"}, rows[1]);
        assertArrayEquals(new String[]{"Bob", "25", "London"}, rows[2]);
    }

    @Test
    void supportsQuotedValuesAndEscapedQuotes() throws IOException {
        String csv = "\"Name\",\"Address\",\"Notes\"\n" +
                "\"Doe, John\",\"\"\"Main\"\" Street\",\"Line with \"\"quote\"\" inside\"";
        CSVParser parser = new CSVParser();

        String[][] rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, rows.length);
        assertArrayEquals(new String[]{"Name", "Address", "Notes"}, rows[0]);
        assertArrayEquals(new String[]{"Doe, John", "\"Main\" Street", "Line with \"quote\" inside"}, rows[1]);
    }

    @Test
    void honorsCustomSeparatorAndEmptyValues() throws IOException {
        String csv = "one;two;;\n;three;four;";
        CSVParser parser = new CSVParser(';');

        String[][] rows = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, rows.length);
        assertArrayEquals(new String[]{"one", "two", "", ""}, rows[0]);
        assertArrayEquals(new String[]{"", "three", "four"}, rows[1]);
    }
}
