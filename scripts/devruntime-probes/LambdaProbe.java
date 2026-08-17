import com.codename1.ui.*;
import com.codename1.ui.events.*;
import java.util.*;
public class LambdaProbe {
    interface Op { int apply(int v); default Op twice() { return v -> apply(apply(v)); } }
    public static void main(String[] a) {
        Op inc = v -> v + 1;
        Runnable r = () -> System.out.println("PROBE LambdaProbe: lambda-runnable ran");
        r.run();
        java.util.List<String> l = new ArrayList<String>();
        l.add("b"); l.add("a");
        Collections.sort(l, (p, q) -> p.compareTo(q));
        ActionListener al = evt -> System.out.println("PROBE LambdaProbe: listener fired");
        al.actionPerformed(new ActionEvent(null));
        System.out.println("PROBE LambdaProbe: inc=" + inc.apply(1)
            + " twice=" + inc.twice().apply(1) + " sorted=" + l);
        new Form("Lambda").show();
    }
}
