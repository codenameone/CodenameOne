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
package com.codename1.xml;

import com.codename1.io.CharArrayReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// A parse that a callback ends early must not leave its unread input behind
/// for the next parse. The read-ahead buffer was static and only `parse`
/// emptied it, so after RSSService stopped a feed at its item limit, the next
/// `eventParser` call anywhere in the process read the rest of that feed first.
class XMLParserBufferTest {

    private static final class Stop extends RuntimeException {
    }

    /// Records text elements; throws after `stopAfter` of them, 0 for never.
    private static final class Recorder extends XMLParser {
        final List<String> texts = new ArrayList<String>();
        private final int stopAfter;

        Recorder(int stopAfter) {
            this.stopAfter = stopAfter;
        }

        @Override
        protected void textElement(String text) {
            texts.add(text);
            if (stopAfter > 0 && texts.size() >= stopAfter) {
                throw new Stop();
            }
        }
    }

    @Test
    void anAbandonedEventParseLeavesNothingForTheNextOne() throws Exception {
        Recorder first = new Recorder(1);
        try {
            first.eventParser(reader("<a><b>1</b><b>2</b><b>3</b></a>"));
        } catch (Stop expected) {
            // the callback ended the parse, as RSSService's limit does
        }
        assertEquals("[1]", first.texts.toString());

        Recorder second = new Recorder(0);
        second.eventParser(reader("<a><b>x</b></a>"));
        assertEquals("[x]", second.texts.toString());

        Recorder third = new Recorder(0);
        Element doc = third.parse(reader("<a><b>y</b></a>"));
        assertEquals("y", doc.getChildAt(0).getChildAt(0).getText());
    }

    @Test
    void aParseStartedFromACallbackDoesNotDisturbTheOuterOne() throws Exception {
        final List<String> outer = new ArrayList<String>();
        XMLParser p = new XMLParser() {
            @Override
            protected void textElement(String text) {
                outer.add(text);
                new XMLParser().parse(reader("<i>inner</i>"));
            }
        };
        p.eventParser(reader("<a><b>1</b><b>2</b></a>"));
        assertEquals("[1, 2]", outer.toString());
    }

    private static CharArrayReader reader(String s) {
        return new CharArrayReader(s.toCharArray());
    }
}
