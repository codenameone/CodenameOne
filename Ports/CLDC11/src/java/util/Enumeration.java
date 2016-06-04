package java.util;
/**
 * An object that implements the Enumeration interface generates a series of elements, one at a time. Successive calls to the nextElement method return successive elements of the series.
 * For example, to print all elements of a vector v:
 * Methods are provided to enumerate through the elements of a vector, the keys of a hashtable, and the values in a hashtable.
 * Since: JDK1.0, CLDC 1.0 Version: 12/17/01 (CLDC 1.1) See Also:nextElement(), Hashtable, Hashtable.elements(), Hashtable.keys(), Vector, Vector.elements()
 */
public interface Enumeration<T>{
    /**
     * Tests if this enumeration contains more elements.
     */
    abstract boolean hasMoreElements();

    /**
     * Returns the next element of this enumeration if this enumeration object has at least one more element to provide.
     */
    abstract T nextElement();

}
