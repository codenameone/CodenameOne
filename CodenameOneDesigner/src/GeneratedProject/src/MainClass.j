package mainPackageName;

import userclasses.StateMachine;

/**
 * This is the main lifecycle class for the application, the methods within this class
 * are invoked by the device callbacks. 
 *
 * @author Shai Almog
 */
public class MainClass {
    /**
     * Invoked to initialize the lifecycle
     * 
     * @param o platform specific handle object
     */
    public void init(Object o) {
    }
    
    /**
     * Invoked to start or resume the application
     */
    public void start() {
        new StateMachine("/resourceFileNameNoExt");
    }
    
    /**
     * Invoked when the application is exited or paused
     */
    public void stop() {
    }
    
    /**
     * Invoked to completely cleanup the application
     */
    public void destroy() {
    }    
}
