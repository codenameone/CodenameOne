// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::performance-java-001[]
@Concrete(name = "com.codename1.impl.ios.IOSImplementation")
public abstract class CodenameOneImplementation {
    // ...
}
// end::performance-java-001[]

// tag::performance-java-002[]
Simd simd = Simd.get(); // equivalent to CN.getSimd()
if (simd.isSupported()) {
    // native SIMD is wired up on this platform
}
// end::performance-java-002[]

// tag::performance-java-003[]
byte[]  bytes  = simd.allocByte(64);   // heap, 16-byte aligned, registered
int[]   ints   = simd.allocInt(32);    // heap, 16-byte aligned, registered
float[] floats = simd.allocFloat(32);  // heap, 16-byte aligned, registered
// end::performance-java-003[]

// tag::performance-java-004[]
Simd simd = Simd.get();

byte[] a   = simd.allocByte(64);
byte[] b   = simd.allocByte(64);
byte[] out = simd.allocByte(64);

// fill a and b with your data (plain array writes are fine)
for (int i = 0; i < 64; i++) {
    a[i] = (byte) i;
    b[i] = (byte) (64 - i);
}

// saturating byte add, one vector instruction per 16 bytes on NEON
simd.add(a, b, out, 0, 64);
// end::performance-java-004[]

// tag::performance-java-005[]
byte[]  scratchB = simd.allocaByte(64);
int[]   scratchI = simd.allocaInt(32);
float[] scratchF = simd.allocaFloat(32);

// Deterministic initial contents:
byte[]  zeroed   = simd.allocaByteZeroed(64);
int[]   filled   = simd.allocaIntFilled(32, -1);
// end::performance-java-005[]

// tag::performance-java-006[]
Log.bindCrashProtection(true);
// end::performance-java-006[]

// tag::performance-java-007[]
public ShareButton() {
    setUIID("ShareButton");
    FontImage.setMaterialIcon(this, FontImage.MATERIAL_SHARE);
    addActionListener(this);
    shareServices.addElement(new SMSShare());
    shareServices.addElement(new EmailShare());
    shareServices.addElement(new FacebookShare());
}
// end::performance-java-007[]

// tag::performance-java-008[]
public SMSShare() {
    super("SMS", Resources.getSystemResource().getImage("sms.png"));
}
// end::performance-java-008[]

// tag::performance-java-009[]
public SMSShare() {
    super("SMS", null);
}

@Override
public Image getIcon() {
    Image i = super.getIcon();
    if(i == null) {
        i = Resources.getSystemResource().getImage("sms.png");
        setIcon(i);
    }
    return i;
}
// end::performance-java-009[]

// tag::performance-java-010[]
private long lastScroll;
// end::performance-java-010[]

// tag::performance-java-011[]
// don't do anything while we are scrolling or animating
long idle = System.currentTimeMillis() - lastScroll;
while(idle < 1500 || contactsDemo.getAnimationManager().isAnimating() || scrollY != contactsDemo.getScrollY()) {
    scrollY = contactsDemo.getScrollY();
    Util.sleep(Math.min(1500, Math.max(100, 2000 - ((int)idle))));
    idle = System.currentTimeMillis() - lastScroll;
}
// end::performance-java-011[]

// tag::performance-java-012[]
parentForm.addPointerDraggedListener(e -> lastScroll = System.currentTimeMillis());
// end::performance-java-012[]

// tag::performance-java-013[]
contactsDemo.addScrollListener(new ScrollListener() {
    int initial = -1;
    @Override
    public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
        // scrolling is sensitive on devices...
        if(initial < 0) {
            initial = scrollY;
        }
        lastScroll = System.currentTimeMillis();
        ...
    }
});
// end::performance-java-013[]
