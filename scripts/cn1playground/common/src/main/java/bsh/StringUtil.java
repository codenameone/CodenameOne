package bsh;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

public class StringUtil {
    public static String typeString(Object value) {
        return value == null || value == Primitive.NULL ? "null"
                : value instanceof Primitive ? typeString(((Primitive) value).getType())
                : typeString(Types.getType(value));
    }

    public static String typeString(Class<?> clas) {
        if (clas == null) {
            return "null";
        }
        if (Map.class.isAssignableFrom(clas)) clas = Map.class;
        else if (List.class.isAssignableFrom(clas)) clas = List.class;
        else if (Queue.class.isAssignableFrom(clas)) clas = Queue.class;
        else if (Deque.class.isAssignableFrom(clas)) clas = Deque.class;
        else if (Set.class.isAssignableFrom(clas)) clas = Set.class;
        else if (Entry.class.isAssignableFrom(clas)) clas = Entry.class;
        return clas.getName().startsWith("java") ? clas.getSimpleName() : clas.getName();
    }

    public static String typeValueString(final Object value) {
        return valueString(value) + " :" + typeString(value);
    }

    public static String valueString(final Object value) {
        if (value == null || value == Primitive.NULL) {
            return "null";
        }
        if (value instanceof Object[]) {
            return joinArray((Object[]) value, "{", "}");
        }
        if (value instanceof Collection) {
            return joinCollection((Collection<?>) value, "[", "]");
        }
        if (value instanceof Map) {
            StringBuffer out = new StringBuffer("{");
            boolean first = true;
            for (Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                if (!first) out.append(", ");
                first = false;
                out.append(valueString(e.getKey())).append("=").append(valueString(e.getValue()));
            }
            return out.append("}").toString();
        }
        if (value instanceof Entry) {
            Entry<?, ?> e = (Entry<?, ?>) value;
            return valueString(e.getKey()) + "=" + valueString(e.getValue());
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        Object unwrapped = Primitive.unwrap(value);
        if (unwrapped instanceof Character) {
            return "'" + unwrapped + "'";
        }
        return String.valueOf(value);
    }

    public static String maxCommonPrefix(String one, String two) {
        int i = 0;
        int max = Math.min(one.length(), two.length());
        while (i < max && one.charAt(i) == two.charAt(i)) {
            i++;
        }
        return one.substring(0, i);
    }

    public static String methodString(String name, Object[] args) {
        return methodString(name, Types.getTypes(args));
    }

    public static String methodString(String name, String[] types) {
        StringBuffer sb = new StringBuffer();
        sb.append(name).append('(');
        for (int i = 0; i < types.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(types[i]);
        }
        return sb.append(')').toString();
    }

    public static String methodString(String name, Class<?>[] types) {
        return methodString(name, getTypeNames(types));
    }

    public static String methodString(String name, Class<?>[] types, String[] names) {
        String[] sig = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            sig[i] = getTypeName(types[i]) + " " + names[i];
        }
        return methodString(name, sig);
    }

    public static String methodString(BshMethod method) {
        StringBuffer sb = new StringBuffer();
        String mods = method.getModifiers().toString().substring(11);
        sb.append(mods).append(" ")
          .append(getTypeName(method.getReturnType())).append(" ")
          .append(methodString(method.getName(), method.getParameterTypes(), method.getParameterNames()));
        return sb.append(mods.indexOf("abstract") >= 0 ? ";" : " {}").toString();
    }

    public static String classString(Class<?> type) {
        StringBuffer sb = new StringBuffer();
        if (Reflect.isGeneratedClass(type)) {
            sb.append(Reflect.getClassModifiers(type).toString().substring(11));
        }
        sb.append(type.isInterface() ? " interface " : " class ");
        sb.append(getTypeName(type)).append(" {");
        return sb.toString().trim();
    }

    public static String variableString(Variable var) {
        return new StringBuffer()
                .append(var.getModifiers().toString().substring(11))
                .append(" ").append(getTypeName(var.getType()))
                .append(" ").append(var.getName())
                .append(";").toString();
    }

    private static String[] getTypeNames(Class<?>[] types) {
        String[] out = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            out[i] = getTypeName(types[i]);
        }
        return out;
    }

    private static String getTypeName(Class<?> type) {
        return type == null ? "Object" : type.getSimpleName();
    }

    private static String joinArray(Object[] values, String open, String close) {
        StringBuffer out = new StringBuffer(open);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(", ");
            out.append(valueString(values[i]));
        }
        return out.append(close).toString();
    }

    private static String joinCollection(Collection<?> values, String open, String close) {
        StringBuffer out = new StringBuffer(open);
        boolean first = true;
        for (Object value : values) {
            if (!first) out.append(", ");
            first = false;
            out.append(valueString(value));
        }
        return out.append(close).toString();
    }
}
