/*
 * Android counterpart of the iOS runtime-clazz-synthesis spike.
 *
 * Dalvik/ART has no patchable vtable and Google Play forbids loading dex at
 * runtime, so the plan's Android mechanism is different from the iOS one: a
 * subclass generated at BUILD time, whose every overridable method either
 * delegates to the interpreter or calls super. Interp_Base below is written by
 * hand in exactly the shape that generator would emit.
 *
 * The claims under test are identical to the iOS spike's, because they are
 * claims about the object model rather than about either mechanism:
 * dispatch from an AOT caller, instanceof, inherited methods, parent self-calls,
 * non-interference with the real class, and GC survival.
 */
import java.lang.reflect.Method;

public class AndroidInterpSpike {

    /** Stands in for a framework class an interpreted program subclasses. */
    public static class Base {
        public int tag = 7;
        public int size() { return 1; }
        public String greet() { return "base"; }
        public String describe() { return greet() + "/" + size(); }
    }

    /**
     * Stands in for the interpreter's state for one interpreted object: which
     * methods the interpreted class overrides, and how to run them.
     */
    static final class InterpInstance {
        private final java.util.Set<String> overridden;
        InterpInstance(String... names) {
            overridden = new java.util.HashSet<String>(java.util.Arrays.asList(names));
        }
        boolean overrides(String slot) { return overridden.contains(slot); }
        Object call(String slot) { return "interpreted"; }
    }

    /**
     * The build-time generated shim. Every overridable method is guarded: if
     * the interpreted class overrides it, run the interpreter; otherwise defer
     * to super. super_* bridges give interpreted code a way back to super.
     */
    public static final class Interp_Base extends Base {
        private final InterpInstance $i;
        Interp_Base(InterpInstance i) { this.$i = i; }

        @Override public String greet() {
            return $i.overrides("greet") ? (String) $i.call("greet") : super.greet();
        }
        public String super_greet() { return super.greet(); }

        @Override public int size() {
            return $i.overrides("size") ? ((Integer) $i.call("size")).intValue() : super.size();
        }
        public int super_size() { return super.size(); }
    }

    public static void main(String[] args) throws Exception {
        // The Android InterpLinker binds framework calls through real
        // reflection rather than generated dispatch -- this is that path.
        Method greet = Base.class.getMethod("greet");

        Object raw = new Interp_Base(new InterpInstance("greet"));
        System.out.println("INSTANCEOF:" + (raw instanceof Base));

        Base b = (Base) raw;
        System.out.println("DISPATCH:" + b.greet());
        System.out.println("INHERITED:" + b.size());
        // describe() is compiled parent code calling greet() on itself; the
        // override has to win there too.
        System.out.println("INTERNAL:" + b.describe());
        System.out.println("REFLECT:" + greet.invoke(b));

        Base plain = new Base();
        System.out.println("UNAFFECTED:" + plain.greet());

        b.tag = 42;
        StringBuilder sink = new StringBuilder();
        for (int i = 0; i < 400000; i++) { sink.append('x'); if (sink.length() > 64) sink.setLength(0); }
        System.gc();
        System.out.println("AFTERGC:" + b.greet() + ":" + b.tag);
        System.out.println("DONE");
    }
}
