/*****************************************************************************
 * Codename One Playground BeanShell fork.                                    *
 *****************************************************************************/

package bsh.cn1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Minimal Stream-like API for the Codename One playground. CN1's
 * {@link Collection} backport does not expose {@code stream()}, so
 * {@link bsh.Reflect#invokeObjectMethod} returns a bridge of this type
 * when a script calls {@code .stream()} on a Collection. The bridge
 * supports the common filter/map/reduce patterns and chains by wrapping
 * each intermediate List.
 *
 * <p>This is not a full Stream implementation — it's a convenience shim
 * so scripts can write idiomatic pipelines against CN1's collections.
 * Terminal ops that ordinarily return {@code Optional<T>} return the
 * value directly (or {@code null} when the stream is empty), because
 * CN1's runtime doesn't include {@code java.util.Optional}. Likewise
 * {@code java.util.function.BiFunction} is absent from the CN1
 * runtime, so reductions are keyed off {@code BinaryOperator}.
 */
public final class CN1StreamBridge {

    private final List<Object> backing;

    public CN1StreamBridge(Collection<?> source) {
        this.backing = new ArrayList<Object>(source == null ? 0 : source.size());
        if (source != null) {
            for (Object o : source) this.backing.add(o);
        }
    }

    private CN1StreamBridge(List<Object> items) {
        this.backing = items;
    }

    public CN1StreamBridge filter(Predicate<Object> p) {
        List<Object> out = new ArrayList<Object>();
        for (Object o : backing) if (p.test(o)) out.add(o);
        return new CN1StreamBridge(out);
    }

    public CN1StreamBridge map(Function<Object, Object> fn) {
        List<Object> out = new ArrayList<Object>(backing.size());
        for (Object o : backing) out.add(fn.apply(o));
        return new CN1StreamBridge(out);
    }

    public CN1StreamBridge flatMap(Function<Object, Object> fn) {
        List<Object> out = new ArrayList<Object>();
        for (Object o : backing) {
            Object mapped = fn.apply(o);
            if (mapped instanceof CN1StreamBridge) {
                out.addAll(((CN1StreamBridge) mapped).backing);
            } else if (mapped instanceof Collection) {
                out.addAll((Collection<?>) mapped);
            } else if (mapped instanceof Iterable) {
                for (Object m : (Iterable<?>) mapped) out.add(m);
            } else if (mapped != null) {
                out.add(mapped);
            }
        }
        return new CN1StreamBridge(out);
    }

    public CN1StreamBridge peek(Consumer<Object> c) {
        for (Object o : backing) c.accept(o);
        return new CN1StreamBridge(new ArrayList<Object>(backing));
    }

    public CN1StreamBridge sorted() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Object> out = new ArrayList<Object>(backing);
        Collections.sort(out, new Comparator<Object>() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            public int compare(Object a, Object b) {
                return ((Comparable) a).compareTo(b);
            }
        });
        return new CN1StreamBridge(out);
    }

    public CN1StreamBridge sorted(Comparator<Object> c) {
        List<Object> out = new ArrayList<Object>(backing);
        Collections.sort(out, c);
        return new CN1StreamBridge(out);
    }

    public CN1StreamBridge distinct() {
        LinkedHashSet<Object> seen = new LinkedHashSet<Object>(backing);
        return new CN1StreamBridge(new ArrayList<Object>(seen));
    }

    public CN1StreamBridge limit(long n) {
        if (n >= backing.size()) {
            return new CN1StreamBridge(new ArrayList<Object>(backing));
        }
        List<Object> out = new ArrayList<Object>();
        long remaining = Math.max(0, n);
        for (Object o : backing) {
            if (remaining-- <= 0) break;
            out.add(o);
        }
        return new CN1StreamBridge(out);
    }

    public CN1StreamBridge skip(long n) {
        if (n <= 0) {
            return new CN1StreamBridge(new ArrayList<Object>(backing));
        }
        List<Object> out = new ArrayList<Object>();
        long skipped = 0;
        for (Object o : backing) {
            if (skipped < n) { skipped++; continue; }
            out.add(o);
        }
        return new CN1StreamBridge(out);
    }

    public void forEach(Consumer<Object> c) {
        for (Object o : backing) c.accept(o);
    }

    public long count() {
        return backing.size();
    }

    public boolean anyMatch(Predicate<Object> p) {
        for (Object o : backing) if (p.test(o)) return true;
        return false;
    }

    public boolean allMatch(Predicate<Object> p) {
        for (Object o : backing) if (!p.test(o)) return false;
        return true;
    }

    public boolean noneMatch(Predicate<Object> p) {
        for (Object o : backing) if (p.test(o)) return false;
        return true;
    }

    /** Returns the first element, or {@code null} when empty. The real
     * Stream API returns {@code Optional}, which CN1 doesn't ship. */
    public Object findFirst() {
        return backing.isEmpty() ? null : backing.get(0);
    }

    public Object findAny() {
        return findFirst();
    }

    /** Minimum by natural order, or {@code null} when empty. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object min() {
        Object best = null;
        for (Object o : backing) {
            if (best == null || ((Comparable) o).compareTo(best) < 0) best = o;
        }
        return best;
    }

    public Object min(Comparator<Object> c) {
        Object best = null;
        for (Object o : backing) {
            if (best == null || c.compare(o, best) < 0) best = o;
        }
        return best;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object max() {
        Object best = null;
        for (Object o : backing) {
            if (best == null || ((Comparable) o).compareTo(best) > 0) best = o;
        }
        return best;
    }

    public Object max(Comparator<Object> c) {
        Object best = null;
        for (Object o : backing) {
            if (best == null || c.compare(o, best) > 0) best = o;
        }
        return best;
    }

    /** Reduce with an accumulator. Returns the single element when the
     * stream has one, the reduction when it has more, or {@code null}
     * when empty (real Stream returns an Optional here). */
    public Object reduce(BinaryOperator<Object> op) {
        Object acc = null;
        boolean started = false;
        for (Object o : backing) {
            acc = started ? op.apply(acc, o) : o;
            started = true;
        }
        return acc;
    }

    public Object reduce(Object identity, BinaryOperator<Object> op) {
        Object acc = identity;
        for (Object o : backing) acc = op.apply(acc, o);
        return acc;
    }

    public Object[] toArray() {
        return backing.toArray();
    }

    /** Collect into a List. Ignores the collector argument — we only
     * support list-collection in this shim; anything else returns a
     * best-effort ArrayList. */
    public List<Object> collect(Object collector) {
        return new ArrayList<Object>(backing);
    }

    public List<Object> toList() {
        return new ArrayList<Object>(backing);
    }

    public Iterator<Object> iterator() {
        return backing.iterator();
    }
}
