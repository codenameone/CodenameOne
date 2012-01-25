package userclasses;

import generated.StateMachineBase;
import com.codename1.ui.*;

/**
 * This is the class where the UI meets your business logic. The GUI builder will only add callbacks
 * to this class which you can override
 *
 * @author Your Name Here
 */
public class StateMachine extends StateMachineBase {

    public StateMachine(String resFile) {
        super(resFile);
        // do not modify, write code in initVars and initialize class members there,
        // the constructor might be invoked too late due to race conditions that might occur
    }

    /**
     * this method should be used to initialize variables instead of
     * the constructor/class scope to avoid race conditions
     */
    protected void initVars() {
    }
}
