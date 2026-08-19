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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
 * network session pairs first. This prints a six-digit code; the code is typed
 * on the device; both ends then derive the same secret from it without ever
 * sending it, and the device challenges this computer to prove it holds the
 * same one. Every connection afterwards answers a fresh challenge that also
 * covers the bundle, so a captured frame authenticates nothing the second time
 * and a program cannot be swapped in behind a valid answer. The device still
 * asks its user to approve, unless they chose "Always".</p>
 *
 * @author Shai Almog
 */
public final class DevicePush {
    private static final int MAGIC = 0x434E3150;   // "CN1P"
    private static final int V1 = 1;

    /**
     * Challenge-response push. There was a v2 in which the peer id alone
     * authorised a push -- a plaintext bearer token on a LAN -- and it is gone
     * rather than deprecated.
     */
    private static final int V3 = 3;
    private static final int FRAME_PING = 0;
    private static final int FRAME_PAIR = 1;
    private static final int FRAME_PUSH = 2;

    /** Must equal InterpPairingSecret.ITERATIONS, or nothing pairs. */
    private static final int PAIRING_ITERATIONS = 20000;

    /**
     * One instance, seeded once. A fresh SecureRandom per call re-seeds every
     * time, which is both slower and worse, and these values are a peer id and
     * a pairing code -- guessing either is the whole attack.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

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
    static String findEntryPoint(List<File> classes) throws IOException {
        java.util.Map<String, String> supers = new java.util.HashMap<String, String>();
        java.util.Set<String> abstractClasses = new java.util.HashSet<String>();
        java.util.List<String> mains = new java.util.ArrayList<String>();
        for (File f : classes) {
            ClassNode cn = new ClassNode();
            new ClassReader(Files.readAllBytes(f.toPath())).accept(cn, ClassReader.SKIP_CODE);
            for (Object mo : cn.methods) {
                MethodNode m = (MethodNode) mo;
                if ("main".equals(m.name) && "([Ljava/lang/String;)V".equals(m.desc)
                        && (m.access & Opcodes.ACC_STATIC) != 0) {
                    mains.add(cn.name);
                }
            }
            supers.put(cn.name, cn.superName);
            if ((cn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0) {
                abstractClasses.add(cn.name);
            }
        }
        if (!mains.isEmpty()) {
            // Sorted, because listFiles() has no defined order: a tree with a
            // second main -- a diagnostic launcher, a utility -- would
            // otherwise push one program today and the other tomorrow from the
            // same sources. Naming them says which was chosen and that there
            // was a choice.
            java.util.Collections.sort(mains);
            if (mains.size() > 1) {
                System.out.println("more than one main(String[]): " + mains
                        + " -- entering " + mains.get(0));
            }
            return mains.get(0);
        }
        // Transitively, and skipping the abstract ones. A project whose app
        // extends its own BaseApp extends Lifecycle has two Lifecycle
        // descendants, and entering the wrong one runs a class that was never
        // meant to be instantiated.
        String lifecycle = null;
        for (java.util.Map.Entry<String, String> e : supers.entrySet()) {
            if (abstractClasses.contains(e.getKey()) || !descendsFromLifecycle(e.getKey(), supers)) {
                continue;
            }
            if (lifecycle == null) {
                lifecycle = e.getKey();
                continue;
            }
            // Deepest wins: with BaseApp and MyApp both descending from
            // Lifecycle, MyApp is the application. A genuine tie is broken by
            // name, because entries arrive in hash order and an entry point
            // that changes between two identical pushes is worse than either
            // answer.
            int mine = depthOf(e.getKey(), supers);
            int best = depthOf(lifecycle, supers);
            if (mine > best || (mine == best && e.getKey().compareTo(lifecycle) < 0)) {
                lifecycle = e.getKey();
            }
        }
        if (lifecycle != null) {
            return lifecycle;
        }
        throw new IllegalStateException(
                "no entry point: expected a main(String[]) or a Lifecycle subclass");
    }

    /**
     * Whether a class reaches Lifecycle through its superclasses.
     *
     * <p>Bounded by what has been seen, not by a count: a chain of classes is
     * acyclic, and a count refused a hierarchy for being deep -- reporting that
     * a project with an entry point has none.</p>
     */
    private static boolean descendsFromLifecycle(String name,
                                                 java.util.Map<String, String> supers) {
        java.util.Set<String> seen = new java.util.HashSet<String>();
        String parent = supers.get(name);
        while (parent != null && seen.add(parent)) {
            if ("com/codename1/system/Lifecycle".equals(parent)) {
                return true;
            }
            parent = supers.get(parent);
        }
        return false;
    }

    /** How far a class sits below the deepest ancestor the bundle knows. */
    private static int depthOf(String name, java.util.Map<String, String> supers) {
        java.util.Set<String> seen = new java.util.HashSet<String>();
        int depth = 0;
        String at = supers.get(name);
        while (at != null && seen.add(at)) {
            depth++;
            at = supers.get(at);
        }
        return depth;
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
            send(payload, port, null, true);
            return;
        }
        String peerId = peerId();
        String peerName = System.getProperty("user.name") + "@"
                + InetAddress.getLocalHost().getHostName();
        if (send(payload, port, peerId, false)) {
            return;
        }
        if (!rejectedAsUnpaired()) {
            // Denied on the device, or the program threw. Either way the push
            // did not run, and a Maven goal that exits 0 there reports green
            // for a program that never started.
            System.exit(1);
        }
        {
            // Either this computer has never paired, or the device has
            // forgotten it -- reinstalled, or "forget paired computers". Both
            // recover the same way, and doing it automatically beats making the
            // user work out why a push that worked yesterday does not today.
            String code = String.format("%06d", RANDOM.nextInt(1000000));
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
            System.out.println("paired; pushing");
            if (!send(payload, port, peerId, false)) {
                System.exit(1);
            }
        }
    }

    /**
     * Pairs with a device: two round trips, since the device cannot challenge
     * until a human has typed the code the challenge is answered with.
     *
     * <p>The secret is derived here and on the device from the same three
     * inputs and is never transmitted. What this sends is an HMAC over the
     * device's nonce, which authenticates this exchange and no other.</p>
     */
    private static boolean pair(int port, String peerId, String peerName, String code)
            throws Exception {
        Socket s = accept(port, false, 180000);
        try {
            s.setSoTimeout(180000);   // the user has to read the code and type it
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            out.writeInt(MAGIC);
            out.writeInt(V3);
            out.writeInt(FRAME_PAIR);
            out.writeUTF(peerId);
            out.writeUTF(peerName);
            out.flush();

            if (in.readByte() != 1) {
                String message = in.readUTF();
                lastRejection = message;
                System.out.println("FAILED: " + message);
                return false;
            }
            String deviceId = in.readUTF();
            String challenge = in.readUTF();
            byte[] secret = deriveSecret(code, peerId, deviceId);
            out.writeUTF(respond(secret, challenge, null));
            out.flush();
            boolean ok = report(s);
            if (ok) {
                rememberSecret(deviceId, secret);
            }
            return ok;
        } finally {
            s.close();
        }
    }

    /**
     * Sends a bundle.
     *
     * <p>Over loopback this is v1 and unauthenticated, because possession of
     * the USB cable or of this machine already is the authentication. Over a
     * network it is v3: the device names itself and issues a nonce, and the
     * answer covers the bundle as well as the nonce, so what runs on the phone
     * is what left this process.</p>
     */
    private static boolean send(byte[] payload, int port, String peerId,
                                boolean loopbackOnly) throws Exception {
        System.out.println("awaiting the device on port " + port);
        Socket s = accept(port, loopbackOnly, 120000);
        try {
            s.setSoTimeout(120000);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            out.writeInt(MAGIC);
            if (loopbackOnly) {
                out.writeInt(V1);
                out.writeInt(payload.length);
                out.write(payload);
                out.flush();
                // The bundle is away; what follows is the device installing it
                // and entering the program, which takes as long as the program
                // takes. Timing out here would leave the desktop reporting a
                // failed push for a program that is running.
                s.setSoTimeout(0);
                boolean ok = report(s);
                if (!ok) {
                    System.exit(1);
                }
                return ok;
            }
            String desktopChallenge = hex(randomBytes(32));
            out.writeInt(V3);
            out.writeInt(FRAME_PUSH);
            out.writeUTF(peerId);
            out.writeUTF(desktopChallenge);
            out.flush();

            if (in.readByte() != 1) {
                String message = in.readUTF();
                lastRejection = message;
                System.out.println("FAILED: " + message);
                return false;
            }
            String deviceId = in.readUTF();
            String challenge = in.readUTF();
            String deviceProof = in.readUTF();
            // Which device answered decides which secret applies: one computer
            // may be paired with several phones, and the dial-in gives no
            // advance notice of which one this is.
            byte[] secret = secretFor(deviceId);
            if (secret == null) {
                lastRejection = "this computer is not paired with this device";
                System.out.println("FAILED: " + lastRejection);
                return false;
            }
            // The device proves itself before the bundle leaves this process. A
            // device id is public, so anything on the LAN could answer this
            // dial claiming to be a paired phone, and the bundle carries the
            // program's whole source.
            if (!respond(secret, desktopChallenge, null).equals(deviceProof)) {
                lastRejection = "the device on the other end did not authenticate";
                System.out.println("FAILED: " + lastRejection);
                return false;
            }
            out.writeUTF(respond(secret, challenge, payload));
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
            // Past this point the device may be waiting for a person to approve
            // the push, and then installing and starting the program. Discovery
            // and authentication kept their deadlines; this part cannot have
            // one, or a slow start is reported as a failure while the program
            // runs.
            s.setSoTimeout(0);
            return report(s);
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
    private static String scanForDevice(int port) {
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
        final java.util.concurrent.atomic.AtomicReference<String> found =
                new java.util.concurrent.atomic.AtomicReference<String>();
        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(64);
        try {
            for (final String candidate : candidates) {
                pool.execute(new Runnable() {
                    public void run() {
                        if (found.get() != null) {
                            return;
                        }
                        if (isDeviceRuntime(candidate, port)) {
                            found.compareAndSet(null, candidate);
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
        String address = found.get();
        if (address != null) {
            System.out.println("found a device at " + address);
        }
        return address;
    }

    /**
     * Asks an address whether it is a device runtime, and believes only an
     * answer in our own protocol.
     *
     * <p>A separate connection from the one the push will use, because the
     * question consumes a connection: the desktop speaks first in this
     * protocol, so there is no way to probe without committing the frame.</p>
     */
    private static boolean isDeviceRuntime(String candidate, int port) {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(candidate, port), 300);
            s.setSoTimeout(1500);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.writeInt(MAGIC);
            out.writeInt(V3);
            out.writeInt(FRAME_PING);
            out.flush();
            DataInputStream in = new DataInputStream(s.getInputStream());
            if (in.readByte() != 1) {
                return false;
            }
            return in.readUTF().length() > 0;
        } catch (IOException notTheRuntime) {
            // Nobody there, or something there that does not speak this.
            return false;
        } finally {
            try {
                s.close();
            } catch (IOException ignored) {
                // Nothing useful to do.
            }
        }
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
            if (discoveredDevice == null) {
                String scanned = scanForDevice(port);
                if (scanned != null) {
                    // Pinned for the rest of this push. A push is up to three
                    // exchanges -- try, pair, try again -- each its own
                    // connection, and scanning again each time picks whichever
                    // device answers first. With two runtimes on the network
                    // that means pairing one phone and then pushing to the
                    // other, which correctly reports that it is not paired.
                    discoveredDevice = scanned;
                    System.out.println("device at " + scanned);
                }
            }
            if (discoveredDevice != null) {
                // A fresh connection to the address that answered: the probe
                // consumed the one it asked on.
                Socket direct = new Socket();
                direct.connect(new InetSocketAddress(discoveredDevice, port), 4000);
                return direct;
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

    /// The address a scan found, kept for the whole push so the pairing and the
    /// push that follows it reach the same device.
    private static String discoveredDevice;

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
        String id = hex(randomBytes(16));
        createParent(f);
        Files.write(f, id.getBytes(StandardCharsets.UTF_8));
        return id;
    }

    /** Where the secrets established with each device are kept. */
    private static Path secretsFile() {
        return Paths.get(System.getProperty("user.home"), ".codenameone",
                "devruntime-secrets.properties");
    }

    private static byte[] secretFor(String deviceId) throws IOException {
        Path f = secretsFile();
        if (!Files.exists(f)) {
            return null;
        }
        Properties p = new Properties();
        InputStream in = Files.newInputStream(f);
        try {
            p.load(in);
        } finally {
            in.close();
        }
        String hex = p.getProperty(deviceId);
        return hex == null ? null : unhex(hex);
    }

    private static void rememberSecret(String deviceId, byte[] secret) throws IOException {
        Path f = secretsFile();
        Properties p = new Properties();
        if (Files.exists(f)) {
            InputStream in = Files.newInputStream(f);
            try {
                p.load(in);
            } finally {
                in.close();
            }
        }
        p.setProperty(deviceId, hex(secret));
        createParent(f);
        OutputStream out = Files.newOutputStream(f);
        try {
            p.store(out, "Codename One device runtime -- shared secrets, one per paired device");
        } finally {
            out.close();
        }
        // It authorises running code on somebody's phone; nobody else on this
        // machine needs to read it.
        try {
            Files.setPosixFilePermissions(f,
                    PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException notPosix) {
            // Windows: the default ACL is the user's own, which is the intent.
        }
    }

    /** Creates the .codenameone directory, if the path has one to create. */
    private static void createParent(Path f) throws IOException {
        Path parent = f.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Must stay identical to {@code InterpPairingSecret} in the runtime.
     *
     * <p>Written against the JDK's own HMAC while the device half is written
     * against Codename One's, because ParparVM has no {@code javax.crypto} --
     * two implementations of one standard, which is exactly what
     * {@code InterpPairingSecretTest} exists to keep honest.</p>
     */
    private static byte[] deriveSecret(String code, String peerId, String deviceId) {
        byte[] key = code.trim().getBytes(StandardCharsets.UTF_8);
        byte[] block = hmac(key,
                ("cn1-device-runtime|" + peerId + "|" + deviceId).getBytes(StandardCharsets.UTF_8));
        for (int i = 1; i < PAIRING_ITERATIONS; i++) {
            block = hmac(key, block);
        }
        return block;
    }

    /** The answer to a device's challenge, optionally covering a bundle. */
    private static String respond(byte[] secret, String challenge, byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update(challenge.getBytes(StandardCharsets.UTF_8));
            if (payload != null) {
                mac.update(payload);
            }
            return hex(mac.doFinal());
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException("HmacSHA256 is required of every JRE", impossible);
        }
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException("HmacSHA256 is required of every JRE", impossible);
        }
    }

    private static byte[] randomBytes(int count) {
        byte[] out = new byte[count];
        RANDOM.nextBytes(out);
        return out;
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    private static byte[] unhex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte)((Character.digit(s.charAt(i * 2), 16) << 4)
                    | Character.digit(s.charAt(i * 2 + 1), 16));
        }
        return out;
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
