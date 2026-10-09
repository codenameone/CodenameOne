/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.codename1.compat.jdk.Resources;
import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.FontFiles;
import com.codename1.fxcompat.runtime.ResourceUrls;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/// A font file the application carries is loaded: `Font.loadFont` answers
/// a font named as the file names itself, and the family then finds the
/// file. The font is the Material Icons file the framework ships.
public class FontLoadingTest {

    private static final String PATH = "com/example/fonts/Icons.ttf";
    private static final String FLAT = "com__example__fonts__Icons.ttf";

    @Rule
    public MainThreadRule edt = new MainThreadRule();

    private final List<String> opened = new ArrayList<String>();

    private static byte[] fontBytes() throws IOException {
        InputStream in = FontLoadingTest.class.getResourceAsStream("/material-design-font.ttf");
        assertNotNull("the framework's icon font is on the class path", in);
        try {
            return ResourceUrls.readAll(in);
        } finally {
            in.close();
        }
    }

    @Before
    public void shipTheFont() throws IOException {
        HeadlessImplementation.install();
        HeadlessImplementation.pixelImages = true;
        final byte[] font = fontBytes();
        FontFiles.reset();
        Resources.cn1BeginIndex();
        Resources.cn1AddResource(PATH);
        Resources.cn1AddResource("com/example/img/dot.png");
        Resources.cn1SetProvider(new Resources.Provider() {
            @Override
            public InputStream open(String flatName) {
                opened.add(flatName);
                if (FLAT.equals(flatName)) {
                    return new ByteArrayInputStream(font);
                }
                if ("com__example__img__dot.png".equals(flatName)) {
                    return FontLoadingTest.class.getResourceAsStream("/dot.png");
                }
                return null;
            }
        });
    }

    @After
    public void forget() {
        HeadlessImplementation.pixelImages = false;
        Resources.cn1SetProvider(null);
        Resources.cn1ClearIndex();
        FontFiles.reset();
    }

    @Test
    public void aFontFileAnswersAFontNamedAsTheFileNamesItself() {
        Font font = Font.loadFont("cn1res:/" + PATH, 18);
        assertNotNull(font);
        assertEquals("Material Icons", font.getFamily());
        assertEquals("Material Icons", font.getName());
        assertEquals("Regular", font.getStyle());
        assertEquals(18, font.getSize(), 0);
        assertNotNull(font.cn1Native());
        FontFiles.Face face = FontFiles.find("material icons", 400, false);
        assertNotNull("the family finds the file", face);
        // The platform is handed the flat name the build shipped, and the
        // name the font is installed under.
        assertEquals(FLAT, face.file());
        assertEquals("MaterialIcons-Regular", face.postScriptName());
        assertSame(face, FontFiles.find("MaterialIcons-Regular", 700, true));
        assertNull(FontFiles.find("Clear Sans", 400, false));
    }

    @Test
    public void aDesktopUrlFindsTheSameResource() {
        assertNotNull(Font.loadFont("jar:file:/opt/app/app.jar!/" + PATH, 10));
        FontFiles.reset();
        assertNotNull(Font.loadFont("file:/home/me/build/classes/" + PATH, 10));
    }

    @Test
    public void whatIsNoFontIsNull() {
        assertNull(Font.loadFont("cn1res:/com/example/fonts/Missing.ttf", 10));
        assertNull(Font.loadFont(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13}), 10));
        assertNull(Font.loadFont((String) null, 10));
    }

    @Test
    public void aStreamIsMatchedToTheResourceItCameFrom() throws IOException {
        Font font = Font.loadFont(new ByteArrayInputStream(fontBytes()), 12);
        assertNotNull(font);
        assertEquals("Material Icons", font.getFamily());
        FontFiles.Face face = FontFiles.find("Material Icons", 400, false);
        assertNotNull(face);
        assertEquals(FLAT, face.file());
    }

    @Test
    public void bytesThatAreNoTableDirectoryAreRefused() {
        assertNull(FontFiles.parse(new byte[0]));
        assertNull(FontFiles.parse(new byte[64]));
        byte[] truncated = new byte[20];
        truncated[1] = 1;
        truncated[5] = 9;
        assertNull(FontFiles.parse(truncated));
    }

    /// An image named by the URL of a nested resource is read under the
    /// flat name, never asked of the platform as a path with directories.
    @Test
    public void anImageUrlIsReadThroughTheResourceIndex() {
        opened.clear();
        Image image = new Image("cn1res:/com/example/img/dot.png");
        assertEquals(opened.toString(), true, opened.contains("com__example__img__dot.png"));
        assertEquals(false, image.isError());
        assertEquals(2, image.getWidth(), 0);
    }
}
