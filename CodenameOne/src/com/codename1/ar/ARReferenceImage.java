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
package com.codename1.ar;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;

/// A 2D image the AR session should recognize in the real world - a poster, a
/// game board, a product label. Register reference images through
/// `ARSessionOptions#referenceImages(ARReferenceImage[])`; when the camera
/// sees one, the session delivers an `ARImageAnchor` carrying this image's
/// name.
///
/// The physical width tells the platform how large the printed image is in the
/// real world, which it needs to estimate the image's distance and pose
/// accurately.
public final class ARReferenceImage {
    private final String name;
    private final byte[] encodedImage;
    private final float physicalWidthMeters;

    /// Creates a reference image from encoded (PNG or JPEG) bytes.
    ///
    /// #### Parameters
    ///
    /// - `name`: a unique name identifying this image in `ARImageAnchor`s
    ///
    /// - `encodedImage`: the PNG or JPEG bytes of the image to detect
    ///
    /// - `physicalWidthMeters`: the width of the printed image in meters
    public ARReferenceImage(String name, byte[] encodedImage, float physicalWidthMeters) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name is required");
        }
        if (encodedImage == null || encodedImage.length == 0) {
            throw new IllegalArgumentException("encodedImage is required");
        }
        if (physicalWidthMeters <= 0.0f) {
            throw new IllegalArgumentException("physicalWidthMeters must be positive");
        }
        this.name = name;
        byte[] copy = new byte[encodedImage.length];
        System.arraycopy(encodedImage, 0, copy, 0, encodedImage.length);
        this.encodedImage = copy;
        this.physicalWidthMeters = physicalWidthMeters;
    }

    /// Creates a reference image from an `Image`, encoding it as PNG.
    ///
    /// #### Parameters
    ///
    /// - `name`: a unique name identifying this image in `ARImageAnchor`s
    ///
    /// - `image`: the image to detect
    ///
    /// - `physicalWidthMeters`: the width of the printed image in meters
    public ARReferenceImage(String name, Image image, float physicalWidthMeters) {
        this(name, encode(image), physicalWidthMeters);
    }

    private static byte[] encode(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("image is required");
        }
        if (image instanceof EncodedImage) {
            return ((EncodedImage) image).getImageData();
        }
        return EncodedImage.createFromImage(image, false).getImageData();
    }

    /// The unique name identifying this image.
    public String getName() {
        return name;
    }

    /// The encoded (PNG or JPEG) bytes of the image as a newly allocated
    /// array.
    public byte[] getEncodedImage() {
        byte[] copy = new byte[encodedImage.length];
        System.arraycopy(encodedImage, 0, copy, 0, encodedImage.length);
        return copy;
    }

    /// The width of the printed image in meters.
    public float getPhysicalWidthMeters() {
        return physicalWidthMeters;
    }
}
