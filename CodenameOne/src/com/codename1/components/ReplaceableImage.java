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

package com.codename1.components;

import com.codename1.ui.EncodedImage;

/**
 * Allows the image data to be replaced at runtime when a different image is 
 * available. The only limitation is that the image width/height must be identical and
 * opacity status can't change (an opaque image can't be made translucent and visa versa).
 *
 * @author Shai Almog
 */
public class ReplaceableImage extends EncodedImage {
    private boolean replaced;
    private byte[] data;
    private boolean opaque;
    private ReplaceableImage(EncodedImage placeholder) {
        super(placeholder.getWidth(), placeholder.getHeight());
        data = placeholder.getImageData();
        opaque = placeholder.isOpaque();
    }

    /**
     * @inheritDoc
     */
    public byte[] getImageData() {
        replaced = false;
        return data;
    }

    /**
     * Replaces the current image with the new image which must match the dimensions
     * etc. of the previous image.
     * @param newImage the image to apply
     */
    public void replace(EncodedImage newImage) {
        data = newImage.getImageData();
        super.resetCache();
        replaced = true;
    }
    
    /**
     * Creates an encoded image that can later be replaced with a different image
     *
     * @param placeholder a temporary image
     * @return image that will be replaceable later on
     */
    public static ReplaceableImage create(EncodedImage placeholder) {
        return new ReplaceableImage(placeholder);
    }

    /**
     * @inheritDoc
     */
    public boolean animate() {
        if(replaced) {
            return true;
        }
        return false;
    }
    
    /**
     * @inheritDoc
     */
    public boolean isAnimation() {
        return true;
    }
    
    /**
     * @inheritDoc
     */
    public boolean isOpaque() {
        return opaque;
    }
}
