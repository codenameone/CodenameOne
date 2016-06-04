package java.lang.ref;
/**
 * Abstract base class for reference objects. This class defines the operations common to all reference objects. Because reference objects are implemented in close cooperation with the garbage collector, this class may not be subclassed directly.
 * Since: JDK1.2, CLDC 1.1
 */
public abstract class Reference{
    /**
     * Clears this reference object.
     */
    public void clear(){
        return; //TODO codavaj!!
    }

    /**
     * Returns this reference object's referent. If this reference object has been cleared, either by the program or by the garbage collector, then this method returns null.
     */
    public java.lang.Object get(){
        return null; //TODO codavaj!!
    }

}
