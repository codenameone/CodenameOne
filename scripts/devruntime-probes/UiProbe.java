import com.codename1.ui.*;
import com.codename1.ui.events.*;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.*;
public class UiProbe {
    public static void main(String[] a) {
        Form f = new Form("Ui", BoxLayout.y());
        final Label out = new Label("idle");
        Button b = new Button("press");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { out.setText("pressed"); }
        });
        f.add(out).add(b);
        DefaultListModel<String> model = new DefaultListModel<String>(new String[]{"one","two"});
        f.add(new com.codename1.ui.List<String>(model));
        f.getToolbar().addCommandToRightBar("Cmd", null, e -> out.setText("cmd"));
        f.show();
        b.pressed(); b.released();
        System.out.println("PROBE UiProbe: after=" + out.getText() + " model=" + model.getSize()
            + " title=" + f.getTitle());
    }
}
