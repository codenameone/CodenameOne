package com.codename1.tools.translator;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Inverts the "is this symbol referenced by native code" scan.
 *
 * The old code did, per Java method/class, a full substring scan of every
 * native source string -- O(methods x native_bytes). Native symbols are CN1
 * mangled identifiers ([A-Za-z0-9_]+), and a query string X matches the native
 * text iff X is a substring of some maximal identifier token in it. So we:
 *   1. tokenize all native sources into the DISTINCT set of identifier tokens
 *      (this dedups repeated symbols across files, bounding the structure),
 *   2. build a suffix automaton over those tokens (joined by '\n' so a query
 *      can never span two tokens), giving O(|X|) substring membership.
 *
 * Result: native cost becomes O(native_bytes) once + O(|X|) per query, instead
 * of O(methods x native_bytes). Semantics are identical to String.contains over
 * the raw native text because queries are themselves delimiter-free identifiers.
 */
final class NativeSymbolIndex {
    private int[] len;
    private int[] link;
    private HashMap<Character, Integer>[] next;
    private int last;
    private int sz;

    @SuppressWarnings("unchecked")
    NativeSymbolIndex(String[] nativeSources) {
        HashSet<String> tokens = new HashSet<String>();
        if (nativeSources != null) {
            for (String s : nativeSources) {
                if (s == null) {
                    continue;
                }
                int n = s.length();
                int i = 0;
                while (i < n) {
                    if (isIdent(s.charAt(i))) {
                        int j = i + 1;
                        while (j < n && isIdent(s.charAt(j))) {
                            j++;
                        }
                        tokens.add(s.substring(i, j));
                        i = j;
                    } else {
                        i++;
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            sb.append(t).append('\n');
        }
        String text = sb.toString();

        int cap = 2 * Math.max(1, text.length()) + 5;
        len = new int[cap];
        link = new int[cap];
        next = new HashMap[cap];
        sz = 0;
        last = newState(0, -1);
        for (int i = 0; i < text.length(); i++) {
            extend(text.charAt(i));
        }
    }

    private int newState(int l, int lnk) {
        len[sz] = l;
        link[sz] = lnk;
        next[sz] = new HashMap<Character, Integer>();
        return sz++;
    }

    private void extend(char c) {
        int cur = newState(len[last] + 1, -1);
        int p = last;
        while (p != -1 && !next[p].containsKey(c)) {
            next[p].put(c, cur);
            p = link[p];
        }
        if (p == -1) {
            link[cur] = 0;
        } else {
            int q = next[p].get(c);
            if (len[p] + 1 == len[q]) {
                link[cur] = q;
            } else {
                int clone = newState(len[p] + 1, link[q]);
                next[clone].putAll(next[q]);
                while (p != -1) {
                    Integer t = next[p].get(c);
                    if (t == null || t != q) {
                        break;
                    }
                    next[p].put(c, clone);
                    p = link[p];
                }
                link[q] = clone;
                link[cur] = clone;
            }
        }
        last = cur;
    }

    /** True iff pat occurs as a substring of some native identifier token. */
    boolean contains(String pat) {
        int cur = 0;
        for (int i = 0; i < pat.length(); i++) {
            Integer nx = next[cur].get(pat.charAt(i));
            if (nx == null) {
                return false;
            }
            cur = nx;
        }
        return true;
    }

    private static boolean isIdent(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9') || c == '_';
    }
}
