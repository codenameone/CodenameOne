import com.codename1.ui.*;
import java.util.*;

/**
 * Collections reached through interface-typed references.
 *
 * The declared type at the call site is java.util.List, not ArrayList, so a
 * linker that resolves against the declared owner rather than the receiver runs
 * AbstractList's method -- which throws. Every real application does this in
 * its first ten lines, and it is invisible to a probe that declares the
 * concrete type.
 */
public class CollectionProbe {
    public static void main(String[] a) {
        java.util.List<String> l = new ArrayList<String>();
        l.add("b"); l.add("a"); l.add("c");
        Collections.sort(l);
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("k", 7);
        Set<String> s = new HashSet<String>();
        s.add("x"); s.add("x");
        Iterator<String> it = l.iterator();
        StringBuilder walked = new StringBuilder();
        while (it.hasNext()) { walked.append(it.next()); }
        Collection<String> c = l;
        System.out.println("PROBE CollectionProbe: list=" + l + " size=" + l.size()
            + " map=" + m.get("k") + " set=" + s.size() + " walked=" + walked
            + " contains=" + c.contains("a") + " removed=" + l.remove("a") + " now=" + l);
        new Form("Collection").show();
    }
}
