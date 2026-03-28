import com.codename1.impl.platform.js.VMHost;

public class JsHostEventQueueApp {
    public static void main(String[] args) {
        int first = VMHost.pollEventCode();
        int second = VMHost.pollEventCode();
        int none = VMHost.pollEventCode();
        System.exit((first * 1000) + (second * 10) + (none + 1));
    }
}
