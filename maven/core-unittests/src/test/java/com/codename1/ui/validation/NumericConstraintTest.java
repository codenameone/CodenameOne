/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NumericConstraintTest {

    @Test
    public void testNumericConstraintValidation() {
        // Test with minimum and maximum
        NumericConstraint range = new NumericConstraint(true, 5.0, 10.0, "Must be between 5 and 10");
        assertTrue(range.isValid(5.0));
        assertTrue(range.isValid(7.5));
        assertTrue(range.isValid(10.0));
        assertFalse(range.isValid(4.9));
        assertFalse(range.isValid(10.1));
        assertFalse(range.isValid("abc")); // Invalid number

        // Test with only minimum (maximum is NaN)
        NumericConstraint minOnly = new NumericConstraint(true, 5.0, Double.NaN, "Must be > 5");
        assertTrue(minOnly.isValid(5.0));
        assertTrue(minOnly.isValid(100.0));
        assertFalse(minOnly.isValid(4.9));

        // Test with only maximum (minimum is NaN)
        NumericConstraint maxOnly = new NumericConstraint(true, Double.NaN, 10.0, "Must be < 10");
        assertTrue(maxOnly.isValid(10.0));
        assertTrue(maxOnly.isValid(-100.0));
        assertFalse(maxOnly.isValid(10.1));

        // Test with no limits (both NaN)
        NumericConstraint noLimit = new NumericConstraint(true, Double.NaN, Double.NaN, "Any number");
        assertTrue(noLimit.isValid(Double.MIN_VALUE));
        assertTrue(noLimit.isValid(Double.MAX_VALUE));
        assertTrue(noLimit.isValid(0));
    }
}
