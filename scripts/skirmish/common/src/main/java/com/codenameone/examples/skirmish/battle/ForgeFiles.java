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
package com.codenameone.examples.skirmish.battle;

import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.util.Base64;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// The files Forge packs use -- uploaded models and images -- kept on the
/// device by their SHA-256, so each is downloaded once however many packs
/// share it.
public final class ForgeFiles {
    private ForgeFiles() {
    }

    private static String key(String sha) {
        return "forge-file-" + sha;
    }

    public static boolean has(String sha) {
        return Storage.getInstance().exists(key(sha));
    }

    /// The file's bytes as a stream, or null when it is not on the device.
    public static InputStream open(String sha) {
        byte[] b = bytes(sha);
        return b == null ? null : new ByteArrayInputStream(b);
    }

    public static byte[] bytes(String sha) {
        if (!has(sha)) {
            return null;
        }
        try (InputStream in = Storage.getInstance().createInputStream(key(sha))) {
            return Util.readInputStream(in);
        } catch (java.io.IOException e) {
            Log.e(e);
            return null;
        }
    }

    public static void put(String sha, byte[] data) {
        try (OutputStream out = Storage.getInstance().createOutputStream(key(sha))) {
            out.write(data);
        } catch (java.io.IOException e) {
            Log.e(e);
        }
    }

    /// Downloads every file of `shas` the device does not have yet, one after
    /// another, then runs `done`.
    public static void fetch(List<String> shas, final Runnable done, final Net.Failed failed) {
        final List<String> missing = new ArrayList<String>();
        for (String s : shas) {
            if (!has(s) && !missing.contains(s)) {
                missing.add(s);
            }
        }
        next(missing, done, failed);
    }

    private static void next(final List<String> missing, final Runnable done, final Net.Failed failed) {
        if (missing.isEmpty()) {
            done.run();
            return;
        }
        final String sha = missing.remove(0);
        Api.forge().asset(sha, Net.to(a -> {
            if (a.data() != null) {
                put(sha, Base64.decode(a.data().getBytes()));
            }
            next(missing, done, failed);
        }, failed));
    }
}
