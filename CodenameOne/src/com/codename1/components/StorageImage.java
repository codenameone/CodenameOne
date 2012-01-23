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
import com.codename1.io.Storage;
import java.io.IOException;
import java.io.InputStream;

/**
 * An encoded image that stores the actual data of the encoded image in storage.
 *
 * @author Shai Almog
 */
public class StorageImage extends EncodedImage {
    private String fileName;
    private boolean keep;
    private byte[] data;

    
    private StorageImage(String fileName, int w, int h, boolean keep) {
        super(w, h);
        this.fileName = fileName;
        this.keep = keep;
    }

    /**
     * @inheritDoc
     */
    public byte[] getImageData() {
        if(data != null) {
            return data;
        }
        byte[] imageData = (byte[])Storage.getInstance().readObject(fileName);
        if(keep) {
            data = imageData;
        }
        return imageData;
    }

    /**
     * Creates an encoded image that maps to a storage file thus allowing to
     * seamlessly cache images as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the storage file
     * @param data the data
     * @param width the width of the file or -1 if unknown (notice that this will improve performance)
     * @param height the height of the file or -1 if unknown (notice that this will improve performance)
     * @return image that will load the file seamlessly or null if the storage failed
     */
    public static StorageImage create(String fileName, byte[] data, int width, int height) {
        if(Storage.getInstance().writeObject(fileName, data)){
            return new StorageImage(fileName, width, height, true);
        }
        return null;
    }

    /**
     * Creates an encoded image that maps to a storage file thus allowing to
     * seamlessly cache images as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the storage file
     * @param data the stream to cache
     * @param width the width of the file or -1 if unknown (notice that this will improve performance)
     * @param height the height of the file or -1 if unknown (notice that this will improve performance)
     * @return image that will load the file seamlessly
     */
    public static StorageImage create(String fileName, InputStream data, int width, int height) throws IOException {
        EncodedImage e = EncodedImage.create(data);
        return create(fileName, e.getImageData(), width, height);
    }

    /**
     * Creates an encoded image that maps to a storage file thus allowing to
     * seamlessly cache images as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the storage file
     * @param width the width of the file or -1 if unknown (notice that this will improve performance)
     * @param height the height of the file or -1 if unknown (notice that this will improve performance)
     * @return image that will load the file seamlessly
     */
    public static StorageImage create(String fileName, int width, int height) {
        return new StorageImage(fileName, width, height, true);
    }


    /**
     * Creates an encoded image that maps to a storage file thus allowing to
     * seamlessly cache images as needed. This only works reasonably well for very small
     * files.
     *
     * @param fileName the name of the storage file
     * @param width the width of the file or -1 if unknown (notice that this will improve performance)
     * @param height the height of the file or -1 if unknown (notice that this will improve performance)
     * @param keep if set to true keeps the file in RAM once loaded
     * @return image that will load the file seamlessly
     */
    public static StorageImage create(String fileName, int width, int height, boolean keep) {
        return new StorageImage(fileName, width, height, keep);
    }
}
