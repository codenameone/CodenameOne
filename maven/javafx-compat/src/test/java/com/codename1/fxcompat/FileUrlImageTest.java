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
import static org.junit.Assert.fail;

import com.codename1.compat.jdk.Resources;
import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.ResourceUrls;
import com.codename1.io.FileSystemStorage;
import java.io.IOException;
import java.io.InputStream;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/// An image named by a `file:` URL never throws out of its constructor.
///
/// A port's file system throws `IllegalArgumentException` for any path
/// that is not an absolute `file:/` one, and the headless implementation
/// does the same, so a relative URL handed over as written kills the
/// application in `start`. JavaFX answers an image in error instead.
public class FileUrlImageTest {

    @Rule
    public MainThreadRule edt = new MainThreadRule();

    private static byte[] dot() throws IOException {
        InputStream in = FileUrlImageTest.class.getResourceAsStream("/dot.png");
        assertNotNull(in);
        try {
            return ResourceUrls.readAll(in);
        } finally {
            in.close();
        }
    }

    @Before
    public void start() {
        HeadlessImplementation.install();
        HeadlessImplementation.pixelImages = true;
        HeadlessImplementation.fileSystem = true;
        HeadlessImplementation.FILES.clear();
    }

    @After
    public void forget() {
        HeadlessImplementation.pixelImages = false;
        HeadlessImplementation.fileSystem = false;
        HeadlessImplementation.FILES.clear();
        Resources.cn1SetProvider(null);
        Resources.cn1ClearIndex();
    }

    @Test
    public void theFileSystemRefusesARelativePathAsAPortDoes() throws IOException {
        try {
            FileSystemStorage.getInstance().openInputStream("file:resources/images/address_book_32.png");
            fail("a port throws for a path that is not absolute");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            FileSystemStorage.getInstance().exists("records.txt");
            fail("a port throws for a path that is not absolute");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void aRelativeFileUrlThatNamesNothingIsAnImageInError() {
        Image image = new Image("file:resources/images/address_book_32.png");
        assertEquals(true, image.isError());
        assertNotNull(image.getException());
        assertEquals(0, image.getWidth(), 0);
        assertEquals(0, image.getHeight(), 0);
        assertNull(image.getPixelReader());
        // And through the view that takes a URL.
        assertEquals(true, new ImageView("file:resources/images/address_book_32.png").getImage().isError());
    }

    @Test
    public void anAbsoluteFileUrlThatNamesNothingIsAnImageInError() {
        assertEquals(true, new Image("file:/nowhere/at/all.png").isError());
        assertEquals(true, new Image("file:///nowhere/at/all.png").isError());
        assertEquals(true, new Image("file:").isError());
    }

    @Test
    public void aRelativeFileUrlIsReadFromTheApplicationHome() throws IOException {
        String home = FileSystemStorage.getInstance().getAppHomePath();
        HeadlessImplementation.FILES.put(home + "pictures/dot.png", dot());
        Image image = new Image("file:pictures/dot.png");
        assertEquals(false, image.isError());
        assertEquals(2, image.getWidth(), 0);
    }

    @Test
    public void anAbsoluteFileUrlIsReadFromTheFileSystem() throws IOException {
        HeadlessImplementation.FILES.put("file:///data/dot.png", dot());
        assertEquals(false, new Image("file:/data/dot.png").isError());
        assertEquals(false, new Image("file:///data/dot.png").isError());
    }

    @Test
    public void aRelativeFileUrlFindsABundledResource() {
        Resources.cn1BeginIndex();
        Resources.cn1AddResource("resources/images/dot.png");
        Resources.cn1SetProvider(new Resources.Provider() {
            @Override
            public InputStream open(String flatName) {
                if ("resources__images__dot.png".equals(flatName)) {
                    return FileUrlImageTest.class.getResourceAsStream("/dot.png");
                }
                return null;
            }
        });
        Image image = new Image("file:resources/images/dot.png");
        assertEquals(false, image.isError());
        assertEquals(2, image.getWidth(), 0);
    }
}
