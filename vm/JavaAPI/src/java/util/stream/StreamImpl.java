package java.util.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class StreamImpl<T> implements Stream<T> {
    private final List<T> values;

    StreamImpl(List<T> values) {
        this.values = values;
    }

    public Stream<T> filter(Predicate<? super T> predicate) {
        List<T> out = new ArrayList<T>();
        for (T value : values) {
            if (predicate.test(value)) {
                out.add(value);
            }
        }
        return new StreamImpl<T>(out);
    }

    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        List<R> out = new ArrayList<R>();
        for (T value : values) {
            out.add(mapper.apply(value));
        }
        return new StreamImpl<R>(out);
    }

    public Stream<T> sorted() {
        List<T> out = new ArrayList<T>(values);
        Collections.sort((List) out);
        return new StreamImpl<T>(out);
    }

    public Stream<T> distinct() {
        Set<T> unique = new LinkedHashSet<T>(values);
        return new StreamImpl<T>(new ArrayList<T>(unique));
    }

    public Stream<T> limit(long maxSize) {
        List<T> out = new ArrayList<T>();
        int limit = maxSize < 0 ? 0 : (int) Math.min(maxSize, values.size());
        for (int i = 0; i < limit; i++) {
            out.add(values.get(i));
        }
        return new StreamImpl<T>(out);
    }

    public Stream<T> skip(long n) {
        int from = n < 0 ? 0 : (int) Math.min(n, values.size());
        List<T> out = new ArrayList<T>();
        for (int i = from; i < values.size(); i++) {
            out.add(values.get(i));
        }
        return new StreamImpl<T>(out);
    }

    public void forEach(Consumer<? super T> action) {
        for (T value : values) {
            action.accept(value);
        }
    }

    public Object[] toArray() {
        return values.toArray();
    }

    public T reduce(T identity, BinaryOperator<T> accumulator) {
        T result = identity;
        for (T value : values) {
            result = accumulator.apply(result, value);
        }
        return result;
    }

    public <A, R> R collect(Collector<? super T, A, R> collector) {
        A container = collector.supplier().get();
        for (T value : values) {
            collector.accumulator().accept(container, value);
        }
        return collector.finisher().apply(container);
    }

    public long count() {
        return values.size();
    }

    public boolean anyMatch(Predicate<? super T> predicate) {
        for (T value : values) {
            if (predicate.test(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean allMatch(Predicate<? super T> predicate) {
        for (T value : values) {
            if (!predicate.test(value)) {
                return false;
            }
        }
        return true;
    }

    public boolean noneMatch(Predicate<? super T> predicate) {
        return !anyMatch(predicate);
    }

    public Iterator<T> iterator() {
        return values.iterator();
    }

    public Stream<T> sequential() {
        return this;
    }

    public Stream<T> parallel() {
        return this;
    }

    public void close() {
    }
}
