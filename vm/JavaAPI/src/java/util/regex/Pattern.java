/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util.regex;

/**
 *
 * @author shannah
 */
public class Pattern {
    /**
     * This constant specifies that a pattern matches Unix line endings ('\n')
     * only against the '.', '^', and '$' meta characters.
     */
    public static final int UNIX_LINES = 1;

    /**
     * This constant specifies that a {@code Pattern} is matched
     * case-insensitively. That is, the patterns "a+" and "A+" would both match
     * the string "aAaAaA".
     */
    public static final int CASE_INSENSITIVE = 1 << 1;

    /**
     * This constant specifies that a {@code Pattern} may contain whitespace or
     * comments. Otherwise comments and whitespace are taken as literal
     * characters.
     */
    public static final int COMMENTS = 1 << 2;

    /**
     * This constant specifies that the meta characters '^' and '$' match only
     * the beginning and end end of an input line, respectively. Normally, they
     * match the beginning and the end of the complete input.
     */
    public static final int MULTILINE = 1 << 3;

    /**
     * This constant specifies that the whole {@code Pattern} is to be taken
     * literally, that is, all meta characters lose their meanings.
     */
    public static final int LITERAL = 1 << 4;

    /**
     * This constant specifies that the '.' meta character matches arbitrary
     * characters, including line endings, which is normally not the case.
     */
    public static final int DOTALL = 1 << 5;

    /**
     * This constant specifies that a {@code Pattern} is matched
     * case-insensitively with regard to all Unicode characters. It is used in
     * conjunction with the {@link #CASE_INSENSITIVE} constant to extend its
     * meaning to all Unicode characters.
     */
    public static final int UNICODE_CASE = 1 << 6;

    /**
     * This constant specifies that a character in a {@code Pattern} and a
     * character in the input string only match if they are canonically
     * equivalent.
     */
    public static final int CANON_EQ = 1 << 7;

    
    public static Pattern compile(String regex) {
        throw new UnsupportedOperationException("Pattern.compile() not implemented on this platform");
    }
    public static Pattern compile(String regex, int flags) {
        throw new UnsupportedOperationException("Pattern.compile() not implemented on this platform");
    }
    public int flags() {
        return 0;
    }
    public String toString() {
        return null;
    }
    public static boolean matches(String regex, CharSequence input) {
        throw new UnsupportedOperationException("Pattern.matches() not implemented on thsi platform");
    }
    
    public Matcher matcher(CharSequence input) {
        return null;
    }
    
    public String pattern() {
        return null;
    }
    
    public static String quote(String input) {
        throw new UnsupportedOperationException("Pattern.quote() not implemented on this platform");
    }
    
    public String[] split(CharSequence input) {
        return null;
    }
    
    public String[] split(CharSequence input, int limit) {
        return null;
    }
    
    private Pattern() {
        
    }
    
}
