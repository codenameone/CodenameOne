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

public abstract class TimerTask implements Runnable {
    long lastExecution;
    long initialDelay;
    long repeatDelay;
    boolean canceled;
    
    protected TimerTask() {}

    public boolean cancel() {
        canceled = true;
        return false;
    }
    
    void runImpl() throws InterruptedException {
        lastExecution = System.currentTimeMillis();
        if(initialDelay > 0) {
            Thread.sleep(initialDelay);
        }
        if(canceled) {
            return;
        }
        run();
        if(repeatDelay > 0) {
            while(!canceled) {
                Thread.sleep(repeatDelay);
                lastExecution = System.currentTimeMillis();
                run();
            }
        }
    }
    
    public long scheduledExecutionTime() {
        return lastExecution;
    }
}
