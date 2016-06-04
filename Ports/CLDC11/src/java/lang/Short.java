package java.lang;
/**
 * The Short class is the standard wrapper for short values.
 * Since: JDK1.1, CLDC 1.0
 */
public final class Short{
    /**
     * The maximum value a Short can have.
     * See Also:Constant Field Values
     */
    public static final short MAX_VALUE=32767;

    /**
     * The minimum value a Short can have.
     * See Also:Constant Field Values
     */
    public static final short MIN_VALUE=-32768;

    /**
     * Constructs a Short object initialized to the specified short value.
     * value - the initial value of the Short
     */
    public Short(short value){
         //TODO codavaj!!
    }

    /**
     * Compares this object to the specified object.
     */
    public boolean equals(java.lang.Object obj){
        return false; //TODO codavaj!!
    }

    /**
     * Returns a hashcode for this Short.
     */
    public int hashCode(){
        return 0; //TODO codavaj!!
    }

    /**
     * Assuming the specified String represents a short, returns that short's value. Throws an exception if the String cannot be parsed as a short. The radix is assumed to be 10.
     */
    public static short parseShort(java.lang.String s) throws java.lang.NumberFormatException{
        return 0; //TODO codavaj!!
    }

    /**
     * Assuming the specified String represents a short, returns that short's value in the radix specified by the second argument. Throws an exception if the String cannot be parsed as a short.
     */
    public static short parseShort(java.lang.String s, int radix) throws java.lang.NumberFormatException{
        return 0; //TODO codavaj!!
    }

    /**
     * Returns the value of this Short as a short.
     */
    public short shortValue(){
        return 0; //TODO codavaj!!
    }

    /**
     * Returns a String object representing this Short's value.
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    public static Short valueOf(short i) {
        return null;
    }
}
