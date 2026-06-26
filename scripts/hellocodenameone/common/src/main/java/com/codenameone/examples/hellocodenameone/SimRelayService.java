package com.codenameone.examples.hellocodenameone;

import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The simulator relay: this Codename One app runs as a REAL native build
 * (Mac Catalyst with full UIKit) and serves as the JVM simulator's
 * out-of-process native layer. It connects back to the simulator JVM over a
 * localhost socket, executes incoming RenderBridge-level drawing batches as
 * ordinary Codename One Graphics calls (which run through this build's real
 * native pipeline), forwards its input events upstream, and hosts real native
 * text editing by placing an actual TextField where the JVM asks.
 *
 * <p>No native interfaces, no JNI: everything platform-specific is what this
 * app's own port already does.</p>
 */
public class SimRelayService {
    /** the simulator JVM listens here; this app connects out */
    private static final int PORT = 17995;

    /* frame opcodes JVM -> relay */
    private static final int OP_BATCH = 1;
    private static final int OP_CREATE_SYSTEM_FONT = 2;
    private static final int OP_DERIVE_FONT = 3;
    private static final int OP_CREATE_TTF_FONT = 4;
    private static final int OP_STRING_WIDTH = 5;
    private static final int OP_CHAR_WIDTH = 6;
    private static final int OP_CREATE_IMAGE = 8;
    private static final int OP_CREATE_IMAGE_ARGB = 9;
    private static final int OP_SCALE_IMAGE = 10;
    private static final int OP_GET_RGB = 11;
    private static final int OP_RELEASE = 12;
    private static final int OP_EDIT_STRING = 13;
    private static final int OP_DISPLAY_INFO = 14;
    private static final int OP_PEER_CREATE = 16;
    private static final int OP_PEER_SET_FRAME = 17;
    private static final int OP_PEER_CMD = 18;
    private static final int OP_PEER_QUERY = 19;
    private static final int OP_MUTABLE_CREATE = 20;
    private static final int OP_MUTABLE_OPS = 21;
    private static final int OP_SET_TOUCH_CORR = 22;
    private static final int OP_CAPTURE_PHOTO = 23;
    private static final int OP_SET_NATIVE_MENU = 24;
    private static final int OP_SET_ALWAYS_ON_TOP = 25;
    private static final int OP_SET_WINDOW_SIZE = 26;

    /* peer kinds */
    private static final int PEER_WEBVIEW = 1;
    private static final int PEER_MEDIA = 2;
    private static final int PEER_CAMERA = 4;

    /* batch ops */
    private static final int B_CLIP = 1;
    private static final int B_FILL_RECT = 2;
    private static final int B_DRAW_LINE = 3;
    private static final int B_DRAW_STRING = 4;
    private static final int B_DRAW_IMAGE = 5;
    private static final int B_TILE_IMAGE = 6;
    private static final int B_SHAPE = 7;

    /* relay -> JVM frame kinds */
    private static final int K_REPLY = 0;
    private static final int K_EVENT = 1;

    /* event codes */
    private static final int E_POINTER_PRESSED = 1;
    private static final int E_POINTER_RELEASED = 2;
    private static final int E_POINTER_DRAGGED = 3;
    private static final int E_KEY_PRESSED = 4;
    private static final int E_KEY_RELEASED = 5;
    private static final int E_SIZE_CHANGED = 6;
    private static final int E_EDIT_UPDATE = 7;
    private static final int E_EDIT_DONE = 8;
    private static final int E_LOG = 9;
    private static final int E_CAPTURE_DONE = 10;
    private static final int E_MENU_SELECTED = 11;

    private final Map<Integer, Font> fonts = new HashMap<Integer, Font>();
    private final Map<Integer, Image> images = new HashMap<Integer, Image>();
    private int nextHandle = 1;

    /* native peers: real CN1 components of THIS port floated on the layered pane */
    private final Map<Integer, Component> peerViews = new HashMap<Integer, Component>();
    private final Map<Integer, com.codename1.media.Media> peerMedia =
            new HashMap<Integer, com.codename1.media.Media>();
    private final Map<Integer, com.codename1.camera.CameraSession> peerCameras =
            new HashMap<Integer, com.codename1.camera.CameraSession>();

    private Canvas canvas;
    private Form form;
    private DataOutputStream out;
    private final Object outLock = new Object();

    /** one decoded drawing op, replayed inside paint() */
    private static class DrawOp {
        int op;
        int[] i = new int[8];
        String s;
        Object ref;
        byte[] cmds;
        float[] pts;
        float[] f = new float[4];
    }

    /** live touch Y correction (relay-px), set by the JVM via OP_SET_TOUCH_CORR */
    private int touchCorr = 0;

    class Canvas extends Component {
        protected Dimension calcPreferredSize() {
            return new Dimension(Display.getInstance().getDisplayWidth(),
                    Display.getInstance().getDisplayHeight());
        }

        public void paint(Graphics g) {
            // The screen is driven directly by the RPC batch path onto the
            // impl's native screen graphics (SimRelayScreen) + flushGraphics,
            // NOT by this component. Painting nothing here (and keeping the
            // form transparent) avoids clearing the RPC-drawn frame. This
            // component exists only to own the screen region + capture input.
        }

        public void pointerPressed(int x, int y) {
            sendPointer(E_POINTER_PRESSED, x - getAbsoluteX(), y - getAbsoluteY() - touchCorr);
        }

        public void pointerReleased(int x, int y) {
            sendPointer(E_POINTER_RELEASED, x - getAbsoluteX(), y - getAbsoluteY() - touchCorr);
        }

        public void pointerDragged(int x, int y) {
            sendPointer(E_POINTER_DRAGGED, x - getAbsoluteX(), y - getAbsoluteY() - touchCorr);
        }

        public void keyPressed(int code) {
            sendEvent2(E_KEY_PRESSED, code, 0);
        }

        public void keyReleased(int code) {
            sendEvent2(E_KEY_RELEASED, code, 0);
        }
    }

    // Replay a draw op by invoking the impl's native-graphics API directly,
    // with `ng` = the opaque native-graphics surface pointer (the display or a
    // mutable image). No CN1 Graphics object is ever allocated. Images/fonts
    // hand their native peer to the impl (getImage()/getNativeFont()).
    // dx,dy = draw origin offset into the relay display. The screen batch is
    // shifted by the canvas absolute position so the UI lands where the touch
    // side expects it (pointer events subtract getAbsoluteX/Y); mutable-image
    // ops pass (0,0) since the image is its own coordinate space.
    private void replay(com.codename1.impl.CodenameOneImplementation impl, Object ng, DrawOp o,
                        int dx, int dy) {
        switch (o.op) {
            case B_CLIP:
                impl.setClip(ng, o.i[0] + dx, o.i[1] + dy, o.i[2], o.i[3]);
                break;
            case B_FILL_RECT:
                impl.setColor(ng, o.i[0]);
                impl.setAlpha(ng, o.i[1]);
                impl.fillRect(ng, o.i[2] + dx, o.i[3] + dy, o.i[4], o.i[5]);
                break;
            case B_DRAW_LINE:
                impl.setColor(ng, o.i[0]);
                impl.setAlpha(ng, o.i[1]);
                impl.drawLine(ng, o.i[2] + dx, o.i[3] + dy, o.i[4] + dx, o.i[5] + dy);
                break;
            case B_DRAW_STRING: {
                impl.setColor(ng, o.i[0]);
                impl.setAlpha(ng, o.i[1]);
                Font f = (Font) o.ref;
                if (f != null) {
                    impl.setNativeFont(ng, f.getNativeFont());
                }
                impl.drawString(ng, o.s, o.i[3] + dx, o.i[4] + dy);
                break;
            }
            case B_DRAW_IMAGE: {
                impl.setAlpha(ng, o.i[1]);
                Image img = (Image) o.ref;
                if (img != null) {
                    impl.drawImage(ng, img.getImage(), o.i[2] + dx, o.i[3] + dy, o.i[4], o.i[5]);
                }
                break;
            }
            case B_TILE_IMAGE: {
                impl.setAlpha(ng, o.i[1]);
                Image img = (Image) o.ref;
                if (img != null) {
                    impl.tileImage(ng, img.getImage(), o.i[2] + dx, o.i[3] + dy, o.i[4], o.i[5]);
                }
                break;
            }
            case B_SHAPE: {
                impl.setColor(ng, o.i[0]);
                impl.setAlpha(ng, o.i[1]);
                GeneralPath p = new GeneralPath();
                int pi = 0;
                for (int c = 0; c < o.cmds.length; c++) {
                    switch (o.cmds[c]) {
                        case 0:
                            p.moveTo(o.pts[pi] + dx, o.pts[pi + 1] + dy);
                            pi += 2;
                            break;
                        case 1:
                            p.lineTo(o.pts[pi] + dx, o.pts[pi + 1] + dy);
                            pi += 2;
                            break;
                        case 2:
                            p.quadTo(o.pts[pi] + dx, o.pts[pi + 1] + dy,
                                    o.pts[pi + 2] + dx, o.pts[pi + 3] + dy);
                            pi += 4;
                            break;
                        case 3:
                            p.curveTo(o.pts[pi] + dx, o.pts[pi + 1] + dy,
                                    o.pts[pi + 2] + dx, o.pts[pi + 3] + dy,
                                    o.pts[pi + 4] + dx, o.pts[pi + 5] + dy);
                            pi += 6;
                            break;
                        case 4:
                            p.closePath();
                            break;
                        default:
                            break;
                    }
                }
                if (o.i[2] != 0) {
                    impl.drawShape(ng, p, new com.codename1.ui.Stroke(o.f[0], o.i[3], o.i[4], o.f[1]));
                } else {
                    impl.fillShape(ng, p);
                }
                break;
            }
            default:
                break;
        }
    }

    public void start() {
        // Keep the title STRING (the iOS port pushes it to the native NSWindow
        // title bar) but COLLAPSE the CN1 title area so it renders nothing -
        // otherwise the CN1 toolbar title smears a second, duplicate title into
        // the surface on top of the native one. Collapse via UIID + zero
        // preferred size (the shell does the same); do NOT use setHidden(true),
        // which silently kills ALL pointer dispatch on this port (A/B verified).
        form = new Form("Codename One Simulator", new BorderLayout());
        form.getTitleArea().setUIID("Container");
        form.getTitleArea().setPreferredSize(new Dimension(0, 0));
        form.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
        form.getContentPane().getAllStyles().setMargin(0, 0, 0, 0);
        // Dark content-pane bg COLOR (still fully transparent, so it never
        // paints over the RPC surface). The iOS port's syncMacWindowAppearance()
        // derives the Mac window's dark/light appearance from this form's bg
        // luma on every form change (resize/rotate); a dark color keeps the
        // title bar in DarkAqua so the title text stays light/readable instead
        // of reverting to black on a re-layout.
        form.getContentPane().getAllStyles().setBgColor(0x2b2b2b);
        // Transparent chrome so the relay's own EDT paint never clears the
        // RPC-drawn native screen (which retains pixels frame-to-frame).
        form.getAllStyles().setBgTransparency(0);
        form.getContentPane().getAllStyles().setBgTransparency(0);
        canvas = new Canvas();
        canvas.setFocusable(true);
        canvas.getAllStyles().setBgTransparency(0);
        form.add(BorderLayout.CENTER, canvas);
        if ("true".equals(Display.getInstance().getProperty("cn1.relay.canary", "false"))) {
            // peer canary: proves native views render in this app at all,
            // independent of the RPC overlay path
            com.codename1.ui.BrowserComponent canary = new com.codename1.ui.BrowserComponent();
            canary.setURL("https://www.codenameone.com");
            canary.setPreferredH(300);
            form.add(BorderLayout.SOUTH, canary);
        }
        form.addSizeChangedListener(e ->
                sendEvent2(E_SIZE_CHANGED, canvas.getWidth(), canvas.getHeight()));
        form.show();
        connect();
    }

    private void connect() {
        Socket.connect("127.0.0.1", PORT, new SocketConnection() {
            public void connectionError(int errorCode, String message) {
                // the JVM is not up yet - retry until it is
                new com.codename1.ui.util.UITimer(new Runnable() {
                    public void run() {
                        connect();
                    }
                }).schedule(1500, false, form);
            }

            public void connectionEstablished(InputStream is, OutputStream os) {
                try {
                    out = new DataOutputStream(os);
                    serve(new DataInputStream(is));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Display.getInstance().exitApplication();
                }
            }
        });
    }

    /** runs on the socket thread for the lifetime of the connection */
    private void serve(DataInputStream in) throws IOException {
        while (true) {
            int opcode = in.readInt();
            if (opcode != OP_BATCH && System.getProperty("cn1.relay.trace") != null) {
                sendLog("recv op=" + opcode);
            }
            switch (opcode) {
                case OP_BATCH:
                    readBatch(in);
                    break;
                case OP_CREATE_SYSTEM_FONT: {
                    int reqId = in.readInt();
                    int face = in.readInt();
                    int style = in.readInt();
                    int size = in.readInt();
                    Font f = Font.createSystemFont(face, style, size);
                    replyFont(reqId, f);
                    break;
                }
                case OP_DERIVE_FONT: {
                    int reqId = in.readInt();
                    int fontId = in.readInt();
                    boolean bold = in.readBoolean();
                    boolean italic = in.readBoolean();
                    float size = in.readFloat();
                    Font base = fonts.get(fontId);
                    Font f = base != null
                            ? base.derive(size, (bold ? Font.STYLE_BOLD : 0)
                                    | (italic ? Font.STYLE_ITALIC : 0))
                            : Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
                    replyFont(reqId, f);
                    break;
                }
                case OP_CREATE_TTF_FONT: {
                    int reqId = in.readInt();
                    String name = in.readUTF();
                    Font f;
                    try {
                        f = Font.createTrueTypeFont(name, name);
                    } catch (Exception ex) {
                        f = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
                    }
                    if (f == null) {
                        f = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
                    }
                    replyFont(reqId, f);
                    break;
                }
                case OP_STRING_WIDTH: {
                    int reqId = in.readInt();
                    int fontId = in.readInt();
                    String str = in.readUTF();
                    Font f = fonts.get(fontId);
                    reply(reqId, f != null ? f.stringWidth(str) : 0);
                    break;
                }
                case OP_CHAR_WIDTH: {
                    int reqId = in.readInt();
                    int fontId = in.readInt();
                    char c = in.readChar();
                    Font f = fonts.get(fontId);
                    reply(reqId, f != null ? f.charWidth(c) : 0);
                    break;
                }
                case OP_CREATE_IMAGE: {
                    int reqId = in.readInt();
                    byte[] data = new byte[in.readInt()];
                    in.readFully(data);
                    Image img = null;
                    try {
                        img = Image.createImage(data, 0, data.length);
                    } catch (Exception ignored) {
                    }
                    if (img == null) {
                        reply3(reqId, 0, 0, 0);
                    } else {
                        int id = nextHandle++;
                        images.put(id, img);
                        reply3(reqId, id, img.getWidth(), img.getHeight());
                    }
                    break;
                }
                case OP_CREATE_IMAGE_ARGB: {
                    int reqId = in.readInt();
                    int w = in.readInt();
                    int h = in.readInt();
                    int[] argb = new int[w * h];
                    for (int i = 0; i < argb.length; i++) {
                        argb[i] = in.readInt();
                    }
                    Image img = Image.createImage(argb, w, h);
                    int id = nextHandle++;
                    images.put(id, img);
                    reply(reqId, id);
                    break;
                }
                case OP_SCALE_IMAGE: {
                    int reqId = in.readInt();
                    int imgId = in.readInt();
                    int w = in.readInt();
                    int h = in.readInt();
                    Image img = images.get(imgId);
                    int id = 0;
                    if (img != null) {
                        Image scaled = img.scaled(w, h);
                        id = nextHandle++;
                        images.put(id, scaled);
                    }
                    reply(reqId, id);
                    break;
                }
                case OP_GET_RGB: {
                    int reqId = in.readInt();
                    int imgId = in.readInt();
                    Image img = images.get(imgId);
                    int[] rgb = img != null ? img.getRGB() : new int[0];
                    replyInts(reqId, rgb);
                    break;
                }
                case OP_RELEASE: {
                    int id = in.readInt();
                    fonts.remove(id);
                    images.remove(id);
                    break;
                }
                case OP_EDIT_STRING:
                    readEditString(in);
                    break;
                case OP_PEER_CREATE: {
                    int reqId = in.readInt();
                    int kind = in.readInt();
                    int flag = in.readInt();
                    String arg = in.readUTF();
                    reply(reqId, peerCreate(kind, flag, arg));
                    break;
                }
                case OP_PEER_SET_FRAME: {
                    final int peerId = in.readInt();
                    final int x = in.readInt();
                    final int y = in.readInt();
                    final int w = in.readInt();
                    final int h = in.readInt();
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            peerSetFrame(peerId, x, y, w, h);
                        }
                    });
                    break;
                }
                case OP_PEER_CMD: {
                    final int peerId = in.readInt();
                    final int cmd = in.readInt();
                    final int arg = in.readInt();
                    final String sArg = in.readUTF();
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            peerCommand(peerId, cmd, arg, sArg);
                        }
                    });
                    break;
                }
                case OP_SET_TOUCH_CORR: {
                    touchCorr = in.readInt();
                    sendLog("touchCorr set to " + touchCorr);
                    break;
                }
                case OP_CAPTURE_PHOTO:
                    readCapturePhoto();
                    break;
                case OP_SET_NATIVE_MENU: {
                    final String enc = in.readUTF();
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            installNativeMenu(enc);
                        }
                    });
                    break;
                }
                case OP_SET_ALWAYS_ON_TOP: {
                    final boolean onTop = in.readInt() != 0;
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            com.codename1.ui.SimRelayGfx.impl().setMacWindowAlwaysOnTop(onTop);
                        }
                    });
                    break;
                }
                case OP_SET_WINDOW_SIZE: {
                    final int winW = in.readInt();
                    final int winH = in.readInt();
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            com.codename1.impl.CodenameOneImplementation impl =
                                    com.codename1.ui.SimRelayGfx.impl();
                            sendLog("OP_SET_WINDOW_SIZE " + winW + "x" + winH
                                    + " desktop=" + impl.isDesktop());
                            impl.setMacWindowContentSize(winW, winH);
                        }
                    });
                    break;
                }
                case OP_MUTABLE_CREATE: {
                    int reqId = in.readInt();
                    int w = in.readInt();
                    int h = in.readInt();
                    int fill = in.readInt();
                    int id = nextHandle++;
                    images.put(id, Image.createImage(w, h, fill));
                    reply(reqId, id);
                    break;
                }
                case OP_MUTABLE_OPS: {
                    final int imgId = in.readInt();
                    int count = in.readInt();
                    final List<DrawOp> ops = readOps(in, count);
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            Image img = images.get(imgId);
                            if (img == null) {
                                return;
                            }
                            // A mutable image is just another native-graphics
                            // surface pointer - no Graphics allocation.
                            com.codename1.impl.CodenameOneImplementation impl =
                                    com.codename1.ui.SimRelayGfx.impl();
                            Object ng = impl.getNativeGraphics(img.getImage());
                            impl.setClip(ng, 0, 0, img.getWidth(), img.getHeight());
                            for (DrawOp o : ops) {
                                replay(impl, ng, o, 0, 0);
                            }
                        }
                    });
                    break;
                }
                case OP_PEER_QUERY: {
                    int reqId = in.readInt();
                    int peerId = in.readInt();
                    int what = in.readInt();
                    com.codename1.media.Media m = peerMedia.get(peerId);
                    int val = 0;
                    if (m != null) {
                        switch (what) {
                            case 0:
                                val = m.getTime();
                                break;
                            case 1:
                                val = m.getDuration();
                                break;
                            case 2:
                                val = m.isPlaying() ? 1 : 0;
                                break;
                            default:
                                break;
                        }
                    }
                    reply(reqId, val);
                    break;
                }
                case OP_DISPLAY_INFO: {
                    int reqId = in.readInt();
                    reply3(reqId, canvas.getWidth() > 0 ? canvas.getWidth()
                                    : Display.getInstance().getDisplayWidth(),
                            canvas.getHeight() > 0 ? canvas.getHeight()
                                    : Display.getInstance().getDisplayHeight(), 0);
                    break;
                }
                default:
                    throw new IOException("Unknown opcode " + opcode);
            }
        }
    }

    private void readBatch(DataInputStream in) throws IOException {
        int count = in.readInt();
        final List<DrawOp> batch = readOps(in, count);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                // Ops are already in DISPLAY coordinates (RpcRegionBridge added
                // the universe offset before sending), so draw them straight
                // onto the impl's native display graphics and present - no
                // Graphics, no buffer, no extra translate (the old canvas-offset
                // translate double-offset the content off-screen on some
                // layouts = the intermittent black). Native screen retention
                // handles partial (dirty-region) repaints like a device.
                com.codename1.impl.CodenameOneImplementation impl =
                        com.codename1.ui.SimRelayGfx.impl();
                Object ng = impl.getNativeGraphics();
                // Shift the draw to the canvas origin so it matches the touch
                // side (pointer events subtract getAbsoluteX/Y). Without this
                // the UI drew at display (0,0) and clicks landed ~titlebar-high.
                int dx = canvas != null ? canvas.getAbsoluteX() : 0;
                int dy = canvas != null ? canvas.getAbsoluteY() : 0;
                for (DrawOp o : batch) {
                    replay(impl, ng, o, dx, dy);
                }
                impl.flushGraphics();
            }
        });
    }

    private List<DrawOp> readOps(DataInputStream in, int count) throws IOException {
        final List<DrawOp> batch = new ArrayList<DrawOp>(count);
        for (int n = 0; n < count; n++) {
            DrawOp o = new DrawOp();
            o.op = in.readInt();
            switch (o.op) {
                case B_CLIP:
                    for (int k = 0; k < 4; k++) {
                        o.i[k] = in.readInt();
                    }
                    break;
                case B_FILL_RECT:
                case B_DRAW_LINE:
                    for (int k = 0; k < 6; k++) {
                        o.i[k] = in.readInt();
                    }
                    break;
                case B_DRAW_STRING:
                    o.i[0] = in.readInt();
                    o.i[1] = in.readInt();
                    o.i[2] = in.readInt();
                    o.i[3] = in.readInt();
                    o.i[4] = in.readInt();
                    o.s = in.readUTF();
                    o.ref = fonts.get(o.i[2]);
                    break;
                case B_DRAW_IMAGE:
                case B_TILE_IMAGE:
                    for (int k = 0; k < 6; k++) {
                        o.i[k] = in.readInt();
                    }
                    o.ref = images.get(o.i[0]);
                    break;
                case B_SHAPE: {
                    o.i[0] = in.readInt();
                    o.i[1] = in.readInt();
                    o.i[2] = in.readInt();
                    o.i[3] = in.readInt();
                    o.i[4] = in.readInt();
                    o.f[0] = in.readFloat();
                    o.f[1] = in.readFloat();
                    o.cmds = new byte[in.readInt()];
                    in.readFully(o.cmds);
                    o.pts = new float[in.readInt()];
                    for (int k = 0; k < o.pts.length; k++) {
                        o.pts[k] = in.readFloat();
                    }
                    break;
                }
                default:
                    throw new IOException("Unknown batch op " + o.op);
            }
            batch.add(o);
        }
        return batch;
    }

    private TextArea editor;

    private void readEditString(DataInputStream in) throws IOException {
        final String text = in.readUTF();
        final int x = in.readInt();
        final int y = in.readInt();
        final int w = in.readInt();
        final int h = in.readInt();
        final int fontSizePx = in.readInt();
        final int fontStyle = in.readInt();
        final int fgColor = in.readInt();
        final int bgColor = in.readInt();
        final int bgTransparency = in.readInt();
        final boolean multiline = in.readBoolean();
        final int constraint = in.readInt();
        final int align = in.readInt();
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                // A SYNTHETIC field carrying the remote component's resolved
                // style. The relay has no copy of the app's theme, so we set
                // every value the native UIKit editor queries EXPLICITLY (font
                // size, colors, alignment) rather than inheriting this build's
                // default TextField theme (which is what produced the tiny font
                // + stray 1px border). The visible chrome is the app's own
                // component rendered underneath; this field is a transparent,
                // borderless host for the native text + caret.
                final TextArea t = multiline ? new TextArea(text) : new TextField(text);
                // growByContent collapses the field to one default-metrics line,
                // overriding our forced height - the native field then reads a
                // ~19px frame and clips the real font. Pin the frame instead.
                t.setGrowByContent(false);
                Font ef = editFont(fontSizePx, fontStyle);
                // set the font on EVERY style mode the component/native editor
                // may query (unselected drives measurement, selected drives the
                // editing field) - getAllStyles alone did not reach the style the
                // native editor reads
                applyEditStyle(t.getUnselectedStyle(), ef, fgColor, bgColor, bgTransparency, align);
                applyEditStyle(t.getSelectedStyle(), ef, fgColor, bgColor, bgTransparency, align);
                applyEditStyle(t.getPressedStyle(), ef, fgColor, bgColor, bgTransparency, align);
                applyEditStyle(t.getDisabledStyle(), ef, fgColor, bgColor, bgTransparency, align);
                // mirror the remote component's input type (numeric/password/
                // email/...) so the native field behaves right
                t.setConstraint(constraint);
                // force the field to the requested pixel rectangle: the native
                // field is sized to the component frame, so a too-short frame
                // clips a large font and the text reads tiny
                t.setPreferredW(w);
                t.setPreferredH(h);
                placeFloating(t, x, y, w, h);
                // the native editor reads cmp.getWidth()/getHeight() at
                // startEditingAsync; pin them to the requested rectangle so a
                // layout pass can't collapse the native frame
                t.setWidth(w);
                t.setHeight(h);
                editor = t;
                t.addDataChangedListener((type, index) -> sendEditEvent(E_EDIT_UPDATE, t.getText()));
                t.addActionListener(e -> finishEdit(t));
                t.startEditingAsync();
            }
        });
    }

    private void finishEdit(TextArea t) {
        if (editor != t) {
            return;
        }
        editor = null;
        sendEditEvent(E_EDIT_DONE, t.getText());
        t.remove();
        form.revalidate();
    }

    private final Map<Integer, Font> editFonts = new HashMap<Integer, Font>();

    /**
     * Builds a native font at the exact pixel size the app reported for the
     * editing component, so the native field's text matches the rendered text.
     * native: fonts only; the CN1 style flag picks the matching face.
     */
    private Font editFont(int sizePx, int style) {
        if (sizePx <= 0) {
            sizePx = 18;
        }
        int key = (sizePx << 2) | (style & 3);
        Font f = editFonts.get(key);
        if (f != null) {
            return f;
        }
        boolean bold = (style & Font.STYLE_BOLD) != 0;
        boolean italic = (style & Font.STYLE_ITALIC) != 0;
        String face;
        if (italic) {
            face = bold ? "native:ItalicBold" : "native:ItalicRegular";
        } else {
            face = bold ? "native:MainBold" : "native:MainRegular";
        }
        Font base;
        try {
            base = Font.createTrueTypeFont(face, face);
        } catch (Throwable t) {
            base = Font.createTrueTypeFont("native:MainRegular", "native:MainRegular");
        }
        f = base.derive(sizePx, style);
        editFonts.put(key, f);
        return f;
    }

    /**
     * Applies the remote component's resolved style to one style mode. The
     * editing field is a TRANSPARENT, borderless host: the app's own component
     * (bg + border) is already rendered underneath in its universe, and an
     * opaque bg here would paint over the native text overlay. Only the font and
     * foreground color drive the native field's visible text.
     */
    private void applyEditStyle(com.codename1.ui.plaf.Style s, Font ef, int fgColor,
            int bgColor, int bgTransparency, int align) {
        s.setFont(ef);
        s.setFgColor(fgColor);
        s.setBgTransparency(0);
        s.setBorder(com.codename1.ui.plaf.Border.createEmpty());
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        if (align == Component.LEFT || align == Component.CENTER
                || align == Component.RIGHT) {
            s.setAlignment(align);
        }
    }

    /* ---- camera -------------------------------------------------------------- */

    private int captureSeq = 0;

    /**
     * Opens this build's REAL native camera (AVCaptureSession via the iOS
     * port's Capture API) and, on commit, copies the captured still to a
     * world-shared /tmp path so the host JVM process can read it. The path
     * travels back over E_CAPTURE_DONE; an empty string means the user
     * cancelled or denied permission - the host turns that into a null pick,
     * which is the graceful deny fallback.
     */
    private void readCapturePhoto() {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                try {
                    com.codename1.capture.Capture.capturePhoto(
                            new com.codename1.ui.events.ActionListener() {
                        public void actionPerformed(
                                com.codename1.ui.events.ActionEvent ev) {
                            Object src = ev != null ? ev.getSource() : null;
                            sendEditEvent(E_CAPTURE_DONE,
                                    shareCapture(src == null ? null : src.toString()));
                        }
                    });
                } catch (Throwable t) {
                    // camera unavailable / permission denied
                    sendEditEvent(E_CAPTURE_DONE, "");
                }
            }
        });
    }

    /**
     * Copies a captured image out of this app's private storage into a shared
     * /tmp file the host JVM can open, returning the plain OS path (no scheme;
     * the host prepends file://). Returns "" when there was no capture.
     */
    private String shareCapture(String capturedPath) {
        if (capturedPath == null || capturedPath.length() == 0) {
            return "";
        }
        try {
            com.codename1.io.FileSystemStorage fs =
                    com.codename1.io.FileSystemStorage.getInstance();
            String ext = capturedPath.toLowerCase().endsWith(".png") ? ".png" : ".jpg";
            String name = "cn1sim-capture-" + (captureSeq++) + ext;
            String dest = "file:///tmp/" + name;
            java.io.InputStream is = fs.openInputStream(capturedPath);
            java.io.OutputStream os = fs.openOutputStream(dest);
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) > 0) {
                os.write(buf, 0, r);
            }
            is.close();
            os.close();
            return "/tmp/" + name;
        } catch (Throwable t) {
            // fall back to the raw path; the host may still reach it on a
            // non-sandboxed dev build
            return capturedPath.startsWith("file://")
                    ? capturedPath.substring("file://".length()) : capturedPath;
        }
    }

    /* ---- native menu bar ----------------------------------------------------- */

    /**
     * Installs the simulator's macOS menu bar from the shell's encoded rows
     * ("hint\tlabel\tshortcutKeyChar\tshortcutModifiers\t[checked]", newline
     * joined). Each row becomes a real Codename One Command on THIS build's
     * iOS port, so the port's own UIMenuBuilder bridge renders the native Mac
     * menu (submenus via ">" in the hint, "-" separators, checkmarks). The row
     * index is preserved 1:1, so a selection fires back as E_MENU_SELECTED with
     * the same index the shell's MenuDispatcher expects.
     */
    private void installNativeMenu(String enc) {
        java.util.Vector cmds = new java.util.Vector();
        if (enc != null && enc.length() > 0) {
            int len = enc.length();
            int rowStart = 0;
            int idx = 0;
            while (rowStart <= len) {
                int nl = enc.indexOf('\n', rowStart);
                String row = (nl < 0) ? enc.substring(rowStart)
                        : enc.substring(rowStart, nl);
                String[] cols = splitTabs(row);
                String hint = cols.length > 0 ? cols[0] : "";
                String label = cols.length > 1 ? cols[1] : row;
                int keyChar = (cols.length > 2) ? parseIntSafe(cols[2]) : 0;
                int mods = (cols.length > 3) ? parseIntSafe(cols[3]) : 0;
                boolean checked = cols.length > 4 && cols[4].indexOf('c') >= 0;
                final int fidx = idx;
                com.codename1.ui.Command c = new com.codename1.ui.Command(label) {
                    public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                        sendEvent2(E_MENU_SELECTED, fidx, 0);
                    }
                };
                c.setDesktopMenu(hint);
                if (keyChar != 0) {
                    c.setDesktopShortcut((char) keyChar, mods);
                }
                if (checked) {
                    c.putClientProperty("cn1.sim.checked", Boolean.TRUE);
                }
                cmds.addElement(c);
                idx++;
                if (nl < 0) {
                    break;
                }
                rowStart = nl + 1;
            }
        }
        com.codename1.ui.SimRelayGfx.impl().setNativeCommands(cmds);
    }

    private static String[] splitTabs(String row) {
        java.util.Vector parts = new java.util.Vector();
        int start = 0;
        int len = row.length();
        for (int i = 0; i <= len; i++) {
            if (i == len || row.charAt(i) == '\t') {
                parts.addElement(row.substring(start, i));
                start = i + 1;
            }
        }
        String[] out = new String[parts.size()];
        parts.copyInto(out);
        return out;
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /* ---- native peers -------------------------------------------------------- */

    /**
     * Creates a real native-backed CN1 component of this port: WKWebView via
     * BrowserComponent, AVPlayer via MediaManager, AVCaptureSession preview
     * via the camera API. Runs the UI work on the EDT and blocks for the id.
     */
    private int peerCreate(final int kind, final int flag, final String arg) {
        final int id = nextHandle++;
        final boolean[] ok = new boolean[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                try {
                    switch (kind) {
                        case PEER_WEBVIEW: {
                            com.codename1.ui.BrowserComponent b =
                                    new com.codename1.ui.BrowserComponent();
                            peerViews.put(id, b);
                            ok[0] = true;
                            break;
                        }
                        case PEER_MEDIA: {
                            com.codename1.media.Media m =
                                    com.codename1.media.MediaManager.createMedia(arg, flag == 1);
                            peerMedia.put(id, m);
                            if (flag == 1) {
                                Component vc = m.getVideoComponent();
                                if (vc != null) {
                                    peerViews.put(id, vc);
                                }
                            }
                            ok[0] = m != null;
                            break;
                        }
                        case PEER_CAMERA: {
                            com.codename1.camera.CameraInfo[] cams =
                                    com.codename1.camera.Camera.getCameras();
                            sendLog("peerCreate camera: " + cams.length + " cameras");
                            if (cams.length == 0) {
                                break;
                            }
                            com.codename1.camera.CameraSession sess =
                                    com.codename1.camera.Camera.open(cams[0],
                                            new com.codename1.camera.CameraSessionOptions()
                                                    .captureAudio(false));
                            peerCameras.put(id, sess);
                            peerViews.put(id, sess.createView());
                            sendLog("peerCreate camera: session open, view created");
                            ok[0] = true;
                            break;
                        }
                        default:
                            break;
                    }
                } catch (Exception ex) {
                    sendLog("peerCreate kind " + kind + " failed: " + ex);
                }
            }
        });
        return ok[0] ? id : 0;
    }

    /**
     * Positions a floated view at canvas-relative coords through the layered
     * pane's own LayeredLayout per-component insets - the STANDARD layout
     * path, so initialization, layout and painting (which drive native-view
     * attachment and positioning on this port) all run conventionally.
     */
    /** the class-keyed layer hosting floated views; getLayeredPane() layers
     * default to FlowLayout, so give ours a real LayeredLayout */
    private Container floatLayer;

    private Container floatLayer() {
        if (floatLayer == null) {
            floatLayer = form.getLayeredPane(SimRelayService.class, true);
            floatLayer.setLayout(new com.codename1.ui.layouts.LayeredLayout());
        }
        return floatLayer;
    }

    private void placeFloating(Component v, int x, int y, int w, int h) {
        // a zero-size frame is a transient pre-layout report (the peer's
        // host container hasn't been sized yet); floating it would only
        // park a 0x0 view and poison the constraint - wait for a real frame
        if (w <= 0 || h <= 0) {
            return;
        }
        Container layered = floatLayer();
        com.codename1.ui.layouts.LayeredLayout ll =
                (com.codename1.ui.layouts.LayeredLayout) layered.getLayout();
        if (v.getParent() == null) {
            layered.add(v);
        }
        int layerW = layered.getWidth() > 0 ? layered.getWidth()
                : Display.getInstance().getDisplayWidth();
        int layerH = layered.getHeight() > 0 ? layered.getHeight()
                : Display.getInstance().getDisplayHeight();
        // desired absolute window position of the peer
        int absX = canvas.getAbsoluteX() + x;
        int absY = canvas.getAbsoluteY() + y;
        // insets are relative to the float layer's own origin - compute them
        // directly (the layer fills the form, but resolving its origin keeps
        // this correct without a fragile measure-and-correct second pass that
        // overshoots whenever the first placement clamps)
        int top = Math.max(0, absY - layered.getAbsoluteY());
        int left = Math.max(0, absX - layered.getAbsoluteX());
        int right = Math.max(0, layerW - (left + w));
        int bottom = Math.max(0, layerH - (top + h));
        ll.setInsets(v, top + "px " + right + "px " + bottom + "px " + left + "px");
        form.revalidate();
        sendLog("placeFloating want=" + absX + "," + absY + " " + w + "x" + h
                + " layer=" + layerW + "x" + layerH
                + " got=" + v.getAbsoluteX() + "," + v.getAbsoluteY()
                + " " + v.getWidth() + "x" + v.getHeight());
    }

    /** positions a peer view at canvas-relative coords (EDT) */
    private void peerSetFrame(int peerId, int x, int y, int w, int h) {
        Component v = peerViews.get(peerId);
        if (v == null) {
            return;
        }
        placeFloating(v, x, y, w, h);
    }

    /** webview/media/camera control + removal (EDT) */
    private void peerCommand(int peerId, int cmd, int arg, String sArg) {
        sendLog("peerCmd " + peerId + " cmd=" + cmd + " arg=" + arg
                + (sArg.length() > 0 ? " s=" + (sArg.length() > 60 ? sArg.substring(0, 60) : sArg) : ""));
        Component v = peerViews.get(peerId);
        com.codename1.media.Media m = peerMedia.get(peerId);
        com.codename1.camera.CameraSession cam = peerCameras.get(peerId);
        switch (cmd) {
            case 1:
                if (v instanceof com.codename1.ui.BrowserComponent) {
                    ((com.codename1.ui.BrowserComponent) v).setURL(sArg);
                }
                break;
            case 2:
                if (v instanceof com.codename1.ui.BrowserComponent) {
                    ((com.codename1.ui.BrowserComponent) v).setPage(sArg, null);
                }
                break;
            case 10:
                if (m != null) {
                    m.play();
                }
                break;
            case 11:
                if (m != null) {
                    m.pause();
                }
                break;
            case 12:
                if (m != null) {
                    m.setTime(arg);
                }
                break;
            case 20:
            case 21:
                if (cam != null) {
                    cam.close();
                }
                if (m != null) {
                    m.cleanup();
                }
                if (v != null && v.getParent() != null) {
                    v.getParent().removeComponent(v);
                    form.repaint();
                }
                if (cmd == 21) {
                    peerViews.remove(peerId);
                    peerMedia.remove(peerId);
                    peerCameras.remove(peerId);
                }
                break;
            default:
                break;
        }
    }

    /* ---- outgoing frames ---------------------------------------------------- */

    /** every reply is length-prefixed - the wire is self-describing */
    private void replyInts(int reqId, int[] values) throws IOException {
        synchronized (outLock) {
            out.writeInt(K_REPLY);
            out.writeInt(reqId);
            out.writeInt(values.length);
            for (int v : values) {
                out.writeInt(v);
            }
            out.flush();
        }
    }

    private void reply(int reqId, int value) throws IOException {
        replyInts(reqId, new int[]{value});
    }

    private void reply3(int reqId, int a, int b, int c) throws IOException {
        replyInts(reqId, new int[]{a, b, c});
    }

    private void replyFont(int reqId, Font f) throws IOException {
        int id = nextHandle++;
        fonts.put(id, f);
        replyInts(reqId, new int[]{id, f.getHeight(), f.getAscent(), f.getDescent()});
    }

    /** upstream diagnostics - the relay has no visible stdout when open()ed */
    private void sendLog(String msg) {
        try {
            synchronized (outLock) {
                if (out == null) {
                    return;
                }
                out.writeInt(K_EVENT);
                out.writeInt(E_LOG);
                out.writeInt(0);
                out.writeInt(0);
                out.writeUTF(msg);
                out.flush();
            }
        } catch (IOException ex) {
            // diagnostics must never break the protocol
        }
    }

    /** the display safe-area top inset (status-bar/titlebar reservation) */
    private int safeAreaTop() {
        com.codename1.ui.geom.Rectangle r = new com.codename1.ui.geom.Rectangle();
        Display.getInstance().getDisplaySafeArea(r);
        return r.getY();
    }

    /**
     * Vertical correction applied to incoming touch coordinates: the Catalyst
     * port reports touches offset from where it draws. Tunable via the
     * cn1.touchYCorrection system property (relay-side); defaults to the
     * safe-area top inset.
     */
    private int touchYCorrection() {
        String p = System.getProperty("cn1.touchYCorrection");
        if (p != null) {
            try {
                return Integer.parseInt(p);
            } catch (NumberFormatException ignored) {
            }
        }
        // geometry analysis: the touch form-Y is offset from the drawn
        // content by the canvas's own top inset (canvasAbs.Y), so the
        // single canvasAbs subtraction in sendPointer leaves exactly that
        // much residual. Subtract it again here.
        return canvas != null ? canvas.getAbsoluteY() : 0;
    }

    private void sendPointer(int code, int x, int y) {
        sendEvent2(code, x, y);
    }

    private void sendEvent2(int code, int a, int b) {
        try {
            synchronized (outLock) {
                if (out == null) {
                    return;
                }
                out.writeInt(K_EVENT);
                out.writeInt(code);
                out.writeInt(a);
                out.writeInt(b);
                out.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendEditEvent(int code, String text) {
        try {
            synchronized (outLock) {
                if (out == null) {
                    return;
                }
                out.writeInt(K_EVENT);
                out.writeInt(code);
                out.writeInt(0);
                out.writeInt(0);
                out.writeUTF(text);
                out.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void init(Object context) {
    }

    public void stop() {
    }

    public void destroy() {
    }
}
