/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.rules.forge;

/// A file a pack uses that is not part of the app: an uploaded glTF character or
/// a portrait. It is named by the SHA-256 of its bytes, so a pack's hash covers the
/// exact files it was published with, and the same file uploaded twice is stored once.
public final class AssetRef {
    public static final String MODEL = "model";
    public static final String IMAGE = "image";

    /// Lower-case hex SHA-256 of the bytes.
    public String sha256 = "";
    public String kind = MODEL;
    public String name = "";
    public int bytes;

    /// How a level's definitions refer to it, in their `art` field.
    public String ref() {
        return "asset:" + sha256;
    }
}
