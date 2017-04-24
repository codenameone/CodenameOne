/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.lang;
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
public final class StringBuffer implements CharSequence {
    /**
     * Constructs a string buffer with no characters in it and an initial capacity of 16 characters.
     */
    public StringBuffer(){
         //TODO codavaj!!
    }

    /**
     * Constructs a string buffer with no characters in it and an initial capacity specified by the length argument.
     * length - the initial capacity.
     * - if the length argument is less than 0.
     */
    public StringBuffer(int length){
         //TODO codavaj!!
    }

    /**
     * Constructs a string buffer so that it represents the same sequence of characters as the string argument; in other words, the initial contents of the string buffer is a copy of the argument string. The initial capacity of the string buffer is 16 plus the length of the string argument.
     * str - the initial contents of the buffer.
     */
    public StringBuffer(java.lang.String str){
         //TODO codavaj!!
    }

    /**
     * Appends the string representation of the boolean argument to the string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuffer append(boolean b){
        return null; //TODO codavaj!!
    }

    /**
     * Appends the string representation of the char argument to this string buffer.
     * The argument is appended to the contents of this string buffer. The length of this string buffer increases by 1.
     * The overall effect is exactly as if the argument were converted to a string by the method String.valueOf(char) and the character in that string were then appended to this StringBuffer object.
     */
    public java.lang.StringBuffer append(char c){
        return null; //TODO codavaj!!
    }

    java.lang.StringBuffer append(char[] str){
        return null; //TODO codavaj!!
    }

    /**
     * Appends the string representation of a subarray of the char array argument to this string buffer.
     * Characters of the character array str, starting at index offset, are appended, in order, to the contents of this string buffer. The length of this string buffer increases by the value of len.
     * The overall effect is exactly as if the arguments were converted to a string by the method String.valueOf(char[],int,int) and the characters of that string were then appended to this StringBuffer object.
     */
    public java.lang.StringBuffer append(char[] str, int offset, int len){
        return null; //TODO codavaj!!
    }

    /**
     * Appends the string representation of the double argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuffer append(double d){
        return null; //TODO codavaj!!
    }

    /**
     * Appends the string representation of the float argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuffer append(float f){
        return null; //TODO codavaj!!
    }

    /**
     * Appends the string representation of the int argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuffer append(int i){
        return null; //TODO codavaj!!
    }

    /**
     * Appends the string representation of the long argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuffer append(long l){
        return null; //TODO codavaj!!
    }

    /**
     * Appends the string representation of the Object argument to this string buffer.
     * The argument is converted to a string as if by the method String.valueOf, and the characters of that string are then appended to this string buffer.
     */
    public java.lang.StringBuffer append(java.lang.Object obj){
        return null; //TODO codavaj!!
    }

    /**
     * Appends the string to this string buffer.
     * The characters of the String argument are appended, in order, to the contents of this string buffer, increasing the length of this string buffer by the length of the argument. If str is null, then the four characters "null" are appended to this string buffer.
     * Let n be the length of the old character sequence, the one contained in the string buffer just prior to execution of the append method. Then the character at index k in the new character sequence is equal to the character at index k in the old character sequence, if k is less than n; otherwise, it is equal to the character at index k-n in the argument str.
     */
    public java.lang.StringBuffer append(java.lang.String str){
        return null; //TODO codavaj!!
    }

    /**
     * Returns the current capacity of the String buffer. The capacity is the amount of storage available for newly inserted characters; beyond which an allocation will occur.
     */
    public int capacity(){
        return 0; //TODO codavaj!!
    }

    /**
     * The specified character of the sequence currently represented by the string buffer, as indicated by the index argument, is returned. The first character of a string buffer is at index 0, the next at index 1, and so on, for array indexing.
     * The index argument must be greater than or equal to 0, and less than the length of this string buffer.
     */
    public char charAt(int index){
        return ' '; //TODO codavaj!!
    }

    /**
     * Removes the characters in a substring of this StringBuffer. The substring begins at the specified start and extends to the character at index end - 1 or to the end of the StringBuffer if no such character exists. If start is equal to end, no changes are made.
     */
    public java.lang.StringBuffer delete(int start, int end){
        return null; //TODO codavaj!!
    }

    /**
     * Removes the character at the specified position in this StringBuffer (shortening the StringBuffer by one character).
     */
    public java.lang.StringBuffer deleteCharAt(int index){
        return null; //TODO codavaj!!
    }

    /**
     * Ensures that the capacity of the buffer is at least equal to the specified minimum. If the current capacity of this string buffer is less than the argument, then a new internal buffer is allocated with greater capacity. The new capacity is the larger of: The minimumCapacity argument. Twice the old capacity, plus 2. If the minimumCapacity argument is nonpositive, this method takes no action and simply returns.
     */
    public void ensureCapacity(int minimumCapacity){
        return; //TODO codavaj!!
    }

    /**
     * Characters are copied from this string buffer into the destination character array dst. The first character to be copied is at index srcBegin; the last character to be copied is at index srcEnd-1. The total number of characters to be copied is srcEnd-srcBegin. The characters are copied into the subarray of dst starting at index dstBegin and ending at index:
     * dstbegin + (srcEnd-srcBegin) - 1
     */
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin){
        return; //TODO codavaj!!
    }

    /**
     * Inserts the string representation of the boolean argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuffer insert(int offset, boolean b){
        return null; //TODO codavaj!!
    }

    /**
     * Inserts the string representation of the char argument into this string buffer.
     * The second argument is inserted into the contents of this string buffer at the position indicated by offset. The length of this string buffer increases by one.
     * The overall effect is exactly as if the argument were converted to a string by the method String.valueOf(char) and the character in that string were then inserted into this StringBuffer object at the position indicated by offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuffer insert(int offset, char c){
        return null; //TODO codavaj!!
    }

    java.lang.StringBuffer insert(int offset, char[] str){
        return null; //TODO codavaj!!
    }

    /**
     * Inserts the string representation of the double argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuffer insert(int offset, double d){
        return null; //TODO codavaj!!
    }

    /**
     * Inserts the string representation of the float argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuffer insert(int offset, float f){
        return null; //TODO codavaj!!
    }

    /**
     * Inserts the string representation of the second int argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuffer insert(int offset, int i){
        return null; //TODO codavaj!!
    }

    /**
     * Inserts the string representation of the long argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the position indicated by offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuffer insert(int offset, long l){
        return null; //TODO codavaj!!
    }

    /**
     * Inserts the string representation of the Object argument into this string buffer.
     * The second argument is converted to a string as if by the method String.valueOf, and the characters of that string are then inserted into this string buffer at the indicated offset.
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuffer insert(int offset, java.lang.Object obj){
        return null; //TODO codavaj!!
    }

    /**
     * Inserts the string into this string buffer.
     * The characters of the String argument are inserted, in order, into this string buffer at the indicated offset, moving up any characters originally above that position and increasing the length of this string buffer by the length of the argument. If str is null, then the four characters "null" are inserted into this string buffer.
     * The character at index k in the new character sequence is equal to: the character at index k in the old character sequence, if k is less than offset the character at index k-offset in the argument str, if k is not less than offset but is less than offset+str.length() the character at index k-str.length() in the old character sequence, if k is not less than offset+str.length()
     * The offset argument must be greater than or equal to 0, and less than or equal to the length of this string buffer.
     */
    public java.lang.StringBuffer insert(int offset, java.lang.String str){
        return null; //TODO codavaj!!
    }

    /**
     * Returns the length (character count) of this string buffer.
     */
    public int length(){
        return 0; //TODO codavaj!!
    }

    /**
     * The character sequence contained in this string buffer is replaced by the reverse of the sequence.
     * Let n be the length of the old character sequence, the one contained in the string buffer just prior to execution of the reverse method. Then the character at index k in the new character sequence is equal to the character at index n-k-1 in the old character sequence.
     */
    public java.lang.StringBuffer reverse(){
        return null; //TODO codavaj!!
    }

    /**
     * The character at the specified index of this string buffer is set to ch. The string buffer is altered to represent a new character sequence that is identical to the old character sequence, except that it contains the character ch at position index.
     * The offset argument must be greater than or equal to 0, and less than the length of this string buffer.
     */
    public void setCharAt(int index, char ch){
        return; //TODO codavaj!!
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
        return; //TODO codavaj!!
    }

    /**
     * Converts to a string representing the data in this string buffer. A new String object is allocated and initialized to contain the character sequence currently represented by this string buffer. This String is then returned. Subsequent changes to the string buffer do not affect the contents of the String.
     * Implementation advice: This method can be coded so as to create a new String object without allocating new memory to hold a copy of the character sequence. Instead, the string can share the memory used by the string buffer. Any subsequent operation that alters the content or capacity of the string buffer must then make a copy of the internal buffer at that time. This strategy is effective for reducing the amount of memory allocated by a string concatenation operation when it is implemented using a string buffer.
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    	public StringBuffer StringBuffer(java.lang.CharSequence cs) {
		return new StringBuffer(cs.toString());
	}

	public void trimToSize() {
		// do nothing: according to the 1.5 javadoc,
		// there is no garantee the buffer capacity will be reduced to
		// fit the actual size
	}

	public StringBuffer append(
			final java.lang.CharSequence cs) {
		return null;
	}

	public StringBuffer append(
			final java.lang.CharSequence cs, final int start, final int end) {
		return null;
	}

	public StringBuffer insert(final int offset,
			final java.lang.CharSequence cs) {
		return null;
	}

	public StringBuffer insert(final int offset,
			final CharSequence cs, final int start, final int end) {
		return null;
	}

    public CharSequence subSequence(int start, int end) {
        return null;
    }


}
