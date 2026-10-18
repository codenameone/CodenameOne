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
package com.codename1.mapping;

import com.codename1.xml.Element;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// `Mappers.fromXml` parses in strict mode: names match exactly as `toXml`
/// wrote them and text values keep their whitespace.
class MappersXmlStrictTest {

    static final class Note {
        String title;
        String body;
    }

    /// Reads and writes the way the generated mappers do.
    private static final class NoteMapper implements Mapper<Note> {
        public Class<Note> type() {
            return Note.class;
        }

        public Map<String, Object> toMap(Note instance) {
            throw new UnsupportedOperationException();
        }

        public Note fromMap(Map<String, Object> map) {
            throw new UnsupportedOperationException();
        }

        public String xmlRootName() {
            return "Note";
        }

        public void writeXml(Note n, Element root) {
            root.setAttribute("Title", n.title);
            Element b = new Element("bodyText");
            b.addChild(new Element(n.body, true));
            root.addChild(b);
        }

        public Note readXml(Element root) {
            Note n = new Note();
            n.title = root.getAttribute("Title");
            Element b = (Element) root.getChildrenByTagName("bodyText").elementAt(0);
            n.body = b.getNumChildren() > 0 ? b.getChildAt(0).getText() : null;
            return n;
        }
    }

    @Test
    void whitespaceAndCaseRoundTrip() {
        Mappers.register(new NoteMapper());
        Note n = new Note();
        n.title = "T";
        n.body = "  two leading, one trailing ";
        Note back = Mappers.fromXml(Mappers.toXml(n), Note.class);
        assertEquals("T", back.title);
        assertEquals(n.body, back.body);

        n.body = " ";
        assertEquals(" ", Mappers.fromXml(Mappers.toXml(n), Note.class).body);
    }
}
