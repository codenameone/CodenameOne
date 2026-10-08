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
package com.codename1.androidcompat.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// Persian's cardinal rule is "i = 0 or n = 1", so 0 and 1 select ONE; it
/// was grouped with the languages that only have OTHER.
public class PluralRulesTest {

    @Test
    public void persianZeroAndOneAreSingular() {
        assertEquals(PluralRules.ONE, PluralRules.select("fa", 0));
        assertEquals(PluralRules.ONE, PluralRules.select("fa", 1));
        assertEquals(PluralRules.OTHER, PluralRules.select("fa", 2));
        assertEquals(PluralRules.OTHER, PluralRules.select("ja", 1));
    }
}
