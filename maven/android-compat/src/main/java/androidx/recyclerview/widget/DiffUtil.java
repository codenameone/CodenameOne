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

import java.util.ArrayList;
import java.util.Arrays;

/// Computes the updates that turn one list into another: Myers'
/// difference algorithm in linear space, plus a pass that pairs removed and
/// inserted items that are the same item as moves.
public class DiffUtil {

    private DiffUtil() {
    }

    public static DiffResult calculateDiff(Callback cb) {
        return calculateDiff(cb, true);
    }

    public static DiffResult calculateDiff(Callback cb, boolean detectMoves) {
        int oldSize = cb.getOldListSize();
        int newSize = cb.getNewListSize();
        int[] oldToNew = new int[oldSize];
        int[] newToOld = new int[newSize];
        Arrays.fill(oldToNew, DiffResult.NO_POSITION);
        Arrays.fill(newToOld, DiffResult.NO_POSITION);
        ArrayList<int[]> stack = new ArrayList<int[]>();
        stack.add(new int[] {0, oldSize, 0, newSize});
        while (!stack.isEmpty()) {
            int[] r = stack.remove(stack.size() - 1);
            diffRange(cb, r[0], r[1], r[2], r[3], oldToNew, newToOld, stack);
        }
        boolean[] moved = new boolean[oldSize];
        if (detectMoves) {
            for (int i = 0; i < oldSize; i++) {
                if (oldToNew[i] != DiffResult.NO_POSITION) {
                    continue;
                }
                for (int j = 0; j < newSize; j++) {
                    if (newToOld[j] == DiffResult.NO_POSITION && cb.areItemsTheSame(i, j)) {
                        oldToNew[i] = j;
                        newToOld[j] = i;
                        moved[i] = true;
                        break;
                    }
                }
            }
        }
        return new DiffResult(cb, oldToNew, newToOld, moved);
    }

    private static void match(int o, int n, int[] oldToNew, int[] newToOld) {
        oldToNew[o] = n;
        newToOld[n] = o;
    }

    /// Matches the common prefix and suffix of the range, then splits what
    /// remains at its middle snake, pushing both halves.
    private static void diffRange(Callback cb, int oS, int oE, int nS, int nE, int[] oldToNew, int[] newToOld,
                                  ArrayList<int[]> stack) {
        while (oS < oE && nS < nE && cb.areItemsTheSame(oS, nS)) {
            match(oS, nS, oldToNew, newToOld);
            oS++;
            nS++;
        }
        while (oS < oE && nS < nE && cb.areItemsTheSame(oE - 1, nE - 1)) {
            match(oE - 1, nE - 1, oldToNew, newToOld);
            oE--;
            nE--;
        }
        int n = oE - oS;
        int m = nE - nS;
        if (n == 0 || m == 0) {
            return;
        }
        int delta = n - m;
        boolean odd = (delta & 1) != 0;
        int max = (n + m + 1) / 2;
        int offset = max + 1;
        int[] vf = new int[2 * max + 3];
        int[] vb = new int[2 * max + 3];
        vf[offset + 1] = 0;
        vb[offset + 1] = 0;
        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int x;
                if (k == -d || (k != d && vf[offset + k - 1] < vf[offset + k + 1])) {
                    x = vf[offset + k + 1];
                } else {
                    x = vf[offset + k - 1] + 1;
                }
                int y = x - k;
                int sx = x;
                int sy = y;
                while (x < n && y < m && cb.areItemsTheSame(oS + x, nS + y)) {
                    x++;
                    y++;
                }
                vf[offset + k] = x;
                if (odd && k >= delta - (d - 1) && k <= delta + (d - 1)) {
                    int kb = delta - k;
                    if (vf[offset + k] + vb[offset + kb] >= n) {
                        for (int t = 0; t < x - sx; t++) {
                            match(oS + sx + t, nS + sy + t, oldToNew, newToOld);
                        }
                        split(oS, oE, nS, nE, oS + sx, nS + sy, oS + x, nS + y, stack);
                        return;
                    }
                }
            }
            for (int k = -d; k <= d; k += 2) {
                int x;
                if (k == -d || (k != d && vb[offset + k - 1] < vb[offset + k + 1])) {
                    x = vb[offset + k + 1];
                } else {
                    x = vb[offset + k - 1] + 1;
                }
                int y = x - k;
                int sx = x;
                int sy = y;
                while (x < n && y < m && cb.areItemsTheSame(oS + n - x - 1, nS + m - y - 1)) {
                    x++;
                    y++;
                }
                vb[offset + k] = x;
                int kf = delta - k;
                if (!odd && kf >= -d && kf <= d) {
                    if (vb[offset + k] + vf[offset + kf] >= n) {
                        int startX = n - x;
                        int startY = m - y;
                        int endX = n - sx;
                        int endY = m - sy;
                        for (int t = 0; t < endX - startX; t++) {
                            match(oS + startX + t, nS + startY + t, oldToNew, newToOld);
                        }
                        split(oS, oE, nS, nE, oS + startX, nS + startY, oS + endX, nS + endY, stack);
                        return;
                    }
                }
            }
        }
    }

    private static void split(int oS, int oE, int nS, int nE, int snakeStartO, int snakeStartN, int snakeEndO,
                              int snakeEndN, ArrayList<int[]> stack) {
        boolean leftWhole = snakeStartO == oE && snakeStartN == nE;
        boolean rightWhole = snakeEndO == oS && snakeEndN == nS;
        if (!leftWhole && (snakeStartO > oS || snakeStartN > nS)) {
            stack.add(new int[] {oS, snakeStartO, nS, snakeStartN});
        }
        if (!rightWhole && (snakeEndO < oE || snakeEndN < nE)) {
            stack.add(new int[] {snakeEndO, oE, snakeEndN, nE});
        }
    }

    /// Describes the old and new lists to the diff.
    public abstract static class Callback {
        public abstract int getOldListSize();

        public abstract int getNewListSize();

        public abstract boolean areItemsTheSame(int oldItemPosition, int newItemPosition);

        public abstract boolean areContentsTheSame(int oldItemPosition, int newItemPosition);

        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return null;
        }
    }

    /// Compares two items of a list adapter's lists.
    public abstract static class ItemCallback<T> {
        public abstract boolean areItemsTheSame(T oldItem, T newItem);

        public abstract boolean areContentsTheSame(T oldItem, T newItem);

        public Object getChangePayload(T oldItem, T newItem) {
            return null;
        }
    }

    /// The outcome of a diff: which old position became which new one, and
    /// the update operations that get there.
    public static class DiffResult {
        public static final int NO_POSITION = -1;

        private final Callback mCallback;
        private final int[] mOldToNew;
        private final int[] mNewToOld;
        private final boolean[] mMoved;

        DiffResult(Callback callback, int[] oldToNew, int[] newToOld, boolean[] moved) {
            mCallback = callback;
            mOldToNew = oldToNew;
            mNewToOld = newToOld;
            mMoved = moved;
        }

        public int convertOldPositionToNew(int oldListPosition) {
            if (oldListPosition < 0 || oldListPosition >= mOldToNew.length) {
                throw new IndexOutOfBoundsException("Index out of bounds - passed position = " + oldListPosition
                        + ", old list size = " + mOldToNew.length);
            }
            return mOldToNew[oldListPosition];
        }

        public int convertNewPositionToOld(int newListPosition) {
            if (newListPosition < 0 || newListPosition >= mNewToOld.length) {
                throw new IndexOutOfBoundsException("Index out of bounds - passed position = " + newListPosition
                        + ", new list size = " + mNewToOld.length);
            }
            return mNewToOld[newListPosition];
        }

        public void dispatchUpdatesTo(RecyclerView.Adapter adapter) {
            dispatchUpdatesTo(new AdapterListUpdateCallback(adapter));
        }

        /// Removals first (from the end, so earlier positions stay put);
        /// then each moved item, in its new order, once, to just after the
        /// item it follows in the new list; then the insertions in order;
        /// then the content changes at their final positions.
        public void dispatchUpdatesTo(ListUpdateCallback updateCallback) {
            BatchingListUpdateCallback batching = updateCallback instanceof BatchingListUpdateCallback
                    ? (BatchingListUpdateCallback) updateCallback : new BatchingListUpdateCallback(updateCallback);
            int oldSize = mOldToNew.length;
            int newSize = mNewToOld.length;
            ArrayList<Integer> current = new ArrayList<Integer>(oldSize);
            for (int i = 0; i < oldSize; i++) {
                current.add(Integer.valueOf(i));
            }
            for (int i = oldSize - 1; i >= 0; i--) {
                if (mOldToNew[i] == NO_POSITION) {
                    batching.onRemoved(i, 1);
                    current.remove(i);
                }
            }
            boolean[] placed = new boolean[oldSize];
            for (int j = 0; j < newSize; j++) {
                int target = mNewToOld[j];
                if (target == NO_POSITION || !mMoved[target]) {
                    continue;
                }
                int from = current.indexOf(Integer.valueOf(target));
                current.remove(from);
                int to = 0;
                for (int k = j - 1; k >= 0; k--) {
                    int before = mNewToOld[k];
                    if (before != NO_POSITION && (!mMoved[before] || placed[before])) {
                        to = current.indexOf(Integer.valueOf(before)) + 1;
                        break;
                    }
                }
                current.add(to, Integer.valueOf(target));
                placed[target] = true;
                if (from != to) {
                    batching.onMoved(from, to);
                }
            }
            for (int j = 0; j < newSize; j++) {
                if (mNewToOld[j] == NO_POSITION) {
                    batching.onInserted(j, 1);
                    current.add(j, Integer.valueOf(-1 - j));
                }
            }
            for (int j = 0; j < newSize; j++) {
                int target = mNewToOld[j];
                if (target != NO_POSITION && !mCallback.areContentsTheSame(target, j)) {
                    batching.onChanged(j, 1, mCallback.getChangePayload(target, j));
                }
            }
            batching.dispatchLastEvent();
        }
    }
}
