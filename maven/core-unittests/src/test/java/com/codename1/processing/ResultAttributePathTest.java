/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.processing;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// The attribute half of the path language, which the developer guide has
/// always documented and which answered nothing at all.
class ResultAttributePathTest {

    /// Three players, and the third carries no rank.
    private static final String RANKINGS =
            "<rankings>"
            + "<player id='1036' rank='1'><lastname>Tomic</lastname></player>"
            + "<player id='2585' rank='2'><lastname>Ebden</lastname></player>"
            + "<player id='6457'><lastname>Hewitt</lastname></player>"
            + "</rankings>";

    private static Result rankings() {
        return Result.fromContent(RANKINGS, Result.XML);
    }

    @Test
    void anAttributeIsReadFromEveryNodeTheStepSelected() {
        // SubContent.getAttribute() answers null by definition -- a node set
        // has no single attribute value -- and Result asked it for one, so a
        // step matching more than one element dropped them all. A single match
        // worked, which is what made this look like a predicate problem.
        int[] all = rankings().getAsIntegerArray("//player/@id");
        assertEquals(3, all.length, "reading an attribute across a node set answered nothing");
        assertEquals(1036, all[0]);
        assertEquals(2585, all[1]);
        assertEquals(6457, all[2]);
    }

    @Test
    void aComparisonPredicateKeepsEveryMatchingAttribute() {
        // The comparison itself always worked; what followed it did not.
        int[] top2 = rankings().getAsIntegerArray("//player[@rank < 3]/@id");
        assertEquals(2, top2.length, "the guide's own top-2 example answered nothing");
        assertEquals(1036, top2[0]);
        assertEquals(2585, top2[1]);
    }

    @Test
    void anAttributeExistencePredicateMatchesTheElementsThatCarryIt() {
        // AttributeEvaluator.evaluateSingle looked up the predicate text with
        // its '@' still on, so it asked for an attribute named "@rank".
        int[] ranked = rankings().getAsIntegerArray("//player[@rank]/@id");
        assertEquals(2, ranked.length, "[@rank] matched nothing");
        assertEquals(1036, ranked[0]);
        assertEquals(2585, ranked[1]);
    }

    @Test
    void aNullAttributePredicateMatchesTheElementsThatLackIt() {
        // The documented way to ask for absence, and it returned before the
        // rvalue was read -- so the one predicate written for a missing
        // attribute could never match one.
        int[] unranked = rankings().getAsIntegerArray("//player[@rank=null]/@id");
        assertEquals(1, unranked.length, "[@rank=null] matched nothing");
        assertEquals(6457, unranked[0]);
    }

    @Test
    void aQuotedNullIsStillAValueToCompareAgainst() {
        // 'null' in quotes is a string an attribute can really hold, so it
        // must not be read as the absence test.
        Result r = Result.fromContent(
                "<t><a v='null'/><a v='x'/></t>", Result.XML);
        assertEquals("null", r.getAsString("/t/a[@v='null']/@v"));
    }

    @Test
    void aBranchThatResolvedToNothingIsSkippedRatherThanDereferenced() {
        // apply() puts a null in the node set for every branch where the step
        // after a predicate matched nothing, and it can nest one set inside
        // another. Reading attributes per node walked straight into both.
        Result r = Result.fromContent(
                "<t><player rank='1'><address code='X'/></player>"
                + "<player rank='2'/></t>", Result.XML);
        String[] codes = r.getAsStringArray("//player[@rank]/address/@code");
        assertEquals(1, codes.length, "a branch with no address took the others with it");
        assertEquals("X", codes[0]);
    }

    @Test
    void jsonHasNoAttributesSoTheFieldIsTheChild() {
        // HashtableContent.getAttribute() answers null for every name, so an
        // absence test that only asked it matched every object in the document
        // -- including the ones carrying the field. Both halves read the child.
        Result r = Result.fromContent(
                "{\"players\":[{\"rank\":1,\"name\":\"A\"},{\"name\":\"B\"}]}",
                Result.JSON);
        String[] unranked = r.getAsStringArray("/players[@rank=null]/name");
        assertEquals(1, unranked.length, "[@rank=null] matched a player that has a rank");
        assertEquals("B", unranked[0]);

        String[] ranked = r.getAsStringArray("/players[@rank]/name");
        assertEquals(1, ranked.length, "[@rank] and [@rank=null] disagree on JSON");
        assertEquals("A", ranked[0]);
    }

    @Test
    void xmlKeepsAttributesAndChildrenApart() {
        // The JSON fallback must not reach XML: an element there can carry a
        // "rank" child and no rank attribute, and "[@rank]" asks about the
        // attribute.
        Result r = Result.fromContent(
                "<t><player id='1'><rank>1</rank></player>"
                + "<player id='2' rank='2'/></t>", Result.XML);

        String[] byAttribute = r.getAsStringArray("//player[@rank]/@id");
        assertEquals(1, byAttribute.length, "[@rank] matched an element whose rank is a child");
        assertEquals("2", byAttribute[0]);

        // And a bare "[rank]" is not the other half of the pair: the factory
        // has no branch for a name with no comparison in it, so the child
        // existence form the guide once promised does not exist.
        try {
            r.getAsStringArray("//player[rank]/@id");
            fail("a bare child name was accepted");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().indexOf("comparator") >= 0,
                    "wrong diagnosis: " + expected.getMessage());
        }
    }

    @Test
    void aJsonFieldSetToNullCountsAsNotHavingOne() {
        // Three cases, and the pair has to partition them: no key at all, a
        // key whose value is null, and a real value. A null value counts as
        // not having one, which is what reading the field answers anyway and
        // the only reading XML can share -- an attribute there cannot be
        // present and null at once. Key presence is a different question and
        // the expression language cannot ask it.
        Map<String, Object> nullValued = new HashMap<String, Object>();
        nullValued.put("rank", null);
        nullValued.put("name", "A");
        Map<String, Object> absent = new HashMap<String, Object>();
        absent.put("name", "B");
        Map<String, Object> real = new HashMap<String, Object>();
        real.put("rank", Integer.valueOf(3));
        real.put("name", "C");
        List<Object> players = new ArrayList<Object>();
        players.add(nullValued);
        players.add(absent);
        players.add(real);
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("players", players);

        Result r = Result.fromContent(root);
        String[] ranked = r.getAsStringArray("/players[@rank]/name");
        assertEquals(1, ranked.length, "[@rank] matched a player with no usable rank");
        assertEquals("C", ranked[0]);

        String[] unranked = r.getAsStringArray("/players[@rank=null]/name");
        assertEquals(2, unranked.length, "[@rank=null] is not the complement of [@rank]");
        assertEquals("A", unranked[0]);
        assertEquals("B", unranked[1]);
    }

    @Test
    void anAttributeExpressionReadsAJsonFieldEverywhereItAppears() {
        // The guide has always said an attribute expression selects the child
        // of that name on a JSON document. Only the existence test did: a
        // comparison and a terminal read both went to getAttribute(), which
        // answers null for every name there, so they found nothing.
        Result r = Result.fromContent(
                "{\"players\":[{\"id\":10,\"rank\":1,\"name\":\"A\"},"
                + "{\"id\":20,\"rank\":5,\"name\":\"B\"}]}", Result.JSON);

        assertEquals(2, r.getAsStringArray("/players/@id").length,
                "reading an attribute at the end of a path found no JSON field");
        String[] byValue = r.getAsStringArray("/players[@rank='1']/name");
        assertEquals(1, byValue.length, "an equality predicate found no JSON field");
        assertEquals("A", byValue[0]);
        String[] byComparison = r.getAsStringArray("/players[@rank < 3]/name");
        assertEquals(1, byComparison.length, "a comparison found no JSON field");
        assertEquals("A", byComparison[0],
                "the comparison matched the wrong side");
    }

    @Test
    void aTextComparisonIsNotBackwards() {
        // The non-numeric branch of the less-than method answered when the
        // attribute sorted AFTER the value, and the greater-than method did
        // the reverse. Every comparison against a text attribute was inverted.
        Result r = Result.fromContent(
                "<t><p name='alpha' id='1'/><p name='zulu' id='2'/></t>", Result.XML);

        String[] early = r.getAsStringArray("/t/p[@name < 'm']/@id");
        assertEquals(1, early.length, "less-than matched the wrong element");
        assertEquals("1", early[0]);

        String[] late = r.getAsStringArray("/t/p[@name > 'm']/@id");
        assertEquals(1, late.length, "greater-than matched the wrong element");
        assertEquals("2", late[0]);
    }

    @Test
    void aDecimalChildComparisonDoesNotThrow() {
        // isNumeric learning to read a decimal put "2.5" through
        // TextEvaluator's Integer.parseInt, which threw. The two halves of the
        // comparison have to read the same grammar.
        Result r = Result.fromContent(
                "<t><item><price>2.5</price><n>A</n></item></t>", Result.XML);
        String[] found = r.getAsStringArray("/t/item[price=2.5]/n");
        assertEquals(1, found.length, "a decimal equality found nothing");
        assertEquals("A", found[0]);
    }

    @Test
    void aBigJsonNumberIsStillANumber() {
        // The parser hands a large number back as "1.0E7". Rejecting the
        // exponent dropped the comparison to text, where it sorts before "3".
        Result r = Result.fromContent(
                "{\"players\":[{\"rank\":10000000,\"name\":\"BIG\"},"
                + "{\"rank\":1,\"name\":\"A\"}]}", Result.JSON);
        String[] small = r.getAsStringArray("/players[@rank < 3]/name");
        assertEquals(1, small.length, "ten million compared as less than three");
        assertEquals("A", small[0]);
    }

    @Test
    void aChildTextComparisonIsNotBackwards() {
        // TextEvaluator's non-numeric branch was inverted the same way the
        // attribute one was.
        Result r = Result.fromContent(
                "<t><p><name>alpha</name><id>1</id></p>"
                + "<p><name>zulu</name><id>2</id></p></t>", Result.XML);
        // Through text(), which is how the factory reaches TextEvaluator with
        // an ordering comparison at all -- a bare child name with < is not
        // dispatched anywhere.
        String[] early = r.getAsStringArray("//name[text() < 'm']/../id");
        assertEquals(1, early.length, "less-than matched the wrong element");
        assertEquals("1", early[0]);

        String[] late = r.getAsStringArray("//name[text() > 'm']/../id");
        assertEquals(1, late.length, "greater-than matched the wrong element");
        assertEquals("2", late[0]);
    }

    @Test
    void everyChildOfThatNameIsConsidered() {
        // An element can carry several children of one name, and the numeric
        // branch returned after the first instead of walking them the way the
        // text branch always has -- so <price>1.5</price> hid the 2.5 behind
        // it. Invisible while a decimal was not a number at all.
        Result r = Result.fromContent(
                "<t><item><price>1.5</price><price>2.5</price><n>A</n></item></t>",
                Result.XML);
        assertEquals(1, r.getAsStringArray("/t/item[price=2.5]/n").length,
                "the second price was never looked at");
        assertEquals(1, r.getAsStringArray("/t/item[price=1.5]/n").length,
                "the first price stopped matching");
    }

    @Test
    void anIdTooBigForADoubleStaysExact() {
        // 9007199254740992 and 9007199254740993 are the same double, and an
        // id of that size is ordinary. Comparing them as doubles made a
        // predicate written for one select both.
        Result r = Result.fromContent(
                "<t><p id='9007199254740992' n='X'/>"
                + "<p id='9007199254740993' n='Y'/></t>", Result.XML);
        String[] found = r.getAsStringArray("/t/p[@id='9007199254740993']/@n");
        assertEquals(1, found.length, "two distinct ids compared equal");
        assertEquals("Y", found[0]);
    }

    @Test
    void anUnclosedPredicateIsReportedRatherThanSpun() {
        // getPredicate() ran off the end with the bracket still open, left the
        // position where it was, and tokenize() called it again from there for
        // ever: the process died with an OutOfMemoryError. It is reachable
        // from a well-formed expression, because a predicate containing a
        // nested one is split at its comparator before being tokenized.
        try {
            rankings().getAsIntegerArray("//player[//address[country='CA']]/@id");
            fail("an unclosed predicate was accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().indexOf("unclosed predicate") >= 0,
                    "wrong diagnosis: " + expected.getMessage());
        }
    }
}
