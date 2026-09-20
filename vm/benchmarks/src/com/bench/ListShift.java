/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.bench;

import java.util.ArrayList;

/**
 * Drives the same-block reference move -- ArrayList insert and remove in the MIDDLE
 * of a list, which shift the storage block with System.arraycopy.
 *
 * <p>This exists for {@code run-gc-verify.sh}'s seventh self-test. The SATB deletion
 * barrier for those shifts was narrowed from the whole destination range to the
 * {@code min(count, |to-from|)} slots that actually leave the block, and nothing in
 * the VM could contradict the claim: with the barrier removed outright, all six
 * existing self-tests and all 33 gauntlet tortures reported GREEN.</p>
 *
 * <p>What makes the check possible is that the invariant does not depend on the
 * collector at all. "Every overwritten slot whose old value survives nowhere in the
 * block afterwards must be inside the reported lost range" is a property of the move,
 * so {@code cn1SatbVerifyMove} can check it on every move and this driver only has to
 * perform a lot of varied shifts. An earlier attempt to catch the same bug with a
 * GC torture failed because reaching the hazard needs a barrier-free republication
 * path the VM no longer has.</p>
 *
 * <p>The shapes are deliberately varied -- front, middle, back, and both directions
 * -- because the lost range is computed differently for a shift left than a shift
 * right, and an arithmetic error can easily live in only one of them.</p>
 */
public class ListShift {
    private static final int LISTS = 24;
    private static final int SIZE = 96;
    private static final int ROUNDS = 40;

    static final class Item {
        final int id;
        Item(int id) { this.id = id; }
    }

    public static void main(String[] args) {
        ArrayList<ArrayList<Item>> all = new ArrayList<ArrayList<Item>>();
        for (int l = 0; l < LISTS; l++) {
            ArrayList<Item> list = new ArrayList<Item>();
            for (int i = 0; i < SIZE; i++) {
                list.add(new Item(l * SIZE + i));
            }
            all.add(list);
        }

        long checksum = 0;
        for (int round = 0; round < ROUNDS; round++) {
            for (int l = 0; l < LISTS; l++) {
                ArrayList<Item> list = all.get(l);
                // remove: shift LEFT, the dropped slot is at the removal index
                for (int k = 0; k < 8 && list.size() > 4; k++) {
                    int at = (round * 7 + k * 13 + l) % (list.size() - 1);
                    list.remove(at);
                }
                // insert: shift RIGHT, the dropped slot is at the far end
                for (int k = 0; k < 8; k++) {
                    int at = (round * 5 + k * 11 + l) % (list.size() + 1);
                    list.add(at, new Item(round * 1000 + k));
                }
                // front and back, where the shift covers the whole block or none of it
                list.add(0, new Item(round));
                list.remove(0);
                list.add(list.size(), new Item(round));
                list.remove(list.size() - 1);
            }
            for (int l = 0; l < LISTS; l++) {
                ArrayList<Item> list = all.get(l);
                for (int i = 0; i < list.size(); i++) {
                    checksum += list.get(i).id;
                }
            }
        }
        System.out.println("ListShift checksum=" + checksum);
    }
}
