import com.codename1.ui.*;
import com.codename1.ui.events.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.plaf.*;
public class NavProbe {
    static Form home;
    static Form detail(String item) {
        Form f = new Form(item, BoxLayout.y());
        f.add(new Label("detail for " + item));
        f.getToolbar().setBackCommand("Back", e -> home.showBack());
        return f;
    }
    public static void main(String[] a) {
        home = new Form("Items", BoxLayout.y());
        for (final String s : new String[]{"alpha","beta"}) {
            Button b = new Button(s);
            b.addActionListener(e -> detail(s).show());
            home.add(b);
        }
        home.add(new Label(UIManager.getInstance().getThemeConstant("x", "themed-ok")));
        home.show();
        System.out.println("PROBE NavProbe: kids=" + home.getContentPane().getComponentCount()
            + " current=" + Display.getInstance().getCurrent().getTitle());
    }
}
