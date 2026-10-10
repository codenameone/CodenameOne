/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.desktopcompat.java.awt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.compat.jdk.Resources;
import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.image.BufferedImage;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.rt.FontFiles;
import com.codename1.desktopcompat.rt.Fonts;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/// A font the application carries: `Font.createFont` reads a stream of
/// it, finds the resource the bytes are, and the font -- at any size
/// derived from it, and by its family once registered -- is drawn with
/// the file. The font is the Material Icons file the framework ships.
public class CreatedFontTest extends KernelTestBase {

    private static final String PATH = "com/example/fonts/Icons.ttf";
    private static final String FLAT = "com__example__fonts__Icons.ttf";

    private final List<String> opened = new ArrayList<String>();

    private static byte[] fontBytes() throws IOException {
        InputStream in = CreatedFontTest.class.getResourceAsStream("/material-design-font.ttf");
        assertNotNull("the framework's icon font is on the class path", in);
        try {
            return FontFiles.readAll(in);
        } finally {
            in.close();
        }
    }

    @Before
    public void shipTheFont() throws IOException {
        final byte[] font = fontBytes();
        FontFiles.reset();
        Resources.cn1BeginIndex();
        Resources.cn1AddResource(PATH);
        Resources.cn1AddResource("com/example/readme.txt");
        Resources.cn1SetProvider(new Resources.Provider() {
            @Override
            public InputStream open(String flatName) {
                opened.add(flatName);
                return FLAT.equals(flatName) ? new ByteArrayInputStream(font) : null;
            }
        });
    }

    @After
    public void forget() {
        Resources.cn1SetProvider(null);
        Resources.cn1ClearIndex();
        FontFiles.reset();
    }

    @Test
    public void aStreamOfAFontFileMakesAFontNamedAsTheFileNamesItself() throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontBytes()));
        assertEquals("Material Icons", font.getFamily());
        assertEquals(1, font.getSize());
        assertTrue(font.isPlain());
        FontFiles.Face face = font.cn1Face();
        assertNotNull(face);
        // The platform is handed the flat name the build shipped, and the
        // name the font is installed under.
        assertEquals(FLAT, face.file());
        assertEquals("MaterialIcons-Regular", face.postScriptName());
        assertNotNull("the platform opened the file", face.base());
        assertEquals("only font resources are read", Arrays.asList(FLAT), opened);
    }

    @Test
    public void aDerivedFontKeepsTheFaceAndASystemFontHasNone() throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontBytes()));
        Font big = font.deriveFont(24f);
        assertEquals(24, big.getSize());
        assertSame(font.cn1Face(), big.cn1Face());
        assertSame(font.cn1Face(), big.deriveFont(Font.BOLD).cn1Face());
        assertSame(font.cn1Face(), big.deriveFont(Font.ITALIC, 30f).cn1Face());
        assertEquals("Material Icons", big.getFamily());
        assertNull(new Font("Dialog", Font.PLAIN, 24).cn1Face());
        assertFalse(big.equals(new Font(big.getName(), Font.PLAIN, 24)));
        assertEquals(big, font.deriveFont(24f));
        // The same face at the same size is one platform font; another
        // size is another.
        com.codename1.ui.Font a = Fonts.nativeFont(big, 48);
        assertNotNull(a);
        assertSame(a, Fonts.nativeFont(font.deriveFont(24f), 48));
        assertNotNull(Toolkit.getDefaultToolkit().getFontMetrics(big));
    }

    @Test
    public void aRegisteredFontIsFoundByItsFamily() throws Exception {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        assertNull(new Font("Material Icons", Font.PLAIN, 16).cn1Face());
        assertFalse("not a created font", ge.registerFont(new Font("Dialog", Font.PLAIN, 12)));
        Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontBytes()));
        assertFalse(Arrays.asList(ge.getAvailableFontFamilyNames()).contains("Material Icons"));
        assertTrue(ge.registerFont(font));
        assertTrue(Arrays.asList(ge.getAvailableFontFamilyNames()).contains("Material Icons"));
        assertSame(font.cn1Face(), new Font("Material Icons", Font.PLAIN, 16).cn1Face());
        assertSame("names are matched without regard to case", font.cn1Face(),
                new Font("material icons", Font.BOLD, 16).cn1Face());
        assertTrue("again is no clash", ge.registerFont(font.deriveFont(9f)));
    }

    @Test
    public void whatIsNoFontIsRefused() throws IOException {
        try {
            Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13}));
            fail("not a font");
        } catch (FontFormatException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            Font.createFont(Font.TYPE1_FONT, new ByteArrayInputStream(fontBytes()));
            fail("Type 1 is not read");
        } catch (FontFormatException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            Font.createFont(7, new ByteArrayInputStream(fontBytes()));
            fail("no such format");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        } catch (FontFormatException wrong) {
            fail("an unknown format is an argument error");
        }
        assertNull(FontFiles.parse(new byte[0]));
        assertNull(FontFiles.parse(new byte[64]));
    }

    @Test
    public void bytesNoResourceMatchesStillMakeAFontWithItsNames() throws Exception {
        Resources.cn1ClearIndex();
        FontFiles.reset();
        Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontBytes()));
        assertEquals("Material Icons", font.getFamily());
        assertNull(font.cn1Face().file());
        assertNull("drawn with the platform's typeface", font.cn1Face().base());
        assertNotNull(Fonts.nativeFont(font.deriveFont(20f), 40));
    }

    /// Paints one glyph of the font in red.
    private static final class Glyph extends JPanel {

        final Font font;
        final String glyph = String.valueOf((char) 0xe5cd);

        Glyph(Font font) {
            this.font = font;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.white);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(font);
            g.setColor(Color.red);
            g.drawString(glyph, 10, 40);
        }
    }

    @Test
    public void aGlyphOfTheFontIsDrawn() throws Exception {
        Font icons = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontBytes())).deriveFont(24f);
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        Glyph panel = new Glyph(icons);
        f.add(panel, BorderLayout.CENTER);
        show(f);
        List<Object[]> text = paint(f);
        assertNotNull("the glyph was handed to the platform", find(text, panel.glyph));
        int[][] rows = raster(f);
        assertTrue("and painted in the color set", count(rows, panel, 0, 0, 80, 60, 0xff0000) > 0);

        // The graphics of an image, as an icon pack makes its icons.
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(img);
        assertNotNull(g);
        g.setFont(icons);
        assertSame(icons, g.getFont());
        g.setColor(Color.red);
        g.drawString(panel.glyph, 0, 24);
        g.dispose();
    }
}
