/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.components;

import com.codename1.ui.EncodedImage;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An encoded image that stores the actual data of the encoded image in a disk file
 * or resource and loads it only when necessary. The huge advantage is that RAM usage
 * is practically none-existant, it is potentially very slow in worst case scenarios
 * and has the other drawback of requiring the width/height in advanced to work properly.
 *
 * @author Shai Almog
 */
public class FileEncodedImage extends EncodedImage {
    private String fileName;
    private boolean keep;
    private byte[] data;
    private FileEncodedImage(String fileName, int w, int h, boolean keep) {
        super(w, h);
        this.fileName = fileName;
        this.keep = keep;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getImageData() {
        if(data != null) {
            return data;
        }
        InputStream i = null;
        try {
            byte[] imageData = new byte[(int) FileSystemStorage.getInstance().getLength(fileName)];
            i = FileSystemStorage.getInstance().openInputStream(fileName);
            Util.readFully(i, imageData);
            if(keep) {
                data = imageData;
            }
            return imageData;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            Util.cleanup(i);
        }
    }

    /**
     * Creates an encoded image that maps to a local file thus allowing to
     * seamlessly fetch files as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the file
     * @param width the width of the file or -1 if unknown (notice that this will improve performance)
     * @param height the height of the file or -1 if unknown (notice that this will improve performance)
     * @return image that will load the file seamlessly
     */
    public static FileEncodedImage create(String fileName, int width, int height) {
        return new FileEncodedImage(fileName, width, height, true);
    }

    /**
     * Creates an encoded image that maps to a local file thus allowing to
     * seamlessly fetch files as needed. This only works reasonably well for very small
     * files. This version of the method creates the file from an input stream
     *
     * @param fileName the name of the file
     * @param i input stream from which to create the file
     * @param width the width of the file or -1 if unknown (notice that this will improve performance)
     * @param height the height of the file or -1 if unknown (notice that this will improve performance)
     * @return image that will load the file seamlessly
     */
    public static FileEncodedImage create(String fileName, InputStream i, int width, int height) throws IOException {
        EncodedImage e = EncodedImage.create(i);
        FileEncodedImage f = new FileEncodedImage(fileName, width, height, true);
        f.data = e.getImageData();
        OutputStream o = FileSystemStorage.getInstance().openOutputStream(fileName);
        o.write(f.data);
        o.close();
        return f;
    }

    /**
     * Creates an encoded image that maps to a local file thus allowing to
     * seamlessly fetch files as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the file
     * @param width the width of the file or -1 if unknown (notice that this will improve performance)
     * @param height the height of the file or -1 if unknown (notice that this will improve performance)
     * @param keep if set to true keeps the file in RAM once loaded
     * @return image that will load the file seamlessly
     */
    public static FileEncodedImage create(String fileName, int width, int height, boolean keep) {
        return new FileEncodedImage(fileName, width, height, keep);
    }
}
