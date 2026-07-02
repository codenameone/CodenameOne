// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::network-connectivity-java-001[]
import com.codename1.io.wifi.WiFi;

if (WiFi.isInfoSupported()) {
    String ssid = WiFi.getCurrentSSID();   // e.g. "Codename One"
    String bssid = WiFi.getBSSID();        // "aa:bb:cc:11:22:33"
    String gw = WiFi.getGateway();         // "192.168.1.1"
    String ip = WiFi.getIp();              // "192.168.1.42"
}
// end::network-connectivity-java-001[]

// tag::network-connectivity-java-002[]
WiFi.scan(new WiFiScanCallback() {
    @Override
    public void onScanComplete(WiFiNetwork[] networks, Throwable error) {
        if (error != null) {
            Log.e(error);
            return;
        }
        for (WiFiNetwork n : networks) {
            // n.getSSID(), n.getBSSID(), n.getRssi(), n.getSecurity()
        }
    }
});
// end::network-connectivity-java-002[]

// tag::network-connectivity-java-003[]
WiFi.connect("MyAccessPoint", "s3cret!", WiFiSecurity.WPA_PSK,
        new WiFiConnectCallback() {
    @Override
    public void onConnectResult(boolean connected, Throwable error) {
        if (!connected) {
            Log.e(error);
            return;
        }
        // ssid is now joined
    }
});
// end::network-connectivity-java-003[]

// tag::network-connectivity-java-004[]
import com.codename1.io.bonjour.BonjourBrowser;
import com.codename1.io.bonjour.BonjourService;
import com.codename1.io.bonjour.BonjourServiceListener;

BonjourBrowser browser = BonjourBrowser.browse("_http._tcp.",
        new BonjourServiceListener() {
    @Override
    public void onServiceResolved(BonjourService s) {
        Log.p("Found " + s.getName() + " at " + s.getHost() + ":" + s.getPort());
    }
    @Override
    public void onServiceLost(BonjourService s) { /* ... */ }
    @Override
    public void onBrowseError(Throwable t) { Log.e(t); }
});

// when done:
browser.stop();
// end::network-connectivity-java-004[]

// tag::network-connectivity-java-005[]
BonjourPublisher pub = BonjourPublisher.publish(
        "My Server", "_http._tcp.", 8080, null);

// later:
pub.unpublish();
// end::network-connectivity-java-005[]

// tag::network-connectivity-java-006[]
import com.codename1.io.NetworkManager;
import com.codename1.io.NetworkTypeListener;

NetworkManager.getInstance().addNetworkTypeListener(new NetworkTypeListener() {
    @Override
    public void onNetworkTypeChanged(int oldType, int newType, boolean vpnActive) {
        if (newType == NetworkManager.NETWORK_TYPE_NONE) {
            // offline -- queue uploads for later
        }
        if (vpnActive) {
            // refuse to roam to corporate-only resources
        }
    }
});
// end::network-connectivity-java-006[]

// tag::network-connectivity-java-007[]
int type = NetworkManager.getInstance().getCurrentNetworkType();
boolean vpn = NetworkManager.getInstance().isVPNActive();
// end::network-connectivity-java-007[]

// tag::network-connectivity-java-008[]
import com.codename1.io.wifi.WiFiDirect;
import com.codename1.io.wifi.WiFiDirectListener;
import com.codename1.io.wifi.WiFiDirectPeer;

if (!WiFiDirect.isSupported()) {
    return;
}

WiFiDirect.startDiscovery(new WiFiDirectListener() {
    @Override
    public void onPeersAvailable(WiFiDirectPeer[] peers) {
        for (WiFiDirectPeer p : peers) {
            // p.getDeviceName(), p.getDeviceAddress(), p.getState()
        }
    }
    @Override
    public void onDiscoveryError(Throwable error) { Log.e(error); }
});

// later, with the user's chosen peer:
WiFiDirect.connect(peer, new WiFiConnectCallback() {
    @Override
    public void onConnectResult(boolean connected, Throwable error) { /* ... */ }
});

// when finished:
WiFiDirect.disconnect();
WiFiDirect.stopDiscovery();
// end::network-connectivity-java-008[]

// tag::network-connectivity-java-009[]
import com.codename1.io.usb.Usb;
import com.codename1.io.usb.UsbDevice;
import com.codename1.io.usb.UsbDeviceListener;

if (!Usb.isSupported()) { return; }

Usb.addDeviceListener(new UsbDeviceListener() {
    @Override
    public void onDeviceAttached(UsbDevice d) {
        if (d.getVendorId() == 0x0403 && d.getProductId() == 0x6001) {
            Usb.requestPermission(d);
        }
    }
    @Override
    public void onDeviceDetached(UsbDevice d) { }
    @Override
    public void onPermissionResult(UsbDevice d, boolean granted) {
        if (granted) {
            try (InputStream in = Usb.openInputStream(d, 0x81);
                 OutputStream out = Usb.openOutputStream(d, 0x02)) {
                out.write("AT\r\n".getBytes());
                byte[] buf = new byte[64];
                int n = in.read(buf);
                // ...
            } catch (IOException e) { Log.e(e); }
        }
    }
});
// end::network-connectivity-java-009[]
