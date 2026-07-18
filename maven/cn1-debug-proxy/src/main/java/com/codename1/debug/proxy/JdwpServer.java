/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.debug.proxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimum-viable JDWP server. Speaks enough of the protocol that jdb can
 * connect, set a line breakpoint, see it fire, walk the stack, and read
 * primitive locals — which is the bar we're aiming for in this first cut.
 *
 * Many JDWP commands are answered with empty / "no capability" replies so
 * the IDE doesn't hang; expanding the surface (object inspection, eval,
 * field reads, ARM events) is a follow-up.
 *
 * IDs: typeID = classId from the sidecar, methodID = methodId from the
 * sidecar, both padded to 8 bytes. threadID = the ParparVM threadId.
 * frameID = (threadId &lt;&lt; 32) | frameIdxFromTop. The "bytecode index"
 * inside a JDWP Location is just the source line — we don't carry real
 * bytecode offsets in ParparVM, and jdb uses the index purely to look
 * the same value back up via Method.LineTable.
 */
public final class JdwpServer implements DeviceConnection.DeviceListener {

    private static final String HANDSHAKE = "JDWP-Handshake";

    // JDWP command sets
    private static final int CS_VIRTUAL_MACHINE     = 1;
    private static final int CS_REFERENCE_TYPE      = 2;
    private static final int CS_CLASS_TYPE          = 3;
    private static final int CS_METHOD              = 6;
    private static final int CS_OBJECT_REFERENCE    = 9;
    private static final int CS_STRING_REFERENCE    = 10;
    private static final int CS_THREAD_REFERENCE    = 11;
    private static final int CS_THREAD_GROUP_REF    = 12;
    private static final int CS_ARRAY_REFERENCE     = 13;
    private static final int CS_CLASS_LOADER_REF    = 14;
    private static final int CS_EVENT_REQUEST       = 15;
    private static final int CS_STACK_FRAME         = 16;
    private static final int CS_CLASS_OBJECT_REF    = 17;
    private static final int CS_EVENT               = 64;

    // Event kinds (subset)
    private static final int EK_SINGLE_STEP   = 1;
    private static final int EK_BREAKPOINT    = 2;
    private static final int EK_THREAD_START  = 6;
    private static final int EK_THREAD_DEATH  = 7;
    private static final int EK_CLASS_PREPARE = 8;
    private static final int EK_CLASS_UNLOAD  = 9;
    private static final int EK_VM_START      = 90;
    private static final int EK_VM_DEATH      = 99;

    // refTypeTag
    private static final int TYPE_TAG_CLASS = 1;
    private static final int TYPE_TAG_INTERFACE = 2;
    private static final int TYPE_TAG_ARRAY = 3;

    // SuspendPolicy
    private static final int SP_NONE         = 0;
    private static final int SP_EVENT_THREAD = 1;
    private static final int SP_ALL          = 2;

    private final int port;
    private final int devicePort;
    // Delivered by the device over the wire (CMD_GET_SYMBOLS), not a local
    // file. Null until the device dials in and streams its table. Some JDWP
    // clients query classes before VM_START, so an IDE-first connection waits
    // on symbolsLock before dispatching any commands.
    private volatile SymbolTable symbols;
    private final Object symbolsLock = new Object();
    private volatile DeviceConnection device;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final Object writeLock = new Object();

    private final AtomicInteger nextRequestId = new AtomicInteger(1);
    private final Map<Integer, BpRequest> bpRequests = new ConcurrentHashMap<>();
    // Pending step request keyed by threadId so the device's STEP_COMPLETE
    // event can find the JDWP request ID that triggered it.
    private final Map<Long, Integer> stepRequests = new ConcurrentHashMap<>();
    private final List<Long> knownThreads = new ArrayList<>();
    // Synthetic thread group ID. jdb wants a non-null group on every thread.
    private static final long FAKE_GROUP_ID = 0xCAFEL;
    private static final long FAKE_CLASSLOADER_ID = 0;
    // Last suspending event's thread, used by ThreadReference.Frames when
    // no explicit per-thread bookkeeping has been done yet.
    private volatile long lastSuspendedThread = 0;

    /*
     * JDWP treats reference-type and method IDs of 0 as "null", but the
     * translator's classId/methodId space is dense starting at 0. We shift
     * both directions by +1 at the JDWP boundary so id 0 stays reserved.
     */
    private static long toJdwpRef(int sidecarId)  { return sidecarId + 1L; }
    private static int  fromJdwpRef(long jdwpId)  { return (int)(jdwpId - 1L); }

    /**
     * JDWP method modifier bits. We track only static in the sidecar
     * today; public is assumed. Adding STATIC for static methods is
     * load-bearing — jdb's expression parser only resolves
     * {@code Class.method()} syntax against methods reported with the
     * STATIC modifier bit.
     */
    private static int jdwpMethodModifiers(SymbolTable.MethodInfo m) {
        int flags = 0x0001; // PUBLIC
        if (m.isStatic) flags |= 0x0008;
        return flags;
    }
    // True once the device has handshook; the VM_START event will fire as
    // soon as a JDWP client is attached (and was already attached when the
    // device joined — order doesn't matter).
    private volatile boolean deviceHelloReceived = false;

    private static final class BpRequest {
        final int requestId;
        final long typeID;  // classId
        final long methodID;
        final long codeIndex; // source line
        final int suspendPolicy;
        BpRequest(int rid, long typeID, long methodID, long codeIndex, int sp) {
            this.requestId = rid; this.typeID = typeID; this.methodID = methodID;
            this.codeIndex = codeIndex; this.suspendPolicy = sp;
        }
    }

    public JdwpServer(int port) {
        this(port, 55333);
    }

    public JdwpServer(int port, int devicePort) {
        this.port = port;
        this.devicePort = devicePort;
    }

    public void setDevice(DeviceConnection device) {
        this.device = device;
    }

    /**
     * Receives the symbol table streamed off the device, just before onHello.
     * Wakes an IDE that attached before the device so its queued JDWP requests
     * can continue against a complete table.
     */
    @Override public void onSymbols(SymbolTable symbols) {
        synchronized (symbolsLock) {
            this.symbols = symbols;
            symbolsLock.notifyAll();
        }
    }

    public void acceptAndServe() throws IOException {
        // Loop on accept so the developer can detach and reattach the IDE
        // multiple times without restarting the proxy. The device side keeps
        // running between attaches and the proxy preserves its breakpoint /
        // event-request state.
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[jdwp] listening on port " + port + " for debugger (jdb) to attach");
            while (true) {
                socket = server.accept();
                socket.setTcpNoDelay(true);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                System.out.println("[jdwp] debugger connected from " + socket.getRemoteSocketAddress());

                if (!doHandshake()) {
                    System.err.println("[jdwp] handshake failed");
                    closeJdwpSession();
                    continue;
                }
                System.out.println("[jdwp] handshake complete");

                // NetBeans queries classes immediately after the JDWP
                // handshake, before VM_START. Wait here instead of dispatching
                // those commands against a null symbol table. Commands already
                // sent by the IDE remain buffered on the socket and are handled
                // after the device connects.
                waitForSymbols();
                sendVmStart();
                scheduleAutoResume();

                try {
                    packetLoop();
                } catch (IOException eof) {
                    // Debugger disconnected mid-session; fall through to reset.
                } finally {
                    closeJdwpSession();
                }
                System.out.println("[jdwp] debugger session ended; listening for the next attach");
            }
        }
    }

    private SymbolTable waitForSymbols() throws IOException {
        SymbolTable current = symbols;
        if (current != null) {
            return current;
        }

        System.out.println("[jdwp] debugger attached before the iOS app; waiting for device symbols");
        System.out.println("[jdwp] launch the app now; this debugger session will continue when it connects");
        boolean troubleshootingShown = false;
        synchronized (symbolsLock) {
            while ((current = symbols) == null) {
                try {
                    symbolsLock.wait(10000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for device symbols", e);
                }
                if (symbols == null && !troubleshootingShown) {
                    System.err.println("[jdwp] still waiting for the iOS app on device port " + devicePort);
                    System.err.println("[jdwp] verify the installed build uses "
                            + "codename1.arg.ios.onDeviceDebug=true, proxyHost is this computer's "
                            + "LAN IP, and the firewall allows TCP " + devicePort);
                    troubleshootingShown = true;
                }
            }
        }
        System.out.println("[jdwp] device symbols received; continuing debugger session");
        return current;
    }

    /**
     * Releases the device-side waitForAttach gate only after both sides are
     * connected. The delay gives the IDE time to register breakpoints. Most
     * JDWP debuggers don't send VM.Resume automatically on attach.
     */
    private void scheduleAutoResume() {
        Thread autoResume = new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignore) {}
            try { if (device != null) device.resumeAll(); }
            catch (IOException ignore) {}
        }, "cn1-debug-auto-resume");
        autoResume.setDaemon(true);
        autoResume.start();
    }

    private void closeJdwpSession() {
        try { if (socket != null) socket.close(); } catch (IOException ignore) {}
        socket = null;
        in = null;
        out = null;
        // Clear any pending step requests so a stale one from the previous
        // attach can't fire against the new debugger.
        stepRequests.clear();
        // Re-arm VM_START for the next attach.
        vmStartSent = false;
        // Breakpoints stay in bpRequests so the device keeps them set; the
        // next attaching IDE will see them via EventRequest semantics.
    }

    // Guards against sending VM_START twice per attach — the accept loop and
    // onHello can both reach it depending on connect ordering.
    private volatile boolean vmStartSent = false;

    private synchronized void sendVmStart() {
        if (vmStartSent || out == null) {
            return;
        }
        vmStartSent = true;
        try {
            Buf b = new Buf();
            b.writeByte(SP_NONE);
            b.writeInt(1);
            b.writeByte(EK_VM_START);
            b.writeInt(0);     // requestID 0 = auto-generated
            b.writeLong(1);    // dummy thread id; jdb tolerates this
            writeEventCommand(b.bytes());
        } catch (IOException e) {
            System.err.println("[jdwp] failed to send VM_START: " + e.getMessage());
        }
    }

    private boolean doHandshake() throws IOException {
        byte[] expected = HANDSHAKE.getBytes(StandardCharsets.US_ASCII);
        byte[] buf = new byte[expected.length];
        in.readFully(buf);
        for (int i = 0; i < expected.length; i++) {
            if (buf[i] != expected[i]) return false;
        }
        out.write(expected);
        out.flush();
        return true;
    }

    private void packetLoop() throws IOException {
        while (true) {
            int len;
            try {
                len = in.readInt();
            } catch (IOException eof) {
                System.out.println("[jdwp] debugger disconnected");
                return;
            }
            if (len < 11) throw new IOException("Malformed JDWP packet length=" + len);
            int id = in.readInt();
            int flags = in.readUnsignedByte();
            byte[] payload = new byte[len - 11];
            if ((flags & 0x80) != 0) {
                // Reply packet — we don't initiate commands TO jdb (yet), so ignore.
                int err = in.readUnsignedShort();
                in.readFully(payload);
                continue;
            }
            int cmdSet = in.readUnsignedByte();
            int cmd    = in.readUnsignedByte();
            in.readFully(payload);
            if (traceJdwp) {
                System.out.println("[jdwp-trace] CMD set=" + cmdSet + " cmd=" + cmd
                        + " id=" + id + " len=" + payload.length);
            }
            dispatchCommand(id, cmdSet, cmd, payload);
        }
    }

    /** Set true to dump every inbound JDWP command. Toggled by --trace-jdwp. */
    public static volatile boolean traceJdwp = false;

    // -------- Packet writers ------------------------------------------------

    private void writeReply(int id, int errorCode, byte[] data) throws IOException {
        synchronized (writeLock) {
            int len = 11 + (data == null ? 0 : data.length);
            out.writeInt(len);
            out.writeInt(id);
            out.writeByte(0x80);
            out.writeShort(errorCode);
            if (data != null && data.length > 0) out.write(data);
            out.flush();
        }
    }

    private void writeEventCommand(byte[] data) throws IOException {
        synchronized (writeLock) {
            // If the IDE has detached between us deciding to emit an event
            // and actually serializing it, out goes null. Swallow rather
            // than NPE'ing the listener thread.
            if (out == null) return;
            int len = 11 + data.length;
            out.writeInt(len);
            out.writeInt(nextRequestId.incrementAndGet());
            out.writeByte(0x00);
            out.writeByte(CS_EVENT);
            out.writeByte(100); // Composite
            out.write(data);
            out.flush();
        }
    }

    private static byte[] empty() { return new byte[0]; }

    // -------- Dispatch ------------------------------------------------------

    private static final boolean LOG_JDWP = Boolean.getBoolean("cn1.debug.logJdwp");

    private void dispatchCommand(int id, int cmdSet, int cmd, byte[] p) throws IOException {
        if (LOG_JDWP) System.out.println("[jdwp<-] cmdSet=" + cmdSet + " cmd=" + cmd + " len=" + p.length);
        try {
            switch (cmdSet) {
                case CS_VIRTUAL_MACHINE:  handleVM(id, cmd, p); return;
                case CS_REFERENCE_TYPE:   handleRefType(id, cmd, p); return;
                case CS_METHOD:           handleMethod(id, cmd, p); return;
                case CS_THREAD_REFERENCE: handleThread(id, cmd, p); return;
                case CS_THREAD_GROUP_REF: handleThreadGroup(id, cmd, p); return;
                case CS_STACK_FRAME:      handleStackFrame(id, cmd, p); return;
                case CS_EVENT_REQUEST:    handleEventRequest(id, cmd, p); return;
                case CS_OBJECT_REFERENCE: handleObject(id, cmd, p); return;
                case CS_CLASS_TYPE:       handleClassType(id, cmd, p); return;
                case CS_STRING_REFERENCE: handleString(id, cmd, p); return;
                case CS_ARRAY_REFERENCE:  handleArray(id, cmd, p); return;
                case CS_CLASS_LOADER_REF:
                case CS_CLASS_OBJECT_REF:
                    // Reply with NOT_IMPLEMENTED (100) so jdb falls back gracefully.
                    writeReply(id, 100, empty());
                    return;
                default:
                    writeReply(id, 100, empty());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            writeReply(id, 103, empty()); // INTERNAL
        }
    }

    // -------- VirtualMachine ------------------------------------------------

    private void handleVM(int id, int cmd, byte[] p) throws IOException {
        switch (cmd) {
            case 1: { // Version
                Buf b = new Buf();
                b.writeString("Codename One ParparVM on-device-debug proxy");
                b.writeInt(1); b.writeInt(8); // JDWP 1.8
                b.writeString("1.8");
                b.writeString("ParparVM");
                writeReply(id, 0, b.bytes());
                return;
            }
            case 2: { // ClassesBySignature
                String sig = readString(p, 0);
                Buf b = new Buf();
                SymbolTable.ClassInfo c = symbols.classByJvmSignature(sig);
                if (c != null) {
                    b.writeInt(1);
                    b.writeByte(TYPE_TAG_CLASS);
                    b.writeLong(toJdwpRef(c.classId));
                    b.writeInt(7); // VERIFIED|PREPARED|INITIALIZED
                } else {
                    b.writeInt(0);
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 3: { // AllClasses
                Buf b = new Buf();
                b.writeInt(symbols.allClasses().size());
                for (SymbolTable.ClassInfo c : symbols.allClasses()) {
                    b.writeByte(TYPE_TAG_CLASS);
                    b.writeLong(toJdwpRef(c.classId));
                    b.writeString(c.jvmSignature());
                    b.writeInt(7);
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 20: { // AllClassesWithGeneric — extra empty generic-sig per class
                Buf b = new Buf();
                b.writeInt(symbols.allClasses().size());
                for (SymbolTable.ClassInfo c : symbols.allClasses()) {
                    b.writeByte(TYPE_TAG_CLASS);
                    b.writeLong(toJdwpRef(c.classId));
                    b.writeString(c.jvmSignature());
                    b.writeString(""); // generic signature
                    b.writeInt(7);
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 4: { // AllThreads
                Buf b = new Buf();
                b.writeInt(knownThreads.size());
                for (long tid : knownThreads) b.writeLong(tid);
                writeReply(id, 0, b.bytes());
                return;
            }
            case 5: { // TopLevelThreadGroups
                Buf b = new Buf();
                b.writeInt(1);
                b.writeLong(FAKE_GROUP_ID);
                writeReply(id, 0, b.bytes());
                return;
            }
            case 6: { // Dispose
                if (device != null) try { device.dispose(); } catch (IOException ignore) {}
                writeReply(id, 0, empty());
                return;
            }
            case 7: { // IDSizes
                Buf b = new Buf();
                b.writeInt(8); b.writeInt(8); b.writeInt(8); b.writeInt(8); b.writeInt(8);
                writeReply(id, 0, b.bytes());
                return;
            }
            case 8: { // Suspend
                writeReply(id, 0, empty());
                return;
            }
            case 9: { // Resume
                System.out.println("[jdwp] VM.Resume");
                invalidateStack();
                invalidateLocals();
                if (device != null) try { device.resumeAll(); } catch (IOException ignore) {}
                writeReply(id, 0, empty());
                return;
            }
            case 10: { // Exit
                writeReply(id, 0, empty());
                return;
            }
            case 12: case 17: { // Capabilities / CapabilitiesNew — return all false
                Buf b = new Buf();
                int n = (cmd == 12) ? 7 : 32;
                for (int i = 0; i < n; i++) b.writeByte(0);
                writeReply(id, 0, b.bytes());
                return;
            }
            case 13: { // ClassPaths
                Buf b = new Buf();
                b.writeString("/");      // base dir
                b.writeInt(0); b.writeInt(0); // empty classpath/bootclasspath
                writeReply(id, 0, b.bytes());
                return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    // -------- ReferenceType -------------------------------------------------

    private void handleRefType(int id, int cmd, byte[] p) throws IOException {
        long typeID = readLong(p, 0);
        SymbolTable.ClassInfo c = symbols.classById(fromJdwpRef(typeID));
        switch (cmd) {
            case 1: { // Signature
                Buf b = new Buf();
                b.writeString(c != null ? c.jvmSignature() : "Ljava/lang/Object;");
                writeReply(id, 0, b.bytes());
                return;
            }
            case 2: { // ClassLoader
                Buf b = new Buf();
                b.writeLong(FAKE_CLASSLOADER_ID);
                writeReply(id, 0, b.bytes());
                return;
            }
            case 3: { // Modifiers
                Buf b = new Buf(); b.writeInt(0x0001); writeReply(id, 0, b.bytes()); return;
            }
            case 4: { // Fields
                Buf b = new Buf();
                if (c == null) { b.writeInt(0); writeReply(id, 0, b.bytes()); return; }
                // Only fields declared on this class, not inherited — JDWP
                // semantics. We filter from c.instanceFields (which may
                // include inherited entries due to the translator listing
                // them under the storing class for value-read efficiency).
                java.util.List<SymbolTable.FieldInfo> declared = new java.util.ArrayList<>();
                for (SymbolTable.FieldInfo fi : c.instanceFields) {
                    if (fi.classId == c.classId) declared.add(fi);
                }
                b.writeInt(declared.size());
                for (SymbolTable.FieldInfo fi : declared) {
                    b.writeLong(toJdwpRef(fi.fieldId));
                    b.writeString(fi.name);
                    b.writeString(fi.descriptor);
                    b.writeInt(fi.accessFlags);
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 14: { // FieldsWithGeneric
                Buf b = new Buf();
                if (c == null) { b.writeInt(0); writeReply(id, 0, b.bytes()); return; }
                java.util.List<SymbolTable.FieldInfo> declared = new java.util.ArrayList<>();
                for (SymbolTable.FieldInfo fi : c.instanceFields) {
                    if (fi.classId == c.classId) declared.add(fi);
                }
                b.writeInt(declared.size());
                for (SymbolTable.FieldInfo fi : declared) {
                    b.writeLong(toJdwpRef(fi.fieldId));
                    b.writeString(fi.name);
                    b.writeString(fi.descriptor);
                    b.writeString(""); // generic signature
                    b.writeInt(fi.accessFlags);
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 5: { // Methods
                Buf b = new Buf();
                if (c == null) { b.writeInt(0); writeReply(id, 0, b.bytes()); return; }
                b.writeInt(c.methods.size());
                for (SymbolTable.MethodInfo m : c.methods) {
                    b.writeLong(toJdwpRef(m.methodId));
                    b.writeString(m.name);
                    b.writeString(m.descriptor);
                    b.writeInt(jdwpMethodModifiers(m));
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 15: { // MethodsWithGeneric — same as Methods + per-method genericSig
                Buf b = new Buf();
                if (c == null) { b.writeInt(0); writeReply(id, 0, b.bytes()); return; }
                b.writeInt(c.methods.size());
                for (SymbolTable.MethodInfo m : c.methods) {
                    b.writeLong(toJdwpRef(m.methodId));
                    b.writeString(m.name);
                    b.writeString(m.descriptor);
                    b.writeString(""); // generic signature
                    b.writeInt(jdwpMethodModifiers(m));
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 7: { // SourceFile
                Buf b = new Buf();
                b.writeString(c != null && c.sourceFile != null && !c.sourceFile.isEmpty()
                        ? c.sourceFile : "Unknown.java");
                writeReply(id, 0, b.bytes());
                return;
            }
            case 9: { // Status
                Buf b = new Buf(); b.writeInt(7); writeReply(id, 0, b.bytes()); return;
            }
            case 10: { // Interfaces
                Buf b = new Buf(); b.writeInt(0); writeReply(id, 0, b.bytes()); return;
            }
            case 11: { // ClassObject
                Buf b = new Buf(); b.writeLong(typeID); writeReply(id, 0, b.bytes()); return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    // -------- Method --------------------------------------------------------

    private void handleMethod(int id, int cmd, byte[] p) throws IOException {
        long typeID = readLong(p, 0);
        long methodID = readLong(p, 8);
        SymbolTable.MethodInfo m = symbols.methodById(fromJdwpRef(methodID));
        switch (cmd) {
            case 1: { // LineTable
                Buf b = new Buf();
                if (m == null || m.lines.isEmpty()) {
                    b.writeLong(0); b.writeLong(0); b.writeInt(0);
                } else {
                    long start = m.lines.first();
                    long end = m.lines.last();
                    b.writeLong(start); b.writeLong(end); b.writeInt(m.lines.size());
                    for (int line : m.lines) {
                        b.writeLong(line); b.writeInt(line);
                    }
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 2: { // VariableTable
                Buf b = new Buf();
                if (m == null || m.locals.isEmpty()) {
                    b.writeInt(0); b.writeInt(0);
                } else {
                    // argSlots without isStatic distinction — we don't track
                    // modifiers in the sidecar yet, so assume instance (off
                    // by one for static methods is harmless to jdb's display).
                    b.writeInt(m.argSlots(false));
                    b.writeInt(m.locals.size());
                    for (SymbolTable.LocalVarInfo v : m.locals) {
                        // codeIndex=0, length=large => "always live"
                        b.writeLong(0L);
                        b.writeString(v.name);
                        b.writeString(v.descriptor);
                        b.writeInt(Integer.MAX_VALUE);
                        b.writeInt(v.slot);
                    }
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 5: { // VariableTableWithGeneric — adds per-var generic sig
                Buf b = new Buf();
                if (m == null || m.locals.isEmpty()) {
                    b.writeInt(0); b.writeInt(0);
                } else {
                    b.writeInt(m.argSlots(false));
                    b.writeInt(m.locals.size());
                    for (SymbolTable.LocalVarInfo v : m.locals) {
                        b.writeLong(0L);
                        b.writeString(v.name);
                        b.writeString(v.descriptor);
                        b.writeString(""); // generic signature
                        b.writeInt(Integer.MAX_VALUE);
                        b.writeInt(v.slot);
                    }
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 3: { // Bytecodes
                Buf b = new Buf(); b.writeInt(0);
                writeReply(id, 0, b.bytes());
                return;
            }
            case 4: { // IsObsolete
                Buf b = new Buf(); b.writeByte(0); writeReply(id, 0, b.bytes()); return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    private void handleClassType(int id, int cmd, byte[] p) throws IOException {
        switch (cmd) {
            case 1: { // Superclass
                long typeID = readLong(p, 0);
                SymbolTable.ClassInfo c = symbols.classById(fromJdwpRef(typeID));
                Buf b = new Buf();
                if (c != null && c.superId >= 0) {
                    b.writeLong(toJdwpRef(c.superId));
                } else {
                    b.writeLong(0); // null = java.lang.Object's super
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 3: { // InvokeMethod (static)
                // refType(8) thread(8) method(8) argCount(4) args[].
                // Each arg: tag(1) + value(tag-sized). options(4).
                /* refType */ long classRef = readLong(p, 0);
                long threadRef = readLong(p, 8);
                long methodRef = readLong(p, 16);
                int argCount = readInt(p, 24);
                int devMid = fromJdwpRef(methodRef);
                int off = 28;
                byte[] argTypes = new byte[argCount];
                long[] argValues = new long[argCount];
                for (int i = 0; i < argCount; i++) {
                    byte tag = p[off]; off += 1;
                    argTypes[i] = tag;
                    long v;
                    switch ((char) tag) {
                        case 'Z': case 'B': v = p[off] & 0xff; off += 1; break;
                        case 'S': case 'C': v = readShort(p, off) & 0xffff; off += 2; break;
                        case 'I': case 'F': v = readInt(p, off) & 0xffffffffL; off += 4; break;
                        case 'J': case 'D': v = readLong(p, off); off += 8; break;
                        default: // object refs etc.
                            v = readLong(p, off); off += 8; break;
                    }
                    argValues[i] = v;
                }
                int methodId = fromJdwpRef(methodRef);
                long threadId = threadRef;
                boolean ok = blockingInvoke(threadId, methodId, /*thisObj=static*/0L, argTypes, argValues);
                writeInvokeReply(id, ok);
                return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    /**
     * Packs the result of a CMD_INVOKE_METHOD round-trip into a JDWP
     * InvokeMethod reply (returnValue + exception object).
     */
    private void writeInvokeReply(int id, boolean ok) throws IOException {
        Buf b = new Buf();
        if (!ok) {
            // returnValue = void, exception = null
            b.writeByte('V');
            b.writeByte('L');
            b.writeLong(0);
            writeReply(id, 0, b.bytes());
            return;
        }
        byte t = lastInvokeType;
        long v = lastInvokeValue;
        boolean threw = t == 'X';
        if (threw) {
            // Return value is void in this case; exception object goes in
            // the second slot.
            b.writeByte('V');
            b.writeByte('L');
            b.writeLong(v);
        } else {
            b.writeByte(t == 0 ? 'V' : t);
            switch ((char) t) {
                case 'Z': b.writeByte((int) v & 1); break;
                case 'B': b.writeByte((int) v & 0xff); break;
                case 'S': case 'C': b.writeShort((int) v & 0xffff); break;
                case 'I': case 'F': b.writeInt((int) v); break;
                case 'J': case 'D': b.writeLong(v); break;
                case 'L': case '[': b.writeLong(v); break;
                case 'V': default: /* no value bytes for void */ break;
            }
            // No exception.
            b.writeByte('L');
            b.writeLong(0);
        }
        writeReply(id, 0, b.bytes());
    }

    // -------- Thread / ThreadGroup -----------------------------------------

    private void handleThread(int id, int cmd, byte[] p) throws IOException {
        long tid = readLong(p, 0);
        switch (cmd) {
            case 1: { // Name
                Buf b = new Buf(); b.writeString("Thread-" + tid); writeReply(id, 0, b.bytes()); return;
            }
            case 2: { // Suspend
                writeReply(id, 0, empty()); return;
            }
            case 3: { // Resume
                System.out.println("[jdwp] Thread.Resume tid=" + tid);
                invalidateStack();
                invalidateLocals();
                if (device != null) try { device.resume(tid); } catch (IOException ignore) {}
                writeReply(id, 0, empty()); return;
            }
            case 4: { // Status
                // 1 = SLEEPING, 4 = RUNNING; suspendStatus bit 1 = SUSPENDED
                Buf b = new Buf();
                b.writeInt(4); // RUNNING
                b.writeInt(tid == lastSuspendedThread ? 1 : 0);
                writeReply(id, 0, b.bytes()); return;
            }
            case 5: { // ThreadGroup
                Buf b = new Buf(); b.writeLong(FAKE_GROUP_ID); writeReply(id, 0, b.bytes()); return;
            }
            case 6: { // Frames
                int startFrame = readInt(p, 8);
                int length = readInt(p, 12);
                fetchStackForThread(tid);
                Buf b = new Buf();
                int[] mids = lastStackMids;
                int[] lines = lastStackLines;
                int total = mids == null ? 0 : mids.length;
                int count = total - startFrame;
                if (length >= 0 && length < count) count = length;
                if (count < 0) count = 0;
                b.writeInt(count);
                for (int i = 0; i < count; i++) {
                    int idx = startFrame + i;
                    long frameId = ((tid & 0xFFFFFFFFL) << 32) | (idx & 0xFFFFFFFFL);
                    b.writeLong(frameId);
                    // Location: typeTag, classID, methodID, codeIndex (in JDWP space)
                    SymbolTable.MethodInfo m = symbols.methodById(mids[idx]);
                    int classId = m != null ? m.classId : 0;
                    b.writeByte(TYPE_TAG_CLASS);
                    b.writeLong(toJdwpRef(classId));
                    b.writeLong(toJdwpRef(mids[idx]));
                    b.writeLong(lines[idx]);
                }
                writeReply(id, 0, b.bytes()); return;
            }
            case 7: { // FrameCount
                fetchStackForThread(tid);
                Buf b = new Buf(); b.writeInt(lastStackMids == null ? 0 : lastStackMids.length);
                writeReply(id, 0, b.bytes()); return;
            }
            case 12: { // SuspendCount
                Buf b = new Buf();
                b.writeInt(tid == lastSuspendedThread ? 1 : 0);
                writeReply(id, 0, b.bytes()); return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    private void handleThreadGroup(int id, int cmd, byte[] p) throws IOException {
        switch (cmd) {
            case 1: { Buf b = new Buf(); b.writeString("main"); writeReply(id, 0, b.bytes()); return; }
            case 2: { Buf b = new Buf(); b.writeLong(0); writeReply(id, 0, b.bytes()); return; }
            case 3: {
                Buf b = new Buf();
                b.writeInt(knownThreads.size());
                for (long t : knownThreads) b.writeLong(t);
                b.writeInt(0);
                writeReply(id, 0, b.bytes()); return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    // -------- StackFrame ----------------------------------------------------

    private void handleStackFrame(int id, int cmd, byte[] p) throws IOException {
        long tid = readLong(p, 0);
        long frameId = readLong(p, 8);
        int frameIdx = (int)(frameId & 0xFFFFFFFFL);
        switch (cmd) {
            case 1: { // GetValues
                int slotCount = readInt(p, 16);
                // Read the requested slots into a list.
                List<int[]> requested = new ArrayList<>(); // each: { slot, sigByte }
                int off = 20;
                for (int i = 0; i < slotCount; i++) {
                    int slot = readInt(p, off); off += 4;
                    int sig = p[off] & 0xff; off += 1;
                    requested.add(new int[]{ slot, sig });
                }
                // Synchronously ask the device for locals at frameIdx.
                int[] devSlots; byte[] devTypes; long[] devValues;
                synchronized (localsLock) {
                    pendingLocals = true;
                    lastLocalsSlots = null; lastLocalsTypes = null; lastLocalsValues = null;
                    try { device.getLocals(tid, frameIdx); } catch (IOException io) {
                        writeReply(id, 103, empty()); return;
                    }
                    long deadline = System.currentTimeMillis() + 2000;
                    while (pendingLocals && System.currentTimeMillis() < deadline) {
                        try { localsLock.wait(deadline - System.currentTimeMillis()); } catch (InterruptedException ie) { break; }
                    }
                    devSlots = lastLocalsSlots; devTypes = lastLocalsTypes; devValues = lastLocalsValues;
                }
                Buf b = new Buf();
                b.writeInt(slotCount);
                for (int[] r : requested) {
                    int slot = r[0];
                    int idx = findSlot(devSlots, slot);
                    if (idx < 0) {
                        b.writeByte('L'); b.writeLong(0);
                    } else {
                        byte tc = devTypes[idx];
                        long v = devValues[idx];
                        b.writeByte(tc);
                        switch ((char) tc) {
                            case 'Z': b.writeByte((int)v & 1); break;
                            case 'B': b.writeByte((int)v & 0xff); break;
                            case 'S': case 'C': b.writeShort((int)v & 0xffff); break;
                            case 'I': case 'F': b.writeInt((int)v); break;
                            case 'J': case 'D': b.writeLong(v); break;
                            case 'L': case '[': b.writeLong(v); break;
                            default: b.writeLong(v);
                        }
                    }
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 3: { // ThisObject
                // JDWP "ThisObject" returns the receiver of the frame's
                // method, or null for statics. We don't track static-ness
                // explicitly, so we ask the device for slot 0 of this frame:
                // for instance methods the JVM places `this` there on entry,
                // for statics it stays 0/null which is the correct JDWP reply.
                int[] devSlots; byte[] devTypes; long[] devValues;
                synchronized (localsLock) {
                    pendingLocals = true;
                    lastLocalsSlots = null; lastLocalsTypes = null; lastLocalsValues = null;
                    try { device.getLocals(tid, frameIdx); }
                    catch (IOException io) {
                        Buf b = new Buf(); b.writeByte('L'); b.writeLong(0); writeReply(id, 0, b.bytes()); return;
                    }
                    long deadline = System.currentTimeMillis() + 2000;
                    while (pendingLocals && System.currentTimeMillis() < deadline) {
                        try { localsLock.wait(deadline - System.currentTimeMillis()); }
                        catch (InterruptedException ie) { break; }
                    }
                    devSlots = lastLocalsSlots; devTypes = lastLocalsTypes; devValues = lastLocalsValues;
                }
                Buf b = new Buf();
                b.writeByte('L');
                long ref = 0;
                if (devSlots != null && devTypes != null && devValues != null) {
                    int i = findSlot(devSlots, 0);
                    if (i >= 0 && ((char) devTypes[i] == 'L' || (char) devTypes[i] == '[')) {
                        ref = devValues[i];
                    }
                }
                b.writeLong(ref);
                writeReply(id, 0, b.bytes());
                return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    private int findSlot(int[] slots, int target) {
        if (slots == null) return -1;
        for (int i = 0; i < slots.length; i++) if (slots[i] == target) return i;
        return -1;
    }

    // -------- EventRequest --------------------------------------------------

    private void handleEventRequest(int id, int cmd, byte[] p) throws IOException {
        switch (cmd) {
            case 1: { // Set
                int eventKind = p[0] & 0xff;
                int suspendPolicy = p[1] & 0xff;
                int modCount = readInt(p, 2);
                int off = 6;
                long typeID = 0, methodID = 0, codeIndex = 0;
                long stepThread = 0;
                int stepDepth = 0;
                boolean badModifier = false;
                for (int i = 0; i < modCount && !badModifier; i++) {
                    if (off >= p.length) { badModifier = true; break; }
                    int modKind = p[off] & 0xff; off += 1;
                    // JDWP modifier-kind numbering per the spec:
                    //   1 Count, 2 Conditional (deprecated),
                    //   3 ThreadOnly, 4 ClassOnly, 5 ClassMatch,
                    //   6 ClassExclude, 7 LocationOnly, 8 ExceptionOnly,
                    //   9 FieldOnly, 10 Step, 11 InstanceOnly,
                    //   12 SourceNameMatch.
                    // IntelliJ leans heavily on 6 (ClassExclude) — it
                    // auto-attaches java.*, javax.*, sun.*, etc. to every
                    // step request, so a parser that drops modKind=6
                    // aborts every IntelliJ step.
                    switch (modKind) {
                        case 1: { // Count
                            if (off + 4 > p.length) { badModifier = true; break; }
                            off += 4; break;
                        }
                        case 2: { // ConditionalExpression (deprecated) — exprID
                            if (off + 4 > p.length) { badModifier = true; break; }
                            off += 4; break;
                        }
                        case 3: { // ThreadOnly (objectID)
                            if (off + 8 > p.length) { badModifier = true; break; }
                            off += 8; break;
                        }
                        case 4: { // ClassOnly (refTypeID)
                            if (off + 8 > p.length) { badModifier = true; break; }
                            off += 8; break;
                        }
                        case 5: case 6: { // ClassMatch, ClassExclude (string)
                            if (off + 4 > p.length) { badModifier = true; break; }
                            int slen = readInt(p, off);
                            if (slen < 0 || off + 4 + slen > p.length) { badModifier = true; break; }
                            off += 4 + slen;
                            break;
                        }
                        case 7: { // LocationOnly: typeTag(1) + classID(8) + methodID(8) + codeIndex(8) = 25
                            if (off + 25 > p.length) { badModifier = true; break; }
                            /* typeTag */ off += 1;
                            typeID = readLong(p, off); off += 8;
                            methodID = readLong(p, off); off += 8;
                            codeIndex = readLong(p, off); off += 8;
                            break;
                        }
                        case 8: { // ExceptionOnly: refTypeID + caught(byte) + uncaught(byte)
                            if (off + 10 > p.length) { badModifier = true; break; }
                            off += 10; break;
                        }
                        case 9: { // FieldOnly: refTypeID + fieldID
                            if (off + 16 > p.length) { badModifier = true; break; }
                            off += 16; break;
                        }
                        case 10: { // Step: threadID(8) + size(4) + depth(4) = 16
                            if (off + 16 > p.length) { badModifier = true; break; }
                            stepThread = readLong(p, off); off += 8;
                            /* size */ off += 4;
                            stepDepth = readInt(p, off); off += 4;
                            break;
                        }
                        case 11: { // InstanceOnly (objectID)
                            if (off + 8 > p.length) { badModifier = true; break; }
                            off += 8; break;
                        }
                        case 12: { // SourceNameMatch (string) — JDWP 1.6+
                            if (off + 4 > p.length) { badModifier = true; break; }
                            int slen = readInt(p, off);
                            if (slen < 0 || off + 4 + slen > p.length) { badModifier = true; break; }
                            off += 4 + slen;
                            break;
                        }
                        default:
                            // Unknown modifier — bail rather than guess its
                            // width and walk the read pointer past the buffer.
                            System.out.println("[jdwp] EventRequest.Set: unknown modKind=" + modKind
                                    + " — ignoring remaining " + (modCount - i - 1) + " modifiers");
                            badModifier = true;
                            break;
                    }
                }
                int rid = nextRequestId.incrementAndGet();
                if (eventKind == EK_BREAKPOINT) {
                    // typeID / methodID arrive in JDWP-space; convert back.
                    int classIdInt = fromJdwpRef(typeID);
                    int methodIdInt = fromJdwpRef(methodID);
                    BpRequest br = new BpRequest(rid, classIdInt, methodIdInt, codeIndex, suspendPolicy);
                    bpRequests.put(rid, br);
                    // If the device isn't connected yet, the breakpoint will
                    // be replayed when onHello fires. Either way bpRequests
                    // is the source of truth so the event handler can match
                    // device-emitted BP_HIT events back to a JDWP request id.
                    if (device != null && deviceHelloReceived) try {
                        device.setBreakpoint(methodIdInt, (int) codeIndex);
                    } catch (IOException io) { /* ignore */ }
                } else if (eventKind == EK_SINGLE_STEP && stepThread != 0) {
                    // depth: 0=INTO, 1=OVER, 2=OUT — same numeric values as
                    // the wire protocol's STEP_INTO/OVER/OUT, so no mapping.
                    stepRequests.put(stepThread, rid);
                    System.out.println("[jdwp] STEP request tid=" + stepThread
                            + " depth=" + stepDepth
                            + " (0=INTO 1=OVER 2=OUT) rid=" + rid);
                    if (device != null && deviceHelloReceived) try {
                        device.step(stepThread, stepDepth);
                    } catch (IOException io) { /* ignore */ }
                }
                Buf b = new Buf(); b.writeInt(rid); writeReply(id, 0, b.bytes());
                return;
            }
            case 2: { // Clear
                int eventKind = p[0] & 0xff;
                int rid = readInt(p, 1);
                BpRequest br = bpRequests.remove(rid);
                if (br != null && device != null && eventKind == EK_BREAKPOINT) {
                    try { device.clearBreakpoint((int) br.methodID, (int) br.codeIndex); } catch (IOException ignore) {}
                }
                if (eventKind == EK_SINGLE_STEP) {
                    stepRequests.entrySet().removeIf(e -> e.getValue() == rid);
                }
                writeReply(id, 0, empty());
                return;
            }
            case 3: { // ClearAllBreakpoints
                for (BpRequest br : bpRequests.values()) {
                    if (device != null) try {
                        device.clearBreakpoint((int) br.methodID, (int) br.codeIndex);
                    } catch (IOException ignore) {}
                }
                bpRequests.clear();
                writeReply(id, 0, empty());
                return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    private void handleObject(int id, int cmd, byte[] p) throws IOException {
        long objectId = readLong(p, 0);
        switch (cmd) {
            case 1: { // ReferenceType — get class
                int classId = blockingGetObjectClass(objectId);
                boolean isArr;
                synchronized (objectClassLock) { isArr = lastObjectIsArray; }
                Buf b = new Buf();
                if (classId < 0) {
                    b.writeByte(TYPE_TAG_CLASS);
                    b.writeLong(0);
                } else {
                    b.writeByte(isArr ? TYPE_TAG_ARRAY : TYPE_TAG_CLASS);
                    b.writeLong(toJdwpRef(classId));
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 2: { // GetValues — instance field reads
                int count = readInt(p, 8);
                int[] fieldIds = new int[count];
                for (int i = 0; i < count; i++) {
                    long jdwpFid = readLong(p, 12 + i * 8);
                    fieldIds[i] = fromJdwpRef(jdwpFid);
                }
                Buf b = new Buf();
                b.writeInt(count);
                if (count == 0) { writeReply(id, 0, b.bytes()); return; }
                boolean ok = blockingGetObjectFields(objectId, fieldIds);
                byte[] types = lastObjectFieldsTypes;
                long[] values = lastObjectFieldsValues;
                if (!ok || types == null || values == null || types.length != count) {
                    // Fall back to writing nulls for each requested field so jdb
                    // still gets a well-formed reply.
                    for (int i = 0; i < count; i++) { b.writeByte('L'); b.writeLong(0); }
                    writeReply(id, 0, b.bytes());
                    return;
                }
                for (int i = 0; i < count; i++) {
                    byte tc = types[i];
                    long v = values[i];
                    b.writeByte(tc);
                    switch ((char) tc) {
                        case 'Z': b.writeByte((int) v & 1); break;
                        case 'B': b.writeByte((int) v & 0xff); break;
                        case 'S': case 'C': b.writeShort((int) v & 0xffff); break;
                        case 'I': case 'F': b.writeInt((int) v); break;
                        case 'J': case 'D': b.writeLong(v); break;
                        case 'L': case '[': b.writeLong(v); break;
                        default: b.writeLong(v);
                    }
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            case 6: { // InvokeMethod (instance)
                // objId(8) thread(8) refType(8) method(8) argCount(4) args[] options(4)
                long thisObj = objectId;
                long threadRef = readLong(p, 8);
                /* refType */ readLong(p, 16);
                long methodRef = readLong(p, 24);
                int argCount = readInt(p, 32);
                int devMid = fromJdwpRef(methodRef);
                int off = 36;
                byte[] argTypes = new byte[argCount];
                long[] argValues = new long[argCount];
                for (int i = 0; i < argCount; i++) {
                    byte tag = p[off]; off += 1;
                    argTypes[i] = tag;
                    long v;
                    switch ((char) tag) {
                        case 'Z': case 'B': v = p[off] & 0xff; off += 1; break;
                        case 'S': case 'C': v = readShort(p, off) & 0xffff; off += 2; break;
                        case 'I': case 'F': v = readInt(p, off) & 0xffffffffL; off += 4; break;
                        case 'J': case 'D': v = readLong(p, off); off += 8; break;
                        default: v = readLong(p, off); off += 8; break;
                    }
                    argValues[i] = v;
                }
                int methodId = fromJdwpRef(methodRef);
                long threadId = threadRef;
                boolean ok = blockingInvoke(threadId, methodId, thisObj, argTypes, argValues);
                writeInvokeReply(id, ok);
                return;
            }
            case 9: { // IsCollected — we don't track GC; always false.
                Buf b = new Buf(); b.writeByte(0); writeReply(id, 0, b.bytes()); return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    private void handleString(int id, int cmd, byte[] p) throws IOException {
        long objectId = readLong(p, 0);
        switch (cmd) {
            case 1: { // Value
                String s = blockingGetString(objectId);
                Buf b = new Buf();
                b.writeString(s == null ? "" : s);
                writeReply(id, 0, b.bytes());
                return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    private void handleArray(int id, int cmd, byte[] p) throws IOException {
        long objectId = readLong(p, 0);
        switch (cmd) {
            case 1: { // Length
                int length = blockingGetArrayLength(objectId);
                Buf b = new Buf();
                b.writeInt(length < 0 ? 0 : length);
                writeReply(id, 0, b.bytes());
                return;
            }
            case 2: { // GetValues
                int first = readInt(p, 8);
                int count = readInt(p, 12);
                if (!blockingGetArrayValues(objectId, first, count)) {
                    // ArrayRegion: tag byte 'L', count 0 — well-formed empty.
                    Buf b = new Buf();
                    b.writeByte('L');
                    b.writeInt(0);
                    writeReply(id, 0, b.bytes());
                    return;
                }
                byte tag = lastArrayTag;
                int n = lastArrayCount;
                byte[] raw = lastArrayBytes != null ? lastArrayBytes : new byte[0];
                Buf b = new Buf();
                b.writeByte(tag & 0xff);
                b.writeInt(n);
                // JDWP ArrayRegion encoding: primitive arrays carry the
                // packed primitive values directly; object arrays carry a
                // tagged value (1-byte tag + objectID) per element.
                if (tag == 'L' || tag == '[') {
                    // Each element on the wire is 8 bytes (object reference).
                    int per = 8;
                    int off = 0;
                    for (int i = 0; i < n; i++) {
                        b.writeByte('L');
                        // raw bytes are already big-endian
                        for (int k = 0; k < per; k++) {
                            b.writeByte(raw[off + k] & 0xff);
                        }
                        off += per;
                    }
                } else {
                    // Primitive: just append the raw bytes — no per-element
                    // tag, just count * width.
                    for (byte by : raw) b.writeByte(by & 0xff);
                }
                writeReply(id, 0, b.bytes());
                return;
            }
            default:
                writeReply(id, 100, empty());
        }
    }

    // -------- Synchronous request/reply against the device ----------------

    private final Object arrayLock = new Object();
    private boolean pendingArrayLength = false;
    private int lastArrayLength = 0;
    private boolean pendingArrayValues = false;
    private byte lastArrayTag = 'L';
    private int lastArrayCount = 0;
    private byte[] lastArrayBytes = null;

    private int blockingGetArrayLength(long objectId) {
        if (device == null) return -1;
        synchronized (arrayLock) {
            pendingArrayLength = true;
            lastArrayLength = 0;
            try { device.getArrayLength(objectId); }
            catch (IOException io) { pendingArrayLength = false; return -1; }
            long deadline = System.currentTimeMillis() + 2000;
            while (pendingArrayLength && System.currentTimeMillis() < deadline) {
                try { arrayLock.wait(deadline - System.currentTimeMillis()); }
                catch (InterruptedException ie) { break; }
            }
            return lastArrayLength;
        }
    }

    private boolean blockingGetArrayValues(long objectId, int firstIndex, int count) {
        if (device == null) return false;
        synchronized (arrayLock) {
            pendingArrayValues = true;
            lastArrayBytes = null;
            try { device.getArrayValues(objectId, firstIndex, count); }
            catch (IOException io) { pendingArrayValues = false; return false; }
            long deadline = System.currentTimeMillis() + 2000;
            while (pendingArrayValues && System.currentTimeMillis() < deadline) {
                try { arrayLock.wait(deadline - System.currentTimeMillis()); }
                catch (InterruptedException ie) { break; }
            }
            return lastArrayBytes != null;
        }
    }

    private final Object objectClassLock = new Object();
    private boolean pendingObjectClass = false;
    private int lastObjectClass = -1;
    private boolean lastObjectIsArray = false;

    private int blockingGetObjectClass(long objectId) {
        if (device == null) return -1;
        synchronized (objectClassLock) {
            pendingObjectClass = true;
            lastObjectClass = -1;
            lastObjectIsArray = false;
            try { device.getObjectClass(objectId); } catch (IOException io) { return -1; }
            long deadline = System.currentTimeMillis() + 2000;
            while (pendingObjectClass && System.currentTimeMillis() < deadline) {
                try { objectClassLock.wait(deadline - System.currentTimeMillis()); }
                catch (InterruptedException ie) { break; }
            }
            return lastObjectClass;
        }
    }

    private final Object invokeLock = new Object();
    private boolean pendingInvoke = false;
    private byte lastInvokeType = 'V';
    private long lastInvokeValue = 0;

    /**
     * Sends an InvokeMethod request to the device and blocks until the
     * suspended thread runs the thunk and returns a result. Returns false
     * on timeout — the proxy can't tell the IDE much in that case beyond
     * an INTERNAL error.
     */
    private boolean blockingInvoke(long threadId, int methodId, long thisObj,
                                   byte[] argTypes, long[] argValues) {
        if (device == null) return false;
        synchronized (invokeLock) {
            pendingInvoke = true;
            lastInvokeType = 'V';
            lastInvokeValue = 0;
            try { device.invokeMethod(threadId, methodId, thisObj, argTypes, argValues); }
            catch (IOException io) { pendingInvoke = false; return false; }
            // Bigger budget than the field-read path because the call can
            // actually run user code that does anything.
            long deadline = System.currentTimeMillis() + 10000;
            while (pendingInvoke && System.currentTimeMillis() < deadline) {
                try { invokeLock.wait(deadline - System.currentTimeMillis()); }
                catch (InterruptedException ie) { break; }
            }
            return !pendingInvoke;
        }
    }

    private final Object objectFieldsLock = new Object();
    private boolean pendingObjectFields = false;
    private byte[] lastObjectFieldsTypes = null;
    private long[] lastObjectFieldsValues = null;

    /**
     * Blocking call to the device's CMD_GET_OBJECT_FIELDS. Returns a parallel
     * pair of arrays (type-code, raw value) or null on timeout. The proxy
     * orders the request fields the way the IDE asked, so the response
     * preserves the same ordering — no field-id round-trip needed here.
     */
    private boolean blockingGetObjectFields(long objectId, int[] fieldIds) {
        if (device == null) return false;
        synchronized (objectFieldsLock) {
            pendingObjectFields = true;
            lastObjectFieldsTypes = null;
            lastObjectFieldsValues = null;
            try { device.getObjectFields(objectId, fieldIds); }
            catch (IOException io) { return false; }
            long deadline = System.currentTimeMillis() + 2000;
            while (pendingObjectFields && System.currentTimeMillis() < deadline) {
                try { objectFieldsLock.wait(deadline - System.currentTimeMillis()); }
                catch (InterruptedException ie) { break; }
            }
            return lastObjectFieldsTypes != null;
        }
    }

    private final Object stringLock = new Object();
    private boolean pendingString = false;
    private String lastString = null;

    private String blockingGetString(long objectId) {
        if (device == null) return null;
        synchronized (stringLock) {
            pendingString = true;
            lastString = null;
            try { device.getString(objectId); } catch (IOException io) { return null; }
            long deadline = System.currentTimeMillis() + 2000;
            while (pendingString && System.currentTimeMillis() < deadline) {
                try { stringLock.wait(deadline - System.currentTimeMillis()); }
                catch (InterruptedException ie) { break; }
            }
            return lastString;
        }
    }

    // -------- DeviceListener (events from device) --------------------------

    private final Object localsLock = new Object();
    private boolean pendingLocals = false;
    private int[] lastLocalsSlots;
    private byte[] lastLocalsTypes;
    private long[] lastLocalsValues;

    private final Object stackLock = new Object();
    private volatile long stackThreadId = -1;
    private volatile boolean pendingStack = false;
    private volatile int[] lastStackMids;
    private volatile int[] lastStackLines;

    private void fetchStackForThread(long tid) {
        if (device == null) return;
        synchronized (stackLock) {
            // Already cached for this thread? Reuse without round-tripping.
            if (stackThreadId == tid && lastStackMids != null && !pendingStack) {
                return;
            }
            pendingStack = true;
            stackThreadId = tid;
            lastStackMids = null;
            lastStackLines = null;
            try { device.getStack(tid); } catch (IOException io) { pendingStack = false; return; }
            long deadline = System.currentTimeMillis() + 2000;
            while (pendingStack && System.currentTimeMillis() < deadline) {
                try { stackLock.wait(deadline - System.currentTimeMillis()); }
                catch (InterruptedException ie) { break; }
            }
        }
    }

    @Override public void onHello(int version) {
        System.out.println("[jdwp] device handshake (proto v" + version + ")");
        deviceHelloReceived = true;
        if (out != null) {
            sendVmStart();
        }
        // Replay any breakpoints that were registered before the device
        // joined — common when jdb's startup script issues "stop at" before
        // the iOS app finishes booting.
        if (device != null) {
            for (BpRequest br : bpRequests.values()) {
                try { device.setBreakpoint((int) br.methodID, (int) br.codeIndex); }
                catch (IOException ignore) {}
            }
        }
    }

    @Override public void onBreakpointHit(long threadId, int methodId, int line) {
        System.out.println("[jdwp] BP_HIT tid=" + threadId + " methodId=" + methodId + " line=" + line);
        if (!knownThreads.contains(threadId)) knownThreads.add(threadId);
        lastSuspendedThread = threadId;
        // The thread just paused at a new location — drop any stale cache
        // from a previous suspension and re-fetch.
        invalidateStack();
        invalidateLocals();
        // Eagerly ask the device for stack so the IDE's subsequent Frames
        // query returns something useful.
        if (device != null) try { device.getStack(threadId); } catch (IOException ignore) {}
        // Find matching JDWP request id; if none we still send a generic
        // event so jdb sees the suspend.
        int rid = 0;
        for (BpRequest br : bpRequests.values()) {
            if (br.methodID == methodId && br.codeIndex == line) { rid = br.requestId; break; }
        }
        try {
            SymbolTable.MethodInfo m = symbols.methodById(methodId);
            int classId = m != null ? m.classId : 0;
            Buf b = new Buf();
            b.writeByte(SP_ALL);
            b.writeInt(1);
            b.writeByte(EK_BREAKPOINT);
            b.writeInt(rid);
            b.writeLong(threadId);
            b.writeByte(TYPE_TAG_CLASS);
            b.writeLong(toJdwpRef(classId));
            b.writeLong(toJdwpRef(methodId));
            b.writeLong(line);
            writeEventCommand(b.bytes());
        } catch (IOException e) {
            System.err.println("[jdwp] failed to send breakpoint event: " + e.getMessage());
        }
    }

    @Override public void onStepComplete(long threadId, int methodId, int line) {
        Integer rid = stepRequests.remove(threadId);
        // Drop any cached stack from before the step landed. Without this
        // the IDE's Frames request after STEP_COMPLETE hits the cache and
        // re-uses the BP_HIT stack — Frames panel sticks on the previous
        // line, double-click jumps to the previous line, evaluation runs
        // against a dead frame.
        invalidateStack();
        invalidateLocals();
        System.out.println("[jdwp] STEP_COMPLETE tid=" + threadId + " methodId=" + methodId + " line=" + line + " rid=" + rid);
        try {
            SymbolTable.MethodInfo m = symbols.methodById(methodId);
            int classId = m != null ? m.classId : 0;
            Buf b = new Buf();
            b.writeByte(SP_ALL);
            b.writeInt(1);
            b.writeByte(EK_SINGLE_STEP);
            b.writeInt(rid == null ? 0 : rid);
            b.writeLong(threadId);
            b.writeByte(TYPE_TAG_CLASS);
            b.writeLong(toJdwpRef(classId));
            b.writeLong(toJdwpRef(methodId));
            b.writeLong(line);
            writeEventCommand(b.bytes());
        } catch (IOException e) {
            System.err.println("[jdwp] failed to send step event: " + e.getMessage());
        }
    }

    @Override public void onStack(long threadId, int[] methodIds, int[] lines) {
        synchronized (stackLock) {
            lastStackMids = methodIds;
            lastStackLines = lines;
            stackThreadId = threadId;
            pendingStack = false;
            stackLock.notifyAll();
        }
    }

    /**
     * Drop the cached stack so the next fetchStackForThread round-trips
     * to the device. Called whenever the thread state changes under us:
     * a new suspend (BP_HIT / STEP_COMPLETE) or a resume (VM.Resume,
     * Thread.Resume).
     */
    private void invalidateStack() {
        synchronized (stackLock) {
            lastStackMids = null;
            lastStackLines = null;
            stackThreadId = -1;
        }
    }

    /** Drop the cached locals — same lifecycle as invalidateStack. */
    private void invalidateLocals() {
        synchronized (localsLock) {
            lastLocalsSlots = null;
            lastLocalsTypes = null;
            lastLocalsValues = null;
        }
    }

    @Override public void onLocals(int[] slots, byte[] typeCodes, long[] values) {
        synchronized (localsLock) {
            lastLocalsSlots = slots;
            lastLocalsTypes = typeCodes;
            lastLocalsValues = values;
            pendingLocals = false;
            localsLock.notifyAll();
        }
    }

    @Override public void onVmDeath() {
        try {
            Buf b = new Buf();
            b.writeByte(SP_NONE);
            b.writeInt(1);
            b.writeByte(EK_VM_DEATH);
            b.writeInt(0);
            writeEventCommand(b.bytes());
        } catch (IOException ignore) {}
    }

    @Override public void onStringValue(String value) {
        synchronized (stringLock) {
            lastString = value;
            pendingString = false;
            stringLock.notifyAll();
        }
    }
    @Override public void onObjectClass(int classId, boolean isArray) {
        synchronized (objectClassLock) {
            lastObjectClass = classId;
            lastObjectIsArray = isArray;
            pendingObjectClass = false;
            objectClassLock.notifyAll();
        }
    }
    @Override public void onObjectFields(byte[] typeCodes, long[] values) {
        synchronized (objectFieldsLock) {
            lastObjectFieldsTypes = typeCodes;
            lastObjectFieldsValues = values;
            pendingObjectFields = false;
            objectFieldsLock.notifyAll();
        }
    }
    @Override public void onInvokeResult(byte type, long value) {
        synchronized (invokeLock) {
            lastInvokeType = type;
            lastInvokeValue = value;
            pendingInvoke = false;
            invokeLock.notifyAll();
        }
    }
    @Override public void onArrayLength(int length) {
        synchronized (arrayLock) {
            lastArrayLength = length;
            pendingArrayLength = false;
            arrayLock.notifyAll();
        }
    }
    @Override public void onArrayValues(byte tag, int count, byte[] rawBytes) {
        synchronized (arrayLock) {
            lastArrayTag = tag;
            lastArrayCount = count;
            lastArrayBytes = rawBytes;
            pendingArrayValues = false;
            arrayLock.notifyAll();
        }
    }
    @Override public void onReplyStatus() {}
    @Override public void onStdoutLine(String line) {
        // Surface device prints in whatever console the proxy is running in
        // (a terminal during local dev, or the IDE's "Run" console when the
        // proxy is launched as an IDE run configuration). The prefix keeps
        // proxy-internal noise distinguishable from app output.
        System.out.println("[device] " + line);
    }
    @Override public void onStderrLine(String line) {
        System.err.println("[device] " + line);
    }
    @Override public void onUnknownEvent(int code, byte[] payload) {
        System.out.println("[jdwp] unknown device event code 0x" + Integer.toHexString(code));
    }
    @Override public void onDisconnected() {
        System.out.println("[jdwp] device disconnected");
        onVmDeath();
    }

    // -------- buffer helpers -----------------------------------------------

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xff) << 24) | ((b[off+1] & 0xff) << 16)
             | ((b[off+2] & 0xff) << 8) | (b[off+3] & 0xff);
    }
    private static short readShort(byte[] b, int off) {
        return (short)(((b[off] & 0xff) << 8) | (b[off+1] & 0xff));
    }
    private static long readLong(byte[] b, int off) {
        return ((long)readInt(b, off) << 32) | (readInt(b, off + 4) & 0xffffffffL);
    }
    private static String readString(byte[] b, int off) {
        int len = readInt(b, off);
        return new String(b, off + 4, len, StandardCharsets.UTF_8);
    }
    private static int skipString(byte[] b, int off) {
        int len = readInt(b, off);
        return off + 4 + len;
    }

    /** Tiny growable byte buffer with JDWP-flavoured writers. */
    private static final class Buf {
        private byte[] arr = new byte[64];
        private int n = 0;
        private void ensure(int more) {
            if (n + more > arr.length) {
                int cap = arr.length;
                while (cap < n + more) cap *= 2;
                byte[] na = new byte[cap];
                System.arraycopy(arr, 0, na, 0, n);
                arr = na;
            }
        }
        void writeByte(int v) { ensure(1); arr[n++] = (byte)v; }
        void writeShort(int v) { ensure(2); arr[n++] = (byte)(v>>>8); arr[n++] = (byte)v; }
        void writeInt(int v) { ensure(4); arr[n++]=(byte)(v>>>24); arr[n++]=(byte)(v>>>16); arr[n++]=(byte)(v>>>8); arr[n++]=(byte)v; }
        void writeLong(long v) { writeInt((int)(v>>>32)); writeInt((int)v); }
        void writeString(String s) {
            byte[] u = s.getBytes(StandardCharsets.UTF_8);
            writeInt(u.length);
            ensure(u.length);
            System.arraycopy(u, 0, arr, n, u.length);
            n += u.length;
        }
        byte[] bytes() { byte[] o = new byte[n]; System.arraycopy(arr, 0, o, 0, n); return o; }
    }
}
