/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.util;

public class Timer {
    boolean canceled;
    private String name;
    
    public Timer(boolean isDaemon) {
        
    }
    
    public Timer(String name) {
        this.name = name;
    }
    
    public Timer() {}
    
    public void cancel() {
        canceled = true;
    }
    
    public void schedule(TimerTask task, Date time) {
        schedule(task, time.getTime() - System.currentTimeMillis());
    }
    
    public void schedule(TimerTask task, Date firstTime, long period) {
        task.initialDelay = firstTime.getTime() - System.currentTimeMillis();
        task.repeatDelay = period;
        T t = new T();
        t.task = task;
        t.start();
    }
    
    public void schedule(TimerTask task, long delay) {
        task.initialDelay = delay;
        T t = new T();
        t.task = task;
        t.start();
    }
    
    public void schedule(TimerTask task, long delay, long period) {
        task.initialDelay = delay;
        task.repeatDelay = period;
        T t = new T();
        t.task = task;
        t.start();
    }
    
    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        task.initialDelay = firstTime.getTime() - System.currentTimeMillis();
        task.repeatDelay = period;
        T t = new T();
        t.task = task;
        t.start();
    }
    
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        task.initialDelay = delay;
        task.repeatDelay = period;
        T t = new T();
        t.task = task;
        t.start();
    }
    
    class T extends Thread {
        TimerTask task;
        public void run() {
            task.parent = Timer.this;
            try {
                task.runImpl();
            } catch(InterruptedException i) {
                i.printStackTrace();
            }
        }
    }
}
