package java.lang;
/**
 * Every Java application has a single instance of class Runtime that allows the application to interface with the environment in which the application is running. The current runtime can be obtained from the getRuntime method.
 * An application cannot create its own instance of this class.
 * Since: JDK1.0, CLDC 1.0 See Also:getRuntime()
 */
public class Runtime{
    /**
     * Terminates the currently running Java application. This method never returns normally.
     * The argument serves as a status code; by convention, a nonzero status code indicates abnormal termination.
     */
    public void exit(int status){
        return; //TODO codavaj!!
    }

    /**
     * Returns the amount of free memory in the system. Calling the gc method may result in increasing the value returned by freeMemory.
     */
    public long freeMemory(){
        return 0l; //TODO codavaj!!
    }

    /**
     * Runs the garbage collector. Calling this method suggests that the Java Virtual Machine expend effort toward recycling unused objects in order to make the memory they currently occupy available for quick reuse. When control returns from the method call, the Java Virtual Machine has made its best effort to recycle all discarded objects.
     * The name gc stands for "garbage collector". The Java Virtual Machine performs this recycling process automatically as needed even if the gc method is not invoked explicitly.
     * The method System.gc() is the conventional and convenient means of invoking this method.
     */
    public void gc(){
        return; //TODO codavaj!!
    }

    /**
     * Returns the runtime object associated with the current Java application. Most of the methods of class Runtime are instance methods and must be invoked with respect to the current runtime object.
     */
    public static java.lang.Runtime getRuntime(){
        return null; //TODO codavaj!!
    }

    /**
     * Returns the total amount of memory in the Java Virtual Machine. The value returned by this method may vary over time, depending on the host environment.
     * Note that the amount of memory required to hold an object of any given type may be implementation-dependent.
     */
    public long totalMemory(){
        return 0l; //TODO codavaj!!
    }

}
