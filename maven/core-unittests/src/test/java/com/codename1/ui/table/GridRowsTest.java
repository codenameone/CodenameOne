package com.codename1.ui.table;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The row views under a `DataGrid` without the grid: filtering, sorting and
/// grouping a table, flattening a tree, and the selection's ranges.
class GridRowsTest {

    private static DefaultTableModel people() {
        return new DefaultTableModel(new String[]{"Name", "Team", "Score"}, new Object[][]{
                {"dana", "red", Integer.valueOf(7)},
                {"Ann", "blue", Integer.valueOf(3)},
                {"carl", "red", Integer.valueOf(3)},
                {"Bea", "blue", Integer.valueOf(9)},
                {"eli", null, Integer.valueOf(1)},
        }, true);
    }

    private static int[] modelRows(GridRows r) {
        int[] out = new int[r.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = r.modelRow(i);
        }
        return out;
    }

    private static FlatRows flat(TableModel m) {
        FlatRows r = new FlatRows(m);
        GridColumn[] cols = new GridColumn[m.getColumnCount()];
        for (int i = 0; i < cols.length; i++) {
            cols[i] = new GridColumn(i, m.getColumnName(i));
        }
        r.setColumns(cols);
        return r;
    }

    @Test
    void sortsByTextIgnoringCaseAndKeepsTiesInModelOrder() {
        FlatRows r = flat(people());
        GridColumn name = new GridColumn(0, "Name");
        r.setSort(new GridColumn[]{name}, new boolean[]{true});
        r.rebuild();
        assertArrayEquals(new int[]{1, 3, 2, 0, 4}, modelRows(r));

        // By score descending: the two 3s keep their model order.
        GridColumn score = new GridColumn(2, "Score");
        r.setSort(new GridColumn[]{score}, new boolean[]{false});
        r.rebuild();
        assertArrayEquals(new int[]{3, 0, 1, 2, 4}, modelRows(r));
    }

    @Test
    void aSecondSortKeyBreaksTheTiesOfTheFirst() {
        FlatRows r = flat(people());
        GridColumn score = new GridColumn(2, "Score");
        GridColumn name = new GridColumn(0, "Name");
        r.setSort(new GridColumn[]{score, name}, new boolean[]{true, false});
        r.rebuild();
        // 1, then the 3s by name descending (carl before Ann), 7, 9
        assertArrayEquals(new int[]{4, 2, 1, 0, 3}, modelRows(r));
    }

    @Test
    void numbersSortByValueNotByText() {
        DefaultTableModel m = new DefaultTableModel(new String[]{"n"}, new Object[][]{
                {Integer.valueOf(10)}, {Double.valueOf(9.5)}, {Long.valueOf(100)}, {null}, {Integer.valueOf(-2)}});
        FlatRows r = flat(m);
        r.setSort(new GridColumn[]{new GridColumn(0, "n")}, new boolean[]{true});
        r.rebuild();
        assertArrayEquals(new int[]{3, 4, 1, 0, 2}, modelRows(r));
    }

    @Test
    void aFilterHidesRowsAndFindAnswersMinusOneForThem() {
        FlatRows r = flat(people());
        r.filter = new GridFilter() {
            @Override
            public boolean accept(GridRow row) {
                return "red".equals(row.getValue(1));
            }
        };
        r.rebuild();
        assertArrayEquals(new int[]{0, 2}, modelRows(r));
        assertEquals(1, r.find(Integer.valueOf(2)));
        assertEquals(-1, r.find(Integer.valueOf(1)));
    }

    @Test
    void groupsFollowTheirKeyAndShowAggregates() {
        FlatRows r = flat(people());
        GridColumn team = new GridColumn(1, "Team");
        GridColumn score = new GridColumn(2, "Score").setAggregate(GridAggregate.SUM);
        r.setColumns(new GridColumn[]{new GridColumn(0, "Name"), team, score});
        r.setGroupColumn(team);
        r.rebuild();
        // null, blue, red -- each group row followed by its rows
        assertEquals(8, r.size());
        assertTrue(r.isGroup(0));
        assertEquals(1, r.groupCount(0));
        assertEquals(4, r.modelRow(1));
        assertTrue(r.isGroup(2));
        assertEquals("blue", r.groupLabel(2));
        assertEquals(Long.valueOf(12), r.value(2, 2));
        assertEquals("blue", r.value(2, 1));
        assertTrue(r.isGroup(5));
        assertEquals(Long.valueOf(10), r.value(5, 2));
        assertEquals(1, r.depth(6));
        assertEquals(0, r.depth(5));

        assertEquals(-2, r.setExpanded(2, false));
        assertEquals(6, r.size());
        assertFalse(r.isExpanded(2));
        assertTrue(r.isGroup(3), "red follows the closed blue group directly");
        assertEquals(-1, r.find(Integer.valueOf(1)), "a row of a closed group is not shown");
        assertEquals(2, r.find(new FlatRows.GroupKey("blue")));

        // Closed stays closed across a rebuild.
        r.rebuild();
        assertEquals(6, r.size());
        assertEquals(2, r.setExpanded(2, true));
    }

    @Test
    void aggregatesCountAverageMinAndMax() {
        GridColumn c = new GridColumn(0, "x");
        Object acc = null;
        Object min = null;
        Object max = null;
        Object avg = null;
        Object count = null;
        Object[] values = {Integer.valueOf(4), null, Integer.valueOf(1), Integer.valueOf(7)};
        for (int i = 0; i < values.length; i++) {
            acc = GridAggregate.SUM.fold(c, acc, values[i]);
            min = GridAggregate.MIN.fold(c, min, values[i]);
            max = GridAggregate.MAX.fold(c, max, values[i]);
            avg = GridAggregate.AVERAGE.fold(c, avg, values[i]);
            count = GridAggregate.COUNT.fold(c, count, values[i]);
        }
        assertEquals(Long.valueOf(12), GridAggregate.SUM.finish(acc));
        assertEquals(Integer.valueOf(1), GridAggregate.MIN.finish(min));
        assertEquals(Integer.valueOf(7), GridAggregate.MAX.finish(max));
        assertEquals(Double.valueOf(4), GridAggregate.AVERAGE.finish(avg));
        assertEquals(Integer.valueOf(3), GridAggregate.COUNT.finish(count));
    }

    /// A million rows sort and filter as arrays of row numbers, and each
    /// value is read once per sort, not once per comparison.
    @Test
    void aMillionRowsSortReadingEachValueOnce() {
        final int n = 1000000;
        final int[] reads = new int[1];
        AbstractTableModel m = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return n;
            }

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public String getColumnName(int i) {
                return "v";
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Object getValueAt(int row, int column) {
                reads[0]++;
                // a scrambled permutation of 0..n-1
                return Integer.valueOf((int) ((row * 7919L) % n));
            }

            @Override
            public void setValueAt(int row, int column, Object o) {
            }

            @Override
            public void addDataChangeListener(com.codename1.ui.events.DataChangedListener d) {
            }

            @Override
            public void removeDataChangeListener(com.codename1.ui.events.DataChangedListener d) {
            }
        };
        FlatRows r = flat(m);
        r.setSort(new GridColumn[]{new GridColumn(0, "v")}, new boolean[]{true});
        r.rebuild();
        assertEquals(n, r.size());
        assertEquals(n, reads[0], "every value was read exactly once");
        for (int i = 0; i < 1000; i++) {
            assertEquals(Integer.valueOf(i), m.getValueAt(r.modelRow(i), 0));
        }
        assertEquals(Integer.valueOf(n - 1), m.getValueAt(r.modelRow(n - 1), 0));
    }

    // ------------------------------------------------------------ trees

    /// A tree of paths: "a" has "a/1" and "a/2", "a/1" has "a/1/x"; "b" is a
    /// leaf. Counts how often each node's children were asked for.
    static final class PathTree implements TreeTableModel {
        final HashMap<Object, List<String>> kids = new HashMap<Object, List<String>>();
        final HashMap<Object, Integer> asked = new HashMap<Object, Integer>();
        final HashMap<String, Object> size = new HashMap<String, Object>();

        PathTree() {
            put(null, "a", "b", "c");
            put("a", "a/2", "a/1");
            put("a/1", "a/1/x");
            put("c", "c/1");
            size.put("a", Integer.valueOf(5));
            size.put("b", Integer.valueOf(1));
            size.put("c", Integer.valueOf(3));
            size.put("a/1", Integer.valueOf(4));
            size.put("a/2", Integer.valueOf(9));
            size.put("a/1/x", Integer.valueOf(2));
            size.put("c/1", Integer.valueOf(8));
        }

        void put(Object parent, String... children) {
            kids.put(parent, new ArrayList<String>(Arrays.asList(children)));
        }

        @Override
        public Vector getChildren(Object parent) {
            Integer n = asked.get(parent);
            asked.put(parent, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
            List<String> k = kids.get(parent);
            return k == null ? new Vector() : new Vector(k);
        }

        @Override
        public boolean isLeaf(Object node) {
            return !kids.containsKey(node);
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Path" : "Size";
        }

        @Override
        public Object getValueAt(Object node, int column) {
            return column == 0 ? node : size.get(node);
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return column == 1;
        }

        @Override
        public void setValueAt(Object node, int column, Object value) {
            size.put((String) node, value);
        }

        int askedFor(Object parent) {
            Integer n = asked.get(parent);
            return n == null ? 0 : n.intValue();
        }
    }

    private static List<Object> nodes(TreeRows r) {
        List<Object> out = new ArrayList<Object>();
        for (int i = 0; i < r.size(); i++) {
            out.add(r.node(i));
        }
        return out;
    }

    @Test
    void aTreeLoadsChildrenOnlyWhenANodeIsOpened() {
        PathTree m = new PathTree();
        TreeRows r = new TreeRows(m);
        r.rebuild();
        assertEquals(Arrays.<Object>asList("a", "b", "c"), nodes(r));
        assertEquals(0, m.askedFor("a"));
        assertTrue(r.isExpandable(0));
        assertFalse(r.isExpandable(1));

        assertEquals(2, r.setExpanded(0, true));
        assertEquals(Arrays.<Object>asList("a", "a/2", "a/1", "b", "c"), nodes(r));
        assertEquals(1, r.depth(1));
        assertEquals(1, m.askedFor("a"));

        assertEquals(1, r.setExpanded(2, true));
        assertEquals(Arrays.<Object>asList("a", "a/2", "a/1", "a/1/x", "b", "c"), nodes(r));
        assertEquals(2, r.depth(3));

        // Closing removes the whole open subtree, and opening again asks
        // nothing more of the model.
        assertEquals(-3, r.setExpanded(0, false));
        assertEquals(Arrays.<Object>asList("a", "b", "c"), nodes(r));
        assertEquals(3, r.setExpanded(0, true));
        assertEquals(1, m.askedFor("a"));
        assertEquals(1, m.askedFor("a/1"));
        assertEquals(3, r.find("a/1/x"));
    }

    @Test
    void sortingATreeOrdersSiblingsOnly() {
        PathTree m = new PathTree();
        TreeRows r = new TreeRows(m);
        r.setSort(new GridColumn[]{new GridColumn(1, "Size")}, new boolean[]{true});
        r.rebuild();
        r.setExpanded(r.find("a"), true);
        // b(1) c(3) a(5), and under a: a/1(4) a/2(9)
        assertEquals(Arrays.<Object>asList("b", "c", "a", "a/1", "a/2"), nodes(r));
    }

    @Test
    void aFilterKeepsThePathToEveryMatchOpen() {
        PathTree m = new PathTree();
        TreeRows r = new TreeRows(m);
        r.filter = new GridFilter() {
            @Override
            public boolean accept(GridRow row) {
                return "a/1/x".equals(row.getValue(0)) || "b".equals(row.getValue(0));
            }
        };
        r.rebuild();
        assertEquals(Arrays.<Object>asList("a", "a/1", "a/1/x", "b"), nodes(r));
        assertTrue(r.isExpanded(0));
        assertEquals(2, r.depth(2));

        // Without the filter the tree is as the user left it: closed.
        r.filter = null;
        r.rebuild();
        assertEquals(Arrays.<Object>asList("a", "b", "c"), nodes(r));
    }

    @Test
    void forgettingANodeAsksForItsChildrenAgain() {
        PathTree m = new PathTree();
        TreeRows r = new TreeRows(m);
        r.rebuild();
        r.setExpanded(0, true);
        m.put("a", "a/1", "a/2", "a/3");
        r.forget("a");
        assertEquals(Arrays.<Object>asList("a", "a/1", "a/2", "a/3", "b", "c"), nodes(r));
        assertEquals(2, m.askedFor("a"));
    }

    // -------------------------------------------------------- selection

    @Test
    void selectionRangesMergeSplitAndShift() {
        GridSelection s = new GridSelection();
        s.add(2, 4);
        s.add(6, 8);
        assertEquals(2, s.rangeCount());
        s.add(5, 5);
        assertEquals(1, s.rangeCount(), "adjacent ranges merge");
        assertEquals(7, s.size());
        s.remove(4, 6);
        assertArrayEquals(new int[]{2, 3, 7, 8}, s.toArray());
        s.toggle(3);
        assertArrayEquals(new int[]{2, 7, 8}, s.toArray());

        // Three rows inserted after row 2: what was after moves down.
        s.shift(3, 3);
        assertArrayEquals(new int[]{2, 10, 11}, s.toArray());
        // Two rows removed from row 9 on: row 10 goes, 11 moves up.
        s.shift(9, -2);
        assertArrayEquals(new int[]{2, 9}, s.toArray());

        s.set(0, 999999);
        assertEquals(1, s.rangeCount());
        assertTrue(s.contains(500000));
        assertFalse(s.contains(1000000));
    }

    @Test
    void insertingInsideASelectedRangeLeavesTheNewRowsUnselected() {
        GridSelection s = new GridSelection();
        s.add(0, 5);
        s.shift(3, 2);
        assertArrayEquals(new int[]{0, 1, 2, 5, 6, 7}, s.toArray());
    }
}
