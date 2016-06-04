package java.lang.annotation;

/**
 * A mirror of java.lang.Annotation.
 * 
 * @author Toby Reyelts
 */
public interface Annotation {

	/**
	 * Returns the annotation type of this annotation.
	 */
	Class<? extends Annotation> annotationType();

	/**
	 * Returns true if the specified object represents an annotation that is
	 * logically equivalent to this one.
	 */
	boolean equals(Object obj);

	/**
	 * Returns the hash code of this annotation, as defined below:
	 */
	int hashCode();

	/**
	 * Returns a string representation of this annotation.
	 */
	String toString();

}
