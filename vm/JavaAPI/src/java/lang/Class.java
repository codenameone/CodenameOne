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

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Instances of the class Class represent classes and interfaces in a running
 * Java application. Every array also belongs to a class that is reflected as a
 * Class object that is shared by all arrays with the same element type and
 * number of dimensions. Class has no public constructor. Instead Class objects
 * are constructed automatically by the Java Virtual Machine as classes are
 * loaded. The following example uses a Class object to print the class name of
 * an object: Since: JDK1.0, CLDC 1.0
 */
public final class Class<T> implements java.lang.reflect.Type {
    
    
    public ClassLoader getClassLoader() {
        if (isPrimitive()) {
            // A primitive class is bootstrap-defined and must report null, which is
            // what reflection code tests to tell such a type from a loaded one.
            return null;
        }
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * Returns the Class object associated with the class with the given string
     * name. Given the fully-qualified name for a class or interface, this
     * method attempts to locate, load and link the class. For example, the
     * following code fragment returns the runtime Class descriptor for the
     * class named java.lang.Thread: Classt= Class.forName("java.lang.Thread")
     */
    /**
     * Returns the Class object for {@code className}.
     *
     * ParparVM links the whole program ahead of time, so there is no second class
     * loader to consult: both extra arguments are accepted and ignored, and the
     * class is resolved exactly as the one-argument form resolves it. The overload
     * exists because library bytecode calls it -- ASM's
     * ClassWriter.getCommonSuperClass does -- and an absent overload is a link
     * error in translated code, not a compile error here.
     *
     * &lt;p&gt;What {@code initialize == true} does NOT do here: it does not run the
     * named class's static initializer. ParparVM runs one on first use -- the
     * generated code calls the class's static initializer at every NEW, GETSTATIC
     * and INVOKESTATIC -- so any code that goes on to TOUCH the class sees its
     * statics initialized as normal. What does not work is using forName purely for
     * a registration side effect and never referencing the class again, the
     * JDBC-driver idiom. That pattern cannot work on this platform for a second
     * reason anyway: obfuscation rewrites class names, so a name looked up as a
     * string does not survive a release build.
     */
    public static java.lang.Class forName(java.lang.String className, boolean initialize,
            ClassLoader loader) throws java.lang.ClassNotFoundException {
        return forName(className);
    }

    public static java.lang.Class forName(java.lang.String className) throws java.lang.ClassNotFoundException {
        className = className.replace('$', '.');
        Class c = forNameImpl(className);
        if(c == null) {
            throw new ClassNotFoundException(className);
        }
        return c;
    }

    private native static java.lang.Class forNameImpl(java.lang.String className) throws java.lang.ClassNotFoundException;
    
    /**
     * Returns the fully-qualified name of the entity (class, interface, array
     * class, primitive type, or void) represented by this Class object, as a
     * String. If this Class object represents a class of arrays, then the
     * internal form of the name consists of the name of the element type in
     * Java signature format, preceded by one or more "[" characters
     * representing the depth of array nesting. Thus: (new
     * Object[3]).getClass().getName() returns "[Ljava.lang.Object;" and: (new
     * int[3][4][5][6][7][8][9]).getClass().getName() returns "[[[[[[[I". The
     * encoding of element type names is as follows: B byte C char D double F
     * float I int J long L class or interface S short Z boolean The class or
     * interface name is given in fully qualified form as shown in the example
     * above.
     */
    public native java.lang.String getName();/* {
        if (this.name == null) {
            String name = getNameImpl();
            if (name.endsWith("[]")) {
                String componentType = name.substring(name.indexOf("["));
                int dimension = (name.length() - componentType.length())/2;
                String type = null;
                StringBuilder sb = new StringBuilder();
                while (dimension-- > 0) {
                    sb.append("[");
                }
                if (componentType.indexOf(".") != -1) {
                    sb.append("L").append(componentType).append(";");
                    
                } else if ("int".equals(componentType)) {
                    sb.append("I");
                    
                } else if ("float".equals(componentType)) {
                    sb.append("F");
                } else if ("boolean".equals(componentType)) {
                    sb.append("Z");
                } else if ("byte".equals(componentType)) {
                    sb.append("B");
                } else if ("char".equals(componentType)) {
                    sb.append("C");
                } else if ("short".equals(componentType)) {
                    sb.append("S");
                } else if ("long".equals(componentType)) {
                    sb.append("J");
                } else if ("double".equals(componentType)) {
                    sb.append("D");
                } else {
                    sb.append(name);
                }
                this.name = sb.toString();
            } else {
                this.name = name;
            }
            
        }
        return this.name;
    }
    
    native java.lang.String getNameImpl();
    */

    /**
     * Finds a resource with a given name in the application's JAR file. This
     * method returns null if no resource with this name is found in the
     * application's JAR file. The resource names can be represented in two
     * different formats: absolute or relative. Absolute format:
     * /packagePathName/resourceName Relative format: resourceName In the
     * absolute format, the programmer provides a fully qualified name that
     * includes both the full path and the name of the resource inside the JAR
     * file. In the path names, the character "/" is used as the separator. In
     * the relative format, the programmer provides only the name of the actual
     * resource. Relative names are converted to absolute names by the system by
     * prepending the resource name with the fully qualified package name of
     * class upon which the getResourceAsStream method was called.
     */
    public java.io.InputStream getResourceAsStream(java.lang.String name){
        if (name == null) {
            return null;
        }
        String absolute = name;
        if (!absolute.startsWith("/")) {
            // Relative names resolve against this class's package, as the javadoc
            // above describes.
            //
            // KNOWN LIMITATION, for a NESTED class only. getName() cannot be told
            // apart from a package here, because ParparVM builds the runtime class
            // name as clsName.replace('_', '.') in ByteCodeClass -- it starts from
            // the MANGLED name, so the '$' that separates a nested class from its
            // outer one arrives as a '.', and so does any '_' in a class's own
            // name. Outer$Inner therefore reports "a.b.Outer.Inner" where the JDK
            // reports "a.b.Outer$Inner", and the package derived below is
            // "a.b.Outer" rather than "a.b".
            //
            // The consequence is a MISS, not a wrong file: the derived path is a
            // directory named after a class, which a resource tree does not have,
            // so the lookup returns null exactly as it did before this method was
            // implemented. It is deliberately not patched up by walking shorter
            // prefixes -- a package really can be named like a class, and that
            // would turn today's miss into a confidently wrong hit. The fix
            // belongs in the name the VM reports, which is a change to getName()
            // for every translated application and wants its own testing.
            String className = getName();
            int lastDot = className.lastIndexOf('.');
            absolute = lastDot < 0 ? "/" + name
                    : "/" + className.substring(0, lastDot).replace('.', '/') + "/" + name;
        }
        // Resources linked INTO the executable are deliberately not consulted here.
        // They were, and it changed how shipping applications render.
        //
        // On master this method is `return null` on every ParparVM target, so no
        // application has ever received anything from it, and every caller has
        // always taken its not-found path. Reading the embedded table handed some
        // of those callers a resource for the first time: the Windows, Linux and
        // cross-compiled screenshot legs all began reporting ValidatorLightweight
        // Picker as changed, a four-pixel layout shift with identical content -- a
        // caller that had been falling back on a built-in default now had a file.
        // The javadoc this replaces claimed "nothing can regress, only start
        // working", which assumed every not-found path was strictly worse than the
        // resource. That assumption was wrong, and three ports disagreed with it.
        //
        // The filesystem tier below stays, because it is OPT-IN: it answers only
        // when CN1_RESOURCE_PATH names a search root, which no application sets and
        // the self-hosted translator does. So an application sees exactly what it
        // saw on master -- null -- and the translator can still find the C runtime
        // it has to copy into its output.
        //
        // Letting applications read their own embedded resources is a good feature
        // and wants its own change, where the screenshot baselines it moves can be
        // reviewed as the point of the change rather than as fallout from one.
        return cn1FileResource(absolute);
    }


    /**
     * The filesystem half of {@link #getResourceAsStream}: looks the resource up
     * under a search path, so a translated command-line program can read files that
     * sit beside it rather than being linked into it.
     *
     * The path comes from CN1_RESOURCE_PATH, else a "cn1runtime" directory next to
     * the executable. Entries are separated the way the platform separates path
     * entries.
     */
    private static java.io.InputStream cn1FileResource(String absolute) {
        String path = System.getenv("CN1_RESOURCE_PATH");
        if (path == null || path.length() == 0) {
            return null;
        }
        String relative = absolute.substring(1);
        // A resource name is not a path expression. Refusing any ".." segment keeps
        // a lookup inside the search root it was found under; without it a name
        // like "../../etc/passwd" reads straight out of the filesystem, and the
        // caller is usually passing a name that came from data.
        if (relative.length() == 0 || cn1EscapesRoot(relative)) {
            return null;
        }
        int from = 0;
        while (from <= path.length()) {
            int end = cn1PathEntryEnd(path, from);
            String root = end < 0 ? path.substring(from) : path.substring(from, end);
            if (root.length() > 0) {
                java.io.File candidate = new java.io.File(root, relative);
                if (candidate.exists()) {
                    try {
                        return new java.io.FileInputStream(candidate);
                    } catch (java.io.IOException err) {
                        return null;
                    }
                }
            }
            if (end < 0) {
                break;
            }
            from = end + 1;
        }
        return null;
    }

    /**
     * True when any segment of a resource-relative path is "..".
     *
     * A backslash counts as a separator as well as '/'. Resource names are
     * '/'-separated by specification, but nothing stops a caller passing a Windows
     * path, and there File("root", "..\\..\\x") escapes exactly as the '/' form
     * does -- checking only '/' would leave the traversal open on the one platform
     * whose separator it is.
     */
    private static boolean cn1EscapesRoot(String relative) {
        int from = 0;
        for (int i = 0; i <= relative.length(); i++) {
            boolean atEnd = i == relative.length();
            if (!atEnd && relative.charAt(i) != '/' && relative.charAt(i) != '\\') {
                continue;
            }
            if (relative.substring(from, i).equals("..")) {
                return true;
            }
            from = i + 1;
        }
        return false;
    }

    /**
     * The index that ends the search-path entry starting at {@code from}, or -1 for
     * the last one.
     *
     * This cannot use {@code File.pathSeparatorChar}, which is a hard-coded ':' in
     * this class library rather than a platform value -- on a native Windows build
     * that splits "C:\\res;D:\\res" after the drive letter and every entry is
     * nonsense. Both separators are therefore accepted, and a ':' is not a
     * separator when it sits directly after a single-letter entry and is followed
     * by a slash, which is exactly a DOS drive prefix and never a POSIX path.
     */
    private static int cn1PathEntryEnd(String path, int from) {
        for (int i = from; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == ';') {
                return i;
            }
            if (c == ':') {
                boolean driveLetter = i == from + 1
                        && i + 1 < path.length()
                        && (path.charAt(i + 1) == '\\' || path.charAt(i + 1) == '/')
                        && cn1IsLetter(path.charAt(from));
                if (!driveLetter) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** ASCII letter test; Character.isLetter is locale-aware and not wanted here. */
    private static boolean cn1IsLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    
    /**
     * Determines if this Class object represents an array class.
     */
    public native boolean isArray();

    /**
     * Determines if the class or interface represented by this Class object is
     * either the same as, or is a superclass or superinterface of, the class or
     * interface represented by the specified Class parameter. It returns true
     * if so; otherwise it returns false. If this Class object represents a
     * primitive type, this method returns true if the specified Class parameter
     * is exactly this Class object; otherwise it returns false. Specifically,
     * this method tests whether the type represented by the specified Class
     * parameter can be converted to the type represented by this Class object
     * via an identity conversion or via a widening reference conversion. See
     * The Java Language Specification, sections 5.1.1 and 5.1.4 , for details.
     */
    public native boolean isAssignableFrom(java.lang.Class cls);

    /**
     * Determines if the specified Object is assignment-compatible with the
     * object represented by this Class. This method is the dynamic equivalent
     * of the Java language instanceof operator. The method returns true if the
     * specified Object argument is non-null and can be cast to the reference
     * type represented by this Class object without raising a
     * ClassCastException. It returns false otherwise. Specifically, if this
     * Class object represents a declared class, this method returns true if the
     * specified Object argument is an instance of the represented class (or of
     * any of its subclasses); it returns false otherwise. If this Class object
     * represents an array class, this method returns true if the specified
     * Object argument can be converted to an object of the array class by an
     * identity conversion or by a widening reference conversion; it returns
     * false otherwise. If this Class object represents an interface, this
     * method returns true if the class or any superclass of the specified
     * Object argument implements this interface; it returns false otherwise. If
     * this Class object represents a primitive type, this method returns false.
     */
    public native boolean isInstance(java.lang.Object obj);

    /**
     * The Class representing the superclass of this class, or null when this
     * class is Object, an interface, a primitive type or void.
     */
    public native java.lang.Class getSuperclass();

    /**
     * Determines if the specified Class object represents an interface type.
     */
    public native boolean isInterface();

    /**
     * Creates a new instance of a class.
     */
    public java.lang.Object newInstance() throws java.lang.InstantiationException, java.lang.IllegalAccessException {
        if (isPrimitive()) {
            // A primitive descriptor has no constructor, and its newInstanceFp is
            // zero -- the native calls that pointer unconditionally, so letting one
            // through jumps to address zero instead of throwing.
            throw new InstantiationException();
        }
        Object o = newInstanceImpl();
        if(o == null) {
            throw new InstantiationException();
        }
        return o; 
    }

    private native java.lang.Object newInstanceImpl();
    
    /**
     * Converts the object to a string. The string representation is the string
     * "class" or "interface", followed by a space, and then by the fully
     * qualified name of the class in the format returned by getName. If this
     * Class object represents a primitive type, this method returns the name of
     * the primitive type. If this Class object represents void this method
     * returns "void".
     */
    public java.lang.String toString() {
        if (isPrimitive()) {
            // "int", not "int class" -- java.lang.Class documents the primitive form
            // as the name alone.
            return getName();
        }
        return getName() + " class";
    }

    public native boolean isAnnotation();

    /**
     * Returns this element's annotation for the specified type if such an
     * annotation is present, else null.
     *
     */
    public <T extends Annotation> T getAnnotation(Class annotationType) {
        if (annotationType == null) {
            throw new NullPointerException("Null annotationType");
        }

        return null;
    }

    /**
     * Returns all annotations present on this element.
     */
    public Annotation[] getAnnotations() {
        return null;
    }

    /**
     * Returns all annotations that are directly present on this element.
     */
    public Annotation[] getDeclaredAnnotations() {
        return null;
    }

    /**
     * Returns true if an annotation for the specified type is present on this
     * element, else false.
     */
    public boolean isAnnotationPresent(Class annotationType) {
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
     * Replacement for Class.cast(Object). Throws a ClassCastException if
     * <i>obj</i>
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
        if (object == null) {
            return null;
        }
        if (!isAssignableFrom(object.getClass())) {
            throw new java.lang.ClassCastException("Cannot cast "+object.getClass()+" to "+this);
        }
        return object;
    }

    /**
     * Replacement for Class.isEnum().
     *
     * @param class_ class we want to test.
     * @return true if the class was declared as an Enum.
     */
    public native boolean isEnum();

    /**
     * replacement for Class.isAnonymousClass()
     */
    public native boolean isAnonymousClass();    
    

    /**
     * replacement for Class.getSimpleName()
     */
    public String getSimpleName() {
        String n = getName();
        return n.substring(n.lastIndexOf('.') + 1);
    }

    /**
     * replacement for Class.isSynthetic()
     */
    public native boolean isSynthetic();

    public String getCanonicalName() {
        return getName();
    }

    @Override
    public native int hashCode();

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    public boolean desiredAssertionStatus() {
        return false;
    }
    
    public native Class getComponentType();
    
    public java.lang.reflect.Type[] getGenericInterfaces() {
        throw new UnsupportedOperationException("Class.getGenericInterfaces() not supported on this platform");
    }
    
    public native boolean isPrimitive();
    
    public Method getEnclosingMethod() {
        return null;
    }
    
    public Constructor getEnclosingConstructor() {
        return null;
    }
    
    public boolean isLocalClass() {
        return false;
    }

    public boolean isRecord() {
        return this != java.lang.Record.class && java.lang.Record.class.isAssignableFrom(this);
    }
}
