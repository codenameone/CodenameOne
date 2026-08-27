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

import com.codename1.io.JSONParser;
import com.codename1.io.JSONWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Converts a published document tree to and from the `index.json` the platform readers consume.
///
/// The format is deliberately small and forward-compatible: absent keys mean "unknown", which is
/// what lets a reader written against one version of the schema survive a newer publisher. `v`
/// carries the schema version so a future reader can tell a missing key from a renamed one.
///
/// The reader half exists for the simulator and for tests. On device the parsing is done natively
/// -- Swift in the Apple extension, Java in the Android provider -- because the app process is not
/// running when the browser asks.
public final class DocumentIndexSerializer {
    /// The schema version written into every index.
    public static final int VERSION = 1;

    private DocumentIndexSerializer() {
    }

    /// Serializes a document tree.
    ///
    /// #### Parameters
    ///
    /// - `root`: the root node, must not be null
    ///
    /// #### Returns
    ///
    /// the index JSON
    public static String serialize(DocumentNode root) {
        if (root == null) {
            throw new IllegalArgumentException("A document index needs a root node");
        }
        // Refused here rather than left to the readers. Two nodes sharing an id resolve to
        // whichever the reader indexed last while both parents keep listing it, so the browser
        // shows two entries that open the same content -- a defect that surfaces on a device,
        // far from the publish that caused it.
        Set<String> seen = new HashSet<String>();
        String duplicate = findDuplicateId(root, seen);
        if (duplicate != null) {
            throw new IllegalArgumentException("Two document nodes share the id \"" + duplicate
                    + "\". Ids must be unique across the whole published tree.");
        }
        if (!root.isFolder()) {
            // Both platforms expect the provider's root to be a directory: Android publishes it
            // as the root document id and would then report a non-directory MIME type for it,
            // and Apple's .rootContainer would carry no content-enumeration capability. The
            // location exists in both cases and cannot be browsed.
            throw new IllegalArgumentException("The published root must be a folder; \""
                    + root.getId() + "\" is a file.");
        }
        validate(root);
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("v", Integer.valueOf(VERSION));
        doc.put("rev", nextRevision());
        doc.put("root", toMap(root));
        return JSONWriter.toJson(doc);
    }

    /// A value that differs for every publication.
    ///
    /// The readers need to tell one publication from another: a download that started against
    /// the previous one has to be refused, and an item with no declared size or date has nothing
    /// else to version it by. They used to take that from the index file's modification time,
    /// which is only as fine as the filesystem's clock -- two publications in the same
    /// millisecond were one revision, and a response fetched against the first was accepted
    /// after the second.
    ///
    /// The clock still leads, so revisions sort the way publications happened. The counter
    /// separates publications inside one tick, and the token separates process lifetimes -- a
    /// counter alone would repeat after a restart, and a restart is exactly when an app republishes.
    private static synchronized String nextRevision() {
        revisionCounter++;
        return Long.toString(System.currentTimeMillis()) + "-"
                + Integer.toHexString(revisionCounter) + "-" + PROCESS_TOKEN;
    }

    private static int revisionCounter;

    /// Distinguishes this run of the app from the last one.
    ///
    /// The identity hash of a fresh object, which differs between runs on every implementation
    /// this ships to. It is belt and braces rather than the load-bearing part: the clock leads
    /// the revision, so two runs would have to publish inside the same millisecond for the token
    /// to matter at all, and a restart cannot happen in that window. Not persisted, because
    /// storage the app was never asked for is a worse price than a collision nobody can construct.
    private static final String PROCESS_TOKEN =
            Integer.toHexString(System.identityHashCode(new Object()));

    /// Parses an index produced by `serialize`.
    ///
    /// #### Parameters
    ///
    /// - `json`: the index JSON
    ///
    /// #### Returns
    ///
    /// the root node
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the text is not readable as an index
    public static DocumentNode deserialize(String json) throws IOException {
        if (json == null) {
            throw new IOException("Empty document index");
        }
        Map<String, Object> doc = JSONParser.parseJSON(json);
        Object root = doc == null ? null : doc.get("root");
        if (!(root instanceof Map)) {
            throw new IOException("Document index has no root object");
        }
        return fromMap((Map) root);
    }

    /// Refuses a tree the platform readers cannot serve faithfully.
    ///
    /// The rule is about the name the readers actually use, which is not always the name that was
    /// set: `CN1DocumentItem.filename` falls back to the node id when no name was given, and the
    /// pre-iOS-16 provider builds a storage URL from it. A separator in either therefore becomes
    /// several path components, and that provider's identifier round-trip then reads the wrong
    /// directory and cannot open the file at all. "." and ".." are refused for the same reason.
    ///
    /// Reserved File Provider identifiers need no rule here: ids are namespaced before they
    /// become NSFileProviderItemIdentifiers, so an app id can no longer collide with a system
    /// token whatever it says.
    private static void validate(DocumentNode node) {
        String effective = node.getName() != null ? node.getName() : node.getId();
        if (effective.indexOf('/') >= 0 || effective.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("\"" + effective + "\" contains a path separator. "
                    + "A document is shown under a single file name; put the structure in folders "
                    + "instead.");
        }
        if (".".equals(effective) || "..".equals(effective)) {
            throw new IllegalArgumentException("\"" + effective + "\" is not a usable file name.");
        }
        int control = controlCharacterAt(effective);
        if (control >= 0) {
            throw new IllegalArgumentException("\"" + effective + "\" contains U+"
                    + Integer.toHexString(effective.charAt(control)).toUpperCase() + " at index "
                    + control + ", which cannot appear in a file name. A path cannot carry a NUL "
                    + "byte at all, and the rest of the control range makes a name the browser "
                    + "cannot show or the user retype.");
        }
        int loneSurrogate = loneSurrogateAt(effective);
        if (loneSurrogate < 0) {
            loneSurrogate = loneSurrogateAt(node.getId());
        }
        if (loneSurrogate < 0 && node.getPath() != null) {
            // The local path travels the same lossy path as the ids and lands in the same place:
            // "?" for the surrogate, and the readers then resolve a DIFFERENT file under the
            // shared directory -- one whose name really does contain a question mark, if the app
            // wrote such a file.
            loneSurrogate = loneSurrogateAt(node.getPath());
        }
        if (loneSurrogate < 0 && node.getRemoteId() != null) {
            // The remote id travels the same lossy path and is worse when it breaks: it is the
            // key the readers send to the endpoint, so a "?" in place of the surrogate asks the
            // server for a DIFFERENT object -- and gets one, if the server has a key with a
            // question mark in it.
            loneSurrogate = loneSurrogateAt(node.getRemoteId());
        }
        if (loneSurrogate >= 0) {
            throw new IllegalArgumentException("A document id, name, path or remote id contains "
                    + "an unpaired surrogate (index " + loneSurrogate + ", on the node shown as \""
                    + effective + "\"). It cannot be encoded as UTF-8, and the index is written "
                    + "as UTF-8: the character becomes \"?\", two values that differed only "
                    + "there become one, and the reader then rejects the whole index as holding "
                    + "a duplicate -- so the location disappears rather than one document going "
                    + "missing.");
        }
        // The ids as well as the shown name. Two ids that are canonically equivalent are two
        // strings here and ONE key in the readers -- Swift dictionaries and Apple filesystems
        // both compare that way -- so the duplicate-id check above passes and one node then
        // overwrites the other, which enumerates the same document twice. The remote id is
        // checked with them: it is the key sent to the endpoint, which compares bytes, while the
        // reader that matches a response against it compares canonically.
        int mark = combiningMarkAt(effective);
        if (mark < 0) {
            mark = combiningMarkAt(node.getId());
        }
        if (mark < 0 && node.getRemoteId() != null) {
            mark = combiningMarkAt(node.getRemoteId());
        }
        if (mark >= 0) {
            throw new IllegalArgumentException("The node shown as \"" + effective + "\" has an "
                    + "id, name or remote id containing a character at index " + mark
                    + " that Apple's filesystems normalize to something else. They "
                    + "compare names after canonical normalization, so a name spelled this way "
                    + "and its normalized twin are one file there while they are two strings "
                    + "here -- and the browser hides one of the pair. Use the normalized "
                    + "spelling: \"\u00e9\" rather than \"e\" followed by U+0301, and the "
                    + "ordinary letter rather than a letterlike or compatibility form. For a "
                    + "few scripts normalization goes the other way -- U+0F73 is U+0F71 U+0F72, "
                    + "and U+FB2A is U+05E9 U+05C1 -- so it is the normalized spelling that "
                    + "counts, not the shorter one.");
        }
        if (utf8Length(effective) > MAX_COMPONENT_BYTES) {
            throw new IllegalArgumentException("\"" + effective + "\" is too long to be a file "
                    + "name (" + utf8Length(effective) + " bytes; the limit is "
                    + MAX_COMPONENT_BYTES + ").");
        }
        if (storagePathLength(node.getId()) > MAX_COMPONENT_BYTES) {
            throw new IllegalArgumentException("The document id \"" + node.getId() + "\" is too "
                    + "long to name a directory once escaped (" + storagePathLength(node.getId())
                    + " bytes; the limit is " + MAX_COMPONENT_BYTES + "). Shorten the id, or put "
                    + "the long part in the name.");
        }
        Set<String> siblingNames = new HashSet<String>();
        for (DocumentNode child : node.getChildren()) {
            String childName = displayName(child);
            if (!siblingNames.add(childName)) {
                throw new IllegalArgumentException("Two children of \"" + effective + "\" are "
                        + "both shown as \"" + childName + "\". A file browser addresses an item "
                        + "by its name within its folder, so two that share one there is a folder "
                        + "with an item the user cannot open -- Apple's provider drops one of "
                        + "them. Distinct ids are not enough; give them distinct names.");
            }
            validate(child);
        }
    }

    /// The name a reader will actually show for this node, which is what has to be unique among
    /// its siblings.
    ///
    /// It is not simply `getName()`: the readers fall back to the id when no name is set, and an
    /// empty name is normalized to "item" rather than shown as nothing. Comparing the raw values
    /// would let a node named "" and one named "item" through, and the browser would then see one
    /// name twice.
    ///
    /// Case is deliberately not folded. Two siblings differing only in case collide on a
    /// case-insensitive volume and not on iOS, whose data volume is case-sensitive, and refusing
    /// them here would reject a tree that works on the platform this ships for.
    ///
    /// Canonical equivalence is handled by `validate` refusing combining marks outright rather
    /// than by normalizing here. There is no normalizer to call: the iOS runtime has no
    /// `java.text.Normalizer`, and this class is translated for it. Requiring precomposed names
    /// gets the same result for every name anyone writes -- two precomposed names that are
    /// canonically equivalent are the same string -- without a Unicode table in the core.
    private static String displayName(DocumentNode node) {
        String name = node.getName() != null ? node.getName() : node.getId();
        return name.length() == 0 ? "item" : name;
    }

    /// The most bytes a single path component may take.
    ///
    /// 255 is what every filesystem the published tree lands on allows -- APFS, ext4 on Android
    /// and NTFS all stop there. Beyond it the pre-iOS-16 provider's `createDirectory` fails with
    /// ENAMETOOLONG, and it fails at open time: the item is listed, the user taps it, and nothing
    /// happens. Refusing the publish says which node is at fault instead.
    private static final int MAX_COMPONENT_BYTES = 255;

    /// The Hangul jamo that compose into syllables.
    ///
    /// Hangul is the other decomposition Unicode does without a combining mark: U+1100 U+1161 is
    /// the same filename as U+AC00, composed by an algorithm rather than from a table, so a
    /// combining-mark test and a singleton table both miss it. The conjoining jamo blocks are
    /// only ever the decomposed spelling -- the syllables at U+AC00-U+D7A3 are the composed one,
    /// and the COMPATIBILITY jamo at U+3130-U+318F, which is what a name meaning the letter
    /// itself uses, are a different range and stay allowed.
    private static boolean isConjoiningJamo(int c) {
        return (c >= 0x1100 && c <= 0x11FF)
                || (c >= 0xA960 && c <= 0xA97F)
                || (c >= 0xD7B0 && c <= 0xD7FF);
    }

    /// The index of the first character a file name cannot carry, or -1.
    ///
    /// NUL terminates a path, so a name holding one is a name the filesystem cannot be given.
    /// The rest of the C0 range and DEL are refused with it: they are not illegal everywhere, but
    /// a name carrying a newline or a backspace is one no browser shows honestly and no user can
    /// retype, and nothing publishes one on purpose.
    private static int controlCharacterAt(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return i;
            }
        }
        return -1;
    }

    /// The index of the first unpaired surrogate, or -1.
    ///
    /// A lone surrogate is not a character: it has no UTF-8 encoding, and every writer here
    /// encodes as UTF-8, replacing it with "?". Two ids differing only in that character then
    /// become one on disk, and a reader that finds a duplicate rejects the index whole -- so the
    /// published location vanishes instead of one document going missing, which is a far worse
    /// failure than the one the publisher was asked to prevent.
    static int loneSurrogateAt(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(i + 1))) {
                    return i;
                }
                i++;
            } else if (Character.isLowSurrogate(c)) {
                return i;
            }
        }
        return -1;
    }

    /// Code points a canonical normalization rewrites on their own.
    ///
    /// Two kinds, and one rule: refuse every spelling that is not the normalized one. A SINGLETON
    /// is a character that normalizes to a different single character -- U+212B ANGSTROM SIGN is
    /// U+00C5, U+1F71 GREEK ALPHA WITH OXIA is U+03AC WITH TONOS. A PRESENTATION FORM is a
    /// precomposed character Unicode excludes from composition, so normalizing it takes it apart
    /// instead of leaving it alone: U+FB2A HEBREW SHIN WITH SHIN DOT becomes U+05E9 U+05C1, and
    /// U+0F73 TIBETAN VOWEL SIGN II becomes U+0F71 U+0F72. Either way the character and its
    /// normalized spelling are one name on an Apple volume and one key in a Swift dictionary,
    /// while they are two strings here.
    ///
    /// Derived from the Unicode character database rather than chosen: every code point whose
    /// NFC form differs from itself. Sorted pairs of inclusive bounds.
    private static final int[] RENORMALIZED = {
        0x0340, 0x0341, 0x0343, 0x0344, 0x0374, 0x0374, 0x037E, 0x037E,
        0x0387, 0x0387, 0x0958, 0x095F, 0x09DC, 0x09DD, 0x09DF, 0x09DF,
        0x0A33, 0x0A33, 0x0A36, 0x0A36, 0x0A59, 0x0A5B, 0x0A5E, 0x0A5E,
        0x0B5C, 0x0B5D, 0x0F43, 0x0F43, 0x0F4D, 0x0F4D, 0x0F52, 0x0F52,
        0x0F57, 0x0F57, 0x0F5C, 0x0F5C, 0x0F69, 0x0F69, 0x0F73, 0x0F73,
        0x0F75, 0x0F76, 0x0F78, 0x0F78, 0x0F81, 0x0F81, 0x0F93, 0x0F93,
        0x0F9D, 0x0F9D, 0x0FA2, 0x0FA2, 0x0FA7, 0x0FA7, 0x0FAC, 0x0FAC,
        0x0FB9, 0x0FB9, 0x1F71, 0x1F71, 0x1F73, 0x1F73, 0x1F75, 0x1F75,
        0x1F77, 0x1F77, 0x1F79, 0x1F79, 0x1F7B, 0x1F7B, 0x1F7D, 0x1F7D,
        0x1FBB, 0x1FBB, 0x1FBE, 0x1FBE, 0x1FC9, 0x1FC9, 0x1FCB, 0x1FCB,
        0x1FD3, 0x1FD3, 0x1FDB, 0x1FDB, 0x1FE3, 0x1FE3, 0x1FEB, 0x1FEB,
        0x1FEE, 0x1FEF, 0x1FF9, 0x1FF9, 0x1FFB, 0x1FFB, 0x1FFD, 0x1FFD,
        0x2000, 0x2001, 0x2126, 0x2126, 0x212A, 0x212B, 0x2329, 0x232A,
        0x2ADC, 0x2ADC, 0xF900, 0xFA0D, 0xFA10, 0xFA10, 0xFA12, 0xFA12,
        0xFA15, 0xFA1E, 0xFA20, 0xFA20, 0xFA22, 0xFA22, 0xFA25, 0xFA26,
        0xFA2A, 0xFA6D, 0xFA70, 0xFAD9, 0xFB1D, 0xFB1D, 0xFB1F, 0xFB1F,
        0xFB2A, 0xFB36, 0xFB38, 0xFB3C, 0xFB3E, 0xFB3E, 0xFB40, 0xFB41,
        0xFB43, 0xFB44, 0xFB46, 0xFB4E, 0x1D15E, 0x1D164, 0x1D1BB, 0x1D1C0,
        0x2F800, 0x2FA1D,
    };

    /// The pairs a canonical normalization composes, base first.
    ///
    /// A decomposed spelling is a base followed by a mark it composes with -- "e" followed by
    /// U+0301 -- so refusing the pair refuses the decomposed half and keeps the composed one,
    /// which is what normalization produces and what a person types. The voiced kana are the
    /// case that matters most in practice: a file copied from a Mac arrives as the kana followed
    /// by U+3099, and the app's own spelling is the single character.
    ///
    /// A PAIR, not a mark on its own, because a mark composes after some bases and not others.
    /// U+0301 composes with "e" and there is no precomposed "q" with acute, so "q" followed by
    /// U+0301 IS the normalized spelling and has to be publishable. The same for the marks
    /// Unicode excludes from composition after certain bases: U+093C composes onto U+0928 and
    /// not onto U+0915, so refusing the mark itself would refuse the normalized spelling of the
    /// Indic nukta letters, and refusing U+05C1 would refuse normalized Hebrew.
    ///
    /// Derived from the character database rather than chosen, and re-derived by the test: every
    /// two-part canonical decomposition the normalizer puts back together, Hangul excepted --
    /// its composition is algorithmic and the conjoining jamo are refused above. Each entry is
    /// the base in the high bits and the mark in the low 24, sorted, for a binary search.
    private static final long[] COMPOSING_PAIRS = {
        0x03C000338L, 0x03D000338L, 0x03E000338L, 0x041000300L, 0x041000301L,
        0x041000302L, 0x041000303L, 0x041000304L, 0x041000306L, 0x041000307L,
        0x041000308L, 0x041000309L, 0x04100030AL, 0x04100030CL, 0x04100030FL,
        0x041000311L, 0x041000323L, 0x041000325L, 0x041000328L, 0x042000307L,
        0x042000323L, 0x042000331L, 0x043000301L, 0x043000302L, 0x043000307L,
        0x04300030CL, 0x043000327L, 0x044000307L, 0x04400030CL, 0x044000323L,
        0x044000327L, 0x04400032DL, 0x044000331L, 0x045000300L, 0x045000301L,
        0x045000302L, 0x045000303L, 0x045000304L, 0x045000306L, 0x045000307L,
        0x045000308L, 0x045000309L, 0x04500030CL, 0x04500030FL, 0x045000311L,
        0x045000323L, 0x045000327L, 0x045000328L, 0x04500032DL, 0x045000330L,
        0x046000307L, 0x047000301L, 0x047000302L, 0x047000304L, 0x047000306L,
        0x047000307L, 0x04700030CL, 0x047000327L, 0x048000302L, 0x048000307L,
        0x048000308L, 0x04800030CL, 0x048000323L, 0x048000327L, 0x04800032EL,
        0x049000300L, 0x049000301L, 0x049000302L, 0x049000303L, 0x049000304L,
        0x049000306L, 0x049000307L, 0x049000308L, 0x049000309L, 0x04900030CL,
        0x04900030FL, 0x049000311L, 0x049000323L, 0x049000328L, 0x049000330L,
        0x04A000302L, 0x04B000301L, 0x04B00030CL, 0x04B000323L, 0x04B000327L,
        0x04B000331L, 0x04C000301L, 0x04C00030CL, 0x04C000323L, 0x04C000327L,
        0x04C00032DL, 0x04C000331L, 0x04D000301L, 0x04D000307L, 0x04D000323L,
        0x04E000300L, 0x04E000301L, 0x04E000303L, 0x04E000307L, 0x04E00030CL,
        0x04E000323L, 0x04E000327L, 0x04E00032DL, 0x04E000331L, 0x04F000300L,
        0x04F000301L, 0x04F000302L, 0x04F000303L, 0x04F000304L, 0x04F000306L,
        0x04F000307L, 0x04F000308L, 0x04F000309L, 0x04F00030BL, 0x04F00030CL,
        0x04F00030FL, 0x04F000311L, 0x04F00031BL, 0x04F000323L, 0x04F000328L,
        0x050000301L, 0x050000307L, 0x052000301L, 0x052000307L, 0x05200030CL,
        0x05200030FL, 0x052000311L, 0x052000323L, 0x052000327L, 0x052000331L,
        0x053000301L, 0x053000302L, 0x053000307L, 0x05300030CL, 0x053000323L,
        0x053000326L, 0x053000327L, 0x054000307L, 0x05400030CL, 0x054000323L,
        0x054000326L, 0x054000327L, 0x05400032DL, 0x054000331L, 0x055000300L,
        0x055000301L, 0x055000302L, 0x055000303L, 0x055000304L, 0x055000306L,
        0x055000308L, 0x055000309L, 0x05500030AL, 0x05500030BL, 0x05500030CL,
        0x05500030FL, 0x055000311L, 0x05500031BL, 0x055000323L, 0x055000324L,
        0x055000328L, 0x05500032DL, 0x055000330L, 0x056000303L, 0x056000323L,
        0x057000300L, 0x057000301L, 0x057000302L, 0x057000307L, 0x057000308L,
        0x057000323L, 0x058000307L, 0x058000308L, 0x059000300L, 0x059000301L,
        0x059000302L, 0x059000303L, 0x059000304L, 0x059000307L, 0x059000308L,
        0x059000309L, 0x059000323L, 0x05A000301L, 0x05A000302L, 0x05A000307L,
        0x05A00030CL, 0x05A000323L, 0x05A000331L, 0x061000300L, 0x061000301L,
        0x061000302L, 0x061000303L, 0x061000304L, 0x061000306L, 0x061000307L,
        0x061000308L, 0x061000309L, 0x06100030AL, 0x06100030CL, 0x06100030FL,
        0x061000311L, 0x061000323L, 0x061000325L, 0x061000328L, 0x062000307L,
        0x062000323L, 0x062000331L, 0x063000301L, 0x063000302L, 0x063000307L,
        0x06300030CL, 0x063000327L, 0x064000307L, 0x06400030CL, 0x064000323L,
        0x064000327L, 0x06400032DL, 0x064000331L, 0x065000300L, 0x065000301L,
        0x065000302L, 0x065000303L, 0x065000304L, 0x065000306L, 0x065000307L,
        0x065000308L, 0x065000309L, 0x06500030CL, 0x06500030FL, 0x065000311L,
        0x065000323L, 0x065000327L, 0x065000328L, 0x06500032DL, 0x065000330L,
        0x066000307L, 0x067000301L, 0x067000302L, 0x067000304L, 0x067000306L,
        0x067000307L, 0x06700030CL, 0x067000327L, 0x068000302L, 0x068000307L,
        0x068000308L, 0x06800030CL, 0x068000323L, 0x068000327L, 0x06800032EL,
        0x068000331L, 0x069000300L, 0x069000301L, 0x069000302L, 0x069000303L,
        0x069000304L, 0x069000306L, 0x069000308L, 0x069000309L, 0x06900030CL,
        0x06900030FL, 0x069000311L, 0x069000323L, 0x069000328L, 0x069000330L,
        0x06A000302L, 0x06A00030CL, 0x06B000301L, 0x06B00030CL, 0x06B000323L,
        0x06B000327L, 0x06B000331L, 0x06C000301L, 0x06C00030CL, 0x06C000323L,
        0x06C000327L, 0x06C00032DL, 0x06C000331L, 0x06D000301L, 0x06D000307L,
        0x06D000323L, 0x06E000300L, 0x06E000301L, 0x06E000303L, 0x06E000307L,
        0x06E00030CL, 0x06E000323L, 0x06E000327L, 0x06E00032DL, 0x06E000331L,
        0x06F000300L, 0x06F000301L, 0x06F000302L, 0x06F000303L, 0x06F000304L,
        0x06F000306L, 0x06F000307L, 0x06F000308L, 0x06F000309L, 0x06F00030BL,
        0x06F00030CL, 0x06F00030FL, 0x06F000311L, 0x06F00031BL, 0x06F000323L,
        0x06F000328L, 0x070000301L, 0x070000307L, 0x072000301L, 0x072000307L,
        0x07200030CL, 0x07200030FL, 0x072000311L, 0x072000323L, 0x072000327L,
        0x072000331L, 0x073000301L, 0x073000302L, 0x073000307L, 0x07300030CL,
        0x073000323L, 0x073000326L, 0x073000327L, 0x074000307L, 0x074000308L,
        0x07400030CL, 0x074000323L, 0x074000326L, 0x074000327L, 0x07400032DL,
        0x074000331L, 0x075000300L, 0x075000301L, 0x075000302L, 0x075000303L,
        0x075000304L, 0x075000306L, 0x075000308L, 0x075000309L, 0x07500030AL,
        0x07500030BL, 0x07500030CL, 0x07500030FL, 0x075000311L, 0x07500031BL,
        0x075000323L, 0x075000324L, 0x075000328L, 0x07500032DL, 0x075000330L,
        0x076000303L, 0x076000323L, 0x077000300L, 0x077000301L, 0x077000302L,
        0x077000307L, 0x077000308L, 0x07700030AL, 0x077000323L, 0x078000307L,
        0x078000308L, 0x079000300L, 0x079000301L, 0x079000302L, 0x079000303L,
        0x079000304L, 0x079000307L, 0x079000308L, 0x079000309L, 0x07900030AL,
        0x079000323L, 0x07A000301L, 0x07A000302L, 0x07A000307L, 0x07A00030CL,
        0x07A000323L, 0x07A000331L, 0x0A8000300L, 0x0A8000301L, 0x0A8000342L,
        0x0C2000300L, 0x0C2000301L, 0x0C2000303L, 0x0C2000309L, 0x0C4000304L,
        0x0C5000301L, 0x0C6000301L, 0x0C6000304L, 0x0C7000301L, 0x0CA000300L,
        0x0CA000301L, 0x0CA000303L, 0x0CA000309L, 0x0CF000301L, 0x0D4000300L,
        0x0D4000301L, 0x0D4000303L, 0x0D4000309L, 0x0D5000301L, 0x0D5000304L,
        0x0D5000308L, 0x0D6000304L, 0x0D8000301L, 0x0DC000300L, 0x0DC000301L,
        0x0DC000304L, 0x0DC00030CL, 0x0E2000300L, 0x0E2000301L, 0x0E2000303L,
        0x0E2000309L, 0x0E4000304L, 0x0E5000301L, 0x0E6000301L, 0x0E6000304L,
        0x0E7000301L, 0x0EA000300L, 0x0EA000301L, 0x0EA000303L, 0x0EA000309L,
        0x0EF000301L, 0x0F4000300L, 0x0F4000301L, 0x0F4000303L, 0x0F4000309L,
        0x0F5000301L, 0x0F5000304L, 0x0F5000308L, 0x0F6000304L, 0x0F8000301L,
        0x0FC000300L, 0x0FC000301L, 0x0FC000304L, 0x0FC00030CL, 0x102000300L,
        0x102000301L, 0x102000303L, 0x102000309L, 0x103000300L, 0x103000301L,
        0x103000303L, 0x103000309L, 0x112000300L, 0x112000301L, 0x113000300L,
        0x113000301L, 0x14C000300L, 0x14C000301L, 0x14D000300L, 0x14D000301L,
        0x15A000307L, 0x15B000307L, 0x160000307L, 0x161000307L, 0x168000301L,
        0x169000301L, 0x16A000308L, 0x16B000308L, 0x17F000307L, 0x1A0000300L,
        0x1A0000301L, 0x1A0000303L, 0x1A0000309L, 0x1A0000323L, 0x1A1000300L,
        0x1A1000301L, 0x1A1000303L, 0x1A1000309L, 0x1A1000323L, 0x1AF000300L,
        0x1AF000301L, 0x1AF000303L, 0x1AF000309L, 0x1AF000323L, 0x1B0000300L,
        0x1B0000301L, 0x1B0000303L, 0x1B0000309L, 0x1B0000323L, 0x1B700030CL,
        0x1EA000304L, 0x1EB000304L, 0x226000304L, 0x227000304L, 0x228000306L,
        0x229000306L, 0x22E000304L, 0x22F000304L, 0x29200030CL, 0x391000300L,
        0x391000301L, 0x391000304L, 0x391000306L, 0x391000313L, 0x391000314L,
        0x391000345L, 0x395000300L, 0x395000301L, 0x395000313L, 0x395000314L,
        0x397000300L, 0x397000301L, 0x397000313L, 0x397000314L, 0x397000345L,
        0x399000300L, 0x399000301L, 0x399000304L, 0x399000306L, 0x399000308L,
        0x399000313L, 0x399000314L, 0x39F000300L, 0x39F000301L, 0x39F000313L,
        0x39F000314L, 0x3A1000314L, 0x3A5000300L, 0x3A5000301L, 0x3A5000304L,
        0x3A5000306L, 0x3A5000308L, 0x3A5000314L, 0x3A9000300L, 0x3A9000301L,
        0x3A9000313L, 0x3A9000314L, 0x3A9000345L, 0x3AC000345L, 0x3AE000345L,
        0x3B1000300L, 0x3B1000301L, 0x3B1000304L, 0x3B1000306L, 0x3B1000313L,
        0x3B1000314L, 0x3B1000342L, 0x3B1000345L, 0x3B5000300L, 0x3B5000301L,
        0x3B5000313L, 0x3B5000314L, 0x3B7000300L, 0x3B7000301L, 0x3B7000313L,
        0x3B7000314L, 0x3B7000342L, 0x3B7000345L, 0x3B9000300L, 0x3B9000301L,
        0x3B9000304L, 0x3B9000306L, 0x3B9000308L, 0x3B9000313L, 0x3B9000314L,
        0x3B9000342L, 0x3BF000300L, 0x3BF000301L, 0x3BF000313L, 0x3BF000314L,
        0x3C1000313L, 0x3C1000314L, 0x3C5000300L, 0x3C5000301L, 0x3C5000304L,
        0x3C5000306L, 0x3C5000308L, 0x3C5000313L, 0x3C5000314L, 0x3C5000342L,
        0x3C9000300L, 0x3C9000301L, 0x3C9000313L, 0x3C9000314L, 0x3C9000342L,
        0x3C9000345L, 0x3CA000300L, 0x3CA000301L, 0x3CA000342L, 0x3CB000300L,
        0x3CB000301L, 0x3CB000342L, 0x3CE000345L, 0x3D2000301L, 0x3D2000308L,
        0x406000308L, 0x410000306L, 0x410000308L, 0x413000301L, 0x415000300L,
        0x415000306L, 0x415000308L, 0x416000306L, 0x416000308L, 0x417000308L,
        0x418000300L, 0x418000304L, 0x418000306L, 0x418000308L, 0x41A000301L,
        0x41E000308L, 0x423000304L, 0x423000306L, 0x423000308L, 0x42300030BL,
        0x427000308L, 0x42B000308L, 0x42D000308L, 0x430000306L, 0x430000308L,
        0x433000301L, 0x435000300L, 0x435000306L, 0x435000308L, 0x436000306L,
        0x436000308L, 0x437000308L, 0x438000300L, 0x438000304L, 0x438000306L,
        0x438000308L, 0x43A000301L, 0x43E000308L, 0x443000304L, 0x443000306L,
        0x443000308L, 0x44300030BL, 0x447000308L, 0x44B000308L, 0x44D000308L,
        0x456000308L, 0x47400030FL, 0x47500030FL, 0x4D8000308L, 0x4D9000308L,
        0x4E8000308L, 0x4E9000308L, 0x627000653L, 0x627000654L, 0x627000655L,
        0x648000654L, 0x64A000654L, 0x6C1000654L, 0x6D2000654L, 0x6D5000654L,
        0x92800093CL, 0x93000093CL, 0x93300093CL, 0x9C70009BEL, 0x9C70009D7L,
        0xB47000B3EL, 0xB47000B56L, 0xB47000B57L, 0xB92000BD7L, 0xBC6000BBEL,
        0xBC6000BD7L, 0xBC7000BBEL, 0xC46000C56L, 0xCBF000CD5L, 0xCC6000CC2L,
        0xCC6000CD5L, 0xCC6000CD6L, 0xCCA000CD5L, 0xD46000D3EL, 0xD46000D57L,
        0xD47000D3EL, 0xDD9000DCAL, 0xDD9000DCFL, 0xDD9000DDFL, 0xDDC000DCAL,
        0x102500102EL, 0x1B05001B35L, 0x1B07001B35L, 0x1B09001B35L, 0x1B0B001B35L,
        0x1B0D001B35L, 0x1B11001B35L, 0x1B3A001B35L, 0x1B3C001B35L, 0x1B3E001B35L,
        0x1B3F001B35L, 0x1B42001B35L, 0x1E36000304L, 0x1E37000304L, 0x1E5A000304L,
        0x1E5B000304L, 0x1E62000307L, 0x1E63000307L, 0x1EA0000302L, 0x1EA0000306L,
        0x1EA1000302L, 0x1EA1000306L, 0x1EB8000302L, 0x1EB9000302L, 0x1ECC000302L,
        0x1ECD000302L, 0x1F00000300L, 0x1F00000301L, 0x1F00000342L, 0x1F00000345L,
        0x1F01000300L, 0x1F01000301L, 0x1F01000342L, 0x1F01000345L, 0x1F02000345L,
        0x1F03000345L, 0x1F04000345L, 0x1F05000345L, 0x1F06000345L, 0x1F07000345L,
        0x1F08000300L, 0x1F08000301L, 0x1F08000342L, 0x1F08000345L, 0x1F09000300L,
        0x1F09000301L, 0x1F09000342L, 0x1F09000345L, 0x1F0A000345L, 0x1F0B000345L,
        0x1F0C000345L, 0x1F0D000345L, 0x1F0E000345L, 0x1F0F000345L, 0x1F10000300L,
        0x1F10000301L, 0x1F11000300L, 0x1F11000301L, 0x1F18000300L, 0x1F18000301L,
        0x1F19000300L, 0x1F19000301L, 0x1F20000300L, 0x1F20000301L, 0x1F20000342L,
        0x1F20000345L, 0x1F21000300L, 0x1F21000301L, 0x1F21000342L, 0x1F21000345L,
        0x1F22000345L, 0x1F23000345L, 0x1F24000345L, 0x1F25000345L, 0x1F26000345L,
        0x1F27000345L, 0x1F28000300L, 0x1F28000301L, 0x1F28000342L, 0x1F28000345L,
        0x1F29000300L, 0x1F29000301L, 0x1F29000342L, 0x1F29000345L, 0x1F2A000345L,
        0x1F2B000345L, 0x1F2C000345L, 0x1F2D000345L, 0x1F2E000345L, 0x1F2F000345L,
        0x1F30000300L, 0x1F30000301L, 0x1F30000342L, 0x1F31000300L, 0x1F31000301L,
        0x1F31000342L, 0x1F38000300L, 0x1F38000301L, 0x1F38000342L, 0x1F39000300L,
        0x1F39000301L, 0x1F39000342L, 0x1F40000300L, 0x1F40000301L, 0x1F41000300L,
        0x1F41000301L, 0x1F48000300L, 0x1F48000301L, 0x1F49000300L, 0x1F49000301L,
        0x1F50000300L, 0x1F50000301L, 0x1F50000342L, 0x1F51000300L, 0x1F51000301L,
        0x1F51000342L, 0x1F59000300L, 0x1F59000301L, 0x1F59000342L, 0x1F60000300L,
        0x1F60000301L, 0x1F60000342L, 0x1F60000345L, 0x1F61000300L, 0x1F61000301L,
        0x1F61000342L, 0x1F61000345L, 0x1F62000345L, 0x1F63000345L, 0x1F64000345L,
        0x1F65000345L, 0x1F66000345L, 0x1F67000345L, 0x1F68000300L, 0x1F68000301L,
        0x1F68000342L, 0x1F68000345L, 0x1F69000300L, 0x1F69000301L, 0x1F69000342L,
        0x1F69000345L, 0x1F6A000345L, 0x1F6B000345L, 0x1F6C000345L, 0x1F6D000345L,
        0x1F6E000345L, 0x1F6F000345L, 0x1F70000345L, 0x1F74000345L, 0x1F7C000345L,
        0x1FB6000345L, 0x1FBF000300L, 0x1FBF000301L, 0x1FBF000342L, 0x1FC6000345L,
        0x1FF6000345L, 0x1FFE000300L, 0x1FFE000301L, 0x1FFE000342L, 0x2190000338L,
        0x2192000338L, 0x2194000338L, 0x21D0000338L, 0x21D2000338L, 0x21D4000338L,
        0x2203000338L, 0x2208000338L, 0x220B000338L, 0x2223000338L, 0x2225000338L,
        0x223C000338L, 0x2243000338L, 0x2245000338L, 0x2248000338L, 0x224D000338L,
        0x2261000338L, 0x2264000338L, 0x2265000338L, 0x2272000338L, 0x2273000338L,
        0x2276000338L, 0x2277000338L, 0x227A000338L, 0x227B000338L, 0x227C000338L,
        0x227D000338L, 0x2282000338L, 0x2283000338L, 0x2286000338L, 0x2287000338L,
        0x2291000338L, 0x2292000338L, 0x22A2000338L, 0x22A8000338L, 0x22A9000338L,
        0x22AB000338L, 0x22B2000338L, 0x22B3000338L, 0x22B4000338L, 0x22B5000338L,
        0x3046003099L, 0x304B003099L, 0x304D003099L, 0x304F003099L, 0x3051003099L,
        0x3053003099L, 0x3055003099L, 0x3057003099L, 0x3059003099L, 0x305B003099L,
        0x305D003099L, 0x305F003099L, 0x3061003099L, 0x3064003099L, 0x3066003099L,
        0x3068003099L, 0x306F003099L, 0x306F00309AL, 0x3072003099L, 0x307200309AL,
        0x3075003099L, 0x307500309AL, 0x3078003099L, 0x307800309AL, 0x307B003099L,
        0x307B00309AL, 0x309D003099L, 0x30A6003099L, 0x30AB003099L, 0x30AD003099L,
        0x30AF003099L, 0x30B1003099L, 0x30B3003099L, 0x30B5003099L, 0x30B7003099L,
        0x30B9003099L, 0x30BB003099L, 0x30BD003099L, 0x30BF003099L, 0x30C1003099L,
        0x30C4003099L, 0x30C6003099L, 0x30C8003099L, 0x30CF003099L, 0x30CF00309AL,
        0x30D2003099L, 0x30D200309AL, 0x30D5003099L, 0x30D500309AL, 0x30D8003099L,
        0x30D800309AL, 0x30DB003099L, 0x30DB00309AL, 0x30EF003099L, 0x30F0003099L,
        0x30F1003099L, 0x30F2003099L, 0x30FD003099L, 0x105D2000307L, 0x105DA000307L,
        0x110990110BAL, 0x1109B0110BAL, 0x110A50110BAL, 0x11131011127L, 0x11132011127L,
        0x1134701133EL, 0x11347011357L, 0x113820113C9L, 0x113840113BBL, 0x1138B0113C2L,
        0x113900113C9L, 0x113C20113B8L, 0x113C20113C2L, 0x113C20113C9L, 0x114B90114B0L,
        0x114B90114BAL, 0x114B90114BDL, 0x115B80115AFL, 0x115B90115AFL, 0x11935011930L,
        0x1611E01611EL, 0x1611E01611FL, 0x1611E016120L, 0x1611E016129L, 0x1612101611FL,
        0x16121016120L, 0x1612201611FL, 0x1612901611FL, 0x16D63016D67L, 0x16D67016D67L,
        0x16D69016D67L,
    };

    /// Whether normalization would join these two into one character.
    private static boolean composes(int base, int mark) {
        if (base < 0) {
            return false;
        }
        long key = ((long) base << 24) | mark;
        int low = 0;
        int high = COMPOSING_PAIRS.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long value = COMPOSING_PAIRS[mid];
            if (value < key) {
                low = mid + 1;
            } else if (value > key) {
                high = mid - 1;
            } else {
                return true;
            }
        }
        return false;
    }

    private static boolean inRanges(int c, int[] ranges) {
        for (int i = 0; i < ranges.length; i += 2) {
            if (c < ranges[i]) {
                // Sorted, so the first range that starts above it ends the search.
                return false;
            }
            if (c <= ranges[i + 1]) {
                return true;
            }
        }
        return false;
    }

    /// The index of the first character that makes the value non-canonical, or -1.
    ///
    /// The rule is one sentence: the only accepted spelling of a name is the normalized one.
    /// Apple's filesystems compare names after canonical normalization and Swift compares
    /// Strings the same way, so two spellings of one name are two strings here and ONE key
    /// there -- the sibling check passes, and then one node overwrites the other.
    ///
    /// There is no normalizer to call. This class is translated for the iOS runtime, which has
    /// no `java.text.Normalizer`, so the property is carried by two tables derived from the
    /// character database instead: the code points normalization rewrites on their own, and the
    /// marks that follow a base in a composition it performs. Between them every unnormalized
    /// spelling is refused and the normalized one is accepted, which is what the sibling check
    /// needs -- including for the scripts a mark-block heuristic gets backwards, where the
    /// DECOMPOSED sequence is the normalized spelling and the precomposed character is not.
    ///
    /// `DocumentIndexSerializerTest` re-derives both halves from `java.text.Normalizer` over the
    /// whole code point space, so neither table can fall behind the property it stands for.
    static int combiningMarkAt(String value) {
        // By CODE POINT, not by char. The supplementary compatibility ideographs live at
        // U+2F800-U+2FA1F and arrive here as surrogate pairs, so a char-by-char scan never sees
        // them -- and U+2F800 is one of the clearest cases there is: it normalizes to U+4E3D, so
        // the two spellings are one filename on an Apple volume and two strings here.
        int i = 0;
        // The code point before the one being looked at, because a mark is only wrong where it
        // composes: -1 until there is one, which is why a name that BEGINS with a mark is
        // accepted -- it composes with nothing, so it is its own normalized spelling.
        int previous = -1;
        while (i < value.length()) {
            // Character.codePointAt, not String.codePointAt: the iOS and CLDC runtimes carry the
            // helper and not the instance method. This module is compiled twice -- by Maven
            // against the JDK, where the instance method exists, and by the Ant build against
            // Ports/CLDC11, where it does not -- so the first accepts what the second refuses,
            // and the Maven tests pass over a core that cannot be built for the device.
            int c = Character.codePointAt(value, i);
            if (inRanges(c, RENORMALIZED) || isConjoiningJamo(c) || composes(previous, c)) {
                return i;
            }
            previous = c;
            i += Character.charCount(c);
        }
        return -1;
    }

    private static int utf8Length(String value) {
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x80) {
                total++;
            } else if (c < 0x800) {
                total += 2;
            } else if (Character.isHighSurrogate(c) && i + 1 < value.length()
                    && Character.isLowSurrogate(value.charAt(i + 1))) {
                total += 4;
                i++;
            } else {
                total += 3;
            }
        }
        return total;
    }

    /// How many bytes the id takes as a directory name in the pre-iOS-16 provider's storage.
    ///
    /// That provider gives each item a directory of its own so two items may share a file name,
    /// and it percent-escapes the id to keep it a single component -- everything outside
    /// lowercase alphanumerics, "-" and "_" becomes three characters, uppercase included, which
    /// is what keeps ids that differ only in case apart on a case-insensitive volume. So the
    /// budget is spent on the ESCAPED form, and an id of plain ASCII digits costs one byte each
    /// while one of accented text costs six.
    private static int storagePathLength(String id) {
        // The PREFIXED identifier, because that is what the provider encodes. Ids are namespaced
        // before they become NSFileProviderItemIdentifiers -- see validate() -- and the ":" is
        // not unreserved, so the four characters cost six bytes in the directory name. Measuring
        // the bare id let a 250-character one through and produced a 256-byte component, which
        // is one past what the filesystem takes.
        String component = IDENTIFIER_NAMESPACE + id;
        int unreserved = 0;
        for (int i = 0; i < component.length(); i++) {
            char c = component.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                unreserved++;
            }
        }
        // An unreserved character survives as its own byte; every other BYTE becomes "%XX".
        // Counted in bytes rather than characters so a surrogate pair costs the twelve it really
        // costs, not the six a per-character count would guess.
        return unreserved + 3 * (utf8Length(component) - unreserved);
    }

    /// The prefix the Apple providers put in front of every application id.
    ///
    /// Kept here so the length budget measures what actually becomes a path component. It is not
    /// this class's job to produce the namespaced identifier -- the provider does that -- but it
    /// is this class's job to refuse an id that cannot be stored.
    private static final String IDENTIFIER_NAMESPACE = "cn1:";

    private static String findDuplicateId(DocumentNode node, Set<String> seen) {
        if (!seen.add(node.getId())) {
            return node.getId();
        }
        for (DocumentNode child : node.getChildren()) {
            String duplicate = findDuplicateId(child, seen);
            if (duplicate != null) {
                return duplicate;
            }
        }
        return null;
    }

    private static Map<String, Object> toMap(DocumentNode node) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", node.getId());
        if (node.getName() != null) {
            m.put("name", node.getName());
        }
        m.put("folder", Boolean.valueOf(node.isFolder()));
        if (node.getContentType() != null) {
            m.put("contentType", node.getContentType());
        }
        if (node.getPath() != null) {
            m.put("path", node.getPath());
        }
        if (node.getRemoteId() != null) {
            m.put("remoteId", node.getRemoteId());
        }
        if (node.getSize() >= 0) {
            m.put("size", Long.valueOf(node.getSize()));
        }
        if (node.getLastModified() >= 0) {
            m.put("lastModified", Long.valueOf(node.getLastModified()));
        }
        if (node.isFolder()) {
            List<DocumentNode> kids = node.getChildren();
            List<Object> out = new ArrayList<Object>(kids.size());
            for (DocumentNode k : kids) {
                out.add(toMap(k));
            }
            m.put("children", out);
        }
        return m;
    }

    private static DocumentNode fromMap(Map m) throws IOException {
        Object id = m.get("id");
        if (id == null) {
            throw new IOException("Document node has no id");
        }
        DocumentNode node = new DocumentNode(id.toString(), str(m.get("name")),
                bool(m.get("folder")));
        node.setContentType(str(m.get("contentType")));
        node.setPath(str(m.get("path")));
        node.setRemoteId(str(m.get("remoteId")));
        node.setSize(num(m.get("size")));
        node.setLastModified(num(m.get("lastModified")));
        Object children = m.get("children");
        if (children instanceof List) {
            for (Object child : (List) children) {
                if (child instanceof Map) {
                    node.add(fromMap((Map) child));
                }
            }
        }
        return node;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static boolean bool(Object o) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        return o != null && "true".equals(o.toString());
    }

    // The parser hands back whatever number type the text implied -- Double for a plain integer
    // literal in most cases -- so this reads through Number rather than casting to Long, which
    // would be a cast whose failure iOS could not report (see the CHECKCAST note in CLAUDE.md).
    private static long num(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o == null) {
            return -1;
        }
        try {
            return Long.parseLong(o.toString().trim());
        } catch (NumberFormatException err) {
            return -1;
        }
    }
}
