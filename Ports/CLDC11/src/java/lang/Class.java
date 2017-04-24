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

import java.lang.annotation.*;
/**
 * Instances of the class Class represent classes and interfaces in a running Java application. Every array also belongs to a class that is reflected as a Class object that is shared by all arrays with the same element type and number of dimensions.
 * Class has no public constructor. Instead Class objects are constructed automatically by the Java Virtual Machine as classes are loaded.
 * The following example uses a Class object to print the class name of an object:
 * Since: JDK1.0, CLDC 1.0
 */
public final class Class<T>{
    /**
     * Returns the Class object associated with the class with the given string name. Given the fully-qualified name for a class or interface, this method attempts to locate, load and link the class.
     * For example, the following code fragment returns the runtime Class descriptor for the class named java.lang.Thread: Classt= Class.forName("java.lang.Thread")
     * @deprecated don't use this method for anything important since class names are obfuscated on the device!
     */
    public static java.lang.Class forName(java.lang.String className) throws java.lang.ClassNotFoundException{
        return null; //TODO codavaj!!
    }

    /**
     * Returns the fully-qualified name of the entity (class, interface, array class, primitive type, or void) represented by this Class object, as a String.
     * If this Class object represents a class of arrays, then the internal form of the name consists of the name of the element type in Java signature format, preceded by one or more "[" characters representing the depth of array nesting. Thus:
     * (new Object[3]).getClass().getName() returns "[Ljava.lang.Object;" and: (new int[3][4][5][6][7][8][9]).getClass().getName() returns "[[[[[[[I". The encoding of element type names is as follows: B byte C char D double F float I int J long L
     * class or interface S short Z boolean The class or interface name
     * is given in fully qualified form as shown in the example above.
     * @deprecated don't use this method for anything important since class names are obfuscated on the device!
     */
    public java.lang.String getName(){
        return null; //TODO codavaj!!
    }

    /**
     * Finds a resource with a given name in the application's JAR file. This method returns null if no resource with this name is found in the application's JAR file.
     * The resource names can be represented in two different formats: absolute or relative.
     * Absolute format: /packagePathName/resourceName
     * Relative format: resourceName
     * In the absolute format, the programmer provides a fully qualified name that includes both the full path and the name of the resource inside the JAR file. In the path names, the character "/" is used as the separator.
     * In the relative format, the programmer provides only the name of the actual resource. Relative names are converted to absolute names by the system by prepending the resource name with the fully qualified package name of class upon which the getResourceAsStream method was called.
     */
    /*public java.io.InputStream getResourceAsStream(java.lang.String name){
        return null; //TODO codavaj!!
    }*/

    /**
     * Determines if this Class object represents an array class.
     */
    public boolean isArray(){
        return false; //TODO codavaj!!
    }

    /**
     * Determines if the class or interface represented by this Class object is either the same as, or is a superclass or superinterface of, the class or interface represented by the specified Class parameter. It returns true if so; otherwise it returns false. If this Class object represents a primitive type, this method returns true if the specified Class parameter is exactly this Class object; otherwise it returns false.
     * Specifically, this method tests whether the type represented by the specified Class parameter can be converted to the type represented by this Class object via an identity conversion or via a widening reference conversion. See The Java Language Specification, sections 5.1.1 and 5.1.4 , for details.
     */
    public boolean isAssignableFrom(java.lang.Class cls){
        return false; //TODO codavaj!!
    }

    /**
     * Determines if the specified Object is assignment-compatible with the object represented by this Class. This method is the dynamic equivalent of the Java language instanceof operator. The method returns true if the specified Object argument is non-null and can be cast to the reference type represented by this Class object without raising a ClassCastException. It returns false otherwise.
     * Specifically, if this Class object represents a declared class, this method returns true if the specified Object argument is an instance of the represented class (or of any of its subclasses); it returns false otherwise. If this Class object represents an array class, this method returns true if the specified Object argument can be converted to an object of the array class by an identity conversion or by a widening reference conversion; it returns false otherwise. If this Class object represents an interface, this method returns true if the class or any superclass of the specified Object argument implements this interface; it returns false otherwise. If this Class object represents a primitive type, this method returns false.
     */
    public boolean isInstance(java.lang.Object obj){
        return false; //TODO codavaj!!
    }

    /**
     * Determines if the specified Class object represents an interface type.
     */
    public boolean isInterface(){
        return false; //TODO codavaj!!
    }

    /**
     * Creates a new instance of a class.
     */
    public java.lang.Object newInstance() throws java.lang.InstantiationException, java.lang.IllegalAccessException{
        return null; //TODO codavaj!!
    }

    /**
     * Converts the object to a string. The string representation is the string "class" or "interface", followed by a space, and then by the fully qualified name of the class in the format returned by getName. If this Class object represents a primitive type, this method returns the name of the primitive type. If this Class object represents void this method returns "void".
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    
    
	  public boolean isAnnotation() {
                return 	false;
	  }

	  /**
	   * Returns this element's annotation for the specified type if such an annotation is present, else null.
	   * 
	   */
	  public <T extends Annotation> T getAnnotation( Class annotationType ) {
		  if ( annotationType == null ) {
	      throw new NullPointerException( "Null annotationType" );
	    }

	    return null;//AIB.getAib(c).getClassAnnotation(annotationType);
	  }

	  /**
	   * Returns all annotations present on this element.
	   */
	  public Annotation[] getAnnotations() {
	    return null;//AIB.getAib(c).getClassAnnotations();
	  }

	  /**
	   * Returns all annotations that are directly present on this element.
	   */
	  public Annotation[] getDeclaredAnnotations() {
	    return null;//AIB.getAib(c).getDeclaredClassAnnotations();
	  }

	  /**
	   * Returns true if an annotation for the specified type is present on this element, else false.
	   */
	  public boolean isAnnotationPresent( Class annotationType ) {
                return false;
	  }

	/**
	 * Replacement for Class.asSubclass(Class).
	 * 
	 * @param c a Class
	 * @param superclass another Class which must be a superclass of <i>c</i>
	 * @return <i>c</i>
	 * @throws java.lang.ClassCastException if <i>c</i> is
	 */
	public Class asSubclass(Class superclass) {
		return null;
	}

	/**
	 * Replacement for Class.cast(Object). Throws a ClassCastException if <i>obj</i>
	 * is not an instance of class <var>c</var>, or a subtype of <var>c</var>.
	 * 
	 * @param c Class we want to cast <var>obj</var> to
	 * @param object object we want to cast
	 * @return The object, or <code>null</code> if the object is
	 * <code>null</code>.
	 * @throws java.lang.ClassCastException if <var>obj</var> is not
	 * <code>null</code> or an instance of <var>c</var>
	 */
	public Object cast(Object object) {
		return null;
	}

	/**
	 * Replacement for Class.isEnum().
	 * 
	 * @param class_ class we want to test.
	 * @return true if the class was declared as an Enum.
	 */
	public boolean isEnum() {
            return false;
	}

	/**
	* replacement for Class.isAnonymousClass()
	*/
	public boolean isAnonymousClass() {
		return false;
	}

        /**
         * @deprecated don't use this method for anything important since class names are obfuscated on the device!
         */
	public String getSimpleName() {
            return null;
	}

	/**
	 * replacement for Class.isSynthetic()
	 */
	public boolean isSynthetic() {
		return false;
	}

        /**
         * @deprecated don't use this method for anything important since class names are obfuscated on the device!
         */
	public String getCanonicalName() {
            return null;
	}

    public boolean desiredAssertionStatus() {
        return false;
    }        
}
