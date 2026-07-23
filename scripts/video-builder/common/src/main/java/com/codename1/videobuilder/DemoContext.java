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
package com.codename1.videobuilder;

import java.nio.file.Path;

/** Immutable viewport and project information supplied to a compiled demo. */
public final class DemoContext {
    private final String orientation;
    private final int width;
    private final int height;
    private final Path projectDirectory;
    private long timelinePositionMs;

    public DemoContext(String orientation, int width, int height, Path projectDirectory) {
        this.orientation = orientation;
        this.width = width;
        this.height = height;
        this.projectDirectory = projectDirectory;
    }

    public String getOrientation() { return orientation; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Path getProjectDirectory() { return projectDirectory; }
    public long getTimelinePositionMs() { return timelinePositionMs; }
    void setTimelinePositionMs(long value) { timelinePositionMs = value; }

    public Path resolveAsset(String relativePath) {
        Path resolved = projectDirectory.resolve(relativePath).normalize();
        if (!resolved.startsWith(projectDirectory)) {
            throw new IllegalArgumentException("Asset escapes project directory: " + relativePath);
        }
        return resolved;
    }
}
