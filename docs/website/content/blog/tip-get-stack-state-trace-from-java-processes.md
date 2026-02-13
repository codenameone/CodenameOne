---
title: 'TIP: Get Stack State (trace) from Java Processes'
slug: tip-get-stack-state-trace-from-java-processes
url: /blog/tip-get-stack-state-trace-from-java-processes/
original_url: https://www.codenameone.com/blog/tip-get-stack-state-trace-from-java-processes.html
aliases:
- /blog/tip-get-stack-state-trace-from-java-processes.html
date: '2016-11-13'
author: Shai Almog
---

![Header Image](/blog/tip-get-stack-state-trace-from-java-processes/just-the-tip.jpg)

One of the most frustrating things that can happen to developers is when you manage to reproduce a rare bug but  
you are not in the debugger when you did that. My kingdom for a stack trace…​  
But this is also pretty frustrating when you work on a tool like Codename One’s designer or GUIBuilder and they  
suddenly freeze with no visible error. How do you provide a viable bug report for that?

This was pretty obvious for me but I’ve programmed in Java from the age that predated IDE’s and surprisingly  
this isn’t common knowledge. On Windows you can just do a ctrl → Break on a Java process and get the  
full JVM dump which includes everything you need to debug the issue. With Mac/Linux you can send a  
`kill -QUIT` message for the same effect which is slightly more of an effort but in some ways more rewarding.

To do this open your console/terminal and type in:
    
    
    ps auxw | grep java

This should provide you with the list of Java processes ID’s, identify the one that is relevant and use:
    
    
    kill -QUIT process-id

Where `process-id` is the number representing the process e.g. running this on a fresh instance of the Codename One  
designer shows how powerful this tool is. You can see the current state of every thread and memory state rather  
easily and without any special tools.
    
    
    Full thread dump Java HotSpot(TM) 64-Bit Server VM (25.45-b02 mixed mode):
    
    "TimerQueue" #36 daemon prio=5 os_prio=31 tid=0x00007fbd49408000 nid=0x1141f waiting on condition [0x000070000260b000]
       java.lang.Thread.State: WAITING (parking)
    	at sun.misc.Unsafe.park(Native Method)
    	- parking to wait for  <0x000000076da064e0> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
    	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
    	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)
    	at java.util.concurrent.DelayQueue.take(DelayQueue.java:211)
    	at javax.swing.TimerQueue.run(TimerQueue.java:171)
    	at java.lang.Thread.run(Thread.java:745)
    
    "qtp984828826-34" #34 prio=5 os_prio=31 tid=0x00007fbd49af7000 nid=0x10b03 waiting on condition [0x0000700002508000]
       java.lang.Thread.State: TIMED_WAITING (parking)
    	at sun.misc.Unsafe.park(Native Method)
    	- parking to wait for  <0x000000076d19ac80> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
    	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
    	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
    	at org.eclipse.jetty.util.BlockingArrayQueue.poll(BlockingArrayQueue.java:337)	at org.eclipse.jetty.util.BlockingArrayQueue.poll(BlockingArrayQueue.java:337)
    	at org.eclipse.jetty.util.BlockingArrayQueue.poll(BlockingArrayQueue.java:337)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.idleJobPoll(QueuedThreadPool.java:516)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.access$600(QueuedThreadPool.java:39)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:562)
    	at java.lang.Thread.run(Thread.java:745)
    
    "qtp984828826-33" #33 prio=5 os_prio=31 tid=0x00007fbd4ac7b800 nid=0x10903 waiting on condition [0x0000700002405000]
       java.lang.Thread.State: TIMED_WAITING (parking)
    	at sun.misc.Unsafe.park(Native Method)
    	- parking to wait for  <0x000000076d19ac80> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
    	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
    	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
    	at org.eclipse.jetty.util.BlockingArrayQueue.poll(BlockingArrayQueue.java:337)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.idleJobPoll(QueuedThreadPool.java:516)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.access$600(QueuedThreadPool.java:39)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:562)
    	at java.lang.Thread.run(Thread.java:745)
    
    "qtp984828826-32" #32 prio=5 os_prio=31 tid=0x00007fbd4ac9a800 nid=0x10703 waiting on condition [0x0000700002302000]
       java.lang.Thread.State: TIMED_WAITING (parking)
    	at sun.misc.Unsafe.park(Native Method)
    	- parking to wait for  <0x000000076d19ac80> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
    	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
    	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
    	at org.eclipse.jetty.util.BlockingArrayQueue.poll(BlockingArrayQueue.java:337)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.idleJobPoll(QueuedThreadPool.java:516)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.access$600(QueuedThreadPool.java:39)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:562)
    	at java.lang.Thread.run(Thread.java:745)
    
    "qtp984828826-31" #31 prio=5 os_prio=31 tid=0x00007fbd4ac94000 nid=0x10503 waiting on condition [0x00007000021ff000]
       java.lang.Thread.State: TIMED_WAITING (parking)
    	at sun.misc.Unsafe.park(Native Method)
    	- parking to wait for  <0x000000076d19ac80> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
    	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
    	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
    	at org.eclipse.jetty.util.BlockingArrayQueue.poll(BlockingArrayQueue.java:337)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.idleJobPoll(QueuedThreadPool.java:516)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.access$600(QueuedThreadPool.java:39)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:562)
    	at java.lang.Thread.run(Thread.java:745)
    
    "qtp984828826-30" #30 prio=5 os_prio=31 tid=0x00007fbd4aa89000 nid=0x10303 waiting on condition [0x00007000020fc000]
       java.lang.Thread.State: TIMED_WAITING (parking)
    	at sun.misc.Unsafe.park(Native Method)
    	- parking to wait for  <0x000000076d19ac80> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
    	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
    	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
    	at org.eclipse.jetty.util.BlockingArrayQueue.poll(BlockingArrayQueue.java:337)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.idleJobPoll(QueuedThreadPool.java:516)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.access$600(QueuedThreadPool.java:39)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:562)
    	at java.lang.Thread.run(Thread.java:745)
    
    "qtp984828826-29" #29 prio=5 os_prio=31 tid=0x00007fbd4a333800 nid=0x10103 waiting on condition [0x0000700001ff9000]
       java.lang.Thread.State: TIMED_WAITING (parking)
    	at sun.misc.Unsafe.park(Native Method)
    	- parking to wait for  <0x000000076d19ac80> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
    	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)
    	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)
    	at org.eclipse.jetty.util.BlockingArrayQueue.poll(BlockingArrayQueue.java:337)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.idleJobPoll(QueuedThreadPool.java:516)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.access$600(QueuedThreadPool.java:39)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:562)
    	at java.lang.Thread.run(Thread.java:745)
    
    "qtp984828826-28 Selector0" #28 prio=5 os_prio=31 tid=0x00007fbd4a34c000 nid=0xff03 runnable [0x0000700001ef6000]
       java.lang.Thread.State: RUNNABLE
    	at sun.nio.ch.KQueueArrayWrapper.kevent0(Native Method)
    	at sun.nio.ch.KQueueArrayWrapper.poll(KQueueArrayWrapper.java:198)
    	at sun.nio.ch.KQueueSelectorImpl.doSelect(KQueueSelectorImpl.java:103)
    	at sun.nio.ch.SelectorImpl.lockAndDoSelect(SelectorImpl.java:86)
    	- locked <0x000000076d21dc00> (a sun.nio.ch.Util$2)
    	- locked <0x000000076d21db78> (a java.util.Collections$UnmodifiableSet)
    	- locked <0x000000076d21d9f0> (a sun.nio.ch.KQueueSelectorImpl)
    	at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:97)
    	at org.eclipse.jetty.io.nio.SelectorManager$SelectSet.doSelect(SelectorManager.java:560)
    	at org.eclipse.jetty.io.nio.SelectorManager$1.run(SelectorManager.java:277)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:598)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:533)
    	at java.lang.Thread.run(Thread.java:745)
    
    "qtp984828826-27 Acceptor0 [[email protected]](/cdn-cgi/l/email-protection):9000 STARTING" #27 prio=5 os_prio=31 tid=0x00007fbd4a230000 nid=0xfb13 runnable [0x0000700001df3000]
       java.lang.Thread.State: RUNNABLE
    	at sun.nio.ch.ServerSocketChannelImpl.accept0(Native Method)
    	at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:422)
    	at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:250)
    	- locked <0x000000076d1a3bf8> (a java.lang.Object)
    	at org.eclipse.jetty.server.nio.SelectChannelConnector.accept(SelectChannelConnector.java:97)
    	at org.eclipse.jetty.server.AbstractConnector$Acceptor.run(AbstractConnector.java:833)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:598)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:533)
    	at java.lang.Thread.run(Thread.java:745)
    
    "HashSessionScavenger-0" #26 daemon prio=6 os_prio=31 tid=0x00007fbd493cf000 nid=0xfd0b in Object.wait() [0x0000700001cf0000]
       java.lang.Thread.State: TIMED_WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	at java.util.TimerThread.mainLoop(Timer.java:552)
    	- locked <0x000000076d18f048> (a java.util.TaskQueue)
    	at java.util.TimerThread.run(Timer.java:505)
    
    "Thread-5" #24 prio=6 os_prio=31 tid=0x00007fbd4a2de000 nid=0x671f in Object.wait() [0x0000700001aea000]
       java.lang.Thread.State: WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	- waiting on <0x000000076d092240> (a java.lang.Object)
    	at java.lang.Object.wait(Object.java:502)
    	at org.eclipse.jetty.util.thread.QueuedThreadPool.join(QueuedThreadPool.java:385)
    	- locked <0x000000076d092240> (a java.lang.Object)
    	at org.eclipse.jetty.server.Server.join(Server.java:403)
    	at com.codename1.designer.LocalServer$1.run(LocalServer.java:73)
    
    "Network Thread" #21 prio=5 os_prio=31 tid=0x00007fbd49372800 nid=0xed03 in Object.wait() [0x00007000019e7000]
       java.lang.Thread.State: WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	- waiting on <0x00000006c0945408> (a java.lang.Object)
    	at java.lang.Object.wait(Object.java:502)
    	at com.codename1.io.NetworkManager$NetworkThread.run(NetworkManager.java:337)
    	- locked <0x00000006c0945408> (a java.lang.Object)
    	at com.codename1.impl.CodenameOneThread.run(CodenameOneThread.java:176)
    
    "DestroyJavaVM" #18 prio=5 os_prio=31 tid=0x00007fbd4911d800 nid=0x1703 waiting on condition [0x0000000000000000]
       java.lang.Thread.State: RUNNABLE
    
    "EDT" #17 prio=6 os_prio=31 tid=0x00007fbd49956000 nid=0xe613 in Object.wait() [0x00007000017e1000]
       java.lang.Thread.State: WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	- waiting on <0x00000006c01b28c8> (a java.lang.Object)
    	at java.lang.Object.wait(Object.java:502)
    	at com.codename1.ui.Display.mainEDTLoop(Display.java:959)
    	- locked <0x00000006c01b28c8> (a java.lang.Object)
    	at com.codename1.ui.RunnableWrapper.run(RunnableWrapper.java:120)
    	at com.codename1.impl.CodenameOneThread.run(CodenameOneThread.java:176)
    
    "Timer-0" #15 daemon prio=5 os_prio=31 tid=0x00007fbd4a18a800 nid=0xe403 in Object.wait() [0x00007000016de000]
       java.lang.Thread.State: WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	at java.lang.Object.wait(Object.java:502)
    	at java.util.TimerThread.mainLoop(Timer.java:526)
    	- locked <0x00000006c019aef8> (a java.util.TaskQueue)
    	at java.util.TimerThread.run(Timer.java:505)
    
    "Java2D Disposer" #14 daemon prio=10 os_prio=31 tid=0x00007fbd4a988000 nid=0xe203 in Object.wait() [0x00007000015db000]
       java.lang.Thread.State: WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	- waiting on <0x00000006c01b2d68> (a java.lang.ref.ReferenceQueue$Lock)
    	at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:143)
    	- locked <0x00000006c01b2d68> (a java.lang.ref.ReferenceQueue$Lock)
    	at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:164)
    	at sun.java2d.Disposer.run(Disposer.java:148)
    	at java.lang.Thread.run(Thread.java:745)
    
    "Java2D Queue Flusher" #13 daemon prio=10 os_prio=31 tid=0x00007fbd4a1dc000 nid=0xce07 in Object.wait() [0x00007000014d8000]
       java.lang.Thread.State: TIMED_WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	at sun.java2d.opengl.OGLRenderQueue$QueueFlusher.run(OGLRenderQueue.java:203)
    	- locked <0x00000006c019af48> (a sun.java2d.opengl.OGLRenderQueue$QueueFlusher)
    
    "AWT-EventQueue-0" #12 prio=6 os_prio=31 tid=0x00007fbd491dd000 nid=0xc007 waiting on condition [0x00007000013d5000]
       java.lang.Thread.State: WAITING (parking)
    	at sun.misc.Unsafe.park(Native Method)
    	- parking to wait for  <0x00000006c01b4860> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
    	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
    	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)
    	at java.awt.EventQueue.getNextEvent(EventQueue.java:554)
    	at java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:170)
    	at java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:116)
    	at java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:105)
    	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:101)
    	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:93)
    	at java.awt.EventDispatchThread.run(EventDispatchThread.java:82)
    
    "AWT-Shutdown" #11 prio=5 os_prio=31 tid=0x00007fbd4914d800 nid=0x6b23 in Object.wait() [0x00007000011cc000]
       java.lang.Thread.State: WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	at java.lang.Object.wait(Object.java:502)
    	at sun.awt.AWTAutoShutdown.run(AWTAutoShutdown.java:295)
    	- locked <0x00000006c019b248> (a java.lang.Object)
    	at java.lang.Thread.run(Thread.java:745)
    
    "AppKit Thread" #10 daemon prio=5 os_prio=31 tid=0x00007fbd498d8000 nid=0xa0b runnable [0x0000000000000000]
       java.lang.Thread.State: RUNNABLE
    
    "Service Thread" #8 daemon prio=9 os_prio=31 tid=0x00007fbd4a01a800 nid=0x4903 runnable [0x0000000000000000]
       java.lang.Thread.State: RUNNABLE
    
    "C1 CompilerThread2" #7 daemon prio=9 os_prio=31 tid=0x00007fbd4a019800 nid=0x4703 waiting on condition [0x0000000000000000]
       java.lang.Thread.State: RUNNABLE
    
    "C2 CompilerThread1" #6 daemon prio=9 os_prio=31 tid=0x00007fbd4a014800 nid=0x4503 waiting on condition [0x0000000000000000]
       java.lang.Thread.State: RUNNABLE
    
    "C2 CompilerThread0" #5 daemon prio=9 os_prio=31 tid=0x00007fbd4902e000 nid=0x4303 waiting on condition [0x0000000000000000]
       java.lang.Thread.State: RUNNABLE
    
    "Signal Dispatcher" #4 daemon prio=9 os_prio=31 tid=0x00007fbd4a013800 nid=0x360f waiting on condition [0x0000000000000000]
       java.lang.Thread.State: RUNNABLE
    
    "Finalizer" #3 daemon prio=8 os_prio=31 tid=0x00007fbd4a83b000 nid=0x3003 in Object.wait() [0x000070000092e000]
       java.lang.Thread.State: WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	- waiting on <0x00000006c02124a0> (a java.lang.ref.ReferenceQueue$Lock)
    	at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:143)
    	- locked <0x00000006c02124a0> (a java.lang.ref.ReferenceQueue$Lock)
    	at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:164)
    	at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:209)
    
    "Reference Handler" #2 daemon prio=10 os_prio=31 tid=0x00007fbd4a83a800 nid=0x2e03 in Object.wait() [0x000070000082b000]
       java.lang.Thread.State: WAITING (on object monitor)
    	at java.lang.Object.wait(Native Method)
    	- waiting on <0x00000006c019b608> (a java.lang.ref.Reference$Lock)
    	at java.lang.Object.wait(Object.java:502)
    	at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:157)
    	- locked <0x00000006c019b608> (a java.lang.ref.Reference$Lock)
    
    "VM Thread" os_prio=31 tid=0x00007fbd49018800 nid=0x2c03 runnable
    
    "GC task thread#0 (ParallelGC)" os_prio=31 tid=0x00007fbd4980a000 nid=0x2403 runnable
    
    "GC task thread#1 (ParallelGC)" os_prio=31 tid=0x00007fbd4980a800 nid=0x2603 runnable
    
    "GC task thread#2 (ParallelGC)" os_prio=31 tid=0x00007fbd4980b000 nid=0x2803 runnable
    
    "GC task thread#3 (ParallelGC)" os_prio=31 tid=0x00007fbd49008800 nid=0x2a03 runnable
    
    "VM Periodic Task Thread" os_prio=31 tid=0x00007fbd4a06b000 nid=0x4b03 waiting on condition
    
    JNI global references: 807
    
    Heap
     PSYoungGen      total 76288K, used 50593K [0x000000076ab00000, 0x0000000770000000, 0x00000007c0000000)
      eden space 65536K, 77% used [0x000000076ab00000,0x000000076dc685b8,0x000000076eb00000)
      from space 10752K, 0% used [0x000000076eb00000,0x000000076eb00000,0x000000076f580000)
      to   space 10752K, 0% used [0x000000076f580000,0x000000076f580000,0x0000000770000000)
     ParOldGen       total 126976K, used 11704K [0x00000006c0000000, 0x00000006c7c00000, 0x000000076ab00000)
      object space 126976K, 9% used [0x00000006c0000000,0x00000006c0b6e050,0x00000006c7c00000)
     Metaspace       used 25623K, capacity 25970K, committed 26240K, reserved 1073152K
      class space    used 3249K, capacity 3341K, committed 3456K, reserved 1048576K

### Visual VM

You can take this a step further with `jvisualvm` which is like a console for your running JVM processes. You can  
get exactly the result outlined above and far more by just selecting the process matching the JVM, selecting the  
threads view and their dump:

![Visual VM showing the stack trace for the designer tool](/blog/tip-get-stack-state-trace-from-java-processes/visualvm.png)

Figure 1. Visual VM showing the stack trace for the designer tool

This tool is pretty powerful and can provide a lot of insight into the on-goings of Java processes.

### Submitting Stacks

When you run into an issue that might benefit from the stacks in the simulator it’s the time to pull out these  
tools and point them at the process. An issue that includes the full JVM stack states would often be easier to  
solve especially if it’s a deadlock issues.

If you have an issue on an Android device check out [this stackoverflow thread](http://stackoverflow.com/questions/13589074/how-to-make-java-thread-dump-in-android)  
where you can see some information on listing the thread states on a physical device.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
