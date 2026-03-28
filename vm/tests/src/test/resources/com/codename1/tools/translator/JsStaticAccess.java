public class JsStaticAccess {
    static int value = 7;

    static int twice() {
        return value + value;
    }

    public static void main(String[] args) {
        System.exit(twice());
    }
}
