public class JsWorkerProtocolApp {
    static final Object LOCK = new Object();
    static boolean ready;

    static class Worker extends Thread {
        public void run() {
            synchronized (LOCK) {
                while (!ready) {
                    try {
                        LOCK.wait(1000);
                    } catch (InterruptedException err) {
                        System.exit(900);
                        return;
                    }
                }
            }
            System.exit(321);
        }
    }

    public static void main(String[] args) throws Exception {
        Worker worker = new Worker();
        worker.start();
        while (!worker.isAlive()) {
            Thread.sleep(1);
        }
        synchronized (LOCK) {
            ready = true;
            LOCK.notifyAll();
        }
        worker.join();
    }
}
