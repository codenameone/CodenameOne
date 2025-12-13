package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class CustomFontTest extends UITestBase {

    @FormTest
    public void testCustomFont() {
        // Create a dummy bitmap for the font
        // Assume 2 chars 'A' and 'B', each 10x10 pixels
        Image bitmap = Image.createImage(20, 10, 0xFF000000);
        int[] cutOffsets = new int[]{0, 10};
        int[] charWidth = new int[]{10, 10};
        String charsets = "AB";

        CustomFont font = new CustomFont(bitmap, cutOffsets, charWidth, charsets);

        Assertions.assertEquals(10, font.charWidth('A'));
        Assertions.assertEquals(10, font.charWidth('B'));
        Assertions.assertEquals(0, font.charWidth('C')); // Not in charset

        Assertions.assertEquals(10, font.getHeight());
        Assertions.assertEquals("AB", font.getCharset());

        Assertions.assertEquals(20, font.stringWidth("AB"));
        Assertions.assertEquals(10, font.substringWidth("ABC", 0, 1));
    }
}
