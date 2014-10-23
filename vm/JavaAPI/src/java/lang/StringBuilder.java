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

import java.util.Arrays;

/**
 * A string buffer implements a mutable sequence of characters. A string buffer is like a String, but can be modified. At any point in time it contains some particular sequence of characters, but the length and content of the sequence can be changed through certain method calls.
 * String buffers are safe for use by multiple threads. The methods are synchronized where necessary so that all the operations on any particular instance behave as if they occur in some serial order that is consistent with the order of the method calls made by each of the individual threads involved.
 * String buffers are used by the compiler to implement the binary string concatenation operator +. For example, the code:
 * is compiled to the equivalent of:
 * The principal operations on a StringBuffer are the append and insert methods, which are overloaded so as to accept data of any type. Each effectively converts a given datum to a string and then appends or inserts the characters of that string to the string buffer. The append method always adds these characters at the end of the buffer; the insert method adds the characters at a specified point.
 * For example, if z refers to a string buffer object whose current contents are "start", then the method call z.append("le") would cause the string buffer to contain "startle", whereas z.insert(4, "le") would alter the string buffer to contain "starlet".
 * In general, if sb refers to an instance of a StringBuffer, then sb.append(x) has the same effect as sb.insert(sb.length(),x).
 * Every string buffer has a capacity. As long as the length of the character sequence contained in the string buffer does not exceed the capacity, it is not necessary to allocate a new internal buffer array. If the internal buffer overflows, it is automatically made larger.
 * Since: JDK1.0, CLDC 1.0 See Also:ByteArrayOutputStream, String
 */
public final class StringBuilder implements CharSequence {
    static final int INITIAL_CAPACITY = 16;

    private char[] value;

    private int count;

    //private boolean shared;

    /**
     * Constructs a string buffer with no characters in it and an initial capacity of 16 characters.
     */
    public StringBuilder(){
        value = new char[INITIAL_CAPACITY];
    }

    /**
     * Constructs a string buffer with no characters in it and an initial capacity specified by the length argument.
     * length - the initial capacity.
     * - if the length argument is less than 0.
     */
    public StringBuilder(int length){
        if (length < 0) {
            throw new NegativeArraySizeException(Integer.toString(length));
        }
        value = new char[length];
    }

    /**
     * Constructs a string buffer so that it represents the same sequence of characters as the string argument; in other words, the initial contents of the string buffer is a copy of the argument string. The initial capacity of the string buffer is 16 plus the length of the string argument.
     * str - the initial contents of the buffer.
     */
    public StringBuilder(java.lang.String str){
        count = str.length();
        //shared = false;
        value = new char[count + INITIAL_CAPACITY];
        str.getChars(0, count, value, 0);
    }

    private void enlargeBuffer(int min) {
        int newCount = ((value.length >> 1) + value.length) + 2;
        char[] newData = new char[min > newCount ? min : newCount];
        System.arraycopy(value, 0, newData, 0, count);
        value = newData;
        //shared = false;
    }

    final void appendNull() {
        int newCount = count + 4;
        if (newCount > value.length) {
            enlargeBuffer(newCount);
        }
        value[count++] = 'n';
        value[count++] = 'u';
        value[count++] = 'l';
        value[count++] = 'l';
    }

    /**
     * Appends the string representation of the boolean argument to the string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuilder append(boolean b){
        if(b) {
            return append("true");
        }
        return append("false");
    }

    /**
     * Appends the string representation of the char argument to this string buffer.
     * The argument is appended to the contents of this string buffer. The length of this string buffer increases by 1.
     * The overall effect is exactly as if the argument were converted to a string by the method String.valueOf(char) and the character in that string were then appended to this StringBuffer object.
     */
    public java.lang.StringBuilder append(char c){
        if (count == value.length) {
            enlargeBuffer(count + 1);
        }
        value[count++] = c;
        return this; 
    }

    java.lang.StringBuilder append(char[] chars){
        int newCount = count + chars.length;
        if (newCount > value.length) {
            enlargeBuffer(newCount);
        }
        System.arraycopy(chars, 0, value, count, chars.length);
        count = newCount;
        return this;
    }

    /**
     * Appends the string representation of a subarray of the char array argument to this string buffer.
     * Characters of the character array str, starting at index offset, are appended, in order, to the contents of this string buffer. The length of this string buffer increases by the value of len.
     * The overall effect is exactly as if the arguments were converted to a string by the method String.valueOf(char[],int,int) and the characters of that string were then appended to this StringBuffer object.
     */
    public java.lang.StringBuilder append(char[] chars, int offset, int length){
        //Arrays.checkOffsetAndCount(chars.length, offset, length);
        int newCount = count + length;
        if (newCount > value.length) {
            enlargeBuffer(newCount);
        }
        System.arraycopy(chars, offset, value, count, length);
        count = newCount;
        return this;
    }

    /**
     * Appends the string representation of the double argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuilder append(double d){
        return append(Double.toString(d)); 
    }

    /**
     * Appends the string representation of the float argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuilder append(float f){
        return append(Float.toString(f)); 
    }

    /**
     * Appends the string representation of the int argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuilder append(int i){
        return append(Integer.toString(i)); 
    }

    /**
     * Appends the string representation of the long argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuilder append(long l){
        return append(Long.toString(l)); 
    }

    /**
     * Appends the string representation of the Object argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuilder append(java.lang.Object obj){
        if(obj == null) {
            appendNull();
            return this;
        }
        return append(obj.toString()); 
    }

    /**
     * Appends the string to this string buffer.
     * The characters of the String argument are appended, in order, to the contents of this string buffer, increasing the length of this string buffer by the length of the argument. If str is null, then the four characters "null" are appended to this string buffer.
     * Let n be the length of the old character sequence, the one contained in the string buffer just prior to execution of the append method. Then the character at index k in the new character sequence is equal to the character at index k in the old character sequence, if k is less than n; otherwise, it is equal to the character at index k-n in the argument str.
     */
    public java.lang.StringBuilder append(java.lang.String str){
        if (str == null) {
            appendNull();
            return this;
        }
        int length = str.length();
        int newCount = count + length;
        if (newCount > value.length) {
            enlargeBuffer(newCount);
        }
        str.getChars(0, length, value, count);
        count = newCount;
        return this;
    }

    /**
     * Returns the current capacity of the String buffer. The capacity is the amount of storage available for newly inserted characters; beyond which an allocation will occur.
     */
    public int capacity(){
        return value.length;
    }

    /**
     * The specified character of the sequence currently represented by the string buffer, as indicated by the index argument, is returned. The first character of a string buffer is at index 0, the next at index 1, and so on, for array indexing.
     * The index argument must be greater than or equal to 0, and less than the length of this string buffer.
     */
    public char charAt(int index){
        return value[index];
    }

    /**
     * Removes the characters in a substring of this StringBuffer. The substring begins at the specified start and extends to the character at index end - 1 or to the end of the StringBuffer if no such character exists. If start is equal to end, no changes are made.
     */
    public java.lang.StringBuilder delete(int start, int end){
        if (end > count) {
            end = count;
        }
        if (end == start) {
            return this;
        }
        if (end > start) {
            int length = count - end;
            if (length >= 0) {
                //if (!shared) {
                    System.arraycopy(value, end, value, start, length);
                /*} else {
                    char[] newData = new char[value.length];
                    System.arraycopy(value, 0, newData, 0, start);
                    System.arraycopy(value, end, newData, start, length);
                    value = newData;
                    shared = false;
                }*/
            }
            count -= end - start;
            return this;
        }
        return this;
    }

    /**
     * Removes the character at the specified position in this StringBuffer (shortening the StringBuffer by one character).
     */
    public java.lang.StringBuilder deleteCharAt(int index){
        int length = count - index - 1;
        if (length > 0) {
            //if (!shared) {
                System.arraycopy(value, index + 1, value, index, length);
            /*} else {
                char[] newData = new char[value.length];
                System.arraycopy(value, 0, newData, 0, index);
                System.arraycopy(value, index + 1, newData, index, length);
                value = newData;
                shared = false;
            }*/
        }
        count--;
        return this;
    }

    /**
     * Ensures that the capacity of the buffer is at least equal to the specified minimum. If the current capacity of this string buffer is less than the argument, then a new internal buffer is allocated with greater capacity. The new capacity is the larger of: The minimumCapacity argument. Twice the old capacity, plus 2. If the minimumCapacity argument is nonpositive, this method takes no action and simply returns.
     */
    public void ensureCapacity(int minimumCapacity){
        if (minimumCapacity > value.length) {
            int ourMin = value.length*2 + 2;
            enlargeBuffer(Math.max(ourMin, minimumCapacity));
        }
    }

    /**
     * Characters are copied from this string buffer into the destination character array dst. The first character to be copied is at index srcBegin; the last character to be copied is at index srcEnd-1. The total number of characters to be copied is srcEnd-srcBegin. The characters are copied into the subarray of dst starting at index dstBegin and ending at index:
     * dstbegin + (srcEnd-srcBegin) - 1
     */
    public void getChars(int start, int end, char[] dst, int dstStart) {
        System.arraycopy(value, start, dst, dstStart, end - start);
    }

    /**
     * Inserts the string representation of the boolean argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuilder insert(int offset, boolean b){
        if(b) return insert(offset, "true"); 
        return insert(offset, "false"); 
    }

    /**
     * Inserts the string representation of the char argument into this string buffer.
     * The second argument is inserted into the contents of this string buffer at the position indicated by offset. The length of this string buffer increases by one.
     * The overall effect is exactly as if the argument were converted to a string by the method String.valueOf(char) and the character in that string were then inserted into this StringBuffer object at the position indicated by offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuilder insert(int offset, char c){
        move(1, offset);
        value[offset] = c;
        count++;
        return this;
    }
    
    private void move(int size, int index) {
        int newCount;
        if (value.length - count >= size) {
            //if (!shared) {
                // index == count case is no-op
                System.arraycopy(value, index, value, index + size, count - index);
                return;
            /*}
            newCount = value.length;*/
        } else {
            newCount = Math.max(count + size, value.length*2 + 2);
        }

        char[] newData = new char[newCount];
        System.arraycopy(value, 0, newData, 0, index);
        // index == count case is no-op
        System.arraycopy(value, index, newData, index + size, count - index);
        value = newData;
        //shared = false;
    }

    java.lang.StringBuilder insert(int index, char[] chars){
        if (chars.length != 0) {
            move(chars.length, index);
            System.arraycopy(chars, 0, value, index, chars.length);
            count += chars.length;
        }
        return this; 
    }

    /**
     * Inserts the string representation of the double argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuilder insert(int offset, double d){
        return insert(offset, Double.toString(d));
    }

    /**
     * Inserts the string representation of the float argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuilder insert(int offset, float f){
        return insert(offset, Float.toString(f));
    }

    /**
     * Inserts the string representation of the second int argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuilder insert(int offset, int i){
        return insert(offset, Integer.toString(i));
    }

    /**
     * Inserts the string representation of the long argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the position indicated by offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuilder insert(int offset, long l){
        return insert(offset, Long.toString(l));
    }

    /**
     * Inserts the string representation of the Object argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuilder insert(int offset, java.lang.Object obj){
        return insert(offset, obj.toString());
    }

    /**
     * Inserts the string into this string buffer.
     * The characters of the String argument are inserted, in order, into this string buffer at the indicated offset, moving up any characters originally above that position and increasing the length of this string buffer by the length of the argument. If str is null, then the four characters "null" are inserted into this string buffer.
     * The character at index k in the new character sequence is equal to: the character at index k in the old character sequence, if k is less than offset the character at index k-offset in the argument str, if k is not less than offset but is less than offset+str.length() the character at index k-str.length() in the old character sequence, if k is not less than offset+str.length()
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuilder insert(int index, java.lang.String string){
        if (string == null) {
            string = "null";
        }
        int min = string.length();
        if (min != 0) {
            move(min, index);
            string.getChars(0, min, value, index);
            count += min;
        }
        return this;
    }

    /**
     * Returns the length (character count) of this string buffer.
     */
    public int length(){
        return count;
    }

    /**
     * The character sequence contained in this string buffer is replaced by the reverse of the sequence.
     * Let n be the length of the old character sequence, the one contained in the string buffer just prior to execution of the reverse method. Then the character at index k in the new character sequence is equal to the character at index n-k-1 in the old character sequence.
     */
    public java.lang.StringBuilder reverse(){
        if (count < 2) {
            return this;
        }
        //if (!shared) {
            int end = count - 1;
            char frontHigh = value[0];
            char endLow = value[end];
            boolean allowFrontSur = true, allowEndSur = true;
            for (int i = 0, mid = count / 2; i < mid; i++, --end) {
                char frontLow = value[i + 1];
                char endHigh = value[end - 1];
                boolean surAtFront = allowFrontSur && frontLow >= 0xdc00
                        && frontLow <= 0xdfff && frontHigh >= 0xd800
                        && frontHigh <= 0xdbff;
                if (surAtFront && (count < 3)) {
                    return this;
                }
                boolean surAtEnd = allowEndSur && endHigh >= 0xd800
                        && endHigh <= 0xdbff && endLow >= 0xdc00
                        && endLow <= 0xdfff;
                allowFrontSur = allowEndSur = true;
                if (surAtFront == surAtEnd) {
                    if (surAtFront) {
                        // both surrogates
                        value[end] = frontLow;
                        value[end - 1] = frontHigh;
                        value[i] = endHigh;
                        value[i + 1] = endLow;
                        frontHigh = value[i + 2];
                        endLow = value[end - 2];
                        i++;
                        end--;
                    } else {
                        // neither surrogates
                        value[end] = frontHigh;
                        value[i] = endLow;
                        frontHigh = frontLow;
                        endLow = endHigh;
                    }
                } else {
                    if (surAtFront) {
                        // surrogate only at the front
                        value[end] = frontLow;
                        value[i] = endLow;
                        endLow = endHigh;
                        allowFrontSur = false;
                    } else {
                        // surrogate only at the end
                        value[end] = frontHigh;
                        value[i] = endHigh;
                        frontHigh = frontLow;
                        allowEndSur = false;
                    }
                }
            }
            if ((count & 1) == 1 && (!allowFrontSur || !allowEndSur)) {
                value[end] = allowFrontSur ? endLow : frontHigh;
            }
        /*} else {
            char[] newData = new char[value.length];
            for (int i = 0, end = count; i < count; i++) {
                char high = value[i];
                if ((i + 1) < count && high >= 0xd800 && high <= 0xdbff) {
                    char low = value[i + 1];
                    if (low >= 0xdc00 && low <= 0xdfff) {
                        newData[--end] = low;
                        i++;
                    }
                }
                newData[--end] = high;
            }
            value = newData;
            shared = false;
        }*/
        return this;
    }

    /**
     * The character at the specified index of this string buffer is set to ch. The string buffer is altered to represent a new character sequence that is identical to the old character sequence, except that it contains the character ch at position index.
     * The offset argument must be greater than or equal to 0, and less than the length of this string buffer.
     */
    public void setCharAt(int index, char ch){
        value[index] = ch;
    }

    /**
     * Sets the length of this string buffer. This string buffer is altered to represent a new character sequence whose length is specified by the argument. For every nonnegative index
     * less than newLength, the character at index
     * in the new character sequence is the same as the character at index
     * in the old sequence if
     * is less than the length of the old character sequence; otherwise, it is the null character '\u0000'. In other words, if the newLength argument is less than the current length of the string buffer, the string buffer is truncated to contain exactly the number of characters given by the newLength argument.
     * If the newLength argument is greater than or equal to the current length, sufficient null characters ('u0000') are appended to the string buffer so that length becomes the newLength argument.
     * The newLength argument must be greater than or equal to 0.
     */
    public void setLength(int newLength){
        if (newLength > value.length) {
            enlargeBuffer(newLength);
        } else {
            //if (shared) {
                char[] newData = new char[value.length];
                System.arraycopy(value, 0, newData, 0, count);
                value = newData;
                /*shared = false;
            } else {
                if (count < newLength) {
                    Arrays.fill(value, count, newLength, (char) 0);
                }
            }*/
        }
        count = newLength;
    }

    /**
     * Converts to a string representing the data in this string buffer. A new String object is allocated and initialized to contain the character sequence currently represented by this string buffer. This String is then returned. Subsequent changes to the string buffer do not affect the contents of the String.
     * Implementation advice: This method can be coded so as to create a new String object without allocating new memory to hold a copy of the character sequence. Instead, the string can share the memory used by the string buffer. Any subsequent operation that alters the content or capacity of the string buffer must then make a copy of the internal buffer at that time. This strategy is effective for reducing the amount of memory allocated by a string concatenation operation when it is implemented using a string buffer.
     */
    public java.lang.String toString(){
        if (count == 0) {
            return "";
        }
        // Optimize String sharing for more performance
        /*int wasted = value.length - count;
        if (wasted >= 256
                || (wasted >= INITIAL_CAPACITY && wasted >= (count >> 1))) {
            return new String(value, 0, count);
        }
        shared = true;*/
        return new String(value, 0, count);
    }

    public StringBuilder StringBuffer(java.lang.CharSequence cs) {
            return new StringBuilder(cs.toString());
    }

    public void trimToSize() {
            // do nothing: according to the 1.5 javadoc,
            // there is no garantee the buffer capacity will be reduced to
            // fit the actual size
    }

    public StringBuilder append(final java.lang.CharSequence cs) {
        return null;
    }

    public StringBuilder append(java.lang.CharSequence s, final int start, final int end) {
        if (s == null) {
            s = "null";
        }

        int length = end - start;
        int newCount = count + length;
        if (newCount > value.length) {
            enlargeBuffer(newCount);
        } 

        if (s instanceof String) {
            ((String) s).getChars(start, end, value, count);
        } else if (s instanceof StringBuilder) {
            StringBuilder other = (StringBuilder) s;
            System.arraycopy(other.value, start, value, count, length);
        } else {
            int j = count; // Destination index.
            for (int i = start; i < end; i++) {
                value[j++] = s.charAt(i);
            }
        }

        this.count = newCount;
        return this;
    }

    public StringBuilder insert(final int offset, final java.lang.CharSequence cs) {
        return insert(offset, cs.toString());
    }

    public StringBuilder insert(final int offset, final CharSequence cs, final int start, final int end) {
        return insert(offset, cs.toString(), start, end);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().substring(start, end);
    }
}
