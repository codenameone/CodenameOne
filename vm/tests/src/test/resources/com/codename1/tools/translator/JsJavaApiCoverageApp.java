import java.lang.reflect.Array;
import java.util.HashMap;

public class JsJavaApiCoverageApp {
    enum Mode {
        ALPHA,
        BETA
    }

    static int result;

    public static void main(String[] args) throws Exception {
        int mask = 0;

        HashMap map = new HashMap();
        map.put("key", "value");
        if ("value".equals(map.get("key"))) {
            mask |= 1;
        }

        Mode mode = Enum.valueOf(Mode.class, "BETA");
        if (mode == Mode.BETA) {
            mask |= 2;
        }

        Object reflectedArray = Array.newInstance(String.class, 2);
        ((String[]) reflectedArray)[0] = "cn1";
        ((String[]) reflectedArray)[1] = "vm";
        if ("[Ljava.lang.String;".equals(reflectedArray.getClass().getName())) {
            mask |= 4;
        }

        if (Class.forName("java.lang.String") == String.class) {
            mask |= 8;
        }

        String formatted = String.format("%s-%d", "cn1", Integer.valueOf(7));
        if ("cn1-7".equals(formatted)) {
            mask |= 16;
        }

        int[] src = new int[] {1, 2, 3};
        int[] dst = new int[3];
        System.arraycopy(src, 0, dst, 0, src.length);
        if (dst[2] == 3) {
            mask |= 32;
        }

        try {
            throw new IllegalStateException("expected");
        } catch (RuntimeException ex) {
            if ("expected".equals(ex.getMessage())) {
                mask |= 64;
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append('o').append("k");
        if ("ok".equals(builder.toString())) {
            mask |= 128;
        }

        result = mask;
        System.exit(mask);
    }
}
