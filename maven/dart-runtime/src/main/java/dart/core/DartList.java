package dart.core;

import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.RandomAccess;

/**
 * Dart's List&lt;E&gt;: growable, insertion-ordered. Extends
 * java.util.AbstractList so it interoperates with any Java/CN1 API for free,
 * and adds the Dart API surface the transpiler targets.
 *
 * <p>Indexes in the Dart API arrive as {@code long} (Dart int); they are
 * range-checked with Dart's RangeError semantics.</p>
 */
public class DartList<E> extends AbstractList<E> implements RandomAccess {

    private final ArrayList<E> impl;
    private final boolean growable;

    public DartList() {
        this.impl = new ArrayList<>();
        this.growable = true;
    }

    private DartList(ArrayList<E> impl, boolean growable) {
        this.impl = impl;
        this.growable = growable;
    }

    /** Literal helper: DartList.of(a, b, c) for Dart's [a, b, c]. */
    @SafeVarargs
    public static <E> DartList<E> of(E... elements) {
        DartList<E> l = new DartList<>();
        for (E e : elements) {
            l.impl.add(e);
        }
        return l;
    }

    public static <E> DartList<E> from(Iterable<E> elements) {
        DartList<E> l = new DartList<>();
        for (E e : elements) {
            l.impl.add(e);
        }
        return l;
    }

    /** Dart's List.filled(length, fill). */
    public static <E> DartList<E> filled(long length, E fill, boolean growable) {
        ArrayList<E> impl = new ArrayList<>();
        for (long i = 0; i < length; i++) {
            impl.add(fill);
        }
        return new DartList<>(impl, growable);
    }

    public static <E> DartList<E> filled(long length, E fill) {
        return filled(length, fill, false);
    }

    /** Dart's List.generate(length, generator). */
    public static <E> DartList<E> generate(long length, Funcs.Func1<Long, E> generator, boolean growable) {
        ArrayList<E> impl = new ArrayList<>();
        for (long i = 0; i < length; i++) {
            impl.add(generator.call(i));
        }
        return new DartList<>(impl, growable);
    }

    public static <E> DartList<E> generate(long length, Funcs.Func1<Long, E> generator) {
        return generate(length, generator, true);
    }

    private void checkGrowable(String op) {
        if (!growable) {
            throw new UnsupportedError(op + " on a fixed-length list");
        }
    }

    // ------------------------------------------------------------------
    // java.util.List plumbing
    // ------------------------------------------------------------------

    @Override
    public E get(int index) {
        RangeError.checkValidIndex(index, impl.size());
        return impl.get(index);
    }

    @Override
    public E set(int index, E element) {
        RangeError.checkValidIndex(index, impl.size());
        return impl.set(index, element);
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public boolean add(E e) {
        checkGrowable("add");
        return impl.add(e);
    }

    @Override
    public void add(int index, E element) {
        checkGrowable("insert");
        impl.add(index, element);
    }

    @Override
    public E remove(int index) {
        checkGrowable("removeAt");
        RangeError.checkValidIndex(index, impl.size());
        return impl.remove(index);
    }

    // ------------------------------------------------------------------
    // Dart API (long-indexed)
    // ------------------------------------------------------------------

    /** Dart's list[i]. */
    public E idx(long index) {
        RangeError.checkValidIndex(index, impl.size());
        return impl.get((int) index);
    }

    /** Dart's list[i] = v. */
    public E idxSet(long index, E value) {
        RangeError.checkValidIndex(index, impl.size());
        impl.set((int) index, value);
        return value;
    }

    public long length() {
        return impl.size();
    }

    public boolean isNotEmpty() {
        return !impl.isEmpty();
    }

    public E first() {
        if (impl.isEmpty()) {
            throw new StateError("No element");
        }
        return impl.get(0);
    }

    public E last() {
        if (impl.isEmpty()) {
            throw new StateError("No element");
        }
        return impl.get(impl.size() - 1);
    }

    public void insert(long index, E element) {
        checkGrowable("insert");
        RangeError.checkValueInInterval(index, 0, impl.size(), "index");
        impl.add((int) index, element);
    }

    public E removeAt(long index) {
        checkGrowable("removeAt");
        RangeError.checkValidIndex(index, impl.size());
        return impl.remove((int) index);
    }

    public E removeLast() {
        checkGrowable("removeLast");
        if (impl.isEmpty()) {
            throw new RangeError("RangeError (index): Invalid value: Valid value range is empty: -1");
        }
        return impl.remove(impl.size() - 1);
    }

    /** Dart's List.remove(Object) — removes first match, returns whether found. */
    public boolean removeValue(Object value) {
        checkGrowable("remove");
        for (int i = 0; i < impl.size(); i++) {
            if (DartRuntime.eq(impl.get(i), value)) {
                impl.remove(i);
                return true;
            }
        }
        return false;
    }

    /** Dart's List.addAll — named distinctly because java.util.List.addAll(Collection) makes the overload ambiguous. */
    public void addAllIterable(Iterable<? extends E> elements) {
        checkGrowable("addAll");
        for (E e : elements) {
            impl.add(e);
        }
    }

    /** Dart's List.indexOf — long-typed; named to avoid clashing with java.util.List.indexOf(Object). */
    public long indexOfDart(E element) {
        for (int i = 0; i < impl.size(); i++) {
            if (DartRuntime.eq(impl.get(i), element)) {
                return i;
            }
        }
        return -1;
    }

    public DartList<E> sublist(long start, long end) {
        RangeError.checkValueInInterval(start, 0, impl.size(), "start");
        RangeError.checkValueInInterval(end, start, impl.size(), "end");
        DartList<E> l = new DartList<>();
        for (long i = start; i < end; i++) {
            l.impl.add(impl.get((int) i));
        }
        return l;
    }

    public DartList<E> sublist(long start) {
        return sublist(start, impl.size());
    }

    public void sort(Funcs.Func2<E, E, Long> compare) {
        if (compare == null) {
            impl.sort(null);
        } else {
            impl.sort((a, b) -> {
                long r = compare.call(a, b);
                return r < 0 ? -1 : (r > 0 ? 1 : 0);
            });
        }
    }

    public void sortDefault() {
        impl.sort(null);
    }

    public DartIterable<E> reversed() {
        DartList<E> self = this;
        return DartIterable.wrap(() -> new java.util.Iterator<E>() {
            private int i = self.impl.size() - 1;

            @Override
            public boolean hasNext() {
                return i >= 0;
            }

            @Override
            public E next() {
                return self.impl.get(i--);
            }
        });
    }

    // Dart iterable combinators, delegating to a lazy view.

    public DartIterable<E> asIterable() {
        return DartIterable.wrap(this);
    }

    public <R> DartIterable<R> map(Funcs.Func1<E, R> f) {
        return asIterable().map(f);
    }

    public DartIterable<E> where(Funcs.Func1<E, Boolean> test) {
        return asIterable().where(test);
    }

    public E firstWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        return asIterable().firstWhere(test, orElse);
    }

    public boolean any(Funcs.Func1<E, Boolean> test) {
        return asIterable().any(test);
    }

    public boolean every(Funcs.Func1<E, Boolean> test) {
        return asIterable().every(test);
    }

    public <R> R fold(R initialValue, Funcs.Func2<R, E, R> combine) {
        return asIterable().fold(initialValue, combine);
    }

    public String join(String separator) {
        return asIterable().join(separator);
    }

    public String join() {
        return join("");
    }

    public void forEachDart(Funcs.VoidFunc1<E> action) {
        // Named forEachDart because AbstractList inherits Java's forEach(Consumer).
        for (E e : impl) {
            action.call(e);
        }
    }

    public DartList<E> toList() {
        return DartList.from(impl);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < impl.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(DartRuntime.str(impl.get(i)));
        }
        return sb.append("]").toString();
    }
}
