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
package com.codenameone.examples.bazaar.pos;

/// The shift's sales against its target, and the milestones on the way.
///
/// Counted on the register itself, sale by sale, so the bar moves the moment
/// a sale completes, offline too. A new shift starts from what the server
/// says the shift already sold. No UI here.
final class ShiftGoal {
    /// The target when the store set none: $1,500.00 a shift.
    static final long DEFAULT_TARGET = 150000;
    /// Quarter marks, in percent; the last one is the goal itself.
    static final int[] MILESTONES = {25, 50, 75, 100};

    private String shiftId = "";
    private long soldCents;
    private long targetCents = DEFAULT_TARGET;

    /// Starts counting for `shift`, from `alreadySold`; the same shift again
    /// keeps the larger of the two, so a late server total never takes back
    /// sales rung here.
    void begin(String shift, long alreadySold) {
        String id = shift == null ? "" : shift;
        if (!id.equals(shiftId)) {
            shiftId = id;
            soldCents = Math.max(0, alreadySold);
        } else {
            soldCents = Math.max(soldCents, alreadySold);
        }
    }

    void target(long cents) {
        targetCents = cents > 0 ? cents : DEFAULT_TARGET;
    }

    long target() {
        return targetCents;
    }

    long sold() {
        return soldCents;
    }

    String shift() {
        return shiftId;
    }

    /// Adds a sale and answers the milestone it crossed (25, 50, 75 or 100),
    /// or 0 when it crossed none. A sale that jumps two marks answers the
    /// higher one: one celebration, not two.
    int record(long saleCents) {
        int before = percent();
        soldCents += Math.max(0, saleCents);
        int after = percent();
        int crossed = 0;
        for (int iter = 0; iter < MILESTONES.length; iter++) {
            if (before < MILESTONES[iter] && after >= MILESTONES[iter]) {
                crossed = MILESTONES[iter];
            }
        }
        return crossed;
    }

    /// Progress in whole percent, capped at 100.
    int percent() {
        if (targetCents <= 0) {
            return 0;
        }
        long p = soldCents * 100 / targetCents;
        return (int) Math.min(100, p);
    }

    /// Progress in thousandths, capped at 1000, for a smooth bar.
    int permille() {
        if (targetCents <= 0) {
            return 0;
        }
        return (int) Math.min(1000, soldCents * 1000 / targetCents);
    }

    /// As text, for storage: "shift|sold|target".
    String save() {
        return shiftId + "|" + soldCents + "|" + targetCents;
    }

    /// Back from [#save()]; anything unreadable leaves a fresh goal.
    static ShiftGoal load(String text) {
        ShiftGoal g = new ShiftGoal();
        if (text == null) {
            return g;
        }
        int a = text.indexOf('|');
        int b = a < 0 ? -1 : text.indexOf('|', a + 1);
        if (a < 0 || b < 0) {
            return g;
        }
        try {
            long sold = Long.parseLong(text.substring(a + 1, b));
            long target = Long.parseLong(text.substring(b + 1));
            g.shiftId = text.substring(0, a);
            g.soldCents = Math.max(0, sold);
            g.target(target);
        } catch (NumberFormatException bad) {
            return new ShiftGoal();
        }
        return g;
    }
}
