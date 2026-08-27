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
        doc.put("root", toMap(root));
        return JSONWriter.toJson(doc);
    }

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
