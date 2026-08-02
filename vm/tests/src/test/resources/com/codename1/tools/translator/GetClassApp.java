import java.util.HashMap;

/**
 * Reproduces the shape of the class-code table in issue #5482, where the reporter saw
 * {@code getClass()} apparently return null: an interface-typed reference is asked for
 * its class, and that Class object is used as a HashMap key while the program allocates
 * heavily. Class objects are not ordinary heap objects in ParparVM -- they are static
 * structs whose vtable is wired up separately -- so identity, hashing and string
 * conversion of a Class all deserve pinning.
 *
 * <p>Every line is a property that holds on any conforming JVM, so the harness can diff
 * this output against a real JVM run without depending on how ParparVM spells class
 * names.</p>
 */
public class GetClassApp {
    interface Bulkable {
    }

    static class Entry implements Bulkable {
    }

    static class Other implements Bulkable {
    }

    private static final StringBuilder OUT = new StringBuilder();

    private static void say(String label, Object value) {
        OUT.append("CASE|").append(label).append('|').append(value).append('\n');
    }

    public static void main(String[] args) {
        Bulkable a = new Entry();
        Bulkable b = new Other();

        Class classOfA = a.getClass();
        Class classOfB = b.getClass();

        say("notNull", Boolean.valueOf(classOfA != null && classOfB != null));
        say("stable", Boolean.valueOf(classOfA == a.getClass()));
        say("distinct", Boolean.valueOf(classOfA != classOfB));
        say("hashStable", Boolean.valueOf(classOfA.hashCode() == a.getClass().hashCode()));
        say("equalsSelf", Boolean.valueOf(classOfA.equals(a.getClass())));

        // "class is " + cl -- the concatenation that printed "null" in the report. This
        // goes through StringBuilder.append(Object), i.e. a virtual toString dispatch on
        // a Class object.
        String concatenated = "class is " + classOfA;
        say("concatNotNull", Boolean.valueOf(concatenated != null));
        say("concatHasClass", Boolean.valueOf(!"class is null".equals(concatenated)
                && concatenated.length() > "class is ".length()));
        say("nameNotEmpty", Boolean.valueOf(classOfA.getName() != null
                && classOfA.getName().length() > 0));
        say("namesDiffer", Boolean.valueOf(!classOfA.getName().equals(classOfB.getName())));

        HashMap classCode = new HashMap();
        classCode.put(classOfA, Byte.valueOf((byte) 1));
        classCode.put(classOfB, Byte.valueOf((byte) 2));
        say("mapLookupA", classCode.get(a.getClass()));
        say("mapLookupB", classCode.get(b.getClass()));
        say("mapSize", Integer.valueOf(classCode.size()));

        // The reporter's failure only showed up while a dictionary load was allocating
        // hard, so re-check every invariant under churn instead of once at startup.
        int failures = 0;
        StringBuilder scratch = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            Bulkable fresh = (i & 1) == 0 ? (Bulkable) new Entry() : (Bulkable) new Other();
            Class cls = fresh.getClass();
            if (cls == null) {
                failures++;
                continue;
            }
            if (cls != (((i & 1) == 0) ? classOfA : classOfB)) {
                failures++;
            }
            if (classCode.get(cls) == null) {
                failures++;
            }
            scratch.setLength(0);
            scratch.append("pad").append(i).append(cls);
        }
        say("churnFailures", Integer.valueOf(failures));

        System.out.println(OUT.toString());
        System.out.println("DONE");
    }
}
