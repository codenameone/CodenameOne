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
import com.codename1.wearable.WearableMessage;
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
    /// How long a freshly accepted socket has to identify itself before it is dropped.
    private static final int HELLO_TIMEOUT_MILLIS = 5000;
    /// Ceiling on a single frame. Generous for any real payload, small enough that a corrupt length
    /// cannot exhaust the heap.
    private static final int MAX_FRAME_BYTES = 64 * 1024 * 1024;

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
            if (replyToken != 0) {
                // The write succeeding is not the answer arriving. If the peer quits before
                // replying, or never registers a listener for this path, nothing else would ever
                // complete the handler -- and the API promises it runs exactly once.
                scheduleReplyTimeout(replyToken);
            }
        } catch (IOException err) {
            dropPeer();
            if (replyToken != 0) {
                WearableConnection.deliverReply(replyToken, null, "Link lost: " + err);
            }
        }
    }

    /** How long an accepted request may go unanswered before the handler is failed. */
    private static final int REPLY_TIMEOUT_MILLIS = 30000;
    /** One timer for every deadline in the process, as on the device ports. */
    private static final java.util.Timer replyTimer =
            new java.util.Timer("cn1-wearable-sim-replies", true);
    private static final Map<Integer, java.util.TimerTask> replyTimeouts =
            new HashMap<Integer, java.util.TimerTask>();

    private static void scheduleReplyTimeout(final int replyToken) {
        java.util.TimerTask task = new java.util.TimerTask() {
            public void run() {
                synchronized (replyTimeouts) {
                    replyTimeouts.remove(Integer.valueOf(replyToken));
                }
                WearableConnection.deliverReply(replyToken, null,
                        "The peer did not answer within " + (REPLY_TIMEOUT_MILLIS / 1000)
                                + " seconds");
            }
        };
        synchronized (replyTimeouts) {
            replyTimeouts.put(Integer.valueOf(replyToken), task);
        }
        replyTimer.schedule(task, REPLY_TIMEOUT_MILLIS);
    }

    private static void cancelReplyTimeout(int replyToken) {
        java.util.TimerTask task;
        synchronized (replyTimeouts) {
            task = replyTimeouts.remove(Integer.valueOf(replyToken));
        }
        if (task != null) {
            task.cancel();
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
        writeValue(dataFile(path), payload, path);
    }

    private void writeValue(File f, byte[] payload, String path) {
        try {
            f.getParentFile().mkdirs();
            // Write-then-rename: the peer polls this directory every 500ms, and writing in place
            // would let it read a truncated payload mid-write and report a malformed value.
            //
            // The staging name is unique per writer, not per path. The phone and the watch are two
            // JVMs sharing this directory, and both may publish the same path at once: a shared
            // "<path>.tmp" lets each truncate the other's staging file, and the delete-then-rename
            // fallback below can then destroy the winner's file outright.
            File tmp = new File(f.getParentFile(), f.getName() + stagingSuffix());
            // Chosen BEFORE the bytes are written, because it now travels inside them. It still
            // depends only on the target file's current stamp, so moving it earlier changes
            // nothing about the value it picks.
            long stamp = nextStamp(f);
            FileOutputStream out = new FileOutputStream(tmp);
            try {
                // The author travels INSIDE the value. It used to live in a ".author" sidecar, and
                // two files are two operations however they are ordered: A could write its author,
                // B could overwrite that author, and A could then publish its value -- leaving A's
                // bytes permanently labelled as B's, so B suppressed the genuine peer callback and
                // A reported its own write as remote. Prefixing the payload makes the label and the
                // bytes one object, and the rename below publishes both or neither.
                out.write(VALUE_MAGIC);
                out.write(watchSide ? 'w' : 'p');
                for (int i = 7; i >= 0; i--) {
                    out.write((int) ((stamp >>> (i * 8)) & 0xff));
                }
                out.write(payload == null ? new byte[0] : payload);
                out.flush();
            } finally {
                out.close();
            }
            // Stamp the staging file, then publish by rename. Stamping AFTER the rename was a race:
            // writer A could rename, writer B could replace A's file, and A would then set the
            // modification time on B's file and record that time as its own -- so A's watcher would
            // skip B's winning value forever. Renaming an already-stamped file makes publication a
            // single atomic step that can only ever touch this writer's own bytes.
            tmp.setLastModified(stamp);
            // Read from the STAGING file, before it is published. Reading f.lastModified() after
            // the move could see a peer's replacement -- both JVMs publish into this directory --
            // and this writer would then record the PEER's timestamp as its own, so the scan would
            // skip that value as locally authored and the peer's publication would never be
            // announced. The staging file is ours alone until the rename.
            long staged = tmp.lastModified();
            try {
                // An atomic replace, not delete-then-rename. The old fallback deleted whatever was
                // published before retrying, so a peer publishing the same path in that gap had its
                // newer value destroyed and replaced by this side's older staging file -- a lost
                // write, or a momentary removal seen by the peer's watcher.
                java.nio.file.Files.move(tmp.toPath(), f.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.io.IOException | RuntimeException noAtomicMove) {
                // Some filesystems cannot do it. Keep the old path as a fallback rather than
                // failing the publish, but it carries the race described above.
                if (!tmp.renameTo(f)) {
                    f.delete();
                    if (!tmp.renameTo(f)) {
                        throw new IOException("could not replace " + f, noAtomicMove);
                    }
                }
            }
            // Record what the filesystem actually stored, not what we asked for. setLastModified
            // can be refused outright or quantized (FAT to 2s, some network mounts coarser), and
            // recording the requested value then left the real mtime unseen -- so the next scan
            // read this side's own publication as a peer update and invoked its own data listener.
            // A rename preserves the mtime, so the staged value is what lands on disk.
            // Coarse timestamps can also erase the phone/watch side bit, which is encoded in the
            // stamp's low bit.
            long recorded = staged;
            if (recorded <= 0) {
                recorded = stamp;
            }
            // Our own write must not come back to us as a peer change. Stored in the same
            // encoding the scan compares against, and always as locally authored -- this IS our
            // publication.
            synchronized (seenData) {
                seenData.put(f.getName(), Long.valueOf(seenKey(stamp, recorded, true)));
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
            return readPayload(f);
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
            // A transfer is not a readable replicated path -- getData() on its storage name is not
            // part of the API -- so it is left out, matching the device ports.
            if (f.isFile() && !f.getName().endsWith(".tmp") && !isTransfer(f.getName())) {
                out.add(decodePath(f.getName()));
            }
        }
        return out.toArray(new String[out.size()]);
    }

    public void transferFile(String path, String name, byte[] contents) {
        // The desktop has no background-transfer scheduler worth simulating, and a transfer that
        // arrives eventually is indistinguishable from a data write that arrives eventually. The
        // bytes still have to be encoded as a payload, though: the receiving side decodes every
        // value as one, and raw file bytes would arrive as a malformed message with no name.
        String fileName = name == null ? "file" : name;
        WearableMessage wrapper = new WearableMessage(path)
                .put("name", fileName)
                .put("contents", contents == null ? new byte[0] : contents);
        // Two files sent to the same logical path must not overwrite each other, so the file name is
        // part of the storage name -- but it must not become the *delivered* path: a listener routes
        // on the path the sender passed to transferFile. The marker keeps the two recoverable, and
        // is a character encodePath can never emit.
        //
        // The sequence is what makes each transfer its own file. A transfer is one-shot, so sending
        // twice to the same path and name before the 500ms watcher has consumed the first -- or at
        // any time while the peer is offline -- must queue two deliveries, not silently replace one
        // with the other. (A replicated value is the opposite: putData deliberately overwrites.)
        // The sender's side is part of the name. Both halves scan this one directory, so without it
        // a sender cannot tell its own pending transfer from an inbound one: after a restart
        // primeSeenData() records nothing, and the sender's first scan would consume and delete the
        // very transfer it is waiting to hand over.
        writeValue(new File(dataDir, encodePath(path) + TRANSFER_MARKER + encodePath(fileName)
                        + TRANSFER_MARKER + sideTag()
                        + TRANSFER_MARKER + Long.toHexString(nextTransferSequence())),
                wrapper.toByteArray(), path);
    }

    /** Identifies which half wrote a file: transfers are only consumed by the other side. */
    private String sideTag() {
        return watchSide ? "w" : "p";
    }

    /** True when this transfer was written by the other half, and so is ours to consume. */
    private boolean isInboundTransfer(String storageName) {
        if (!isTransfer(storageName)) {
            return false;
        }
        String[] parts = storageName.split(TRANSFER_MARKER);
        // <path>X<name>X<side>X<seq>; anything shorter predates the side tag, so treat it as inbound
        // rather than stranding it.
        return parts.length < 4 || !parts[2].equals(sideTag());
    }

    /** Distinguishes successive transfers so neither overwrites the other on disk. */
    private static synchronized long nextTransferSequence() {
        long now = System.currentTimeMillis();
        lastTransferSequence = now > lastTransferSequence ? now : lastTransferSequence + 1;
        return lastTransferSequence;
    }

    private static long lastTransferSequence;


    /**
     * Separates the logical path from the file name in a transfer's storage name. Uppercase, which
     * {@link #encodePath} never produces, so it cannot occur inside either half.
     */
    private static final String TRANSFER_MARKER = "X";

    /// The path a stored value is delivered on: for a transfer, the path its sender passed to
    /// {@code transferFile} rather than the filename-suffixed name it is stored under.
    private static String deliveryPath(String storageName) {
        int marker = storageName.indexOf(TRANSFER_MARKER);
        return decodePath(marker < 0 ? storageName : storageName.substring(0, marker));
    }

    private static boolean isTransfer(String storageName) {
        return storageName.indexOf(TRANSFER_MARKER) >= 0;
    }

    // --- rendezvous ---------------------------------------------------------

    /// Both sides race to bind the loopback port; the winner listens, the loser connects and retries
    /// until the winner exists. Which side wins does not matter, which means the phone and the watch
    /// can be started in either order.
    private void startRendezvous() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                // Re-run for as long as the bridge lives, because the role is not permanent. A
                // bind that fails only because an unrelated process momentarily held the port used
                // to make this half a connector for good: once that process let go, neither half
                // would bind again, both kept dialling a server that did not exist, and the pair
                // stayed unreachable until a restart. Losing the election is a fact about right
                // now, not about the run.
                while (!closed) {
                    ServerSocket server = null;
                    try {
                        server = new ServerSocket(port(), 1, InetAddress.getByName("127.0.0.1"));
                    } catch (IOException alreadyBound) {
                        server = null;
                    }
                    if (server != null) {
                        acceptLoop(server);
                    } else {
                        connectOnce();
                    }
                }
            }
        }, "CN1 wearable link");
        t.setDaemon(true);
        t.start();
    }

    private void acceptLoop(ServerSocket server) {
        try {
            while (!closed) {
                try {
                    Socket s = server.accept();
                    readLoop(s, adoptPeer(s));
                } catch (IOException err) {
                    if (closed) {
                        return;
                    }
                }
            }
        } finally {
            // Released on the way out, so the election that follows can bind again rather than
            // losing to this thread's own abandoned socket.
            try {
                server.close();
            } catch (IOException ignored) {
            }
        }
    }

    /// ONE attempt to reach the server, then returns so the election can run again.
    ///
    /// It used to loop internally for the bridge's lifetime, which is what made a lost election
    /// permanent -- the caller never got the chance to try binding again.
    private void connectOnce() {
        try {
            Socket s = new Socket(InetAddress.getByName("127.0.0.1"), port());
            readLoop(s, adoptPeer(s));
        } catch (IOException notUpYet) {
            // The peer app is not running. The caller waits and re-elects -- the user may open it
            // at any point, and by then this half may be the one that can bind.
        }
        if (closed) {
            return;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /// Connects and says hello, returning the output stream WITHOUT publishing it.
    ///
    /// The stream stays private until the peer's own hello checks out. Assigning `peerOut` here
    /// made `isReachable()` true and let `sendMessage()` write application traffic into whatever
    /// held the derived port -- an unrelated local service, or a colliding project -- for the whole
    /// five seconds before the identity check timed out. The hello itself is the one thing written
    /// before verification, which is what verification is for.
    private DataOutputStream adoptPeer(Socket s) throws IOException {
        s.setTcpNoDelay(true);
        // A read deadline, because a service that merely ACCEPTS on our derived port and then says
        // nothing would otherwise leave the reader blocked in readByte() forever -- with the
        // simulator reporting itself reachable the whole time.
        s.setSoTimeout(HELLO_TIMEOUT_MILLIS);
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        // The hello carries the project identity. The port is derived from a hash of the shared
        // directory truncated to 10,000 values, so two unrelated projects whose paths collide -- or
        // any other local service already sitting on that port -- would otherwise connect, both
        // report reachable, and exchange live messages and replies between unrelated apps. A
        // successful connection to a small shared port range proves nothing about who is on it.
        writeFrame(out, FRAME_HELLO, projectIdentity(), new byte[0], 0);
        // `peer` and `peerOut` are both assigned by the reader once the peer's own hello is
        // verified, NOT here. Publishing either on a bare accepted socket meant an unrelated local
        // service holding the port made isReachable() true and live messages went into it.
        return out;
    }

    private void readLoop(Socket s, DataOutputStream unverified) {
        boolean helloVerified = false;
        try {
            DataInputStream in = new DataInputStream(s.getInputStream());
            while (!closed) {
                int kind = in.readByte();
                String path = in.readUTF();
                int token = in.readInt();
                int length = in.readInt();
                if (length < 0 || length > MAX_FRAME_BYTES) {
                    // A corrupt or mismatched peer stream. Allocating on this would throw
                    // NegativeArraySizeException or OutOfMemoryError, neither of which the
                    // accept/connect loop catches -- it would take the link's thread with it.
                    throw new IOException("Implausible frame length " + length);
                }
                byte[] payload = new byte[length];
                in.readFully(payload);
                switch (kind) {
                    case FRAME_HELLO:
                        // Validate the identity before anything else is honoured. A peer on a
                        // colliding port -- another project, or an unrelated local service that
                        // happens to speak enough of this to get here -- is dropped rather than
                        // treated as the pair.
                        if (!projectIdentity().equals(path)) {
                            throw new IOException("Wearable simulator: refusing a peer from a "
                                    + "different project (" + path + ")");
                        }
                        helloVerified = true;
                        // Only now is this a peer. Reachability, the writable stream and the state
                        // change all follow the identity, not the connection.
                        peer = s;
                        peerOut = unverified;
                        s.setSoTimeout(0);
                        WearableConnection.notifyStateChanged();
                        break;
                    case FRAME_MESSAGE:
                        if (!helloVerified) {
                            throw new IOException("Wearable simulator: traffic before a verified hello");
                        }
                        WearableConnection.deliverMessage(path, payload, token);
                        break;
                    case FRAME_REPLY:
                        if (!helloVerified) {
                            throw new IOException("Wearable simulator: traffic before a verified hello");
                        }
                        cancelReplyTimeout(token);
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
            // And close THIS socket, which dropPeer does not: it clears the globally verified peer,
            // and a socket rejected for a failed or timed-out hello never became one. The connector
            // retries every second, so leaking one descriptor per attempt against an unrelated
            // service on the derived port would eventually exhaust the simulator's file handles.
            try {
                s.close();
            } catch (IOException ignored) {
            }
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

    /// Identifies the project on the wire, so a port collision cannot be mistaken for a peer.
    ///
    /// The absolute shared directory is the identity: it is what "the same project" means here, and
    /// it is exactly what the port hash throws away.
    private String projectIdentity() {
        return dataDir.getAbsolutePath();
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

    /// Leaves what is already on disk unrecorded, so the first watcher pass replays it.
    ///
    /// A value the peer published while this side was stopped is exactly what a starting app needs
    /// to see -- that is the guarantee replicated data makes, and recording the files as already
    /// seen would silently break it. The cost is that a value this app published itself last run is
    /// replayed to it too, which listeners handle the same way they handle any republish.
    private void primeSeenData() {
        // Deliberately empty: see above. Kept as a named step so the reasoning has somewhere to
        // live rather than being an absence.
    }

    private void scanData() {
        File[] files = dataDir.listFiles();
        List<String> gone;
        synchronized (seenData) {
            gone = new ArrayList<String>(seenData.keySet());
        }
        if (files != null) {
            for (File f : files) {
                if (!f.isFile() || f.getName().endsWith(".tmp")) {
                    continue;
                }
                gone.remove(f.getName());
                Long previous;
                synchronized (seenData) {
                    previous = seenData.get(f.getName());
                }
                long stamp = f.lastModified();
                // The AUTHOR is part of what makes a file "already seen", not the timestamp alone.
                // On a filesystem with coarse mtime granularity a local publication and a peer
                // replacement can land in the same tick: the local write has already stored that
                // timestamp, so the peer's file matched and was skipped without being looked at,
                // and nothing delivered it until some later write happened to move the stamp. A
                // cheap header peek separates them -- the two sides never write the same author.
                long seenKey = peekSeenKey(f, stamp);
                if (previous != null && previous.longValue() == seenKey) {
                    continue;
                }
                synchronized (seenData) {
                    seenData.put(f.getName(), Long.valueOf(seenKey));
                }
                if (isTransfer(f.getName()) && !isInboundTransfer(f.getName())) {
                    // Our own outbound transfer, seen again because primeSeenData() deliberately
                    // records nothing at startup. It is not an inbound delivery: reporting it would
                    // hand the sender its own file through its own data listener.
                    continue;
                }
                // ONE read, used for both the author test and the payload. Classifying from the
                // file and then reading it again let this JVM's own putData land in between: the
                // author check inspected the peer's file, the read returned our freshly published
                // bytes, and the peer-only listener was handed this device's own publication --
                // with writeValue having already recorded the local stamp, so no later scan
                // corrected it.
                byte[] snapshot;
                try {
                    snapshot = readFully(f);
                } catch (IOException stillBeingWritten) {
                    synchronized (seenData) {
                        seenData.remove(f.getName());
                    }
                    continue;
                }
                // The DELIVERED snapshot decides what was seen, not the earlier peek. A peer can
                // replace the file between the two with one carrying the same coarse timestamp, so
                // the peek's classification could be recorded while the peer's bytes were handed
                // over -- and the next scan, computing the key from that same peer file, would see
                // a different key and deliver it a second time.
                synchronized (seenData) {
                    seenData.put(f.getName(),
                            Long.valueOf(seenKey(framedStamp(snapshot), stamp,
                                    authoredLocallyFor(snapshot, stamp))));
                }
                if (f.lastModified() != stamp) {
                    // Replaced while we were reading it, so this snapshot belongs to neither the
                    // file we classified nor the one now on disk. Forget the marker and let the
                    // next scan see the winner whole.
                    //
                    // Unless the file is GONE. This scan has already taken the name off `gone`, so
                    // dropping the marker too would leave the next scan with nothing to notice the
                    // disappearance by -- a peer that republished and then removed a path would
                    // never produce dataRemoved, and the listener would hold the old value for
                    // good. Restoring the previous marker puts the name back in the next scan's
                    // `gone` list, which is what reports the removal.
                    synchronized (seenData) {
                        if (!f.exists() && previous != null) {
                            seenData.put(f.getName(), previous);
                        } else {
                            seenData.remove(f.getName());
                        }
                    }
                    continue;
                }
                if (!isTransfer(f.getName()) && authoredLocallyFor(snapshot, stamp)) {
                    // Our own VALUE, for the same reason. primeSeenData() records nothing so that a
                    // value published while this side was down still replays on startup -- but that
                    // also replayed values THIS side published before it restarted, reporting them
                    // through WearableDataListener, whose contract is peer changes only.
                    //
                    // The author is already in the stamp: nextStamp puts the two JVMs in disjoint
                    // residue classes (base * 2 + sideBit) so they cannot collide, and that bit
                    // says which side wrote the file. No extra bookkeeping needed, and it survives
                    // a restart because it lives in the file's own timestamp.
                    continue;
                }
                {
                    final File delivered = f;
                    final boolean inbound = isInboundTransfer(f.getName());
                    // Deleted from INSIDE the delivery, not beside it. This file is the only durable
                    // copy of a one-shot transfer: deleting it as soon as the delivery was queued
                    // lost it outright if the simulator closed before the listener ran, or if the
                    // delivery was merely parked because no listener had registered yet.
                    WearableConnection.deliverDataChangedTracked(deliveryPath(f.getName()),
                            payloadOf(snapshot), inbound ? new Runnable() {
                                public void run() {
                                    delivered.delete();
                                    synchronized (seenData) {
                                        seenData.remove(delivered.getName());
                                    }
                                }
                            } : null, inbound ? new Runnable() {
                                public void run() {
                                    // Evicted from the pending queue before a listener existed. The
                                    // file is still on disk, but this scan already recorded it as
                                    // seen, so nothing would offer it again until a restart.
                                    // Forgetting it puts it back in front of the next scan.
                                    synchronized (seenData) {
                                        seenData.remove(delivered.getName());
                                    }
                                }
                            } : null);
                    // The deletion that used to live here now runs inside the delivery callback
                    // above. A transfer is one-shot, so the delivered file goes -- leaving it would
                    // make every restart of the receiving simulator replay it, since
                    // primeSeenData() deliberately records nothing so that offline VALUES do
                    // replay. Only an INBOUND transfer is deleted: removing our own would destroy
                    // one still waiting for the peer to start.
                }
            }
        }
        for (String name : gone) {
            synchronized (seenData) {
                seenData.remove(name);
            }
            if (isTransfer(name)) {
                // A transfer disappearing means the peer consumed it, which is the transport doing
                // its job -- not the logical path being removed. Reporting dataRemoved here would
                // tell the sender's own listeners that a path it never removed had gone, and that
                // path may well still hold an unrelated replicated value.
                continue;
            }
            WearableConnection.deliverDataRemoved(deliveryPath(name));
        }
    }

    // --- helpers ------------------------------------------------------------

    private File dataFile(String path) {
        return new File(dataDir, encodePath(path));
    }

    /**
     * A modification stamp strictly newer than the one this file already carries, and than any this
     * process has written for it. The file system's own granularity can be as coarse as a second, so
     * "now" is not on its own enough to mark a value as new.
     */
    /**
     * A staging-file suffix unique to this process and call. Still ends in {@code .tmp} so the
     * watcher's existing skip rule keeps ignoring staging files.
     */
    private static synchronized String stagingSuffix() {
        return "." + PROCESS_TAG + "." + (stagingCounter++) + ".tmp";
    }

    private static int stagingCounter;
    /** Identifies this JVM among the pair; the two sides share a directory but not a process. */
    private static final String PROCESS_TAG =
            Integer.toHexString(java.lang.management.ManagementFactory.getRuntimeMXBean()
                    .getName().hashCode());

    private long nextStamp(File f) {
        synchronized (JavaSEWearableBridge.class) {
            long now = System.currentTimeMillis();
            // The file already carries an ENCODED stamp (base * 2 + sideBit), so decode it before
            // using it as a floor. Feeding the encoded value straight back in doubled the base on
            // every publish, which runs away exponentially within a few dozen writes.
            long floor = Math.max(f.lastModified() / 2, lastStamp);
            long base = now > floor ? now : floor + 1;
            // Put the two JVMs in disjoint residue classes. lastStamp and this lock are process
            // local, so both halves publishing the same path in the same millisecond could otherwise
            // compute the SAME stamp from the same lastModified() -- and each would then record the
            // other's published stamp as its own and never deliver the peer's value. Doubling and
            // adding a side bit makes a collision arithmetically impossible while keeping the
            // strictly-increasing property the watcher relies on.
            lastStamp = base;
            return base * 2 + (watchSide ? 1 : 0);
        }
    }

    /**
     * Whether a published stamp was written by THIS side of the pair.
     *
     * <p>Reads the side bit {@link #nextStamp} encodes. Only meaningful for stamps this bridge
     * wrote; a file whose modification time the filesystem quantized may answer either way, which
     * is why publication records the value the filesystem actually stored rather than the one it
     * was asked for.</p>
     */
    private boolean authoredLocally(long stamp) {
        return (stamp & 1L) == (watchSide ? 1L : 0L);
    }

    /**
     * Author identity that does not depend on the filesystem preserving a single bit.
     *
     * <p>The side bit rides in the stamp's low bit, and a filesystem that rounds modification times
     * -- FAT to two seconds, some network mounts coarser -- erases it. The phone then reads every
     * watch publication as locally authored and suppresses its callback, which is the pairing
     * silently not working rather than failing.
     *
     * <p>So the author is also written into the value's own header, which the filesystem cannot
     * round. The stamp stays authoritative when no header is present (a value left by an older
     * build), because a wrong-but-present answer is worse than the previous behaviour only if it
     * disagrees, and the header is published by the same rename as the bytes it describes.</p>
     */
    /// The writer's own stamp out of the frame, or Long.MIN_VALUE when the bytes carry none.
    private static long framedStamp(byte[] all) {
        // Only the current framing carries one; a v1 file falls back to mtime folded with author.
        if (headerLength(all) != VALUE_HEADER_LENGTH) {
            return Long.MIN_VALUE;
        }
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (all[VALUE_MAGIC.length + 1 + i] & 0xffL);
        }
        return v;
    }

    /// Identifies a version of a file.
    ///
    /// The writer's embedded stamp when there is one, because it is unique per write and cannot be
    /// blurred by a filesystem that rounds timestamps -- two writes by the same side within one
    /// coarse tick were otherwise identical here, so the second was skipped as already-seen and the
    /// listener kept the older value indefinitely.
    ///
    /// Falls back to mtime folded with the author for bytes that carry no frame, which is the best
    /// available answer for a file this build did not write.
    private static long seenKey(long framedStamp, long mtime, boolean authoredLocally) {
        if (framedStamp != Long.MIN_VALUE) {
            return framedStamp;
        }
        return mtime * 2 + (authoredLocally ? 1 : 0);
    }

    /// Reads just the frame header to decide authorship, so a 500ms scan does not pull whole
    /// transfer payloads into memory. The delivery path re-derives this from the full snapshot it
    /// actually hands over, so a file replaced between the two is still caught there.
    /// The version marker from the header alone, so a 500ms scan does not pull whole transfer
    /// payloads into memory. The delivery path recomputes this from the full snapshot it hands
    /// over, so a file replaced between the two is recorded as the file actually delivered.
    private long peekSeenKey(File f, long mtime) {
        byte[] head = readHead(f);
        if (head == null) {
            return seenKey(Long.MIN_VALUE, mtime, authoredLocally(mtime));
        }
        return seenKey(framedStamp(head), mtime, authoredLocallyFor(head, mtime));
    }

    private boolean peekAuthoredLocally(File f, long stamp) {
        byte[] head = readHead(f);
        return head == null ? authoredLocally(stamp) : authoredLocallyFor(head, stamp);
    }

    /// The frame header, or null when the file is shorter than one or unreadable.
    private static byte[] readHead(File f) {
        byte[] head = new byte[VALUE_HEADER_LENGTH];
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            int off = 0;
            while (off < head.length) {
                int r = in.read(head, off, head.length - off);
                if (r < 0) {
                    break;
                }
                off += r;
            }
            if (off < VALUE_HEADER_V1_LENGTH) {
                return null;
            }
            if (off < head.length) {
                // A v1 file, or a v2 file truncated: hand back exactly what was read so
                // headerLength can judge it rather than guessing from a padded buffer.
                byte[] shorter = new byte[off];
                System.arraycopy(head, 0, shorter, 0, off);
                return shorter;
            }
        } catch (IOException unreadable) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return head;
    }

    private boolean authoredLocallyFor(byte[] snapshot, long stamp) {
        Boolean recorded = authorOf(snapshot);
        if (recorded != null) {
            return recorded.booleanValue() == watchSide;
        }
        return authoredLocally(stamp);
    }

    /// Convenience for callers holding a File rather than its bytes.
    private boolean authoredLocallyFor(File f, long stamp) {
        try {
            return authoredLocallyFor(readFully(f), stamp);
        } catch (IOException unreadable) {
            return authoredLocally(stamp);
        }
    }

    /// The author recorded in the value's own header: TRUE for the watch, FALSE for the phone,
    /// null when the file predates the header or is too short to carry one.
    private static Boolean authorOf(byte[] snapshot) {
        if (headerLength(snapshot) == 0) {
            return null;
        }
        // Same offset in both framings.
        byte who = snapshot[VALUE_MAGIC.length];
        if (who == 'w') {
            return Boolean.TRUE;
        }
        if (who == 'p') {
            return Boolean.FALSE;
        }
        return null;
    }

    /// True when these bytes carry the author frame.
    private static boolean framed(byte[] all) {
        return headerLength(all) > 0;
    }

    /// The frame length these bytes carry: 0 when unframed, {@link #VALUE_HEADER_V1_LENGTH} for the
    /// first framing, {@link #VALUE_HEADER_LENGTH} for the current one.
    private static int headerLength(byte[] all) {
        if (matches(all, VALUE_MAGIC) && all.length >= VALUE_HEADER_LENGTH) {
            return VALUE_HEADER_LENGTH;
        }
        if (matches(all, VALUE_MAGIC_V1) && all.length >= VALUE_HEADER_V1_LENGTH) {
            return VALUE_HEADER_V1_LENGTH;
        }
        return 0;
    }

    private static boolean matches(byte[] all, byte[] magic) {
        if (all == null || all.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (all[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /// The value's bytes with the author frame removed, from an in-memory snapshot.
    private static byte[] payloadOf(byte[] all) {
        int skip = headerLength(all);
        if (skip == 0) {
            return all;
        }
        byte[] body = new byte[all.length - skip];
        System.arraycopy(all, skip, body, 0, body.length);
        return body;
    }

    /// Marks a framed value file. Chosen so a stale sandbox written before the header existed still
    /// reads correctly: no magic simply means "fall back to the stamp's side bit".
    private static final byte[] VALUE_MAGIC = {'C', 'N', '1', 'W', 'A', '2'};

    /// The first framing, still read. It carries the author but no stamp, so a sandbox written by
    /// the immediately preceding build keeps working: rejecting it outright would hand the app its
    /// own six magic bytes as payload, and WearableMessage would read the 'C' as a wire version and
    /// decode an empty value.
    private static final byte[] VALUE_MAGIC_V1 = {'C', 'N', '1', 'W', 'A', '1'};
    private static final int VALUE_HEADER_V1_LENGTH = VALUE_MAGIC_V1.length + 1;

    /// magic + author + the writer's own 8-byte stamp.
    ///
    /// The stamp is in the FILE because the filesystem cannot be trusted to keep it. mtime is what
    /// the scan used to identify a version, and on a coarse-granularity filesystem two writes by
    /// the same side within one tick are indistinguishable by mtime and author alike -- so the
    /// second value was skipped as already-seen and the listener sat on the older one until some
    /// unrelated write moved the clock. nextStamp already produces a value that is unique per write
    /// and monotonic per side; writing it down makes the identity exact and independent of what the
    /// filesystem chose to record.
    private static final int VALUE_HEADER_LENGTH = VALUE_MAGIC.length + 1 + 8;


    /// The value's bytes with the author frame removed. An unframed file is returned whole, so a
    /// value left by an older build still reads as itself rather than losing its first seven bytes.
    private static byte[] readPayload(File f) throws IOException {
        // One read, then inspect the prefix in memory. Reading the file twice -- once for the
        // header, once for the bytes -- could straddle a republication and return one file's
        // header with another file's payload.
        return payloadOf(readFully(f));
    }

    private static long lastStamp;

    /// Paths are URL-ish (`/workout/start`) and must survive a round trip through a file name on a
    /// case-insensitive file system, so everything outside a conservative set is percent-escaped.
    private static String encodePath(String path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            // '.' is deliberately NOT in this set, which fixes two problems at once and makes
            // both structural rather than pattern matches.
            //
            // Staging files are named "<encoded>.<tag>.<n>.tmp". While an encoded path could
            // itself contain a dot, a published path of "/sync/state.tmp" was indistinguishable
            // from a staging file, so its peer callback was skipped forever and getDataPaths()
            // hid it even though getData() could read it. With dots escaped, a literal dot in a
            // file name can only have come from the staging suffix.
            //
            // It also stops "." and ".." being filesystem references: they encoded to themselves,
            // so dataFile(".") resolved to the data directory, putData(".") could never replace
            // it, and removeData(".") could delete the directory when empty.
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-') {
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
