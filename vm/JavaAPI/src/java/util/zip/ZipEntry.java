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
package java.util.zip;

/** Metadata for a ZIP entry. Directory names end in '/'. */
public class ZipEntry implements Cloneable {
    public static final int STORED = 0, DEFLATED = 8;
    private String name, comment;
    private long time = -1, size = -1, compressedSize = -1, crc = -1;
    private int method = -1;
    private byte[] extra;
    public ZipEntry(String name) {
        if (name == null)
            throw new NullPointerException();
        if (name.length() > 65535)
            throw new IllegalArgumentException("Entry name too long");
        this.name = name;
    }
    public ZipEntry(ZipEntry e) {
        name = e.name;
        comment = e.comment;
        time = e.time;
        size = e.size;
        compressedSize = e.compressedSize;
        crc = e.crc;
        method = e.method;
        extra = e.extra == null ? null : e.extra.clone();
    }
    public String getName() {
        return name;
    }
    public void setTime(long time) {
        this.time = time;
    }
    public long getTime() {
        return time;
    }
    public void setSize(long size) {
        if (size < 0)
            throw new IllegalArgumentException();
        this.size = size;
    }
    public long getSize() {
        return size;
    }
    public void setCompressedSize(long size) {
        compressedSize = size;
    }
    public long getCompressedSize() {
        return compressedSize;
    }
    public void setCrc(long crc) {
        if (crc < 0 || crc > 0xffffffffL)
            throw new IllegalArgumentException();
        this.crc = crc;
    }
    public long getCrc() {
        return crc;
    }
    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED)
            throw new IllegalArgumentException();
        this.method = method;
    }
    public int getMethod() {
        return method;
    }
    public void setExtra(byte[] extra) {
        if (extra != null && extra.length > 65535)
            throw new IllegalArgumentException("Extra too long");
        this.extra = extra;
        if (extra != null)
            for (int at = 0; at + 4 <= extra.length;) {
                int id = (extra[at] & 255) | ((extra[at + 1] & 255) << 8);
                int n = (extra[at + 2] & 255) | ((extra[at + 3] & 255) << 8);
                if (n > extra.length - at - 4)
                    break;
                if (id == 0x5455 && n >= 5 && (extra[at + 4] & 1) != 0) {
                    int seconds = (extra[at + 5] & 255) | ((extra[at + 6] & 255) << 8) | ((extra[at + 7] & 255) << 16)
                            | (extra[at + 8] << 24);
                    time = seconds * 1000L;
                }
                at += 4 + n;
            }
    }
    public byte[] getExtra() {
        return extra;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public String getComment() {
        return comment;
    }
    public boolean isDirectory() {
        return name.endsWith("/");
    }
    public String toString() {
        return name;
    }
    public int hashCode() {
        return name.hashCode();
    }
    public Object clone() {
        return new ZipEntry(this);
    }
}
