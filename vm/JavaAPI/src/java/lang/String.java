/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package java.lang;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * The String class represents character strings. All string literals in Java programs, such as "abc", are implemented as instances of this class.
 * Strings are constant; their values cannot be changed after they are created. String buffers support mutable strings. Because String objects are immutable they can be shared. For example:
 * is equivalent to:
 * Here are some more examples of how strings can be used:
 * The class String includes methods for examining individual characters of the sequence, for comparing strings, for searching strings, for extracting substrings, and for creating a copy of a string with all characters translated to uppercase or to lowercase.
 * The Java language provides special support for the string concatenation operator (+), and for conversion of other objects to strings. String concatenation is implemented through the StringBuffer class and its append method. String conversions are implemented through the method toString, defined by Object and inherited by all classes in Java. For additional information on string concatenation and conversion, see Gosling, Joy, and Steele, The Java Language Specification.
 * Since: JDK1.0, CLDC 1.0 See Also:Object.toString(), StringBuffer, StringBuffer.append(boolean), StringBuffer.append(char), StringBuffer.append(char[]), StringBuffer.append(char[], int, int), StringBuffer.append(int), StringBuffer.append(long), StringBuffer.append(java.lang.Object), StringBuffer.append(java.lang.String)
 */
@com.codename1.annotations.Fused
public final class String implements java.lang.CharSequence, Comparable<String> {
    
    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new Comparator<String>() {
        public int compare(String o1, String o2){
            return o1.compareToIgnoreCase(o2);
        }
    };
    
    private static ArrayList<String> str = new ArrayList<String>();

    // Compact-string storage: ONE backing reference holding EITHER a char[] (general UTF-16, the
    // original 2-byte representation -- a Unicode charAt is a direct 16-bit read, NO decode) OR a
    // byte[] (pure Latin-1, code units 0..255 -- the common ASCII case, 1 byte/char). The element
    // kind IS the coder: `value instanceof byte[]` means Latin-1. No second field, no length flag,
    // so the C String struct is unchanged (value was already a JAVA_OBJECT). @Fused fuses either
    // array kind for String.value (see FusedConstructor.stringCompactValueMatch); natives pick
    // byte vs char by a single array-class compare, hoisted out of character loops.
    private Object value;

    // THERE IS NO `offset` FIELD, ON PURPOSE. It was four bytes on every String
    // recording a window into the backing array, and it was ALWAYS ZERO: the only
    // constructor that ever set it non-zero was the (int,int,char[]) aliasing form,
    // which is package-private and had no caller anywhere -- verified in the emitted
    // C for the whole self-hosting corpus, where the symbol appeared only in its own
    // definition and declaration. Every other constructor, substring included, copies
    // its window (a fused String's value lives inside its own allocation block, so a
    // slice cannot point into the parent's), which is what made the field dead.
    //
    // Removing it is only worth anything together with nsString: 48 -> 44 still lands
    // in the same 48-byte BiBOP size class and buys literally nothing. Both together
    // take the struct to 32. See vm/benchmarks/memshape.sh.
    private final int count;

    private int hashCode;

    // cached native string
    private long nsString;
    private static final char[] ZERO_CHAR = new char[0];

    /* ---- BACKING-STORE PREDICATES -------------------------------------------
     * Every question about HOW the characters are stored goes through these four,
     * so the representation is decided in one place rather than at each use. The
     * array kind is the coder today -- a byte[] means Latin-1 -- but the inner
     * array is 24 bytes of header (length duplicating count, dimensions always 1,
     * primitiveSize the coder, dataOffset a constant, and two GC sentinels) on
     * every fused String, and only the class pointer in it carries information.
     * Consolidating the accesses is what lets that change without touching each
     * caller.
     */
    /* An INLINE String keeps its characters inside its own object and holds no
     * array at all, so value is null and the coder lives in the VM's class word.
     * These two natives are the only way Java can ask about that storage; every
     * other method here goes through them or through charInternal. */
    private native boolean cn1InlineLatin1();
    private native char cn1InlineCharAt(int index);

    /** Latin-1 storage: one byte per character, the character IS the byte. */
    private boolean isLatin1() {
        return value == null ? cn1InlineLatin1() : value instanceof byte[];
    }

    /** The Latin-1 bytes. Valid only under isLatin1() AND a non-inline store. */
    private byte[] latin1Value() {
        return (byte[]) value;
    }

    /** UTF-16 storage whose length is EXACTLY count -- the only shape shareable without a copy. */
    private boolean isUtf16Exact() {
        return value instanceof char[] && ((char[]) value).length == count;
    }

    /** The UTF-16 units. Valid only when the store is a char[]. */
    private char[] utf16Value() {
        return (char[]) value;
    }

    /** Character at logical index i (0-based); offset is applied here. */
    private char charInternal(int i) {
        Object v = value;
        if (v == null) {
            return cn1InlineCharAt(i);
        }
        return v instanceof byte[] ? (char) (((byte[]) v)[i] & 0xff) : ((char[]) v)[i];
    }

    /**
     * VM-INTERNAL fast path: wrap a freshly-built Latin-1 {@code byte[]} directly as a compact
     * String WITHOUT copying or a code-unit scan. Callers must guarantee every byte is a Latin-1
     * code unit (0..255) that IS the character, and must not retain {@code latin1Bytes} elsewhere
     * (ownership transfers). Number/format code that knows its output is ASCII (Long.toString,
     * Integer.toString, digit formatting) uses this to skip the char[] intermediate. Straight-line
     * NEWARRAY [B at the call site -> @Fused packs the bytes inline (stringCompactValueMatch).
     */
    static String latin1(byte[] latin1Bytes, int count) {
        return new String(count, latin1Bytes);
    }

    /** Aliasing ctor for {@link #latin1} -- distinct signature (int,byte[]) so it never collides
     *  with the UTF-8-decoding public String(byte[],...) ctors. Takes ownership, no copy. */
    private String(int count, byte[] latin1Bytes) {
        this.count = count;
        this.value = latin1Bytes;
    }
    
    // VM-INTERNAL compact string concatenation. The translator rewrites an all-String
    // makeConcat(WithConstants) (the common String interpolation / a+b+c shape once every
    // argument is already String-typed) into a cn1Concat call instead of the generic
    // StringBuilder helper. These helpers write directly to the immutable result,
    // avoiding a mutable builder and its growth buffer. Latin-1 parts stay compact;
    // a part containing a code unit above 255 selects UTF-16 storage.
    private static String cn1c(String s) { return s != null ? s : "null"; }

    // When every part is a compact Latin-1 String (the overwhelming common case), the native
    // cn1FusedConcatN builds the whole result in ONE fused allocation (byte[] inline in the String)
    // with no byte<->char conversion. Only if some part carries a code unit > 0xFF do we fall to the
    // char[] path here (rare). null args map to "null" like makeConcat. See nativeMethods.m.
    private static native String cn1FusedConcat2(String a, String b);
    private static native String cn1FusedConcat3(String a, String b, String c);
    private static native String cn1FusedConcat4(String a, String b, String c, String d);
    private static native String cn1FusedConcat5(String a, String b, String c, String d, String e);

    static String cn1Concat2(String a, String b) {
        a = cn1c(a); b = cn1c(b);
        if (a.isLatin1() && b.isLatin1()) {
            return cn1FusedConcat2(a, b);
        }
        char[] r = new char[a.count + b.count];
        a.getChars(0, a.count, r, 0);
        b.getChars(0, b.count, r, a.count);
        return new String(r);
    }

    static String cn1Concat3(String a, String b, String c) {
        a = cn1c(a); b = cn1c(b); c = cn1c(c);
        if (a.isLatin1() && b.isLatin1() && c.isLatin1()) {
            return cn1FusedConcat3(a, b, c);
        }
        int ca = a.count, cb = b.count;
        char[] r = new char[ca + cb + c.count];
        a.getChars(0, ca, r, 0);
        b.getChars(0, cb, r, ca);
        c.getChars(0, c.count, r, ca + cb);
        return new String(r);
    }

    static String cn1Concat4(String a, String b, String c, String d) {
        a = cn1c(a); b = cn1c(b); c = cn1c(c); d = cn1c(d);
        if (a.isLatin1() && b.isLatin1()
                && c.isLatin1() && d.isLatin1()) {
            return cn1FusedConcat4(a, b, c, d);
        }
        int ca = a.count, cb = b.count, cc = c.count;
        char[] r = new char[ca + cb + cc + d.count];
        a.getChars(0, ca, r, 0);
        b.getChars(0, cb, r, ca);
        c.getChars(0, cc, r, ca + cb);
        d.getChars(0, d.count, r, ca + cb + cc);
        return new String(r);
    }

    static String cn1Concat5(String a, String b, String c, String d, String e) {
        a = cn1c(a); b = cn1c(b); c = cn1c(c); d = cn1c(d); e = cn1c(e);
        if (a.isLatin1() && b.isLatin1() && c.isLatin1()
                && d.isLatin1() && e.isLatin1()) {
            return cn1FusedConcat5(a, b, c, d, e);
        }
        int ca = a.count, cb = b.count, cc = c.count, cd = d.count;
        char[] r = new char[ca + cb + cc + cd + e.count];
        a.getChars(0, ca, r, 0);
        b.getChars(0, cb, r, ca);
        c.getChars(0, cc, r, ca + cb);
        d.getChars(0, cd, r, ca + cb + cc);
        e.getChars(0, e.count, r, ca + cb + cc + cd);
        return new String(r);
    }

    /**
     * Initializes a newly created String object so that it represents an empty character sequence.
     */
    public String(){
        value = ZERO_CHAR;
        count = 0;        
    }

    /**
     * Construct a new String by converting the specified array of bytes using the platform's default character encoding. The length of the new String is a function of the encoding, and hence may not be equal to the length of the byte array.
     * bytes - The bytes to be converted into characters
     * JDK1.1
     */
    public String(byte[] bytes){
        this(bytes, 0, bytes.length);
    }

    /**
     * Construct a new String by converting the specified subarray of bytes using the platform's default character encoding. The length of the new String is a function of the encoding, and hence may not be equal to the length of the subarray.
     * bytes - The bytes to be converted into charactersoff - Index of the first byte to convertlen - Number of bytes to convert
     * JDK1.1
     */
    public String(byte[] bytes, int off, int len){
        this(bytesToChars(bytes, off, len, "UTF-8"));
    }

    /**
     * Construct a new String by converting the specified subarray of bytes using the specified character encoding. The length of the new String is a function of the encoding, and hence may not be equal to the length of the subarray.
     * bytes - The bytes to be converted into charactersoff - Index of the first byte to convertlen - Number of bytes to convertenc - The name of a character encoding
     * - If the named encoding is not supported
     * JDK1.1
     */
    public String(byte[] bytes, int off, int len, java.lang.String enc) throws java.io.UnsupportedEncodingException{
        this(bytesToChars(bytes, off, len, enc));
    }
    
    public String(byte[] bytes, java.nio.charset.Charset charset) throws java.io.UnsupportedEncodingException {
        this(bytes, 0, bytes.length, charset.displayName());
    }

    /**
     * Construct a new String by converting the specified array of bytes using the specified character encoding. The length of the new String is a function of the encoding, and hence may not be equal to the length of the byte array.
     * bytes - The bytes to be converted into charactersenc - The name of a supported character encoding
     * - If the named encoding is not supported
     * JDK1.1
     */
    public String(byte[] bytes, java.lang.String enc) throws java.io.UnsupportedEncodingException{
        this(bytesToChars(bytes, 0, bytes.length, enc));
    }

    private static native char[] bytesToChars(byte[] b, int off, int len, String encoding); 
    
    /**
     * Allocates a new String so that it represents the sequence of characters currently contained in the character array argument. The contents of the character array are copied; subsequent modification of the character array does not affect the newly created string.
     * value - the initial value of the string.
     * - if value is null.
     */
    public String(char[] value){
        this(value, 0, value.length);
    }

    private StringIndexOutOfBoundsException failedBoundsCheck(int arrayLength, int offset, int count) {
        throw new StringIndexOutOfBoundsException(count);
    }

    /**
     * Allocates a new String that contains characters from a subarray of the character array argument. The offset argument is the index of the first character of the subarray and the count argument specifies the length of the subarray. The contents of the subarray are copied; subsequent modification of the character array does not affect the newly created string.
     * value - array that is the source of characters.offset - the initial offset.count - the length.
     * - if the offset and count arguments index characters outside the bounds of the value array.
     * - if value is null.
     */
    public String(char[] data, int offset, int charCount){
        if ((offset | charCount) < 0 || charCount > data.length - offset) {
            throw failedBoundsCheck(data.length, offset, charCount);
        }
        this.count = charCount;
        // WRITTEN AS ONE UNCONDITIONAL `this.value = new byte[len]` ON PURPOSE. That is
        // the exact shape FusedConstructor collects -- ALOAD 0; <length over the ctor's
        // own parameters>; NEWARRAY byte; PUTFIELD value -- and stringCompactValueMatch
        // already admits String.value for a byte NEWARRAY, so the allocation site packs
        // the characters INSIDE the String instead of allocating a second object.
        //
        // The previous form asked toLatin1() for the array, and a NEWARRAY that happens
        // inside another method is invisible to a pass that reads this constructor's own
        // body: every String built from a char[] cost two objects. That is not a rare
        // path -- ASM's ClassReader calls new String(char[], 0, n) for every constant
        // pool string of every class it reads (verified: String."<init>":([CII)V in
        // ClassReader), which on the self-hosting corpus is the largest single source of
        // byte[] in the program.
        //
        // The wide branch below replaces value and leaves the inline bytes unused. That
        // is the right trade and not a leak -- the unused bytes sit inside the String's
        // own block, so they are freed with it -- because Latin-1 is the overwhelming
        // case: the corpus allocates 647,383 byte[] against 3,967 char[].
        this.value = new byte[charCount];
        if (!packLatin1(data, offset, charCount)) {
            char[] wide = new char[charCount];
            System.arraycopy(data, offset, wide, 0, charCount);
            this.value = wide;
        }
    }

    /**
     * Packs data[offset..offset+n) into the Latin-1 byte[] already installed in
     * {@code value}, in ONE pass. Returns false at the first code unit above 0xFF,
     * leaving the partially written bytes for the caller to discard -- it only ever
     * calls this on an array it is about to replace.
     */
    private boolean packLatin1(char[] data, int offset, int n) {
        byte[] out = latin1Value();
        for (int i = 0; i < n; i++) {
            char c = data[offset + i];
            if (c > 0xFF) {
                return false;
            }
            out[i] = (byte) c;
        }
        return true;
    }

    /**
     * UNCHECKED aliasing constructor for VM-internal callers that construct the
     * exact-length private array themselves (StringBuilder's copy-on-write
     * share, concat's scratch buffer): no bounds guard, so the translator can
     * inline it to three plain field stores at the allocation site. Same
     * ownership rules as the guarded aliasing constructor above.
     */
    String(char[] data, int charCount) {
        this.value = data;
        this.count = charCount;
    }

    /**
     * Slice constructor. A String may be fused with its backing primitive array,
     * so a slice must own its backing storage instead of pointing into the
     * parent's allocation block. Preserve the parent's compact representation
     * while copying the requested logical window.
     */
    /**
     * Slice into a FUSED String -- one allocation for the object and its characters,
     * instead of the object plus a separate backing array.
     *
     * substring is the busiest String producer in this VM: 290,581 calls on the
     * self-hosting corpus, averaging twelve characters, and 866 call sites in the
     * framework core alone. At that length the separate array is mostly HEADER -- 32
     * bytes of array header for 12 bytes of payload -- so fusing removes both an
     * allocation and the header, and keeps the parent's coder so a compact string
     * stays compact.
     *
     * Returns null when a fused block is unavailable (the slice is too large for the
     * page heap, or BiBOP is off), and the caller takes the two-object path below,
     * which is correct for any length.
     */
    private native String cn1SubstringFused(int off, int n);

    private String(String parent, int newOffset, int newCount) {
        Object parentValue = parent.value;
        if (parentValue == null) {
            // Parent keeps its characters inline, so there is no array to copy from.
            // Match the parent's coder rather than widening: a Latin-1 source must
            // not become UTF-16 just because it was fused.
            if (parent.isLatin1()) {
                byte[] copy = new byte[newCount];
                for (int i = 0; i < newCount; i++) {
                    copy[i] = (byte) parent.charInternal(newOffset + i);
                }
                this.value = copy;
            } else {
                char[] copy = new char[newCount];
                for (int i = 0; i < newCount; i++) {
                    copy[i] = parent.charInternal(newOffset + i);
                }
                this.value = copy;
            }
            this.count = newCount;
            return;
        }
        if (parentValue instanceof byte[]) {
            byte[] copy = new byte[newCount];
            System.arraycopy((byte[]) parentValue, newOffset, copy, 0, newCount);
            this.value = copy;
        } else {
            char[] copy = new char[newCount];
            System.arraycopy((char[]) parentValue, newOffset, copy, 0, newCount);
            this.value = copy;
        }
        this.count = newCount;
    }

    /**
     * Initializes a newly created String object so that it represents the same sequence of characters as the argument; in other words, the newly created string is a copy of the argument string.
     * value - a String.
     */
    public String(java.lang.String value){
        // COPY, do not share: the source may be a FUSED string whose value array
        // lives inside the source's own allocation block -- sharing it would let
        // this string outlive the block it points into.
        //
        // PRESERVE THE SOURCE'S REPRESENTATION, exactly as the slice constructor
        // above does. This used to copy via toCharArray(), which was wrong twice
        // over: it allocated a char[] the constructor then had to keep, and it
        // DE-COMPACTED a Latin-1 source into UTF-16, so copying a compact string
        // doubled its storage. Unlike compacting String(char[],int,int) -- measured
        // and reverted, because it makes readers pay in toCharArray() -- this
        // direction only ever matches what the source already is, so no reader can
        // be pushed off a fast path it was on.
        this.count = value.count;
        Object src = value.value;
        if (src == null) {
            // Same as the substring constructor above: an inline source has no array.
            if (value.isLatin1()) {
                byte[] copy = new byte[count];
                for (int i = 0; i < count; i++) {
                    copy[i] = (byte) value.charInternal(i);
                }
                this.value = copy;
            } else {
                char[] copy = new char[count];
                for (int i = 0; i < count; i++) {
                    copy[i] = value.charInternal(i);
                }
                this.value = copy;
            }
            return;
        }
        if (src instanceof byte[]) {
            byte[] copy = new byte[count];
            System.arraycopy((byte[]) src, 0, copy, 0, count);
            this.value = copy;
        } else {
            char[] copy = new char[count];
            System.arraycopy((char[]) src, 0, copy, 0, count);
            this.value = copy;
        }
    }

    /**
     * Allocates a new string that contains the sequence of characters currently contained in the string buffer argument. The contents of the string buffer are copied; subsequent modification of the string buffer does not affect the newly created string.
     * buffer - a StringBuffer.
     * - If buffer is null.
     */
    public String(java.lang.StringBuffer buffer) {
        this(buffer.toString());
    }

    public String(java.lang.StringBuilder buffer) {
        this(buffer.toString());
    }

    /**
     * Returns the character at the specified index. An index ranges from 0 to length() - 1. The first character of the sequence is at index 0, the next at index 1, and so on, as for array indexing.
     */
    public final native char charAt(int index);//{
//        return value[offset + index];
//    }

    /**
     * Compares two strings lexicographically. The comparison is based on the Unicode value of each character in the strings. The character sequence represented by this String object is compared lexicographically to the character sequence represented by the argument string. The result is a negative integer if this String object lexicographically precedes the argument string. The result is a positive integer if this String object lexicographically follows the argument string. The result is zero if the strings are equal; compareTo returns 0 exactly when the
     * method would return true.
     * This is the definition of lexicographic ordering. If two strings are different, then either they have different characters at some index that is a valid index for both strings, or their lengths are different, or both. If they have different characters at one or more index positions, let k be the smallest such index; then the string whose character at position k has the smaller value, as determined by using the < operator, lexicographically precedes the other string. In this case, compareTo returns the difference of the two character values at position k in the two string -- that is, the value:
     * this.charAt(k)-anotherString.charAt(k) If there is no index position at which they differ, then the shorter string lexicographically precedes the longer string. In this case, compareTo returns the difference of the lengths of the strings -- that is, the value: this.length()-anotherString.length()
     */
    public native int compareTo(java.lang.String anotherString);
    
    /* Folded in place for ASCII, which is what this is nearly always asked about.
     * It used to be
     *     toLowerCase().compareTo(anotherString.toLowerCase())
     * which allocated TWO whole strings to answer a comparison -- and
     * CASE_INSENSITIVE_ORDER is this method, so sorting a list of n strings allocated
     * on the order of 2*n*log(n) of them and handed every one to the collector.
     *
     * The non-ASCII case still takes that path, and must. The obvious rewrite folds
     * every character through Character.toUpperCase/toLowerCase the way the JDK
     * specifies this method, and it is WRONG HERE: Character.toLowerCase(char) in
     * this VM maps A-Z and returns everything else unchanged, while
     * String.toLowerCase goes through NSString (or towlower) and performs the full
     * simple mapping. So the per-character fold answered that LATIN CAPITAL LETTER I
     * WITH DOT ABOVE (U+0130) lower-cases to itself, where Java gives 'i', and
     * "zzz".compareToIgnoreCase("\u0130...") came out with the opposite SIGN --
     * caught by StrQueryT against a real JDK, not by reasoning.
     *
     * That gap in Character is a real defect and worth fixing on its own; depending
     * on it here would have shipped a wrong ordering to get an allocation back.
     *
     * Equivalence of the fast path: for characters below 0x80 the full mapping IS
     * the ASCII mapping, so folding them here gives the same answer the old path
     * gave, including the length tiebreak. The moment a differing pair involves a
     * character at or above 0x80 the answer is deferred to the old path, and every
     * position already passed was ASCII and equal under either fold, so restarting
     * there cannot change the result. */
    public int compareToIgnoreCase(java.lang.String anotherString) {
        if (anotherString == this) {
            return 0;
        }
        int n1 = count;
        int n2 = anotherString.count;
        int min = n1 < n2 ? n1 : n2;
        for (int i = 0; i < min; i++) {
            char c1 = charInternal(i);
            char c2 = anotherString.charInternal(i);
            if (c1 == c2) {
                continue;
            }
            if (c1 < 0x80 && c2 < 0x80) {
                if (c1 >= 'A' && c1 <= 'Z') {
                    c1 = (char) (c1 + ('a' - 'A'));
                }
                if (c2 >= 'A' && c2 <= 'Z') {
                    c2 = (char) (c2 + ('a' - 'A'));
                }
                if (c1 != c2) {
                    return c1 - c2;
                }
                continue;
            }
            return toLowerCase().compareTo(anotherString.toLowerCase());
        }
        return n1 - n2;
    }
    
    /* Compared in place. Both of these used to build a String out of the argument
     * purely to hand it to equals -- an allocation, and a full copy of the content,
     * to answer a question that needs neither. The String case still short circuits
     * on equals, which is native and compares words at a time. */
    public boolean contentEquals(CharSequence cs) {
        if (cs == null) {
            return false;
        }
        if (cs instanceof String) {
            return equals(cs);
        }
        if (cs.length() != count) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            if (charInternal(i) != cs.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean contentEquals(StringBuffer buf) {
        if (buf == null) {
            return false;
        }
        // StringBuffer is synchronized, so the length and the characters are read
        // under one lock rather than one per character -- and without building a
        // String of the whole buffer just to compare it.
        synchronized (buf) {
            if (buf.length() != count) {
                return false;
            }
            for (int i = 0; i < count; i++) {
                if (charInternal(i) != buf.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    public static String copyValueOf(char[] data) {
        return new String(data);
    }
    
    public static String copyValueOf(char[] data, int offset, int count) {
        return new String(data, offset, count);
    }

    /**
     * Concatenates the specified string to the end of this string.
     * If the length of the argument string is 0, then this String object is returned. Otherwise, a new String object is created, representing a character sequence that is the concatenation of the character sequence represented by this String object and the character sequence represented by the argument string.
     * Examples:
     * "cares".concat("s") returns "caress" "to".concat("get").concat("her") returns "together"
     */
    public java.lang.String concat(java.lang.String str) {
        if (str.count == 0) return this;
        return cn1Concat2(this, str);
    }

    /**
     * Tests if this string ends with the specified suffix.
     */
    public boolean endsWith(java.lang.String suffix){
        if(suffix.length() > length()) {
            return false;
        }
        int offset = suffix.length() - 1;
        for(int iter = length() - 1 ; offset >= 0 ; iter--) {
            if(charInternal(iter) != suffix.charInternal(offset)) {
                return false;
            }
            offset--;
        }
        return true;
    }

    /**
     * Compares this string to the specified object. The result is true if and only if the argument is not null and is a String object that represents the same sequence of characters as this object.
     */
    public native boolean equals(java.lang.Object anObject);
    /*public boolean equals(java.lang.Object anObject){
        if(anObject == this) {
            return true;
        }
        if(anObject == null || anObject.getClass() != getClass()) {
            return false;
        }
        String s = (String)anObject;
        if(s.length() != length()) {
            return false;
        }
        for(int iter = 0 ; iter < count ; iter++) {
            if(value[offset + iter] != s.value[s.offset + iter]) {
                return false;
            }
        }
        return true;
    }*/

    /**
     * Compares this String to another String, ignoring case considerations. Two strings are considered equal ignoring case if they are of the same length, and corresponding characters in the two strings are equal ignoring case.
     * Two characters c1 and c2 are considered the same, ignoring case if at least one of the following is true: The two characters are the same (as compared by the == operator). Applying the method Character.toUpperCase(char) to each character produces the same result. Applying the method Character.toLowerCase(char) to each character produces the same result.
     */
    public native boolean equalsIgnoreCase(java.lang.String s);

    /**
     * Convert this String into bytes according to the platform's default character encoding, storing the result into a new byte array.
     */
    public byte[] getBytes(){
        try {
            return getBytes("UTF-8"); 
        } catch(java.io.UnsupportedEncodingException e) {
            // dumbass checked exception
            return null;
        }
    }

    private static native byte[] charsToBytes(char[] arr, char[] encoding);
    private native byte[] compactBytes(String encoding);
    
    /**
     * Convert this String into bytes according to the specified character encoding, storing the result into a new byte array.
     */
    public byte[] getBytes(java.lang.String enc) throws java.io.UnsupportedEncodingException{
        if(isLatin1()) {
            byte[] compact = compactBytes(enc);
            if(compact != null) return compact;
        }
        if(isUtf16Exact()) {
            if(enc == null) {
                return charsToBytes(toCharNoCopy(), null);
            }
            return charsToBytes(toCharNoCopy(), enc.toCharNoCopy()); 
        } 
        if(enc == null) {
            return charsToBytes(toCharArray(), null); 
        }
        return charsToBytes(toCharArray(), enc.toCharNoCopy()); 
    }
    
    public byte[] getBytes(Charset charset) throws java.io.UnsupportedEncodingException {
        return getBytes(charset.displayName());
    }

    /**
     * Copies characters from this string into the destination character array.
     * The first character to be copied is at index srcBegin; the last character to be copied is at index srcEnd-1 (thus the total number of characters to be copied is srcEnd-srcBegin). The characters are copied into the subarray of dst starting at index dstBegin and ending at index:
     * dstbegin + (srcEnd-srcBegin) - 1
     */
    public native void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin);//{
//        for(int iter = srcBegin ; iter < srcEnd ; iter++) {
//            dst[dstBegin] = value[offset + iter];
//            dstBegin++;
//        }
//    }

    /**
     * Returns a hashcode for this string. The hashcode for a String object is computed as s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1] using int arithmetic, where s[i] is the
     * th character of the string, n is the length of the string, and ^ indicates exponentiation. (The hash value of the empty string is zero.)
     */
    public native int hashCode();

    /**
     * Returns the index within this string of the first occurrence of the specified character. If a character with value ch occurs in the character sequence represented by this String object, then the index of the first such occurrence is returned -- that is, the smallest value
     * such that: this.charAt(
     * ) == ch is true. If no such character occurs in this string, then -1 is returned.
     */
    public int indexOf(int ch){
        return indexOf(ch, 0); 
    }

    /**
     * Returns the index within this string of the first occurrence of the specified character, starting the search at the specified index.
     * If a character with value ch occurs in the character sequence represented by this String object at an index no smaller than fromIndex, then the index of the first such occurrence is returned--that is, the smallest value k such that:
     * (this.charAt(
     * ) == ch) &amp;&amp; (
     * >= fromIndex) is true. If no such character occurs in this string at or after position fromIndex, then -1 is returned.
     * There is no restriction on the value of fromIndex. If it is negative, it has the same effect as if it were zero: this entire string may be searched. If it is greater than the length of this string, it has the same effect as if it were equal to the length of this string: -1 is returned.
     */
    public native int indexOf(int ch, int fromIndex);//{
//        for(int iter = offset + fromIndex ; iter < count + offset ; iter++) {
//            if(value[iter] == ch) {
//                return iter - offset;
//            }
//        }
//        return -1; 
//    }

    /**
     * Returns the index within this string of the first occurrence of the specified substring. The integer returned is the smallest value
     * such that: this.startsWith(str,
     * ) is true.
     */
    public int indexOf(java.lang.String string){
        int start = 0;
        int subCount = string.count;
        int _count = count;
        if (subCount > 0) {
            if (subCount > _count) {
                return -1;
            }
            char firstChar = string.charInternal(0);
            while (true) {
                int i = indexOf(firstChar, start);
                if (i == -1 || subCount + i > _count) {
                    return -1; // handles subCount > count || start >= count
                }
                int k = 1;
                while (k < subCount && charInternal(i + k) == string.charInternal(k)) {
                    // Intentionally empty
                    k++;
                }
                if (k == subCount) {
                    return i;
                }
                start = i + 1;
            }
        }
        return start < _count ? start : _count;
    }

    /**
     * Returns the index within this string of the first occurrence of the specified substring, starting at the specified index. The integer returned is the smallest value
     * such that: this.startsWith(str,
     * ) &amp;&amp; (
     * >= fromIndex) is true.
     * There is no restriction on the value of fromIndex. If it is negative, it has the same effect as if it were zero: this entire string may be searched. If it is greater than the length of this string, it has the same effect as if it were equal to the length of this string: -1 is returned.
     */
    public int indexOf(java.lang.String subString, int start){
        if (start < 0) {
            start = 0;
        }
        int subCount = subString.count;
        int _count = count;
        if (subCount > 0) {
            if (subCount + start > _count) {
                return -1;
            }
            char firstChar = subString.charInternal(0);
            while (true) {
                int i = indexOf(firstChar, start);
                if (i == -1 || subCount + i > _count) {
                    return -1; // handles subCount > count || start >= count
                }
                int k = 1;
                while (k < subCount && charInternal(i + k) == subString.charInternal(k)) {
                    // Intentionally empty
                    k++;
                }
                if (k == subCount) {
                    return i;
                }
                start = i + 1;
            }
        }
        return start < _count ? start : _count;
    }

    /**
     * Returns a canonical representation for the string object.
     * A pool of strings, initially empty, is maintained privately by the class String.
     * When the intern method is invoked, if the pool already contains a string equal to this String object as determined by the equals(Object) method, then the string from the pool is returned. Otherwise, this String object is added to the pool and a reference to this String object is returned.
     * It follows that for any two strings s and t, s.intern()==t.intern() is true if and only if s.equals(t) is true.
     * All literal strings and string-valued constant expressions are interned. String literals are defined in Section 3.10.5 of the Java Language Specification
     */
    public java.lang.String intern() {
        // Synchronized on the shared pool: intern() must be atomic so two threads canonicalizing
        // equal strings concurrently return the same object (and never corrupt the pool by adding
        // during another thread's traversal). The JDK contract requires s.intern()==t.intern()
        // whenever s.equals(t).
        synchronized(str) {
            int off = str.indexOf(this);
            if(off > -1) {
                return str.get(off);
            }
            str.add(this);
            return this;
        }
    }

    /**
     * Returns the index within this string of the last occurrence of the specified character. That is, the index returned is the largest value
     * such that: this.charAt(
     * ) == ch is true. The String is searched backwards starting at the last character.
     */
    public int lastIndexOf(int ch){
        for(int iter = count - 1 ; iter >= 0 ; iter--) {
            if(charInternal(iter) == ch) {
                return iter;
            }
        }
        return -1;
    }

    /**
     * Returns the index within this string of the last occurrence of the specified character, searching backward starting at the specified index. That is, the index returned is the largest value
     * such that: (this.charAt(k) == ch) &amp;&amp; (k  &lt;= fromIndex) is true.
     */
    public int lastIndexOf(int ch, int start){
        int _count = count;
        if (start >= 0) {
            if (start >= _count) {
                start = _count - 1;
            }
            for (int i = start; i >= 0; --i) {
                if (charInternal(i) == ch) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Searches in this string for the last index of the specified string. The
     * search for the string starts at the end and moves towards the beginning
     * of this string.
     * 
     * @param string
     *            the string to find.
     * @return the index of the first character of the specified string in this
     *         string, -1 if the specified string is not a substring.
     * @throws NullPointerException
     *             if {@code string} is {@code null}.
     */
    public int lastIndexOf(String string) {
        return lastIndexOf(string, length());
    }

    /**
     * Searches in this string for the index of the specified string. The search
     * for the string starts at the specified offset and moves towards the
     * beginning of this string.
     * 
     * @param subString
     *            the string to find.
     * @param start
     *            the starting offset.
     * @return the index of the first character of the specified string in this
     *         string , -1 if the specified string is not a substring.
     * @throws NullPointerException
     *             if {@code subString} is {@code null}.
     */
    public int lastIndexOf(String subString, int start) {
        int count = length();
        int subCount = subString.length();
        if (subCount <= count && start >= 0) {
            if (subCount > 0) {
                if (start > count - subCount) {
                    start = count - subCount;
                }
                // count and subCount are both >= 1
                char[] target = subString.toCharArray();
                int subOffset = 0;
                char firstChar = target[subOffset];
                int end = subOffset + subCount;
                while (true) {
                    int i = lastIndexOf(firstChar, start);
                    if (i == -1) {
                        return -1;
                    }
                    int o1 = i, o2 = subOffset;
                    while (++o2 < end && charInternal(++o1) == target[o2]) {
                        // Intentionally empty
                    }
                    if (o2 == end) {
                        return i;
                    }
                    start = i - 1;
                }
            }
            return start < count ? start : count;
        }
        return -1;
    }

    
    /**
     * Returns the length of this string. The length is equal to the number of 16-bit Unicode characters in the string.
     */
    public int length(){
        return count;
    }
    
    /**
     * Compares the specified string to this string and compares the specified
     * range of characters to determine if they are the same.
     *
     * @param thisStart
     *            the starting offset in this string.
     * @param string
     *            the string to compare.
     * @param start
     *            the starting offset in the specified string.
     * @param length
     *            the number of characters to compare.
     * @return {@code true} if the ranges of characters are equal, {@code false}
     *         otherwise
     * @throws NullPointerException
     *             if {@code string} is {@code null}.
     */
    public boolean regionMatches(int thisStart, String string, int start, int length) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        if (start < 0 || string.count - start < length) {
            return false;
        }
        if (thisStart < 0 || count - thisStart < length) {
            return false;
        }
        if (length <= 0) {
            return true;
        }
        for (int i = 0; i < length; ++i) {
            if (charInternal(thisStart + i) != string.charInternal(start + i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests if two string regions are equal.
     * A substring of this String object is compared to a substring of the argument other. The result is true if these substrings represent character sequences that are the same, ignoring case if and only if ignoreCase is true. The substring of this String object to be compared begins at index toffset and has length len. The substring of other to be compared begins at index ooffset and has length len. The result is false if and only if at least one of the following is true: toffset is negative. ooffset is negative. toffset+len is greater than the length of this String object. ooffset+len is greater than the length of the other argument. There is some nonnegative integer k less than len such that:
     * this.charAt(toffset+k) != other.charAt(ooffset+k) ignoreCase is true and there is some nonnegative integer
     * less than len such that: Character.toLowerCase(this.charAt(toffset+k)) != Character.toLowerCase(other.charAt(ooffset+k)) and: Character.toUpperCase(this.charAt(toffset+k)) != Character.toUpperCase(other.charAt(ooffset+k))
     */
    public boolean regionMatches(boolean ignoreCase, int thisStart, String string, int start, int length) {
        if (!ignoreCase) {
            return regionMatches(thisStart, string, start, length);
        }
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        if (thisStart < 0 || length > count - thisStart) {
            return false;
        }
        if (start < 0 || length > string.count - start) {
            return false;
        }
        int end = thisStart + length;
        while (thisStart < end) {
            char c1 = charInternal(thisStart++);
            char c2 = string.charInternal(start++);
            if (c1 != c2 && foldCase(c1) != foldCase(c2)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * This isn't equivalent to either of ICU's u_foldCase case folds, and thus any of the Unicode
     * case folds, but it's what the RI uses.
     */
    private char foldCase(char ch) {
        if (ch < 128) {
            if ('A' <= ch && ch <= 'Z') {
                return (char) (ch + ('a' - 'A'));
            }
            return ch;
        }
        return Character.toLowerCase(Character.toUpperCase(ch));
    }

    /**
     * Returns a new string resulting from replacing all occurrences of oldChar in this string with newChar.
     * If the character oldChar does not occur in the character sequence represented by this String object, then a reference to this String object is returned. Otherwise, a new String object is created that represents a character sequence identical to the character sequence represented by this String object, except that every occurrence of oldChar is replaced by an occurrence of newChar.
     * Examples:
     * "mesquite in your cellar".replace('e', 'o') returns "mosquito in your collar" "the war of baronets".replace('r', 'y') returns "the way of bayonets" "sparring with a purple porpoise".replace('p', 't') returns "starring with a turtle tortoise" "JonL".replace('q', 'x') returns "JonL" (no change)
     */
    public java.lang.String replace(char oldChar, char newChar) {
        if (oldChar == newChar) return this;
        int first = indexOf(oldChar);
        if (first < 0) return this;
        if (isLatin1() && newChar <= 255) {
            // Reads through charInternal rather than the backing array: an inline
            // String has no array to take. cn1InlStrReplace is the fast path for
            // both shapes; this stays correct for whatever it declines.
            byte[] result = new byte[count];
            for (int i = 0; i < count; i++) {
                int ch = charInternal(i);
                result[i] = (byte) (ch == oldChar ? newChar : ch);
            }
            return latin1(result, count);
        }
        char[] result = toCharArray();
        for (int i = first; i < count; i++) if (result[i] == oldChar) result[i] = newChar;
        return new String(result);
    }

    /**
     * Replaces each substring of this string that matches the literal target sequence with the specified literal replacement sequence.
     * The replacement proceeds from the beginning of the string to the end, for example, replacing "aa" with "b" in the string "aaa" will result in "ba" rather than "ab".
     */
    public java.lang.String replace(java.lang.CharSequence target, java.lang.CharSequence replacement) {
        if (target == null) {
            throw new NullPointerException("target");
        }
        if (replacement == null) {
            throw new NullPointerException("replacement");
        }
        java.lang.String targetStr = target.toString();
        java.lang.String replacementStr = replacement.toString();
        int targetLen = targetStr.length();
        if (targetLen == 0) {
            int len = count;
            StringBuilder sb = new StringBuilder(len + (len + 1) * replacementStr.length());
            sb.append(replacementStr);
            for (int i = 0; i < len; i++) {
                sb.append(charInternal(i));
                sb.append(replacementStr);
            }
            return sb.toString();
        }
        int idx = indexOf(targetStr);
        if (idx < 0) {
            return this;
        }
        // No toCharArray. That allocated a char[] as long as this string -- twice
        // its bytes when it is Latin-1 -- purely to feed append(char[],int,int),
        // when append(CharSequence,int,int) reads the ranges straight out of this
        // string and keeps the builder's own compact representation.
        StringBuilder sb = new StringBuilder(count);
        int prev = 0;
        while (idx >= 0) {
            sb.append(this, prev, idx);
            sb.append(replacementStr);
            prev = idx + targetLen;
            idx = indexOf(targetStr, prev);
        }
        sb.append(this, prev, count);
        return sb.toString();
    }

    /**
     * Tests if this string starts with the specified prefix.
     */
    public boolean startsWith(java.lang.String prefix){
        return startsWith(prefix, 0);
    }

    /**
     * Tests if this string starts with the specified prefix beginning at the specified index.
     */
    public boolean startsWith(java.lang.String prefix, int toffset){
        if(toffset + prefix.count > count) {
            return false;
        }
        for(int iter = 0 ; iter < prefix.count ; iter++) {
            if(prefix.charInternal(iter) != charInternal(iter + toffset)) {
                return false;
            }
        }
        return true; 
    }

    /**
     * Returns a new string that is a substring of this string. The substring begins with the character at the specified index and extends to the end of this string.
     * Examples:
     * "unhappy".substring(2) returns "happy" "Harbison".substring(3) returns "bison" "emptiness".substring(9) returns "" (an empty string)
     */
    public java.lang.String substring(int start){
        if (start == 0) {
            return this;
        }
        if (start >= 0 && start <= count) {
            String fused = cn1SubstringFused(start, count - start);
            return fused != null ? fused : new String(this, start, count - start);
        }
        throw new ArrayIndexOutOfBoundsException(start);
    }

    /**
     * Returns a new string that is a substring of this string. The substring begins at the specified beginIndex and extends to the character at index endIndex - 1. Thus the length of the substring is endIndex-beginIndex.
     * Examples:
     * "hamburger".substring(4, 8) returns "urge" "smiles".substring(1, 5) returns "mile"
     */
    public java.lang.String substring(int start, int end) {
        if (start == 0 && end == count) {
            return this;
        }
        // NOTE last character not copied!
        // Fast range check.
        if (start >= 0 && start <= end && end <= count) {
            String fused = cn1SubstringFused(start, end - start);
            return fused != null ? fused : new String(this, start, end - start);
        }
        throw new ArrayIndexOutOfBoundsException(start);
    }

    private char[] toCharNoCopy() {
        if(isUtf16Exact()) {
            return utf16Value();
        }
        return toCharArray();
    }
    
    /**
     * Converts this string to a new character array.
     */
    public char[] toCharArray(){
        char[] buffer = new char[count];
        if (value == null) {
            // Inline: no array exists to copy from, so decode through the VM.
            for (int i = 0; i < count; i++) {
                buffer[i] = cn1InlineCharAt(i);
            }
            return buffer;
        }
        if (isLatin1()) {
            byte[] b = latin1Value();
            for (int i = 0; i < count; i++) {
                buffer[i] = (char) (b[i] & 0xff);
            }
        } else {
            System.arraycopy(utf16Value(), 0, buffer, 0, count);
        }
        return buffer;
    }

    /**
     * Converts all of the characters in this String to lower case.
     */
//    public java.lang.String toLowerCase(){
//        char[] c = new char[length()];
//        for(int iter = 0 ; iter < count ; iter++) {
//            c[iter] = Character.toLowerCase(value[offset + iter]);
//        }
//        return new String(c);
//    }
    
    public native java.lang.String toLowerCase();

    /**
     * This object (which is already a string!) is itself returned.
     */
    public native java.lang.String toString();

    /**
     * Converts all of the characters in this String to upper case.
     */
//    public java.lang.String toUpperCase(){
//        char[] c = new char[length()];
//        for(int iter = 0 ; iter < count ; iter++) {
//            c[iter] = Character.toUpperCase(value[offset + iter]);
//        }
//        return new String(c);
//    }

    public native java.lang.String toUpperCase();
    
    public java.lang.String toUpperCase(java.util.Locale locale) {
        return toUpperCase();
    }
    
    /**
     * Removes white space from both ends of this string.
     * If this String object represents an empty character sequence, or the first and last characters of character sequence represented by this String object both have codes greater than 'u0020' (the space character), then a reference to this String object is returned.
     * Otherwise, if there is no character with a code greater than 'u0020' in the string, then a new String object representing an empty string is created and returned.
     * Otherwise, let k be the index of the first character in the string whose code is greater than 'u0020', and let m be the index of the last character in the string whose code is greater than 'u0020'. A new String object is created, representing the substring of this string that begins with the character at index k and ends with the character at index m-that is, the result of this.substring(k,m+1).
     * This method may be used to trim whitespace from the beginning and end of a string; in fact, it trims all ASCII control characters as well.
     */
    public java.lang.String trim(){
        int lstart = 0, llast = count - 1;
        int lend = llast;
        while ((lstart <= lend) && (charInternal(lstart) <= ' ')) {
            lstart++;
        }
        while ((lend >= lstart) && (charInternal(lend) <= ' ')) {
            lend--;
        }
        if (lstart == 0 && lend == llast) {
            return this;
        }
        return new String(this, lstart, lend - lstart + 1);
    }

    /**
     * Returns the string representation of the boolean argument.
     */
    public static java.lang.String valueOf(boolean value){
        return value ? "true" : "false";
    }

    /**
     * Returns the string representation of the char argument.
     */
    public static java.lang.String valueOf(char value){
        String s = new String(new char[] { value }, 1);
        s.hashCode = value;
        return s;
    }

    public static java.lang.String valueOf(char[] data){
        return new String(data, 0, data.length);
    }

    /**
     * Returns the string representation of a specific subarray of the char array argument.
     * The offset argument is the index of the first character of the subarray. The count argument specifies the length of the subarray. The contents of the subarray are copied; subsequent modification of the character array does not affect the newly created string.
     */
    public static java.lang.String valueOf(char[] data, int start, int length){
        return new String(data, start, length);
    }

    /**
     * Returns the string representation of the double argument.
     * The representation is exactly the one returned by the Double.toString method of one argument.
     */
    public static java.lang.String valueOf(double value){
        return Double.toString(value);
    }

    /**
     * Returns the string representation of the float argument.
     * The representation is exactly the one returned by the Float.toString method of one argument.
     */
    public static java.lang.String valueOf(float f){
        return Float.toString(f);
    }

    /**
     * Returns the string representation of the int argument.
     * The representation is exactly the one returned by the Integer.toString method of one argument.
     */
    public static java.lang.String valueOf(int i){
        return Integer.toString(i);
    }

    /**
     * Returns the string representation of the long argument.
     * The representation is exactly the one returned by the Long.toString method of one argument.
     */
    public static java.lang.String valueOf(long l){
        return Long.toString(l);
    }

    /**
     * Returns the string representation of the Object argument.
     */
    public static java.lang.String valueOf(java.lang.Object obj){
        return obj == null ? "null" : obj.toString();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    // NOTE: String deliberately has NO finalize(). Its old finalizer existed only
    // to release the cached NSString peer (nsString) -- but a finalizer's mere
    // existence forces the VM's page sweep to walk every slot of every page that
    // ever held a dead String, on EVERY platform. The peer is now released by the
    // VM's reclaim path natively (see cn1ReleaseStringPeer in cn1_globals.m),
    // which only iOS pays for, and only on pages that actually hold peer-cached
    // strings.

    
    /**
     * Returns a formatted string using the specified format string and arguments.
     * Supports the {@code s b h c d o x e f g n %} conversions (and their uppercase
     * variants) with the {@code - + ' ' 0 , ( #} flags, width, precision, and the
     * {@code %n$} / {@code %<} argument selectors.
     */
    public static String format(String format, Object... args) {
        return StringFormatter.format(format, args);
    }
    
    public boolean contains(CharSequence seq) {
        if (seq == null) {
            return false;
        }
        // A String needs no conversion at all, which is the overwhelmingly common
        // call. Anything else still has to be materialised for indexOf, but only
        // then.
        if (seq instanceof String) {
            return indexOf((String) seq) != -1;
        }
        return indexOf(seq.toString()) != -1;
    }
    
    public boolean isEmpty() {
        return length() == 0;
    }
}
