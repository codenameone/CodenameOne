package com.codename1.impl;

import com.codename1.util.regex.RE;
import com.codename1.util.regex.RESyntaxException;

/// Bridge methods used by bytecode rewrite rules for JDK APIs that are risky/unsupported on some targets.
public final class JdkApiRewriteHelper {
    private JdkApiRewriteHelper() {
    }

    public static String[] split(String source, String regex) {
        return split(source, regex, 0);
    }

    public static String[] split(String source, String regex, int limit) {
        if (source == null) {
            throw new NullPointerException("source is null");
        }
        if (regex == null) {
            throw new NullPointerException("regex is null");
        }
        if (regex.length() == 0) {
            return new String[]{source};
        }
        try {
            String[] split = new RE(regex).split(source);
            if (limit > 0 && split.length > limit) {
                String[] limited = new String[limit];
                for (int i = 0; i < limit - 1; i++) {
                    limited[i] = split[i];
                }
                StringBuilder remainder = new StringBuilder();
                for (int i = limit - 1; i < split.length; i++) {
                    if (i > limit - 1) {
                        remainder.append(regex);
                    }
                    remainder.append(split[i]);
                }
                limited[limit - 1] = remainder.toString();
                return limited;
            }
            if (limit == 0) {
                int end = split.length;
                while (end > 0 && split[end - 1].length() == 0) {
                    end--;
                }
                if (end == split.length) {
                    return split;
                }
                String[] trimmed = new String[end];
                System.arraycopy(split, 0, trimmed, 0, end);
                return trimmed;
            }
            return split;
        } catch (RESyntaxException ex) {
            java.util.List<String> fallback = com.codename1.util.StringUtil.tokenize(source, regex);
            return fallback.toArray(new String[fallback.size()]);
        }
    }
}
