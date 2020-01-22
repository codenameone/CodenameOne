/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;

/**
 *
 * @author shannah
 */
public class InvokeAndBlockTest extends AbstractTest {

        @Override
        public boolean shouldExecuteOnEDT() {
            return true;
        }

        @Override
        public boolean runTest() throws Exception {
            StringBuffer sb = new StringBuffer();
            
            // 1st leg of the test is basic and has worked correctly 
            // always.  The first callSerially is added to the pending serial calls
            // array.
            // the InvokeAndBlock will then block this method - but will allow
            // the pending serial calls to run.
            
            CN.callSerially(()->{;
                sb.append("A");
            });
            sb.append("B");
            CN.invokeAndBlock(()->{
                Util.sleep(500);
                sb.append("D");
            });
            sb.append("C");
            
            assertEqual("BADC", sb.toString(), "Calls run in wrong order");
            
            
            // The 2nd leg of the test used to perform undesirably.  Essentially
            // the first callSerially() would not run until the invokeAndBlock
            // in the 2nd callSerially was complete.
            // This is because Both the first and second callSeriallys() are
            // included in the same pendingSerialCalls queue, 
            // and it used to be copied as a batch to a local array 
            // inside processSerialCalls().  
            // If any runnable inside that batch blocked, it would prevent
            // the others from running until it was unblocked.
            
            // I have corrected this now so that invokeAndBlock is able to pull
            // the rest of the runnables in the batch and re-add them to the pending
            // serial calls list, so that they will be executed by the new EDT.
            
            CN.callSerially(()->{
                sb.append("E");
            });
            CN.callSerially(()->{
                sb.append("G");
                CN.invokeAndBlock(()->{
                    Util.sleep(500);
                    sb.append("F");
                });
                sb.append("H");
            });
            CN.callSerially(()->{
                sb.append("I");
            });
            CN.invokeAndBlock(()->{
                Util.sleep(1000);
                // Give them all a chance to finish.
            });
            
            
            assertEqual("BADCEGIFH", sb.toString(), "Calls run in wrong order");
            
            sb.setLength(0);
            // Finally, this test ensures that the invokeAndBlock call be allowed to 
            // proceed in between callSerially runnables that are in the queue.
            CN.callSerially(()->{
                sb.append("E");
            });
            CN.callSerially(()->{
                sb.append("G");
                CN.invokeAndBlock(()->{
                    Util.sleep(500);
                    sb.append("F");
                });
                sb.append("H");
            });
            CN.callSerially(()->{
                sb.append("I");
            });
            CN.callSerially(()->{
               sb.append("X");
               CN.invokeAndBlock(()->{
                    Util.sleep(1000);
                    sb.append("Y");
               });
               sb.append("Z");
            });
            CN.callSerially(()->sb.append("Q"));
            CN.invokeAndBlock(()->{
                Util.sleep(3000);
                // Give them all a chance to finish.
            });
            assertEqual("EGIXQFYZH", sb.toString(), "Calls in wrong order");
            return true;
        }
    
            
        
        
    }
