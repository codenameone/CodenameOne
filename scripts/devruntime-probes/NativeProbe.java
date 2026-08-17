import com.codename1.ui.*;
import com.codename1.system.*;
public class NativeProbe {
    public interface MyNative extends NativeInterface { String hello(); }
    public static void main(String[] a) {
        String s;
        try {
            MyNative n = (MyNative)NativeLookup.create(MyNative.class);
            s = (n == null) ? "create returned null" : ("isSupported=" + n.isSupported());
        } catch (Throwable t) {
            s = "threw " + t.getClass().getName() + ": " + t.getMessage();
        }
        System.out.println("PROBE NativeProbe: " + s);
        new Form("Native").show();
    }
}
