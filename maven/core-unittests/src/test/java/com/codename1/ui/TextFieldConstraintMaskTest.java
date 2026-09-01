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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A modifier bit must not turn a typed constraint back into "anything goes".
 */
class TextFieldConstraintMaskTest extends UITestBase {

    @FormTest
    void aHintBesideNumericStillRestrictsToDigits() {
        // the documented way to mark a code field is NUMERIC | ONE_TIME_CODE, and the
        // lightweight editing path compared the whole constraint, so the hint beside the
        // base type turned a digits-only field into one that took letters
        TextField f = new TextField();
        f.setConstraint(TextArea.NUMERIC | TextArea.ONE_TIME_CODE);
        assertTrue(f.validChar("7"));
        assertFalse(f.validChar("a"));
    }

    @FormTest
    void everyOtherModifierBehavesTheSameWay() {
        TextField f = new TextField();
        f.setConstraint(TextArea.NUMERIC | TextArea.SENSITIVE);
        assertFalse(f.validChar("a"));
        f.setConstraint(TextArea.PHONENUMBER | TextArea.NON_PREDICTIVE);
        assertTrue(f.validChar("+"));
        assertFalse(f.validChar("a"));
    }

    @FormTest
    void aPlainConstraintIsUnaffected() {
        TextField f = new TextField();
        f.setConstraint(TextArea.ANY);
        assertTrue(f.validChar("a"));
        f.setConstraint(TextArea.NUMERIC);
        assertFalse(f.validChar("a"));
    }
}
