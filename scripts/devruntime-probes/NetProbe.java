import com.codename1.ui.*;
import com.codename1.io.*;

/**
 * A pushed program doing real networking, against a host-side endpoint reached
 * over `adb reverse` -- the emulator has no DNS, and a hermetic endpoint is a
 * better test anyway.
 *
 * The interesting part is not the request: it is that ConnectionRequest is a
 * framework class being subclassed by interpreted code, so readResponse is an
 * interpreted override the framework calls back into on its own network thread.
 */
public class NetProbe {
    public static void main(String[] a) {
        final StringBuilder r = new StringBuilder();
        ConnectionRequest cr = new ConnectionRequest() {
            protected void readResponse(java.io.InputStream in) throws java.io.IOException {
                r.append("body=").append(Util.readToString(in).trim());
            }
            protected void handleErrorResponseCode(int code, String message) {
                r.append("http ").append(code);
            }
        };
        cr.setUrl("http://127.0.0.1:18080/hello.txt");
        cr.setPost(false);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        System.out.println("PROBE NetProbe: " + (r.length() == 0 ? "no callback" : r.toString())
            + " status=" + cr.getResponseCode());
        new Form("Net").show();
    }
}
