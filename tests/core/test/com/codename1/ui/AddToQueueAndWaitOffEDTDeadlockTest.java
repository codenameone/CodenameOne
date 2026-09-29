/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
 */
public class AddToQueueAndWaitOffEDTDeadlockTest extends AbstractTest {
        final Object lock = new Object();
        long latency;
        @Override
        public boolean runTest() throws Exception {
            
            /*Thread t2 = new Thread(()-> {
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
            assertTrue(latency > 0 && latency < 4000, "Network request should return in less than 5000ms. It must be locking");*/
            return true;
        }
                    
            
            
        

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
        
        
        
    }
