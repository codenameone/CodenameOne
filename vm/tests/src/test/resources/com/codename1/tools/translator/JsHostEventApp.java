import com.codename1.impl.platform.js.VMHost;

public class JsHostEventApp {
    public static void main(String[] args) {
        int eventCode = VMHost.getLastEventCode();
        int echoed = VMHost.echoInt(eventCode);
        System.exit((eventCode * 100) + echoed);
    }
}
