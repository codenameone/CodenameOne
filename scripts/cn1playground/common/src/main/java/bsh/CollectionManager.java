package bsh;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Reduced CN1-safe collection manager. */
public final class CollectionManager {
    private static final CollectionManager manager = new CollectionManager();

    public static synchronized CollectionManager getCollectionManager() {
        return manager;
    }

    private <T> Iterator<T> emptyIt() {
        return new Iterator<T>() {
            public boolean hasNext() {
                return false;
            }

            public T next() {
                throw new NoSuchElementException();
            }
        };
    }

    private Iterator<Object> objectArrayIt(final Object[] array) {
        return new Iterator<Object>() {
            private int index;

            public boolean hasNext() {
                return index < array.length;
            }

            public Object next() {
                return array[index++];
            }
        };
    }

    private Iterator<Object> primitiveIntArrayIt(final int[] array) {
        return new Iterator<Object>() {
            private int index;
            public boolean hasNext() { return index < array.length; }
            public Object next() { return Integer.valueOf(array[index++]); }
        };
    }

    private Iterator<Object> primitiveLongArrayIt(final long[] array) {
        return new Iterator<Object>() {
            private int index;
            public boolean hasNext() { return index < array.length; }
            public Object next() { return Long.valueOf(array[index++]); }
        };
    }

    private Iterator<Object> primitiveDoubleArrayIt(final double[] array) {
        return new Iterator<Object>() {
            private int index;
            public boolean hasNext() { return index < array.length; }
            public Object next() { return Double.valueOf(array[index++]); }
        };
    }

    private Iterator<Object> primitiveFloatArrayIt(final float[] array) {
        return new Iterator<Object>() {
            private int index;
            public boolean hasNext() { return index < array.length; }
            public Object next() { return Float.valueOf(array[index++]); }
        };
    }

    private Iterator<Object> primitiveShortArrayIt(final short[] array) {
        return new Iterator<Object>() {
            private int index;
            public boolean hasNext() { return index < array.length; }
            public Object next() { return Short.valueOf(array[index++]); }
        };
    }

    private Iterator<Object> primitiveByteArrayIt(final byte[] array) {
        return new Iterator<Object>() {
            private int index;
            public boolean hasNext() { return index < array.length; }
            public Object next() { return Byte.valueOf(array[index++]); }
        };
    }

    private Iterator<Object> primitiveCharArrayIt(final char[] array) {
        return new Iterator<Object>() {
            private int index;
            public boolean hasNext() { return index < array.length; }
            public Object next() { return Character.valueOf(array[index++]); }
        };
    }

    private Iterator<Object> primitiveBooleanArrayIt(final boolean[] array) {
        return new Iterator<Object>() {
            private int index;
            public boolean hasNext() { return index < array.length; }
            public Object next() { return Boolean.valueOf(array[index++]); }
        };
    }

    public <T> Iterator<T> getBshIterator(final Enumeration<T> obj) {
        return Collections.list(obj).iterator();
    }

    public <T> Iterator<T> getBshIterator(final Iterable<T> obj) {
        return obj.iterator();
    }

    public <T> Iterator<T> getBshIterator(final Iterator<T> obj) {
        return obj;
    }

    public Iterator<Object> getBshIterator(final CharSequence obj) {
        final char[] chars = obj.toString().toCharArray();
        return new Iterator<Object>() {
            private int index;

            public boolean hasNext() {
                return index < chars.length;
            }

            public Object next() {
                return Character.valueOf(chars[index++]);
            }
        };
    }

    public Iterator<Object> getBshIterator(final String obj) {
        return getBshIterator((CharSequence) obj);
    }

    public <T> Iterator<T> getBshIterator(final T[] obj) {
        return Arrays.asList(obj).iterator();
    }

    public Iterator<Integer> getBshIterator(final Number obj) {
        final int target = obj.intValue();
        if (target == 0) {
            return emptyIt();
        }
        return new Iterator<Integer>() {
            private int current = target > 0 ? 0 : target;
            private final int end = target > 0 ? target : 0;
            private boolean first = true;

            public boolean hasNext() {
                return first || current != end;
            }

            public Integer next() {
                if (first) {
                    first = false;
                    return Integer.valueOf(current);
                }
                current = target > 0 ? current + 1 : current + 1;
                return Integer.valueOf(current);
            }
        };
    }

    public Iterator<String> getBshIterator(final Character obj) {
        return Collections.singletonList(String.valueOf(obj)).iterator();
    }

    public Iterator<?> getBshIterator(final Object obj) {
        if (obj == null) {
            return emptyIt();
        }
        if (obj instanceof Primitive) {
            return getBshIterator(Primitive.unwrap(obj));
        }
        if (obj instanceof Object[]) {
            return objectArrayIt((Object[]) obj);
        }
        // Primitive arrays — iterate and box each element. CN1's
        // reduced reflection surface forbids java.lang.reflect.Array,
        // so dispatch by instanceof on each primitive array type.
        if (obj instanceof int[]) return primitiveIntArrayIt((int[]) obj);
        if (obj instanceof long[]) return primitiveLongArrayIt((long[]) obj);
        if (obj instanceof double[]) return primitiveDoubleArrayIt((double[]) obj);
        if (obj instanceof float[]) return primitiveFloatArrayIt((float[]) obj);
        if (obj instanceof short[]) return primitiveShortArrayIt((short[]) obj);
        if (obj instanceof byte[]) return primitiveByteArrayIt((byte[]) obj);
        if (obj instanceof char[]) return primitiveCharArrayIt((char[]) obj);
        if (obj instanceof boolean[]) return primitiveBooleanArrayIt((boolean[]) obj);
        if (obj instanceof Iterable) {
            return getBshIterator((Iterable<?>) obj);
        }
        if (obj instanceof Iterator) {
            return getBshIterator((Iterator<?>) obj);
        }
        if (obj instanceof Enumeration) {
            return getBshIterator((Enumeration<?>) obj);
        }
        if (obj instanceof CharSequence) {
            return getBshIterator((CharSequence) obj);
        }
        if (obj instanceof Number) {
            return getBshIterator((Number) obj);
        }
        if (obj instanceof Character) {
            return getBshIterator((Character) obj);
        }
        if (obj instanceof String) {
            return getBshIterator((String) obj);
        }
        return Collections.singletonList(StringUtil.valueString(obj)).iterator();
    }
}
