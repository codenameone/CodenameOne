/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * An {@link ImageProvider} that wraps another provider and resizes its decoded
 * image to a target {@code width}/{@code height} — Flutter's {@code ResizeImage}.
 * The wrapped provider's identity is threaded into {@link #sourceKey()} so the
 * framework still detects source changes across in-place updates.
 */
public class ResizeImage extends ImageProvider {

    private final ImageProvider imageProvider;
    private Long width;
    private Long height;
    private Object policy;
    private boolean allowUpscaling;

    public ResizeImage(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    /** Named parameter setter for the Dart {@code width:} parameter. */
    public void width(long v) {
        this.width = v;
    }

    /** Named parameter setter for the Dart {@code height:} parameter. */
    public void height(long v) {
        this.height = v;
    }

    /** Named parameter setter for the Dart {@code policy:} parameter. */
    public void policy(Object v) {
        this.policy = v;
    }

    /** Named parameter setter for the Dart {@code allowUpscaling:} parameter. */
    public void allowUpscaling(boolean v) {
        this.allowUpscaling = v;
    }

    public ImageProvider getImageProvider() {
        return imageProvider;
    }

    public Long getWidth() {
        return width;
    }

    public Long getHeight() {
        return height;
    }

    @Override
    public String sourceKey() {
        String inner = imageProvider == null ? "null" : imageProvider.sourceKey();
        return "resize:" + width + "x" + height + ":" + inner;
    }
}
