import java.util.HashMap;
import java.util.Map;

/**
 * Emits the identity and behaviour of the nine primitive class objects so the JVM
 * and ParparVM runs can be compared line for line.
 *
 * These were all null on ParparVM before the primitive class objects existed:
 * javac lowers a primitive class literal to a read of the boxed type's own TYPE
 * field, so `TYPE = int.class` compiled to `getstatic TYPE; putstatic TYPE`.
 * Nothing threw -- a Map keyed on them simply collapsed to one entry and answered
 * every lookup with whatever was stored last.
 */
public class PrimitiveTypeApp {
    public static void main(String[] args) {
        Class[] types = {
            Integer.TYPE, Long.TYPE, Short.TYPE, Byte.TYPE, Character.TYPE,
            Float.TYPE, Double.TYPE, Boolean.TYPE, Void.TYPE
        };
        String[] names = {
            "int", "long", "short", "byte", "char",
            "float", "double", "boolean", "void"
        };

        for (int i = 0; i < types.length; i++) {
            System.out.println("CASE|name." + names[i] + "|"
                    + (types[i] == null ? "<null>" : types[i].getName()));
        }

        // Distinct identities. Any two collapsing is the failure that made the
        // translator's own primitive-to-C-type maps answer wrongly.
        int distinct = 0;
        for (int i = 0; i < types.length; i++) {
            boolean unique = true;
            for (int j = 0; j < i; j++) {
                if (types[i] == types[j]) {
                    unique = false;
                }
            }
            if (unique) {
                distinct++;
            }
        }
        System.out.println("CASE|distinctIdentities|" + distinct);

        // The shape the translator's Util actually uses.
        Map<Class, String> byType = new HashMap<Class, String>();
        for (int i = 0; i < types.length; i++) {
            byType.put(types[i], names[i]);
        }
        System.out.println("CASE|mapSize|" + byType.size());

        int lookupFailures = 0;
        for (int i = 0; i < types.length; i++) {
            if (!names[i].equals(byType.get(types[i]))) {
                lookupFailures++;
            }
        }
        System.out.println("CASE|lookupFailures|" + lookupFailures);

        // isPrimitive, and the two natives that index instanceof tables by classId
        // and so must special-case a primitive class rather than look it up.
        System.out.println("CASE|isPrimitive.int|" + Integer.TYPE.isPrimitive());
        System.out.println("CASE|isPrimitive.boxed|" + Integer.class.isPrimitive());
        System.out.println("CASE|isArray.int|" + Integer.TYPE.isArray());
        System.out.println("CASE|assignable.self|" + Integer.TYPE.isAssignableFrom(Integer.TYPE));
        System.out.println("CASE|assignable.cross|" + Integer.TYPE.isAssignableFrom(Long.TYPE));
        System.out.println("CASE|assignable.boxed|" + Integer.TYPE.isAssignableFrom(Integer.class));
        System.out.println("CASE|isInstance.boxed|" + Integer.TYPE.isInstance(Integer.valueOf(1)));
        System.out.println("CASE|isInstance.string|" + Integer.TYPE.isInstance("x"));
        System.out.println("CASE|boxedNotPrimitive|" + (Integer.TYPE == Integer.class));

        System.out.println("DONE");
    }
}
