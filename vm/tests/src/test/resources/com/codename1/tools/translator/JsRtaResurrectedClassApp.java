public class JsRtaResurrectedClassApp {
    public static int result;

    private static final class Worker extends Thread {
        public void run() {
            result = new HiddenValue().get();
        }
    }

    private static final class HiddenValue {
        int get() {
            return 73;
        }
    }

    public static void main(String[] args) throws Exception {
        Worker worker = new Worker();
        worker.start();
        worker.join();
    }
}
