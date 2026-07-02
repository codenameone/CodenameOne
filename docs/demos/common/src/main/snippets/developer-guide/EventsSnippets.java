// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::events-java-001[]
Button b = new Button("Click Me");
b.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent ev) {
        // button was clicked, you can do anything you want here...
    }
});
// end::events-java-001[]

// tag::events-java-002[]
Button b = new Button("Click Me");
b.addActionListener((ev) -> {
    // button was clicked, you can do anything you want here.
});
// end::events-java-002[]

// tag::events-java-003[]
Display.getInstance().addEdtErrorHandler((e) -> {
    Exception err = (Exception)e.getSource();
    // ...
});
// end::events-java-003[]

// tag::events-java-004[]
Display.getInstance().addEdtErrorHandler((e) -> {
    e.consume();
    Exception err = (Exception)e.getSource();
    // ...
});
// end::events-java-004[]

// tag::events-java-005[]
NetworkManager.getInstance().addErrorListener(new ActionListener<NetworkEvent>() {
    public void actionPerformed(NetworkEvent ev) {
        // now we have access to the methods on NetworkEvent that provide more information about the network specific flags
    }
});
// end::events-java-005[]

// tag::events-java-006[]
NetworkManager.getInstance().addErrorListener((ev) -> {
    // now we have access to the methods on NetworkEvent that provide more information about the network specific flags
});
// end::events-java-006[]

// tag::events-java-007[]
ConnectionRequest r = new ConnectionRequest() {
    @Override
    protected void readResponse(InputStream input) throws IOException {
        // read the input stream
    }
};
// end::events-java-007[]

// tag::events-java-008[]
ConnectionRequest r = new ConnectionRequest();
r.addResponseListener((e) -> {
    byte[] data = (byte[])e.getMetaData();
    // work with the byte data
});
// end::events-java-008[]

// tag::events-java-009[]
public class CustomToolbar extends Toolbar implements ScrollListener {
    private int alpha;

    public CustomToolbar() {
    }

    public void paintComponentBackground(Graphics g) {
        int a = g.getAlpha();
        g.setAlpha(alpha);
        super.paintComponentBackground(g);
        g.setAlpha(a);
    }

    public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
        alpha = scrollY;
        alpha = Math.max(alpha, 0);
        alpha = Math.min(alpha, 255);
    }
}
// end::events-java-009[]

// tag::events-java-010[]
cmp.getUnselectedStyle().setFgColor(0xffffff);
// end::events-java-010[]

// tag::events-java-011[]
private final EventDispatcher listeners = new EventDispatcher();

public void addActionListener(ActionListener a) {
    listeners.addListener(a);
}
public void removeActionListener(ActionListener a) {
    listeners.removeListener(a);
}
// end::events-java-011[]

// tag::events-java-012[]
private void fireEvent(ActionEvent ev) {
    listeners.fireActionEvent(ev);
}
// end::events-java-012[]

// tag::events-java-013[]
public void pointerDragged(int[] x, int[] y)
public void pointerDragged(final int x, final int y)
public void pointerPressed(int[] x, int[] y)
public void pointerPressed(int x, int y)
public void pointerReleased(int[] x, int[] y)
public void pointerReleased(int x, int y)
public void pointerHover(int[] x, int[] y)
public void pointerHoverPressed(int[] x, int[] y)
public void pointerHoverReleased(int[] x, int[] y)
public void longPointerPress(int x, int y)
public void keyPressed(int keyCode)
public void keyReleased(int keyCode)
public void keyRepeated(int keyCode)
// end::events-java-013[]

// tag::events-java-014[]
public class MyComponent extends Component {
    protected int getDragRegionStatus(int x, int y) {
        return DRAG_REGION_LIKELY_DRAG_XY;
    }
}
// end::events-java-014[]

// tag::events-java-015[]
bc.setBrowserNavigationCallback((url) -> {
    if(url.startsWith("http://click")) {
        Display.getInstance().callSerially(() -> bc.execute("fnc('<p>You clicked!</p>')"));
        return false;
    }
    return true;
});
// end::events-java-015[]
