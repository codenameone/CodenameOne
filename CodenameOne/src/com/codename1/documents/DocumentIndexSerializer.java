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

    /// Code points that follow a base in a composition the normalizer PERFORMS.
    ///
    /// These are what a decomposed spelling is made of -- "e" followed by U+0301 -- so refusing
    /// them refuses the decomposed half of every composable pair and keeps the composed one,
    /// which is what normalization produces and what a person types. The voiced kana are the
    /// case that matters most in practice: a file copied from a Mac arrives as the kana followed
    /// by U+3099, and the app's own spelling is the single character.
    ///
    /// Only the compositions that are PERFORMED, and only marks whose refusal cannot cost a
    /// normalized name. Unicode excludes some compositions, and there the decomposed sequence IS
    /// the normalized spelling: U+FB2A is U+05E9 U+05C1 and U+0958 is U+0915 U+093C, so refusing
    /// their marks would refuse normalized Hebrew and the Indic nukta letters outright. U+093C
    /// and U+0338 appear in both kinds and are left out for that reason; the marks that appear
    /// only in a normalized spelling made of OTHER marks, like U+0301 in U+0344, stay in, since
    /// refusing those costs nothing a name can carry.
    ///
    /// Derived from the character database rather than chosen, and re-derived by the test.
    /// Sorted pairs of inclusive bounds.
    private static final int[] COMPOSING_TAIL = {
        0x0300, 0x0304, 0x0306, 0x030C, 0x030F, 0x030F, 0x0311, 0x0311,
        0x0313, 0x0314, 0x031B, 0x031B, 0x0323, 0x0328, 0x032D, 0x032E,
        0x0330, 0x0331, 0x0342, 0x0342, 0x0345, 0x0345, 0x0653, 0x0655,
        0x09BE, 0x09BE, 0x09D7, 0x09D7, 0x0B3E, 0x0B3E, 0x0B56, 0x0B57,
        0x0BBE, 0x0BBE, 0x0BD7, 0x0BD7, 0x0C56, 0x0C56, 0x0CC2, 0x0CC2,
        0x0CD5, 0x0CD6, 0x0D3E, 0x0D3E, 0x0D57, 0x0D57, 0x0DCA, 0x0DCA,
        0x0DCF, 0x0DCF, 0x0DDF, 0x0DDF, 0x102E, 0x102E, 0x1B35, 0x1B35,
        0x3099, 0x309A, 0x110BA, 0x110BA, 0x11127, 0x11127, 0x1133E, 0x1133E,
        0x11357, 0x11357, 0x113B8, 0x113B8, 0x113BB, 0x113BB, 0x113C2, 0x113C2,
        0x113C9, 0x113C9, 0x114B0, 0x114B0, 0x114BA, 0x114BA, 0x114BD, 0x114BD,
        0x115AF, 0x115AF, 0x11930, 0x11930, 0x1611E, 0x16120, 0x16129, 0x16129,
        0x16D67, 0x16D67,
    };

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
        while (i < value.length()) {
            // Character.codePointAt, not String.codePointAt: the iOS and CLDC runtimes carry the
            // helper and not the instance method. This module is compiled twice -- by Maven
            // against the JDK, where the instance method exists, and by the Ant build against
            // Ports/CLDC11, where it does not -- so the first accepts what the second refuses,
            // and the Maven tests pass over a core that cannot be built for the device.
            int c = Character.codePointAt(value, i);
            if (inRanges(c, RENORMALIZED) || inRanges(c, COMPOSING_TAIL)
                    || isConjoiningJamo(c)) {
                return i;
            }
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
