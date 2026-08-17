import com.codename1.ui.*;
public class LoopProbe {
    public static void main(String[] a) {
        System.out.println("PROBE LoopProbe: entering an infinite loop on purpose");
        long n = 0;
        while (true) { n++; }
    }
}
