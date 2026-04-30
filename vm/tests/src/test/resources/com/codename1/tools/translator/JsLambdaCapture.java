public class JsLambdaCapture {
    interface Action {
        void run();
    }

    private final String prefix;
    private final Action action;

    public JsLambdaCapture(String prefix) {
        this.prefix = prefix;
        this.action = () -> System.out.println(this.prefix + "-ok");
    }

    public void run() {
        action.run();
    }

    public static void main(String[] args) {
        new JsLambdaCapture("js").run();
        System.exit(0);
    }
}
