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
            // Primitive arrays — read element and box into a Primitive
            // wrapper so arithmetic on the result works without an
            // explicit cast. Falls through to the Object[] path.
            Object primitive = primitiveGet(array, index);
            if (primitive != null) return primitive;
            return Primitive.wrap(((Object[]) array)[index], Object.class);
        } catch (IndexOutOfBoundsException e) {
            int len = lengthOf(array);
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
            if (primitiveSet(array, index, value)) return;
            ((Object[]) array)[index] = value;
        } catch (IndexOutOfBoundsException e) {
            int len = lengthOf(array);
            throw new UtilTargetError("Index " + index + " out-of-bounds for length " + len, e);
        } catch (ClassCastException e) {
            throw new UtilTargetError(new ArrayStoreException(e.getMessage()));
        }
    }

    /** Read an element of a primitive array and return it as the
     * corresponding boxed wrapper. Returns {@code null} when
     * {@code array} isn't a primitive array (caller falls back to
     * the {@code Object[]} path). CN1's reduced reflection surface
     * forbids {@code Float.TYPE}/{@code Short.TYPE} etc., so we
     * box directly rather than calling {@link Primitive#wrap} with a
     * primitive-type {@code Class} literal. */
    private static Object primitiveGet(Object array, int index) {
        if (array instanceof int[]) return Integer.valueOf(((int[]) array)[index]);
        if (array instanceof long[]) return Long.valueOf(((long[]) array)[index]);
        if (array instanceof double[]) return Double.valueOf(((double[]) array)[index]);
        if (array instanceof float[]) return Float.valueOf(((float[]) array)[index]);
        if (array instanceof short[]) return Short.valueOf(((short[]) array)[index]);
        if (array instanceof byte[]) return Byte.valueOf(((byte[]) array)[index]);
        if (array instanceof char[]) return Character.valueOf(((char[]) array)[index]);
        if (array instanceof boolean[]) return Boolean.valueOf(((boolean[]) array)[index]);
        return null;
    }

    /** Store {@code value} into a primitive array, coercing Number /
     * Character / Boolean wrappers as needed. Returns true when
     * {@code array} was a primitive array and the store happened. */
    private static boolean primitiveSet(Object array, int index, Object value) {
        if (array instanceof int[]) { ((int[]) array)[index] = ((Number) value).intValue(); return true; }
        if (array instanceof long[]) { ((long[]) array)[index] = ((Number) value).longValue(); return true; }
        if (array instanceof double[]) { ((double[]) array)[index] = ((Number) value).doubleValue(); return true; }
        if (array instanceof float[]) { ((float[]) array)[index] = ((Number) value).floatValue(); return true; }
        if (array instanceof short[]) { ((short[]) array)[index] = ((Number) value).shortValue(); return true; }
        if (array instanceof byte[]) { ((byte[]) array)[index] = ((Number) value).byteValue(); return true; }
        if (array instanceof char[]) { ((char[]) array)[index] = ((Character) value).charValue(); return true; }
        if (array instanceof boolean[]) { ((boolean[]) array)[index] = ((Boolean) value).booleanValue(); return true; }
        return false;
    }

    /** Length of any supported array/List type, including primitive
     * arrays. Exposed so callers that used to hard-cast
     * {@code (Object[]) obj} can be primitive-array-safe. */
    public static int arrayLength(Object array) {
        return lengthOf(array);
    }

    private static int lengthOf(Object array) {
        if (array instanceof List) return ((List<?>) array).size();
        if (array instanceof Object[]) return ((Object[]) array).length;
        if (array instanceof int[]) return ((int[]) array).length;
        if (array instanceof long[]) return ((long[]) array).length;
        if (array instanceof double[]) return ((double[]) array).length;
        if (array instanceof float[]) return ((float[]) array).length;
        if (array instanceof short[]) return ((short[]) array).length;
        if (array instanceof byte[]) return ((byte[]) array).length;
        if (array instanceof char[]) return ((char[]) array).length;
        if (array instanceof boolean[]) return ((boolean[]) array).length;
        return 0;
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
        // Box primitive arrays so legacy Object[]-only helpers still work.
        // CN1's reduced reflection surface forbids java.lang.reflect.Array,
        // so dispatch by instanceof.
        if (value instanceof int[]) return boxIntArray((int[]) value);
        if (value instanceof long[]) return boxLongArray((long[]) value);
        if (value instanceof double[]) return boxDoubleArray((double[]) value);
        if (value instanceof float[]) return boxFloatArray((float[]) value);
        if (value instanceof short[]) return boxShortArray((short[]) value);
        if (value instanceof byte[]) return boxByteArray((byte[]) value);
        if (value instanceof char[]) return boxCharArray((char[]) value);
        if (value instanceof boolean[]) return boxBooleanArray((boolean[]) value);
        throw new IllegalArgumentException("Only Object[] arrays are supported in the reduced CN1 runtime.");
    }

    private static Object[] boxIntArray(int[] a) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Integer.valueOf(a[i]);
        return out;
    }

    private static Object[] boxLongArray(long[] a) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Long.valueOf(a[i]);
        return out;
    }

    private static Object[] boxDoubleArray(double[] a) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Double.valueOf(a[i]);
        return out;
    }

    private static Object[] boxFloatArray(float[] a) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Float.valueOf(a[i]);
        return out;
    }

    private static Object[] boxShortArray(short[] a) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Short.valueOf(a[i]);
        return out;
    }

    private static Object[] boxByteArray(byte[] a) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Byte.valueOf(a[i]);
        return out;
    }

    private static Object[] boxCharArray(char[] a) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Character.valueOf(a[i]);
        return out;
    }

    private static Object[] boxBooleanArray(boolean[] a) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Boolean.valueOf(a[i]);
        return out;
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
