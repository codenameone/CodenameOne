package bsh;

import java.util.HashMap;
import java.util.Map;

/** CN1-safe modifier constants without relying on java.lang.reflect.Modifier. */
public class Modifiers implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final int PUBLIC = 0x0001;
    public static final int PRIVATE = 0x0002;
    public static final int PROTECTED = 0x0004;
    public static final int STATIC = 0x0008;
    public static final int FINAL = 0x0010;
    public static final int SYNCHRONIZED = 0x0020;
    public static final int VOLATILE = 0x0040;
    public static final int TRANSIENT = 0x0080;
    public static final int NATIVE = 0x0100;
    public static final int INTERFACE_FLAG = 0x0200;
    public static final int ABSTRACT = 0x0400;
    public static final int STRICT = 0x0800;
    public static final int SYNTHETIC = 0x1000;
    public static final int ANNOTATION = 0x2000;
    public static final int ENUM = 0x4000;
    public static final int MANDATED = 0x8000;
    public static final int DEFAULT = 0x10000;

    public static final int CLASS = 0;
    public static final int INTERFACE = 1;
    public static final int METHOD = 2;
    public static final int FIELD = 3;
    public static final int PARAMETER = 4;
    public static final int CONSTRUCTOR = 5;

    public static final Map<String, Integer> CONST = new HashMap<String, Integer>(17);

    private static final int ACCESS_MODIFIERS = PUBLIC | PRIVATE | PROTECTED;
    private static final int CLASS_VALID = PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL | ABSTRACT
            | STRICT | SYNTHETIC | ANNOTATION | ENUM;
    private static final int INTERFACE_VALID = PUBLIC | PRIVATE | PROTECTED | STATIC | ABSTRACT
            | STRICT | SYNTHETIC | ANNOTATION;
    private static final int METHOD_VALID = PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL
            | SYNCHRONIZED | NATIVE | ABSTRACT | STRICT | DEFAULT;
    private static final int FIELD_VALID = PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL
            | VOLATILE | TRANSIENT | ENUM;
    private static final int PARAMETER_VALID = FINAL | SYNTHETIC | MANDATED;
    private static final int CONSTRUCTOR_VALID = PUBLIC | PRIVATE | PROTECTED;

    static {
        CONST.put("public", PUBLIC);
        CONST.put("private", PRIVATE);
        CONST.put("protected", PROTECTED);
        CONST.put("static", STATIC);
        CONST.put("final", FINAL);
        CONST.put("synchronized", SYNCHRONIZED);
        CONST.put("volatile", VOLATILE);
        CONST.put("transient", TRANSIENT);
        CONST.put("native", NATIVE);
        CONST.put("interface", INTERFACE_FLAG);
        CONST.put("abstract", ABSTRACT);
        CONST.put("strict", STRICT);
        CONST.put("synthetic", SYNTHETIC);
        CONST.put("annotation", ANNOTATION);
        CONST.put("enum", ENUM);
        CONST.put("mandated", MANDATED);
        CONST.put("default", DEFAULT);
    }

    private String type;
    private int valid;
    private int context;
    private int modifiers = 0;

    public Modifiers(int context) {
        appliedContext(context);
    }

    public void addModifier(String name) {
        addModifier(toModifier(name));
    }

    public void addModifier(int mod) {
        if ((valid & mod) == 0) {
            throw new IllegalStateException(type + " cannot be declared '" + toModifier(mod) + "'");
        } else if ((mod & ACCESS_MODIFIERS) != 0
                && (modifiers & ACCESS_MODIFIERS) != 0
                && (modifiers & ACCESS_MODIFIERS) != mod) {
            throw new IllegalStateException("public/private/protected cannot be used in combination.");
        }
        modifiers |= mod;
    }

    public void addModifiers(int mods) {
        for (int mod = 1; mod != 0 && mod <= mods; mod <<= 1) {
            if ((mods & mod) != 0) {
                addModifier(mod);
            }
        }
    }

    public int getModifiers() {
        return modifiers;
    }

    public void changeContext(int context) {
        int mods = modifiers;
        modifiers = 0;
        appliedContext(context);
        addModifiers(mods);
    }

    public boolean isAppliedContext(int context) {
        return this.context == context;
    }

    public boolean hasModifier(String name) {
        return hasModifier(toModifier(name));
    }

    public boolean hasModifier(int mod) {
        return (modifiers & mod) != 0;
    }

    public void setConstant() {
        modifiers = PUBLIC | STATIC | FINAL;
    }

    private void appliedContext(int context) {
        this.context = context;
        switch (context) {
            case CLASS:
                valid = CLASS_VALID;
                type = "Class";
                break;
            case INTERFACE:
                valid = INTERFACE_VALID;
                type = "Interface";
                break;
            case METHOD:
                valid = METHOD_VALID;
                type = "Method";
                break;
            case FIELD:
                valid = FIELD_VALID;
                type = "Field";
                break;
            case PARAMETER:
                valid = PARAMETER_VALID;
                type = "Parameter";
                break;
            case CONSTRUCTOR:
                valid = CONSTRUCTOR_VALID;
                type = "Constructor";
                break;
            default:
                valid = 0;
                type = "Unknown";
        }
    }

    private int toModifier(String name) {
        Integer mod = CONST.get(name);
        if (mod == null) {
            throw new IllegalStateException("Unknown modifier: '" + name + "'");
        }
        return mod.intValue();
    }

    private String toModifier(int mod) {
        for (Map.Entry<String, Integer> entry : CONST.entrySet()) {
            if (entry.getValue().intValue() == mod) {
                return entry.getKey();
            }
        }
        return String.valueOf(mod);
    }

    @Override
    public String toString() {
        StringBuffer out = new StringBuffer("Modifiers:");
        append(out, PUBLIC, " public");
        append(out, PRIVATE, " private");
        append(out, PROTECTED, " protected");
        append(out, STATIC, " static");
        append(out, FINAL, " final");
        append(out, SYNCHRONIZED, " synchronized");
        append(out, VOLATILE, " volatile");
        append(out, TRANSIENT, " transient");
        append(out, NATIVE, " native");
        append(out, INTERFACE_FLAG, " interface");
        append(out, ABSTRACT, " abstract");
        append(out, STRICT, " strict");
        append(out, ENUM, " enum");
        append(out, DEFAULT, " default");
        return out.toString();
    }

    private void append(StringBuffer out, int flag, String name) {
        if ((modifiers & flag) != 0) {
            out.append(name);
        }
    }
}
