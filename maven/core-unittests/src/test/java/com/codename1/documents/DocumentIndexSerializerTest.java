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
package com.codename1.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The on-disk contract between the app and the platform readers. Everything the Swift extension and
 * the Android provider parse is produced here, and neither of them is exercised by a JVM test, so a
 * silent change to this format is a feature that goes inert on device with a green build.
 */
class DocumentIndexSerializerTest {

    @Test
    void roundTripsAWholeTree() throws Exception {
        DocumentNode root = DocumentNode.folder("root", "My Invoices");
        DocumentNode year = DocumentNode.folder("y2031", "2031");
        year.add(DocumentNode.file("inv-1", "January.pdf")
                .setContentType("application/pdf")
                .setPath("invoices/january.pdf")
                .setSize(4096L)
                .setLastModified(1735689600000L));
        year.add(DocumentNode.file("inv-2", "February.pdf")
                .setContentType("application/pdf")
                .setRemoteId("s3://bucket/feb"));
        root.add(year);

        DocumentNode back = DocumentIndexSerializer.deserialize(
                DocumentIndexSerializer.serialize(root));

        assertEquals("root", back.getId());
        assertEquals("My Invoices", back.getName());
        assertTrue(back.isFolder());
        assertEquals(1, back.getChildren().size());

        DocumentNode backYear = back.getChildren().get(0);
        assertEquals("y2031", backYear.getId());
        assertEquals(2, backYear.getChildren().size());

        DocumentNode jan = backYear.getChildren().get(0);
        assertEquals("inv-1", jan.getId());
        assertEquals("January.pdf", jan.getName());
        assertFalse(jan.isFolder());
        assertEquals("application/pdf", jan.getContentType());
        assertEquals("invoices/january.pdf", jan.getPath());
        assertEquals(4096L, jan.getSize());
        assertEquals(1735689600000L, jan.getLastModified());

        DocumentNode feb = backYear.getChildren().get(1);
        assertEquals("s3://bucket/feb", feb.getRemoteId());
        assertNull(feb.getPath());
    }

    @Test
    void childOrderIsPreserved() throws Exception {
        // The browser lists children in index order, so a serializer that used a hash map here
        // would reshuffle the user's folder on every publish.
        DocumentNode root = DocumentNode.folder("root", "Root");
        for (int i = 0; i < 25; i++) {
            root.add(DocumentNode.file("f" + i, "File " + i + ".txt"));
        }
        DocumentNode back = DocumentIndexSerializer.deserialize(
                DocumentIndexSerializer.serialize(root));
        assertEquals(25, back.getChildren().size());
        for (int i = 0; i < 25; i++) {
            assertEquals("f" + i, back.getChildren().get(i).getId());
        }
    }

    @Test
    void omitsUnknownAndDefaultFieldsRatherThanWritingNulls() {
        String json = DocumentIndexSerializer.serialize(
                DocumentNode.folder("root", "Root").add(DocumentNode.file("f", "f.txt")));
        // A reader distinguishes "absent" from "present but null"; writing nulls would make every
        // unset field look like a deliberate erasure.
        assertFalse(json.contains("null"), json);
        assertFalse(json.contains("remoteId"), json);
        assertFalse(json.contains("\"size\""), json);
    }

    @Test
    void carriesTheSchemaVersion() {
        assertTrue(DocumentIndexSerializer.serialize(DocumentNode.folder("root", "Root"))
                .contains("\"v\""));
    }

    @Test
    void unknownSizeAndDateRoundTripAsUnknown() throws Exception {
        DocumentNode root = DocumentNode.folder("root", "Root");
        root.add(DocumentNode.file("f", "f.txt"));
        DocumentNode back = DocumentIndexSerializer.deserialize(
                DocumentIndexSerializer.serialize(root));
        assertEquals(-1L, back.getChildren().get(0).getSize());
        assertEquals(-1L, back.getChildren().get(0).getLastModified());
    }

    @Test
    void toleratesAnIntegerLiteralForSize() throws Exception {
        // The JSON parser is free to hand back a Double for a plain integer literal, and a
        // hand-written or server-produced index is not obliged to match our own number formatting.
        DocumentNode back = DocumentIndexSerializer.deserialize(
                "{\"v\":1,\"root\":{\"id\":\"root\",\"folder\":true,\"children\":["
                        + "{\"id\":\"f\",\"name\":\"f.txt\",\"folder\":false,\"size\":12345}]}}");
        assertEquals(12345L, back.getChildren().get(0).getSize());
    }

    @Test
    void rejectsAnIndexWithNoRoot() {
        assertThrows(IOException.class, () -> DocumentIndexSerializer.deserialize("{\"v\":1}"));
        assertThrows(IOException.class, () -> DocumentIndexSerializer.deserialize(null));
    }

    @Test
    void rejectsANodeWithNoId() {
        assertThrows(IllegalArgumentException.class, () -> DocumentNode.file(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> DocumentNode.file("", "x"));
    }

    @Test
    void refusesATreeWithDuplicateIds() {
        // Two nodes sharing an id resolve to whichever the reader indexed last while both parents
        // keep listing it, so the browser shows two entries that open the same content. Caught at
        // publish, where the developer is, rather than on a device.
        DocumentNode root = DocumentNode.folder("root", "Root");
        DocumentNode a = DocumentNode.folder("a", "A");
        DocumentNode b = DocumentNode.folder("b", "B");
        a.add(DocumentNode.file("same", "one.txt"));
        b.add(DocumentNode.file("same", "two.txt"));
        root.add(a).add(b);
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(root));
        assertTrue(err.getMessage().contains("same"), err.getMessage());
        // The root's own id counts too.
        DocumentNode clash = DocumentNode.folder("root", "Root");
        clash.add(DocumentNode.file("root", "root.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(clash));
    }

    @Test
    void refusesAFileAsTheRoot() {
        // Both platforms expect the provider root to be a directory: Android would publish a
        // non-directory MIME type as the root document, and Apple's .rootContainer would carry no
        // content-enumeration capability. The location exists and cannot be browsed.
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(DocumentNode.file("root", "a.txt")));
        assertTrue(err.getMessage().contains("must be a folder"), err.getMessage());
    }

    @Test
    void refusesComponentsTooLongForAFilesystem() {
        // 255 bytes is where every filesystem the tree lands on stops. Past it the pre-iOS-16
        // provider's createDirectory fails with ENAMETOOLONG, and it fails at OPEN time: the item
        // is listed, the user taps it, nothing happens. Refusing the publish names the node.
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            name.append('a');
        }
        DocumentNode longName = DocumentNode.folder("root", "Root");
        longName.add(DocumentNode.file("f", name.toString()));
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(longName));
        assertTrue(err.getMessage().contains("too long"), err.getMessage());

        // One byte under the limit is fine, so the check is a limit rather than a fence.
        DocumentNode allowed = DocumentNode.folder("root", "Root");
        allowed.add(DocumentNode.file("f", name.substring(0, 255)));
        assertNotNull(DocumentIndexSerializer.serialize(allowed));

        // The id is budgeted on its ESCAPED, NAMESPACED length, not its raw one: the classic
        // provider gives each item a directory named after the percent-escaped identifier, the
        // "cn1:" prefix included, and uppercase escapes too -- 100 capitals are 300 bytes there
        // plus 6 for the prefix, while 100 lowercase are 106.
        StringBuilder shouting = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            shouting.append('A');
        }
        DocumentNode longId = DocumentNode.folder("root", "Root");
        longId.add(DocumentNode.file(shouting.toString(), "fine.pdf"));
        IllegalArgumentException idErr = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(longId));
        assertTrue(idErr.getMessage().contains("once escaped"), idErr.getMessage());

        DocumentNode quietId = DocumentNode.folder("root", "Root");
        quietId.add(DocumentNode.file(shouting.toString().toLowerCase(), "fine.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(quietId));

        // The prefix counts. 250 lowercase characters cost 250 bytes on their own and 256 once
        // namespaced, which is one past the limit -- the case that made measuring the bare id
        // wrong.
        StringBuilder wide = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            wide.append('b');
        }
        DocumentNode namespaced = DocumentNode.folder("root", "Root");
        namespaced.add(DocumentNode.file(wide.toString(), "fine.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(namespaced));

        DocumentNode fits = DocumentNode.folder("root", "Root");
        fits.add(DocumentNode.file(wide.substring(0, 249), "fine.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(fits));
    }

    @Test
    void refusesNamesCarryingAPathSeparator() {
        // The pre-iOS-16 provider builds a storage URL from the name, so a separator becomes
        // several path components and its identifier round-trip then reads the wrong directory --
        // the item is listed and cannot be opened.
        DocumentNode root = DocumentNode.folder("root", "Root");
        root.add(DocumentNode.file("f", "reports/2031.pdf"));
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(root));
        assertTrue(err.getMessage().contains("path separator"), err.getMessage());

        DocumentNode backslash = DocumentNode.folder("root", "Root");
        backslash.add(DocumentNode.file("g", "reports\\2031.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(backslash));

        // The name the reader uses is not always the name that was set: it falls back to the id.
        // Validating only a non-null name let file("account/42", null) through, and that is the
        // value the classic provider would have put in the URL.
        DocumentNode viaId = DocumentNode.folder("root", "Root");
        viaId.add(DocumentNode.file("account/42", null));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(viaId));

        DocumentNode dots = DocumentNode.folder("root", "Root");
        dots.add(DocumentNode.file("d", ".."));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(dots));
    }

    @Test
    void refusesToNestUnderAFile() {
        assertThrows(IllegalStateException.class,
                () -> DocumentNode.file("f", "f.txt").add(DocumentNode.file("g", "g.txt")));
    }
}
