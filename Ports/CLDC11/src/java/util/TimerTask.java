/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util;

public abstract class TimerTask implements Runnable {
    protected TimerTask() {}

    public boolean cancel() {
        return false;
    }
    public long scheduledExecutionTime() {
        return 0;
    }
}
