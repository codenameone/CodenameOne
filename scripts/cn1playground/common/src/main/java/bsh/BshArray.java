package bsh;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Map.Entry;

import static bsh.Types.MapEntry;

/** Reduced array helpers for the CN1 BeanShell runtime. */
public final class BshArray {
    private BshArray() {}

    public static Object getIndex(Object array, int index) throws UtilTargetError {
        try {
            if (array instanceof List) {
                return ((List<?>) array).get(index);
            }
            return Primitive.wrap(asObjectArray(array)[index], Object.class);
        } catch (IndexOutOfBoundsException e) {
            int len = array instanceof List ? ((List<?>) array).size() : asObjectArray(array).length;
            throw new UtilTargetError("Index " + index + " out-of-bounds for length " + len, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void setIndex(Object array, int index, Object val) throws ReflectError, UtilTargetError {
        try {
            Object value = Primitive.unwrap(val);
            if (array instanceof List) {
                ((List<Object>) array).set(index, value);
                return;
            }
            asObjectArray(array)[index] = value;
        } catch (IndexOutOfBoundsException e) {
            int len = array instanceof List ? ((List<?>) array).size() : asObjectArray(array).length;
            throw new UtilTargetError("Index " + index + " out-of-bounds for length " + len, e);
        } catch (ClassCastException e) {
            throw new UtilTargetError(new ArrayStoreException(e.getMessage()));
        }
    }

    public static Object slice(List<Object> list, int from, int to, int step) {
        int length = list.size();
        if (to > length) to = length;
        if (from < 0) from = 0;
        length = to - from;
        if (length <= 0) return list.subList(0, 0);
        if (step == 0 || step == 1) return list.subList(from, to);
        List<Integer> slices = new ArrayList<Integer>();
        for (int i = 0; i < length; i++) {
            if (i % step == 0) {
                slices.add(step < 0 ? length - 1 - i : i + from);
            }
        }
        return new SteppedSubList(list, slices);
    }

    public static Object slice(Object arr, int from, int to, int step) {
        Object[] source = asObjectArray(arr);
        if (to > source.length) to = source.length;
        if (from < 0) from = 0;
        int length = to - from;
        if (length <= 0) return new Object[0];
        if (step == 0 || step == 1) return Arrays.copyOfRange(source, from, to);
        ArrayList<Object> out = new ArrayList<Object>();
        for (int i = 0; i < length; i++) {
            if (i % step == 0) {
                out.add(source[step < 0 ? length - 1 - i : i + from]);
            }
        }
        return out.toArray(new Object[out.size()]);
    }

    public static Object repeat(List<Object> list, int times) {
        if (times < 1) {
            return list instanceof Queue ? new LinkedList<Object>() : new ArrayList<Object>(0);
        }
        List<Object> out = list instanceof Queue ? new LinkedList<Object>(list) : new ArrayList<Object>(list);
        while (times-- > 1) {
            out.addAll(list);
        }
        return out;
    }

    public static Object repeat(Object arr, int times) {
        Object[] source = asObjectArray(arr);
        if (times < 1) return new Object[0];
        Object[] out = new Object[source.length * times];
        int offset = 0;
        for (int i = 0; i < times; i++) {
            System.arraycopy(source, 0, out, offset, source.length);
            offset += source.length;
        }
        return out;
    }

    public static Object concat(List<?> lhs, List<?> rhs) {
        List<Object> out = lhs instanceof Queue ? new LinkedList<Object>(lhs) : new ArrayList<Object>(lhs);
        out.addAll(rhs);
        return out;
    }

    public static Object concat(Object lhs, Object rhs) {
        Object[] left = asObjectArray(lhs);
        Object[] right = asObjectArray(rhs);
        Object[] out = new Object[left.length + right.length];
        System.arraycopy(left, 0, out, 0, left.length);
        System.arraycopy(right, 0, out, left.length, right.length);
        return out;
    }

    public static int[] dimensions(Object arr) {
        if (!(arr instanceof Object[])) {
            return new int[0];
        }
        Object[] current = (Object[]) arr;
        ArrayList<Integer> dims = new ArrayList<Integer>();
        dims.add(current.length);
        while (current.length > 0 && current[0] instanceof Object[]) {
            current = (Object[]) current[0];
            dims.add(current.length);
        }
        int[] out = new int[dims.size()];
        for (int i = 0; i < dims.size(); i++) {
            out[i] = dims.get(i).intValue();
        }
        return out;
    }

    static Object castArray(Class<?> toType, Class<?> fromType, Object fromValue) throws UtilEvalError {
        Object[] values = asObjectArray(fromValue);
        if (Collection.class.isAssignableFrom(toType)) {
            if (List.class.isAssignableFrom(toType) || Queue.class == toType) {
                if (toType.isAssignableFrom(ArrayList.class)) return new ArrayList<Object>(Arrays.asList(values));
                if (toType.isAssignableFrom(LinkedList.class)) return new LinkedList<Object>(Arrays.asList(values));
            } else if (toType.isAssignableFrom(ArrayDeque.class)) {
                return new ArrayDeque<Object>(Arrays.asList(values));
            } else if (toType.isAssignableFrom(LinkedHashSet.class)) {
                return new LinkedHashSet<Object>(Arrays.asList(values));
            }
        }
        if (Map.class.isAssignableFrom(toType)) {
            LinkedHashMap<Object, Object> map = new LinkedHashMap<Object, Object>();
            for (int i = 0; i < values.length; i += 2) {
                map.put(values[i], i + 1 < values.length ? values[i + 1] : null);
            }
            return map;
        }
        if (Entry.class.isAssignableFrom(toType)) {
            if (values.length == 1) return new MapEntry(values[0], null);
            if (values.length == 2) return new MapEntry(values[0], values[1]);
            Entry<?, ?>[] entries = new Entry[(values.length + 1) / 2];
            int index = 0;
            for (int i = 0; i < values.length; i += 2) {
                entries[index++] = new MapEntry(values[i], i + 1 < values.length ? values[i + 1] : null);
            }
            return entries;
        }
        return Arrays.copyOf(values, values.length);
    }

    private static Object[] asObjectArray(Object value) {
        if (value instanceof Object[]) {
            return (Object[]) value;
        }
        throw new IllegalArgumentException("Only Object[] arrays are supported in the reduced CN1 runtime.");
    }

    private static class SteppedSubList extends AbstractList<Object> implements RandomAccess {
        private final List<Object> parent;
        private final List<Integer> steps;

        SteppedSubList(List<Object> parent, List<Integer> steps) {
            this.parent = parent;
            this.steps = steps;
        }

        @Override
        public Object set(int index, Object e) {
            return parent.set(steps.get(index).intValue(), e);
        }

        @Override
        public Object get(int index) {
            return parent.get(steps.get(index).intValue());
        }

        @Override
        public int size() {
            return steps.size();
        }

        @Override
        public void add(int index, Object e) {
            int idx = index == size() ? steps.get(index - 1).intValue() + 1 : steps.get(index).intValue();
            parent.add(idx, e);
            for (int i = index; i < size(); i++) {
                steps.set(i, Integer.valueOf(steps.get(i).intValue() + 1));
            }
            steps.add(index, Integer.valueOf(idx));
        }

        @Override
        public Object remove(int index) {
            int idx = steps.get(index).intValue();
            for (int i = index + 1; i < size(); i++) {
                steps.set(i, Integer.valueOf(steps.get(i).intValue() - 1));
            }
            steps.remove(index);
            return parent.remove(idx);
        }

        @Override
        public boolean addAll(Collection<? extends Object> c) {
            return addAll(steps.size(), c);
        }

        @Override
        public boolean addAll(int index, Collection<? extends Object> c) {
            int count = 0;
            for (Object value : c) {
                add(index + count++, value);
            }
            return count > 0;
        }

        @Override
        public List<Object> subList(int fromIndex, int toIndex) {
            return new SteppedSubList(parent, steps.subList(fromIndex, toIndex));
        }

        @Override
        public Iterator<Object> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<Object> listIterator(final int index) {
            final ListIterator<Integer> sliceIter = new ArrayList<Integer>(steps).listIterator(index);
            return new ListIterator<Object>() {
                int lastIndex = 0;

                @Override
                public boolean hasNext() { return sliceIter.hasNext(); }
                @Override
                public Object next() { lastIndex = sliceIter.nextIndex(); return SteppedSubList.this.get(sliceIter.nextIndex()); }
                @Override
                public boolean hasPrevious() { return sliceIter.hasPrevious(); }
                @Override
                public Object previous() { lastIndex = sliceIter.previousIndex(); return SteppedSubList.this.get(sliceIter.previousIndex()); }
                @Override
                public int nextIndex() { return sliceIter.nextIndex(); }
                @Override
                public int previousIndex() { return sliceIter.previousIndex(); }
                @Override
                public void remove() { SteppedSubList.this.remove(lastIndex); }
                @Override
                public void set(Object e) { SteppedSubList.this.set(lastIndex, e); }
                @Override
                public void add(Object e) { SteppedSubList.this.add(nextIndex(), e); }
            };
        }
    }
}
