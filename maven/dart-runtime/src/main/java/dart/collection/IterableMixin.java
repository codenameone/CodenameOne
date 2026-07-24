package dart.collection;

import dart.runtime.Funcs;

/**
 * Dart's {@code dart:collection} {@code IterableMixin<E>}. A transpiled class
 * such as {@code class Board with IterableMixin<BoardPoint>} is emitted as a
 * Java class that {@code implements IterableMixin<BoardPoint>} and supplies the
 * single abstract member {@link #iterator()}; every other member of the
 * Iterable protocol is reached here as an inherited default that iterates via
 * the returned {@link Iterator}.
 *
 * @param <E> the element type
 */
public interface IterableMixin<E> {

    /** The applying class supplies this — the source of every default below. */
    Iterator<E> iterator();

    /** Dart's {@code Iterable.length}. */
    default long length() {
        long count = 0;
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            count++;
        }
        return count;
    }

    /** Dart's {@code Iterable.isEmpty}. */
    default boolean isEmpty() {
        return !iterator().moveNext();
    }

    /** Dart's {@code Iterable.isNotEmpty}. */
    default boolean isNotEmpty() {
        return iterator().moveNext();
    }

    /** Dart's {@code Iterable.first}. */
    default E first() {
        Iterator<E> it = iterator();
        if (!it.moveNext()) {
            throw new java.util.NoSuchElementException("No element");
        }
        return it.current();
    }

    /** Dart's {@code Iterable.last}. */
    default E last() {
        Iterator<E> it = iterator();
        if (!it.moveNext()) {
            throw new java.util.NoSuchElementException("No element");
        }
        E result;
        do {
            result = it.current();
        } while (it.moveNext());
        return result;
    }

    /** Dart's {@code Iterable.single}. */
    default E single() {
        Iterator<E> it = iterator();
        if (!it.moveNext()) {
            throw new java.util.NoSuchElementException("No element");
        }
        E result = it.current();
        if (it.moveNext()) {
            throw new IllegalStateException("Too many elements");
        }
        return result;
    }

    /** Dart's {@code Iterable.contains(element)}. */
    default boolean contains(Object element) {
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            E e = it.current();
            if (e == null ? element == null : e.equals(element)) {
                return true;
            }
        }
        return false;
    }

    /** Dart's {@code Iterable.forEach(action)}. */
    default void forEach(Funcs.VoidFunc1<E> action) {
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            action.call(it.current());
        }
    }

    /** Dart's {@code Iterable.elementAt(index)}. */
    default E elementAt(long index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        Iterator<E> it = iterator();
        long i = 0;
        while (it.moveNext()) {
            if (i == index) {
                return it.current();
            }
            i++;
        }
        throw new IndexOutOfBoundsException("index: " + index + " (length " + i + ")");
    }

    /** Dart's {@code Iterable.any(test)}. */
    default boolean any(Funcs.Func1<E, Boolean> test) {
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            if (Boolean.TRUE.equals(test.call(it.current()))) {
                return true;
            }
        }
        return false;
    }

    /** Dart's {@code Iterable.every(test)}. */
    default boolean every(Funcs.Func1<E, Boolean> test) {
        Iterator<E> it = iterator();
        while (it.moveNext()) {
            if (!Boolean.TRUE.equals(test.call(it.current()))) {
                return false;
            }
        }
        return true;
    }

    /** Dart's {@code Iterable.join([separator])}. */
    default String join(String separator) {
        StringBuilder sb = new StringBuilder();
        Iterator<E> it = iterator();
        boolean firstElement = true;
        while (it.moveNext()) {
            if (!firstElement && separator != null) {
                sb.append(separator);
            }
            E e = it.current();
            sb.append(e == null ? "null" : e.toString());
            firstElement = false;
        }
        return sb.toString();
    }

    /** Dart's {@code Iterable.join()} with no separator. */
    default String join() {
        return join("");
    }
}
