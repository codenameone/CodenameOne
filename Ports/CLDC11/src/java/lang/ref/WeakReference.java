package java.lang.ref;
/**
 * This class provides support for weak references. Weak references are most often used to implement canonicalizing mappings. Suppose that the garbage collector determines at a certain point in time that an object is weakly reachable. At that time it will atomically clear all the weak references to that object and all weak references to any other weakly- reachable objects from which that object is reachable through a chain of strong and weak references.
 * Since: JDK1.2, CLDC 1.1
 */
public class WeakReference extends java.lang.ref.Reference{
    /**
     * Creates a new weak reference that refers to the given object.
     */
    public WeakReference(java.lang.Object ref){
         //TODO codavaj!!
    }

}
