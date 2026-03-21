public class JsHello {
    private static native void report(String msg);
    public static void main(String[] args) {
        report("Hello JS");
    }
}
