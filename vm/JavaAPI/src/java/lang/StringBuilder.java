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

/** Mutable UTF-16 sequence with compact storage for Latin-1 code units. */
public final class StringBuilder implements CharSequence, Appendable {
    static final int INITIAL_CAPACITY = 16;
    private long cn1Storage;
    private int capacity;
    private boolean wide;
    private int count;

    public StringBuilder() { resizeBuffer(INITIAL_CAPACITY, false); }
    public StringBuilder(int capacity) {
        if (capacity < 0) throw new NegativeArraySizeException();
        resizeBuffer(capacity, false);
    }
    public StringBuilder(String text) {
        this(text.length() + INITIAL_CAPACITY);
        append(text);
    }
    public StringBuilder(CharSequence text) { this(text.toString()); }

    public int length() { return count; }
    public int capacity() { return capacity; }

    private native boolean resizeBufferImpl(int capacity, boolean wide);
    private void resizeBuffer(int capacity, boolean wide) {
        if (!resizeBufferImpl(capacity, wide)) throw new OutOfMemoryError();
    }
    private native char unit(int index);
    private native void put(int index, char ch);
    private native void move(int from, int to, int length);
    private native void zero(int from, int length);

    private void widen() {
        if (!wide) resizeBuffer(capacity, true);
    }

    private void enlargeBuffer(int minimum) {
        if (minimum < 0) throw new OutOfMemoryError();
        int grown = capacity * 2 + 2;
        if (grown < minimum || grown < 0) grown = minimum;
        resizeBuffer(grown, wide);
    }

    public void ensureCapacity(int minimum) {
        if (minimum > capacity()) enlargeBuffer(minimum);
    }

    public void trimToSize() {
        if (capacity != count) resizeBuffer(count, wide);
    }

    final void appendNull() { append("null"); }
    public native StringBuilder append(char ch);
    public native StringBuilder append(int number);
    public native StringBuilder append(long number);
    public native StringBuilder append(Object object);
    public native StringBuilder append(String text);
    public native char charAt(int index);
    public native void getChars(int start, int end, char[] destination, int destinationStart);
    public native String toString();

    public StringBuilder append(boolean value) { return append(value ? "true" : "false"); }
    public StringBuilder append(float value) { return append(Float.toString(value)); }
    public StringBuilder append(double value) { return append(Double.toString(value)); }
    public StringBuilder append(StringBuffer buffer) { return append(buffer == null ? "null" : buffer.toString()); }
    public StringBuilder append(char[] text) { return append(text, 0, text.length); }

    public native StringBuilder append(char[] text, int offset, int length);

    public StringBuilder append(CharSequence text) {
        if (text == null) text = "null";
        return append(text, 0, text.length());
    }

    private native boolean tryAppendRange(CharSequence text, int start, int end);

    public StringBuilder append(CharSequence text, int start, int end) {
        if (text == null) text = "null";
        if (start < 0 || end < start || end > text.length()) throw new IndexOutOfBoundsException();
        if (tryAppendRange(text, start, end)) return this;
        // A source may be this builder; append only after the original range.
        for (int i = start; i < end; i++) append(text.charAt(i));
        return this;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= count) throw new StringIndexOutOfBoundsException(index);
    }

    private void checkRange(int start, int end) {
        if (start < 0 || end < start || end > count) throw new StringIndexOutOfBoundsException();
    }

    public void setCharAt(int index, char ch) { checkIndex(index); put(index, ch); }

    public void setLength(int length) {
        if (length < 0) throw new StringIndexOutOfBoundsException(length);
        ensureCapacity(length);
        if (length > count) zero(count, length - count);
        count = length;
    }

    public StringBuilder delete(int start, int end) {
        if (end > count) end = count;
        checkRange(start, end);
        move(end, start, count - end);
        count -= end - start;
        return this;
    }

    public StringBuilder deleteCharAt(int index) { checkIndex(index); return delete(index, index + 1); }

    public StringBuilder insert(int index, String text) {
        if (index < 0 || index > count) throw new StringIndexOutOfBoundsException(index);
        if (text == null) text = "null";
        int length = text.length();
        if (count + length < 0) throw new OutOfMemoryError();
        ensureCapacity(count + length);
        if (!wide) {
            for (int i = 0; i < length; i++) if (text.charAt(i) > 255) { widen(); break; }
        }
        move(index, index + length, count - index);
        for (int i = 0; i < length; i++) put(index + i, text.charAt(i));
        count += length;
        return this;
    }

    // insert(int, char[]) AND ITS RANGE FORM MUST EXIST, EVEN THOUGH insert(int, Object)
    // would "handle" the call. Java's overload resolution picks the most specific
    // applicable method, so when the char[] form is absent `sb.insert(0, chars)`
    // compiles happily against insert(int, Object) and inserts "[C@1b6d3586" instead of
    // the characters. That is not a link error the dead-code pass can catch and it is
    // not a crash -- it is silently wrong text, correct in the simulator (which runs a
    // real JDK) and wrong on the device. SbLatin1T caught exactly this.
    public StringBuilder insert(int index, char[] value) {
        if (value == null) throw new NullPointerException();
        return insert(index, new String(value, 0, value.length));
    }

    public StringBuilder insert(int index, char[] value, int offset, int length) {
        if (value == null) throw new NullPointerException();
        return insert(index, new String(value, offset, length));
    }

    public StringBuilder insert(int index, char value) { return insert(index, String.valueOf(value)); }
    public StringBuilder insert(int index, boolean value) { return insert(index, String.valueOf(value)); }
    public StringBuilder insert(int index, int value) { return insert(index, Integer.toString(value)); }
    public StringBuilder insert(int index, long value) { return insert(index, Long.toString(value)); }
    public StringBuilder insert(int index, float value) { return insert(index, Float.toString(value)); }
    public StringBuilder insert(int index, double value) { return insert(index, Double.toString(value)); }
    public StringBuilder insert(int index, Object value) { return insert(index, String.valueOf(value)); }
    public StringBuilder insert(int index, CharSequence text) { return insert(index, text == null ? "null" : text.toString()); }
    public StringBuilder insert(int index, CharSequence text, int start, int end) {
        if (text == null) text = "null";
        if (start < 0 || end < start || end > text.length()) throw new IndexOutOfBoundsException();
        return insert(index, text.subSequence(start, end).toString());
    }

    public StringBuilder reverse() {
        for (int i = 0, j = count - 1; i < j; i++, j--) {
            char left = unit(i), right = unit(j);
            put(i, right); put(j, left);
        }
        if (wide) {
            for (int i = 0; i + 1 < count; i++) {
                char low = unit(i), high = unit(i + 1);
                if (low >= '\uDC00' && low <= '\uDFFF' && high >= '\uD800' && high <= '\uDBFF') {
                    put(i, high); put(++i, low);
                }
            }
        }
        return this;
    }

    public CharSequence subSequence(int start, int end) { return substring(start, end); }
    // Preserve the API's existing return type.
    public StringBuilder substring(int start, int end) {
        checkRange(start, end);
        StringBuilder result = new StringBuilder(end - start);
        for (int i = start; i < end; i++) result.append(unit(i));
        return result;
    }
}
