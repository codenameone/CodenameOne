package java.util.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Stream<T> extends BaseStream<T, Stream<T>> {
    Stream<T> filter(Predicate<? super T> predicate);

    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    Stream<T> sorted();

    Stream<T> distinct();

    Stream<T> limit(long maxSize);

    Stream<T> skip(long n);

    void forEach(Consumer<? super T> action);

    Object[] toArray();

    T reduce(T identity, BinaryOperator<T> accumulator);

    <A, R> R collect(Collector<? super T, A, R> collector);

    long count();

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    static <T> Stream<T> empty() {
        return new StreamImpl<T>(new ArrayList<T>());
    }

    static <T> Stream<T> of(T... values) {
        List<T> out = new ArrayList<T>();
        if (values != null) {
            Collections.addAll(out, values);
        }
        return new StreamImpl<T>(out);
    }
}
