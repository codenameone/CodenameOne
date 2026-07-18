package dart.core;

import dart.runtime.Funcs;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Dart's Iterable&lt;E&gt;: lazy combinators over a source iterable.
 * Implements java.lang.Iterable for zero-friction interop.
 */
public class DartIterable<E> implements Iterable<E> {

    private final Iterable<E> source;

    protected DartIterable(Iterable<E> source) {
        this.source = source;
    }

    public static <E> DartIterable<E> wrap(Iterable<E> source) {
        return source instanceof DartIterable<E> di ? di : new DartIterable<>(source);
    }

    @Override
    public Iterator<E> iterator() {
        return source.iterator();
    }

    public <R> DartIterable<R> map(Funcs.Func1<E, R> f) {
        Iterable<E> src = this;
        return new DartIterable<>(() -> new Iterator<R>() {
            private final Iterator<E> it = src.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public R next() {
                return f.call(it.next());
            }
        });
    }

    public DartIterable<E> where(Funcs.Func1<E, Boolean> test) {
        Iterable<E> src = this;
        return new DartIterable<>(() -> new Iterator<E>() {
            private final Iterator<E> it = src.iterator();
            private boolean ready;
            private E next;

            private void advance() {
                while (!ready && it.hasNext()) {
                    E candidate = it.next();
                    if (Boolean.TRUE.equals(test.call(candidate))) {
                        next = candidate;
                        ready = true;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                advance();
                return ready;
            }

            @Override
            public E next() {
                advance();
                if (!ready) {
                    throw new NoSuchElementException();
                }
                ready = false;
                E r = next;
                next = null;
                return r;
            }
        });
    }

    public DartIterable<E> take(long count) {
        Iterable<E> src = this;
        return new DartIterable<>(() -> new Iterator<E>() {
            private final Iterator<E> it = src.iterator();
            private long remaining = count;

            @Override
            public boolean hasNext() {
                return remaining > 0 && it.hasNext();
            }

            @Override
            public E next() {
                if (remaining <= 0) {
                    throw new NoSuchElementException();
                }
                remaining--;
                return it.next();
            }
        });
    }

    public DartIterable<E> skip(long count) {
        Iterable<E> src = this;
        return new DartIterable<>(() -> {
            Iterator<E> it = src.iterator();
            for (long i = 0; i < count && it.hasNext(); i++) {
                it.next();
            }
            return it;
        });
    }

    public long length() {
        long n = 0;
        for (E ignored : this) {
            n++;
        }
        return n;
    }

    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    public boolean isNotEmpty() {
        return iterator().hasNext();
    }

    public E first() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            throw new StateError("No element");
        }
        return it.next();
    }

    public E last() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            throw new StateError("No element");
        }
        E e = it.next();
        while (it.hasNext()) {
            e = it.next();
        }
        return e;
    }

    public E firstWhere(Funcs.Func1<E, Boolean> test, Funcs.Func0<E> orElse) {
        for (E e : this) {
            if (Boolean.TRUE.equals(test.call(e))) {
                return e;
            }
        }
        if (orElse != null) {
            return orElse.call();
        }
        throw new StateError("No element");
    }

    public boolean any(Funcs.Func1<E, Boolean> test) {
        for (E e : this) {
            if (Boolean.TRUE.equals(test.call(e))) {
                return true;
            }
        }
        return false;
    }

    public boolean every(Funcs.Func1<E, Boolean> test) {
        for (E e : this) {
            if (!Boolean.TRUE.equals(test.call(e))) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(Object element) {
        for (E e : this) {
            if (dart.runtime.DartRuntime.eq(e, element)) {
                return true;
            }
        }
        return false;
    }

    public void forEach(Funcs.VoidFunc1<E> action) {
        for (E e : this) {
            action.call(e);
        }
    }

    public <R> R fold(R initialValue, Funcs.Func2<R, E, R> combine) {
        R acc = initialValue;
        for (E e : this) {
            acc = combine.call(acc, e);
        }
        return acc;
    }

    public String join(String separator) {
        StringBuilder sb = new StringBuilder();
        boolean firstItem = true;
        for (E e : this) {
            if (!firstItem) {
                sb.append(separator);
            }
            sb.append(dart.runtime.DartRuntime.str(e));
            firstItem = false;
        }
        return sb.toString();
    }

    public String join() {
        return join("");
    }

    public DartList<E> toList() {
        DartList<E> l = new DartList<>();
        for (E e : this) {
            l.add(e);
        }
        return l;
    }

    public DartSet<E> toSet() {
        DartSet<E> s = new DartSet<>();
        for (E e : this) {
            s.add(e);
        }
        return s;
    }

    @Override
    public String toString() {
        return "(" + join(", ") + ")";
    }
}
