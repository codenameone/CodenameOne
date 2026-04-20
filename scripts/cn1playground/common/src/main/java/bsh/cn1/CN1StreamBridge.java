/*****************************************************************************
 * Codename One Playground BeanShell fork.                                    *
 *****************************************************************************/

package bsh.cn1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Minimal Stream-like API for the Codename One playground. CN1's
 * {@link Collection} backport does not expose {@code stream()}, so
 * {@link bsh.Reflect#invokeObjectMethod} returns a bridge of this type
 * when a script calls {@code .stream()} on a Collection. The bridge
 * supports the common patterns — filter / map / forEach / count /
 * collect(toList) — and chains by wrapping each intermediate List.
 *
 * <p>This is not a full Stream implementation; it's a convenience
 * shim so scripts can write idiomatic filter/map pipelines against
 * CN1's collections.
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

    public void forEach(Consumer<Object> c) {
        for (Object o : backing) c.accept(o);
    }

    public long count() {
        return backing.size();
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
