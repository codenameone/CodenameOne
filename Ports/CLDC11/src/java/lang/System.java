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
 * The System class contains several useful class fields and methods. It cannot be instantiated.
 * Since: JDK1.0, CLDC 1.0
 */
public final class System{
    /**
     * The "standard" error output stream. This stream is already open and ready to accept output data.
     * Typically this stream corresponds to display output or another output destination specified by the host environment or user. By convention, this output stream is used to display error messages or other information that should come to the immediate attention of a user even if the principal output stream, the value of the variable out, has been redirected to a file or other destination that is typically not continuously monitored.
     */
    public static final java.io.PrintStream err=null;

    /**
     * The "standard" output stream. This stream is already open and ready to accept output data. Typically this stream corresponds to display output or another output destination specified by the host environment or user.
     * For simple stand-alone Java applications, a typical way to write a line of output data is:
     * System.out.println(data)
     * See the println methods in class PrintStream.
     * See Also:PrintStream.println(), PrintStream.println(boolean), PrintStream.println(char), PrintStream.println(char[]), PrintStream.println(int), PrintStream.println(long), PrintStream.println(java.lang.Object), PrintStream.println(java.lang.String)
     */
    public static final java.io.PrintStream out=null;

    /**
     * Copies an array from the specified source array, beginning at the specified position, to the specified position of the destination array. A subsequence of array components are copied from the source array referenced by src to the destination array referenced by dst. The number of components copied is equal to the length argument. The components at positions srcOffset through srcOffset+length-1 in the source array are copied into positions dstOffset through dstOffset+length-1, respectively, of the destination array.
     * If the src and dst arguments refer to the same array object, then the copying is performed as if the components at positions srcOffset through srcOffset+length-1 were first copied to a temporary array with length components and then the contents of the temporary array were copied into positions dstOffset through dstOffset+length-1 of the destination array.
     * If dst is null, then a NullPointerException is thrown.
     * If src is null, then a NullPointerException is thrown and the destination array is not modified.
     * Otherwise, if any of the following is true, an ArrayStoreException is thrown and the destination is not modified: The src argument refers to an object that is not an array. The dst argument refers to an object that is not an array. The src argument and dst argument refer to arrays whose component types are different primitive types. The src argument refers to an array with a primitive component type and the dst argument refers to an array with a reference component type. The src argument refers to an array with a reference component type and the dst argument refers to an array with a primitive component type.
     * Otherwise, if any of the following is true, an IndexOutOfBoundsException is thrown and the destination is not modified: The srcOffset argument is negative. The dstOffset argument is negative. The length argument is negative. srcOffset+length is greater than src.length, the length of the source array. dstOffset+length is greater than dst.length, the length of the destination array.
     * Otherwise, if any actual component of the source array from position srcOffset through srcOffset+length-1 cannot be converted to the component type of the destination array by assignment conversion, an ArrayStoreException is thrown. In this case, let k be the smallest nonnegative integer less than length such that src[srcOffset+k] cannot be converted to the component type of the destination array; when the exception is thrown, source array components from positions srcOffset through srcOffset+k-1 will already have been copied to destination array positions dstOffset through dstOffset+k-1 and no other positions of the destination array will have been modified. (Because of the restrictions already itemized, this paragraph effectively applies only to the situation where both arrays have component types that are reference types.)
     */
    public static void arraycopy(java.lang.Object src, int srcOffset, java.lang.Object dst, int dstOffset, int length){
        return; //TODO codavaj!!
    }

    /**
     * Returns the current time in milliseconds.
     */
    public static long currentTimeMillis(){
        return 0l; //TODO codavaj!!
    }

    /**
     * Terminates the currently running Java application. The argument serves as a status code; by convention, a nonzero status code indicates abnormal termination.
     * This method calls the exit method in class Runtime. This method never returns normally.
     * The call System.exit(n) is effectively equivalent to the call:
     * Runtime.getRuntime().exit(n)
     */
    public static void exit(int status){
        return; //TODO codavaj!!
    }

    /**
     * Runs the garbage collector.
     * Calling the gc method suggests that the Java Virtual Machine expend effort toward recycling unused objects in order to make the memory they currently occupy available for quick reuse. When control returns from the method call, the Java Virtual Machine has made a best effort to reclaim space from all discarded objects.
     * The call System.gc() is effectively equivalent to the call:
     * Runtime.getRuntime().gc()
     */
    public static void gc(){
        return; //TODO codavaj!!
    }

    /**
     * Gets the system property indicated by the specified key.
     */
    public static java.lang.String getProperty(java.lang.String key){
        return null; //TODO codavaj!!
    }

    /**
     * Returns the same hashcode for the given object as would be returned by the default method hashCode(), whether or not the given object's class overrides hashCode(). The hashcode for the null reference is zero.
     */
    public static int identityHashCode(java.lang.Object x){
        return 0; //TODO codavaj!!
    }

}
