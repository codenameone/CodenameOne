import com.codename1.ui.*;
import java.util.*;
public class StaticInitProbe {
    static final java.util.List<String> LOG = new ArrayList<String>();
    static int counter;
    static { LOG.add("clinit"); counter = 42; }
    static class Late { static final String V; static { V = "late-init"; } }
    public static void main(String[] a) {
        System.out.println("PROBE StaticInitProbe: log=" + LOG + " counter=" + counter
            + " late=" + Late.V);
        new Form("StaticInit").show();
    }
}
