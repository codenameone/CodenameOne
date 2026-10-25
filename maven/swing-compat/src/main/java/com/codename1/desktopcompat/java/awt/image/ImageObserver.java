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
package com.codename1.desktopcompat.java.awt.image;

import com.codename1.desktopcompat.java.awt.Image;

/// Told as an image loads. Images of the layer are complete as soon as they
/// exist, so an observer is accepted wherever the API takes one and never
/// called.
public interface ImageObserver {
    int WIDTH = 1;
    int HEIGHT = 2;
    int PROPERTIES = 4;
    int SOMEBITS = 8;
    int FRAMEBITS = 16;
    int ALLBITS = 32;
    int ERROR = 64;
    int ABORT = 128;

    boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height);
}
