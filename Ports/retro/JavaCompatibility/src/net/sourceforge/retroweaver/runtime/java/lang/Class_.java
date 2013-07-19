package net.sourceforge.retroweaver.runtime.java.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.sourceforge.retroweaver.runtime.java.lang.annotation.Annotation;

/**
 * Replacements for methods added to java.lang.Class in Java 1.5.
 */
public final class Class_ {

	private Class_() {
		// private constructor
	}

	  public static boolean isAnnotation( Class c ) {
	    return 	Annotation.class.isAssignableFrom(c);
	  }

	  /**
	   * Returns this element's annotation for the specified type if such an annotation is present, else null.
	   * 
	   */
	  public static <T extends Annotation> T getAnnotation( Class c, Class<T> annotationType ) {
		  if ( annotationType == null ) {
	      throw new NullPointerException( "Null annotationType" );
	    }

	    return null;//AIB.getAib(c).getClassAnnotation(annotationType);
	  }

	  /**
	   * Returns all annotations present on this element.
	   */
	  public static Annotation[] getAnnotations( Class c ) {
	    return null;//AIB.getAib(c).getClassAnnotations();
	  }

	  /**
	   * Returns all annotations that are directly present on this element.
	   */
	  public static Annotation[] getDeclaredAnnotations( Class c ) {
	    return null;//AIB.getAib(c).getDeclaredClassAnnotations();
	  }

	  /**
	   * Returns true if an annotation for the specified type is present on this element, else false.
	   */
	  public static boolean isAnnotationPresent( Class c, Class<? extends Annotation> annotationType ) {
	    return getAnnotation( c, annotationType ) != null;
	  }

	/**
	 * Replacement for Class.asSubclass(Class).
	 * 
	 * @param c a Class
	 * @param superclass another Class which must be a superclass of <i>c</i>
	 * @return <i>c</i>
	 * @throws java.lang.ClassCastException if <i>c</i> is
	 */
	public static Class asSubclass(Class<?> c, Class<?> superclass) {
		if (!superclass.isAssignableFrom(c)) {
			throw new ClassCastException(superclass.getName());
		}
		return c;
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
	public static Object cast(Class c, Object object) {
		if (object == null || c.isInstance(object)) {
			return object;
		} else {
			throw new ClassCastException(c.getName());
		}
	}

	/**
	 * Replacement for Class.isEnum().
	 * 
	 * @param class_ class we want to test.
	 * @return true if the class was declared as an Enum.
	 */
	public static <T> boolean isEnum(Class<T> class_) {
		/*Class c = class_.getSuperclass();

		if (c == null) {
			return false;
		}*/

		//return Enum.class.isAssignableFrom(c);
                return Enum.class.isAssignableFrom(class_);
	}

	/**
	 * Replacement for Class.getEnumConstants().
	 * 
	 * @param class_ class we want to get Enum constants for.
	 * @return The elements of this enum class or null if this does not represent an enum type.
	 */
	public static <T> T[] getEnumConstants(Class<T> class_) {
		if (!isEnum(class_)) {
			return null;
		}

		return Enum.getEnumValues(class_);
	}

	/**
	* replacement for Class.isAnonymousClass()
	*/
	public static boolean isAnonymousClass(Class class_) {
		return getSimpleName(class_).length() == 0;
	}

	/**
	* replacement for Class.getSimpleName()
	*/
	public static String getSimpleName(Class class_) {
		if (class_.isArray()) {
			return class_.getName();
			//return getSimpleName(class_.getComponentType()) + "[]";
		}

		String className = class_.getName();

		int i = className.lastIndexOf('$');
		if (i != -1) {
			do {
				i++;
			} while (i < className.length() && Character.isDigit(className.charAt(i)));
			return className.substring(i);
		}

		return className.substring(className.lastIndexOf('.') + 1);
	}

	/**
	 * replacement for Class.isSynthetic()
	 */
	public static boolean isSynthetic(Class class_) {
		throw new UnsupportedOperationException("NotImplemented");
	}

	/*public static TypeVariable[] getTypeParameters(Class class_)
			throws GenericSignatureFormatError {
		return ReflectionDescriptor.getReflectionDescriptor(class_).getTypeParameters();
	}

	public static Type getGenericSuperclass(Class class_)
			throws GenericSignatureFormatError, TypeNotPresentException,
			MalformedParameterizedTypeException
	{
		return ReflectionDescriptor.getReflectionDescriptor(class_).getGenericSuperclass();
	}

	public static Type[] getGenericInterfaces(Class class_) throws GenericSignatureFormatError,
			TypeNotPresentException, MalformedParameterizedTypeException
	{
		return ReflectionDescriptor.getReflectionDescriptor(class_).getGenericInterfaces();
	}

	public static Method getEnclosingMethod(Class class_) {
		return ReflectionDescriptor.getReflectionDescriptor(class_).getEnclosingMethod();
	}

	public static Constructor<?> getEnclosingConstructor(Class class_) {
		return ReflectionDescriptor.getReflectionDescriptor(class_).getEnclosingConstructor();
	}

	public static Class<?> getEnclosingClass(Class class_) {
		return ReflectionDescriptor.getReflectionDescriptor(class_).getEnclosingClass();
	}*/

	public static String getCanonicalName(Class class_) {
		if (class_.isArray()) {
			//Class component = class_.getComponentType();
			//String s = getCanonicalName(component);
			//return s == null ? null : s + "[]";
			return class_.getName();
		}

		/*if (isLocalClass(class_) || isAnonymousClass(class_)) {
			return null;
		}*/

		return class_.getName().replace('$', '.');
	}

	/*public static boolean isLocalClass(Class class_) {
		return getEnclosingMethod(class_) != null && !isAnonymousClass(class_);
	}

	public static boolean isMemberClass(Class class_) {
		if (getEnclosingClass(class_) != null) {
			return !(isLocalClass(class_) || isAnonymousClass(class_));
		}
		return false;
	}*/

}
