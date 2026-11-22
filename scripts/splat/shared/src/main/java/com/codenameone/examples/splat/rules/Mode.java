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
package com.codenameone.examples.splat.rules;

/// The game modes. Every match also ends at [Match#TIME_LIMIT].
public enum Mode {
    /// Respawning team play; first team to `target` paints wins.
    TEAM_PAINT("Team Paint", "Operation: Weekend Warrior", 25),
    /// Bring the other team's flag to your base. Yours need not be home: this is a
    /// weekend league, and nobody wanted to wait.
    CAPTURE_THE_FLAG("Capture the Flag", "Operation: Borrowed Bedsheet", 3),
    /// Hold the snack bar with no enemy in it; a point per second held.
    SNACK_BAR("Hold the Snack Bar", "Operation: Nacho Dawn", 60);

    public final String title;
    /// The mission name the briefing reads out, in the gravest voice available.
    public final String operation;
    public final int target;

    Mode(String title, String operation, int target) {
        this.title = title;
        this.operation = operation;
        this.target = target;
    }

    public static Mode byName(String name) {
        Mode[] all = values();
        for (int i = 0; i < all.length; i++) {
            if (all[i].name().equals(name)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("mode " + name);
    }
}
