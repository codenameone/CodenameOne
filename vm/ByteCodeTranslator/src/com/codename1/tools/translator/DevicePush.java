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
package com.codename1.tools.translator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

/**
 * Sends a compiled project to a device runtime and waits for the result.
 *
 * <p>Run it from an IDE: the IDE has already compiled the project, so this
 * takes the class output and the sources, packages a bundle, waits for the
 * device to dial in, and prints what the device made of it. Editing and
 * re-running is the whole loop -- nothing is installed, nothing is rebuilt on
 * the device.</p>
 *
 * <h2>Which way the connection goes</h2>
 *
 * <p>The device dials this tool, not the other way round. A phone cannot accept
 * an inbound connection on a normal network, and on a USB session the device's
 * loopback is mapped onto the desktop's. So this listens and waits.</p>
 *
 * <h2>Pairing</h2>
 *
 * <p>Over USB the transport is loopback and possession of the cable is the
 * authentication. Over Wi-Fi it is not: any machine on the network could answer
 * a device's dial, and the bundle carries the program's whole source. So a
 * network session pairs first -- this prints a six-digit code, the code is
 * typed on the device, and the device stores this computer only if the digest
 * of (code, peer id) matches. Every later connection is still approved on the
 * device unless the user chose "Always".</p>
 *
 * @author Shai Almog
 */
public final class DevicePush {
    private static final int MAGIC = 0x434E3150;   // "CN1P"
    private static final int V1 = 1;
    private static final int V2 = 2;
    private static final int FRAME_PAIR = 1;
    private static final int FRAME_PUSH = 2;

    private DevicePush() {
    }

    public static void main(String[] args) throws Exception {
        File classes = new File("target/classes");
        File sources = new File("src/main/java");
        String mainClass = "";
        int port = 18234;
        boolean lan = false;
        String device = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--classes".equals(a)) {
                classes = new File(args[++i]);
            } else if ("--source".equals(a)) {
                sources = new File(args[++i]);
            } else if ("--main".equals(a)) {
                mainClass = args[++i];
            } else if ("--port".equals(a)) {
                port = Integer.parseInt(args[++i]);
            } else if ("--lan".equals(a)) {
                lan = true;
            } else if ("--device".equals(a) && i + 1 < args.length) {
                device = args[++i];
                lan = true;
            } else if ("--help".equals(a)) {
                usage();
                return;
            }
        }
        if (!classes.isDirectory()) {
            System.err.println("no compiled classes at " + classes.getAbsolutePath()
                    + " -- build the project first");
            System.exit(2);
        }

        byte[] bundle = buildBundle(classes, sources, mainClass);
        System.out.println("bundle " + bundle.length + " bytes");

        if (lan) {
            System.out.println();
            System.out.println("    This computer: " + describeAddresses(port));
            System.out.println("    Enter that address on the device, under Desktop.");
            System.out.println();
        }
        // An explicit address, for a network that will not let a scan work --
        // guest Wi-Fi with client isolation, a VPN, two subnets. The device
        // shows its own address on screen, and Maven passes it straight
        // through: mvn -Ppush-lan package -Ddevruntime.device=192.168.1.50
        if (device == null) {
            String p = System.getProperty("devruntime.device");
            if (p != null && p.trim().length() > 0) {
                device = p.trim();
                lan = true;
            }
        }
        explicitDevice = device;
        push(bundle, port, lan);
    }

    private static void usage() {
        System.out.println("DevicePush [--classes dir] [--source dir] [--main Class]"
                + " [--port 18234] [--lan] [--device <address>]");
        System.out.println("  --device  connect straight to a device at this address,"
                + " which the runtime shows on screen");
        System.out.println("  --lan   the device is on Wi-Fi rather than USB;"
                + " listens on every interface and pairs first");
    }

    // ------------------------------------------------------------ the bundle

    private static byte[] buildBundle(File classesDir, File sourceRoot, String mainClass)
            throws Exception {
        InterpBundleWriter w = new InterpBundleWriter();
        List<File> classes = new ArrayList<File>();
        collect(classesDir, classes);
        if (classes.isEmpty()) {
            throw new IllegalStateException("no .class files under " + classesDir);
        }
        for (File f : classes) {
            w.addClassFile(f);
        }
        if (sourceRoot.isDirectory()) {
            // The runtime refuses to run a class whose source it cannot show,
            // and resources -- theme.res, CSS, images -- travel with it so the
            // program wears its own design rather than the host app's.
            w.addSourceTree(sourceRoot);
            w.addResourceTree(sourceRoot);
        }
        File res = new File(sourceRoot.getParentFile(), "resources");
        if (res.isDirectory()) {
            w.addResourceTree(res);
        }
        if (mainClass.length() == 0) {
            mainClass = findEntryPoint(classes);
            System.out.println("entry point " + mainClass.replace('/', '.'));
        }
        w.setMainClass(mainClass.replace('.', '/'));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        w.write(out);
        return out.toByteArray();
    }

    /**
     * The class to enter: a {@code main(String[])} if the project has one,
     * otherwise a {@code Lifecycle} subclass, which is what a real application
     * has.
     */
    private static String findEntryPoint(List<File> classes) throws IOException {
        String lifecycle = null;
        for (File f : classes) {
            ClassNode cn = new ClassNode();
            new ClassReader(Files.readAllBytes(f.toPath())).accept(cn, ClassReader.SKIP_CODE);
            for (Object mo : cn.methods) {
                MethodNode m = (MethodNode) mo;
                if ("main".equals(m.name) && "([Ljava/lang/String;)V".equals(m.desc)
                        && (m.access & Opcodes.ACC_STATIC) != 0) {
                    return cn.name;
                }
            }
            if ("com/codename1/system/Lifecycle".equals(cn.superName)) {
                lifecycle = cn.name;
            }
        }
        if (lifecycle != null) {
            return lifecycle;
        }
        throw new IllegalStateException(
                "no entry point: expected a main(String[]) or a Lifecycle subclass");
    }

    private static void collect(File dir, List<File> out) {
        File[] kids = dir.listFiles();
        if (kids == null) {
            return;
        }
        for (File f : kids) {
            if (f.isDirectory()) {
                collect(f, out);
            } else if (f.getName().endsWith(".class")) {
                out.add(f);
            }
        }
    }

    // ----------------------------------------------------------- the transport

    private static void push(byte[] payload, int port, boolean lan) throws Exception {
        if (!lan) {
            // Loopback: the only thing that can answer is a USB-authorised
            // device or a simulator on this machine, so there is nothing for a
            // pairing step to establish that possession has not already.
            send(payload, port, V1, null, true);
            return;
        }
        String peerId = peerId();
        String peerName = System.getProperty("user.name") + "@"
                + InetAddress.getLocalHost().getHostName();
        if (!Files.exists(pairedMarker())) {
            String code = String.format("%06d", new Random().nextInt(1000000));
            System.out.println();
            System.out.println("    ==============================");
            System.out.println("      Pairing code:  " + code);
            System.out.println("    ==============================");
            System.out.println("    Type it on the device now and press Pair.");
            System.out.println("    This computer is " + describeAddresses(port) + ".");
            System.out.println();
            if (!pair(port, peerId, peerName, code)) {
                System.exit(1);
            }
            Files.createDirectories(pairedMarker().getParent());
            Files.write(pairedMarker(), peerId.getBytes(StandardCharsets.UTF_8));
            System.out.println("paired; pushing");
        }
        if (!send(payload, port, V2, peerId, false) && rejectedAsUnpaired()) {
            // The device has forgotten us -- reinstalled, or "forget paired
            // computers" -- while this machine still had a marker saying
            // otherwise. Pair again rather than making the user work out why a
            // push that worked yesterday does not today.
            System.out.println("this device no longer knows this computer; pairing again");
            Files.deleteIfExists(pairedMarker());
            String again = String.format("%06d", new Random().nextInt(1000000));
            System.out.println("    Pairing code: " + again);
            System.out.println("    Type it on the device to allow pushes from this computer.");
            System.out.println();
            if (!pair(port, peerId, peerName, again)) {
                System.exit(1);
            }
            Files.write(pairedMarker(), peerId.getBytes(StandardCharsets.UTF_8));
            send(payload, port, V2, peerId, false);
        }
    }

    private static boolean pair(int port, String peerId, String peerName, String code)
            throws Exception {
        Socket s = accept(port, false, 180000);
        try {
            s.setSoTimeout(180000);   // the user has to read the code and type it
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.writeInt(MAGIC);
            out.writeInt(V2);
            out.writeInt(FRAME_PAIR);
            out.writeUTF(peerId);
            out.writeUTF(peerName);
            out.writeUTF(digestOf(code, peerId));
            out.flush();
            return report(s);
        } finally {
            s.close();
        }
    }

    private static boolean send(byte[] payload, int port, int version, String peerId,
                                boolean loopbackOnly) throws Exception {
        System.out.println("awaiting the device on port " + port);
        Socket s = accept(port, loopbackOnly, 120000);
        try {
            s.setSoTimeout(120000);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.writeInt(MAGIC);
            out.writeInt(version);
            if (version >= V2) {
                out.writeInt(FRAME_PUSH);
                out.writeUTF(peerId);
            }
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
            boolean ok = report(s);
            if (!ok && version < V2) {
                System.exit(1);
            }
            return ok;
        } finally {
            s.close();
        }
    }

    /**
     * Finds a device listening on this machine's own subnets.
     *
     * <p>Tried before waiting to be dialled, because it is the half of the
     * search a desktop is actually good at: 254 addresses with a real thread
     * pool and a 300ms timeout finish in about a second, where the same sweep
     * from a phone takes long enough to look broken. iOS has no server socket,
     * so nothing answers there and the wait below is what finds it.</p>
     */
    private static Socket scanForDevice(int port) {
        final List<String> candidates = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                for (java.net.InterfaceAddress ia : nic.getInterfaceAddresses()) {
                    InetAddress a = ia.getAddress();
                    if (!(a instanceof java.net.Inet4Address)) {
                        continue;
                    }
                    String self = a.getHostAddress();
                    String prefix = self.substring(0, self.lastIndexOf('.') + 1);
                    for (int i = 1; i <= 254; i++) {
                        String candidate = prefix + i;
                        if (!candidate.equals(self) && !candidates.contains(candidate)) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
        } catch (Exception failed) {
            return null;
        }
        if (candidates.isEmpty()) {
            return null;
        }
        System.out.println("looking for a device on " + candidates.size() + " addresses");
        final java.util.concurrent.atomic.AtomicReference<Socket> found =
                new java.util.concurrent.atomic.AtomicReference<Socket>();
        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(64);
        try {
            for (final String candidate : candidates) {
                pool.submit(new Runnable() {
                    public void run() {
                        if (found.get() != null) {
                            return;
                        }
                        Socket s = new Socket();
                        try {
                            s.connect(new InetSocketAddress(candidate, port), 300);
                            if (!found.compareAndSet(null, s)) {
                                s.close();
                            }
                        } catch (IOException nothingThere) {
                            try {
                                s.close();
                            } catch (IOException ignored) {
                                // Nothing to do; the address had nobody on it.
                            }
                        }
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(6, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        Socket s = found.get();
        if (s != null) {
            System.out.println("found a device at " + s.getInetAddress().getHostAddress());
        }
        return s;
    }

    /** Waits for the device to dial in. */
    private static Socket accept(int port, boolean loopbackOnly, int timeoutMs) throws IOException {
        if (explicitDevice != null) {
            Socket direct = new Socket();
            direct.connect(new InetSocketAddress(explicitDevice, port), 4000);
            System.out.println("connected to " + explicitDevice);
            return direct;
        }
        if (!loopbackOnly) {
            Socket scanned = scanForDevice(port);
            if (scanned != null) {
                return scanned;
            }
            System.out.println("no device answered; waiting for one to call in");
        }
        ServerSocket server = new ServerSocket();
        server.setReuseAddress(true);
        // Bound to loopback for a USB session and to every interface for a
        // network one. Binding wide by default would expose the listener --
        // and with it the program's source -- on every network the machine is
        // attached to, for a workflow that does not need it.
        server.bind(loopbackOnly
                ? new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port)
                : new InetSocketAddress(port));
        server.setSoTimeout(timeoutMs);
        try {
            return server.accept();
        } catch (java.net.SocketTimeoutException timeout) {
            throw new IOException("the device never connected on port " + port
                    + ". Is the app running, and pointed at this computer?");
        } finally {
            server.close();
        }
    }

    /** Reads the device's answer and prints it. Returns whether it succeeded. */
    private static boolean report(Socket s) throws IOException {
        DataInputStream in = new DataInputStream(s.getInputStream());
        boolean ok = in.readByte() == 1;
        String message = in.readUTF();
        lastRejection = ok ? null : message;
        System.out.println(ok ? "OK: " + message : "FAILED: " + message);
        return ok;
    }

    /// Why the device refused, so a recoverable refusal can be recovered from.
    private static String lastRejection;

    /// An address given on the command line, tried before any searching.
    private static String explicitDevice;

    private static boolean rejectedAsUnpaired() {
        return lastRejection != null && lastRejection.indexOf("not paired") >= 0;
    }

    // -------------------------------------------------------------- identity

    /** A stable identity for this computer, so the device can recognise it. */
    private static String peerId() throws IOException {
        Path f = Paths.get(System.getProperty("user.home"), ".codenameone", "devruntime-peer");
        if (Files.exists(f)) {
            return new String(Files.readAllBytes(f), StandardCharsets.UTF_8).trim();
        }
        String id = Long.toHexString(new Random().nextLong())
                + Long.toHexString(System.currentTimeMillis());
        Files.createDirectories(f.getParent());
        Files.write(f, id.getBytes(StandardCharsets.UTF_8));
        return id;
    }

    private static Path pairedMarker() {
        return Paths.get(System.getProperty("user.home"), ".codenameone", "devruntime-paired");
    }

    /**
     * Must stay identical to {@code InterpPairingDigest} in the runtime.
     *
     * <p>Hand-rolled rather than {@code MessageDigest}: the device half of this
     * runs on ParparVM, which has neither {@code java.security} nor
     * {@code Long.toHexString}, and the two halves have to agree exactly. It
     * establishes that the pairing code came from whoever can see this
     * terminal, which is all it is for -- the session is not encrypted, and the
     * transport is a local network.</p>
     */
    private static String digestOf(String code, String peerId) {
        String material = "cn1-device-runtime " + code.trim() + " " + peerId;
        long h = 1125899906842597L;
        for (int i = 0; i < material.length(); i++) {
            h = 31 * h + material.charAt(i);
        }
        char[] hex = new char[16];
        for (int i = 15; i >= 0; i--) {
            int nibble = (int) (h & 0xf);
            hex[i] = (char) (nibble < 10 ? '0' + nibble : 'a' + nibble - 10);
            h >>>= 4;
        }
        return new String(hex);
    }

    /** Every address of this machine a device could reasonably dial. */
    private static String describeAddresses(int port) {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof java.net.Inet4Address) {
                        if (sb.length() > 0) {
                            sb.append("  or  ");
                        }
                        sb.append(a.getHostAddress());
                    }
                }
            }
        } catch (Exception failed) {
            return "(could not read this machine's addresses)";
        }
        return sb.length() == 0 ? "(no network address)" : sb.toString();
    }
}
