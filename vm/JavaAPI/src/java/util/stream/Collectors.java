package java.util.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Collectors {
    private Collectors() {
    }

    public static <T> Collector<T, List<T>, List<T>> toList() {
        return new Collector<T, List<T>, List<T>>() {
            public Supplier<List<T>> supplier() {
                return new Supplier<List<T>>() {
                    public List<T> get() {
                        return new ArrayList<T>();
                    }
                };
            }

            public BiConsumer<List<T>, T> accumulator() {
                return new BiConsumer<List<T>, T>() {
                    public void accept(List<T> target, T value) {
                        target.add(value);
                    }
                };
            }

            public BinaryOperator<List<T>> combiner() {
                return new BinaryOperator<List<T>>() {
                    public List<T> apply(List<T> left, List<T> right) {
                        left.addAll(right);
                        return left;
                    }
                };
            }

            public Function<List<T>, List<T>> finisher() {
                return new Function<List<T>, List<T>>() {
                    public List<T> apply(List<T> value) {
                        return value;
                    }
                };
            }
        };
    }

    public static Collector<CharSequence, StringBuilder, String> joining() {
        return new Collector<CharSequence, StringBuilder, String>() {
            public Supplier<StringBuilder> supplier() {
                return new Supplier<StringBuilder>() {
                    public StringBuilder get() {
                        return new StringBuilder();
                    }
                };
            }

            public BiConsumer<StringBuilder, CharSequence> accumulator() {
                return new BiConsumer<StringBuilder, CharSequence>() {
                    public void accept(StringBuilder target, CharSequence value) {
                        target.append(value);
                    }
                };
            }

            public BinaryOperator<StringBuilder> combiner() {
                return new BinaryOperator<StringBuilder>() {
                    public StringBuilder apply(StringBuilder left, StringBuilder right) {
                        left.append(right);
                        return left;
                    }
                };
            }

            public Function<StringBuilder, String> finisher() {
                return new Function<StringBuilder, String>() {
                    public String apply(StringBuilder value) {
                        return value.toString();
                    }
                };
            }
        };
    }
}
