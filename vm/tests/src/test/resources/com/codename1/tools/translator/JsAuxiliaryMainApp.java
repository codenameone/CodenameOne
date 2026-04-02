public class JsAuxiliaryMainApp {
    public static void main(String[] args) {
        System.out.println(run());
    }

    public static String run() {
        return new HelperMain().message();
    }

    static class HelperMain {
        public static void main(String[] args) {
            System.out.println("helper");
        }

        String message() {
            return "ok";
        }
    }
}
