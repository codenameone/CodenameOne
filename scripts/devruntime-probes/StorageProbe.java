import com.codename1.ui.*;
import com.codename1.io.*;
import java.util.*;
public class StorageProbe {
    public static void main(String[] a) {
        StringBuilder r = new StringBuilder();
        Storage.getInstance().writeObject("probe-key", "stored-value");
        r.append("storage=").append(Storage.getInstance().readObject("probe-key"));
        Preferences.set("probe-pref", 99);
        r.append(" pref=").append(Preferences.get("probe-pref", 0));
        java.util.List<String> l = new ArrayList<String>();
        l.add("a"); l.add("b");
        Storage.getInstance().writeObject("probe-list", l);
        r.append(" list=").append(Storage.getInstance().readObject("probe-list"));
        System.out.println("PROBE StorageProbe: " + r);
        new Form("Storage").show();
    }
}
