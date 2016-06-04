package java.lang;
/**
 * The Boolean class wraps a value of the primitive type boolean in an object. An object of type Boolean contains a single field whose type is boolean.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Boolean{
    /**
     * The Boolean object corresponding to the primitive value false.
     */
    public static final java.lang.Boolean FALSE=null;

    /**
     * The Boolean object corresponding to the primitive value true.
     */
    public static final java.lang.Boolean TRUE=null;

    /**
     * Allocates a Boolean object representing the value argument.
     * value - the value of the Boolean.
     */
    public Boolean(boolean value){
         //TODO codavaj!!
    }

    /**
     * Returns the value of this Boolean object as a boolean primitive.
     */
    public boolean booleanValue(){
        return false; //TODO codavaj!!
    }

    /**
     * Returns true if and only if the argument is not null and is a Boolean object that represents the same boolean value as this object.
     */
    public boolean equals(java.lang.Object obj){
        return false; //TODO codavaj!!
    }

    /**
     * Returns a hash code for this Boolean object.
     */
    public int hashCode(){
        return 0; //TODO codavaj!!
    }

    /**
     * Returns a String object representing this Boolean's value. If this object represents the value true, a string equal to "true" is returned. Otherwise, a string equal to "false" is returned.
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    /**
     * Returns the object instance of i
     * @param b the primitive
     * @return object instance
     */
    public static Boolean valueOf(final boolean b) {
            return b ? Boolean.TRUE : Boolean.FALSE;
    }

    public static Boolean valueOf(final String b) {
            return valueOf(parseBoolean(b));
    }

    public static boolean parseBoolean(final String s) {
            return (s != null) && s.equalsIgnoreCase("true");
    }

    public int compareTo(final Boolean b2) {
            return 0;
    }
}
