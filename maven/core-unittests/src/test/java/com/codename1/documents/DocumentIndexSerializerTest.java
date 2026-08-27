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
    void refusesTwoChildrenShownUnderOneName() {
        // A file browser addresses an item by its name within its folder. Apple's replicated
        // provider says so outright: two items at the same parent and filename make it "bounce"
        // one of them, so the user sees an item that cannot be opened even though the ids differ.
        DocumentNode root = DocumentNode.folder("root", "Root");
        root.add(DocumentNode.file("a", "Invoice.pdf"));
        root.add(DocumentNode.file("b", "Invoice.pdf"));
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(root));
        assertTrue(err.getMessage().contains("Invoice.pdf"), err.getMessage());

        // The same name in DIFFERENT folders is fine -- that is the whole point of folders.
        DocumentNode split = DocumentNode.folder("root", "Root");
        DocumentNode first = DocumentNode.folder("f1", "2030");
        first.add(DocumentNode.file("a", "Invoice.pdf"));
        DocumentNode second = DocumentNode.folder("f2", "2031");
        second.add(DocumentNode.file("b", "Invoice.pdf"));
        split.add(first);
        split.add(second);
        assertNotNull(DocumentIndexSerializer.serialize(split));

        // Compared as the reader will SHOW them: a node with no name falls back to its id, and an
        // empty name is normalized to "item", so neither can smuggle a duplicate past this.
        DocumentNode viaId = DocumentNode.folder("root", "Root");
        viaId.add(DocumentNode.file("report", null));
        viaId.add(DocumentNode.file("other", "report"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(viaId));

        DocumentNode viaEmpty = DocumentNode.folder("root", "Root");
        viaEmpty.add(DocumentNode.file("a", ""));
        viaEmpty.add(DocumentNode.file("b", "item"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(viaEmpty));

        // Case is not folded: these are two items on iOS, whose data volume is case-sensitive.
        DocumentNode cased = DocumentNode.folder("root", "Root");
        cased.add(DocumentNode.file("a", "Invoice.pdf"));
        cased.add(DocumentNode.file("b", "invoice.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(cased));
    }

    @Test
    void refusesNamesAndIdsAFilesystemCannotCarry() {
        // NUL terminates a path, so a name holding one cannot be given to the filesystem at all;
        // the pre-iOS-16 provider kept it in the storage leaf and failed only when the listed
        // item was opened.
        DocumentNode nul = DocumentNode.folder("root", "Root");
        nul.add(DocumentNode.file("a", "report\u0000.pdf"));
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(nul));
        assertTrue(err.getMessage().contains("U+0"), err.getMessage());

        DocumentNode newline = DocumentNode.folder("root", "Root");
        newline.add(DocumentNode.file("a", "report\n.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(newline));

        // An unpaired surrogate has no UTF-8 encoding, and every writer here encodes as UTF-8:
        // it becomes "?", two ids differing only there become one on disk, and the reader then
        // rejects the whole index as holding a duplicate -- the location disappears.
        DocumentNode surrogate = DocumentNode.folder("root", "Root");
        surrogate.add(DocumentNode.file("\ud800", "fine.pdf"));
        IllegalArgumentException idErr = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(surrogate));
        assertTrue(idErr.getMessage().contains("unpaired surrogate"), idErr.getMessage());

        DocumentNode inName = DocumentNode.folder("root", "Root");
        inName.add(DocumentNode.file("a", "report\udc00.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(inName));

        // The remote id travels the same lossy path and is worse when it breaks: it is the key
        // sent to the endpoint, so "?" in place of the surrogate asks the server for a different
        // object.
        DocumentNode remote = DocumentNode.folder("root", "Root");
        remote.add(DocumentNode.file("a", "fine.pdf").setRemoteId("key\ud800"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(remote));

        // A properly paired one is an ordinary character and is accepted.
        DocumentNode paired = DocumentNode.folder("root", "Root");
        paired.add(DocumentNode.file("a", "report\ud83d\ude00.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(paired));
    }

    @Test
    void onlyTheNormalizedSpellingIsAccepted() {
        // The property the serializer's two tables exist to provide, re-derived here rather than
        // trusted. Accepting two spellings of one name would put two ids in this index that are
        // ONE key in the readers -- Swift dictionaries and Apple filesystems both compare
        // canonically -- so one node would silently overwrite the other and two rows in the tree
        // would open one document. Refusing the normalized spelling would refuse text that is
        // already correct, which is how a first attempt at this refused normalized Hebrew.
        //
        // java.text.Normalizer is the source of truth here and cannot be the one in the
        // serializer: that class is translated for the iOS runtime, which has no normalizer at
        // all. The JDK's Unicode version is older than the one the tables were derived from, so
        // this proves them complete up to what the JDK knows and leaves their extra entries as a
        // superset, which is the safe direction.
        java.util.Set<Integer> performed = new java.util.HashSet<>();
        java.util.Set<Integer> normalizedSpelling = new java.util.HashSet<>();
        for (int c = Character.MIN_CODE_POINT; c <= Character.MAX_CODE_POINT; c++) {
            if (c >= 0xD800 && c <= 0xDFFF) {
                continue;
            }
            String self = new String(Character.toChars(c));
            String decomposed = java.text.Normalizer.normalize(self,
                    java.text.Normalizer.Form.NFD);
            if (decomposed.equals(self)) {
                continue;
            }
            String normalized = java.text.Normalizer.normalize(self,
                    java.text.Normalizer.Form.NFC);
            String name = "U+" + Integer.toHexString(c).toUpperCase();

            // Soundness: whatever normalization produces has to be publishable. Skipped when it
            // is a bare mark sequence -- U+0340 normalizes to U+0300, one mark to another --
            // which carries no text of its own, and where refusing both spellings is safe.
            if (!isMark(Character.codePointAt(normalized, 0))) {
                assertTrue(DocumentIndexSerializer.combiningMarkAt(normalized) < 0,
                        "The normalized spelling of " + name + " is refused, so text that is "
                                + "already correct cannot be published");
            }

            // Completeness, first half: a character normalization rewrites on its own must be
            // refused, or it and its normalized spelling are two ids and one key.
            if (!self.equals(normalized)) {
                assertTrue(DocumentIndexSerializer.combiningMarkAt(self) >= 0,
                        name + " and its normalized spelling are both accepted, so two ids that "
                                + "are one key in the readers would pass");
            }

            int[] parts = decomposed.codePoints().toArray();
            if (parts.length < 2 || (c >= 0xAC00 && c <= 0xD7A3)) {
                continue;
            }
            if (normalized.equals(self)) {
                for (int i = 1; i < parts.length; i++) {
                    performed.add(parts[i]);
                }
            } else if (!isMark(parts[0])) {
                for (int i = 1; i < parts.length; i++) {
                    normalizedSpelling.add(parts[i]);
                }
            }
        }

        // Completeness, second half: the marks a decomposed spelling is made of, for every
        // composition the normalizer performs. Minus the ones that also carry a normalized
        // spelling of their own -- U+093C is the nukta in both U+0929, which composes, and
        // U+0958, which does not -- because refusing those would refuse correct text.
        performed.removeAll(normalizedSpelling);
        for (int mark : performed) {
            assertTrue(DocumentIndexSerializer.combiningMarkAt(new String(Character.toChars(mark)))
                            >= 0,
                    "U+" + Integer.toHexString(mark).toUpperCase() + " only ever appears in a "
                            + "decomposed spelling and is accepted, so that spelling and its "
                            + "composed twin would both pass");
        }
    }

    private static boolean isMark(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    @Test
    void refusesATibetanPrecomposedVowel() {
        // U+0F73 TIBETAN VOWEL SIGN II is canonically U+0F71 U+0F72, and Tibetan is outside every
        // generic diacritical block, so both spellings used to be accepted: two nodes, distinct
        // names, one key in the reader.
        //
        // Note which way round it goes. Unicode excludes this composition, so the normalizer
        // takes the precomposed character APART -- the sequence is the normalized spelling and
        // the single character is the one refused, the opposite of the Latin case below.
        DocumentNode precomposed = DocumentNode.folder("root", "Root");
        precomposed.add(DocumentNode.file("a", "\u0f73.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(precomposed));

        DocumentNode sequence = DocumentNode.folder("root", "Root");
        sequence.add(DocumentNode.file("a", "\u0f71\u0f72.txt"));
        assertNotNull(DocumentIndexSerializer.serialize(sequence));
    }

    @Test
    void acceptsNormalizedHebrewWithPoints() {
        // The same shape as the Tibetan case, and the reason the rule cannot be "refuse every
        // mark that appears in a decomposition". U+FB2A HEBREW SHIN WITH SHIN DOT normalizes to
        // U+05E9 U+05C1, so the sequence is the normalized spelling: refusing its marks would
        // refuse normalized Hebrew outright.
        DocumentNode pointed = DocumentNode.folder("root", "Root");
        pointed.add(DocumentNode.file("a", "\u05e9\u05b7\u05c1.txt"));
        assertNotNull(DocumentIndexSerializer.serialize(pointed));

        DocumentNode presentation = DocumentNode.folder("root", "Root");
        presentation.add(DocumentNode.file("a", "\ufb2a.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(presentation));
    }

    @Test
    void refusesADecomposedName() {
        // Apple's filesystems compare names after canonical normalization, so "e" + U+0301 and
        // the precomposed letter are one file there and two strings here -- the sibling check
        // would pass and the browser would hide one of them. There is no normalizer to call: the
        // iOS runtime has no java.text.Normalizer and this class is translated for it, so the
        // rule is that names arrive precomposed.
        DocumentNode root = DocumentNode.folder("root", "Root");
        root.add(DocumentNode.file("a", "expos\u0065\u0301.pdf"));
        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(root));
        assertTrue(err.getMessage().contains("normalize"), err.getMessage());

        // The ID is checked with the same rule, even when a name of its own hides it: two ids
        // that are canonically equivalent are one key in the readers, so the duplicate-id check
        // passes and one node then overwrites the other.
        DocumentNode decomposedId = DocumentNode.folder("root", "Root");
        decomposedId.add(DocumentNode.file("expos\u0065\u0301", "Shown.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(decomposedId));

        // And the remote id, which is the key sent to the endpoint.
        DocumentNode decomposedRemote = DocumentNode.folder("root", "Root");
        decomposedRemote.add(DocumentNode.file("a", "Shown.pdf")
                .setRemoteId("key\u0065\u0301"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(decomposedRemote));

        // The precomposed spelling of the same name is accepted, and is the one to use.
        DocumentNode composed = DocumentNode.folder("root", "Root");
        composed.add(DocumentNode.file("a", "expos\u00e9.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(composed));

        // A canonical SINGLETON carries no combining mark and still normalizes to another
        // character: U+212B ANGSTROM SIGN is U+00C5 on an Apple filesystem, so the two spellings
        // are one filename. The letterlike signs and the CJK compatibility ideographs are where
        // those live.
        DocumentNode angstrom = DocumentNode.folder("root", "Root");
        angstrom.add(DocumentNode.file("a", "\u212b.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(angstrom));

        DocumentNode compatibility = DocumentNode.folder("root", "Root");
        compatibility.add(DocumentNode.file("a", "\uf900.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(compatibility));

        // The Greek oxia forms are singletons too, and neither spelling is decomposed: U+1F71
        // normalizes to U+03AC, so the two are one filename on an Apple volume.
        DocumentNode oxia = DocumentNode.folder("root", "Root");
        oxia.add(DocumentNode.file("a", "\u1f71.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(oxia));

        DocumentNode tonos = DocumentNode.folder("root", "Root");
        tonos.add(DocumentNode.file("a", "\u03ac.txt"));
        assertNotNull(DocumentIndexSerializer.serialize(tonos));

        // Hangul composes by algorithm rather than from a table, so neither a combining-mark
        // test nor a singleton list sees it: U+1100 U+1161 is the same filename as U+AC00.
        DocumentNode jamo = DocumentNode.folder("root", "Root");
        jamo.add(DocumentNode.file("a", "\u1100\u1161.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(jamo));

        DocumentNode syllable = DocumentNode.folder("root", "Root");
        syllable.add(DocumentNode.file("a", "\uac00.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(syllable));

        // The COMPATIBILITY jamo are a different range and stay allowed: that is what a name
        // meaning the letter itself uses.
        DocumentNode letter = DocumentNode.folder("root", "Root");
        letter.add(DocumentNode.file("a", "\u3131.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(letter));

        // A script whose marks have no precomposed form is untouched -- refusing decomposed
        // spellings must not amount to refusing whole writing systems.
        DocumentNode devanagari = DocumentNode.folder("root", "Root");
        devanagari.add(DocumentNode.file("a", "\u0915\u094d\u0937.txt"));
        assertNotNull(DocumentIndexSerializer.serialize(devanagari));

        DocumentNode hebrew = DocumentNode.folder("root", "Root");
        hebrew.add(DocumentNode.file("a", "\u05e9\u05c1\u05b8.txt"));
        assertNotNull(DocumentIndexSerializer.serialize(hebrew));

        // The supplementary compatibility block too, which a char-by-char scan cannot see: it
        // arrives as a surrogate pair. U+2F800 normalizes to U+4E3D.
        DocumentNode supplementary = DocumentNode.folder("root", "Root");
        supplementary.add(DocumentNode.file("a", new String(Character.toChars(0x2F800)) + ".txt"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentIndexSerializer.serialize(supplementary));

        // The ordinary letter it normalizes to is accepted.
        DocumentNode plain = DocumentNode.folder("root", "Root");
        plain.add(DocumentNode.file("a", "\u00c5.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(plain));

        // And an ordinary supplementary character -- an emoji -- is not refused by that range.
        DocumentNode emoji = DocumentNode.folder("root", "Root");
        emoji.add(DocumentNode.file("a", "party\ud83c\udf89.pdf"));
        assertNotNull(DocumentIndexSerializer.serialize(emoji));
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
