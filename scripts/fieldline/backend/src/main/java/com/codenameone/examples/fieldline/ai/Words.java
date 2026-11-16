package com.codenameone.examples.fieldline.ai;

import java.util.ArrayList;
import java.util.List;

/// Text folded for matching: ASCII letters in lower case, by hand rather than
/// `toLowerCase()`, which follows the server's locale; everything that is not
/// a letter or a digit becomes a space.
final class Words {
    private Words() {
    }

    static String fold(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                out.append((char) (c + ('a' - 'A')));
            } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
            } else {
                out.append(' ');
            }
        }
        return out.toString();
    }

    /// The words of folded text.
    static List<String> words(String folded) {
        List<String> out = new ArrayList<String>();
        int at = 0;
        int n = folded == null ? 0 : folded.length();
        while (at < n) {
            while (at < n && folded.charAt(at) == ' ') {
                at++;
            }
            int end = at;
            while (end < n && folded.charAt(end) != ' ') {
                end++;
            }
            if (end > at) {
                out.add(folded.substring(at, end));
            }
            at = end;
        }
        return out;
    }

    static boolean has(String folded, String word) {
        return words(folded).contains(word);
    }

    static boolean any(String folded, String... options) {
        List<String> w = words(folded);
        for (String o : options) {
            if (w.contains(o)) {
                return true;
            }
        }
        return false;
    }
}
