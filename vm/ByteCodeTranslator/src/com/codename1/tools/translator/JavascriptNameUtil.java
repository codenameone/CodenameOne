package com.codename1.tools.translator;

import java.util.ArrayList;
import java.util.List;

final class JavascriptNameUtil {
    private static final String SYMBOL_PREFIX = "cn1_";

    private JavascriptNameUtil() {
    }

    static String sanitizeClassName(String owner) {
        return owner.replace('/', '_').replace('$', '_').replace('.', '_');
    }

    static String runtimeTypeName(String typeName) {
        if (typeName == null || typeName.length() == 0) {
            return typeName;
        }
        if (typeName.charAt(0) != '[') {
            return sanitizeClassName(typeName);
        }
        int dimensions = 0;
        while (dimensions < typeName.length() && typeName.charAt(dimensions) == '[') {
            dimensions++;
        }
        String componentType;
        char kind = typeName.charAt(dimensions);
        if (kind == 'L') {
            componentType = sanitizeClassName(typeName.substring(dimensions + 1, typeName.length() - 1));
        } else {
            componentType = primitiveArrayComponent(kind);
        }
        StringBuilder out = new StringBuilder(componentType);
        for (int i = 0; i < dimensions; i++) {
            out.append("[]");
        }
        return out.toString();
    }

    private static String primitiveArrayComponent(char kind) {
        switch (kind) {
            case 'Z':
                return "JAVA_BOOLEAN";
            case 'C':
                return "JAVA_CHAR";
            case 'F':
                return "JAVA_FLOAT";
            case 'D':
                return "JAVA_DOUBLE";
            case 'B':
                return "JAVA_BYTE";
            case 'S':
                return "JAVA_SHORT";
            case 'I':
                return "JAVA_INT";
            case 'J':
                return "JAVA_LONG";
            default:
                return sanitizeClassName(String.valueOf(kind));
        }
    }

    static String methodIdentifier(String owner, String name, String desc) {
        StringBuilder b = new StringBuilder();
        b.append(SYMBOL_PREFIX).append(identifierPart(sanitizeClassName(owner))).append("_");
        if ("<init>".equals(name)) {
            b.append("__INIT__");
        } else if ("<clinit>".equals(name)) {
            b.append("__CLINIT__");
        } else {
            b.append(identifierPart(name));
        }
        BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, b, new ArrayList<String>());
        return b.toString();
    }

    static String fieldProperty(String owner, String name) {
        return SYMBOL_PREFIX + identifierPart(sanitizeClassName(owner)) + "_" + identifierPart(name);
    }

    static String identifierPart(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_') {
                out.append(ch);
            } else {
                out.append('_');
            }
        }
        if (out.length() == 0) {
            out.append("value");
        }
        return out.toString();
    }

    static String escapeJs(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (ch < 32 || ch > 126) {
                        String hex = Integer.toHexString(ch);
                        out.append("\\u");
                        for (int j = hex.length(); j < 4; j++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(ch);
                    }
            }
        }
        return out.toString();
    }

    static String defaultValue(String desc) {
        if (desc == null || desc.isEmpty()) {
            return "null";
        }
        if (desc.length() != 1) {
            return "null";
        }
        char type = desc.charAt(0);
        switch (type) {
            case 'Z':
            case 'C':
            case 'F':
            case 'D':
            case 'B':
            case 'S':
            case 'I':
            case 'J':
                return "0";
            default:
                return "null";
        }
    }

    static List<String> argumentTypes(String desc) {
        List<String> arguments = new ArrayList<String>();
        BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, new StringBuilder(), arguments);
        return arguments;
    }
}
