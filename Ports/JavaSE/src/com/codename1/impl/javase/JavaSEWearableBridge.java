/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.wearable.WearableConnection;
import com.codename1.wearable.spi.WearableBridge;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The desktop stand-in for `WCSession` / the Wearable Data Layer, so the phone-to-watch API can be
/// developed and debugged without a device.
///
/// The phone app and the watch app run as two separate JVMs -- they are two apps with two sandboxes
/// on a device, and pretending otherwise in the simulator would let bugs through. Each side creates
/// one of these, and the two halves find each other through a directory both resolve to (the app
/// home, which is per-project and therefore shared by the pair):
///
/// - **Replicated data** is files under `wearable/data`. Both sides read and write the same
///   directory, so a value published while the peer was not running is simply there when it starts,
///   which is exactly the guarantee the real transports make. A poller notices the peer's writes.
/// - **Live messages** need a live peer, so they go over a loopback socket on a port derived from
///   that same directory. Whichever side starts first binds it and the other connects; if nobody is
///   on the other end, [#isReachable()] is false and messages are dropped -- again matching the
///   device behavior rather than papering over it.
/// - **File transfers** are modelled as data writes carrying the bytes, since the desktop has no
///   background-transfer scheduler worth simulating.
class JavaSEWearableBridge implements WearableBridge {
    /// Frame kinds on the loopback socket.
    private static final int FRAME_MESSAGE = 1;
    private static final int FRAME_REPLY = 2;
    private static final int FRAME_HELLO = 3;

    private final File dataDir;
    private final File portFile;
    private final boolean watchSide;
    /// True when the project declares a watch app at all. Without one there is nothing to pair with,
    /// which is what a phone with no watch looks like.
    private final boolean paired;

    private volatile Socket peer;
    private volatile DataOutputStream peerOut;
    private volatile boolean closed;

    /// Last-seen modification time per data file, so the poller reports only genuine changes.
    private final Map<String, Long> seenData = new HashMap<String, Long>();

    /// Creates the bridge and starts the rendezvous and data-watching threads.
    ///
    /// @param home the per-project app home directory both sides resolve to
    /// @param watchSide true when this JVM is running the watch app
    /// @param paired true when the project declares a watch app
    JavaSEWearableBridge(File home, boolean watchSide, boolean paired) {
        this.watchSide = watchSide;
        this.paired = paired;
        File root = new File(home, "wearable");
        this.dataDir = new File(root, "data");
        this.portFile = new File(root, "port");
        dataDir.mkdirs();
        primeSeenData();
        if (paired) {
            startRendezvous();
            startDataWatcher();
        }
    }

    // --- state --------------------------------------------------------------

    public boolean isSupported() {
        return paired;
    }

    public boolean isPaired() {
        return paired;
    }

    public boolean isReachable() {
        return peerOut != null;
    }

    public boolean isCompanionAppInstalled() {
        return paired;
    }

    public String[] getConnectedNodes() {
        if (!isReachable()) {
            return new String[0];
        }
        // Mirrors the id \t displayName \t nearby form the device ports produce.
        String name = watchSide ? "Simulated Phone" : "Simulated Watch";
        return new String[] {(watchSide ? "phone" : "watch") + "\t" + name + "\t1"};
    }

    // --- messages -----------------------------------------------------------

    public void sendMessage(String path, byte[] payload, int replyToken) {
        DataOutputStream out = peerOut;
        if (out == null) {
            if (replyToken != 0) {
                WearableConnection.deliverReply(replyToken, null,
                        "The " + (watchSide ? "phone" : "watch") + " app is not running");
            }
            return;
        }
        try {
            writeFrame(out, FRAME_MESSAGE, path, payload, replyToken);
        } catch (IOException err) {
            dropPeer();
            if (replyToken != 0) {
                WearableConnection.deliverReply(replyToken, null, "Link lost: " + err);
            }
        }
    }

    public void sendReply(int replyToken, byte[] payload) {
        DataOutputStream out = peerOut;
        if (out == null) {
            return;
        }
        try {
            writeFrame(out, FRAME_REPLY, "", payload, replyToken);
        } catch (IOException err) {
            dropPeer();
        }
    }

    // --- replicated data ----------------------------------------------------

    public void putData(String path, byte[] payload) {
        File f = dataFile(path);
        try {
            f.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(f);
            try {
                out.write(payload);
            } finally {
                out.close();
            }
            // Our own write must not come back to us as a peer change.
            synchronized (seenData) {
                seenData.put(f.getName(), new Long(f.lastModified()));
            }
        } catch (IOException err) {
            com.codename1.io.Log.p("Wearable simulator: failed to publish " + path + ": " + err);
        }
    }

    public byte[] getData(String path) {
        File f = dataFile(path);
        if (!f.exists()) {
            return null;
        }
        try {
            return readFully(f);
        } catch (IOException err) {
            return null;
        }
    }

    public void removeData(String path) {
        File f = dataFile(path);
        if (f.delete()) {
            synchronized (seenData) {
                seenData.remove(f.getName());
            }
        }
    }

    public String[] getDataPaths() {
        File[] files = dataDir.listFiles();
        if (files == null) {
            return new String[0];
        }
        List<String> out = new ArrayList<String>();
        for (File f : files) {
            if (f.isFile()) {
                out.add(decodePath(f.getName()));
            }
        }
        return out.toArray(new String[out.size()]);
    }

    public void transferFile(String path, String name, byte[] contents) {
        // The desktop has no background-transfer scheduler worth simulating, and a transfer that
        // arrives eventually is indistinguishable from a data write that arrives eventually.
        putData(path + "/" + (name == null ? "file" : name), contents);
    }

    // --- rendezvous ---------------------------------------------------------

    /// Both sides race to bind the loopback port; the winner listens, the loser connects and retries
    /// until the winner exists. Which side wins does not matter, which means the phone and the watch
    /// can be started in either order.
    private void startRendezvous() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                ServerSocket server = null;
                try {
                    server = new ServerSocket(port(), 1, InetAddress.getByName("127.0.0.1"));
                } catch (IOException alreadyBound) {
                    server = null;
                }
                if (server != null) {
                    acceptLoop(server);
                } else {
                    connectLoop();
                }
            }
        }, "CN1 wearable link");
        t.setDaemon(true);
        t.start();
    }

    private void acceptLoop(ServerSocket server) {
        while (!closed) {
            try {
                Socket s = server.accept();
                adoptPeer(s);
                readLoop(s);
            } catch (IOException err) {
                if (closed) {
                    return;
                }
            }
        }
    }

    private void connectLoop() {
        while (!closed) {
            try {
                Socket s = new Socket(InetAddress.getByName("127.0.0.1"), port());
                adoptPeer(s);
                readLoop(s);
            } catch (IOException notUpYet) {
                // The peer app is not running. Wait and retry -- the user may open it at any point.
            }
            if (closed) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }

    private void adoptPeer(Socket s) throws IOException {
        s.setTcpNoDelay(true);
        peer = s;
        peerOut = new DataOutputStream(s.getOutputStream());
        writeFrame(peerOut, FRAME_HELLO, "", new byte[0], 0);
        WearableConnection.notifyStateChanged();
    }

    private void readLoop(Socket s) {
        try {
            DataInputStream in = new DataInputStream(s.getInputStream());
            while (!closed) {
                int kind = in.readByte();
                String path = in.readUTF();
                int token = in.readInt();
                byte[] payload = new byte[in.readInt()];
                in.readFully(payload);
                switch (kind) {
                    case FRAME_MESSAGE:
                        WearableConnection.deliverMessage(path, payload, token);
                        break;
                    case FRAME_REPLY:
                        WearableConnection.deliverReply(token, payload, null);
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException disconnected) {
            // Falls through to dropPeer: the peer app exited or the link broke.
        } finally {
            dropPeer();
        }
    }

    private void dropPeer() {
        Socket s = peer;
        peer = null;
        peerOut = null;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
            WearableConnection.notifyStateChanged();
        }
    }

    private static void writeFrame(DataOutputStream out, int kind, String path,
                                   byte[] payload, int token) throws IOException {
        byte[] body = payload == null ? new byte[0] : payload;
        synchronized (out) {
            out.writeByte(kind);
            out.writeUTF(path == null ? "" : path);
            out.writeInt(token);
            out.writeInt(body.length);
            out.write(body);
            out.flush();
        }
    }

    /// Derives a stable loopback port from the shared directory, so two JVMs of the same project
    /// meet and two different projects do not. Kept in the ephemeral range.
    private int port() {
        int h = dataDir.getAbsolutePath().hashCode();
        return 49152 + Math.abs(h % 10000);
    }

    // --- data watching ------------------------------------------------------

    /// Notices values the peer published. Polling is enough here: the peer writes rarely, the
    /// directory is tiny, and this stays honest about replicated data being eventually consistent.
    private void startDataWatcher() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (!closed) {
                    scanData();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                        return;
                    }
                }
            }
        }, "CN1 wearable data");
        t.setDaemon(true);
        t.start();
    }

    /// Records what is already on disk without reporting it, so a restart does not replay every
    /// value the app itself published last run.
    private void primeSeenData() {
        File[] files = dataDir.listFiles();
        if (files == null) {
            return;
        }
        synchronized (seenData) {
            for (File f : files) {
                if (f.isFile()) {
                    seenData.put(f.getName(), new Long(f.lastModified()));
                }
            }
        }
    }

    private void scanData() {
        File[] files = dataDir.listFiles();
        List<String> gone;
        synchronized (seenData) {
            gone = new ArrayList<String>(seenData.keySet());
        }
        if (files != null) {
            for (File f : files) {
                if (!f.isFile()) {
                    continue;
                }
                gone.remove(f.getName());
                Long previous;
                synchronized (seenData) {
                    previous = seenData.get(f.getName());
                }
                long stamp = f.lastModified();
                if (previous != null && previous.longValue() == stamp) {
                    continue;
                }
                synchronized (seenData) {
                    seenData.put(f.getName(), new Long(stamp));
                }
                try {
                    WearableConnection.deliverDataChanged(decodePath(f.getName()), readFully(f));
                } catch (IOException stillBeingWritten) {
                    // Re-reported on the next pass once the writer has finished.
                    synchronized (seenData) {
                        seenData.remove(f.getName());
                    }
                }
            }
        }
        for (String name : gone) {
            synchronized (seenData) {
                seenData.remove(name);
            }
            WearableConnection.deliverDataRemoved(decodePath(name));
        }
    }

    // --- helpers ------------------------------------------------------------

    private File dataFile(String path) {
        return new File(dataDir, encodePath(path));
    }

    /// Paths are URL-ish (`/workout/start`) and must survive a round trip through a file name on a
    /// case-insensitive file system, so everything outside a conservative set is percent-escaped.
    private static String encodePath(String path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-') {
                sb.append(c);
            } else {
                sb.append('%').append(Integer.toHexString(0x10000 | c).substring(1));
            }
        }
        return sb.toString();
    }

    private static String decodePath(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '%' && i + 4 < name.length()) {
                sb.append((char) Integer.parseInt(name.substring(i + 1, i + 5), 16));
                i += 4;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static byte[] readFully(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            byte[] out = new byte[(int) f.length()];
            int read = 0;
            while (read < out.length) {
                int n = in.read(out, read, out.length - read);
                if (n < 0) {
                    throw new IOException("Truncated while reading " + f);
                }
                read += n;
            }
            return out;
        } finally {
            in.close();
        }
    }

    /// Stops the link. Called when the simulator shuts down.
    void close() {
        closed = true;
        dropPeer();
    }
}
