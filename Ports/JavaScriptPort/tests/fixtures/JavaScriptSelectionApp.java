import com.codename1.impl.html5.JavaScriptPortBootstrap;
import com.codename1.system.Lifecycle;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;

/** Browser regression fixture for issues 5943, 5944 and 5946. */
public class JavaScriptSelectionApp extends Lifecycle {
    public static void main(String[] args) {
        JavaScriptPortBootstrap.bootstrap(new JavaScriptSelectionApp());
    }

    @Override
    public void init(Object context) {
        String query = Display.getInstance().getProperty("browser.window.location.search", "");
        Display.getInstance().setProperty("javascript.textSelection", query.indexOf("selection=off") >= 0 ? "false" : "true");
        // The initial default Font wraps a null native handle. Android theme
        // initialization measures this before replacing the default (#5943).
        Font font = Font.getDefaultFont();
        if (font.getHeight() <= 0 || font.stringWidth("Default font") <= 0
                || font.charWidth('M') <= 0 || font.charsWidth(new char[] {'M'}, 0, 1) <= 0) {
            throw new IllegalStateException("Default font metrics failed");
        }
        super.init(context);
    }

    @Override
    public void runApp() {
        Form form = new Form("Selection regressions", BoxLayout.y());
        TextField title = new TextField("Title");
        title.setName("selectionTitle");
        TextArea notes = new TextArea("Caffè, perché, città, più, però.\nSecond paragraph with words.", 4, 24);
        notes.setName("selectionNotes");
        notes.setMaxSize(4096);
        notes.setGrowByContent(true);
        TextArea readOnly = new TextArea("First paragraph wraps across several lines so copying must preserve spaces.\nSecond paragraph remains separate.", 4, 24);
        readOnly.setEditable(false);
        readOnly.setName("selectionReadOnly");
        Label status = new Label("Ready");
        Button action = new Button("Run action");
        action.addActionListener(e -> status.setText("Action fired"));
        Button dialog = new Button("Open dialog");
        dialog.addActionListener(e -> Dialog.show("Selection dialog", "Modal text", "OK", null));
        form.addAll(title, notes, readOnly, action, dialog, status);
        TextArea appOwned = new TextArea("Application-owned interaction");
        appOwned.setName("selectionAppOwned");
        appOwned.addActionListener(e -> status.setText("Custom action fired"));
        form.add(appOwned);
        TextArea notSelectable = new TextArea("Selection explicitly disabled");
        notSelectable.setName("selectionDisabled");
        notSelectable.setEditable(false);
        notSelectable.setTextSelectionEnabled(false);
        form.add(notSelectable);
        for (int i = 0; i < 18; i++) form.add(new Label("Scrollable row " + i));
        form.show();
    }
}
