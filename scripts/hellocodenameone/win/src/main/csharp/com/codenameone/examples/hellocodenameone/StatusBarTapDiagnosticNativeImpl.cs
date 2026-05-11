namespace com.codenameone.examples.hellocodenameone{


public class StatusBarTapDiagnosticNativeImpl : IStatusBarTapDiagnosticNativeImpl {
    public bool simulateStatusBarTap() {
        return false;
    }

    public int getTapCount() {
        return 0;
    }

    public bool isSupported() {
        return false;
    }

}
}
