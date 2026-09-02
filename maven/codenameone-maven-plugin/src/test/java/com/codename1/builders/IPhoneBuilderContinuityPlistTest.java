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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code NSUserActivityTypes} has two contributors, and there can only be one key.
 *
 * <p>App intents and continuity both need this key, an app that uses both is ordinary, and a
 * property list carrying it twice is one iOS reads unpredictably. The interesting cases are
 * therefore all about the two meeting: each alone, both together, and both on top of an array the
 * application already declared through {@code ios.plistInject}.</p>
 *
 * <p>The failure this prevents is silent on both sides. iOS only continues an activity whose type
 * the app declared here, so a missing entry is a feature that does nothing at all -- on two
 * devices, with nothing logged anywhere.</p>
 */
class IPhoneBuilderContinuityPlistTest {

    private static final String CONTINUITY_TYPE = "com.example.app.continuity";

    private static Map<String, Object> intent(String id) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", id);
        m.put("assistant", Boolean.TRUE);
        return m;
    }

    private static List<Map<String, Object>> intents(String... ids) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (String id : ids) {
            out.add(intent(id));
        }
        return out;
    }

    private static List<Map<String, Object>> noIntents() {
        return new ArrayList<Map<String, Object>>();
    }

    private static int occurrences(String haystack, String needle) {
        int count = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            count++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Emitting the key
    // ------------------------------------------------------------------

    @Test
    void continuityAloneDeclaresItsActivityType() {
        String key = IPhoneBuilder.userActivityTypesKey(noIntents(), CONTINUITY_TYPE);

        assertTrue(key.contains("<key>NSUserActivityTypes</key>"), key);
        assertTrue(key.contains("<string>" + CONTINUITY_TYPE + "</string>"), key);
        assertEquals(1, occurrences(key, "NSUserActivityTypes"), key);
    }

    @Test
    void intentsAndContinuityShareOneKey() {
        String key = IPhoneBuilder.userActivityTypesKey(intents("logWorkout"), CONTINUITY_TYPE);

        assertEquals(1, occurrences(key, "NSUserActivityTypes"), key);
        assertEquals(1, occurrences(key, "<array>"), key);
        assertTrue(key.contains("<string>logWorkout</string>"), key);
        assertTrue(key.contains("<string>" + CONTINUITY_TYPE + "</string>"), key);
    }

    @Test
    void intentsAloneAreUnchangedByTheContinuityParameter() {
        assertEquals(IPhoneBuilder.userActivityTypesKey(intents("logWorkout")),
                IPhoneBuilder.userActivityTypesKey(intents("logWorkout"), null));
    }

    /**
     * An app with nothing to declare writes nothing. An empty array would state that the app
     * continues no activity at all, into the plist of an app that may well continue its own.
     */
    @Test
    void nothingToDeclareWritesNoKey() {
        assertEquals("", IPhoneBuilder.userActivityTypesKey(noIntents(), null));
        assertEquals("", IPhoneBuilder.userActivityTypesKey(noIntents(), ""));
    }

    // ------------------------------------------------------------------
    // Merging into an array the application supplied
    // ------------------------------------------------------------------

    @Test
    void continuityMergesIntoAnArrayTheApplicationDeclared() {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.legacyHandoff</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);

        assertEquals(1, occurrences(merged, "NSUserActivityTypes"), merged);
        assertTrue(merged.contains("<string>com.example.app.legacyHandoff</string>"), merged);
        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    @Test
    void intentsAndContinuityBothMergeIntoOneSuppliedArray() {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.legacyHandoff</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, intents("logWorkout"),
                CONTINUITY_TYPE);

        assertEquals(1, occurrences(merged, "NSUserActivityTypes"), merged);
        assertEquals(1, occurrences(merged, "<array>"), merged);
        assertTrue(merged.contains("<string>com.example.app.legacyHandoff</string>"), merged);
        assertTrue(merged.contains("<string>logWorkout</string>"), merged);
        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    /**
     * A project that already named the continuity type itself gets it once, not twice.
     */
    @Test
    void anAlreadyDeclaredContinuityTypeIsNotDuplicated() {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<string>" + CONTINUITY_TYPE + "</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);

        assertEquals(1, occurrences(merged, "<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    @Test
    void aFragmentWhoseArrayCannotBeFoundIsReturnedUnchanged() {
        String inject = "<key>NSUserActivityTypes</key><string>not an array</string>";

        assertEquals(inject,
                IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE));
    }

    /**
     * The parser has to accept the shapes a hand-written fragment really carries.
     */
    @Test
    void aSpacedClosingTagIsStillMergedInto() {
        String inject = "<key>NSUserActivityTypes</key><array >"
                + "<string>com.example.app.legacyHandoff</string></array >";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
        assertEquals(1, occurrences(merged, "NSUserActivityTypes"), merged);
    }

    // ------------------------------------------------------------------
    // The shapes a hand-written ios.plistInject really carries
    // ------------------------------------------------------------------

    /**
     * {@code <array/>} is the ordinary XML spelling of an empty array and a plist parser reads it
     * as {@code <array></array>}. The merge looks for the literal pair, so without expansion an
     * app that declared the key that way took the merge branch and had every id dropped -- worse
     * than a duplicate key, because nothing says so until Handoff does not work on a device.
     */
    @Test
    void aSelfClosingArrayIsExpandedSoTheMergeCanSeeIt() {
        String inject = "<key>NSUserActivityTypes</key><array/>";

        String expanded = IPhoneBuilder.expandEmptyUserActivityArray(inject);
        String merged = IPhoneBuilder.mergeUserActivityTypes(expanded, noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
        assertEquals(1, occurrences(merged, "NSUserActivityTypes"), merged);
    }

    @Test
    void aSelfClosingArrayWithWhitespaceAndASpacedTagIsStillExpanded() {
        String inject = "<key>NSUserActivityTypes</key>\n   <array />";

        String merged = IPhoneBuilder.mergeUserActivityTypes(
                IPhoneBuilder.expandEmptyUserActivityArray(inject), intents("logWorkout"),
                CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>logWorkout</string>"), merged);
        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    /** An array that is already a pair is left exactly as it was. */
    @Test
    void anOpenClosePairIsNotRewritten() {
        String inject = "<key>NSUserActivityTypes</key><array><string>a</string></array>";

        assertEquals(inject, IPhoneBuilder.expandEmptyUserActivityArray(inject));
    }

    /** Another key's empty array is none of this method's business. */
    @Test
    void anUnrelatedEmptyArrayIsNotRewritten() {
        String inject = "<key>NSUserActivityTypes</key><array><string>a</string></array>"
                + "<key>SomethingElse</key><array/>";

        String out = IPhoneBuilder.expandEmptyUserActivityArray(inject);

        assertTrue(out.contains("<key>SomethingElse</key><array/>"), out);
    }

    @Test
    void aFragmentWithoutTheKeyIsLeftAlone() {
        String inject = "<key>SomethingElse</key><array/>";

        assertEquals(inject, IPhoneBuilder.expandEmptyUserActivityArray(inject));
    }

    /**
     * The decision has to be made on LIVE elements. A commented-out declaration answered a plain
     * contains() yes, so the builder stood aside, merged into the comment, and shipped an app
     * with no live activity type at all.
     */
    @Test
    void aCommentedOutDeclarationDoesNotCountAsSupplied() {
        String inject = "<!-- <key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.old</string></array> -->";

        assertTrue(IPhoneBuilder.plistKeyIndex(
                IPhoneBuilder.plistWithoutComments(inject), "NSUserActivityTypes") < 0,
                "a commented-out key must read as absent, which is what makes the builder "
                        + "emit a live one of its own");
    }

    /** A live declaration beside a commented-out one still reads as supplied. */
    @Test
    void aLiveDeclarationBesideACommentedOneCountsAsSupplied() {
        String inject = "<!-- <key>NSUserActivityTypes</key><array/> -->"
                + "<key>NSUserActivityTypes</key><array><string>a</string></array>";

        assertTrue(IPhoneBuilder.plistKeyIndex(
                IPhoneBuilder.plistWithoutComments(inject), "NSUserActivityTypes") >= 0);
    }

    /**
     * The case that survived the previous round: an old declaration kept commented out ABOVE the
     * live one. The branch that calls the merge correctly saw a live key; the merge then found
     * the dead one first and inserted into the comment, so the array iOS actually reads shipped
     * without the continuity type and Handoff was never advertised.
     */
    @Test
    void aCommentedDeclarationAboveALiveOneIsNotTheOneMergedInto() {
        String inject = "<!-- <key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.old</string></array> -->"
                + "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.live</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);

        int comment = merged.indexOf("-->");
        int added = merged.indexOf("<string>" + CONTINUITY_TYPE + "</string>");
        assertTrue(added > comment,
                "the continuity type landed inside the commented-out array: " + merged);
        assertTrue(merged.contains("<string>com.example.app.live</string>"), merged);
    }

    /** The expander targets the live key too, for the same reason. */
    @Test
    void aCommentedSelfClosingArrayIsNotTheOneExpanded() {
        String inject = "<!-- <key>NSUserActivityTypes</key><array/> -->"
                + "<key>NSUserActivityTypes</key><array/>";

        String expanded = IPhoneBuilder.expandEmptyUserActivityArray(inject);

        assertTrue(expanded.contains("<!-- <key>NSUserActivityTypes</key><array/> -->"),
                "the commented array was rewritten: " + expanded);
        assertTrue(expanded.endsWith("<array></array>"), expanded);
    }

    @Test
    void insideCommentRecognizesBothSides() {
        String s = "aa<!--bb-->cc";
        assertFalse(IPhoneBuilder.insideComment(s, 0));
        assertTrue(IPhoneBuilder.insideComment(s, 6));
        assertFalse(IPhoneBuilder.insideComment(s, 12));
    }

    /** An unterminated comment swallows the rest, which is what a parser does with it too. */
    @Test
    void anUnterminatedCommentSwallowsWhatFollows() {
        String s = "aa<!--bb";
        assertTrue(IPhoneBuilder.insideComment(s, 7));
    }

    /**
     * A comment between the key and its array is a fragment a person writes, and a plist parser
     * reads the array as the key's value regardless. Skipping only whitespace left the array
     * self-closing, and the merge then found no closing tag and added nothing.
     */
    @Test
    void aCommentBetweenTheKeyAndItsArrayIsSteppedOver() {
        String inject = "<key>NSUserActivityTypes</key><!-- why we declare this --><array/>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(
                IPhoneBuilder.expandEmptyUserActivityArray(inject), noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
        assertEquals(1, occurrences(merged, "NSUserActivityTypes"), merged);
    }

    /**
     * The documented behaviour when this key's value is not an array is to return the fragment
     * untouched. An unbounded search instead reached past it and inserted the ids into a LATER
     * key's array, corrupting a property this code was never asked about.
     */
    @Test
    void aNonArrayValueDoesNotBorrowALaterKeysArray() {
        String inject = "<key>NSUserActivityTypes</key><string>not an array</string>"
                + "<key>SomethingElse</key><array><string>keep</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, intents("logWorkout"),
                CONTINUITY_TYPE);

        assertEquals(inject, merged, "an unrelated array was edited");
    }

    @Test
    void aNonArrayValueIsNotExpandedEither() {
        String inject = "<key>NSUserActivityTypes</key><dict/>"
                + "<key>SomethingElse</key><array/>";

        assertEquals(inject, IPhoneBuilder.expandEmptyUserActivityArray(inject));
    }

    @Test
    void immediateValueIndexStepsOverWhitespaceAndComments() {
        String plist = "<key>K</key>  <!-- a --> <!-- b --> <array/>";
        int at = IPhoneBuilder.immediateValueIndex(plist, 0);

        assertTrue(at > 0, "no value found");
        assertTrue(plist.startsWith("<array", at), plist.substring(at));
    }

    /** An unterminated comment has no value after it, which is what a parser would conclude. */
    @Test
    void anUnterminatedCommentYieldsNoImmediateValue() {
        assertEquals(-1, IPhoneBuilder.immediateValueIndex("<key>K</key><!-- oops", 0));
    }

    /**
     * A key may carry a comment, and a comment may contain the text "</key>". A raw search ended
     * the key inside the comment, concluded the value was not an array, and dropped every
     * activity type -- while the branch that decided to merge had resolved the key correctly.
     */
    @Test
    void aCommentInsideTheKeyDoesNotEndItEarly() {
        String inject = "<key><!-- </key> -->NSUserActivityTypes</key><array/>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(
                IPhoneBuilder.expandEmptyUserActivityArray(inject), noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    @Test
    void nothingToAddLeavesTheFragmentAlone() {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.legacyHandoff</string></array>";

        assertEquals(inject, IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), null));
        assertFalse(IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), null)
                .contains("null"));
    }
}
