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
package androidx.recyclerview.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// DiffUtil's updates, applied in order to the old list, must produce the
/// new list -- for random lists, with and without move detection -- and the
/// content changes must land on the right items.
public class DiffUtilTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// Items are "key:version": the same item when the keys match, the same
    /// content when the versions do too.
    private static DiffUtil.Callback callback(final List<String> oldList, final List<String> newList) {
        return new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return key(oldList.get(oldItemPosition)).equals(key(newList.get(newItemPosition)));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
            }

            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                return newList.get(newItemPosition);
            }
        };
    }

    private static String key(String s) {
        int i = s.indexOf(':');
        return i < 0 ? s : s.substring(0, i);
    }

    /// Replays updates on a copy of the old list.
    private static final class Replay implements ListUpdateCallback {
        final List<String> list;
        int operations;

        Replay(List<String> old) {
            list = new ArrayList<String>(old);
        }

        @Override
        public void onInserted(int position, int count) {
            operations++;
            for (int i = 0; i < count; i++) {
                list.add(position, "?");
            }
        }

        @Override
        public void onRemoved(int position, int count) {
            operations++;
            for (int i = 0; i < count; i++) {
                list.remove(position);
            }
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            operations++;
            list.add(toPosition, list.remove(fromPosition));
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            operations++;
            for (int i = 0; i < count; i++) {
                list.set(position + i, "!" + payload);
            }
        }
    }

    private static void check(List<String> oldList, List<String> newList, boolean detectMoves) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback(oldList, newList), detectMoves);
        Replay replay = new Replay(oldList);
        result.dispatchUpdatesTo(replay);
        assertEquals(newList.size(), replay.list.size());
        for (int i = 0; i < newList.size(); i++) {
            String got = replay.list.get(i);
            String want = newList.get(i);
            if (got.equals("?")) {
                int old = result.convertNewPositionToOld(i);
                assertEquals("an inserted position has no old item", DiffResult_NO_POSITION, old);
            } else if (got.startsWith("!")) {
                assertEquals("changed to the new content at " + i, "!" + want, got);
            } else {
                assertEquals("kept item at " + i + " of " + newList, want, got);
            }
        }
    }

    private static final int DiffResult_NO_POSITION = DiffUtil.DiffResult.NO_POSITION;

    private static List<String> list(String... items) {
        List<String> l = new ArrayList<String>();
        for (String s : items) {
            l.add(s);
        }
        return l;
    }

    @Test
    public void simpleCases() {
        check(list(), list("a"), true);
        check(list("a"), list(), true);
        check(list("a", "b", "c"), list("a", "c"), true);
        check(list("a", "c"), list("a", "b", "c"), true);
        check(list("a", "b", "c"), list("c", "b", "a"), true);
        check(list("a", "b", "c"), list("c", "b", "a"), false);
        check(list("a:1", "b:1"), list("a:2", "b:1"), true);
        check(list("a", "b", "c", "d"), list("d", "a", "b", "c"), true);
    }

    @Test
    public void moveIsOneOperation() {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(
                callback(list("a", "b", "c", "d", "e"), list("a", "c", "d", "e", "b")), true);
        Replay replay = new Replay(list("a", "b", "c", "d", "e"));
        result.dispatchUpdatesTo(replay);
        assertEquals(list("a", "c", "d", "e", "b"), replay.list);
        assertTrue("at most two operations, got " + replay.operations, replay.operations <= 2);
        assertEquals(4, result.convertOldPositionToNew(1));
    }

    @Test
    public void randomListsReplayToTheNewList() {
        Random random = new Random(42);
        for (int round = 0; round < 400; round++) {
            List<String> oldList = new ArrayList<String>();
            int oldSize = random.nextInt(25);
            for (int i = 0; i < oldSize; i++) {
                oldList.add("k" + i + ":" + random.nextInt(2));
            }
            List<String> newList = new ArrayList<String>();
            for (String s : oldList) {
                if (random.nextInt(4) != 0) {
                    newList.add(random.nextInt(5) == 0 ? key(s) + ":" + random.nextInt(3) : s);
                }
            }
            int inserts = random.nextInt(6);
            for (int i = 0; i < inserts; i++) {
                newList.add(random.nextInt(newList.size() + 1), "n" + round + "_" + i + ":0");
            }
            if (newList.size() > 1 && random.nextBoolean()) {
                newList.add(random.nextInt(newList.size()), newList.remove(random.nextInt(newList.size())));
            }
            check(oldList, newList, random.nextBoolean());
        }
    }

    @Test
    public void batchingMergesRanges() {
        final List<String> calls = new ArrayList<String>();
        BatchingListUpdateCallback batching = new BatchingListUpdateCallback(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                calls.add("ins " + position + "," + count);
            }

            @Override
            public void onRemoved(int position, int count) {
                calls.add("rem " + position + "," + count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                calls.add("mov " + fromPosition + "," + toPosition);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                calls.add("chg " + position + "," + count);
            }
        });
        batching.onInserted(3, 1);
        batching.onInserted(4, 1);
        batching.onRemoved(1, 1);
        batching.onRemoved(0, 1);
        batching.onChanged(5, 1, null);
        batching.onChanged(6, 1, null);
        batching.dispatchLastEvent();
        assertEquals(list("ins 3,2", "rem 0,2", "chg 5,2"), calls);
    }

    @Test
    public void spanLookups() {
        GridLayoutManager.SpanSizeLookup lookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? 3 : 1;
            }
        };
        // A full-width header, then rows of three.
        assertEquals(0, lookup.getSpanGroupIndex(0, 3));
        assertEquals(1, lookup.getSpanGroupIndex(1, 3));
        assertEquals(1, lookup.getSpanGroupIndex(3, 3));
        assertEquals(2, lookup.getSpanGroupIndex(4, 3));
        assertEquals(0, lookup.getSpanIndex(1, 3));
        assertEquals(2, lookup.getSpanIndex(3, 3));
        GridLayoutManager.DefaultSpanSizeLookup def = new GridLayoutManager.DefaultSpanSizeLookup();
        assertEquals(3, def.getSpanGroupIndex(10, 3));
        assertEquals(1, def.getSpanIndex(10, 3));
    }
}
