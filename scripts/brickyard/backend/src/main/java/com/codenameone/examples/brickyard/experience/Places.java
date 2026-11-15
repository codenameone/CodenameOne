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
package com.codenameone.examples.brickyard.experience;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.rbx.Engine;
import com.codename1.rbx.place.CompatReport;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceLoader;

/// Checks an uploaded place file by doing what a game server will do with it:
/// parse it, then load it into a server-side engine. Loading builds the
/// instance tree and reports what the engine does not support; it does not run
/// the place's scripts.
@Component
public class Places {
    /// The largest place file accepted.
    public static final int MAX_BYTES = 16 * 1024 * 1024;

    /// The compatibility report for `bytes`, or a 400 when they are not a place.
    public String validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(400, "The place file is empty");
        }
        if (bytes.length > MAX_BYTES) {
            throw new ResponseStatusException(413, "The place file is too large");
        }
        PlaceFile file;
        try {
            file = PlaceFile.read(bytes);
        } catch (RuntimeException notAPlace) {
            throw new ResponseStatusException(400, "That is not a place file: " + notAPlace.getMessage());
        }
        if (file == null) {
            throw new ResponseStatusException(400, "That is not a place file");
        }
        CompatReport report;
        try {
            report = PlaceLoader.loadPlace(new Engine(Engine.SERVER), file);
        } catch (RuntimeException unloadable) {
            throw new ResponseStatusException(400, "The place does not load: " + unloadable.getMessage());
        }
        return report.toString();
    }
}
