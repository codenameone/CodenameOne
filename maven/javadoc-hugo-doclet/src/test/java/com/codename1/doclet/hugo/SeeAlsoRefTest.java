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
package com.codename1.doclet.hugo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Entries copied from real {@code #### See also} lists in the framework sources. */
class SeeAlsoRefTest {

    @Test
    void readsALocalMemberReference() {
        SeeAlsoRef r = SeeAlsoRef.parse("#drawRoundRect");
        assertEquals("", r.type());
        assertEquals("drawRoundRect", r.member());
        assertFalse(r.hasParameterList(), "no list written means any overload will do");
        assertEquals("", r.label());
    }

    @Test
    void readsAQualifiedMemberReference() {
        // 426 entries in the tree take this shape and every one of them used to
        // render unlinked, because the whole string went to a type-only lookup.
        SeeAlsoRef r = SeeAlsoRef.parse("Display#supportsNativeImageCache()");
        assertEquals("Display", r.type());
        assertEquals("supportsNativeImageCache", r.member());
        assertTrue(r.hasParameterList());
        assertEquals(List.of(), r.parameters());
    }

    @Test
    void separatesTrailingProseFromTheReference() {
        SeeAlsoRef r = SeeAlsoRef.parse(
                "#isShapeSupported(java.lang.Object) to determine if the context supports drawing");
        assertEquals("isShapeSupported", r.member());
        assertEquals(List.of("java.lang.Object"), r.parameters());
        assertEquals("to determine if the context supports drawing", r.label());
    }

    @Test
    void doesNotSplitOnASpaceInsideTheSignature() {
        SeeAlsoRef r = SeeAlsoRef.parse("Component#paintShadows(Graphics, int, int)");
        assertEquals("Component", r.type());
        assertEquals(List.of("Graphics", "int", "int"), r.parameters());
        assertEquals("", r.label());
    }

    @Test
    void keepsGenericArgumentsTogether() {
        SeeAlsoRef r = SeeAlsoRef.parse("URLImage#create(Map<String, List<Integer>>, int)");
        assertEquals(List.of("Map<String, List<Integer>>", "int"), r.parameters());
    }

    @Test
    void readsABareTypeName() {
        SeeAlsoRef r = SeeAlsoRef.parse("BackgroundWork");
        assertEquals("BackgroundWork", r.type());
        assertEquals("", r.member());
        assertTrue(r.isReference());
    }

    @Test
    void treatsASentenceAsProse() {
        SeeAlsoRef r = SeeAlsoRef.parse("ButtonList for code samples;");
        assertTrue(r.isReference(), "the first word is still a type name");
        assertEquals("ButtonList", r.type());
        assertEquals("for code samples;", r.label());
    }

    @Test
    void leavesAMarkdownLinkAlone() {
        SeeAlsoRef r = SeeAlsoRef.parse("[the guide](/developer-guide/)");
        assertFalse(r.isReference());
        assertEquals("[the guide](/developer-guide/)", r.label());
    }

    @Test
    void distinguishesAnEmptyListFromNoList() {
        // "#clear()" names the no-argument overload; "#clear" names the method.
        assertTrue(SeeAlsoRef.parse("#clear()").hasParameterList());
        assertFalse(SeeAlsoRef.parse("#clear").hasParameterList());
    }
}
