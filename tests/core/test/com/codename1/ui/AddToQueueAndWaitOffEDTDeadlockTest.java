/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import static com.codename1.ui.CN.callSeriallyAndWait;

/**
 * This test verifies that Network requests sent synchronously on a thread other 
 * than the EDT will not be blocked if the EDT is locked.
 * @author shannah
 */
public class AddToQueueAndWaitOffEDTDeadlockTest extends AbstractTest {
        final Object lock = new Object();
        long latency;
        @Override
        public boolean runTest() throws Exception {
            
            Thread t2 = new Thread(()-> {
                ConnectionRequest req = new ConnectionRequest();
                req.setUrl("https://www.codenameone.com");
                long start = System.currentTimeMillis();
                System.out.println("About to send request");
                NetworkManager.getInstance().addToQueueAndWait(req);
                System.out.println("Request complete");
                latency = System.currentTimeMillis()-start;
                
            });
            
            Runnable task = ()->{
                t2.start();
                synchronized(lock) {
                    System.out.println("On edt sleeping");
                    Util.sleep(5000);
                    System.out.println("On edt finished sleeping");
                }
            };
            
            callSeriallyAndWait(task);
            t2.join();
            assertTrue(latency > 0 && latency < 4000, "Network request should return in less than 5000ms. It must be locking");
            return true;
        }
                    
            
            
        

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
        
        
        
    }
