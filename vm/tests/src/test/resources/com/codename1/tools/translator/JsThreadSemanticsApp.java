public class JsThreadSemanticsApp {
    static final Object LOCK = new Object();
    static final Object INTERRUPT_LOCK = new Object();
    static volatile boolean ready;
    public static volatile int result;

    static class WaitingWorker extends Thread {
        public void run() {
            synchronized (LOCK) {
                result |= 1;
                while (!ready) {
                    try {
                        LOCK.wait(1000);
                    } catch (InterruptedException err) {
                        result |= 2;
                        return;
                    }
                }
                result |= 4;
            }
        }
    }

    static class SleepWorker extends Thread {
        public void run() {
            try {
                Thread.sleep(5);
                result |= 8;
            } catch (InterruptedException err) {
                result |= 16;
            }
        }
    }

    static class InterruptSleepWorker extends Thread {
        public void run() {
            try {
                Thread.sleep(1000);
                result |= 32;
            } catch (InterruptedException err) {
                result |= 64;
                if (!Thread.interrupted()) {
                    result |= 128;
                }
                if (!isInterrupted()) {
                    result |= 256;
                }
            }
        }
    }

    static class InterruptWaitWorker extends Thread {
        public void run() {
            synchronized (INTERRUPT_LOCK) {
                result |= 512;
                try {
                    INTERRUPT_LOCK.wait(1000);
                } catch (InterruptedException err) {
                    result |= 1024;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        WaitingWorker waiting = new WaitingWorker();
        waiting.start();
        while ((result & 1) == 0) {
            Thread.sleep(1);
        }
        synchronized (LOCK) {
            ready = true;
            LOCK.notifyAll();
        }
        waiting.join();
        if (!waiting.isAlive()) {
            result |= 2048;
        }

        SleepWorker sleeper = new SleepWorker();
        sleeper.start();
        sleeper.join();
        if (!sleeper.isAlive()) {
            result |= 4096;
        }

        InterruptSleepWorker interruptedSleep = new InterruptSleepWorker();
        interruptedSleep.start();
        Thread.sleep(1);
        interruptedSleep.interrupt();
        interruptedSleep.join();
        if (!interruptedSleep.isAlive()) {
            result |= 8192;
        }

        InterruptWaitWorker interruptedWait = new InterruptWaitWorker();
        interruptedWait.start();
        while ((result & 512) == 0) {
            Thread.sleep(1);
        }
        interruptedWait.interrupt();
        interruptedWait.join();
        if (!interruptedWait.isAlive()) {
            result |= 16384;
        }
    }
}
