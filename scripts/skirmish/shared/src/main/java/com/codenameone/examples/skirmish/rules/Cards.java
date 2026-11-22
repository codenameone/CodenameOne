/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.skirmish.rules;

import java.util.ArrayList;
import java.util.List;

/// The card catalog: the single place where every unit, building and spell is
/// defined. Change a number here and it changes everywhere the game runs.
///
/// Speeds are milli-tiles per tick: 45 is 0.9 tiles a second, a "medium" walker.
public final class Cards {
    public static final String KING_TOWER = "king-tower";
    public static final String PRINCESS_TOWER = "princess-tower";

    private static final List<CardDef> ALL = new ArrayList<CardDef>();

    static {
        add(new CardDef(KING_TOWER, "King Tower", CardDef.TOWER, 0)
                .hp(3800).damage(90).hitTicks(20).range(7000).sight(7000)
                .targets(TargetKind.GROUND_AND_AIR).radius(2000));
        add(new CardDef(PRINCESS_TOWER, "Archer Tower", CardDef.TOWER, 0)
                .hp(2300).damage(62).hitTicks(18).range(7500).sight(7500)
                .targets(TargetKind.GROUND_AND_AIR).radius(1500));

        // tag::skirmish-cards[]
        add(new CardDef("squire", "Squire", CardDef.TROOP, 3)
                .hp(1450).damage(160).hitTicks(24).range(700).speed(45).radius(500));
        // end::skirmish-cards[]
        add(new CardDef("twin-archers", "Twin Archers", CardDef.TROOP, 3)
                .hp(300).damage(100).hitTicks(18).range(5000).speed(45)
                .targets(TargetKind.GROUND_AND_AIR).count(2).radius(400));
        add(new CardDef("ogre", "Ogre", CardDef.TROOP, 5)
                .hp(3800).damage(230).hitTicks(30).range(1000).speed(30)
                .buildingsOnly().radius(700));
        add(new CardDef("bat-swarm", "Bat Swarm", CardDef.TROOP, 2)
                .hp(80).damage(70).hitTicks(22).range(500).speed(70)
                .targets(TargetKind.GROUND_AND_AIR).flying().count(4).radius(300));
        add(new CardDef("sharpshooter", "Sharpshooter", CardDef.TROOP, 4)
                .hp(650).damage(200).hitTicks(22).range(6000).speed(45)
                .targets(TargetKind.GROUND_AND_AIR).radius(450));
        add(new CardDef("spearmen", "Spearmen", CardDef.TROOP, 3)
                .hp(450).damage(110).hitTicks(22).range(1200).speed(60).count(3).radius(400));
        add(new CardDef("drake", "Drake", CardDef.TROOP, 4)
                .hp(1050).damage(160).hitTicks(30).range(3000).speed(55)
                .targets(TargetKind.GROUND_AND_AIR).flying().splash(1200).radius(600));
        add(new CardDef("imp", "Imp", CardDef.TROOP, 0)
                .hp(160).damage(90).hitTicks(22).range(500).speed(70).radius(350).deploy(10));
        add(new CardDef("imp-den", "Imp Den", CardDef.BUILDING, 4)
                .hp(1200).spawns("imp", 80).lifetime(1000).radius(1000).range(0));
        add(new CardDef("fire-orb", "Fire Orb", CardDef.SPELL, 4)
                .damage(570).splash(2500).towerPercent(35).deploy(16)
                .targets(TargetKind.GROUND_AND_AIR | TargetKind.BUILDINGS));
        add(new CardDef("arrow-rain", "Arrow Rain", CardDef.SPELL, 3)
                .damage(240).splash(4000).towerPercent(30).deploy(12)
                .targets(TargetKind.GROUND_AND_AIR | TargetKind.BUILDINGS));
    }

    private Cards() {
    }

    private static void add(CardDef def) {
        ALL.add(def);
    }

    static List<CardDef> builtins() {
        return new ArrayList<CardDef>(ALL);
    }

    // The built-in set. A match on custom content asks its own `CardSet`.

    public static CardDef get(String id) {
        return CardSet.BUILTIN.get(id);
    }

    public static int indexOf(String id) {
        return CardSet.BUILTIN.indexOf(id);
    }

    public static CardDef at(int index) {
        return CardSet.BUILTIN.at(index);
    }

    public static List<CardDef> playable() {
        return CardSet.BUILTIN.playable();
    }

    public static boolean isValidDeck(String[] deck) {
        return CardSet.BUILTIN.isValidDeck(deck);
    }
}
