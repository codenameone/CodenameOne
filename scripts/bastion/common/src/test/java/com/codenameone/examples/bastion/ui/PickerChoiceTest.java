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
package com.codenameone.examples.bastion.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Pickers choose by id: two things with one label stay two choices.
class PickerChoiceTest {
    @Test
    void repeatedLabelsBecomeDistinct() {
        assertArrayEquals(new String[] {"Rent", "Rent (2)", "Gym", "Rent (3)"},
                Ui.distinct(new String[] {"Rent", "Rent", "Gym", "Rent"}));
    }

    @Test
    void idsAreFoundByValue() {
        String[] ids = {"p1", "p2"};
        assertEquals(1, Ui.indexOf(ids, "p2"));
        assertEquals(-1, Ui.indexOf(ids, null));
        assertEquals(-1, Ui.indexOf(ids, "Rent"));
    }
}
