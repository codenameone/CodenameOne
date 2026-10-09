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
package com.codename1.compat.jdk;

import java.io.IOException;

/// `java.nio.file.FileSystemException` for the Codename One runtime.
public class FileSystemException extends IOException {
    private static final long serialVersionUID = 1L;
    private final String file;
    private final String other;
    private final String reason;

    public FileSystemException(String file) {
        this(file, null, null);
    }

    public FileSystemException(String file, String other, String reason) {
        super((String) null);
        this.file = file;
        this.other = other;
        this.reason = reason;
    }

    public String getFile() {
        return file;
    }

    public String getOtherFile() {
        return other;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getMessage() {
        if (file == null && other == null) {
            return reason;
        }
        StringBuilder sb = new StringBuilder();
        if (file != null) {
            sb.append(file);
        }
        if (other != null) {
            sb.append(" -> ").append(other);
        }
        if (reason != null) {
            sb.append(": ").append(reason);
        }
        return sb.toString();
    }
}
