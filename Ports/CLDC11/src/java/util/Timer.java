/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util;

public class Timer {
    public Timer() {}
    
    public void cancel() {}
    
    public void schedule(TimerTask task, Date time) {}
    public void schedule(TimerTask task, Date firstTime, long period) {}
    public void schedule(TimerTask task, long delay) {}
    public void schedule(TimerTask task, long delay, long period) {}
    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {}
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {}
}
