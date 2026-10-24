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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.geom.Ellipse2D;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;
import com.codename1.desktopcompat.java.awt.geom.RoundRectangle2D;
import com.codename1.desktopcompat.javax.swing.RowFilter;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// Values the JDK's AWT and Swing classes answer, held against the real ones
/// where this JDK has them.
public class ValueParityTest {

    private static String decoded(boolean jdk, String text) {
        try {
            if (jdk) {
                java.awt.Color c = java.awt.Color.decode(text);
                return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
            }
            Color c = Color.decode(text);
            return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
        } catch (NumberFormatException e) {
            return "NumberFormatException";
        }
    }

    /// `Color.decode` reads a number as `Integer.decode` does: a sign before
    /// the prefix, and nothing beyond a signed 32-bit int.
    @Test
    public void decodeReadsNumbersAsTheJdkDoes() {
        String[] cases = {"0xFF0000", "#00ff00", "255", "010", "0", "-0x1", "+0xFF0000", "-#10", "-1", "+12",
            "0x100000000", "4294967296", "-0x80000000", "0x7fffffff", "--1", "-+1", "0x-1", "#", "-", "", "0x",
            " 1", "1 ", "+"};
        for (String c : cases) {
            assertEquals("\"" + c + "\"", decoded(true, c), decoded(false, c));
        }
    }

    private static final class Row extends RowFilter.Entry<Object, Integer> {
        private final Object value;

        Row(Object value) {
            this.value = value;
        }

        @Override
        public Object getModel() {
            return null;
        }

        @Override
        public int getValueCount() {
            return 1;
        }

        @Override
        public Object getValue(int index) {
            return value;
        }

        @Override
        public Integer getIdentifier() {
            return 0;
        }
    }

    /// Two numbers of one class compare as that class does: past 2^53 a
    /// double no longer tells neighbours apart.
    @Test
    public void aNumberFilterComparesBigNumbersExactly() {
        BigInteger big = new BigInteger("9007199254740992");
        RowFilter<Object, Integer> equal = RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, big, 0);
        assertTrue(equal.include(new Row(new BigInteger("9007199254740992"))));
        assertFalse(equal.include(new Row(new BigInteger("9007199254740993"))));
        RowFilter<Object, Integer> after = RowFilter.numberFilter(RowFilter.ComparisonType.AFTER,
                new BigDecimal("0.10000000000000000001"), 0);
        assertTrue(after.include(new Row(new BigDecimal("0.10000000000000000002"))));
        assertFalse(after.include(new Row(new BigDecimal("0.1"))));
        // Across classes the comparison is by magnitude, as before.
        assertTrue(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 3, 0).include(new Row(2.5)));
        assertTrue(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, 3, 0).include(new Row(3L)));
    }

    /// A shape of negative size has no outline: the JDK's iterator is done
    /// at once, and the empty intersection of two rectangles is one.
    @Test
    public void aNegativeSizeHasNoOutline() {
        Rectangle2D none = new Rectangle2D.Double(0, 0, 10, 10)
                .createIntersection(new Rectangle2D.Double(20, 20, 5, 5));
        assertTrue(none.getWidth() < 0);
        assertTrue(none.getPathIterator(null).isDone());
        assertTrue(new Ellipse2D.Double(0, 0, -4, 4).getPathIterator(null).isDone());
        assertTrue(new RoundRectangle2D.Double(0, 0, 4, -4, 1, 1).getPathIterator(null).isDone());
        assertTrue(new java.awt.geom.Rectangle2D.Double(0, 0, 10, 10)
                .createIntersection(new java.awt.geom.Rectangle2D.Double(20, 20, 5, 5))
                .getPathIterator(null).isDone());
        assertFalse(new Rectangle2D.Double(0, 0, 0, 0).getPathIterator(null).isDone());
        assertFalse(new java.awt.geom.Rectangle2D.Double(0, 0, 0, 0).getPathIterator(null).isDone());
    }
}
