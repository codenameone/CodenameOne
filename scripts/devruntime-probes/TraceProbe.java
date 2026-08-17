import com.codename1.ui.*;
public class TraceProbe {
    static void deep(int n) { if (n == 0) { throw new IllegalStateException("from depth 0"); } deep(n - 1); }
    public static void main(String[] a) {
        deep(3);
    }
}
