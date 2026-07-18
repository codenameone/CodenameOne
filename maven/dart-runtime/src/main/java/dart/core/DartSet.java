package dart.core;

import dart.runtime.DartRuntime;

import java.util.LinkedHashSet;

/**
 * Dart's Set&lt;E&gt;: insertion-ordered (Dart set literals are LinkedHashSet).
 */
public class DartSet<E> extends LinkedHashSet<E> {

    public DartSet() {
    }

    @SafeVarargs
    public static <E> DartSet<E> of(E... elements) {
        DartSet<E> s = new DartSet<>();
        for (E e : elements) {
            s.add(e);
        }
        return s;
    }

    public long length() {
        return size();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public DartIterable<E> asIterable() {
        return DartIterable.wrap(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (E e : this) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(DartRuntime.str(e));
            first = false;
        }
        return sb.append("}").toString();
    }
}
