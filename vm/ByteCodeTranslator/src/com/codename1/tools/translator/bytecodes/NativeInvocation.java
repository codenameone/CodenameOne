package com.codename1.tools.translator.bytecodes;

import java.util.List;

/** Emits calls without materializing a second copy of their generated C. */
final class NativeInvocation {
    private NativeInvocation() { }

    static void append(StringBuilder out, String function, String receiver,
                       List<String> qualifiers, String[] literals, int stackOffset,
                       boolean checkedReceiver) {
        if (checkedReceiver) {
            out.append("({ JAVA_OBJECT __cn1DirectReceiver = ").append(receiver).append("; ");
            int offset = stackOffset;
            for (int i = 0; i < qualifiers.size(); i++) {
                String q = qualifiers.get(i);
                String type = "o".equals(q) ? "JAVA_OBJECT" : "l".equals(q) ? "JAVA_LONG"
                        : "d".equals(q) ? "JAVA_DOUBLE" : "f".equals(q) ? "JAVA_FLOAT" : "JAVA_INT";
                out.append(type).append(" __cn1DirectArg_").append(i).append(" = ");
                offset = appendArgument(out, q, literals, i, offset);
                out.append("; ");
            }
            // Java evaluates every argument before throwing for a null receiver.
            out.append("if (__cn1DirectReceiver == JAVA_NULL) { cn1ThrowNullPointerOrDie(threadStateData); } ");
        }
        out.append(function).append("(threadStateData");
        if (receiver != null) out.append(", ").append(checkedReceiver ? "__cn1DirectReceiver" : receiver);
        int offset = stackOffset;
        for (int i = 0; i < qualifiers.size(); i++) {
            out.append(", ");
            if (checkedReceiver) out.append("__cn1DirectArg_").append(i);
            else offset = appendArgument(out, qualifiers.get(i), literals, i, offset);
        }
        out.append(")");
        if (checkedReceiver) out.append("; })");
    }

    private static int appendArgument(StringBuilder out, String qualifier, String[] literals, int index, int offset) {
        if (literals != null && literals[index] != null) out.append(literals[index]);
        else { out.append("SP[-").append(offset--).append("].data.").append(qualifier); }
        return offset;
    }
}
