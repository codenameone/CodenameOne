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
package com.codename1.builders;

/**
 * Builds the AndroidManifest permission fragments injected when an application
 * references {@code com.codename1.location.LocationButton}.
 *
 * <p>Extracted into a pure static helper for the reasons
 * {@link BluetoothManifestFragments} gives: the conditional nuances are
 * unit-testable here, and the BuildDaemon copy of this class stays trivially
 * diffable -- <b>keep this file in sync with
 * {@code com.codename1.build.daemon.LocationButtonManifestFragments}</b>.</p>
 *
 * <h2>What Google Play requires</h2>
 *
 * <p>From 2027-01-27, an application targeting Android 17 (API 37) or later
 * that needs precise location for a <i>transactional</i> purpose -- one-time
 * "near me", an address fill, a single share -- must obtain it through the
 * system-rendered location button rather than a persistent
 * {@code ACCESS_FINE_LOCATION} grant. Persistent access remains available for
 * core functionality (navigation, tracking, geofencing) and carries a Play
 * Console declaration instead.</p>
 *
 * <p>Two manifest facts follow, and only one of them can be inferred:</p>
 *
 * <ul>
 *   <li>{@code USE_LOCATION_BUTTON} is what lets the application render the
 *       system button at all. Injected whenever the button is referenced --
 *       an application that asks for the control needs the permission that
 *       draws it, and there is nothing to decide.</li>
 *   <li>{@code usesPermissionFlags="onlyForLocationButton"} on
 *       {@code ACCESS_FINE_LOCATION} declares that precise location is
 *       reachable through the button and through nothing else. That is
 *       <b>not</b> inferable. It is the same question the Play Console asks --
 *       is precise location core functionality here? -- and answering it wrong
 *       in the permissive direction costs a declaration, while answering it
 *       wrong in the restrictive direction silently breaks every
 *       {@code getCurrentLocationSync} call in the application. So it comes
 *       from the {@code android.locationButton.exclusive} build hint and
 *       defaults to off.</li>
 * </ul>
 *
 * <p>Duplicate suppression uses quote-delimited tokens for the reason
 * {@link NearbyManifestFragments} documents. This class edits declarations it
 * did not write as well as adding its own, which nothing else here has to do:
 * an application that uses Bluetooth, Wi-Fi management or the nearby transport
 * has already had {@code ACCESS_FINE_LOCATION} declared with a
 * {@code maxSdkVersion} cap by the time this runs, and a plain add would leave
 * the cap in place -- a location button wired on Android 17 to a permission the
 * manifest stops granting at API 32.</p>
 *
 * <p>The attribute parser below is this class's own rather than the one
 * {@link NearbyManifestFragments} carries, and that is the mirror rule rather
 * than an oversight: the BuildDaemon has no copy of that class, so a call into
 * it could not be mirrored and the two halves of this feature would have
 * diverged at the first edit.</p>
 */
final class LocationButtonManifestFragments {

    /**
     * Bumped when the fragments change, so a build log names which version
     * produced a manifest.
     */
    static final int FRAGMENT_VERSION = 1;

    /** The permission that lets an app render the system button. */
    static final String USE_LOCATION_BUTTON =
            "android.permission.USE_LOCATION_BUTTON";

    static final String FINE_LOCATION =
            "android.permission.ACCESS_FINE_LOCATION";

    static final String COARSE_LOCATION =
            "android.permission.ACCESS_COARSE_LOCATION";

    /**
     * The permission a persistent precise-location grant needs.
     *
     * <p>Named here because the exclusivity check has to see it declared by
     * HAND as well as inferred from bytecode -- see
     * {@link #declaresBackgroundLocation(String)}.</p>
     */
    static final String BACKGROUND_LOCATION =
            "android.permission.ACCESS_BACKGROUND_LOCATION";

    /** The flag that restricts precise location to the button. */
    static final String ONLY_FOR_LOCATION_BUTTON = "onlyForLocationButton";

    /**
     * Whether the project's own block actively REMOVES background location.
     *
     * <p>{@code tools:node="remove"} in the application's manifest outranks a
     * library that contributed the permission, and the merger honours it: the
     * merged manifest does not request background location at all. Folding an
     * archive's own declaration into the exclusivity check regardless refused
     * the build of a developer who had done exactly the right thing to qualify
     * for the hint -- which is the same mistake, from the other side, as
     * reading their removal as a declaration.</p>
     *
     * @param xPermissions the permission block, or null
     * @return whether some live element removes the background permission
     */
    static java.util.Set<String> removesBackgroundLocation(
            String xPermissions) {
        java.util.Set<String> removed = new java.util.TreeSet<String>();
        if (xPermissions == null) {
            return removed;
        }
        int at = xPermissions.indexOf(BACKGROUND_LOCATION);
        while (at >= 0) {
            if (declaresPermissionAt(xPermissions, at, BACKGROUND_LOCATION)
                    && isRemovalDirective(xPermissions, at)
                    && !isSelectorScoped(xPermissions, at)) {
                int open = xPermissions.lastIndexOf('<', at);
                if (open >= 0) {
                    removed.add(elementName(xPermissions, open));
                }
            }
            at = xPermissions.indexOf(BACKGROUND_LOCATION,
                    at + BACKGROUND_LOCATION.length());
        }
        return removed;
    }

    /**
     * The element types that actively ask for background location in
     * {@code text}.
     *
     * @param text a manifest or permission block
     * @return the tag names, which may be empty
     */
    static java.util.Set<String> backgroundElements(String text) {
        java.util.Set<String> tags = new java.util.TreeSet<String>();
        if (text == null) {
            return tags;
        }
        int at = text.indexOf(BACKGROUND_LOCATION);
        while (at >= 0) {
            if (declaresPermissionAt(text, at, BACKGROUND_LOCATION)
                    && !isRemovalDirective(text, at)) {
                int open = text.lastIndexOf('<', at);
                if (open >= 0) {
                    tags.add(elementName(text, open));
                }
            }
            at = text.indexOf(BACKGROUND_LOCATION,
                    at + BACKGROUND_LOCATION.length());
        }
        return tags;
    }

    /**
     * Whether the manifest permissions accumulated so far already ask for
     * background location.
     *
     * <p>The builder's own {@code backgroundLocationPermission} is set from
     * BYTECODE alone -- a call to the geofencing or background-listener API. A
     * project can also ask for the permission outright, through
     * {@code android.permission.ACCESS_BACKGROUND_LOCATION=true}, through
     * {@code android.uses_permission.*}, or by writing the element into
     * {@code android.xpermissions}; all three land in the same string long
     * before the location button is considered, and none of them sets that
     * flag. Reading only the flag let
     * {@code android.locationButton.exclusive=true} through beside an explicit
     * background request and produced a manifest that contradicts itself: the
     * permission is asked for, and {@code onlyForLocationButton} on fine
     * location stops it ever working. The build has to refuse that the same way
     * it refuses the inferred case.</p>
     *
     * @param xPermissions the manifest permission block built so far, or null
     * @return whether it already names the background-location permission
     */
    static boolean declaresBackgroundLocation(String xPermissions) {
        if (xPermissions == null) {
            return false;
        }
        int at = xPermissions.indexOf(BACKGROUND_LOCATION);
        while (at >= 0) {
            if (declaresPermissionAt(xPermissions, at, BACKGROUND_LOCATION)
                    && !isRemovalDirective(xPermissions, at)) {
                return true;
            }
            at = xPermissions.indexOf(BACKGROUND_LOCATION,
                    at + BACKGROUND_LOCATION.length());
        }
        return false;
    }

    /**
     * Whether {@code at} falls inside an XML comment.
     *
     * <p>By the nearest {@code <!--} before it and where that comment closes,
     * not by the nearest {@code <}. A commented-out element --
     * {@code <!-- <uses-permission android:name="..." /> -->} is how a project
     * parks a declaration it does not currently want -- contains its own
     * {@code <}, so looking backwards for one lands INSIDE the comment and
     * reports live markup.</p>
     *
     * @param text the permission block
     * @param at   an offset within it
     * @return whether that offset is commented out
     */
    private static boolean isInsideComment(String text, int at) {
        int open = text.lastIndexOf("<!--", at);
        if (open < 0) {
            return false;
        }
        int close = text.indexOf("-->", open);
        return close < 0 || close > at;
    }

    /**
     * Whether {@code name} is asked for by an ACTIVE element in {@code text}.
     *
     * <p>What duplicate detection has to mean. A quoted-token search reported a
     * commented-out declaration as present, so nothing real was added and the
     * button shipped without the permission its grant needs.</p>
     *
     * @param text the permission block
     * @param name the permission
     * @return whether some live uses-permission element requests it
     */
    // WHAT THIS COMPARISON IS, AND WHAT IT IS NOT.
    //
    // Lexical, on purpose. It reads the text of an attribute value and matches
    // it against a permission name; it does not resolve what an XML PARSER
    // would make of that text. So a manifest that spells the permission with a
    // character reference -- android:name="android.permission.ACCESS&#95;
    // BACKGROUND_LOCATION", which is legal XML that aapt resolves back to an
    // underscore -- is not recognised here, and nor would CDATA or an entity
    // declared in a DTD be. That is a true gap, not an oversight.
    //
    // It is left open because closing it properly means parsing, and this
    // class hand-parses for reasons that have not changed: it is mirrored
    // byte-for-byte into the BuildDaemon, which cannot take the dependency,
    // and every lexical feature closed one at a time is another asymmetry to
    // get wrong -- the namespace work in this file went round four times
    // before every caller agreed with every other.
    //
    // And the cost of the gap is bounded in a way the namespace cases were
    // not. This check exists to catch an HONEST contradiction and explain it
    // to the developer who wrote it: it refuses their build and names the
    // reason. Missing one does not hand anybody else anything -- the manifest
    // merger still assembles the real manifest and Play still reads it -- it
    // just means a developer who wrote their permission in a form no tool
    // emits gets the manifest they asked for instead of an explanation. Nobody
    // gains by evading a check whose only effect is to protect them.
    //
    // If this ever has to be a boundary rather than an explanation, the answer
    // is a real parser on both sides of the mirror, not another special case.
    private static boolean declaresPermission(String text, String name) {
        if (text == null) {
            return false;
        }
        int at = text.indexOf(name);
        while (at >= 0) {
            if (declaresPermissionAt(text, at, name)
                    && !isRemovalDirective(text, at)) {
                return true;
            }
            at = text.indexOf(name, at + name.length());
        }
        return false;
    }

    /** Where {@code name} is asked for by a live element, or -1. */
    private static int activePermissionIndex(String text, String name) {
        if (text == null) {
            return -1;
        }
        int at = text.indexOf(name);
        while (at >= 0) {
            if (declaresPermissionAt(text, at, name)) {
                return at;
            }
            at = text.indexOf(name, at + name.length());
        }
        return -1;
    }

    /**
     * Whether the text around {@code at} is a real {@code uses-permission}
     * element asking for {@code name}.
     *
     * <p>A plain substring search is not enough, because this block is the
     * project's own XML and a project may say the permission's name without
     * requesting it. The case that matters is a COMMENT -- {@code <!-- do not
     * request ACCESS_BACKGROUND_LOCATION -->} is a reasonable note to leave
     * beside a location button, and reading it as a request refused the build
     * of somebody documenting the very thing the hint requires of them.</p>
     *
     * <p>Three conditions, all cheap: the element is not a comment, its tag is
     * {@code uses-permission}, and its {@code android:name} is this permission
     * exactly rather than one that merely contains it.</p>
     *
     * @param text the permission block
     * @param at   where the permission name was found
     * @param name the permission being asked about
     * @return whether that occurrence is an actual request
     */
    private static boolean declaresPermissionAt(String text, int at,
            String name) {
        int open = text.lastIndexOf('<', at);
        int close = text.indexOf('>', at);
        if (open < 0 || close < 0) {
            return false;
        }
        if (isInsideComment(text, at)) {
            return false;
        }
        String element = text.substring(open, close + 1);
        String tag = element.substring(1).trim();
        if (!tag.startsWith("uses-permission")) {
            return false;
        }
        // EVERY candidate spelling, not the first one that happens to carry a
        // "name" attribute. Prefix bindings are collected document-wide, so a
        // manifest that rebinds the conventional prefix on this element could
        // otherwise put a decoy under it -- the decoy is found first, reports
        // some other permission, and the real ACCESS_BACKGROUND_LOCATION under
        // a second prefix is never looked at. Asking each spelling whether it
        // names THIS permission cannot be hidden from that way.
        String[] prefixes = candidatePrefixes(element, text, ANDROID_NS,
                "android");
        for (int iter = 0; iter < prefixes.length; iter++) {
            int[] value = findAttribute(element, prefixes[iter] + ":name");
            if (value != null
                    && name.equals(element.substring(value[2], value[3])
                            .trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes any {@code tools:node="remove"} element naming {@code name}.
     *
     * <p>A removal directive is how a project or a cn1lib strips a permission
     * it does not want, and the manifest merger honours it over any declaration
     * added beside it. So for a permission this class is about to REQUIRE, the
     * directive cannot be left in place and cannot be worked around by adding a
     * second element: it has to go.</p>
     *
     * <p>Without this the button was declared and its permission removed --
     * {@code declareUncapped} found the removal element, read it as an existing
     * declaration, found no {@code maxSdkVersion} to widen and returned the
     * block untouched. The build then produced an app whose location button
     * cannot be granted the one permission it exists to obtain.</p>
     *
     * @param xPermissions the permission block
     * @param name         the permission being required
     * @return the block with any removal of that permission deleted
     */
    private static String stripRemovals(String xPermissions, String name) {
        String out = xPermissions == null ? "" : xPermissions;
        int at = out.indexOf(name);
        while (at >= 0) {
            // A removal of THIS permission, matched as a whole element. The
            // name is a substring of longer ones -- a project may remove
            // com.example.android.permission.ACCESS_FINE_LOCATION, its own
            // custom permission -- and deleting that directive lets the
            // permission it was suppressing back into the merged manifest.
            if (!declaresPermissionAt(out, at, name)
                    || !isRemovalDirective(out, at)) {
                int next = out.indexOf(name, at + name.length());
                if (next < 0) {
                    return out;
                }
                at = next;
                continue;
            }
            int start = out.lastIndexOf('<', at);
            int end = out.indexOf('>', at);
            if (start < 0 || end < 0) {
                return out;
            }
            String tail = out.substring(end + 1);
            // A removal need not be self-closing.
            // <uses-permission ... tools:node="remove"></uses-permission> is
            // valid, and deleting only the opening tag left the closing one
            // behind -- a manifest fragment that no longer parses, produced by
            // the very path that is supposed to be tidying it up.
            if (out.charAt(end - 1) != '/') {
                // The closing tag of THIS element, derived from its opening
                // tag. declaresPermissionAt accepts any tag beginning
                // "uses-permission", which includes uses-permission-sdk-23,
                // and a hard-coded </uses-permission> does not consume
                // </uses-permission-sdk-23> -- so the opening tag was spliced
                // out and its closing tag left behind, which is the orphan
                // that stops the manifest parsing at all.
                // Past whitespace AND comments, not whitespace alone. A
                // removal may carry its reason with it --
                // <uses-permission ... tools:node="remove"><!-- why -->
                // </uses-permission> -- and skipping only spaces left the
                // closing tag unconsumed while the opening one was spliced
                // out. The result is an orphan </uses-permission>: a fragment
                // that no longer parses, produced by the path that is
                // supposed to be tidying it up, which is the very failure the
                // non-self-closing case was added to fix.
                int lead = skipIgnorable(tail, 0);
                String close = "</" + elementName(out, start) + ">";
                if (tail.regionMatches(lead, close, 0, close.length())) {
                    tail = tail.substring(lead + close.length());
                }
            }
            if (tail.startsWith("\r\n")) {
                tail = tail.substring(2);
            } else if (tail.startsWith("\n")) {
                tail = tail.substring(1);
            }
            out = out.substring(0, start) + tail;
            at = out.indexOf(name);
        }
        return out;
    }

    /**
     * The element's tag name, read off its opening tag.
     *
     * @param text  the block
     * @param start the index of the {@code <}
     * @return the name between it and the first space, slash or {@code >}
     */
    private static String elementName(String text, int start) {
        int at = start + 1;
        int end = at;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '/'
                    || c == '>') {
                break;
            }
            end++;
        }
        return text.substring(at, end);
    }

    /**
     * Advances past whitespace and complete comments.
     *
     * <p>What may legally sit between an empty element's tags: nothing that
     * carries meaning, but not necessarily nothing at all. Anything else is
     * left where it is -- {@code uses-permission} has no content model, so
     * text between its tags is not something this method should learn to
     * step over.</p>
     *
     * @param text the block
     * @param from where to start
     * @return the first index that is neither whitespace nor a comment
     */
    private static int skipIgnorable(String text, int from) {
        int at = from;
        while (at < text.length()) {
            char c = text.charAt(at);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                at++;
                continue;
            }
            if (text.startsWith("<!--", at)) {
                int end = text.indexOf("-->", at + 4);
                if (end < 0) {
                    // Unterminated, so there is no "past" it to reach.
                    return at;
                }
                at = end + 3;
                continue;
            }
            return at;
        }
        return at;
    }

    /**
     * Whether the element around {@code at} is scoped by {@code tools:selector}.
     *
     * <p>A selector restricts a node marker to the one dependency it names, so
     * a scoped removal takes the permission out of THAT library and leaves any
     * other library's alone. The flag it would otherwise clear is an aggregate
     * over every submitted archive, and clearing it on a scoped removal
     * accepted exclusive mode while another archive's declaration went on into
     * the merged manifest.</p>
     *
     * <p>So only an unscoped removal clears it. That is conservative rather
     * than exact: a developer who scoped a removal at the only library that
     * declared the permission is refused a hint they would in fact qualify
     * for, and the way out is to drop the selector. Being exact means
     * attributing each declaration to the archive it came from, which this
     * aggregate scan does not do -- and erring the other way lets a manifest
     * through that contradicts itself, which is the thing the check exists to
     * stop.</p>
     *
     * @param text the permission block
     * @param at   where the permission name was found
     * @return whether that element carries a selector
     */
    private static boolean isSelectorScoped(String text, int at) {
        int open = text.lastIndexOf('<', at);
        int close = text.indexOf('>', at);
        if (open < 0 || close < 0) {
            return false;
        }
        String element = text.substring(open, close);
        String[] prefixes = candidatePrefixes(element, text, TOOLS_NS, "tools");
        for (int iter = 0; iter <= prefixes.length; iter++) {
            int[] value = iter < prefixes.length
                    ? findAttribute(element, prefixes[iter] + ":selector")
                    : findAttribute(element, "selector");
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the element around {@code at} REMOVES the permission rather than
     * asking for it.
     *
     * <p>{@code tools:node="remove"} is how a project strips a permission a
     * library contributed, and it is exactly what somebody wanting
     * {@code android.locationButton.exclusive} would write to get rid of a
     * transitive background-location request. Reading it as a declaration
     * refused the build of the one developer the hint exists for, for having
     * done the right thing.</p>
     *
     * <p>Bounded to the element the match sits in -- from the {@code <} before
     * it to the {@code >} after -- so a removal of one permission cannot excuse
     * a genuine request for another somewhere else in the same block.</p>
     *
     * @param text the permission block
     * @param at   where the permission name was found
     * @return whether that element is a removal directive
     */
    private static boolean isRemovalDirective(String text, int at) {
        int open = text.lastIndexOf('<', at);
        int close = text.indexOf('>', at);
        if (open < 0) {
            open = 0;
        }
        if (close < 0) {
            close = text.length();
        }
        if (isInsideComment(text, at)) {
            // Commented out: neither a declaration nor a removal, and nothing
            // in a comment should be edited or deleted.
            return false;
        }
        String element = text.substring(open, close);
        // Through findAttribute, which tolerates the whitespace XML allows:
        // node = "remove" with spaces around the equals is valid and an exact
        // substring test read it as an ordinary declaration. It also handles
        // either quote style and refuses a name that is only the tail of a
        // longer attribute.
        //
        // "tools:node" first and bare "node" second, because findAttribute
        // deliberately refuses a name preceded by anything other than
        // whitespace or '<' -- that guard is what stops android:maxSdkVersion
        // matching inside tools:android:maxSdkVersion, and it means the
        // prefixed spelling has to be asked for by its whole name.
        // Asked of EVERY candidate spelling, like the permission name is. The
        // short-circuiting form settled on whichever "node" it found first, so
        // a decoy under a rebound prefix -- tools:node="keep" beside a real
        // a:node="remove" -- answered for the element. That direction matters:
        // a removal read as a declaration only refuses a build, while a
        // declaration read as a removal lets an exclusive build through over a
        // permission that is genuinely being asked for.
        String[] prefixes = candidatePrefixes(element, text, TOOLS_NS,
                "tools");
        for (int iter = 0; iter <= prefixes.length; iter++) {
            // The bare spelling last: findAttribute refuses a name preceded by
            // anything but whitespace or '<', so "node" cannot match inside
            // "tools:node" and has to be asked for separately.
            int[] value = iter < prefixes.length
                    ? findAttribute(element, prefixes[iter] + ":node")
                    : findAttribute(element, "node");
            if (value == null) {
                continue;
            }
            String marker = element.substring(value[2], value[3]).trim();
            // "removeAll" as well as "remove". Both are removals to the
            // merger, and reading removeAll as an ordinary declaration was
            // worse than missing it: declareUncapped took the marker for the
            // active declaration it was looking for, added no real permission,
            // and the merger then applied the removal -- so the button was
            // built with the one permission it exists to obtain stripped out.
            if ("remove".equals(marker) || "removeAll".equals(marker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The minimum SDK the AndroidX location button library declares. A build
     * below it fails the manifest merge rather than the compile, with a
     * message that names the library and not the feature.
     */
    static final int MINIMUM_SDK = 24;

    private LocationButtonManifestFragments() {
    }

    /**
     * Returns {@code xPermissions} with the location button entries prepended.
     *
     * @param xPermissions the current accumulated manifest fragment
     * @param exclusive    the application asserts that precise location is
     *                     reachable through the button alone
     * @return the fragment with the location button entries applied
     */
    static String inject(String xPermissions, boolean exclusive) {
        String out = xPermissions == null ? "" : xPermissions;
        // Removal directives first, for every permission this is about to
        // require. See stripRemovals: the merger honours a removal over
        // anything added beside it, so leaving one in place shipped a button
        // that could never be granted.
        out = stripRemovals(out, USE_LOCATION_BUTTON);
        out = stripRemovals(out, FINE_LOCATION);
        out = stripRemovals(out, COARSE_LOCATION);
        // Through the same path as the two below, not a plain add. A plain
        // add suppresses a duplicate and keeps whatever cap the existing
        // declaration carries -- and a capped USE_LOCATION_BUTTON is the one
        // permission whose absence makes the control itself unavailable, on
        // the very platform that introduced it.
        out = declareUncapped(out, USE_LOCATION_BUTTON);
        out = removeCapAcrossMerge(out, USE_LOCATION_BUTTON);

        // Both standard permissions, declared and uncapped.
        //
        // Declared, because the button needs them: the session grant it hands
        // back IS ACCESS_FINE_LOCATION, and a permission the manifest does not
        // name cannot be granted by anything. The gpsPermission block declares
        // them too for an application that names com.codename1.location
        // directly, but an application whose only use of the button is inside
        // a cn1lib never trips that block -- the class scan behind it reads
        // the loose class tree and not the libraries.
        //
        // Uncapped, because Bluetooth, Wi-Fi management and the nearby
        // transport each declare ACCESS_FINE_LOCATION with a maxSdkVersion of
        // their own, and whichever ran first wins a plain add. An application
        // that scans for beacons and also shows a location button would have
        // shipped the button against a permission the manifest stopped
        // granting at API 30.
        out = declareUncapped(out, FINE_LOCATION);
        // And across the MERGE, not just in this block. declareUncapped strips
        // a cap that is here; a lower-priority manifest's cap is not here yet.
        // The merger takes the union of an element's attributes, so a
        // submitted aar declaring ACCESS_FINE_LOCATION with its own
        // maxSdkVersion -- which older Bluetooth and location libraries
        // commonly do -- has that attribute merged INTO the declaration added
        // above, and the button ships against a permission the manifest stops
        // granting. tools:remove is the merger's own answer to that, and it
        // reaches caps this scan never sees, including an aar resolved through
        // android.gradleDependencies.
        out = removeCapAcrossMerge(out, FINE_LOCATION);
        // COARSE alongside FINE. From Android 12 the two are granted together
        // -- the system shows one dialog offering precise or approximate -- and
        // the platform button requests both.
        out = declareUncapped(out, COARSE_LOCATION);
        out = removeCapAcrossMerge(out, COARSE_LOCATION);

        if (exclusive) {
            out = addPermissionFlag(out, FINE_LOCATION,
                    ONLY_FOR_LOCATION_BUTTON);
        }
        return out;
    }

    /**
     * True when the application's own configuration says precise location is
     * reachable through the button and nothing else.
     *
     * @param hint the {@code android.locationButton.exclusive} value, or null
     * @return whether to restrict {@code ACCESS_FINE_LOCATION} to the button
     */
    static boolean isExclusive(String hint) {
        return "true".equalsIgnoreCase(hint == null ? null : hint.trim());
    }

    /**
     * Names the reason an exclusive build is contradicted by its own manifest,
     * or null when it is not.
     *
     * <p>{@code onlyForLocationButton} says precise location arrives through
     * the button. A background-location or geofencing application is saying
     * the opposite in the same manifest, and the platform resolves that by
     * refusing the persistent grant -- so the feature that needed it stops
     * working on Android 17 with nothing in the build to explain it. Reported
     * so the developer sees it here instead.</p>
     *
     * @param exclusive          the hint was set
     * @param backgroundLocation the build declares ACCESS_BACKGROUND_LOCATION
     * @param persistentApi      the application calls a location API whose
     *                           grant has to outlive the session --
     *                           {@code addGeoFencing} or
     *                           {@code setBackgroundLocationListener}
     * @return the message, or null when there is no conflict
     */
    static String exclusiveConflict(boolean exclusive,
            boolean backgroundLocation, boolean persistentApi) {
        if (!exclusive || (!backgroundLocation && !persistentApi)) {
            return null;
        }
        return "android.locationButton.exclusive=true restricts"
                + " ACCESS_FINE_LOCATION to the location button, but this build"
                + " also uses "
                + (backgroundLocation && persistentApi
                        ? "background location, and a location API of its own"
                        : backgroundLocation
                                ? "background location"
                                : "a location API of its own -- geofencing, a"
                                        + " background location listener, or"
                                        + " continuous updates through"
                                        + " setLocationListener")
                + ". Precise location reaches those through the ordinary"
                + " permission, which this hint gives away: every request that"
                + " is not the button is answered with an approximate location"
                + " instead. Remove the hint and declare the use in the Play"
                + " Console, or drop the location APIs.";
    }

    private static String addPermission(String xPermissions, String name) {
        // An ACTIVE element, not any quoted occurrence: a commented-out
        // declaration read as "already present" and left the manifest without
        // the permission the button cannot be granted without.
        if (declaresPermission(xPermissions, name)) {
            return xPermissions;
        }
        return "    <uses-permission android:name=\"" + name + "\" />\n"
                + xPermissions;
    }

    /**
     * Adds one {@code android:usesPermissionFlags} value to a permission that
     * is already declared.
     *
     * <p>Additive rather than assigning, because the attribute is a flag set.
     * Assigning would drop a {@code neverForLocation} another injector had
     * written there, and the permission would start counting as a location
     * access again.</p>
     *
     * @param xPermissions the fragment so far
     * @param name         the permission
     * @param flag         the flag to add
     * @return the fragment with the flag present on that permission
     */
    static String addPermissionFlag(String xPermissions, String name,
            String flag) {
        // The ACTIVE element, like every other lookup here. A commented-out
        // declaration sitting before the live one used to win, and the flag was
        // written INSIDE the comment: the real element kept ordinary precise
        // access and the build claimed to be exclusive.
        int at = activePermissionIndex(xPermissions, name);
        if (at < 0) {
            return "    <uses-permission android:name=\"" + name
                    + "\" android:usesPermissionFlags=\"" + flag + "\" />\n"
                    + xPermissions;
        }
        int start = xPermissions.lastIndexOf('<', at);
        int end = xPermissions.indexOf('>', at);
        if (start < 0 || end < 0) {
            return xPermissions;
        }
        String element = xPermissions.substring(start, end + 1);
        // Namespaced like the name beside it. A project may bind the Android
        // namespace to an alias in its own android.xpermissions block, and the
        // literal lookup then reported no existing flags and added a SECOND
        // attribute beside the one already there.
        // Merged into EVERY candidate spelling that exists, for the reason
        // declareUncapped strips every cap: with bindings collected
        // document-wide, a decoy under a rebound prefix could otherwise take
        // the flag while the attribute the merger actually reads goes without
        // it. Writing the flag into an attribute that is in nobody's namespace
        // is inert; failing to write it into the real one gives away the
        // permission this hint exists to restrict.
        String[] flagPrefixes = candidatePrefixes(element, xPermissions,
                ANDROID_NS, "android");
        String merged = element;
        boolean found = false;
        boolean already = false;
        for (int iter = 0; iter < flagPrefixes.length; iter++) {
            int[] slot = findAttribute(merged,
                    flagPrefixes[iter] + ":usesPermissionFlags");
            if (slot == null) {
                continue;
            }
            found = true;
            String value = merged.substring(slot[2], slot[3]);
            if (hasFlag(value, flag)) {
                already = true;
                continue;
            }
            merged = merged.substring(0, slot[2])
                    + (value.trim().length() == 0 ? flag : value + "|" + flag)
                    + merged.substring(slot[3]);
        }
        if (found) {
            if (already && merged.equals(element)) {
                return xPermissions;
            }
            return xPermissions.substring(0, start) + merged
                    + xPermissions.substring(end + 1);
        }
        // Nothing to merge into, so the element gains the attribute -- under
        // the prefix that NAMED this permission, not the conventional one.
        // An element may rebind "android" to something else and carry the real
        // Android namespace under an alias; inserting android:... there puts
        // the flag in the rebound namespace, where the merger never looks, and
        // the permission it was meant to restrict keeps ordinary precise
        // access while the build reports itself exclusive.
        String prefix = androidPrefixNaming(element, flagPrefixes, name);
        // Before the element's own close, whatever shape it has: the
        // declaration may end in "/>", in "  />" or in ">".
        int insert = element.length() - 1;
        while (insert > 0 && (element.charAt(insert - 1) == '/'
                || element.charAt(insert - 1) == ' '
                || element.charAt(insert - 1) == '\t')) {
            insert--;
        }
        String replacement = element.substring(0, insert)
                + " " + prefix + ":usesPermissionFlags=\"" + flag + "\""
                + element.substring(insert);
        return xPermissions.substring(0, start) + replacement
                + xPermissions.substring(end + 1);
    }

    /**
     * Makes the active declaration of {@code name} strip any cap a merged
     * manifest brings with it.
     *
     * <p>{@code declareUncapped} settles what is in THIS block. It cannot
     * settle what a lower-priority manifest contributes, because the merger
     * takes the union of an element's attributes: a library that declares the
     * same permission with its own {@code maxSdkVersion} has that attribute
     * merged into ours, and the permission the button needs stops being
     * granted above whatever the library chose.</p>
     *
     * <p>{@code tools:remove="android:maxSdkVersion"} is the merger's own
     * instruction for that, and it reaches caps this scan cannot see at all --
     * an aar pulled in through {@code android.gradleDependencies} is resolved
     * by Gradle and never passes through here.</p>
     *
     * <p>Merged into an existing {@code tools:remove} rather than written
     * beside it: that attribute is a comma-separated list, and two of them on
     * one element is not a thing a manifest may contain.</p>
     *
     * @param xPermissions the permission block
     * @param name         the permission whose cap must not survive the merge
     * @return the block with the instruction in place
     */
    private static String removeCapAcrossMerge(String xPermissions,
            String name) {
        int at = activePermissionIndex(xPermissions, name);
        if (at < 0) {
            return xPermissions;
        }
        int start = xPermissions.lastIndexOf('<', at);
        int end = xPermissions.indexOf('>', at);
        if (start < 0 || end < 0) {
            return xPermissions;
        }
        String element = xPermissions.substring(start, end + 1);
        String[] prefixes = candidatePrefixes(element, xPermissions, TOOLS_NS,
                "tools");
        // The VALUE is a QName, so it needs the Android prefix that is in
        // scope HERE. A literal "android:maxSdkVersion" on an element that
        // rebound that prefix names an attribute in somebody else's namespace,
        // and the real cap merges in untouched.
        String cap = androidPrefixNaming(element,
                candidatePrefixes(element, xPermissions, ANDROID_NS, "android"),
                name) + ":maxSdkVersion";
        for (int iter = 0; iter < prefixes.length; iter++) {
            int[] existing = findAttribute(element,
                    prefixes[iter] + ":remove");
            if (existing == null) {
                continue;
            }
            String value = element.substring(existing[2], existing[3]);
            if (hasListed(value, cap)) {
                return xPermissions;
            }
            String merged = element.substring(0, existing[2])
                    + (value.trim().length() == 0 ? cap : value + "," + cap)
                    + element.substring(existing[3]);
            return xPermissions.substring(0, start) + merged
                    + xPermissions.substring(end + 1);
        }
        // None to merge into, so the element gains one. If nothing here is
        // bound to the tools namespace -- an element may rebind the
        // conventional prefix and offer no alias, and candidatePrefixes drops
        // it for exactly that reason -- then a prefix has to be BOUND, not
        // assumed: writing tools:remove into a namespace somebody else took
        // leaves the marker inert and the library cap merges in anyway.
        //
        // The Android side needs no such fallback: an element only reaches
        // these editors because a candidate prefix NAMED the permission on it,
        // so a usable prefix is known to exist there.
        String prefix;
        String binding = "";
        if (prefixes.length > 0) {
            prefix = prefixes[0];
        } else {
            prefix = freePrefix(element, "cn1tools");
            binding = " xmlns:" + prefix + "=\"" + TOOLS_NS + "\"";
        }
        int insert = element.length() - 1;
        while (insert > 0 && (element.charAt(insert - 1) == '/'
                || element.charAt(insert - 1) == ' '
                || element.charAt(insert - 1) == '\t')) {
            insert--;
        }
        String replacement = element.substring(0, insert)
                + binding + " " + prefix + ":remove=\"" + cap + "\""
                + element.substring(insert);
        return xPermissions.substring(0, start) + replacement
                + xPermissions.substring(end + 1);
    }

    /**
     * The prefix this element used to NAME the permission.
     *
     * <p>Which is the one bound to the Android namespace here, whatever the
     * document calls it elsewhere: an element may rebind the conventional
     * prefix and carry the real namespace under an alias. Anything written
     * with the wrong prefix lands in the wrong namespace, where the merger
     * never looks -- and that is as true of a {@code tools:remove} VALUE, which
     * is a QName naming an attribute, as it is of an attribute itself.</p>
     *
     * @param element    the element
     * @param candidates the prefixes that may be the Android namespace here
     * @param name       the permission the element declares
     * @return the prefix that named it, or the likeliest candidate
     */
    private static String androidPrefixNaming(String element,
            String[] candidates, String name) {
        for (int iter = 0; iter < candidates.length; iter++) {
            int[] named = findAttribute(element, candidates[iter] + ":name");
            if (named != null && name.equals(element
                    .substring(named[2], named[3]).trim())) {
                return candidates[iter];
            }
        }
        return candidates.length > 0 ? candidates[0] : "android";
    }

    /**
     * A prefix this element has not already bound.
     *
     * @param element the element
     * @param wanted  the name to use if it is free
     * @return {@code wanted}, or it with a digit appended until it is free
     */
    private static String freePrefix(String element, String wanted) {
        String candidate = wanted;
        for (int suffix = 2; suffix < 100; suffix++) {
            if (findAttribute(element, "xmlns:" + candidate) == null) {
                return candidate;
            }
            candidate = wanted + suffix;
        }
        return candidate;
    }

    /**
     * Drops the {@code maxSdkVersion} entry from a tools list attribute.
     *
     * <p>The value is a comma-separated list of QNames, so this compares the
     * PARTS: any entry whose local name is {@code maxSdkVersion} and whose
     * prefix is one the element uses for the Android namespace. An entry
     * naming something else is somebody's instruction about a different
     * attribute and is left where it is, and an attribute left with no entries
     * is removed rather than emptied -- an empty list is as unmergeable as a
     * dangling one.</p>
     *
     * @param element      the element
     * @param document     the block, for resolving prefixes
     * @param androidNames the prefixes this element uses for Android
     * @param local        the tools attribute, {@code replace} or {@code remove}
     * @return the element, with the entry gone
     */
    private static String dropCapFromList(String element, String document,
            String[] androidNames, String local) {
        String[] toolsNames = candidatePrefixes(element, document, TOOLS_NS,
                "tools");
        for (int iter = 0; iter <= toolsNames.length; iter++) {
            String attribute = iter < toolsNames.length
                    ? toolsNames[iter] + ":" + local
                    : local;
            int[] value = findAttribute(element, attribute);
            if (value == null) {
                continue;
            }
            String[] parts = element.substring(value[2], value[3]).split(",");
            StringBuilder kept = new StringBuilder();
            boolean removed = false;
            for (int part = 0; part < parts.length; part++) {
                String entry = parts[part].trim();
                if (entry.length() == 0) {
                    continue;
                }
                if (namesTheCap(entry, androidNames)) {
                    removed = true;
                    continue;
                }
                if (kept.length() > 0) {
                    kept.append(',');
                }
                kept.append(entry);
            }
            if (!removed) {
                continue;
            }
            if (kept.length() > 0) {
                return element.substring(0, value[2]) + kept
                        + element.substring(value[3]);
            }
            // Nothing left, so the attribute goes with it. value[0]..value[1]
            // is the whole attribute including the space before it.
            return element.substring(0, value[0]) + element.substring(value[1]);
        }
        return element;
    }

    /** Whether a QName entry names the Android maxSdkVersion attribute. */
    private static boolean namesTheCap(String entry, String[] androidNames) {
        int colon = entry.indexOf(':');
        if (colon < 0 || !"maxSdkVersion".equals(entry.substring(colon + 1))) {
            return false;
        }
        String prefix = entry.substring(0, colon);
        for (int iter = 0; iter < androidNames.length; iter++) {
            if (androidNames[iter].equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a comma-separated list already names {@code wanted}. */
    private static boolean hasListed(String list, String wanted) {
        String[] parts = list.split(",");
        for (int iter = 0; iter < parts.length; iter++) {
            if (wanted.equals(parts[iter].trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Makes sure a permission is declared and that nothing caps how far it
     * reaches.
     *
     * <p>A permission another feature already declared is not added a second
     * time -- the manifest would then carry it twice -- so the only way to
     * lift a {@code maxSdkVersion} another injector wrote is to edit the
     * declaration that is there. Bluetooth scanning caps
     * {@code ACCESS_FINE_LOCATION} at API 30 and Wi-Fi management at 32; the
     * location button needs it uncapped on Android 17.</p>
     *
     * @param xPermissions the fragment so far
     * @param name         the permission
     * @return the fragment, with the declaration added or its cap removed
     */
    static String declareUncapped(String xPermissions, String name) {
        // The ACTIVE element's offset. A quoted-name search found the name
        // anywhere, a commented-out declaration included, and then widened text
        // inside a comment while the real manifest gained nothing.
        int at = activePermissionIndex(xPermissions, name);
        if (at < 0) {
            return addPermission(xPermissions, name);
        }
        int start = xPermissions.lastIndexOf('<', at);
        int end = xPermissions.indexOf('>', at);
        if (start < 0 || end < 0) {
            return xPermissions;
        }
        String element = xPermissions.substring(start, end + 1);
        // Namespaced, for the reason activePermissionIndex above already is:
        // it FOUND the aliased declaration, and a literal lookup here then
        // missed the cap on it and returned the block unchanged. No uncapped
        // duplicate is added either, so the button silently lost fine location
        // above API 30 -- which is the whole failure this method exists to
        // prevent, reintroduced one attribute to the left.
        // EVERY candidate spelling, not the first. Prefix bindings are
        // collected document-wide, so an element that rebinds the conventional
        // prefix could carry a decoy android:maxSdkVersion beside the real
        // a:maxSdkVersion -- and stripping the decoy while leaving the real cap
        // in place is the silent loss of fine location above API 30 that this
        // method exists to prevent. Removing a cap that turns out to be in
        // nobody's namespace costs nothing; leaving the real one costs the
        // feature, so every match goes.
        String[] prefixes = candidatePrefixes(element, xPermissions,
                ANDROID_NS, "android");
        String widened = element;
        boolean stripped = false;
        for (int iter = 0; iter < prefixes.length; iter++) {
            int[] cap = findAttribute(widened,
                    prefixes[iter] + ":maxSdkVersion");
            while (cap != null) {
                widened = widened.substring(0, cap[0])
                        + widened.substring(cap[1]);
                stripped = true;
                cap = findAttribute(widened, prefixes[iter] + ":maxSdkVersion");
            }
        }
        if (!stripped) {
            return xPermissions;
        }
        // And the tools:replace that named it, if there is one.
        //
        // A project fighting a library's cap writes
        // android:maxSdkVersion="32" tools:replace="android:maxSdkVersion" --
        // "override theirs with mine". Taking the value away and leaving the
        // instruction is a replacement with nothing to replace, which the
        // merger REFUSES, and removeCapAcrossMerge then adds a tools:remove for
        // the same attribute besides. Two contradictory instructions and a
        // build that stops: worse than the cap this method came to lift.
        widened = dropCapFromList(widened, xPermissions, prefixes, "replace");
        // The attribute left a double space behind it.
        widened = widened.replace("  ", " ");
        return xPermissions.substring(0, start) + widened
                + xPermissions.substring(end + 1);
    }

    /**
     * The Android attribute namespace. A manifest may bind it to ANY prefix.
     */
    private static final String ANDROID_NS =
            "http://schemas.android.com/apk/res/android";

    /** The manifest-merger namespace, bindable to any prefix in the same way. */
    private static final String TOOLS_NS = "http://schemas.android.com/tools";

    /**
     * Every prefix that might spell an attribute in {@code uri}.
     *
     * <p>The conventional one first, then each one the document binds. NOT
     * scoped to the element: working out which binding is in scope where means
     * tracking element nesting, which is writing an XML parser, and this class
     * hand-parses on purpose.</p>
     *
     * <p>The cost of not scoping is that a prefix rebound on an inner element
     * is still offered here. Every caller that a crafted manifest could
     * exploit therefore has to try them ALL rather than act on the first --
     * see declaresPermissionAt, which asks each spelling whether it names the
     * permission instead of asking one spelling what it names. A decoy under a
     * rebound prefix then hides nothing, because the real attribute is still
     * examined.</p>
     *
     * @param element  the element whose attributes are being read
     * @param document the file
     * @param uri      the namespace
     * @param usual    the conventional prefix
     * @return the prefixes to try, most likely first
     */
    private static String[] candidatePrefixes(String element, String document,
            String uri, String usual) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        out.add(usual);
        int at = document.indexOf(uri);
        while (at >= 0) {
            String prefix = prefixBoundAt(document, at, at + uri.length());
            if (prefix != null && !out.contains(prefix)) {
                out.add(prefix);
            }
            at = document.indexOf(uri, at + uri.length());
        }
        // Then drop the ones this ELEMENT has taken for something else.
        //
        // Collecting document-wide and always offering the conventional prefix
        // finds more spellings than a literal test, and for a permission this
        // scan wants to NOTICE that is the safe direction. It is the wrong
        // direction for the callers that ask "is this permission already
        // declared here", and I argued otherwise: an element that rebinds
        // android to a namespace of its own and carries the real one under an
        // alias was read as declaring whatever its android:name said, so
        // inject() added no real ACCESS_FINE_LOCATION and the button could not
        // be granted the permission it exists to obtain.
        //
        // A binding ON the element is the innermost one in scope for its own
        // attributes, so a prefix it points somewhere else is not ours here --
        // whatever the root said. That is not full scope resolution, which
        // would mean tracking nesting, and it does not need to be: these are
        // uses-permission elements directly under manifest, so the element and
        // the root are the only two scopes there are.
        String root = rootElement(document);
        for (int iter = out.size() - 1; iter >= 0; iter--) {
            String prefix = out.get(iter);
            // The INNERMOST binding wins: the element's own, and failing that
            // the root's. Checking only the element left the conventional
            // prefix unexamined whenever the rebinding was at the root -- which
            // is where a manifest declares its namespaces -- so a decoy
            // android:name on an element that binds nothing itself was still
            // read as an Android attribute.
            String owner = element;
            int[] bound = findAttribute(element, "xmlns:" + prefix);
            if (bound == null && root.length() > 0) {
                owner = root;
                bound = findAttribute(root, "xmlns:" + prefix);
            }
            if (bound != null
                    && !uri.equals(owner.substring(bound[2], bound[3])
                            .trim())) {
                out.remove(iter);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    /**
     * The document's root {@code <manifest>} element, or an empty string.
     *
     * <p>Where a manifest declares its namespaces, and the outer scope for
     * every {@code uses-permission} beneath it. Empty for an
     * {@code android.xpermissions} fragment, which has no root element -- and
     * that emptiness matters: searching the whole fragment for a binding would
     * let one element's {@code xmlns} decide the reading of another's, which
     * is not what scope means.</p>
     *
     * @param document the file or fragment
     * @return the root element's text, or {@code ""} when there is none
     */
    private static String rootElement(String document) {
        int at = document.indexOf("<manifest");
        // Past any that are commented out. An aar may open with an example in
        // a comment, and taking the first textual match let that example's
        // namespaces decide how the real root's attributes were read -- which
        // discards the real Android prefix and loses a live declaration.
        while (at >= 0 && isInsideComment(document, at)) {
            at = document.indexOf("<manifest", at + "<manifest".length());
        }
        if (at < 0) {
            return "";
        }
        int close = document.indexOf('>', at);
        return close < 0 ? "" : document.substring(at, close + 1);
    }

    /**
     * Reads backwards from a namespace URI to the prefix it is bound to.
     *
     * <p>Backwards because the prefix is what has to be discovered: there is no
     * name to search for. What must be there, reading right to left from the
     * URI, is a quote, an equals sign, and an attribute called
     * {@code xmlns:something} -- and anything else means this occurrence of the
     * URI is not a binding at all, which is what keeps the same text inside a
     * comment or another attribute's value from being read as one.</p>
     *
     * @param document the file
     * @param at       where the URI starts
     * @param past     the index just past it
     * @return the prefix, or null when this is not an xmlns binding
     */
    private static String prefixBoundAt(String document, int at,
            int past) {
        int cursor = at - 1;
        if (cursor < 0) {
            return null;
        }
        char quote = document.charAt(cursor);
        if (quote != '"' && quote != '\'') {
            return null;
        }
        // And the SAME quote immediately after the URI, which is what makes
        // this the whole value rather than the start of a longer one.
        // Without it xmlns:a="http://schemas.android.com/apk/res/android-fake"
        // bound "a" to the Android namespace: an element could then put a real
        // permission under a prefix the merger reads as somebody else's, and a
        // build that asks for nothing of the sort is refused.
        if (past >= document.length()
                || document.charAt(past) != quote) {
            return null;
        }
        cursor--;
        while (cursor >= 0 && isXmlSpace(document.charAt(cursor))) {
            cursor--;
        }
        if (cursor < 0 || document.charAt(cursor) != '=') {
            return null;
        }
        cursor--;
        while (cursor >= 0 && isXmlSpace(document.charAt(cursor))) {
            cursor--;
        }
        int end = cursor + 1;
        while (cursor >= 0 && isAttributeNameChar(document.charAt(cursor))) {
            cursor--;
        }
        String name = document.substring(cursor + 1, end);
        if (!name.startsWith("xmlns:") || name.length() == "xmlns:".length()) {
            return null;
        }
        return name.substring("xmlns:".length());
    }

    /** Whitespace as XML counts it. */
    private static boolean isXmlSpace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    /** A character that can appear inside an attribute name. */
    private static boolean isAttributeNameChar(char c) {
        return !isXmlSpace(c) && c != '<' && c != '>' && c != '/' && c != '='
                && c != '"' && c != '\'';
    }

    /**
     * Locates one attribute of an element, tolerating what XML allows.
     *
     * <p>{@code android:maxSdkVersion="30"}, {@code android:maxSdkVersion =
     * "30"} and {@code android:maxSdkVersion='30'} are the same attribute.
     * Recognising only the first spelling meant a permission an app had capped
     * in either of the other two read as uncapped and was left alone.</p>
     *
     * @param element the whole element text, angle brackets included
     * @param name    the attribute name
     * @return {attributeStart, attributeEnd, valueStart, valueEnd}, or null
     *         when the element does not carry it
     */
    private static int[] findAttribute(String element, String name) {
        int at = element.indexOf(name);
        while (at >= 0) {
            // A name that is the tail of a longer one is a different
            // attribute: android:maxSdkVersion must not be found inside
            // tools:android:maxSdkVersion.
            char before = at == 0 ? ' ' : element.charAt(at - 1);
            if (before == ' ' || before == '\t' || before == '\n'
                    || before == '\r' || before == '<') {
                int scan = skipSpace(element, at + name.length());
                if (scan < element.length() && element.charAt(scan) == '=') {
                    scan = skipSpace(element, scan + 1);
                    if (scan < element.length()) {
                        char quote = element.charAt(scan);
                        if (quote == '"' || quote == '\'') {
                            int close = element.indexOf(quote, scan + 1);
                            if (close > 0) {
                                return new int[] {at, close + 1, scan + 1,
                                        close};
                            }
                        }
                    }
                }
            }
            at = element.indexOf(name, at + 1);
        }
        return null;
    }

    private static int skipSpace(String s, int at) {
        while (at < s.length()) {
            char c = s.charAt(at);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return at;
            }
            at++;
        }
        return at;
    }

    /** True when the pipe-separated flag set already names this flag. */
    private static boolean hasFlag(String value, String flag) {
        String[] parts = value.split("\\|");
        for (int iter = 0; iter < parts.length; iter++) {
            if (parts[iter].trim().equals(flag)) {
                return true;
            }
        }
        return false;
    }


    // ------------------------------------------------------------------
    // Library bytecode
    // ------------------------------------------------------------------

    /**
     * What a tree of bytecode was found to use.
     *
     * <p>Both halves, from one walk. The button alone is not enough to decide
     * the manifest: {@code onlyForLocationButton} is safe only when NOTHING in
     * the application needs precise location to outlive the session, and a
     * cn1lib that shows the button may be the same cn1lib that geofences.
     * Reading only the button would have let {@code exclusive} through on
     * exactly that library and stopped its background behaviour working.</p>
     */
    public static final class LocationUsage {

        private boolean button;
        private boolean persistent;
        private boolean background;

        /// Which ELEMENT declared the background permission, per archive.
        ///
        /// uses-permission and uses-permission-sdk-23 are distinct element
        /// types to the merger, and a removal marker on one does not remove
        /// the other. Clearing this flag on a removal of the wrong type left
        /// the dependency's permission in the merged manifest and accepted an
        /// exclusive build beside it, so the type has to travel with the fact.
        private final java.util.Set<String> backgroundTags =
                new java.util.TreeSet<String>();

        /** The element types that asked for background location. */
        public java.util.Set<String> backgroundElements() {
            return backgroundTags;
        }

        /** The tree references {@code com.codename1.location.LocationButton}. */
        public boolean usesButton() {
            return button;
        }

        /**
         * The tree calls a location API whose grant has to outlive the session
         * -- {@code addGeoFencing} or {@code setBackgroundLocationListener}.
         */
        /**
         * Whether a submitted archive's own manifest asks for background
         * location.
         *
         * <p>Separate from {@link #usesPersistentLocation()}: that one is about
         * calls into Codename One's location API, and this is about a
         * permission an aar contributes without calling anything of ours -- a
         * native location SDK, typically. Both make
         * {@code android.locationButton.exclusive} wrong, for different
         * reasons.</p>
         *
         * @return whether a contributed manifest requests background location
         */
        public boolean declaresBackgroundLocation() {
            return background;
        }

        public boolean usesPersistentLocation() {
            return persistent;
        }

        /** True when nothing at all was found, which is the ordinary case. */
        public boolean isEmpty() {
            return !button && !persistent && !background;
        }

        /** True once neither marker can change the answer again. */
        private boolean settled() {
            return button && persistent;
        }
    }

    /**
     * The class a reference to it is stored under, in every constant pool that
     * names it.
     */
    private static final String BUTTON_MARKER =
            "com/codename1/location/LocationButton";

    /**
     * The persistent-location markers, which are METHOD names rather than a
     * class: {@code LocationManager} is referenced by any application that
     * touches location at all, so only the call distinguishes a tracking app
     * from a transactional one. The same reasoning
     * {@link NearbyManifestFragments} applies to {@code startObservingPresence}.
     */
    private static final String[] PERSISTENT_MARKERS = {
        "addGeoFencing",
        "setBackgroundLocationListener",
        // Foreground tracking counts too, and the name of this array
        // undersells it. onlyForLocationButton does not restrict fine location
        // to uses that OUTLIVE the session -- it restricts it to the BUTTON,
        // and downgrades every other precise-location request to approximate.
        // An app that navigates with setLocationListener and sets the hint
        // therefore ships a working button and a compass that has stopped
        // being precise, with nothing in the build to say so. The hint's own
        // documentation already promised the build rejects this.
        //
        // Note LocationButton itself does not appear here: it reaches a fix
        // through getCurrentLocationSync, and it is LocationManager -- filtered
        // as framework -- that calls setLocationListener underneath. An app
        // whose only precise-location use IS the button stays buildable, which
        // is the entire point of the hint.
        "setLocationListener",
        // And the one-shot lookups. Same reasoning again: an app that shows the
        // button in one flow and calls getCurrentLocationSync in another gets
        // an approximate answer to the second under the hint, silently. The
        // button's own call is LocationButton$5's, which the framework filter
        // strips to LocationButton and drops.
        "getCurrentLocation",
        "getCurrentLocationSync",
        // The cached lookup as well. It reads the platform provider outside any
        // button session, so the hint downgrades it like the rest.
        "getLastKnownLocation",
    };

    /** The class those methods have to be called ON to count. */
    private static final String LOCATION_MANAGER =
            "com/codename1/location/LocationManager";

    /**
     * The framework wrappers whose CONSTRUCTION is a non-button precise
     * location use, counted by being referenced at all.
     *
     * <p>Their calls into {@code LocationManager} are their own --
     * {@code GeofenceManager} makes the {@code addGeoFencing} and
     * {@code setBackgroundLocationListener} calls, {@code MapComponent} makes a
     * {@code getLastKnownLocation} call to centre itself -- and that is
     * framework code this scan skips by path. So a library using the documented
     * API of either names no marker method at all.</p>
     *
     * <p>Safe as bare class references: nothing in the framework names either
     * except their own inner classes, which the framework filter strips to the
     * outer name and drops.</p>
     *
     * <p>Kept identical to {@code AndroidGradleBuilder}'s list of the same
     * name, and a test asserts it. The two scans answer one question about two
     * trees, and a marker in only one of them is a hole in whichever tree the
     * application happens to use.</p>
     */
    static final String[] NON_BUTTON_LOCATION_CLASSES = {
        "com/codename1/location/GeofenceManager",
    };

    /**
     * The map, whose CENTRELESS constructors look up the last known location.
     *
     * <p>Not a bare class reference like the entry above, and the difference is
     * in {@code MapComponent} itself: {@code getLastKnownLocation()} is called
     * only when {@code centerPosition == null}, so the two constructors that
     * take no centre always look one up and the four that take one never do.
     * Treating every mention of the class as location use refused a map with a
     * caller-supplied centre, which is a perfectly valid thing for a
     * button-exclusive app to build.</p>
     *
     * <p>The residue is a caller who passes null explicitly to a
     * centre-taking constructor: it looks up a location and nothing static can
     * see that it will. That is a miss rather than a false refusal, and it is
     * the rarer of the two by a distance.</p>
     *
     * <p>Kept identical to {@code AndroidGradleBuilder}'s array of the same
     * name; a test asserts it, for the reason
     * {@link #NON_BUTTON_LOCATION_CLASSES} carries.</p>
     */
    static final String MAP_COMPONENT_CLASS = "com/codename1/maps/MapComponent";

    /**
     * Every MapComponent constructor counts, not only the centreless ones.
     *
     * <p>Narrowing to {@code ()V} and the provider-only form was tried, on the
     * reasoning that a map given a centre never reaches
     * {@code getLastKnownLocation()}. It does when the centre it is given is
     * {@code null} -- a supported value {@code MapComponent} handles
     * explicitly, by looking a location up -- and a descriptor cannot tell
     * {@code null} from a real {@code Coord}. Telling them apart needs the
     * value of an argument, which this scan does not have: the loose scanner
     * models {@code ACONST_NULL} as "not a boolean literal", indistinguishable
     * from unknown, and the constant pool has no arguments in it at all.</p>
     *
     * <p>So the choice is over- or under-approximating, and they are not
     * equally bad. Over: a map built with a real centre alongside
     * {@code android.locationButton.exclusive} is refused, with a message
     * naming the conflict, and dropping an opt-in hint fixes it in seconds.
     * Under: the map silently centres on an approximate position in a shipped
     * app. Constructing the class at all is the signal; merely naming the type
     * is still not.</p>
     */
    static final boolean MAP_COMPONENT_ANY_CONSTRUCTOR = true;

    /** Constant-pool tags, from JVMS 4.4. Stable, and only ever added to. */
    private static final int TAG_UTF8 = 1;
    private static final int TAG_INTEGER = 3;
    private static final int TAG_FLOAT = 4;
    private static final int TAG_LONG = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_CLASS = 7;
    private static final int TAG_STRING = 8;
    private static final int TAG_FIELDREF = 9;
    private static final int TAG_METHODREF = 10;
    private static final int TAG_INTERFACE_METHODREF = 11;
    private static final int TAG_NAME_AND_TYPE = 12;
    private static final int TAG_METHOD_HANDLE = 15;
    private static final int TAG_METHOD_TYPE = 16;
    private static final int TAG_DYNAMIC = 17;
    private static final int TAG_INVOKE_DYNAMIC = 18;
    private static final int TAG_MODULE = 19;
    private static final int TAG_PACKAGE = 20;

    /**
     * Framework classes whose own mention of these names says nothing about the
     * application: the component itself, the two classes that DECLARE the
     * persistent methods, the Play-services location manager that calls one,
     * and the Android bridge. A framework jar staged beside the libraries would
     * otherwise report every application as using everything.
     *
     * <p>Exact binary names rather than the package prefixes this used to
     * carry. {@code com/codename1/location/} and {@code com/codename1/impl/}
     * covered every framework class that matters, but they also covered a
     * cn1lib's own helper sitting in one of those namespaces -- and libraries
     * do put native-interface implementations under {@code
     * com.codename1.impl.*}. If such a helper was a library's only reference to
     * the button, the scan missed it, the toolchain guard never fired and the
     * bridge package was deleted out from under it.</p>
     *
     * <p>An exact list can go stale in the dangerous direction: a NEW framework
     * class that mentions a marker would make every application look like a
     * user of it. {@code LocationButtonMarkerCoverageTest} scans the built
     * framework for the markers and fails when a hit is not covered here, so
     * the list is a ratchet rather than a memory.</p>
     */
    private static final String[] FRAMEWORK_CLASSES = {
        "com/codename1/location/LocationButton",
        "com/codename1/location/LocationManager",
        "com/codename1/location/GeofenceManager",
        "com/codename1/location/AndroidLocationPlayServiceManager",
        // A LocationManager subclass, so it DECLARES the methods the markers
        // name. The pool walk asks for a call whose owner is LocationManager
        // and would not charge it for that, but the list is what says "this is
        // ours" and a framework location manager plainly is -- and the day one
        // of these does call super, nothing else would have caught it.
        "com/codename1/location/AndroidLocationManager",
        // MapComponent calls getLastKnownLocation. It is framework code and the
        // framework is staged beside every application, so leaving it off would
        // charge every app ever built for a call it did not make -- and this
        // flag refuses android.locationButton.exclusive. The cost is a real
        // limitation, stated plainly: precise location reached THROUGH a
        // framework wrapper rather than called directly is not detected by this
        // scan, here or for GeofenceManager.
        "com/codename1/maps/MapComponent",
        // The play-services shims, which declare getLastKnownLocation of their
        // own. The pool walk asks for a call whose owner is LocationManager and
        // would not charge these; the coverage test is deliberately blunter,
        // and a framework class is a framework class.
        "com/codename1/impl/android/PlayServices",
        "com/codename1/impl/android/PlayServices_12_0_0",

        "com/codename1/impl/android/locationbutton/AndroidLocationButtonBridge",
    };

    /**
     * What the bytecode under {@code root} uses of the location API.
     *
     * <p>Loose class files, jars and Android archives alike, because a library
     * can be the only thing that touches the API -- the application calls the
     * library and never names the button itself. Reading only the loose tree
     * would report no use at all, and the build would then delete
     * {@code com/codename1/impl/android/locationbutton} out from under the
     * library that needs it and leave {@code USE_LOCATION_BUTTON} out of the
     * manifest, so the application would fall back to the ordinary permission
     * prompt on exactly the Android version where that is a policy
     * violation.</p>
     *
     * <p>The test is a search of the whole class file for the name, which is
     * how every reference to it is stored. A class that mentions the string for
     * some other reason counts too, which errs towards keeping the
     * implementation and towards NOT claiming exclusivity -- both the safe
     * direction.</p>
     *
     * <p>Bounded by {@link Executor.PermScanBudget}, because {@code root} is a
     * directory of libraries the customer uploaded and the hosted daemon runs
     * this beside other people's builds. An unbounded read of a nested entry is
     * a compression bomb away from taking the JVM with it -- the budget's own
     * {@code readEntry} contract records that having happened to the database
     * scan. A refusal propagates rather than being swallowed: skipping an entry
     * is right when it is corrupt and wrong when the budget stopped us, because
     * every entry after it would be refused too and the archive would look read
     * while contributing nothing.</p>
     *
     * @param root a directory of staged classes and libraries, or null
     * @return what it uses, never null and empty when {@code root} is not a
     *         directory
     * @throws IOException when an archive exhausts the scan budget
     */
    public static LocationUsage scanForLocationUsage(java.io.File root)
            throws java.io.IOException {
        LocationUsage found = new LocationUsage();
        if (root != null && root.isDirectory()) {
            scanTree(root, root, found, new Executor.PermScanBudget());
        }
        return found;
    }

    private static void scanTree(java.io.File root, java.io.File dir,
            LocationUsage found, Executor.PermScanBudget budget)
            throws java.io.IOException {
        java.io.File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (int iter = 0; iter < children.length && !found.settled(); iter++) {
            java.io.File child = children[iter];
            String childPath = child.getPath();
            String name = child.getName().toLowerCase(java.util.Locale.ROOT);
            if (child.isDirectory()) {
                scanTree(root, child, found, budget);
            } else if ((name.endsWith(".jar") || name.endsWith(".aar")
                    || name.endsWith(".zip"))
                    && dir.getPath().equals(root.getPath())) {
                // TOP LEVEL only, because that is what the build consumes. The
                // dependency loop reads libsDir.listFiles() and the generated
                // gradle includes fileTree(dir: 'libs', include: ['*.jar']) --
                // neither descends, so an archive submitted at vendor/lib.aar
                // is staged, never added, and never runs. Reading a button
                // reference out of one refused a build over code that is not
                // in it, which is the same rule already applied to
                // assets/sample.jar inside an archive, applied to the tree
                // around them.
                //
                // Loose .class files stay recursive: those are read for the
                // application tree, where the package directories ARE the
                // layout.
                scanArchive(child, found, budget);
            } else if (name.endsWith(".class")
                    && !isFrameworkClass(relativePath(root, child))) {
                // Through the budget, like every archive entry. A loose file
                // cannot lie about its size the way a compressed entry can, but
                // it can still be enormous, and the budget's AGGREGATE cap is
                // what stops a tree of merely large ones adding up to the same
                // heap exhaustion on a shared build host.
                java.io.InputStream in = new java.io.FileInputStream(child);
                try {
                    budget.entry(childPath);
                    inspect(budget.readEntry(in, childPath, child.length()),
                            found);
                } finally {
                    in.close();
                }
            }
        }
    }

    /**
     * The class's binary name, taken from where the file sits UNDER THE ROOT.
     *
     * <p>Anchored, like an archive entry is. The previous form searched the
     * whole filesystem path for {@code com/codename1/} and truncated there,
     * which reads an application's own relocated
     * {@code org/acme/com/codename1/location/LocationButton.class} as the
     * framework's and skips it -- and that class can hold the application's
     * only reference to the real button, so the bridge is deleted under an app
     * that uses it. A build directory that merely happened to contain those
     * segments did the same to everything below it.</p>
     *
     * @param root the tree being scanned
     * @param file a file inside it
     * @return the path relative to the root, with forward slashes
     */
    private static String relativePath(java.io.File root, java.io.File file) {
        String rootPath = root.getPath().replace('\\', '/');
        String path = file.getPath().replace('\\', '/');
        if (path.length() > rootPath.length() && path.startsWith(rootPath)) {
            path = path.substring(rootPath.length());
        }
        int cut = 0;
        while (cut < path.length() && path.charAt(cut) == '/') {
            cut++;
        }
        return path.substring(cut);
    }

    private static void scanArchive(java.io.File archive, LocationUsage found,
            Executor.PermScanBudget budget) throws java.io.IOException {
        boolean isAar = archive.getName()
                .toLowerCase(java.util.Locale.ROOT).endsWith(".aar");
        java.util.zip.ZipFile zip;
        try {
            zip = new java.util.zip.ZipFile(archive);
        } catch (java.io.IOException notAnArchive) {
            // Not an archive, or a broken one. Nothing can be read out of it,
            // and guessing that it uses everything would charge the permission
            // and the library to every application that ships a stray file.
            return;
        }
        try {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries =
                    zip.entries();
            while (entries.hasMoreElements() && !found.settled()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                // Charged before any test of what the entry is, directories
                // included: a directory carries no payload, so neither byte
                // budget ever reaches it, and its header is the whole cost.
                budget.entry(entry.getName());
                if (entry.isDirectory()) {
                    continue;
                }
                // The entry's own name, never a lowercased copy. ZIP names
                // are case-sensitive and every path here is one the runtime
                // looks up by exact name: the Android layout recognises
                // classes.jar and libs/*.jar in that spelling, and a class
                // loader asks a jar for com/example/Sample.class and gets
                // nothing back for an entry called Sample.CLASS. Both are
                // resources nothing loads, and reading a button reference out
                // of one refused a build over code that never runs.
                //
                // An earlier version kept the suffix test lowercase, arguing
                // that a plain jar's bytecode is on the classpath however it is
                // spelled. It is not -- the lookup is exact -- and that is the
                // whole reason the spelling decides anything here.
                String name = entry.getName();
                // Only an aar's CLASSPATH jars. Gradle puts classes.jar and
                // libs/*.jar of an aar on the application's classpath and
                // nothing else, so a sample or tooling jar parked at
                // assets/sample.jar never runs -- and reading a LocationButton
                // reference out of one refused a build over code that is not
                // part of the application. A nested jar inside a plain jar is
                // not on any classpath either: Java has no nested-jar loader.
                //
                // Same rule as the manifest below, from the same source: what
                // the build actually consumes is a short, known list.
                boolean nested = isAar
                        && (name.equals("classes.jar")
                            || (name.startsWith("libs/")
                                && name.endsWith(".jar")
                                && name.indexOf('/', "libs/".length()) < 0));
                // An aar's ROOT AndroidManifest.xml is MERGED into the
                // application's, so a permission it asks for is a permission
                // the app ships -- and a native location SDK asking for
                // background location there calls nothing of ours, so no
                // bytecode scan will ever see it. Without this an exclusive
                // build was accepted and then merged ACCESS_BACKGROUND_LOCATION
                // beside onlyForLocationButton, which is the contradiction the
                // conflict check exists to refuse.
                //
                // The ROOT entry of an AAR specifically. Gradle merges that one
                // and nothing else, so a manifest sitting in an ordinary jar's
                // resources, or a template at assets/example/AndroidManifest
                // .xml inside an aar, contributes no permission to anything --
                // and reading one as a request would refuse an exclusive build
                // over a file the merger never opens.
                //
                // A LIMIT worth stating: an aar pulled in through
                // android.gradleDependencies is resolved by Gradle and never
                // appears here at all, so its manifest is outside what this can
                // see. Closing that would mean resolving the dependency graph,
                // which happens later and elsewhere. The hint remains the
                // developer's assertion that nothing but the button needs
                // precise location; this checks what is in front of it.
                boolean manifest = isAar && name.equals("AndroidManifest.xml");
                // A loose .class inside an AAR is not on the classpath
                // either -- an aar's bytecode lives in classes.jar, and a class
                // file sitting anywhere else in the archive is a resource
                // nothing loads. Same rule as the two above, applied to the
                // last entry kind this loop reads: scan what the build
                // consumes, not what the zip happens to contain. In a plain
                // jar, by contrast, the .class entries ARE the classpath.
                boolean classEntry = !isAar && name.endsWith(".class")
                        && !isFrameworkClass(name);
                if (!nested && !manifest && !classEntry) {
                    continue;
                }
                java.io.InputStream in = zip.getInputStream(entry);
                try {
                    if (nested) {
                        // An Android archive keeps its bytecode in a nested
                        // classes.jar, so the entries that matter are one level
                        // further in.
                        inspectNested(in, found, budget);
                    } else if (manifest) {
                        inspectManifest(budget.readEntry(in, entry.getName(),
                                entry.getSize()), found);
                    } else {
                        inspect(budget.readEntry(in, entry.getName(),
                                entry.getSize()), found);
                    }
                } catch (Executor.ScanBudgetExceeded refused) {
                    throw refused;
                } catch (java.io.IOException cannotReadEntry) {
                    // One unreadable entry says nothing about the entries
                    // after it.
                    continue;
                } finally {
                    in.close();
                }
            }
        } finally {
            try {
                zip.close();
            } catch (java.io.IOException ignored) {
                // Closing a file we have finished reading.
            }
        }
    }

    /**
     * Reads a contributed {@code AndroidManifest.xml} for a background-location
     * request.
     *
     * <p>Through the same {@link #declaresBackgroundLocation(String)} the
     * project's own permission block goes through, so a commented-out
     * declaration and a {@code tools:node="remove"} directive are understood
     * here too -- an aar that parks or removes the permission is not asking for
     * it.</p>
     *
     * @param manifest the manifest bytes, or null
     * @param found    the usage being accumulated
     */
    private static void inspectManifest(byte[] manifest, LocationUsage found) {
        if (manifest == null) {
            return;
        }
        // Decoded by its BOM, not assumed UTF-8. A UTF-16 manifest read as
        // UTF-8 comes back as replacement characters and NULs, and the
        // permission it asks for then matches nothing -- which fails in the
        // dangerous direction: the archive's background-location request goes
        // unseen and the exclusive build is accepted over it.
        String text = decodeXml(manifest);
        if (text == null) {
            return;
        }
        // A binary (aapt-compiled) manifest is not XML and this will simply not
        // match, which is the safe direction: the exclusivity check then rests
        // on the other signals rather than on a string found in a resource
        // table by accident.
        if (declaresBackgroundLocation(text)) {
            found.background = true;
            found.backgroundTags.addAll(backgroundElements(text));
        }
    }

    /**
     * Decodes XML bytes using the byte order mark, defaulting to UTF-8.
     *
     * <p>Covers the encodings a byte order mark can name, which is what an
     * archive's manifest realistically uses. An XML declaration naming some
     * other encoding without a mark is not honoured, and that is a miss in the
     * safe direction only insofar as it is rare -- it is written down here
     * rather than assumed away.</p>
     *
     * @param bytes the file
     * @return its text, or null when no charset here can read it
     */
    private static String decodeXml(byte[] bytes) {
        String charset = "UTF-8";
        int skip = 0;
        if (bytes.length >= 2) {
            int b0 = bytes[0] & 0xff;
            int b1 = bytes[1] & 0xff;
            if (b0 == 0xfe && b1 == 0xff) {
                charset = "UTF-16BE";
                skip = 2;
            } else if (b0 == 0xff && b1 == 0xfe) {
                charset = "UTF-16LE";
                skip = 2;
            } else if (b0 == 0 && b1 != 0) {
                // No mark, but a UTF-16BE document starts with a NUL before
                // the '<' of its first tag.
                charset = "UTF-16BE";
            } else if (b0 != 0 && b1 == 0) {
                charset = "UTF-16LE";
            } else if (bytes.length >= 3 && b0 == 0xef && b1 == 0xbb
                    && (bytes[2] & 0xff) == 0xbf) {
                skip = 3;
            }
        }
        try {
            return new String(bytes, skip, bytes.length - skip, charset);
        } catch (java.io.UnsupportedEncodingException unsupported) {
            try {
                return new String(bytes, "UTF-8");
            } catch (java.io.UnsupportedEncodingException impossible) {
                return null;
            }
        }
    }

    private static void inspectNested(java.io.InputStream nested,
            LocationUsage found, Executor.PermScanBudget budget)
            throws java.io.IOException {
        // Bounded at the source as well as per entry: the per-entry budgets
        // cannot see the bytes getNextEntry() spends parsing local headers, and
        // an enclosing archive compresses fifty thousand near-identical headers
        // to almost nothing -- so without this a small upload buys gigabytes of
        // header parsing that nothing charges for.
        java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(
                new Executor.BoundedInputStream(nested, budget));
        try {
            java.util.zip.ZipEntry entry = in.getNextEntry();
            while (entry != null && !found.settled()) {
                String name = entry.getName();
                budget.entry(name);
                // Case-sensitively, like the outer walk and for the same
                // reason: a class loader asks for Sample.class by that exact
                // name and never finds Sample.CLASS.
                if (!entry.isDirectory()
                        && name.endsWith(".class")
                        && !isFrameworkClass(name)) {
                    inspect(budget.readEntry(in, name, entry.getSize()), found);
                } else {
                    // Drained rather than skipped: stepping over an entry is
                    // not free on a ZipInputStream, because getNextEntry() has
                    // to inflate whatever is left of the current one first.
                    budget.drain(in, name, entry.getSize());
                }
                entry = in.getNextEntry();
            }
        } finally {
            in.close();
        }
    }

    /**
     * Whether this entry is one of the framework's own classes listed in
     * {@link #FRAMEWORK_CLASSES}.
     *
     * <p>Compared on the class's binary name, with any inner-class suffix
     * removed: {@code LocationButton$1} is as much the component as {@code
     * LocationButton} is, and the anonymous listeners inside it carry the same
     * constant-pool entries.</p>
     */
    /**
     * Whether {@code cls} is a framework class OR an inner class of one, by
     * NAME alone.
     *
     * <p>For the loose-class scan, which reports the class it is reading and
     * hands over no bytes: {@code LocationButton$1} calls back into
     * {@code LocationButton}, and in a tree where the framework is staged
     * beside the application that would charge every application ever built.
     * There is nothing else to go on there.</p>
     *
     * <p>It costs a known imprecision. A class genuinely named
     * {@code LocationManager$999} at top level is read as the framework's -- a
     * legal name, since '$' is an ordinary identifier character -- and would be
     * skipped. That is an application declaring a type inside
     * {@code com.codename1.location}, and the library scan, where such a class
     * would really come from, does not use this: it has the bytes and asks
     * {@link #outerClassName(String)} instead.</p>
     *
     * @param cls the class's internal name
     * @return whether it is the framework's, or nested inside it
     */
    static boolean isFrameworkOwner(String cls) {
        if (isFrameworkClass(cls)) {
            return true;
        }
        if (cls == null) {
            return false;
        }
        String normalized = cls.replace('\\', '/');
        int dollar = normalized.indexOf('$');
        return dollar > 0 && isFrameworkClass(normalized.substring(0, dollar));
    }

    /** Whether every character is a digit, and there is at least one. */
    private static boolean isAllDigits(String text) {
        if (text.length() == 0) {
            return false;
        }
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    static boolean isFrameworkClass(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0,
                    normalized.length() - ".class".length());
        }
        // ANCHORED, after stripping only the prefixes an archive is allowed to
        // add. Searching for "com/codename1/" anywhere accepted
        // org/acme/com/codename1/location/LocationManager -- a shaded or
        // relocated copy, which is a LIBRARY'S class and exactly the sort of
        // place a library's only button reference lives. Classifying it as ours
        // skipped it, bypassed the guard and deleted the bridge package.
        while (true) {
            if (normalized.startsWith("classes/")) {
                normalized = normalized.substring("classes/".length());
                continue;
            }
            // META-INF/versions/<n>/ -- a multi-release jar's per-version tree.
            if (normalized.startsWith("META-INF/versions/")) {
                int slash = normalized.indexOf('/',
                        "META-INF/versions/".length());
                if (slash > 0 && isAllDigits(normalized.substring(
                        "META-INF/versions/".length(), slash))) {
                    normalized = normalized.substring(slash + 1);
                    continue;
                }
            }
            break;
        }
        if (!normalized.startsWith("com/codename1/")) {
            return false;
        }
        // NO suffix stripping. By NAME this method answers only for an exact
        // match, which can never wrongly SKIP a library's class -- the
        // direction that deletes the bridge package from a working app.
        //
        // Every shape-based rule tried here was wrong. Cutting at the first '$'
        // swallowed a library's top-level LocationButton$Adapter; cutting a
        // NUMERIC suffix swallowed LocationManager$999, on the theory that an
        // identifier cannot begin with a digit -- which is true of identifiers
        // and irrelevant here, because '$' is an ordinary identifier character
        // and digits are legal identifier PARTS, so that name is a legal
        // top-level class too.
        //
        // The framework's own inner classes are recognised where the bytes are:
        // inspect() asks outerClassName() what the InnerClasses attribute says,
        // which is the only thing that actually knows.
        for (int iter = 0; iter < FRAMEWORK_CLASSES.length; iter++) {
            if (FRAMEWORK_CLASSES[iter].equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    /** The framework classes this scan ignores, for the coverage test. */
    static String[] frameworkClasses() {
        return FRAMEWORK_CLASSES.clone();
    }

    private static void inspect(byte[] classFile, LocationUsage found) {
        if (classFile == null) {
            return;
        }
        // ISO-8859-1, so every byte maps to one character and the search is
        // over the file as it actually is. A UTF-8 decode would drop bytes a
        // constant pool is free to contain.
        String text;
        try {
            text = new String(classFile, "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException impossible) {
            return;
        }
        // The framework's own INNER classes, which no name test can pick out.
        // LocationButton$1 is the button's grant callback and it references
        // LocationButton; counting that would charge every application for a
        // class the framework ships. The InnerClasses attribute is what
        // distinguishes it from a library's top-level LocationButton$Adapter,
        // and it is here rather than in isFrameworkClass because only here are
        // there bytes to ask.
        if (isNestedInsideFramework(text)) {
            return;
        }
        Pool pool = parsePool(text);
        if (pool == null) {
            // Not a class file this can walk -- truncated, or carrying a
            // constant-pool tag from a future release. Fall back to the name
            // search, which is what this did before the walk existed: a
            // strictly worse answer, never a missing one.
            if (namesExactly(text, BUTTON_MARKER)) {
                found.button = true;
            }
            for (int iter = 0; iter < PERSISTENT_MARKERS.length; iter++) {
                if (namesExactly(text, PERSISTENT_MARKERS[iter])) {
                    found.persistent = true;
                    break;
                }
            }
            return;
        }
        // Or as an annotation's class value, which is a different shape
        // entirely. referencesClass wants a CONSTANT_Class, and
        // @Widget(LocationButton.class) creates none: javac stores the value as
        // the field DESCRIPTOR "Lcom/codename1/location/LocationButton;" in a
        // Utf8, pointed at from the annotation's element_value. So the class
        // that is plainly named in the source is absent from the one table this
        // scan was reading, and the bridge was deleted under an app that really
        // does build the control.
        //
        // The BUTTON only. The markers below decide whether to REFUSE a build,
        // and a descriptor test would widen them on the strength of a type
        // appearing in somebody's signature. This one only ever keeps an
        // implementation package that would otherwise be removed.
        if (referencesClass(pool, BUTTON_MARKER)
                || referencesDescriptor(pool, BUTTON_MARKER)) {
            found.button = true;
        }
        // The geofencing wrapper counts as persistent use on its own. Its calls
        // into LocationManager are made by GeofenceManager itself -- framework
        // code this scan skips by path -- so a library that geofences through
        // the documented API names no marker method at all.
        for (int iter = 0; iter < NON_BUTTON_LOCATION_CLASSES.length; iter++) {
            if (referencesClass(pool, NON_BUTTON_LOCATION_CLASSES[iter])) {
                found.persistent = true;
                break;
            }
        }
        if (!found.persistent && constructs(pool, MAP_COMPONENT_CLASS)) {
            found.persistent = true;
        }
        if (callsMethodOn(pool, LOCATION_MANAGER, PERSISTENT_MARKERS)) {
            found.persistent = true;
        }
    }

    /**
     * A class file's constant pool, parsed far enough to answer "does this
     * class REFERENCE that one" and "does it CALL that method on it".
     *
     * <p>Parsed by hand rather than with ASM because the daemon's ASM reads
     * only up to Java 8 bytecode and a library it cannot parse still has to be
     * searchable. The constant pool needs no such knowledge: it sits at a fixed
     * offset, every entry declares its own tag, and the tags have only ever
     * been added to.</p>
     */
    private static final class Pool {
        /** Tag per index; 0 for the unusable slot after a long or double. */
        private int[] tag;
        /** Decoded text for CONSTANT_Utf8 entries, null elsewhere. */
        private String[] utf8;
        /** First u2 operand: a class's name, a ref's class, a nat's name. */
        private int[] first;
        /** Second u2 operand: a ref's name_and_type. */
        private int[] second;
        /** Offset just past the pool, where the class body begins. */
        private int end;
    }

    /** Reads a big-endian u2 out of the byte-per-char decoding. */
    private static int u2(String text, int at) {
        return ((text.charAt(at) & 0xff) << 8) | (text.charAt(at + 1) & 0xff);
    }

    /**
     * Parses the constant pool, or returns null when this is not something it
     * can walk.
     *
     * <p>Null is the honest answer for a truncated file, a magic number that is
     * not a class file, and an unknown tag -- once one entry's length is a
     * guess, every following offset is one too. The caller falls back rather
     * than treating null as "no location use".</p>
     *
     * @param text the class file, decoded ISO-8859-1 so one byte is one char
     * @return the parsed pool, or null when it cannot be trusted
     */
    private static Pool parsePool(String text) {
        if (text.length() < 10
                || (text.charAt(0) & 0xff) != 0xCA
                || (text.charAt(1) & 0xff) != 0xFE
                || (text.charAt(2) & 0xff) != 0xBA
                || (text.charAt(3) & 0xff) != 0xBE) {
            return null;
        }
        int count = u2(text, 8);
        if (count < 1) {
            return null;
        }
        Pool pool = new Pool();
        pool.tag = new int[count];
        pool.utf8 = new String[count];
        pool.first = new int[count];
        pool.second = new int[count];
        int at = 10;
        for (int index = 1; index < count; index++) {
            if (at >= text.length()) {
                return null;
            }
            int tag = text.charAt(at) & 0xff;
            at++;
            pool.tag[index] = tag;
            switch (tag) {
                case TAG_UTF8: {
                    if (at + 2 > text.length()) {
                        return null;
                    }
                    int length = u2(text, at);
                    at += 2;
                    if (at + length > text.length()) {
                        return null;
                    }
                    pool.utf8[index] = text.substring(at, at + length);
                    at += length;
                    break;
                }
                case TAG_CLASS:
                case TAG_STRING:
                case TAG_METHOD_TYPE:
                case TAG_MODULE:
                case TAG_PACKAGE:
                    if (at + 2 > text.length()) {
                        return null;
                    }
                    pool.first[index] = u2(text, at);
                    at += 2;
                    break;
                case TAG_METHOD_HANDLE:
                    if (at + 3 > text.length()) {
                        return null;
                    }
                    at += 3;
                    break;
                case TAG_INTEGER:
                case TAG_FLOAT:
                    if (at + 4 > text.length()) {
                        return null;
                    }
                    at += 4;
                    break;
                case TAG_FIELDREF:
                case TAG_METHODREF:
                case TAG_INTERFACE_METHODREF:
                case TAG_NAME_AND_TYPE:
                case TAG_DYNAMIC:
                case TAG_INVOKE_DYNAMIC:
                    if (at + 4 > text.length()) {
                        return null;
                    }
                    pool.first[index] = u2(text, at);
                    pool.second[index] = u2(text, at + 2);
                    at += 4;
                    break;
                case TAG_LONG:
                case TAG_DOUBLE:
                    if (at + 8 > text.length()) {
                        return null;
                    }
                    at += 8;
                    // "In retrospect, making 8-byte constants take two constant
                    // pool entries was a poor choice" -- JVMS 4.4.5. The next
                    // index is unusable and stays tagged 0.
                    index++;
                    break;
                default:
                    return null;
            }
        }
        pool.end = at;
        return pool;
    }

    /**
     * The binary name of {@code text}'s outer class, or null when it is not a
     * nested class.
     *
     * <p>From the {@code InnerClasses} attribute, which is the only thing that
     * actually knows. A name-shaped test cannot: {@code $} is an ordinary Java
     * identifier character and digits are legal identifier PARTS, so
     * {@code class LocationManager$999 {}} is a perfectly legal TOP-LEVEL class
     * and an earlier revision of this method mistook it for the framework's
     * {@code LocationManager}. A library that put its only button reference
     * there had it skipped, the toolchain guard bypassed and the bridge package
     * deleted.</p>
     *
     * <p>Returns null for anything it cannot read, which is the safe direction
     * here: an unreadable class is then treated as the library's own and
     * INSPECTED rather than skipped.</p>
     *
     * @param text the class file, decoded ISO-8859-1
     * @return the outer class's internal name, or null
     */
    /// What a class file's own metadata says about its nesting.
    ///
    /// Two facts, read in one pass because they come from the same attribute
    /// table and the caller needs both: the immediately enclosing class, and
    /// every name the InnerClasses table lists as a nested class.
    ///
    /// The second is what makes a transitive walk possible without opening any
    /// other file. A class that is not a member of a package MUST appear in the
    /// InnerClasses table of any class whose constant pool names it (JVMS
    /// 4.7.6), so an enclosing anonymous class is listed there -- verified
    /// against the real LocationButton$7$1, whose table carries an entry for
    /// LocationButton$7. A top-level class IS a member of a package and is
    /// therefore absent, however many dollars its name contains.
    private static final class Nesting {

        /// The immediately enclosing class, or null when this class is not
        /// nested at all.
        private String outer;

        /// Every name the InnerClasses table lists as an inner_class.
        private final java.util.Set<String> listed =
                new java.util.HashSet<String>();
    }

    private static Nesting readNesting(String text) {
        Pool pool = parsePool(text);
        if (pool == null) {
            return null;
        }
        Nesting found = new Nesting();
        int at = pool.end;
        // access_flags, this_class, super_class
        if (at + 6 > text.length()) {
            return null;
        }
        int thisClass = u2(text, at + 2);
        at += 6;
        at = skipTable(text, at, 2);            // interfaces
        at = skipMembers(text, at);             // fields
        at = skipMembers(text, at);             // methods
        if (at < 0 || at + 2 > text.length()) {
            return null;
        }
        int attrs = u2(text, at);
        at += 2;
        int enclosing = 0;
        for (int iter = 0; iter < attrs; iter++) {
            if (at + 6 > text.length()) {
                return null;
            }
            String name = utf8At(pool, u2(text, at));
            int length = (int) u4(text, at + 2);
            int body = at + 6;
            if (length < 0 || body + length > text.length()) {
                return null;
            }
            if ("InnerClasses".equals(name)) {
                int count = u2(text, body);
                for (int entry = 0; entry < count; entry++) {
                    int off = body + 2 + entry * 8;
                    if (off + 8 > text.length()) {
                        return null;
                    }
                    int inner = u2(text, off);
                    if (inner > 0 && inner < pool.tag.length
                            && pool.tag[inner] == TAG_CLASS) {
                        String listed = utf8At(pool, pool.first[inner]);
                        if (listed != null) {
                            found.listed.add(listed);
                        }
                    }
                    if (inner == thisClass) {
                        int outer = u2(text, off + 2);
                        // Zero for an ANONYMOUS class, which has no outer entry
                        // here at all -- what encloses it is in EnclosingMethod,
                        // and that is most of the framework's inner classes:
                        // LocationButton$1 through $6 are all anonymous.
                        if (outer > 0 && outer < pool.tag.length
                                && pool.tag[outer] == TAG_CLASS) {
                            found.outer = utf8At(pool, pool.first[outer]);
                        }
                    }
                }
            } else if ("EnclosingMethod".equals(name)) {
                enclosing = u2(text, body);
            }
            at = body + length;
        }
        if (found.outer == null && enclosing > 0
                && enclosing < pool.tag.length
                && pool.tag[enclosing] == TAG_CLASS) {
            found.outer = utf8At(pool, pool.first[enclosing]);
        }
        return found;
    }

    /// The immediately enclosing class, or null when there is none.
    static String outerClassName(String text) {
        Nesting nesting = readNesting(text);
        return nesting == null ? null : nesting.outer;
    }

    /**
     * Whether {@code text}'s own nesting metadata says it is nested
     * inside a framework class.
     *
     * <p>Package visible so LocationButtonMarkerCoverageTest can ask the same
     * question the scanner asks, rather than an easier one.</p>
     *
     * @param text the class file, decoded ISO-8859-1
     * @return whether it is the framework's own inner class
     */
    static boolean isNestedInsideFramework(String text) {
        Nesting nesting = readNesting(text);
        if (nesting == null || nesting.outer == null) {
            // Not nested, so whoever it is, it is not the framework's by
            // nesting. A top-level class whose NAME contains a dollar lands
            // here and is inspected, which is the point.
            return false;
        }
        // Walk outward, not one step. Nesting goes deeper than one level: the
        // Runnable inside the TimerTask inside scheduleStaleWake is
        // LocationButton$7$1, and it reports LocationButton$7 as its outer --
        // which is not on the list, because only the top-level type is.
        //
        // Each step up is licensed by METADATA rather than by the shape of the
        // name. The step is taken only when the InnerClasses table of the class
        // being inspected lists the outer as a nested class, which it is
        // required to do for any class in its pool that is not a member of a
        // package (JVMS 4.7.6). A library's top-level
        // com/codename1/location/LocationButton$Adapter is a package member, so
        // it is absent from that table, the walk stops on it, and its children
        // are inspected as the application code they are. Stripping the name to
        // the first dollar would have charged them to the framework and dropped
        // a real reference on the floor.
        String outer = nesting.outer;
        // Bounded because the input is a file, not a promise: a hand-made class
        // could list itself and spin here forever. Nothing javac emits comes
        // close to this depth.
        for (int step = 0; step < 32; step++) {
            if (isFrameworkClass(outer)) {
                return true;
            }
            if (!nesting.listed.contains(outer)) {
                return false;
            }
            int cut = outer.lastIndexOf('$');
            if (cut <= 0) {
                return false;
            }
            outer = outer.substring(0, cut);
        }
        return false;
    }

    /** Skips a u2 count followed by that many fixed-size entries. */
    private static int skipTable(String text, int at, int entrySize) {
        if (at < 0 || at + 2 > text.length()) {
            return -1;
        }
        int count = u2(text, at);
        int next = at + 2 + count * entrySize;
        return next > text.length() ? -1 : next;
    }

    /** Skips a fields or methods table, attributes and all. */
    private static int skipMembers(String text, int at) {
        if (at < 0 || at + 2 > text.length()) {
            return -1;
        }
        int count = u2(text, at);
        at += 2;
        for (int iter = 0; iter < count; iter++) {
            if (at + 8 > text.length()) {
                return -1;
            }
            int attrs = u2(text, at + 6);
            at += 8;
            for (int a = 0; a < attrs; a++) {
                if (at + 6 > text.length()) {
                    return -1;
                }
                long length = u4(text, at + 2);
                at += 6 + (int) length;
                if (at > text.length() || length < 0) {
                    return -1;
                }
            }
        }
        return at;
    }

    /** Reads a big-endian u4. */
    private static long u4(String text, int at) {
        return ((long) u2(text, at) << 16) | u2(text, at + 2);
    }

    /** The text of a CONSTANT_Utf8 entry, or null for any other index. */
    private static String utf8At(Pool pool, int index) {
        if (index < 1 || index >= pool.tag.length) {
            return null;
        }
        return pool.utf8[index];
    }

    /**
     * Whether the pool holds a constructor reference to {@code owner} with one
     * of {@code descriptors}.
     *
     * <p>Constructing it is the signal. Merely naming the type -- a field, a
     * parameter, a cast -- is not, which is what this buys over matching the
     * class reference itself.</p>
     *
     * @param owner the constructed class, internal name
     * @return whether it is constructed anywhere in this class
     */
    private static boolean constructs(Pool pool, String owner) {
        for (int index = 1; index < pool.tag.length; index++) {
            if (pool.tag[index] != TAG_METHODREF) {
                continue;
            }
            int classIndex = pool.first[index];
            if (classIndex < 1 || classIndex >= pool.tag.length
                    || pool.tag[classIndex] != TAG_CLASS
                    || !owner.equals(utf8At(pool, pool.first[classIndex]))) {
                continue;
            }
            int natIndex = pool.second[index];
            if (natIndex < 1 || natIndex >= pool.tag.length
                    || pool.tag[natIndex] != TAG_NAME_AND_TYPE
                    || !"<init>".equals(utf8At(pool, pool.first[natIndex]))) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * Whether the pool holds this class's FIELD DESCRIPTOR as a Utf8.
     *
     * <p>The shape an annotation class value takes: {@code @Widget(Foo.class)}
     * puts {@code "Lcom/example/Foo;"} in a Utf8 and points the annotation's
     * element_value at it, creating no {@code CONSTANT_Class} at all. A field
     * declared of that type is the same shape.</p>
     *
     * <p>Exact equality, not a substring: a method descriptor such as
     * {@code (Lcom/example/Foo;)V} is one Utf8 of its own, and matching inside
     * it would report a class as used by every signature that merely mentions
     * it. The annotation case needs no such reach -- its Utf8 is the descriptor
     * and nothing else.</p>
     *
     * @param pool         the parsed constant pool
     * @param internalName the class, in internal form
     * @return whether some Utf8 is exactly that class's descriptor
     */
    private static boolean referencesDescriptor(Pool pool,
            String internalName) {
        String descriptor = "L" + internalName + ";";
        for (int index = 1; index < pool.tag.length; index++) {
            if (pool.tag[index] != TAG_UTF8) {
                continue;
            }
            String value = utf8At(pool, index);
            if (value == null) {
                continue;
            }
            // The descriptor itself, or an array of it: an annotation value of
            // Foo[].class is "[Lcom/example/Foo;", and an array of the button
            // is a reference to the button.
            int at = 0;
            while (at < value.length() && value.charAt(at) == '[') {
                at++;
            }
            if (descriptor.equals(value.substring(at))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether some CONSTANT_Class in the pool names {@code internalName}.
     *
     * <p>A CONSTANT_Class rather than the name appearing anywhere: a library
     * that merely holds {@code "com/codename1/location/LocationButton"} as a
     * STRING constant is not a user of the button, and the framing check alone
     * could not tell the two apart -- a string literal's Utf8 entry is byte for
     * byte the same as a class name's. Reading it as use of the button refused
     * that library's whole Android build at the toolchain gate.</p>
     */
    private static boolean referencesClass(Pool pool, String internalName) {
        // EXACT. An array class literal -- Foo[].class, whose CONSTANT_Class
        // is named "[Lcom/example/Foo;" -- is caught by referencesDescriptor
        // instead: that same name IS a Utf8 entry, and stripping the array
        // brackets there leaves the descriptor it compares. Widening this test
        // as well changed no outcome that a test could tell apart, so it is
        // not here.
        for (int index = 1; index < pool.tag.length; index++) {
            if (pool.tag[index] == TAG_CLASS
                    && internalName.equals(utf8At(pool, pool.first[index]))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the pool holds a method reference to one of {@code methods} on
     * {@code owner}.
     *
     * <p>A reference, not a name. A library that DECLARES a method of its own
     * called {@code addGeoFencing} carries that name in its constant pool like
     * any other, and reading it as persistent-location use refused
     * {@code android.locationButton.exclusive} for a project that never
     * geofenced anything.</p>
     *
     * <p>The owner is required to be exactly {@code LocationManager}. An
     * application reaches these methods through
     * {@code LocationManager.getLocationManager()}, whose return type IS that
     * class, so the compile-time owner is always this one; a call made through
     * a reference typed as a port's own subclass would be missed, and that is a
     * class in {@code com.codename1.impl} an application has no business
     * naming -- the obfuscator renames it out from under such code anyway.</p>
     */
    private static boolean callsMethodOn(Pool pool, String owner,
            String[] methods) {
        for (int index = 1; index < pool.tag.length; index++) {
            if (pool.tag[index] != TAG_METHODREF
                    && pool.tag[index] != TAG_INTERFACE_METHODREF) {
                continue;
            }
            int classIndex = pool.first[index];
            if (classIndex < 1 || classIndex >= pool.tag.length
                    || pool.tag[classIndex] != TAG_CLASS
                    || !owner.equals(utf8At(pool, pool.first[classIndex]))) {
                continue;
            }
            int natIndex = pool.second[index];
            if (natIndex < 1 || natIndex >= pool.tag.length
                    || pool.tag[natIndex] != TAG_NAME_AND_TYPE) {
                continue;
            }
            String name = utf8At(pool, pool.first[natIndex]);
            for (int iter = 0; iter < methods.length; iter++) {
                if (methods[iter].equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the class file holds {@code marker} as a WHOLE constant-pool
     * entry rather than merely somewhere inside one.
     *
     * <p>A plain substring search also matched a longer symbol that starts the
     * same way -- {@code com/codename1/location/LocationButtonHelper} in a
     * submitted library, or a method named {@code addGeoFencingLater} -- and
     * reported it as use of the real one. That was tolerable when a false
     * positive cost an unused permission and a small library. It is not
     * tolerable now the flag gates a check that REFUSES the Android build,
     * because a library merely naming something similar would stop the whole
     * thing.</p>
     *
     * <p>Every name in a class file is stored as a CONSTANT_Utf8 entry: two
     * big-endian length bytes, then the bytes themselves. An entry that IS the
     * marker is therefore a match whose preceding two bytes read as exactly the
     * marker's length, and {@code LocationButtonHelper} -- six characters
     * longer -- cannot produce one. Those two bytes are the whole check: no
     * constant-pool walk, and no ASM that has to be able to parse the class,
     * which matters because the daemon's ASM reads only up to Java 8.</p>
     *
     * <p>This narrows in the SAFE direction and deliberately so. It no longer
     * matches a class whose only mention of the button is a field or method
     * DESCRIPTOR -- {@code Lcom/codename1/location/LocationButton;} inside a
     * longer entry -- but a class that merely names the type in a signature and
     * never touches it is dead weight; anything that constructs one or calls a
     * method on one carries the bare name in a CONSTANT_Class. A miss costs one
     * library its permission, a false hit costs every build using that library
     * its whole Android build.</p>
     *
     * @param text   the class file, decoded ISO-8859-1 so one byte is one char
     * @param marker the name to look for
     * @return whether some constant-pool entry is exactly that name
     */
    private static boolean namesExactly(String text, String marker) {
        int at = text.indexOf(marker);
        while (at >= 0) {
            if (at >= 2) {
                int declared = ((text.charAt(at - 2) & 0xff) << 8)
                        | (text.charAt(at - 1) & 0xff);
                if (declared == marker.length()) {
                    return true;
                }
            }
            at = text.indexOf(marker, at + 1);
        }
        return false;
    }

}
