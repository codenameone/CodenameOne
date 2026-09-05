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
import static org.junit.jupiter.api.Assertions.fail;

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
    // The type the native side reads
    // ------------------------------------------------------------------

    @Test
    void theResolvedActivityTypeIsDeclaredForTheNativeSide() throws BuildException {
        String out = IPhoneBuilder.withContinuityActivityType("", CONTINUITY_TYPE);

        assertTrue(out.contains("<key>CN1ContinuityActivityType</key>"), out);
        assertTrue(out.contains("<string>" + CONTINUITY_TYPE + "</string>"), out);
        assertEquals(1, occurrences(out, "<key>CN1ContinuityActivityType</key>"),
                "the key must appear exactly once: " + out);
    }

    @Test
    void anAppThatDoesNotUseContinuityGetsNoSuchKey() throws BuildException {
        String out = IPhoneBuilder.withContinuityActivityType("<key>Other</key><string>x</string>",
                null);

        assertFalse(out.contains("CN1ContinuityActivityType"), out);
    }

    /**
     * The delegate compares the arriving activity type against this key. Deriving it natively from
     * the bundle identifier instead looks equivalent and is wrong on the Mac slice, where
     * DERIVE_MACCATALYST_PRODUCT_BUNDLE_IDENTIFIER makes the id "&lt;package&gt;.maccatalyst" --
     * so the derived type would be "&lt;package&gt;.maccatalyst.continuity" while every device
     * publishes "&lt;package&gt;.continuity", and Handoff would be silently dead on Catalyst.
     * The value written here is the package's, not the bundle's.
     */
    @Test
    void theDeclaredTypeIsThePackagesAndCarriesNoCatalystSuffix() throws BuildException {
        String out = IPhoneBuilder.withContinuityActivityType("", CONTINUITY_TYPE);

        assertTrue(out.contains("<string>com.example.app.continuity</string>"), out);
        assertFalse(out.contains("maccatalyst"), out);
    }

    @Test
    void anApplicationsOwnMatchingDeclarationIsLeftAlone() throws BuildException {
        String existing = "<key>CN1ContinuityActivityType</key><string>" + CONTINUITY_TYPE
                + "</string>";

        String out = IPhoneBuilder.withContinuityActivityType(existing, CONTINUITY_TYPE);

        assertEquals(existing, out, "a matching declaration must not be duplicated");
        assertEquals(1, occurrences(out, "<key>CN1ContinuityActivityType</key>"), out);
    }

    /**
     * A stale injected value is refused rather than left standing. It is not a build hint: the
     * type is what NSUserActivityTypes declares and what every device publishes, so a fragment
     * naming a different one has the delegate turning away the application's own continuations
     * while iOS keeps offering them -- nothing fails and nothing is logged.
     */
    @Test
    void aDisagreeingInjectedTypeIsRefused() {
        String existing = "<key>CN1ContinuityActivityType</key><string>com.other.app.continuity"
                + "</string>";

        try {
            IPhoneBuilder.withContinuityActivityType(existing, CONTINUITY_TYPE);
            fail("a fragment naming a different activity type must not be accepted");
        } catch (BuildException expected) {
            assertTrue(expected.getMessage().contains("CN1ContinuityActivityType"),
                    expected.getMessage());
            assertTrue(expected.getMessage().contains(CONTINUITY_TYPE), expected.getMessage());
        }
    }

    /**
     * A declaration of the wrong plist TYPE is refused rather than accepted. topLevelPlistString
     * answers null for an array or a dict, which used to mean the value was left alone (something
     * was declared) and ours was not added (the key was present) -- so the build succeeded and
     * the delegate found a value that is not an NSString, treated it as absent, and let every
     * continuation bypass the handler on a build that looked configured.
     */
    @Test
    void aNonStringDeclarationIsRefused() {
        String arrayValued = "<key>CN1ContinuityActivityType</key><array>"
                + "<string>com.example.app.continuity</string></array>";

        try {
            IPhoneBuilder.withContinuityActivityType(arrayValued, CONTINUITY_TYPE);
            fail("a non-string CN1ContinuityActivityType must not be accepted");
        } catch (BuildException expected) {
            assertTrue(expected.getMessage().contains("non-string"), expected.getMessage());
        }
    }

    /**
     * A declaration spelled with character references is the SAME declaration.
     *
     * <p>Foundation resolves {@code <string>com&#46;example.app.continuity</string>} to
     * {@code com.example.app.continuity}, so a project that spells its injected type that way has
     * declared exactly what this build publishes. topLevelPlistString sliced the raw XML between
     * the tags and handed back the undecoded text, so the equality check called it a CONFLICTING
     * declaration and failed a build that was correct.</p>
     *
     * <p>The key half of the same method already resolved through the shared helper -- that is
     * what makes the fragment findable at all -- so the two halves were answering one question
     * two different ways.</p>
     */
    @Test
    void aDeclarationSpelledWithCharacterReferencesAgrees() throws BuildException {
        String inject = "<key>CN1ContinuityActivityType</key>"
                + "<string>com&#46;example.app.continuity</string>";

        String out = IPhoneBuilder.withContinuityActivityType(inject, CONTINUITY_TYPE);

        assertEquals(inject, out,
                "an agreeing declaration spelled with a character reference was treated as a "
                        + "conflict, so a correct build was refused");
    }

    /**
     * The same resolution must not blunt the conflict check itself.
     *
     * <p>Decoding is only correct if a genuinely different type still fails: a declaration naming
     * another app's type has the delegate rejecting this application's own continuations while
     * iOS goes on offering them.</p>
     */
    @Test
    void aDifferentTypeSpelledWithCharacterReferencesStillConflicts() {
        String inject = "<key>CN1ContinuityActivityType</key>"
                + "<string>com&#46;other.app.continuity</string>";

        try {
            IPhoneBuilder.withContinuityActivityType(inject, CONTINUITY_TYPE);
            fail("a declaration naming a different type must still be refused");
        } catch (BuildException expected) {
            assertTrue(expected.getMessage().contains("com.other.app.continuity"),
                    "the message should name the DECODED type the project declared: "
                            + expected.getMessage());
        }
    }

    /**
     * Two live root NSUserActivityTypes declarations are refused rather than guessed between.
     *
     * <p>A property list takes the LAST of a duplicated key while every lookup here answers with
     * the first, so merging into the first leaves the second in force on the device: a build that
     * succeeds with the activity types sitting in an array iOS never reads, and Handoff silently
     * not advertised. UIApplicationSceneManifest is already refused for exactly this.</p>
     */
    @Test
    void twoLiveActivityTypesDeclarationsAreRefused() {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.legacy</string></array>"
                + "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.replacement</string></array>";

        try {
            IPhoneBuilder.requireSingleUserActivityTypes(inject);
            fail("a duplicated NSUserActivityTypes must not be silently merged into one of them");
        } catch (BuildException expected) {
            assertTrue(expected.getMessage().contains("twice"), expected.getMessage());
        }
    }

    /**
     * A declaration the project COMMENTED OUT is not a second declaration.
     *
     * <p>The live-element handling exists for exactly this shape -- a project that kept its old
     * declaration above the real one -- so a duplicate check that counted the comment would refuse
     * the projects that handling was added for.</p>
     */
    @Test
    void aCommentedOutActivityTypesDeclarationIsNotADuplicate() throws BuildException {
        String inject = "<!-- <key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.legacy</string></array> -->"
                + "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.replacement</string></array>";

        IPhoneBuilder.requireSingleUserActivityTypes(inject);
    }

    /**
     * A NSUserActivityTypes whose value is not an array is refused once a continuity type depends
     * on it.
     *
     * <p>The caller has already seen the key, so it writes no array of its own, and the merge's
     * documented answer for "no array here" is to return the fragment untouched -- which is right
     * for the intents-only merge, because a SECOND NSUserActivityTypes key is a plist iOS reads
     * unpredictably. Together they mean the continuity type reaches no array at all: the build
     * succeeds, CN1ContinuityActivityType is present, and Handoff is never advertised.</p>
     */
    @Test
    void aNonArrayActivityTypesDeclarationIsRefusedWhenContinuityNeedsIt() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><string>com.example.app.other</string>";

        try {
            IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);
            fail("a NSUserActivityTypes that is not an array must not be silently accepted");
        } catch (BuildException expected) {
            assertTrue(expected.getMessage().contains("NSUserActivityTypes"),
                    expected.getMessage());
            assertTrue(expected.getMessage().contains(CONTINUITY_TYPE), expected.getMessage());
        }
    }

    /**
     * An array that is opened and never closed is refused for the same reason: the merge cannot
     * find where to insert, so it returns the fragment untouched and the type goes nowhere.
     */
    @Test
    void anUnclosedActivityTypesArrayIsRefusedWhenContinuityNeedsIt() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><array><string>a</string>";

        try {
            IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);
            fail("an unterminated NSUserActivityTypes array must not be silently accepted");
        } catch (BuildException expected) {
            assertTrue(expected.getMessage().contains("never closed"), expected.getMessage());
        }
    }

    /**
     * The refusal is scoped to builds that need the array. An intents-only project with the same
     * malformed declaration keeps the behaviour it has today: its plist is wrong either way -- iOS
     * requires an array here -- and failing those builds is not this feature's change to make.
     *
     * <p>This is the half that keeps the two tests above honest. A refusal that fired
     * unconditionally would satisfy both of them and break every existing project.</p>
     */
    @Test
    void theSameDeclarationIsLeftAloneWhenNoContinuityTypeNeedsIt() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><string>com.example.app.other</string>";

        assertEquals(inject, IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), null));
        assertEquals(inject, IPhoneBuilder.mergeUserActivityTypes(inject, noIntents()));
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
    // Only the root dictionary's own declaration counts
    // ------------------------------------------------------------------

    /**
     * Why the detection branch reads the fragment itself instead of stripping comments first.
     * plistWithoutComments is not CDATA-aware: a valid CDATA value carrying the text "&lt;!--"
     * and no "--&gt;" looks like an unterminated comment to it, so everything after is truncated
     * and a live root key beyond it disappears -- and the branch then appends a second one.
     */
    @Test
    void strippingCommentsFirstWouldHideALiveKeyAfterCdata() {
        String plist = "<key>Note</key><string><![CDATA[<!-- not a comment]]></string>"
                + "<key>NSUserActivityTypes</key><array/>";

        assertTrue(IPhoneBuilder.firstLiveRootIndex(plist, "NSUserActivityTypes") > 0, plist);
        assertEquals(-1, IPhoneBuilder.firstLiveRootIndex(
                IPhoneBuilder.plistWithoutComments(plist), "NSUserActivityTypes"), plist);
    }

    /**
     * A commented-out entry is not a declaration. Treating one as already-present added nothing,
     * so the array iOS actually reads never carried the continuity type and Handoff was silently
     * not advertised -- the same failure the commented-out KEY case has one level up.
     */
    @Test
    void aCommentedOutEntryDoesNotSuppressTheType() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<!-- <string>" + CONTINUITY_TYPE + "</string> -->"
                + "<string>com.example.app.other</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
        // Twice in the TEXT -- once dead in the comment, once live -- which is the point.
        assertEquals(2, occurrences(merged, CONTINUITY_TYPE), merged);
    }

    /**
     * A processing instruction between a key and its value is markup a plist parser steps over.
     * Stopping on it made immediateValueIndex answer with the "&lt;?", so both the expansion and
     * the merge decided the value was not an array and dropped every activity type.
     */
    @Test
    void aProcessingInstructionBetweenKeyAndArrayIsSteppedOver() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><?note valid?><array/>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(
                IPhoneBuilder.expandEmptyUserActivityArray(inject), noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    /**
     * A CDATA section is character data, not markup. "&lt;dict&gt;" written inside one is text an
     * application chose to store, and counting it as structure classified a following ROOT
     * NSUserActivityTypes as nested -- so the builder appended a second one and shipped a plist
     * carrying the key twice, which iOS reads unpredictably.
     */
    @Test
    void markupInsideCdataIsNotStructure() {
        String plist = "<key>Note</key><string><![CDATA[a > <dict>]]></string>"
                + "<key>NSUserActivityTypes</key><array/>";

        assertTrue(IPhoneBuilder.firstLiveRootIndex(plist, "NSUserActivityTypes") > 0, plist);
    }

    /**
     * The reverse: a dict that really does open, with a CDATA section inside it, still nests.
     * A fix that simply ignored every "&lt;dict&gt;" would pass the test above and lose this.
     */
    @Test
    void aRealDictStillNestsWhenItContainsCdata() {
        String plist = "<key>MyFeature</key><dict>"
                + "<key>Note</key><string><![CDATA[x]]></string>"
                + "<key>NSUserActivityTypes</key><array/></dict>";

        assertEquals(-1, IPhoneBuilder.firstLiveRootIndex(plist, "NSUserActivityTypes"), plist);
    }

    /**
     * A valid CDATA value may contain the text "&lt;!--" and no "--&gt;". Stripping comments
     * before the lookup read that as an unterminated comment and truncated the fragment, so a
     * live root key after it went missing and a second one was appended beside it.
     */
    @Test
    void aCommentMarkerInsideCdataDoesNotHideALaterKey() {
        String plist = "<key>Note</key><string><![CDATA[<!-- not a comment]]></string>"
                + "<key>NSUserActivityTypes</key><array/>";

        assertTrue(IPhoneBuilder.firstLiveRootIndex(plist, "NSUserActivityTypes") > 0, plist);
    }

    /**
     * iOS reads NSUserActivityTypes at the plist root and nowhere else. Treating one that an
     * application-defined nested dictionary happens to own as the app's declaration merged the
     * continuity type into a dictionary nobody reads it from, AND suppressed the root key that
     * would have advertised Handoff -- so the feature was silently inert while an unrelated
     * property was quietly rewritten.
     */
    @Test
    void aNestedActivityTypesDeclarationIsNotTheAppsDeclaration() {
        String nested = "<key>MyFeature</key><dict>"
                + "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.nested</string></array></dict>";

        assertEquals(-1, IPhoneBuilder.firstLiveRootIndex(nested, "NSUserActivityTypes"), nested);
    }

    /** The root declaration is still found when a nested one precedes it. */
    @Test
    void theRootDeclarationIsFoundPastANestedOne() {
        String both = "<key>MyFeature</key><dict>"
                + "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.nested</string></array></dict>"
                + "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.root</string></array>";

        int at = IPhoneBuilder.firstLiveRootIndex(both, "NSUserActivityTypes");

        assertTrue(at > both.indexOf("</dict>"), "resolved the nested key at " + at + ": " + both);
    }

    /** The merge follows the same rule, or it rewrites an array the detection branch ignored. */
    @Test
    void theMergeTargetsTheRootArrayNotANestedOne() throws BuildException {
        String both = "<key>MyFeature</key><dict>"
                + "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.nested</string></array></dict>"
                + "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.root</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(both, noIntents(), CONTINUITY_TYPE);

        int nestedEnd = merged.indexOf("</dict>");
        assertTrue(merged.indexOf(CONTINUITY_TYPE) > nestedEnd,
                "the continuity type landed inside the nested dictionary: " + merged);
        assertEquals(1, occurrences(merged, CONTINUITY_TYPE), merged);
        assertTrue(merged.contains("<string>com.example.app.nested</string>"),
                "the nested array was rewritten: " + merged);
    }

    /** A self-closing dict is one element and must not be read as opening a nesting level. */
    @Test
    void aSelfClosingDictDoesNotOpenANestingLevel() {
        String plist = "<key>Empty</key><dict/>"
                + "<key>NSUserActivityTypes</key><array/>";

        assertTrue(IPhoneBuilder.firstLiveRootIndex(plist, "NSUserActivityTypes") > 0, plist);
    }

    // ------------------------------------------------------------------
    // Merging into an array the application supplied
    // ------------------------------------------------------------------

    @Test
    void continuityMergesIntoAnArrayTheApplicationDeclared() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.legacyHandoff</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);

        assertEquals(1, occurrences(merged, "NSUserActivityTypes"), merged);
        assertTrue(merged.contains("<string>com.example.app.legacyHandoff</string>"), merged);
        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    @Test
    void intentsAndContinuityBothMergeIntoOneSuppliedArray() throws BuildException {
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
    void anAlreadyDeclaredContinuityTypeIsNotDuplicated() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<string>" + CONTINUITY_TYPE + "</string></array>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), CONTINUITY_TYPE);

        assertEquals(1, occurrences(merged, "<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    /**
     * Returning the fragment unchanged is still the answer when nothing depends on the array.
     *
     * <p>This test used to pass a continuity type and assert the same thing, which is the
     * behaviour that shipped the feature inert: the caller sees the key and writes no array, this
     * writes nothing into the one that is there, and the type ends up in no array at all. Its
     * real subject -- that a value which is not an array is never edited -- is unchanged and now
     * asked without a continuity type; the refusal has tests of its own above.</p>
     */
    @Test
    void aFragmentWhoseArrayCannotBeFoundIsReturnedUnchanged() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><string>not an array</string>";

        assertEquals(inject, IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), null));
    }

    /**
     * The parser has to accept the shapes a hand-written fragment really carries.
     */
    @Test
    void aSpacedClosingTagIsStillMergedInto() throws BuildException {
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
    void aSelfClosingArrayIsExpandedSoTheMergeCanSeeIt() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><array/>";

        String expanded = IPhoneBuilder.expandEmptyUserActivityArray(inject);
        String merged = IPhoneBuilder.mergeUserActivityTypes(expanded, noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
        assertEquals(1, occurrences(merged, "NSUserActivityTypes"), merged);
    }

    @Test
    void aSelfClosingArrayWithWhitespaceAndASpacedTagIsStillExpanded() throws BuildException {
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
    void aCommentedDeclarationAboveALiveOneIsNotTheOneMergedInto() throws BuildException {
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
    void aCommentBetweenTheKeyAndItsArrayIsSteppedOver() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><!-- why we declare this --><array/>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(
                IPhoneBuilder.expandEmptyUserActivityArray(inject), noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
        assertEquals(1, occurrences(merged, "NSUserActivityTypes"), merged);
    }

    /**
     * When this key's value is not an array, the ids never reach a LATER key's array. An unbounded
     * search reached past it and inserted them into the next array it found, corrupting a property
     * this code was never asked about.
     *
     * <p>Asked both ways, because the refusal must not be mistaken for the guarantee. Without a
     * continuity type the fragment comes back untouched, which is what proves nothing was
     * borrowed; with one the build is refused, and the refusal has to happen INSTEAD of the
     * corruption rather than after it -- so the unrelated array is checked in the message-free
     * path where it could actually have been edited.</p>
     */
    @Test
    void aNonArrayValueDoesNotBorrowALaterKeysArray() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><string>not an array</string>"
                + "<key>SomethingElse</key><array><string>keep</string></array>";

        assertEquals(inject,
                IPhoneBuilder.mergeUserActivityTypes(inject, intents("logWorkout"), null),
                "an unrelated array was edited");

        try {
            IPhoneBuilder.mergeUserActivityTypes(inject, intents("logWorkout"), CONTINUITY_TYPE);
            fail("a continuity type with nowhere to go must not be accepted");
        } catch (BuildException expected) {
            assertFalse(expected.getMessage().contains("SomethingElse"), expected.getMessage());
        }
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
    void aCommentInsideTheKeyDoesNotEndItEarly() throws BuildException {
        String inject = "<key><!-- </key> -->NSUserActivityTypes</key><array/>";

        String merged = IPhoneBuilder.mergeUserActivityTypes(
                IPhoneBuilder.expandEmptyUserActivityArray(inject), noIntents(), CONTINUITY_TYPE);

        assertTrue(merged.contains("<string>" + CONTINUITY_TYPE + "</string>"), merged);
    }

    @Test
    void nothingToAddLeavesTheFragmentAlone() throws BuildException {
        String inject = "<key>NSUserActivityTypes</key><array>"
                + "<string>com.example.app.legacyHandoff</string></array>";

        assertEquals(inject, IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), null));
        assertFalse(IPhoneBuilder.mergeUserActivityTypes(inject, noIntents(), null)
                .contains("null"));
    }
}
