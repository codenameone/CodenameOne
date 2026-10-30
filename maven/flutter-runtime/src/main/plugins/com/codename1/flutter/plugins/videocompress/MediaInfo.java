/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.videocompress;

/** video_compress's MediaInfo. Title, author and orientation are never known. */
public class MediaInfo {
    String path;
    Long width;
    Long height;
    Long filesize;
    Double duration;

    MediaInfo(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public String title() {
        return null;
    }

    public String author() {
        return null;
    }

    public Long width() {
        return width;
    }

    public Long height() {
        return height;
    }

    public Long orientation() {
        return null;
    }

    public Long filesize() {
        return filesize;
    }

    /** Milliseconds, as the plugin reports it. */
    public Double duration() {
        return duration;
    }

    public Boolean isCancel() {
        return Boolean.FALSE;
    }

    public dart.io.File file() {
        return path == null ? null : new dart.io.File(path);
    }
}
