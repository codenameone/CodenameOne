public class JsThreadingApp {
    static class Shared {
        private boolean ready;
        synchronized void signal() {
            ready = true;
            notifyAll();
        }
        synchronized void waitForSignal() {
            while (!ready) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }
    public static void main(String[] args) {
        final Shared shared = new Shared();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                shared.waitForSignal();
            }
        });
        worker.start();
        shared.signal();
    }
}
