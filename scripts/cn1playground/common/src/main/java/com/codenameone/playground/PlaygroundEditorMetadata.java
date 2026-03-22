package com.codenameone.playground;

import bsh.cn1.GeneratedCN1Access;

final class PlaygroundEditorMetadata {
    private static final String[] DEFAULT_IMPORTS = new String[]{
            "com.codename1.ui",
            "com.codename1.ui.layouts",
            "com.codename1.components",
            "com.codename1.ui.geom",
            "java.lang",
            "java.io",
            "java.util"
    };

    private static final String[][] GLOBALS = new String[][]{
            {"ctx", "com.codenameone.playground.PlaygroundContext"},
            {"theme", "com.codename1.ui.util.Resources"},
            {"hostForm", "com.codename1.ui.Form"},
            {"previewRoot", "com.codename1.ui.Container"},
            {"Display", "com.codename1.ui.Display"},
            {"UIManager", "com.codename1.ui.plaf.UIManager"},
            {"FontImage", "com.codename1.ui.FontImage"},
            {"CN", "com.codename1.ui.CN"},
            {"BoxLayout", "com.codename1.ui.layouts.BoxLayout"},
            {"BorderLayout", "com.codename1.ui.layouts.BorderLayout"},
            {"FlowLayout", "com.codename1.ui.layouts.FlowLayout"},
            {"GridLayout", "com.codename1.ui.layouts.GridLayout"},
            {"LayeredLayout", "com.codename1.ui.layouts.LayeredLayout"},
            {"Style", "com.codename1.ui.plaf.Style"}
    };

    private static String cachedJson;

    private PlaygroundEditorMetadata() {
    }

    static synchronized String json() {
        if (cachedJson == null) {
            cachedJson = buildJson();
        }
        return cachedJson;
    }

    private static String buildJson() {
        String[] classNames = GeneratedCN1Access.INSTANCE.getIndexedClassNames();
        StringBuilder out = new StringBuilder(1 << 20);
        out.append('{');
        appendArrayField(out, "defaultImports", DEFAULT_IMPORTS);
        out.append(',');
        appendGlobals(out);
        out.append(',');
        out.append("\"types\":{");
        boolean first = true;
        for (int i = 0; i < classNames.length; i++) {
            String qualifiedName = classNames[i];
            if (!first) {
                out.append(',');
            }
            first = false;
            appendString(out, qualifiedName);
            out.append(':');
            appendType(out, qualifiedName);
        }
        out.append("}}");
        return out.toString();
    }

    private static void appendGlobals(StringBuilder out) {
        out.append("\"globals\":{");
        for (int i = 0; i < GLOBALS.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            appendString(out, GLOBALS[i][0]);
            out.append(':');
            appendString(out, GLOBALS[i][1]);
        }
        out.append('}');
    }

    private static void appendType(StringBuilder out, String qualifiedName) {
        String packageName = packageName(qualifiedName);
        out.append('{');
        out.append("\"simple\":");
        appendString(out, simpleName(qualifiedName));
        out.append(",\"package\":");
        appendString(out, packageName);
        out.append(",\"methods\":");
        appendArray(out, GeneratedCN1Access.INSTANCE.getMethodSignatures(qualifiedName));
        out.append(",\"fields\":");
        appendArray(out, GeneratedCN1Access.INSTANCE.getFieldNames(qualifiedName));
        out.append('}');
    }

    private static String packageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot < 0 ? "" : qualifiedName.substring(0, lastDot);
    }

    private static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot < 0 ? qualifiedName : qualifiedName.substring(lastDot + 1);
    }

    private static void appendArrayField(StringBuilder out, String name, String[] values) {
        appendString(out, name);
        out.append(':');
        appendArray(out, values);
    }

    private static void appendArray(StringBuilder out, String[] values) {
        out.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            appendString(out, values[i]);
        }
        out.append(']');
    }

    private static void appendString(StringBuilder out, String value) {
        out.append('"');
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
                    if (ch < 0x20) {
                        String hex = Integer.toHexString(ch);
                        out.append("\\u");
                        for (int j = hex.length(); j < 4; j++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(ch);
                    }
                    break;
            }
        }
        out.append('"');
    }
}
