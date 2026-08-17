import com.codename1.ui.*;
import com.codename1.io.Util;
import com.codename1.ui.util.Resources;
import java.io.InputStream;

/**
 * A program that brings its own resources.
 *
 * The plain stream proves the implementation layer serves them. Resources.open
 * is the one that matters: it resolves inside the framework, which asks the
 * implementation directly and never passes through anything the interpreter
 * sees -- so if the hook were on Display instead, this line would still load
 * the host app's file.
 */
public class ResourceProbe {
    public static void main(String[] a) {
        String text = "?";
        String res = "?";
        try {
            InputStream in = Display.getInstance().getResourceAsStream(null, "/pushed.txt");
            text = in == null ? "missing" : Util.readToString(in).trim();
        } catch (Exception e) {
            text = "threw " + e;
        }
        try {
            Resources r = Resources.open("/pushed.res");
            res = "opened themes=" + r.getThemeResourceNames().length;
        } catch (Exception e) {
            res = "threw " + e;
        }
        System.out.println("PROBE ResourceProbe: text=" + text + " res=" + res);
        new Form("Resource").show();
    }
}
